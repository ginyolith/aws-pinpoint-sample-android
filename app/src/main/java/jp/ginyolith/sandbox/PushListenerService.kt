package jp.ginyolith.sandbox

import android.content.Intent
import android.os.Bundle
import android.support.v4.content.LocalBroadcastManager
import android.util.Log

import com.amazonaws.mobileconnectors.pinpoint.targeting.notification.NotificationClient
import com.amazonaws.mobileconnectors.pinpoint.targeting.notification.NotificationDetails
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import jp.ginyolith.sandbox.MainActivity

import java.util.HashMap

class PushListenerService : FirebaseMessagingService() {


    override fun onNewToken(token: String?) {
        super.onNewToken(token)

        Log.d(TAG,"Registering push notifications token: " + token!!)
        MainActivity.getPinpointManager(applicationContext)?.notificationClient?.registerDeviceToken(token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage?) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG,"Message: " + remoteMessage?.data)

        // プッシュ通知の内容を取得する
        val notificationClient = MainActivity.getPinpointManager(applicationContext)?.notificationClient

        val notificationDetails = NotificationDetails.builder()
                .from(remoteMessage?.from)
                .mapData(remoteMessage?.data)
                .intentAction(NotificationClient.FCM_INTENT_ACTION)
                .build()

        val pushResult = notificationClient?.handleCampaignPush(notificationDetails)

        if (NotificationClient.CampaignPushResult.NOT_HANDLED != pushResult) {
            /** アプリがフォアグラウンドの場合、 alertを表示する */
            if (NotificationClient.CampaignPushResult.APP_IN_FOREGROUND == pushResult) {
                /* Create a message that will display the raw data of the campaign push in a dialog. */
                val dataMap = HashMap(remoteMessage?.data)
                broadcast(remoteMessage?.from, dataMap)
            }
            return
        }
    }

    private fun broadcast(from: String?, dataMap: HashMap<String, String>) {
        val intent = Intent(ACTION_PUSH_NOTIFICATION)
        intent.putExtra(INTENT_SNS_NOTIFICATION_FROM, from)
        intent.putExtra(INTENT_SNS_NOTIFICATION_DATA, dataMap)
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    companion object {
        val TAG = PushListenerService.javaClass.simpleName

        // Intent action used in local broadcast
        val ACTION_PUSH_NOTIFICATION = "push-notification"
        // Intent keys
        val INTENT_SNS_NOTIFICATION_FROM = "from"
        val INTENT_SNS_NOTIFICATION_DATA = "data"

        /**
         * Helper method to extract push message from bundle.
         *
         * @param data bundle
         * @return message string from push notification
         */
        fun getMessage(data: Bundle): String {
            return (data.get("data") as HashMap<*, *>).toString()
        }
    }
}