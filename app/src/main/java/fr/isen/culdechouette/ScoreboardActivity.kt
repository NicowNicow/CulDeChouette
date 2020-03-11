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
import kotlinx.android.synthetic.main.activity_scoreboard.*
import java.lang.NumberFormatException

class ScoreboardActivity : AppCompatActivity() {

    private lateinit var matchmakingAnnulation: MatchmakingAnnulation
    private var matchmakingSettings : SharedPreferences? = null
    private lateinit var userList: MutableList<User>
    private lateinit var firebaseRef: DatabaseReference
    private var usersCount = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scoreboard)
        matchmakingSettings = getSharedPreferences("currentRoom", Context.MODE_PRIVATE)
        firebaseRef = FirebaseDatabase.getInstance().getReference("waiting_rooms").child(matchmakingSettings?.getString("roomKey", null)?: "")
        val displayParam: DisplayMetrics? = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayParam)
        matchmakingAnnulation = MatchmakingAnnulation(this@ScoreboardActivity, displayParam!!, matchmakingSettings?.getString("userKey", null)?:"", matchmakingSettings?.getString("roomKey", null)?:"", firebaseRef)
        userList = mutableListOf()
        fillUserList()
        leaderboard.setOnClickListener { doMainMenu() }
        startService(Intent(this@ScoreboardActivity, MatchmakingDisconnectedService::class.java))
    }

    override fun onResume () {
        super.onResume()
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or SYSTEM_UI_FLAG_IMMERSIVE
        window.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        actionBar?.hide()
    }

    override fun onBackPressed() {
        matchmakingAnnulation.windowStopMatchmaking()
    }

    private fun doMainMenu() {
        deleteUser()
        countUsers()
        if (usersCount == 0) {
            deleteRoom()
        }
        stopService(Intent(this@ScoreboardActivity, MatchmakingDisconnectedService::class.java))
        val doorSound = MediaPlayer.create(this, R.raw.door)
        doorSound.start()
        val intentMain = Intent(this@ScoreboardActivity, MainActivity::class.java)
        intentMain.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        this@ScoreboardActivity.startActivity(intentMain)
    }

    private fun fillUserList() {
        firebaseRef.addListenerForSingleValueEvent(object: ValueEventListener {
            override fun onCancelled(error: DatabaseError) { Log.i("DatabaseError", error.toString()) }
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    userList.clear()
                    fetchUserValues(snapshot.child("users"))
                    userList.sortedBy {it.score}
                    val adapter = ResultsAdapter(applicationContext, R.layout.scoreboard_element, userList )
                    leaderboardView.adapter = adapter
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

    private fun deleteUser() {
        firebaseRef.child("users").child(matchmakingSettings?.getString("userKey", null)?:"").removeValue()
    }

    private fun deleteRoom() {
        firebaseRef.removeValue()
    }

    private fun countUsers() {
        firebaseRef.child("users").addValueEventListener(object: ValueEventListener{
            override fun onCancelled(error: DatabaseError) {
                Log.i("DatabaseError", error.toString())
            }
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    usersCount = snapshot.childrenCount.toInt()
                }
            }
        })
    }
}
