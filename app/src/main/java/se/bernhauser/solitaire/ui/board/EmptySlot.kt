package se.bernhauser.solitaire.ui.board

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import se.bernhauser.solitaire.ui.cards.CardAspectRatio
import se.bernhauser.solitaire.ui.theme.SlotOutline

private val SlotShape = RoundedCornerShape(8.dp)

@Composable
fun EmptySlot(modifier: Modifier = Modifier) {
  Box(
    modifier = modifier
      .fillMaxWidth()
      .aspectRatio(CardAspectRatio)
      .clip(SlotShape)
      .border(width = 2.dp, color = SlotOutline, shape = SlotShape),
  )
}
