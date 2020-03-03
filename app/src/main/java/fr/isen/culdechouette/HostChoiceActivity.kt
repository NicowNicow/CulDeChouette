package fr.isen.culdechouette


import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.MediaPlayer
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.activity_host_choice.*
import java.util.*
import kotlin.concurrent.schedule


// Host Selection activity

class HostChoiceActivity : AppCompatActivity() {

    private var usernamePref: SharedPreferences? = null
    lateinit var roomList: MutableList<Room>
    lateinit var firebaseRef: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_host_choice)
        roomList = mutableListOf()
        firebaseRef = FirebaseDatabase.getInstance().getReference("waiting_rooms")
        firebaseRef.addValueEventListener(object: ValueEventListener{
            override fun onCancelled(error: DatabaseError) {
                throw error.toException()
            }
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    roomList.clear()
                    for (index in snapshot.children){
                        var userCount = index.child("users").childrenCount
                        firebaseRef.child(index.key!!).child("room_key").setValue(index.key!!)
                        firebaseRef.child(index.key!!).child("user_count").setValue(userCount)
                        if (((userCount.toString().toInt())<(index.child("capacity").value.toString().toInt()))&&(!index.child("game_started_boolean").value.toString().toBoolean())) {
                            val room = index.getValue(Room::class.java)
                            roomList.add(room!!)
                        }
                    }
                    val adapter = RoomListAdapter(applicationContext, R.layout.rooms_list, roomList )
                    roomListView.adapter = adapter
                }
            }
        })
        usernamePref = getSharedPreferences("preferedName", Context.MODE_PRIVATE)
        checkPreferences()
        rulesButton.setOnClickListener { doRules() }
        joinButton.setOnClickListener { doJoin() }
        creationButton.setOnClickListener { doCreation() }
        usernameValue.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {}
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) { usernamePref?.edit()?.putString("usernameKey", usernameValue.text.toString())?.apply() }
        })
    }

    override fun onResume () {
        super.onResume()
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE
        window.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        actionBar?.hide()
    }

    private fun doRules() {
        val scrollSound = MediaPlayer.create(this, R.raw.elder_scroll)
        scrollSound.start()
        Timer("SoundTemporisation", false).schedule(500) {
            val intentRules = Intent(this@HostChoiceActivity, TutorialActivity::class.java)
            intentRules.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            this@HostChoiceActivity.startActivity(intentRules)
        }
    }

    private fun doJoin() {

    }

    private fun doCreation() {
        if (usernameValue.text.isNotBlank())
        {
            val scrollSound = MediaPlayer.create(this, R.raw.elder_scroll)
            scrollSound.start()
            Timer("SoundTemporisation", false).schedule(500) {
                val intentCreation = Intent(this@HostChoiceActivity, CreationRoomActivity::class.java)
                intentCreation.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                this@HostChoiceActivity.startActivity(intentCreation)
            }
        }
        else
        {
            Toast.makeText(this, R.string.errorUsername, Toast.LENGTH_LONG).show()
        }

    }

    private fun checkPreferences() {
        val usernameSaved = usernamePref?.getString("usernameKey", null)?:""
        usernameValue?.setText(usernameSaved)
    }


}
