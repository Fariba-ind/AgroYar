package ir.agroyar.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

class SplashActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AgroYarSplash(
                onFinished = {
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                }
            )
        }
    }
}

@Composable
private fun AgroYarSplash(onFinished: () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = if (visible) 900 else 420),
        label = "splashAlpha"
    )
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.86f,
        animationSpec = tween(durationMillis = if (visible) 1150 else 420),
        label = "splashScale"
    )
    val infinite = rememberInfiniteTransition(label = "splashGlow")
    val glow by infinite.animateFloat(
        initialValue = 0.18f,
        targetValue = 0.42f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    LaunchedEffect(Unit) {
        visible = true
        delay(2250)
        visible = false
        delay(460)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFFF4F3D2),
                        Color(0xFFE7EDB7),
                        Color(0xFF8DBB58),
                        Color(0xFF2A7337)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        AmbientGlow(glow)
        SplashParticles(glow)

        Column(
            modifier = Modifier.graphicsLayer {
                this.alpha = alpha
                scaleX = scale
                scaleY = scale
            },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AgroYarLogo(224.dp)
            Spacer(Modifier.height(22.dp))
            Text(
                text = "AgroYar",
                color = Color(0xFF0B572F),
                fontSize = 48.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = "اگرویار",
                color = Color(0xFF0B572F),
                fontSize = 38.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(14.dp))
            Text(
                text = "همراه هوشمند کشاورزی",
                color = Color(0xFF386D35),
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun AmbientGlow(intensity: Float) {
    Canvas(Modifier.fillMaxSize()) {
        val center = Offset(size.width / 2f, size.height * 0.40f)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = intensity),
                    Color(0xFFF9F2B2).copy(alpha = intensity * 0.6f),
                    Color.Transparent
                ),
                center = center,
                radius = size.minDimension * 0.70f
            ),
            radius = size.minDimension * 0.70f,
            center = center
        )
        repeat(3) { index ->
            drawCircle(
                color = Color.White.copy(alpha = 0.13f - index * 0.025f),
                radius = size.minDimension * (0.33f + index * 0.09f),
                center = center,
                style = Stroke(width = 2f)
            )
        }
    }
}

@Composable
private fun SplashParticles(pulse: Float) {
    Canvas(Modifier.fillMaxSize()) {
        val points = listOf(
            0.16f to 0.25f,
            0.82f to 0.23f,
            0.10f to 0.48f,
            0.90f to 0.44f,
            0.22f to 0.68f,
            0.78f to 0.70f,
            0.46f to 0.18f,
            0.58f to 0.76f
        )
        points.forEachIndexed { index, (x, y) ->
            drawCircle(
                color = Color.White.copy(alpha = (0.30f + pulse).coerceAtMost(0.72f)),
                radius = if (index % 3 == 0) 5f else 3f,
                center = Offset(size.width * x, size.height * y)
            )
        }
    }
}

@Composable
private fun AgroYarLogo(sizeDp: androidx.compose.ui.unit.Dp) {
    Canvas(Modifier.size(sizeDp)) {
        val w = size.width
        val h = size.height
        val green = Color(0xFF2E7D32)
        val dark = Color(0xFF0B572F)
        val lime = Color(0xFF9BCB24)
        val cx = w / 2f

        drawArc(
            brush = Brush.sweepGradient(listOf(dark, green, lime, green, dark)),
            startAngle = 145f,
            sweepAngle = 250f,
            useCenter = false,
            topLeft = Offset(w * 0.10f, h * 0.08f),
            size = Size(w * 0.80f, h * 0.80f),
            style = Stroke(width = w * 0.038f, cap = StrokeCap.Round)
        )

        val stemBottom = Offset(cx, h * 0.67f)
        val stemTop = Offset(cx, h * 0.28f)
        drawLine(
            brush = Brush.verticalGradient(listOf(lime, dark)),
            start = stemBottom,
            end = stemTop,
            strokeWidth = w * 0.025f,
            cap = StrokeCap.Round
        )

        fun leafPath(points: List<Offset>): Path = Path().apply {
            moveTo(points[0].x, points[0].y)
            cubicTo(points[1].x, points[1].y, points[2].x, points[2].y, points[3].x, points[3].y)
            cubicTo(points[4].x, points[4].y, points[5].x, points[5].y, points[0].x, points[0].y)
            close()
        }

        val topLeaf = leafPath(
            listOf(
                Offset(cx, h * 0.29f),
                Offset(w * 0.37f, h * 0.22f),
                Offset(w * 0.43f, h * 0.12f),
                Offset(cx, h * 0.08f),
                Offset(w * 0.59f, h * 0.16f),
                Offset(w * 0.62f, h * 0.24f)
            )
        )
        drawPath(topLeaf, Brush.linearGradient(listOf(lime, green, dark)))

        val leftLeaf = leafPath(
            listOf(
                Offset(cx - w * 0.02f, h * 0.50f),
                Offset(w * 0.33f, h * 0.50f),
                Offset(w * 0.23f, h * 0.42f),
                Offset(w * 0.20f, h * 0.34f),
                Offset(w * 0.36f, h * 0.34f),
                Offset(w * 0.45f, h * 0.40f)
            )
        )
        drawPath(leftLeaf, Brush.linearGradient(listOf(dark, green, lime)))

        val rightLeaf = leafPath(
            listOf(
                Offset(cx + w * 0.02f, h * 0.53f),
                Offset(w * 0.62f, h * 0.50f),
                Offset(w * 0.72f, h * 0.43f),
                Offset(w * 0.77f, h * 0.36f),
                Offset(w * 0.64f, h * 0.35f),
                Offset(w * 0.55f, h * 0.42f)
            )
        )
        drawPath(rightLeaf, Brush.linearGradient(listOf(lime, green, dark)))

        val field1 = Path().apply {
            moveTo(w * 0.18f, h * 0.64f)
            cubicTo(w * 0.36f, h * 0.58f, w * 0.48f, h * 0.74f, w * 0.82f, h * 0.57f)
        }
        val field2 = Path().apply {
            moveTo(w * 0.20f, h * 0.72f)
            cubicTo(w * 0.38f, h * 0.64f, w * 0.50f, h * 0.82f, w * 0.80f, h * 0.65f)
        }
        val field3 = Path().apply {
            moveTo(w * 0.27f, h * 0.79f)
            cubicTo(w * 0.42f, h * 0.70f, w * 0.55f, h * 0.88f, w * 0.72f, h * 0.74f)
        }
        listOf(field1, field2, field3).forEachIndexed { index, path ->
            drawPath(
                path = path,
                brush = Brush.horizontalGradient(listOf(dark, green, lime)),
                style = Stroke(width = w * (0.055f - index * 0.008f), cap = StrokeCap.Round)
            )
        }
    }
}
