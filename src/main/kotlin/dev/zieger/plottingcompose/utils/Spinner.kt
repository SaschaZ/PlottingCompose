package dev.zieger.plottingcompose.utils

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.DropdownMenu
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp

data class SpinnerColors(
    val icon: Color = Color.Black,
    val label: Color = Color.Black,
    val dropDownBackground: Color = Color.White,
    val dropDownItem: Color = Color.Black
)

@Composable
fun <T> Spinner(
    enabled: Boolean,
    open: MutableState<Boolean>,
    items: SnapshotStateList<T>,
    selected: MutableState<Int>,
    unselectedText: String = "",
    color: SpinnerColors = SpinnerColors(),
    modifier: Modifier = Modifier,
    onSelect: (idx: Int, label: String) -> Unit = { _, _ -> }
) {
    val angle: Float by animateFloatAsState(
        targetValue = if (open.value) 90f else 0f,
        animationSpec = tween(
            durationMillis = 200
        )
    )
    val spinnerIsClosing = remember { mutableStateOf(false) }
    Column(modifier) {
        Row(
            Modifier.clickable {
                if (open.value || !enabled) return@clickable
                if (spinnerIsClosing.value) {
                    spinnerIsClosing.value = false
                    return@clickable
                }
                open.value = true
            }.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (enabled)
                Icon(
                    Icons.Filled.ArrowForward, "",
                    Modifier.rotate(angle).size(16.dp).padding(1.dp),
                    tint = color.icon
                )
            Text(
                items.getOrNull(selected.value)?.toString() ?: unselectedText,
                fontStyle = if (selected.value < 0) FontStyle.Italic else FontStyle.Normal,
                color = color.label
            )
        }
        if (enabled)
            DropdownMenu(open.value, onDismissRequest = {
                spinnerIsClosing.value = true
                open.value = false
            }, modifier = Modifier.background(color.dropDownBackground)) {
                items.forEachIndexed { idx, str ->
                    if (str.toString().isNotBlank() && str != "leer")
                        Box(Modifier.clickable {
                            selected.value = idx
                            open.value = false
                            onSelect(idx, str.toString())
                        }) {
                            Text(str.toString(), Modifier.padding(8.dp), color = color.dropDownItem)
                        }
                }
            }
    }
}