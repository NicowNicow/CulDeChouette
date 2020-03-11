package fr.isen.culdechouette


import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.MediaPlayer
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import kotlinx.android.synthetic.main.activity_creation_room.*
import java.util.*
import kotlin.concurrent.schedule


class CreationRoomActivity : AppCompatActivity(), AdapterView.OnItemSelectedListener {

    private var usernamePref: SharedPreferences? = null
    private var matchmakingSettings : SharedPreferences? = null
    private lateinit var firebaseRef: DatabaseReference
    private lateinit var firebaseRoomKey: String
    private lateinit var roomCreation: Room

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        usernamePref = getSharedPreferences("preferedName", Context.MODE_PRIVATE)
        matchmakingSettings = getSharedPreferences("currentRoom", Context.MODE_PRIVATE)
        firebaseRef = FirebaseDatabase.getInstance().getReference("waiting_rooms")
        firebaseRoomKey = firebaseRef.push().key!!
        roomCreation = Room()
        setContentView(R.layout.activity_creation_room)
        setNamePlaceholderValue()
        setModificationsListeners()
    }

    override fun onResume () {
        super.onResume()
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE
        window.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        actionBar?.hide()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        val intentRules = Intent(this@CreationRoomActivity, HostChoiceActivity::class.java)
        intentRules.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        this@CreationRoomActivity.startActivity(intentRules)
    }

    private fun setNamePlaceholderValue() {
        val username = usernamePref?.getString("usernameKey", null)?:""
        val placeholder = applicationContext.getString(R.string.baseRoomName, username)
        roomNameValue?.setText(placeholder)
    }

    private fun doBack() {
        val scrollSound = MediaPlayer.create(this, R.raw.elder_scroll)
        scrollSound.start()
        Timer("SoundTemporisation", false).schedule(500) {
            val intentBack = Intent(this@CreationRoomActivity, HostChoiceActivity::class.java)
            intentBack.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            this@CreationRoomActivity.startActivity(intentBack)
        }
    }

    private fun doCreate() {
        modifyGameRoomParameters()
        val scrollSound = MediaPlayer.create(this, R.raw.elder_scroll)
        scrollSound.start()
        Timer("SoundTemporisation", false).schedule(500) {
            val intentWait = Intent(this@CreationRoomActivity, WaitingRoomActivity::class.java)
            intentWait.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            this@CreationRoomActivity.startActivity(intentWait)
        }
    }

    private fun createSpinnerAdapter() {
        ArrayAdapter.createFromResource(this,R.array.player_number_choices,R.layout.custom_spinner_text).also { adapter ->
            adapter.setDropDownViewResource(R.layout.custom_spinner_dropdown)
            numberPlayerValue.adapter = adapter
        }
    }

    private fun modifyGameRoomParameters() {
        val keyInitialUser = firebaseRef.child(firebaseRoomKey).child("users").push().key!!
        val initialUser = User(keyInitialUser,usernamePref?.getString("usernameKey", null)?:"", 0, false)
        roomCreation.room_key = firebaseRoomKey
        roomCreation.game_started_boolean = false
        roomCreation.game_parameters = GameParameters()
        roomCreation.room_name = roomNameValue.text.toString()
        roomCreation.room_password = passwordValue.text.toString()
        firebaseRef.child(firebaseRoomKey).setValue(roomCreation)
        firebaseRef.child(firebaseRoomKey).child("users").child(keyInitialUser).setValue(initialUser)
        matchmakingSettings?.edit()?.putString("roomKey", roomCreation.room_key)?.apply()
        matchmakingSettings?.edit()?.putString("userKey", keyInitialUser)?.apply()
    }

    private fun setModificationsListeners() {
        backButton.setOnClickListener { doBack() }
        creationButton.setOnClickListener { doCreate() }
        createSpinnerAdapter()
        numberPlayerValue.onItemSelectedListener = this
        privateSwitch.setOnCheckedChangeListener { _ , isChecked ->
            if (isChecked) {
                passwordText.visibility = View.VISIBLE
                passwordValue.visibility = View.VISIBLE
                roomCreation.room_password_needed = true
            }
            else {
                passwordText.visibility = View.INVISIBLE
                passwordValue.visibility = View.INVISIBLE
                roomCreation.room_password_needed = false
            }
        }
    }

    override fun onItemSelected(parent: AdapterView<*>, view: View, pos: Int, id: Long) { //Method of the OnItemSelected Interface
        val value = parent.getItemAtPosition(pos).toString().toInt()
        roomCreation.capacity=value
    }

    override fun onNothingSelected(parent: AdapterView<*>) {} //Method of the OnItemSelected Interface
}
