package com.davw.handcommandserver

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun DirectCommandsScreen(
    viewModel: BleViewModel,
    onPrevScreen: () -> Unit
) {
    val commandItems = listOf(
        "Cmd 1" to "Description for command 1",
        "Cmd 2" to "Description for command 2",
        "Cmd 3" to "Description for command 3",
        "Cmd 4x" to "Description for command 4",
        "Cmd 5" to "Description for command 5",
        "Cmd 6" to "Description for command 6",
        "Cmd 7" to "Description for command 7",
        "Cmd 8" to "Description for command 8"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Direct Commands",
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(top = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(commandItems) { item ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            // replace with your real command call
                            // example:
                            // viewModel.sendIntCommandPos(...)
                        }
                    ) {
                        Text(item.first)
                    }

                    Text(
                        text = item.second,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(onClick = onPrevScreen) {
                Text("Prev")
            }

            Button(
                onClick = { },
                enabled = false
            ) {
                Text("Next")
            }
        }
    }
}