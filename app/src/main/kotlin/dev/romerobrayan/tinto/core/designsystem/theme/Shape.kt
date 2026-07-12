package dev.romerobrayan.tinto.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// Corner radii per DESIGN_SYSTEM.md.
val PillShape = RoundedCornerShape(percent = 50)
val CardShape = RoundedCornerShape(18.dp)
val SheetShape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp)
val ButtonShape = RoundedCornerShape(12.dp)
val TileShape = RoundedCornerShape(11.dp)
val FabShape = RoundedCornerShape(16.dp)

val TintoShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = TileShape,
    medium = ButtonShape,
    large = CardShape,
    extraLarge = CardShape,
)
