package fr.isen.culdechouette

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.MediaPlayer
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.view.View.SYSTEM_UI_FLAG_IMMERSIVE
import android.view.WindowManager
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.activity_waiting_room.*
import java.lang.NumberFormatException


class WaitingRoomActivity : AppCompatActivity() {

    private lateinit var matchmakingAnnulation: MatchmakingAnnulation
    private var matchmakingSettings : SharedPreferences? = null
    private lateinit var userList: MutableList<User>
    private var readyBoolean :Boolean = false
    var firebaseRef: DatabaseReference = FirebaseDatabase.getInstance().getReference("waiting_rooms")


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_waiting_room)
        matchmakingSettings = getSharedPreferences("currentRoom", Context.MODE_PRIVATE)
        val displayParam: DisplayMetrics? = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayParam)
        matchmakingAnnulation = MatchmakingAnnulation(this@WaitingRoomActivity, displayParam!!, matchmakingSettings?.getString("userKey", null)?:"", matchmakingSettings?.getString("roomKey", null)?:"", firebaseRef)
        backButton.setOnClickListener { matchmakingAnnulation.windowStopMatchmaking() }
        readyButton.setOnClickListener { doReady() }
        userList = mutableListOf()
        fillUserList()
        startService(Intent(this@WaitingRoomActivity, MatchmakingDisconnectedService::class.java))
        firebaseRef.child(matchmakingSettings?.getString("roomKey", null)?:"").child("game_started_boolean").addValueEventListener(object: ValueEventListener {
            override fun onCancelled(error: DatabaseError) {
                Log.i("DatabaseError", error.toString())
            }
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    if (snapshot.value.toString().toBoolean()) {
                        doStartGame()
                    }
                }
            }
        })
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
        val keyRoom = matchmakingSettings?.getString("roomKey", null)?:""
        firebaseRef.child(keyRoom).addListenerForSingleValueEvent(object: ValueEventListener {
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
            firebaseRef.child(matchmakingSettings?.getString("roomKey", null)?:"").child("game_started_boolean").setValue(true)
        }
    }


    private fun doStartGame() {
        firebaseRef.child(matchmakingSettings?.getString("roomKey", null)?:"").child("game_parameters").child("user_turn").setValue(userList[0].user_key)
        stopService(Intent(this@WaitingRoomActivity, MatchmakingDisconnectedService::class.java))
        val doorSound = MediaPlayer.create(this, R.raw.door)
        doorSound.start()
        val intentGame = Intent(this@WaitingRoomActivity, GameActivity::class.java)
        intentGame.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        this@WaitingRoomActivity.startActivity(intentGame)
    }

}
