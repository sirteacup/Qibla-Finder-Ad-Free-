package com.znazmul.qiblafinder

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlin.math.*

// Kaaba coordinates
private const val KAABA_LAT = 21.4225
private const val KAABA_LNG = 39.8262

private fun calculateQiblaBearing(userLat: Double, userLng: Double): Float {
    val lat1 = Math.toRadians(userLat)
    val lat2 = Math.toRadians(KAABA_LAT)
    val dLng = Math.toRadians(KAABA_LNG - userLng)
    val x = sin(dLng) * cos(lat2)
    val y = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLng)
    return ((Math.toDegrees(atan2(x, y)).toFloat() + 360f) % 360f)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            QiblaCompassScreen()
        }
    }
}

@Composable
fun QiblaCompassScreen() {
    val context = LocalContext.current

    var azimuth by remember { mutableStateOf(0f) }
    var qiblaBearing by remember { mutableStateOf<Float?>(null) }
    var statusText by remember { mutableStateOf("Acquiring location...") }
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        if (!granted) statusText = "Location permission required"
    }

    // Rotation vector sensor for smooth compass heading
    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        val rotationMatrix = FloatArray(9)
        val remappedMatrix = FloatArray(9)
        val orientation = FloatArray(3)
        var filtered = 0f

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                // When pitch > ~25° (phone tilting upright), remap so device Z acts as
                // the world Y reference — avoids gimbal lock in vertical hold.
                // When flat, use the matrix directly (AXIS_X/AXIS_Y is the default).
                SensorManager.getOrientation(rotationMatrix, orientation)
                val pitch = Math.toDegrees(orientation[1].toDouble()).toFloat()
                val useVertical = Math.abs(pitch) > 25f
                if (useVertical) {
                    SensorManager.remapCoordinateSystem(
                        rotationMatrix, SensorManager.AXIS_X, SensorManager.AXIS_Z, remappedMatrix
                    )
                    SensorManager.getOrientation(remappedMatrix, orientation)
                }
                val raw = ((Math.toDegrees(orientation[0].toDouble()).toFloat()) + 360f) % 360f
                // Low-pass filter with wrap-around handling
                var diff = raw - filtered
                if (diff > 180f) diff -= 360f
                if (diff < -180f) diff += 360f
                filtered = ((filtered + 0.2f * diff) + 360f) % 360f
                azimuth = filtered
            }
            override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
        }

        rotationSensor?.let {
            sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_GAME)
        }

        onDispose { sensorManager.unregisterListener(listener) }
    }

    // Location updates
    DisposableEffect(hasPermission) {
        if (!hasPermission) return@DisposableEffect onDispose {}

        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        val locationListener = LocationListener { location ->
            qiblaBearing = calculateQiblaBearing(location.latitude, location.longitude)
        }

        try {
            // Use last known immediately if available
            val last = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            last?.let { qiblaBearing = calculateQiblaBearing(it.latitude, it.longitude) }

            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000L, 5f, locationListener)
            }
            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 5000L, 5f, locationListener)
            }
        } catch (_: SecurityException) {
            statusText = "Location permission required"
        }

        onDispose { locationManager.removeUpdates(locationListener) }
    }

    LaunchedEffect(Unit) {
        if (!hasPermission) permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D1A)),
        contentAlignment = Alignment.Center
    ) {
        if (qiblaBearing != null) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "QIBLA",
                    color = Color(0xFFD4AF37),
                    fontSize = 22.sp,
                    letterSpacing = 6.sp
                )
                Spacer(modifier = Modifier.height(32.dp))
                CompassView(
                    azimuth = azimuth,
                    qiblaBearing = qiblaBearing!!,
                    modifier = Modifier.size(320.dp)
                )
                Spacer(modifier = Modifier.height(32.dp))
                Text(
                    text = "${qiblaBearing!!.toInt()}° from North",
                    color = Color(0xFF888888),
                    fontSize = 14.sp
                )
            }
        } else {
            Text(
                text = statusText,
                color = Color(0xFF888888),
                fontSize = 16.sp
            )
        }
    }
}

@Composable
fun CompassView(azimuth: Float, qiblaBearing: Float, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val radius = minOf(cx, cy) * 0.88f

        val gold = Color(0xFFD4AF37)
        val goldDim = Color(0xFF7A6520)
        val bgColor = Color(0xFF1A1A2E)
        val ringColor = Color(0xFF2A2A4A)

        // Background circle
        drawCircle(color = bgColor, radius = radius, center = Offset(cx, cy))
        drawCircle(
            color = ringColor,
            radius = radius,
            center = Offset(cx, cy),
            style = Stroke(width = 1.5.dp.toPx())
        )
        drawCircle(
            color = gold,
            radius = radius + 3.dp.toPx(),
            center = Offset(cx, cy),
            style = Stroke(width = 1.dp.toPx())
        )

        // Rotating rose (ticks + cardinal labels)
        rotate(-azimuth, pivot = Offset(cx, cy)) {
            for (i in 0 until 360 step 5) {
                val rad = Math.toRadians(i.toDouble())
                val isMajor = i % 90 == 0
                val isMid = i % 45 == 0
                val tickLen = when {
                    isMajor -> 18.dp.toPx()
                    isMid -> 12.dp.toPx()
                    else -> 6.dp.toPx()
                }
                val strokeW = if (isMajor || isMid) 1.5.dp.toPx() else 1.dp.toPx()
                val outerR = radius
                val innerR = radius - tickLen
                drawLine(
                    color = if (isMajor) gold else goldDim,
                    start = Offset((cx + innerR * sin(rad)).toFloat(), (cy - innerR * cos(rad)).toFloat()),
                    end = Offset((cx + outerR * sin(rad)).toFloat(), (cy - outerR * cos(rad)).toFloat()),
                    strokeWidth = strokeW,
                    cap = StrokeCap.Round
                )
            }

            // Cardinal labels
            val labelR = radius - 34.dp.toPx()
            val textSize = 18.sp.toPx()
            val nPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.RED
                this.textSize = textSize
                textAlign = android.graphics.Paint.Align.CENTER
                isFakeBoldText = true
                isAntiAlias = true
            }
            val cardinalPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.WHITE
                this.textSize = textSize
                textAlign = android.graphics.Paint.Align.CENTER
                isFakeBoldText = true
                isAntiAlias = true
            }
            val offset = 6.dp.toPx()
            drawContext.canvas.nativeCanvas.apply {
                drawText("N", cx, cy - labelR + offset, nPaint)
                drawText("S", cx, cy + labelR + offset, cardinalPaint)
                drawText("E", cx + labelR, cy + offset, cardinalPaint)
                drawText("W", cx - labelR, cy + offset, cardinalPaint)
            }
        }

        // Qibla needle (rotates to point at Qibla)
        val needleAngle = qiblaBearing - azimuth
        rotate(needleAngle, pivot = Offset(cx, cy)) {
            val needleLen = radius * 0.65f
            val tailLen = radius * 0.22f
            val needleW = 5.dp.toPx()

            // Tail
            drawLine(
                color = Color(0xFF444466),
                start = Offset(cx, cy),
                end = Offset(cx, cy + tailLen),
                strokeWidth = needleW * 0.7f,
                cap = StrokeCap.Round
            )

            // Needle shaft
            drawLine(
                color = gold,
                start = Offset(cx, cy),
                end = Offset(cx, cy - needleLen),
                strokeWidth = needleW,
                cap = StrokeCap.Round
            )

            // Arrowhead (triangle)
            val tipY = cy - needleLen
            val arrowW = 10.dp.toPx()
            val arrowH = 14.dp.toPx()
            val path = androidx.compose.ui.graphics.Path().apply {
                moveTo(cx, tipY - arrowH)
                lineTo(cx - arrowW, tipY)
                lineTo(cx + arrowW, tipY)
                close()
            }
            drawPath(path, color = gold)

            // Small Kaaba square at arrow tip
            val squareSize = 8.dp.toPx()
            drawRect(
                color = gold,
                topLeft = Offset(cx - squareSize / 2, tipY - arrowH - squareSize - 2.dp.toPx()),
                size = Size(squareSize, squareSize)
            )
        }

        // Center pivot dot
        drawCircle(color = gold, radius = 7.dp.toPx(), center = Offset(cx, cy))
        drawCircle(color = bgColor, radius = 4.dp.toPx(), center = Offset(cx, cy))
    }
}
