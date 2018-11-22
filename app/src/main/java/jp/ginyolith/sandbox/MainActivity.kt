package jp.ginyolith.sandbox

import android.content.*
import android.os.Bundle
import android.support.v4.content.LocalBroadcastManager
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.amazonaws.auth.AWSCredentialsProvider
import com.amazonaws.mobile.client.AWSMobileClient
import com.amazonaws.mobile.config.AWSConfiguration
import com.amazonaws.mobileconnectors.pinpoint.PinpointConfiguration
import com.amazonaws.mobileconnectors.pinpoint.PinpointManager
import com.amazonaws.mobileconnectors.pinpoint.targeting.endpointProfile.EndpointProfileUser
import com.google.firebase.iid.FirebaseInstanceId
import java.util.*


class MainActivity : AppCompatActivity() {
    private var credentialsProvider: AWSCredentialsProvider? = null
    private var configuration: AWSConfiguration? = null

    private val notificationReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Log.d(TAG, "Received notification from local broadcast. Display it in a dialog.")

            val bundle = intent.extras
            val message = PushListenerService.getMessage(bundle!!)

            AlertDialog.Builder(this@MainActivity)
                    .setTitle("Push notification")
                    .setMessage(message)
                    .setPositiveButton(android.R.string.ok, null)
                    .show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // AWS Mobile Client インスタンスを初期化
        AWSMobileClient.getInstance().initialize(this) {
            // AWSCredentialsProvider及びAWSConfigurationオブジェクトへの参照を取得しておく
            credentialsProvider = AWSMobileClient.getInstance().credentialsProvider
            configuration = AWSMobileClient.getInstance().configuration
        }.execute()

        val config = PinpointConfiguration(
                this@MainActivity,
                AWSMobileClient.getInstance().credentialsProvider,
                AWSMobileClient.getInstance().configuration
        )

        // PinpointManager初期化。セッション開始
        pinpointManager = PinpointManager(config)
        pinpointManager?.sessionClient?.startSession()
        pinpointManager?.analyticsClient?.submitEvents()

        // カスタムエンドポイント設定
        val interestsList = Arrays.asList("science", "politics", "travel")
        pinpointManager?.targetingClient?.addAttribute("Interests", interestsList)
        pinpointManager?.targetingClient?.updateEndpointProfile()

        // ユーザーIDをエンドポイントに割り当てる
        val endpointProfile = pinpointManager?.targetingClient?.currentEndpoint()
        val user = EndpointProfileUser()
        user.userId = "UserIdValue"
        endpointProfile?.user = user
        Log.d("a", "endpointId = " + endpointProfile?.endpointId)
        pinpointManager?.targetingClient?.updateEndpointProfile(endpointProfile)


        // エンドポイントIDをコピー出来るように
        findViewById<Button>(R.id.copy_endpoint_id).setOnClickListener {
            (this.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).let {
                it.primaryClip = ClipData.newPlainText("", endpointProfile?.endpointId)
                Toast.makeText(this, "コピー：${endpointProfile?.endpointId}", Toast.LENGTH_SHORT).show()
            }
        }

        // エンドポイントIDを表示
        findViewById<TextView>(R.id.end_point_id).text = "endpointId:${endpointProfile?.endpointId}"

        // イベント送信設定
        setUpForSendEvents()
    }

    override fun onPause() {
        super.onPause()

        // Unregister notification receiver
        LocalBroadcastManager.getInstance(this).unregisterReceiver(notificationReceiver)
    }

    override fun onResume() {
        super.onResume()

        // Register notification receiver
        LocalBroadcastManager.getInstance(this).registerReceiver(notificationReceiver,
                IntentFilter(PushListenerService.ACTION_PUSH_NOTIFICATION))
    }

    /** Amazon Pinpoint イベント送信に関する設定*/
    fun setUpForSendEvents() {
        // Buttonを押した際の処理を定義
        findViewById<Button>(R.id.button_send_ev).setOnClickListener {
            val event = pinpointManager?.analyticsClient?.createEvent("clickEvButton")
                    ?.withAttribute("hoge","clicked")
                    ?.withAttribute("fuga","clicked")
                    ?.withMetric("metric", Math.random())


            // イベントを記録
            pinpointManager?.analyticsClient?.recordEvent(event)

            // 記録したイベントを一斉に送信
            pinpointManager?.analyticsClient?.submitEvents()
        }
    }

    companion object {
        val TAG = MainActivity.javaClass.simpleName

        private var pinpointManager: PinpointManager? = null

        fun getPinpointManager(context: Context): PinpointManager? {
            if (pinpointManager == null) {
                val pinpointConfig = PinpointConfiguration(
                        context,
                        AWSMobileClient.getInstance().credentialsProvider,
                        AWSMobileClient.getInstance().configuration)

                pinpointManager = PinpointManager(pinpointConfig)
                FirebaseInstanceId.getInstance().instanceId
                        .addOnCompleteListener { task ->
                            val token = task.result.token
                            Log.d(TAG, "Registering push notifications token: $token")
                            pinpointManager!!.notificationClient.registerDeviceToken(token)
                        }
            }
            return pinpointManager
        }
    }
}
