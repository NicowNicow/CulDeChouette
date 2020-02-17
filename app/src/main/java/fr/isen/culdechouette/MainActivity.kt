package fr.isen.culdechouette

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.DisplayMetrics
import android.widget.ImageView
import android.widget.Toast
import androidx.dynamicanimation.animation.DynamicAnimation
import androidx.dynamicanimation.animation.FlingAnimation
import kotlinx.android.synthetic.main.activity_main.*

//Creation of the main menu, nothing really interesting here

class MainActivity : AppCompatActivity() {

    companion object {
        val friction = 1f
        val maxValue = 2000f
        val minValue = -200f
        var speed = 500f
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

    }

}
