package fr.isen.culdechouette

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder


class MatchmakingDisconnectedService : Service() {

    private val binder = LocalBinder()

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    inner class LocalBinder : Binder() {
        fun getService(): MatchmakingDisconnectedService = this@MatchmakingDisconnectedService
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        //matchmakingAnnulation.cancelMatchmakingAppKilled(firebaseRef, matchmakingSettings?.getString("userKey", null)?:"", matchmakingSettings?.getString("roomKey", null)?:"")
    }
}
