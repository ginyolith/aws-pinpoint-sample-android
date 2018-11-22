package jp.ginyolith.sandbox

import android.util.Log
import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.iid.FirebaseInstanceIdService

class MyFirebaseInstanceIdService : FirebaseInstanceIdService() {
    override fun onTokenRefresh() {
        super.onTokenRefresh()

        Log.d("token","Registering push notifications token: " + FirebaseInstanceId.getInstance().token!!)
        MainActivity.getPinpointManager(applicationContext)?.notificationClient?.registerDeviceToken(FirebaseInstanceId.getInstance().token)
    }
}