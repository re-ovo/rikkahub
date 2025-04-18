package me.rerere.rikkahub.ui.pages.setting

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.MessageCircle
import com.composables.icons.lucide.NotebookTabs
import me.rerere.ai.provider.ModelType
import me.rerere.rikkahub.ui.components.chat.ModelSelector
import me.rerere.rikkahub.ui.components.nav.BackButton
import org.koin.androidx.compose.koinViewModel

@Composable
fun SettingModelPage(vm: SettingVM = koinViewModel()) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("模型设置")
                },
                navigationIcon = {
                    BackButton()
                }
            )
        }
    ) { contentPadding ->
        Column(
            modifier = Modifier.padding(contentPadding)
        ) {
            ListItem(
                headlineContent = {
                    Text("聊天模型", maxLines = 1)
                },
                leadingContent = {
                    Icon(Lucide.MessageCircle, null)
                },
                trailingContent = {
                    ModelSelector(
                        modelId = settings.chatModelId,
                        type = ModelType.CHAT,
                        onSelect = {
                            vm.updateSettings(settings.copy(
                                chatModelId = it.id
                            ))
                        },
                        providers = settings.providers,
                        modifier = Modifier.widthIn(max = 200.dp)
                    )
                }
            )
            ListItem(
                headlineContent = {
                    Text("标题总结模型", maxLines = 1)
                },
                leadingContent = {
                    Icon(Lucide.NotebookTabs, null)
                },
                trailingContent = {
                    ModelSelector(
                        modelId = settings.titleModelId,
                        type = ModelType.CHAT,
                        onSelect = {
                            vm.updateSettings(settings.copy(
                                titleModelId = it.id
                            ))
                        },
                        providers = settings.providers,
                        modifier = Modifier.widthIn(max = 200.dp)
                    )
                }
            )
        }
    }
}