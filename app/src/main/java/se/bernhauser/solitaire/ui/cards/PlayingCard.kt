package se.bernhauser.solitaire.ui.cards

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import se.bernhauser.solitaire.game.Card
import se.bernhauser.solitaire.game.CardBack

const val CardAspectRatio: Float = 2.5f / 3.5f

@Composable
fun PlayingCard(
  modifier: Modifier = Modifier,
  card: Card,
  faceUp: Boolean = true,
  back: CardBack = CardBack.Red,
  revealed: Boolean = true,
  onImageReady: (() -> Unit)? = null,
) {
  if (faceUp) CardFace(modifier = modifier, card = card, revealed = revealed, onImageReady = onImageReady)
  else CardBackImage(modifier = modifier, back = back, revealed = revealed, onImageReady = onImageReady)
}

@Composable
fun CardFace(
  modifier: Modifier = Modifier,
  card: Card,
  revealed: Boolean = true,
  onImageReady: (() -> Unit)? = null,
) {
  CardSlot(modifier = modifier) {
    if (revealed) {
      CardImage(
        model = assetUrl(card.assetKey),
        contentDescription = "${card.rank.name} of ${card.suit.name}",
        onImageReady = onImageReady,
      )
    }
  }
}

@Composable
fun CardBackImage(
  modifier: Modifier = Modifier,
  back: CardBack = CardBack.Red,
  revealed: Boolean = true,
  onImageReady: (() -> Unit)? = null,
) {
  CardSlot(modifier = modifier) {
    if (revealed) {
      CardImage(
        model = assetUrl(back.assetKey),
        contentDescription = "Card back",
        onImageReady = onImageReady,
      )
    }
  }
}

@Composable
fun FlipCard(
  modifier: Modifier = Modifier,
  card: Card,
  back: CardBack = CardBack.Red,
  onComplete: () -> Unit = {},
) {
  val rotation = remember { Animatable(180f) }
  LaunchedEffect(Unit) {
    rotation.animateTo(0f, animationSpec = tween(durationMillis = 450))
    onComplete()
  }
  val cameraDistPx = with(LocalDensity.current) { 12.dp.toPx() }
  val showFace by remember { derivedStateOf { rotation.value <= 90f } }

  Box(
    modifier = modifier
      .aspectRatio(CardAspectRatio)
      .graphicsLayer {
        rotationY = rotation.value
        cameraDistance = cameraDistPx
      },
  ) {
    if (showFace) {
      AsyncImage(
        modifier = Modifier.matchParentSize(),
        model = assetUrl(card.assetKey),
        contentDescription = "${card.rank.name} of ${card.suit.name}",
        contentScale = ContentScale.Fit,
      )
    } else {
      AsyncImage(
        modifier = Modifier
          .matchParentSize()
          .graphicsLayer { rotationY = 180f },
        model = assetUrl(back.assetKey),
        contentDescription = "Card back",
        contentScale = ContentScale.Fit,
      )
    }
  }
}

@Composable
private fun CardSlot(
  modifier: Modifier = Modifier,
  content: @Composable BoxScope.() -> Unit,
) {
  Box(modifier = modifier.aspectRatio(CardAspectRatio), content = content)
}

@Composable
private fun BoxScope.CardImage(
  model: String,
  contentDescription: String,
  onImageReady: (() -> Unit)? = null,
) {
  var notifiedReady by remember(model) { mutableStateOf(false) }
  AsyncImage(
    modifier = Modifier.matchParentSize(),
    model = model,
    contentDescription = contentDescription,
    contentScale = ContentScale.Fit,
    onSuccess = {
      if (!notifiedReady) {
        notifiedReady = true
        onImageReady?.invoke()
      }
    },
    onError = {
      if (!notifiedReady) {
        notifiedReady = true
        onImageReady?.invoke()
      }
    },
  )
}

private fun assetUrl(key: String): String = "file:///android_asset/generated_cards/$key.png"
