package jp.ginyolith.sandbox

import android.app.Application
import android.util.Log
import com.amazonaws.AmazonClientException
import com.amazonaws.mobile.auth.core.IdentityManager
import com.amazonaws.mobile.config.AWSConfiguration
import com.amazonaws.mobileconnectors.pinpoint.PinpointConfiguration
import com.amazonaws.mobileconnectors.pinpoint.PinpointManager


class MyApplication : Application() {
    lateinit var pinpointManager : PinpointManager
    lateinit var applicationLifeCycleHelper: AbstractApplicationLifeCycleHelper

    override fun onCreate() {
        super.onCreate()
        val awsConfiguration = AWSConfiguration(this)

        if (IdentityManager.getDefaultIdentityManager() == null) {
            val identityManager = IdentityManager(applicationContext, awsConfiguration)
            IdentityManager.setDefaultIdentityManager(identityManager)
        }

        try {
            val config = PinpointConfiguration(this,
                    IdentityManager.getDefaultIdentityManager().credentialsProvider,
                    awsConfiguration)
            pinpointManager = PinpointManager(config)
        } catch (ex: AmazonClientException) {
            Log.e("log", "Unable to initialize PinpointManager. " + ex.message, ex)
        }

        pinpointManager.sessionClient.startSession()

        // The Helper registers itself to receive application lifecycle events when it is constructed.
        // A reference is kept here in order to pass through the onTrimMemory() call from
        // the Application class to properly track when the application enters the background.
        applicationLifeCycleHelper = object : AbstractApplicationLifeCycleHelper(this) {
            override fun applicationEnteredForeground() {
                pinpointManager.getSessionClient().startSession();
                // handle any events that should occur when your app has come to the foreground...
            }

            override fun applicationEnteredBackground() {
                Log.d("aaa", "Detected application has entered the background.");
                pinpointManager.sessionClient.stopSession();
                pinpointManager.analyticsClient.submitEvents();
                // handle any events that should occur when your app has gone into the background...
            }
        };
    }
}