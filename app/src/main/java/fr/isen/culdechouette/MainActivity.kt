package fr.isen.culdechouette

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import kotlinx.android.synthetic.main.activity_main.*

//Creation of the main menu, nothing really interesting here

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mainID.setOnClickListener{ doLogin() }
    }

    private fun doLogin() {
        val intentHome = Intent( this@MainActivity, HostChoiceActivity::class.java)
        intentHome.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION
        this@MainActivity.startActivity(intentHome)
    }


}
