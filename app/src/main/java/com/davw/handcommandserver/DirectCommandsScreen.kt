package com.davw.handcommandserver

import android.content.Context
import android.content.Intent
import android.content.ActivityNotFoundException
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.IOException

private const val DIRECT_COMMANDS_FILE_NAME = "DirectCommansds.json"
private const val DEFAULT_COMMANDS_FILE_NAME = "DefCommands.json"

@Composable
fun DirectCommandsScreen(
    viewModel: BleViewModel,
    onPrevScreen: () -> Unit
) {
    val context = LocalContext.current
    val commandLabels = listOf(
        "Cmd 1",
        "Cmd 2",
        "Cmd 3",
        "Cmd 4x",
        "Cmd 5",
        "Cmd 6",
        "Cmd 7",
        "Cmd 8"
    )
    val commandDescriptions = remember {
        val initialDescriptions = try {
            loadDefaultDescriptionsForStart(context, commandLabels.size)
        } catch (_: IOException) {
            fallbackDescriptions(commandLabels.size)
        } catch (_: JSONException) {
            fallbackDescriptions(commandLabels.size)
        } catch (_: IllegalArgumentException) {
            fallbackDescriptions(commandLabels.size)
        }
        mutableStateListOf(*initialDescriptions.toTypedArray())
    }
    val importJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { selectedUri ->
        if (selectedUri == null) {
            showMessage(context, "Import canceled")
        } else {
            try {
                val loaded = readCommandsFromUri(context, selectedUri)
                applyLoadedDescriptions(commandDescriptions, loaded)
                saveCommandsToFile(context, commandDescriptions)
                showMessage(context, "Imported and saved to $DIRECT_COMMANDS_FILE_NAME")
            } catch (e: IOException) {
                showMessage(context, "Import failed: ${e.message ?: "I/O error"}")
            } catch (e: JSONException) {
                showMessage(context, "Import failed: invalid JSON")
            } catch (e: IllegalArgumentException) {
                showMessage(context, "Import failed: ${e.message}")
            }
        }
    }
    val importJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { selectedUri ->
        if (selectedUri == null) {
            showMessage(context, "Import canceled")
        } else {
            try {
                val loaded = readCommandsFromUri(context, selectedUri)
                applyLoadedDescriptions(commandDescriptions, loaded)
                saveCommandsToFile(context, commandDescriptions)
                showMessage(context, "Imported and saved to $DIRECT_COMMANDS_FILE_NAME")
            } catch (e: IOException) {
                showMessage(context, "Import failed: ${e.message ?: "I/O error"}")
            } catch (e: JSONException) {
                showMessage(context, "Import failed: invalid JSON")
            } catch (e: IllegalArgumentException) {
                showMessage(context, "Import failed: ${e.message}")
            }
        }
    }

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
            itemsIndexed(commandLabels) { index, label ->
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
                        Text(label)
                    }

                    OutlinedTextField(
                        value = commandDescriptions[index],
                        onValueChange = { commandDescriptions[index] = it },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                modifier = Modifier.weight(1f),
                onClick = {
                    try {
                        saveCommandsToFile(context, commandDescriptions)
                        showMessage(context, "Saved to $DIRECT_COMMANDS_FILE_NAME")
                    } catch (e: IOException) {
                        showMessage(context, "Save failed: ${e.message ?: "I/O error"}")
                    } catch (e: JSONException) {
                        showMessage(context, "Save failed: invalid JSON")
                    }
                }
            ) {
                Text("Save Json")
            }

            Button(
                modifier = Modifier.weight(1f),
                onClick = {
                    try {
                        val loaded = readCommandsFromInternalFile(context)
                        applyLoadedDescriptions(commandDescriptions, loaded)
                        showMessage(context, "Loaded from $DIRECT_COMMANDS_FILE_NAME")
                    } catch (e: IOException) {
                        showMessage(context, "Load failed: ${e.message ?: "I/O error"}")
                    } catch (e: JSONException) {
                        showMessage(context, "Load failed: invalid JSON")
                    } catch (e: IllegalArgumentException) {
                        showMessage(context, "Load failed: ${e.message}")
                    }
                }
            ) {
                Text("Load Json")
            }

            Button(
                modifier = Modifier.weight(1f),
                onClick = {
                    try {
                        val loaded = readCommandsFromAssets(context)
                        applyLoadedDescriptions(commandDescriptions, loaded)
                        showMessage(context, "Defaults loaded")
                    } catch (e: IOException) {
                        showMessage(context, "Defaults failed: ${e.message ?: "I/O error"}")
                    } catch (e: JSONException) {
                        showMessage(context, "Defaults failed: invalid JSON")
                    } catch (e: IllegalArgumentException) {
                        showMessage(context, "Defaults failed: ${e.message}")
                    }
                }
            ) {
                Text("Defaults")
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                modifier = Modifier.weight(1f),
                onClick = {
                    try {
                        saveCommandsToFile(context, commandDescriptions)
                        shareCommandsJson(context)
                    } catch (e: IOException) {
                        showMessage(context, "Share failed: ${e.message ?: "I/O error"}")
                    } catch (e: JSONException) {
                        showMessage(context, "Share failed: invalid JSON")
                    } catch (e: IllegalStateException) {
                        showMessage(context, "Share failed: ${e.message}")
                    }
                }
            ) {
                Text("Share")
            }

            Button(
                modifier = Modifier.weight(1f),
                onClick = {
                    importJsonLauncher.launch(arrayOf("application/json", "text/plain"))
                }
            ) {
                Text("Import")
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

private fun saveCommandsToFile(context: Context, descriptions: List<String>) {
    val root = JSONObject()
    val jsonDescriptions = JSONArray()
    descriptions.forEach { jsonDescriptions.put(it) }
    root.put("descriptions", jsonDescriptions)
    context.openFileOutput(DIRECT_COMMANDS_FILE_NAME, Context.MODE_PRIVATE).use { stream ->
        stream.write(root.toString().toByteArray(Charsets.UTF_8))
    }
}

private fun readCommandsFromInternalFile(context: Context): List<String> {
    val file = File(context.filesDir, DIRECT_COMMANDS_FILE_NAME)
    if (!file.exists()) {
        throw IOException("$DIRECT_COMMANDS_FILE_NAME was not found")
    }
    val jsonText = file.readText(Charsets.UTF_8)
    return parseDescriptions(jsonText)
}

private fun readCommandsFromAssets(context: Context): List<String> {
    val jsonText = context.assets.open(DEFAULT_COMMANDS_FILE_NAME).bufferedReader(Charsets.UTF_8).use {
        it.readText()
    }
    return parseDescriptions(jsonText)
}

private fun readCommandsFromUri(context: Context, uri: Uri): List<String> {
    val jsonText = context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8).use {
        it?.readText() ?: throw IOException("Could not open selected file")
    }
    return parseDescriptions(jsonText)
}

private fun parseDescriptions(jsonText: String): List<String> {
    val root = JSONObject(jsonText)
    val jsonDescriptions = root.getJSONArray("descriptions")
    val descriptions = mutableListOf<String>()
    for (index in 0 until jsonDescriptions.length()) {
        descriptions.add(jsonDescriptions.getString(index))
    }
    return descriptions
}

private fun loadDefaultDescriptionsForStart(context: Context, expectedCount: Int): List<String> {
    val loadedDescriptions = readCommandsFromAssets(context)
    if (loadedDescriptions.size != expectedCount) {
        throw IllegalArgumentException("Expected $expectedCount descriptions, found ${loadedDescriptions.size}")
    }
    return loadedDescriptions
}

private fun fallbackDescriptions(size: Int): List<String> {
    return List(size) { index -> "Description for command ${index + 1}" }
}

private fun applyLoadedDescriptions(current: MutableList<String>, loaded: List<String>) {
    if (loaded.size != current.size) {
        throw IllegalArgumentException("Expected ${current.size} descriptions, found ${loaded.size}")
    }
    loaded.forEachIndexed { index, value ->
        current[index] = value
    }
}

private fun showMessage(context: Context, message: String) {
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
}

private fun shareCommandsJson(context: Context) {
    val commandsFile = File(context.filesDir, DIRECT_COMMANDS_FILE_NAME)
    if (!commandsFile.exists()) {
        throw IOException("$DIRECT_COMMANDS_FILE_NAME was not found")
    }
    val fileUri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        commandsFile
    )
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "application/json"
        putExtra(Intent.EXTRA_STREAM, fileUri)
        putExtra(Intent.EXTRA_SUBJECT, DIRECT_COMMANDS_FILE_NAME)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    val chooserIntent = Intent.createChooser(shareIntent, "Share commands JSON").apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    try {
        context.startActivity(chooserIntent)
    } catch (_: ActivityNotFoundException) {
        throw IllegalStateException("No app available to share files")
    }
}
