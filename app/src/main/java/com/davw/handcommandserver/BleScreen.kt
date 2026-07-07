@file:Suppress("unused", "RedundantQualifierName")

package com.davw.handcommandserver

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import androidx.compose.runtime.setValue

const val positionDelta : Float= 5.0f

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
    SUBSCRIBE(9);

    companion object {
        fun fromValue(value: Int): CommandToHand? = entries.find { it.value == value }
    }
}


@Composable
fun CustomStyledButtonRound(
    text: String,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        contentPadding = PaddingValues(
            top = 5.dp,
            bottom = 5.dp,
            start = 10.dp,
            end = 10.dp,
        )
    ) {
        Text(
            text = text,
            textAlign = TextAlign.Center,
            style = LocalTextStyle.current.copy(
                lineHeight = 24.sp,
            )
        )
    }
}

@Composable
fun CustomStyledButtonRound2(
    text: String,
    onClick: () -> Unit,
    hPad: Dp = 1.dp,
    wPad: Dp = 2.dp
) {
    Button(
        onClick = onClick,
        contentPadding = PaddingValues(
            top = hPad,
            bottom = hPad,
            start = wPad,
            end = wPad
        )
    ) {
        Text(
            text = text,
            textAlign = TextAlign.Center,
            style = LocalTextStyle.current.copy(
                lineHeight = 24.sp
            )
        )
    }
}

/////////////////////////////////////////////////////////////////////////////
@Composable
fun MainButsCommands(viewModel: BleViewModel) {
    //val events by viewModel.events.collectAsState()
    Spacer(modifier = Modifier
        .fillMaxWidth()
        .height(10.dp)
        .background(Color.White))

    val numbers = (1..6).toList()

//    val numCmd = intArrayOf(31, 32, 33, 34, 35, 36)
    val numCmd = intArrayOf(CommandToHand.CALIBRATION.value,
        CommandToHand.SUBSCRIBE.value,
        CommandToHand.FORWARD.value,
        CommandToHand.BACKWARD.value,
        CommandToHand.STEP.value,
        CommandToHand.STOP.value)

    val buttonsText = arrayOf(
        "Calibrate", "Subscribe", "Forward", "Backward",  "Step", "Stop")

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
    Spacer(modifier = Modifier
        .fillMaxWidth()
        .height(10.dp)
        .background(Color.White))

    Spacer(modifier = Modifier
        .fillMaxWidth()
        .height(2.dp)
        .background(Color.Blue))
}
////////////////////////////////////////////////////////////////////////////
//@Composable
//fun TrackedSlider0(
//    id: Int,
//    value: Float,
//    onValueChange: (Float) -> Unit,
//    onDragStatusChange: (Boolean, Float) -> Unit,
//    modifier: Modifier = Modifier
//) {
//    var sliderValue by remember { mutableStateOf(0.5f) }
//    val interactionSource = remember { MutableInteractionSource() }
//
//    // Listen directly to raw touch/drag events
//    LaunchedEffect(interactionSource) {
//        interactionSource.interactions.collect { interaction ->
//            when (interaction) {
//                is DragInteraction.Start -> {
//                    // Triggers exactly when the finger hits the slider thumb
//                    println("Touch event: Slider pressed")
//                }
//                is DragInteraction.Stop, is DragInteraction.Cancel -> {
//                    // Triggers exactly when the finger leaves the screen
//                    println("Touch event: Slider released")
//                }
//            }
//        }
//    }
//
//    Slider(
//        value = sliderValue,
//        onValueChange = { sliderValue = it },
//        interactionSource = interactionSource
//    )
//}
////////////////////////////////////////////////////////////////
@Composable
fun TrackedSlideVel(
    id: Int,
    value: Float,
//    touchedAt: (Float) -> Unit,
//    newValue: (Float) -> Unit,
    releasedAt: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val latestValue by rememberUpdatedState(value)
//    val latestTouchedAt by rememberUpdatedState(touchedAt)
//    val latestNewValue by rememberUpdatedState(newValue)
    val latestReleasedAt by rememberUpdatedState(releasedAt)

    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> {
                    println("[$id] touch the slider at $latestValue")
                    latestReleasedAt(latestValue)
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
        onValueChange = { latestReleasedAt(it)
            println("[$id] slider velocity changed to $it")
                        },

        interactionSource = interactionSource,
        modifier = modifier
    )
}
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

@Composable
fun TrackedSliderVelocity(
    id: Int,
    value: Float,
    onValueChange: (Float) -> Unit,
    onDragStatusChange: (Boolean, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }

    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> {
                    println("[$id] touch the slider")
                    onDragStatusChange(true, value)
                }
//                is DragInteraction.Start -> {
//                    println("[$id] drag the slider")
//                    onDragStatusChange(true, value)
//                }
                is PressInteraction.Release, is PressInteraction.Cancel -> {
                    println("[$id] release the slider")
                    onDragStatusChange(true, value)
                }
                is DragInteraction.Stop,
                is DragInteraction.Cancel -> {
                    println("[$id] release the slider")
                    onDragStatusChange(true, value)
                }
            }
        }
    }

    Slider(
        value = value,
        valueRange = 0f..100f,
        onValueChange = { newValue ->
            // Keep your threshold logic if needed
            if (abs(value - newValue) >= positionDelta) {
                onValueChange(newValue)
            }
        },
        interactionSource = interactionSource,
        modifier = modifier
    )
}

@Composable
fun TrackedSlider1(
    id: Int,
    value: Float,
    onValueChange: (Float) -> Unit,
    onDragStatusChange: (Boolean, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isDragging by interactionSource.collectIsDraggedAsState()

    // Track drag start/stop events
    LaunchedEffect(isDragging) {
        onDragStatusChange(isDragging, value)
    }

    Slider(
        value = value,
        valueRange = 0f..100f,
        onValueChange = { newValue ->
            if (abs(value - newValue) >= positionDelta) {
                onValueChange(newValue)
            }
        },
        interactionSource = interactionSource
    )
}
@Composable
fun MotorControlScreenRow(viewModel: BleViewModel) {
    // Local state for 4 sliders (Initial value 50f each)
    // Note: If your ViewModel already tracks these values, use those instead!
    val sliderValues = remember { mutableStateListOf(50f, 50f, 50f, 50f, 50f) }

    Column(
        modifier = Modifier.padding(8.dp),
    ) {
        sliderValues.forEachIndexed { index, currentValue ->
            Row (
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically // 1. Vertically centers text and slider
            ){
                if (index == 0) {
                    println("Place for Slider 0")
                } else {

                    Text(
                        text = "M${index }: ${currentValue.toInt()}",
                    )

                    TrackedSlide(
                        id = index,
                        value = currentValue,
                        modifier = Modifier.weight(1f),
                        touchedAt = { touchedValue ->
                            println("Motor $index started dragging at: $touchedValue")
                        },
                        newValue = { newValue ->
                            sliderValues[index] = newValue
                            // Sends the specific motor index (0, 1, 2, or 3) to your BLE device
                            viewModel.sendMotorPosition(
                                CommandToHand.POSITION.value,
                                index,
                                value = newValue
                            )
                            println("Motor $index changed to: $newValue")
                        },
                        releasedAt = { releasedValue ->
                            println("Motor $index released at: $releasedValue")
                        }
                    )
                }
            }
        }
    }
}
////////////////////////////////////////////////////////////////////////////
@Composable
fun ExitButtonApp() {
    // Get the current context
    val context = LocalContext.current

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(onClick = {
            // Cast context to Activity and call finishAndRemoveTask
            (context as? Activity)?.finishAndRemoveTask()
        }) {
            Text(text = "Exit App")
        }
    }
}
////////////////////////////////////////////////////////////////////////////
@Composable
fun CustomActionSliderCOL(
    startVal: (Float) -> Unit,
    sliderEnded: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    // Local state to keep track of the slider's visual position
    var sliderValue by remember { mutableFloatStateOf(0f) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Displays the changing value in real-time while moving
        Text(
            text = "Value: ${"%.1f".format(sliderValue)}",
            fontSize = 24.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

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
            },
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(Unit) {
                    // Detect the initial down press/click action
                    detectTapGestures(
                        onPress = {
                            // This runs immediately when the user touches the slider
                            startVal(sliderValue)
                        }
                    )
                }
        )
    }
}
////////////////////////////////////////////////////////////////////////////
@Composable
fun CustomActionSliderRow(
    startVal: (Float) -> Unit,
    sliderEnded: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    // Local state to keep track of the slider's visual position
    var sliderValue by remember { mutableFloatStateOf(50f) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp),
       // horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Displays the changing value in real-time while moving
        Text(
            text = "V: ${sliderValue.toInt()}",
//            modifier = Modifier.width(65.dp)
            //fontSize = 16.sp
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
                //.fillMaxWidth()
                //.weight(1f)
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
    //val currentStatus by viewModel.statusText.collectAsState()
    val context = LocalContext.current
    Column(
            //.padding(16.dp)
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
    )
    {
        Text(
            "Android Hand Commander",
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        MainButsCommands(viewModel)
        Spacer(modifier = Modifier
            .fillMaxWidth()
            .height(10.dp)
            .background(Color.White))

        CustomActionSliderRow(
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
        //TrackedSlider(viewModel)
        MotorControlScreenRow(viewModel)


        Spacer(modifier = Modifier
            .fillMaxWidth()
            .height(10.dp)
            .background(Color.White))

        val numbers = (1..8).toList()

        val numCmd = intArrayOf(21, 22, 23, 24, 25, 26, 27, 28)
        val buttonsText = arrayOf(
            "1", "2", "3", "4",
            "5", "6", "7", "8"
        )
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp, bottom = 2.dp, start = 10.dp, end = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(numbers) { num ->
                Button(
                    onClick = { viewModel.sendNotificationSpecialCommand(numCmd[num - 1]) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    val txt: String = buttonsText[num - 1]
                    Text(txt)
                }
            }
        }
        Spacer(modifier = Modifier
            .fillMaxWidth()
            .height(10.dp)
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
                //modifier = Modifier.fillMaxWidth(0.5f)
            ) {
                Text("Prev")
            }
            Button(onClick = {
                // Cast context to Activity and call finishAndRemoveTask
                (context as? Activity)?.finishAndRemoveTask()
            }) {
                Text(text = "Exit App")
            }
            Button(
                onClick = onNextScreen,
                //modifier = Modifier.fillMaxWidth(0.5f)
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

