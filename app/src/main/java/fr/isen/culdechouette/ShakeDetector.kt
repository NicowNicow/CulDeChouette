package fr.isen.culdechouette

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

/**
 * Detects phone shaking. If more than 75% of the samples taken in the past 0.5s are
 * accelerating, the device is a) shaking, or b) free falling 1.84m (h =
 * 1/2*g*t^2*3/4).
 *
 * @author Bob Lee (bob@squareup.com)
 * @author Eric Burke (eric@squareup.com)
 * contributor Dmitry Mina (dmitry.mina@gmail.com)
 */

class ShakeDetector(private val listener: Listener) : SensorEventListener {

    enum class Sensitivity(val sensitivity: Int) {
        SENSITIVITY_LIGHT(11),
        SENSITIVITY_MEDIUM(13),
        SENSITIVITY_HARD(15),
        SENSITIVITY_HARDER(17)
    }


    private var accelerationThreshold = Sensitivity.SENSITIVITY_LIGHT.sensitivity


    interface Listener {
        fun hearShake()
    }

    private val queue = SampleQueue()

    private var sensorManager: SensorManager? = null
    private var accelerometer: Sensor? = null

    fun start(sensorManager: SensorManager): Boolean {
        // Already started?
        if (accelerometer != null) {
            return true
        }

        accelerometer = sensorManager.getDefaultSensor(
            Sensor.TYPE_ACCELEROMETER)

        // If this phone has an accelerometer, listen to it.
        if (accelerometer != null) {
            this.sensorManager = sensorManager
            sensorManager.registerListener(this, accelerometer,
                SensorManager.SENSOR_DELAY_FASTEST)
        }
        return accelerometer != null
    }

    fun stop() {
        if (accelerometer != null) {
            queue.clear()
            sensorManager!!.unregisterListener(this, accelerometer)
            sensorManager = null
            accelerometer = null
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        val accelerating = isAccelerating(event!!)
        val timestamp = event.timestamp
        queue.add(timestamp, accelerating)
        if (queue.isShaking) {
            queue.clear()
            listener.hearShake()
        }
    }

    /** Returns true if the device is currently accelerating.  */
    private fun isAccelerating(event: SensorEvent): Boolean {
        val ax = event.values[0]
        val ay = event.values[1]
        val az = event.values[2]
        val magnitudeSquared = (ax * ax + ay * ay + az * az).toDouble()
        return magnitudeSquared > accelerationThreshold * accelerationThreshold
    }

    /** Sets the acceleration threshold sensitivity.  */
    fun setSensitivity(accelerationThreshold: ShakeDetector.Sensitivity) {
        this.accelerationThreshold = accelerationThreshold.sensitivity
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    }
}