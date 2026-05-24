package se.bernhauser.solitaire.ui.win

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

private const val ParticleCount: Int = 140
private val ConfettiColors = listOf(
  Color(0xFFE53935),
  Color(0xFFFFB300),
  Color(0xFF43A047),
  Color(0xFF1E88E5),
  Color(0xFF8E24AA),
  Color(0xFFFFFFFF),
)

@Composable
fun WinOverlay(
  modifier: Modifier = Modifier,
  onNewGame: () -> Unit,
) {
  Box(
    modifier = modifier
      .fillMaxSize()
      .background(Color.Black.copy(alpha = 0.35f)),
    contentAlignment = Alignment.Center,
  ) {
    Confetti(modifier = Modifier.fillMaxSize())
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(16.dp),
      modifier = Modifier.padding(24.dp),
    ) {
      Text(
        text = "You won!",
        color = Color.White,
        fontSize = 48.sp,
        fontWeight = FontWeight.Bold,
      )
      Button(onClick = onNewGame) { Text("New game") }
    }
  }
}

@Composable
private fun Confetti(modifier: Modifier) {
  val particles = remember { mutableStateOf<List<Particle>>(emptyList()) }
  val bounds = remember { mutableStateOf(Size.Zero) }

  LaunchedEffect(Unit) {
    var lastNanos = 0L
    while (true) {
      withFrameNanos { nanos ->
        if (lastNanos == 0L) {
          lastNanos = nanos
          return@withFrameNanos
        }
        val dt = (nanos - lastNanos) / 1_000_000_000f
        lastNanos = nanos
        val size = bounds.value
        if (size == Size.Zero) return@withFrameNanos
        if (particles.value.isEmpty()) {
          particles.value = List(ParticleCount) { spawn(size, seedY = -Random.nextFloat() * size.height) }
        }
        particles.value = particles.value.map { it.advance(dt, size) }
      }
    }
  }

  Canvas(modifier = modifier) {
    bounds.value = size
    for (p in particles.value) {
      rotate(p.rotation, pivot = Offset(p.x, p.y)) {
        drawRect(
          color = p.color,
          topLeft = Offset(p.x - p.size / 2f, p.y - p.size / 2f),
          size = Size(p.size, p.size * 0.6f),
        )
      }
    }
  }
}

private data class Particle(
  val x: Float,
  val y: Float,
  val vx: Float,
  val vy: Float,
  val rotation: Float,
  val omega: Float,
  val size: Float,
  val color: Color,
  val swayPhase: Float,
)

private fun spawn(canvas: Size, seedY: Float = -20f): Particle = Particle(
  x = Random.nextFloat() * canvas.width,
  y = seedY,
  vx = (Random.nextFloat() - 0.5f) * 60f,
  vy = 120f + Random.nextFloat() * 160f,
  rotation = Random.nextFloat() * 360f,
  omega = (Random.nextFloat() - 0.5f) * 360f,
  size = 8f + Random.nextFloat() * 8f,
  color = ConfettiColors[Random.nextInt(ConfettiColors.size)],
  swayPhase = Random.nextFloat() * (2f * PI.toFloat()),
)

private fun Particle.advance(dt: Float, canvas: Size): Particle {
  val gravity = 320f
  val newVy = vy + gravity * dt
  val sway = sin((swayPhase + y * 0.01f).toDouble()).toFloat() * 30f
  val newX = x + (vx + sway) * dt
  val newY = y + newVy * dt
  val newRotation = rotation + omega * dt
  return if (newY > canvas.height + 40f) {
    spawn(canvas)
  } else {
    copy(x = newX, y = newY, vy = newVy, rotation = newRotation)
  }
}
