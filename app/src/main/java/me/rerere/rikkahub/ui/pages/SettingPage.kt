package me.rerere.rikkahub.ui.pages

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import me.rerere.rikkahub.ui.components.BackButton
import me.rerere.rikkahub.ui.hooks.heroAnimation

@Composable
fun SettingPage(modifier: Modifier = Modifier) {
    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(text = "设置")
                },
                navigationIcon = {
                    BackButton()
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .padding(innerPadding)
                .padding(4.dp)
        ) {
            Card(
                modifier = Modifier
                    .heroAnimation("setting_card"),

                ) {
                Box(
                    modifier = Modifier
                        .padding(8.dp)
                        .fillMaxWidth()
                        .heroAnimation(100.dp)
                ) {
                    Text("设置")
                }
            }
        }
    }
}