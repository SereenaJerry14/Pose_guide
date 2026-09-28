package com.photopose.app.core.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log

/**
 * Monitors hardware gyroscope / rotation sensors for real-time horizon and tilt leveling
 */
class DeviceOrientationSensor(
    context: Context,
    private val onOrientationChanged: (rollDegrees: Float, pitchDegrees: Float) -> Unit
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val rotationSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        ?: sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)

    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)

    companion object {
        private const val TAG = "DeviceOrientationSensor"
    }

    fun start() {
        if (rotationSensor != null) {
            sensorManager.registerListener(this, rotationSensor, SensorManager.SENSOR_DELAY_UI)
        } else {
            Log.w(TAG, "No rotation or gravity sensor available on this device")
        }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
        } else if (event.sensor.type == Sensor.TYPE_GRAVITY) {
            // Approximate rotation matrix from gravity vector
            val gX = event.values[0]
            val gY = event.values[1]
            val roll = Math.toDegrees(kotlin.math.atan2(gX.toDouble(), gY.toDouble())).toFloat()
            onOrientationChanged(roll, 0f)
            return
        }

        SensorManager.getOrientation(rotationMatrix, orientationAngles)
        // Roll: rotation around Y axis (in radians) -> convert to degrees
        val rollDeg = Math.toDegrees(orientationAngles[2].toDouble()).toFloat()
        // Pitch: rotation around X axis (in radians) -> convert to degrees
        val pitchDeg = Math.toDegrees(orientationAngles[1].toDouble()).toFloat()

        onOrientationChanged(rollDeg, pitchDeg)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // No-op
    }
}
