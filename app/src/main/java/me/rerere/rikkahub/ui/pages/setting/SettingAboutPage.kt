package me.rerere.rikkahub.ui.pages.setting

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.composables.icons.lucide.Code
import com.composables.icons.lucide.Github
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Phone
import me.rerere.rikkahub.BuildConfig
import me.rerere.rikkahub.R
import me.rerere.rikkahub.ui.components.nav.BackButton
import me.rerere.rikkahub.ui.context.LocalNavController
import me.rerere.rikkahub.utils.plus
import me.rerere.rikkahub.utils.openUrl

@Composable
fun SettingAboutPage() {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val context = LocalContext.current
    val navController = LocalNavController.current
    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = {
                    Text("关于")
                },
                navigationIcon = {
                    BackButton()
                },
                scrollBehavior = scrollBehavior,
            )
        },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = it + PaddingValues(8.dp)
        ) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AsyncImage(
                        model = R.mipmap.ic_launcher,
                        contentDescription = "Logo",
                        modifier = Modifier
                            .size(150.dp)
                    )

                    Text(
                        text = "RikkaHub",
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            }

            item {
                ListItem(
                    headlineContent = {
                        Text("版本")
                    },
                    supportingContent = {
                        Text(
                            text = "${BuildConfig.VERSION_NAME} / ${BuildConfig.VERSION_CODE}",
                        )
                    },
                    leadingContent = {
                        Icon(Lucide.Code, null)
                    },
                    modifier = Modifier.combinedClickable(
                        onClick = {},
                        onLongClick = {
                            navController.navigate("debug")
                        },
                        interactionSource = remember { MutableInteractionSource() },
                        indication = LocalIndication.current,
                    )
                )
            }

            item {
                ListItem(
                    headlineContent = {
                        Text("系统")
                    },
                    supportingContent = {
                        Text(
                            text = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL} / Android ${android.os.Build.VERSION.RELEASE} / SDK ${android.os.Build.VERSION.SDK_INT}",
                        )
                    },
                    leadingContent = {
                        Icon(Lucide.Phone, null)
                    }
                )
            }

            item {
                ListItem(
                    headlineContent = {
                        Text("Github")
                    },
                    supportingContent = {
                        Text(
                            text = "https://github.com/re-ovo/rikkahub"
                        )
                    },
                    modifier = Modifier.clickable {
                        context.openUrl("https://github.com/re-ovo/rikkahub")
                    },
                    leadingContent = {
                        Icon(Lucide.Github, null)
                    }
                )
            }
        }
    }
}