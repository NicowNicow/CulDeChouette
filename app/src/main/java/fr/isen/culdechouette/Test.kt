package fr.isen.culdechouette

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ImageView
import kotlinx.android.synthetic.main.activity_test.*

class Test : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test)

        Button.setOnClickListener {
            //imageView2.setImageResource(R.drawable.Chouette1)
            animation()
        }
    }

    private fun animation(){
        val image: ImageView = findViewById(R.id.imageView2)
        val hyperspaceJump: Animation =
            AnimationUtils.loadAnimation(this, R.anim.hyperspace_jump)
        image.startAnimation(hyperspaceJump)
    }
}
