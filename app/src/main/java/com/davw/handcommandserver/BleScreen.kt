@file:Suppress("unused", "RedundantQualifierName")

package com.davw.handcommandserver

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import android.os.SystemClock
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.setValue

// const val positionDelta : Float= 5.0f
var numPerSecond = 1L
var minSendIntervalMs = 1000L / numPerSecond  // 1L // 6 updates per second max
val verHeader = "Hand Commander v0.0.2"

enum class CommandToHand(val value: Int) {
    STOP(0),
    FORWARD(1),
    BACKWARD(2),
    CLOCKWISE(3),
    COUNTER_CLOCKWISE(4),
    STEP(5),
    POSITION(6),
    CALIBRATION(7),
    VELOCITY(8),
    SUBSCRIBE(9),
    EMERGENCY_STOP(10),
    DISCONNECT(11);

    companion object {
        fun fromValue(value: Int): CommandToHand? = entries.find { it.value == value }
    }
}


/////////////////////////////////////////////////////////////////////////////
@Composable
fun MainButsCommands(viewModel: BleViewModel) {
    //val events by viewModel.events.collectAsState()
    Spacer(modifier = Modifier
        .fillMaxWidth()
        .height(2.dp)
        .background(Color.White))

    val numbers = (1..6).toList()

    val numCmd = intArrayOf(
        CommandToHand.CALIBRATION.value,
        CommandToHand.STEP.value,
        CommandToHand.FORWARD.value,
        CommandToHand.BACKWARD.value,
        CommandToHand.DISCONNECT.value,
        CommandToHand.STOP.value)

    val buttonsText = arrayOf(
        // "Calibrate", "Subscribe", "Forward", "Backward",  "Step", "Stop")
        // "Calibrate", "Step", "Forward", "Backward",  "Subscribe", "Stop")
        "Calibrate", "Step", "Forward", "Backward",  "Disconnect", "Stop")

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 2.dp, bottom = 2.dp, start = 10.dp, end = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(numbers) { num ->
            Button(
                onClick = { viewModel.sendNotification(numCmd[num - 1]) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                val txt: String = buttonsText[num - 1]
                Text(txt)
            }
        }
    }

//    Spacer(modifier = Modifier
//        .fillMaxWidth()
//        .height(2.dp)
//        .background(Color.Blue))
}
////////////////////////////////////////////////////////////////
@Composable
fun TrackedSlide(
    id: Int,
    value: Float,
    touchedAt: (Float) -> Unit,
    newValue: (Float) -> Unit,
    releasedAt: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val latestValue by rememberUpdatedState(value)
    val latestTouchedAt by rememberUpdatedState(touchedAt)
    val latestNewValue by rememberUpdatedState(newValue)
    val latestReleasedAt by rememberUpdatedState(releasedAt)

    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> {
                    println("[$id] touch the slider at $latestValue")
                    latestTouchedAt(latestValue)
                }

                is PressInteraction.Release, is PressInteraction.Cancel -> {
                    println("[$id] release the slider at $latestValue")
                    latestReleasedAt(latestValue)
                }
            }
        }
    }

    Slider(
        value = value,
        valueRange = 0f..100f,
        onValueChange = { latestNewValue(it) },
        interactionSource = interactionSource,
        modifier = modifier
    )
}

////////////////////////////////////////////////////////////////////////////
@Composable
fun CustomActionSliderRowPosMulti(
    id: Int,
    value: Float,
    startVal: (Float) -> Unit,
    onValueChanged: (Float) -> Unit,
    sliderEnded: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    // Local state to keep track of the slider's visual position
    var sliderValue by remember { mutableFloatStateOf(50f) }
    var lastSentAtMs by remember { mutableLongStateOf(0L) }
    //val minSendIntervalMs = 1000L / 1L // 6 updates per second max

    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Displays the changing value in real-time while moving
        Text(
            text = "P: ${sliderValue.toInt()}",
            modifier = Modifier.weight(0.2f)
        )

        Slider(
            value = sliderValue,
            valueRange = 0f..100f,
            onValueChange = { newValue ->
                sliderValue = newValue
                val nowMs = SystemClock.elapsedRealtime()
                if (nowMs - lastSentAtMs >= minSendIntervalMs) {
                    onValueChanged(newValue)
                    lastSentAtMs = nowMs
                }
            },
            onValueChangeFinished = {
                // Triggered when the user leaves/releases the slider
                sliderEnded(sliderValue)
                println("Slider released at: $sliderValue")
            },
            modifier = Modifier
                .weight(0.7f)
                .pointerInput(Unit) {
                    // Detect the initial down press/click action
                    detectTapGestures(
                        onPress = {
                            // This runs immediately when the user touches the slider
                            startVal(sliderValue)
                            println("Slider touched at: $sliderValue")
                        }
                    )
                }
        )
    }
}

@Composable
fun MotorControlScreenRow(viewModel: BleViewModel) {
    // Local state for 4 sliders (Initial value 50f each)
    // Note: If your ViewModel already tracks these values, use those instead!
    val sliderValues = remember { mutableStateListOf(50f, 50f, 50f, 50f, 50f) }

    Column(
        modifier = Modifier.padding(horizontal = 8.dp),
    ) {
        sliderValues.forEachIndexed { index, currentValue ->
            Row (
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                //,
                //verticalAlignment = Alignment.CenterVertically // 1. Vertically centers text and slider
            ){
                if (index == 0) {
                    //println("Place for Slider 0")
                } else {
//////////////////////////////////////////////////////////////////////////////////
                    CustomActionSliderRowPosMulti(
                        id = index,
                        value = currentValue,
                        startVal = { startValue ->
                            // Handle the start of the slider interaction
                            println("Slider started at: $startValue")
                        },
                        onValueChanged = { changedValue ->
                            sliderValues[index] = changedValue
                            viewModel.sendMotorPosition(
                                CommandToHand.POSITION.value,
                                index,
                                value = changedValue,
                            )
                        },
                        sliderEnded = { endValue ->
                            // Handle the end of the slider interaction
                            sliderValues[index] = endValue
                            viewModel.sendMotorPosition(
                                CommandToHand.POSITION.value,
                                index,
                                value = endValue,
                                )
                            println("Slider ended at: $endValue")
                        }
                    )

                }
            }
        }
    }
}
////////////////////////////////////////////////////////////////////////////
@Composable
fun CustomActionSliderRowVel(
    startVal: (Float) -> Unit,
    sliderEnded: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    // Local state to keep track of the slider's visual position
    var sliderValue by remember { mutableFloatStateOf(50f) }

    Row(
        modifier = modifier.fillMaxWidth().padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Displays the changing value in real-time while moving
        Text(
            text = "V: ${sliderValue.toInt()}",
            modifier = Modifier.weight(0.1f)
        )

        Slider(
            value = sliderValue,
            valueRange = 0f..100f,
            onValueChange = { newValue ->
                // Updates continuously while moving, displaying the changed value
                sliderValue = newValue
            },
            onValueChangeFinished = {
                // Triggered when the user leaves/releases the slider
                sliderEnded(sliderValue)
                println("Slider released at: $sliderValue")
            },
            modifier = Modifier
                .weight(0.8f)
                .pointerInput(Unit) {
                    // Detect the initial down press/click action
                    detectTapGestures(
                        onPress = {
                            // This runs immediately when the user touches the slider
                            startVal(sliderValue)
                            println("Slider touched at: $sliderValue")
                        }
                    )
                }
        )
    }
}
////////////////////////////////////////////////////////////////////////////
@Composable
fun BleScreen(
    viewModel: BleViewModel,
    onNextScreen: () -> Unit
) {
    val events by viewModel.events.collectAsState()
    val isConnected by viewModel.isConnected.collectAsState()
    val connectColor = if (isConnected) Color.Green else Color.Red
    //val currentStatus by viewModel.statusText.collectAsState()
    val context = LocalContext.current
    Column(
            //.padding(16.dp)
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
    )
    {
        // Headers
        Text(
            verHeader,
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        // Main Buttons
        MainButsCommands(viewModel)
        Spacer(modifier = Modifier
            .fillMaxWidth()
            .height(10.dp)
            .background(Color.White)
        )
        Spacer(modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .background(connectColor)
        )

        // Velocity Slider
        CustomActionSliderRowVel(
            startVal = { startValue ->
                // Handle the start of the slider interaction
                println("Slider started at: $startValue")
            },
            sliderEnded = { endValue ->
                // Handle the end of the slider interaction
                println("Slider ended at: $endValue")
                viewModel.sendMotorsVelocity(
                    CommandToHand.VELOCITY.value,
                    0,
                    endValue
                )
            }
        )


        // Motor Control Sliders
        MotorControlScreenRow(viewModel)

        // Special Commands Buttons
        Spacer(modifier = Modifier
            .fillMaxWidth()
            .height(2.dp)
            .background(Color.White))

        val numbers = (1..5).toList()

       // val numCmd = intArrayOf(21, 22, 23, 24, 25, 26, 27, 28)
        val buttonsText = arrayOf(
            "1", "2", "3", "4",
            "5" //, "6", "7", "8"
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(5),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp, bottom = 2.dp, start = 10.dp, end = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(numbers) { num ->
                Button(
                    {specialCommandDo(num, viewModel)},
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    val txt: String = buttonsText[num - 1]
                    Text(txt)
                }
            }
        }

        // ListBox Log
        Spacer(modifier = Modifier
            .fillMaxWidth()
            .height(2.dp)
            .background(Color.White))
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(top = 2.dp, bottom = 2.dp, start = 10.dp, end = 10.dp)
        ) {
            items(events.reversed()) { msg ->
                Text(msg)
            }
        }

        // Navigation Buttons
        Spacer(modifier = Modifier
            .fillMaxWidth()
            .height(2.dp)
            .background(Color.White))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp), //2.dp, bottom = 2.dp, start = 10.dp, end = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween //spacedBy(2.dp)
        ) {
            Button(
                onClick = { },
                enabled = false,
            ) {
                Text("Prev")
            }
            Button(onClick = {
                viewModel.sendDisconnectNotification()
                //viewModel.disconnectAndCloseServer()
                // Cast context to Activity and call finishAndRemoveTask
                (context as? Activity)?.finishAndRemoveTask()
            }) {
                Text(text = "Exit App")
            }
            Button(
                onClick = onNextScreen,
            ) {
                Text("Next")
            }
        }
        Spacer(modifier = Modifier
            .fillMaxWidth()
            .height(2.dp)
            .background(Color.White))
    }
}

fun specialCommandDo(id: Int, viewModel: BleViewModel) {
    when (id) {
        1 -> numPerSecond = 1L
        2 -> numPerSecond = 2L
        3 -> numPerSecond = 3L
        4 -> numPerSecond = 4L
        5 -> viewModel.sendNotificationSpecialCommand(25)
//        6 -> viewModel.sendNotificationSpecialCommand(26)
//        7 -> viewModel.sendNotificationSpecialCommand(27)
//        8 -> viewModel.sendNotificationSpecialCommand(28)

    }
}
//================================================================================
