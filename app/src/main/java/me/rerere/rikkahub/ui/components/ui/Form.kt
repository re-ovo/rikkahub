package me.rerere.rikkahub.ui.components.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun FormItem(
    modifier: Modifier = Modifier,
    label: @Composable () -> Unit,
    content: @Composable () -> Unit
) {
    Column(
        modifier = modifier.padding(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        ProvideTextStyle(
            MaterialTheme.typography.labelMedium.copy(
                color = MaterialTheme.colorScheme.primary
            )
        ) {
            label()
        }
        content()
    }
}

@Preview(showBackground = true)
@Composable
private fun FormItemPreview() {
    FormItem(
        label = { Text("Label") },
        content = {
            OutlinedTextField(
                value = "",
                onValueChange = {}
            )
        }
    )
}