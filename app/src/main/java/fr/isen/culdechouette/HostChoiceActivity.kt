package fr.isen.culdechouette


import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.media.MediaPlayer
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.DisplayMetrics
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.activity_host_choice.*
import java.lang.NumberFormatException
import java.util.*
import kotlin.concurrent.schedule


class HostChoiceActivity : AppCompatActivity() {

    private var usernamePref: SharedPreferences? = null
    private var matchmakingSettings : SharedPreferences? = null
    private var selectedRoom: Int? = null
    lateinit var roomList: MutableList<Room>
    lateinit var firebaseRef: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_host_choice)
        roomList = mutableListOf()
        usernamePref = getSharedPreferences("preferedName", Context.MODE_PRIVATE)
        matchmakingSettings = getSharedPreferences("currentRoom", Context.MODE_PRIVATE)
        matchmakingSettings?.edit()?.clear()?.apply()
        firebaseRef = FirebaseDatabase.getInstance().getReference("waiting_rooms")
        fillRoomList()
        checkPreferences()
        setModificationsListeners()
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
        val keyFetched = matchmakingSettings?.getString("roomKey", null)?:""
        if ((keyFetched != "")&&(usernameValue.text.isNotBlank())) {
            for (room in roomList) {
                if ((room.room_key == keyFetched) && (room.room_password_needed)) { passwordQuery(room) }
                else if ((room.room_key == keyFetched) && (!room.room_password_needed)) { goToWaitingRoom() }
            }
        }
        else if ((matchmakingSettings?.getString("roomKey", null)?:"" == "")&&(usernameValue.text.isNotBlank())) {
            Toast.makeText(this, R.string.errorRoom, Toast.LENGTH_LONG).show()
        }
        else if ((matchmakingSettings?.getString("roomKey", null)?:"" != "")&&(usernameValue.text.isBlank())) {
            Toast.makeText(this, R.string.errorUsername, Toast.LENGTH_LONG).show()
        }
        else {
            Toast.makeText(this, R.string.errorBoth, Toast.LENGTH_LONG).show()
        }
    }

    private fun passwordQuery(room: Room) {
        val passwordDialogBuilder: AlertDialog.Builder? = this@HostChoiceActivity.let { AlertDialog.Builder(it) }
        val displayParam: DisplayMetrics? = DisplayMetrics()
        val layoutParams: WindowManager.LayoutParams? = WindowManager.LayoutParams()
        windowManager.defaultDisplay.getMetrics(displayParam)
        layoutParams?.width = (displayParam?.widthPixels?.times(0.9f))?.toInt()!!
        layoutParams?.height = (displayParam.heightPixels.times(0.9f)).toInt()
        passwordDialogBuilder?.setView(layoutInflater.inflate(R.layout.password_popup, null))?.create()
        val passwordDialog = passwordDialogBuilder?.show()
        passwordDialog?.window?.attributes = layoutParams
        passwordDialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        passwordDialog?.window?.setGravity(Gravity.CENTER)
        passwordDialog?.findViewById<ImageButton>(R.id.passwordBackButton)?.setOnClickListener { passwordDialog.dismiss() }
        passwordDialog?.findViewById<ImageButton>(R.id.validationButton)?.setOnClickListener { passwordValidation(room, passwordDialog) }
    }

    private fun passwordValidation(room: Room, passwordDialog: AlertDialog) {
        val passwordEditText = passwordDialog.findViewById<EditText>(R.id.passwordValue)
        if (room.room_password == passwordEditText.text.toString()) {
            passwordDialog.dismiss()
            goToWaitingRoom()
        }
        else {
            passwordEditText.text.clear()
            Toast.makeText(this, R.string.incorrectPassword, Toast.LENGTH_LONG).show()
        }
    }

    private fun goToWaitingRoom() {
        val scrollSound = MediaPlayer.create(this, R.raw.elder_scroll)
        addUser()
        scrollSound.start()
        Timer("SoundTemporisation", false).schedule(500) {
            val intentWait = Intent(this@HostChoiceActivity, WaitingRoomActivity::class.java)
            intentWait.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            this@HostChoiceActivity.startActivity(intentWait)
        }
    }

    private fun addUser() {
        val keyFetched = matchmakingSettings?.getString("roomKey", null)?:""
        val keyInitialUser = firebaseRef.child(keyFetched).child("users").push().key!!
        val initialUser = User(keyInitialUser,usernamePref?.getString("usernameKey", null)?:"", 0, false)
        firebaseRef.child(keyFetched).child("users").child(keyInitialUser).setValue(initialUser)
        matchmakingSettings?.edit()?.putString("userKey", keyInitialUser)?.apply()
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

    private fun fillRoomList() {
        firebaseRef.addValueEventListener(object: ValueEventListener{
            override fun onCancelled(error: DatabaseError) {
                Log.i("DatabaseError", error.toString())
            }
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    roomList.clear()
                    fetchRoomValues(snapshot)
                    val adapter = RoomListAdapter(applicationContext, R.layout.rooms_list, roomList )
                    roomListView.adapter = adapter
                }
            }
        })
    }

    private fun fetchRoomValues(snapshot: DataSnapshot) {
        for (index in snapshot.children){
            var userCount = index.child("users").childrenCount
            firebaseRef.child(index.key!!).child("room_key").setValue(index.key!!)
            firebaseRef.child(index.key!!).child("user_count").setValue(userCount)
            try {
                if (((userCount.toString().toInt()) < (index.child("capacity").value.toString().toInt())) && (!index.child("game_started_boolean").value.toString().toBoolean())) {
                    val room = index.getValue(Room::class.java)
                    roomList.add(room!!)
                }
            }
            catch(error: NumberFormatException) {
                Log.i("NumberFormatException", error.toString())
                continue
            }
        }
    }

    private fun setModificationsListeners() {
        rulesButton.setOnClickListener { doRules() }
        joinButton.setOnClickListener { doJoin() }
        creationButton.setOnClickListener { doCreation() }
        usernameValue.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {}
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) { usernamePref?.edit()?.putString("usernameKey", usernameValue.text.toString())?.apply() }
        })
        setListViewListener()
    }

    private fun setListViewListener() {
        roomListView.setOnItemClickListener { _, _, position, _ ->
            val itemSelected = roomListView.adapter.getItemId(position).toInt()
            checkSelection(itemSelected)
        }
    }

    private fun checkSelection(itemSelected: Int) {
        if (itemSelected == selectedRoom) {
            selectedRoom = null
            roomListView.setSelector(android.R.color.transparent)
            matchmakingSettings?.edit()?.remove("roomKey")?.apply()
        }
        else {
            selectedRoom = itemSelected
            roomListView.setSelector(android.R.color.holo_red_light)
            matchmakingSettings?.edit()?.putString("roomKey", roomList[itemSelected].room_key)?.apply()
        }
    }
}
