package com.abhijith.frpoc.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateValue
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.Divider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.abhijith.frpoc.R
import com.abhijith.frpoc.ui.theme.claret

@Composable
fun AnimatedLine(
    boxSize: Dp = 450.dp,
    animationDuration: Int = 3000
) {
    val infiniteTransition = rememberInfiniteTransition(label = "")

    val heightAnimation by infiniteTransition.animateValue(
        initialValue = 0.dp,
        targetValue = boxSize,
        typeConverter = Dp.VectorConverter,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = animationDuration
                0.dp at 0 // Start of animation
                boxSize at animationDuration / 2 // Midpoint
                0.dp at animationDuration using FastOutSlowInEasing // End of animation
            }
        ),
        label = "heightAnimation"
    )

    Box(modifier = Modifier.size(boxSize)) {
        Image(
            painter = painterResource(id = R.drawable.top_left),
            contentDescription = "start scanning",
            modifier = Modifier.size(boxSize)
        )
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(heightAnimation))
            Divider(
                thickness = 4.dp,
                color = claret,
                modifier = Modifier.width(boxSize) // Adjusted to match the width of the box
            )
        }
    }
}
