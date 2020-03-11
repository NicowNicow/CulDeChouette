package fr.isen.culdechouette

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Binder
import android.os.IBinder
import android.util.DisplayMetrics
import android.util.Log
import com.google.firebase.database.*


class MatchmakingDisconnectedService : Service() {

    private val binder = LocalBinder()
    private lateinit var  firebaseRef: DatabaseReference
    private var matchmakingSettings : SharedPreferences? = null

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    inner class LocalBinder : Binder() {
        fun getService(): MatchmakingDisconnectedService = this@MatchmakingDisconnectedService
    }

    override fun onCreate() {
        super.onCreate()
        matchmakingSettings = getSharedPreferences("currentRoom", Context.MODE_PRIVATE)
        firebaseRef= FirebaseDatabase.getInstance().getReference("waiting_rooms").child(matchmakingSettings?.getString("roomKey", null)?:"")
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        cancelMatchmakingAppKilled(firebaseRef, matchmakingSettings?.getString("userKey", null)?:"")
    }

    private fun cancelMatchmakingAppKilled(firebaseRef: DatabaseReference, userKey: String) {
        firebaseRef.child("users").child(userKey).removeValue()
        var childrenCount: Int
        firebaseRef.addListenerForSingleValueEvent(object: ValueEventListener {
            override fun onCancelled(error: DatabaseError) {
                Log.i("DatabaseError", error.toString())
            }
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    childrenCount = snapshot.child("users").childrenCount.toInt()
                    if (childrenCount == 0) {
                        firebaseRef.removeValue()
                    }
                    else {
                        firebaseRef.child("user_count").setValue(childrenCount)
                    }
                }
            }
        })
    }
}
