package fr.isen.culdechouette

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.MediaPlayer
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.view.View.SYSTEM_UI_FLAG_IMMERSIVE
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.activity_waiting_room.*
import java.lang.NumberFormatException
import java.util.*
import kotlin.concurrent.schedule


class WaitingRoomActivity : AppCompatActivity() {

    private lateinit var matchmakingAnnulation: MatchmakingAnnulation
    private var matchmakingSettings : SharedPreferences? = null
    private lateinit var userList: MutableList<User>
    private var readyBoolean :Boolean = false
    var firebaseRef: DatabaseReference = FirebaseDatabase.getInstance().getReference("waiting_rooms")

    inner class AppLifecycleObserver : LifecycleObserver {
        @OnLifecycleEvent(Lifecycle.Event.ON_START) fun onForeground() {}
        @OnLifecycleEvent(Lifecycle.Event.ON_STOP) fun onBackground() {

        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_waiting_room)
        matchmakingSettings = getSharedPreferences("currentRoom", Context.MODE_PRIVATE)
        var displayParam: DisplayMetrics? = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayParam)
        matchmakingAnnulation = MatchmakingAnnulation(this@WaitingRoomActivity, displayParam!!, matchmakingSettings?.getString("userKey", null)?:"", matchmakingSettings?.getString("roomKey", null)?:"", firebaseRef)
        backButton.setOnClickListener { matchmakingAnnulation.windowStopMatchmaking() }
        readyButton.setOnClickListener { doReady() }
        userList = mutableListOf()
        fillUserList()
        ProcessLifecycleOwner.get().lifecycle.addObserver(AppLifecycleObserver())
        startService(Intent(this@WaitingRoomActivity, MatchmakingDisconnectedService::class.java))
    }

    override fun onResume() {
        super.onResume()
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or SYSTEM_UI_FLAG_IMMERSIVE
        window.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        actionBar?.hide()
    }

    override fun onBackPressed() {
        matchmakingAnnulation.windowStopMatchmaking()
    }

    private fun fillUserList() {
        firebaseRef.child(matchmakingSettings?.getString("roomKey", null)?:"").addValueEventListener(object: ValueEventListener {
            override fun onCancelled(error: DatabaseError) {
                Log.i("DatabaseError", error.toString())
            }
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    userList.clear()
                    fetchUserValues(snapshot.child("users"))
                    val adapter = LobbyListAdapter(applicationContext, R.layout.lobby_users_list, userList )
                    lobbyList.adapter = adapter
                    doCountReady(snapshot)
                }
            }
        })
    }

    private fun fetchUserValues(snapshot: DataSnapshot) {
        for (index in snapshot.children){
            try {
                val user = index.getValue(User::class.java)
                userList.add(user!!)
            }
            catch(error: NumberFormatException) {
                Log.i("NumberFormatException", error.toString())
                continue
            }
        }
    }

    private fun doReady() {
        val scrollSound = MediaPlayer.create(this, R.raw.elder_scroll)
        scrollSound.start()
        if (readyBoolean) {
            firebaseRef.child(matchmakingSettings?.getString("roomKey", null)?:"").child("users").child(matchmakingSettings?.getString("userKey", null)?:"").child("ready_boolean").setValue(false)
            readyBoolean = false
        }
        else {
            firebaseRef.child(matchmakingSettings?.getString("roomKey", null)?:"").child("users").child(matchmakingSettings?.getString("userKey", null)?:"").child("ready_boolean").setValue(true)
            readyBoolean = true
        }
        doAllReady()
    }

    private fun doAllReady() {
        var keyRoom = matchmakingSettings?.getString("roomKey", null)?:""
        firebaseRef.child(keyRoom).addValueEventListener(object: ValueEventListener {
            override fun onCancelled(error: DatabaseError) {
                Log.i("DatabaseError", error.toString())
            }
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    doCountReady(snapshot)
                }
            }
        })
    }

    private fun doCountReady(snapshot: DataSnapshot) {
        var userReadyCount = 0
        for (index in  snapshot.child("users").children) {
            if (index.child("ready_boolean").value.toString().toBoolean()) {
                userReadyCount++
            }
        }
        if ((userReadyCount == snapshot.child("user_count").value.toString().toInt())&&(userReadyCount !=0)&&(userReadyCount !=1)) {
            doStartGame()
        }
    }

    private fun doStartGame() {
        firebaseRef.child(matchmakingSettings?.getString("roomKey", null)?:"").child("game_started_boolean").setValue(true)
        stopService(Intent(this@WaitingRoomActivity, MatchmakingDisconnectedService::class.java))
        val doorSound = MediaPlayer.create(this, R.raw.door)
        doorSound.start()
        Timer("SoundTemporisation", false).schedule(500) {
            val intentGame = Intent(this@WaitingRoomActivity, TestActivity::class.java)
            intentGame.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            this@WaitingRoomActivity.startActivity(intentGame)
        }
    }

}
