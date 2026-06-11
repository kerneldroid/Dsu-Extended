package com.dsu.extended.ui.compat

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle

object ExpressiveCompatTheme {
    val colorScheme: ColorScheme
        @Composable get() = MaterialTheme.colorScheme
    val textStyles: Typography
        @Composable get() = MaterialTheme.typography
}

@Composable
fun ExpressiveCompatTheme(
    colors: Any? = null,
    textStyles: Any? = null,
    content: @Composable () -> Unit
) {
    MaterialTheme(content = content)
}

enum class ColorSchemeMode { System, Light, Dark }
class ThemeController(
    val colorSchemeMode: ColorSchemeMode = ColorSchemeMode.System,
    val keyColor: Color = Color.Unspecified,
    val isDark: Boolean = false
)

@Composable
fun ExpressiveCompatCard(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.surface,
    content: @Composable () -> Unit
) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = color)) { content() }
}

object ExpressiveCompatButtonDefaults {
    @Composable fun buttonColorsPrimary() = ButtonDefaults.buttonColors()
    @Composable fun buttonColors() = ButtonDefaults.buttonColors()
    @Composable fun textButtonColorsPrimary() = ButtonDefaults.textButtonColors()
}

@Composable
fun ExpressiveCompatButton(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    shapes: Shape = ButtonDefaults.shape,
    content: @Composable RowScope.() -> Unit = { Text(text) }
) {
    Button(onClick = onClick, modifier = modifier, colors = colors, shape = shapes, content = content)
}

@Composable
fun ExpressiveCompatTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    colors: ButtonColors = ButtonDefaults.textButtonColors(),
    shapes: Shape = ButtonDefaults.textShape,
    content: @Composable RowScope.() -> Unit = { Text(text) }
) {
    TextButton(onClick = onClick, modifier = modifier, colors = colors, shape = shapes, content = content)
}

@Composable
fun ExpressiveCompatNavigationBar(modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    NavigationBar(modifier = modifier, content = content)
}

@Composable
fun RowScope.ExpressiveCompatNavigationItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    label: @Composable () -> Unit,
    showDivider: Boolean = false
) {
    NavigationBarItem(selected = selected, onClick = onClick, icon = icon, label = label)
}

@Composable
fun ExpressiveCompatCircularLoadingIndicator(modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.primary) {
    CircularProgressIndicator(modifier = modifier, color = color)
}

@Composable
fun ExpressiveCompatInfiniteLoadingIndicator(modifier: Modifier = Modifier, progressColor: Color = MaterialTheme.colorScheme.primary) {
    CircularProgressIndicator(modifier = modifier, color = progressColor)
}

@Composable
fun ExpressiveCompatInfiniteProgressIndicator(modifier: Modifier = Modifier, progressColor: Color = MaterialTheme.colorScheme.primary) {
    CircularProgressIndicator(modifier = modifier, color = progressColor)
}

@Composable
fun ExpressiveCompatIndeterminateLoadingBar(modifier: Modifier = Modifier, progressColor: Color = MaterialTheme.colorScheme.primary) {
    LinearProgressIndicator(modifier = modifier, color = progressColor)
}

@Composable
fun ExpressiveCompatLinearProgressIndicator(modifier: Modifier = Modifier, progressColor: Color = MaterialTheme.colorScheme.primary) {
    LinearProgressIndicator(modifier = modifier, color = progressColor)
}

@Composable
fun ExpressiveCompatProgressBar(progress: Float, modifier: Modifier = Modifier, progressColor: Color = MaterialTheme.colorScheme.primary) {
    LinearProgressIndicator(progress = { progress }, modifier = modifier, color = progressColor)
}

@Composable
fun ExpressiveCompatSwitch(checked: Boolean, onCheckedChange: ((Boolean) -> Unit)?, modifier: Modifier = Modifier) {
    Switch(checked = checked, onCheckedChange = onCheckedChange, modifier = modifier)
}

@Composable
fun ExpressiveCompatTextField(value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier, label: String? = null) {
    OutlinedTextField(value = value, onValueChange = onValueChange, modifier = modifier, label = if (label != null) { { Text(label) } } else null)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpressiveCompatSmallTopAppBar(title: String, modifier: Modifier = Modifier) {
    TopAppBar(title = { Text(title) }, modifier = modifier)
}

fun Typography.title3() = this.titleLarge
fun Typography.title4() = this.titleMedium
fun Typography.body2() = this.bodyMedium
val ColorScheme.onSurfaceVariantSummary get() = this.onSurfaceVariant
