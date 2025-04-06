package me.rerere.rikkahub.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import me.rerere.rikkahub.ui.theme.extendColors

enum class TagType {
    DEFAULT,
    SUCCESS,
    ERROR,
    WARNING,
    INFO
}

@Composable
fun Tag(
    modifier: Modifier = Modifier,
    type: TagType = TagType.DEFAULT,
    children: @Composable () -> Unit
) {
    val background = when (type) {
        TagType.SUCCESS -> MaterialTheme.extendColors.green2
        TagType.ERROR -> MaterialTheme.extendColors.red2
        TagType.WARNING -> MaterialTheme.extendColors.orange2
        TagType.INFO -> MaterialTheme.extendColors.blue2
        else -> MaterialTheme.extendColors.gray2
    }
    val textColor = when (type) {
        TagType.SUCCESS -> MaterialTheme.extendColors.gray8
        TagType.ERROR -> MaterialTheme.extendColors.red8
        TagType.WARNING -> MaterialTheme.extendColors.orange8
        TagType.INFO -> MaterialTheme.extendColors.blue8
        else -> MaterialTheme.extendColors.gray8
    }
    ProvideTextStyle(MaterialTheme.typography.labelSmall.copy(color = textColor)) {
        Row(
            modifier = modifier
                .clip(RoundedCornerShape(2.dp))
                .background(background)
                .padding(horizontal = 4.dp, vertical = 1.dp)
        ) {
            children()
        }
    }
}

@Composable
@Preview(showBackground = true)
private fun TagPreview() {
    Column(
        modifier = Modifier.padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Tag(type = TagType.SUCCESS) {
            Text("测试")
        }
        Tag(type = TagType.ERROR) {
            Text("测试")
        }
        Tag(type = TagType.WARNING) {
            Text("测试")
        }
        Tag(type = TagType.INFO) {
            Text("测试")
        }
        Tag(type = TagType.DEFAULT) {
            Text("测试")
        }
    }
}