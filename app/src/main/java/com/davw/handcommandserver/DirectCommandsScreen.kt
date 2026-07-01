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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import org.json.JSONException
import java.io.File
import java.io.IOException

/*
 * This file contains the UI for the Direct Commands screen, which allows users to manage a list of direct commands.
 * Users can add, edit, and delete commands, as well as import/export the command list as JSON.
 */

private const val DIRECT_COMMANDS_FILE_NAME = "DirectCommands.json"
private const val DEFAULT_COMMANDS_FILE_NAME = "DefCommands.json"

//@Preview
@Composable
fun DirectCommandsScreen(
    viewModel: BleViewModel,
    onPrevScreen: () -> Unit
) {
    val context = LocalContext.current
    val directCommands = viewModel.directCommands
    val commandActionFields = remember {
        mutableStateListOf(*directCommands.map { TextFieldValue(it.action) }.toTypedArray())
    }
    var selectedActionIndex by remember { mutableStateOf<Int?>(null) }
    val importJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { selectedUri ->
        if (selectedUri == null) {
            showMessage(context, "Import canceled")
        } else {
            try {
                val loaded = readCommandsFromUri(context, selectedUri)
                applyLoadedCommands(directCommands, loaded)
                applyLoadedTextFieldValues(commandActionFields, loaded)
                saveCommandsToFile(context, directCommands)
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
            itemsIndexed(directCommands) { index, command ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val isSelected = selectedActionIndex == index
                    LaunchedEffect(isSelected) {
                        if (isSelected) {
                            val text = commandActionFields[index].text
                            commandActionFields[index] = commandActionFields[index].copy(
                                selection = TextRange(0, text.length)
                            )
                        }
                    }

                    Button(
                        onClick = {
                            viewModel.sendDirectCommand(index + 1)
                        }
                    ) {
                        Text(command.name)
                    }

                    OutlinedTextField(
                        value = commandActionFields[index],
                        onValueChange = {
                            commandActionFields[index] = it
                            viewModel.updateCommandAction(index, it.text)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .onFocusChanged { focusState ->
                                if (focusState.isFocused) {
                                    selectedActionIndex = index
                                } else if (!focusState.isFocused && selectedActionIndex == index) {
                                    selectedActionIndex = null
                                }
                            },
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
                        saveCommandsToFile(context, directCommands)
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
                        applyLoadedCommands(directCommands, loaded)
                        applyLoadedTextFieldValues(commandActionFields, loaded)
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
                        applyLoadedCommands(directCommands, loaded)
                        applyLoadedTextFieldValues(commandActionFields, loaded)
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
                        saveCommandsToFile(context, directCommands)
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

private fun saveCommandsToFile(context: Context, commands: List<DirectCommandConfig>) {
    context.openFileOutput(DIRECT_COMMANDS_FILE_NAME, Context.MODE_PRIVATE).use { stream ->
        stream.write(directCommandsToJson(commands).toByteArray(Charsets.UTF_8))
    }
}

private fun readCommandsFromInternalFile(context: Context): List<DirectCommandConfig> {
    val file = File(context.filesDir, DIRECT_COMMANDS_FILE_NAME)
    if (!file.exists()) {
        throw IOException("$DIRECT_COMMANDS_FILE_NAME was not found")
    }
    val jsonText = file.readText(Charsets.UTF_8)
    return parseDirectCommands(jsonText)
}

private fun readCommandsFromAssets(context: Context): List<DirectCommandConfig> {
    val jsonText = context.assets.open(DEFAULT_COMMANDS_FILE_NAME).bufferedReader(Charsets.UTF_8).use {
        it.readText()
    }
    return parseDirectCommands(jsonText)
}

private fun readCommandsFromUri(context: Context, uri: Uri): List<DirectCommandConfig> {
    val jsonText = context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8).use {
        it?.readText() ?: throw IOException("Could not open selected file")
    }
    return parseDirectCommands(jsonText)
}

private fun applyLoadedCommands(
    current: MutableList<DirectCommandConfig>,
    loaded: List<DirectCommandConfig>
) {
    if (loaded.size != current.size) {
        throw IllegalArgumentException("Expected ${current.size} commands, found ${loaded.size}")
    }
    loaded.forEachIndexed { index, value ->
        current[index] = value
    }
}

private fun applyLoadedTextFieldValues(
    current: MutableList<TextFieldValue>,
    loaded: List<DirectCommandConfig>
) {
    if (loaded.size != current.size) {
        throw IllegalArgumentException("Expected ${current.size} commands, found ${loaded.size}")
    }
    loaded.forEachIndexed { index, value ->
        current[index] = TextFieldValue(value.action)
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
