package com.davw.handcommandserver

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.paddingFrom
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults.buttonColors
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs

//enum class CommandToHand {
//    STOP,
//    FORWARD,
//    BACKWARD,
//    CLOCKWISE,
//    COUNTER_CLOCKWISE,
//    STEP,
//    POSITION,
//    CALIBRATION
//}

val positionDelta : Float= 5.0f

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
fun MainButsCommands(    viewModel: BleViewModel, ) {
    //val events by viewModel.events.collectAsState()
    Spacer(modifier = Modifier
        .fillMaxWidth()
        .height(10.dp)
        .background(Color.White))

    val numbers = (1..6).toList()

//    val numCmnd = intArrayOf(31, 32, 33, 34, 35, 36)
    val numCmnd = intArrayOf(CommandToHand.CALIBRATION.value,
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
                onClick = { viewModel.sendNotification(numCmnd[num - 1]) },
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
//    LazyColumn(
//        modifier = Modifier
//            //.weight(1f)
//            .fillMaxWidth()
//            .padding(top = 2.dp, bottom = 2.dp, start = 10.dp, end = 10.dp)
//    ) {
//        items(events.reversed()) { msg ->
//            Text(msg)
//        }
//    }

    Spacer(modifier = Modifier
        .fillMaxWidth()
        .height(2.dp)
        .background(Color.Blue))
}
////////////////////////////////////////////////////////////////////////////
@Composable
fun TrackedSlider1(viewModel: BleViewModel) {
    var sliderValue by remember { mutableStateOf(50f) }

    // 1. Create an interaction source
    val interactionSource = remember { MutableInteractionSource() }

    // 2. Collect the drag interactions
    val isDragging by interactionSource.collectIsDraggedAsState()

    // 3. React to start and stop events
    LaunchedEffect(isDragging) {
        if (isDragging) {
            println("Slider Event: Started touching/dragging: ${sliderValue}")
        } else {
            println("Slider Event: Stopped touching/released: ${sliderValue}")
        }
        //viewModel.sendMotorPosition(CommandToHand.POSITION.value,0, value = sliderValue)
    }

    Slider(
        value = sliderValue,
        valueRange = 0f..100f,
        onValueChange = {
            // sliderValue = it
            println("Old Val: $sliderValue, New Val: $it, delta: ${abs(sliderValue - it)}")
            if (abs(sliderValue - it) >= positionDelta) {
                sliderValue = it
                viewModel.sendMotorPosition(CommandToHand.POSITION.value, 0, value = sliderValue)
                println("Slider val: $sliderValue")

            }
                        },
        interactionSource = interactionSource // Pass it here
    )
}
//@Composable
//fun TrackedSlider(viewModel: BleViewModel) {
//    var sliderValue by remember { mutableStateOf(50f) }
//
//    // 1. Create an interaction source
//    val interactionSource = remember { MutableInteractionSource() }
//
//    // 2. Collect the drag interactions
//    val isDragging by interactionSource.collectIsDraggedAsState()
//
//    // 3. React to start and stop events
//    LaunchedEffect(isDragging) {
//        if (isDragging) {
//            println("Slider Event: Started touching/dragging: ${sliderValue}")
//        } else {
//            println("Slider Event: Stopped touching/released: ${sliderValue}")
//        }
//        //viewModel.sendMotorPosition(CommandToHand.POSITION.value,0, value = sliderValue)
//    }
//
//    Slider(
//        value = sliderValue,
//        valueRange = 0f..100f,
//        onValueChange = {
//            // sliderValue = it
//            println("Old Val: $sliderValue, New Val: $it, delta: ${abs(sliderValue - it)}")
//            if (abs(sliderValue - it) >= positionDelta) {
//                sliderValue = it
//                viewModel.sendMotorPosition(CommandToHand.POSITION.value, 0, value = sliderValue)
//                println("Slider val: $sliderValue")
//
//            }
//        },
//        interactionSource = interactionSource // Pass it here
//    )
//}
/////////////////////////////////////////////////////////////////////////////
@Composable
fun TrackedSlider(
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
        //verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        sliderValues.forEachIndexed { index, currentValue ->
            Row (
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically // 1. Vertically centers text and slider
            ){
                if (index == 0) {
                    Text(
                        text = "V${index}: ${currentValue.toInt()}",
                        //modifier = Modifier.padding(start = 8.dp).
                        modifier = Modifier.width(65.dp)
                    )
                    TrackedSlider(
                        id = index,
                        value = currentValue,
                        modifier = Modifier.weight(1f),
                        onValueChange = { newValue ->
                            sliderValues[index] = newValue
                            // Sends the specific motor index (0, 1, 2, 3, or 4) to your BLE device
                            viewModel.sendMotorsVelocity(
                                CommandToHand.VELOCITY.value,
                                index,
                                value = newValue
                            )
                            println("Motor $index changed to: $newValue")
                        },
                        onDragStatusChange = { isDragging, value ->
                            if (isDragging) {
                                println("Motor $index started dragging at: $value")
                            } else {
                                println("Motor $index released at: $value")
                            }
                        }
                    )

                } else {

                    Text(
                        text = "M${index }: ${currentValue.toInt()}",
                        //modifier = Modifier.padding(start = 8.dp)
                    )

                    TrackedSlider(
                        id = index,
                        value = currentValue,
                        onValueChange = { newValue ->
                            sliderValues[index] = newValue
                            // Sends the specific motor index (0, 1, 2, or 3) to your BLE device
                            viewModel.sendMotorPosition(
                                CommandToHand.POSITION.value,
                                index,
                                value = newValue
                            )
                            println("Motor $index changed to: $newValue")
                        },
                        onDragStatusChange = { isDragging, value ->
                            if (isDragging) {
                                println("Motor $index started dragging at: $value")
                            } else {
                                println("Motor $index released at: $value")
                            }
                        }
                    )
                }
            }
        }
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

    var slider0 by remember { mutableFloatStateOf(50f) }
    var slider1 by remember { mutableFloatStateOf(50f) }
    var slider2 by remember { mutableFloatStateOf(50f) }
    var slider3 by remember { mutableFloatStateOf(50f) }
    var slider4 by remember { mutableFloatStateOf(50f) }
//    Spacer(Modifier.fillMaxWidth().height(15.dp).background(Color.Red))

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
            .background(Color.Green))
        //TrackedSlider(viewModel)
        MotorControlScreenRow(viewModel)

        Spacer(modifier = Modifier
            .fillMaxWidth()
            .height(10.dp)
            .background(Color.Blue))

//        SliderRow(
//            viewModel,
//            value0 = slider0,
//            value1 = slider1,
//            value2 = slider2,
//            value3 = slider3,
//            value4 = slider4,
//            onValue0Change = {
//                slider0 = it
//                viewModel.sendMotorsVelocity(CommandToHand.VELOCITY.value,0, value = it)
//            },
//            onValue1Change = {
//                slider1 = it
//                viewModel.sendMotorPosition(CommandToHand.POSITION.value, 0, value = it)
//            },
//            onValue2Change = {
//                slider2 = it
//                viewModel.sendMotorPosition(CommandToHand.POSITION.value, 1, it)
//            },
//            onValue3Change = {
//                slider3 = it
//                viewModel.sendMotorPosition(CommandToHand.POSITION.value, 2, it)
//            },
//            onValue4Change = {
//                slider4 = it
//                viewModel.sendMotorPosition(CommandToHand.POSITION.value, 3, it)
//            }
//
//        )
        Spacer(modifier = Modifier
            .fillMaxWidth()
            .height(10.dp)
            .background(Color.White))

        val numbers = (1..8).toList()

        val numCmnd = intArrayOf(21, 22, 23, 24, 25, 26, 27, 28)
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
                    onClick = { viewModel.sendNotificationSpecialCommand(numCmnd[num - 1]) },
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
// TBD1
@Composable
fun SliderRow(
    viewModel: BleViewModel,
    value0: Float,
    value1: Float,
    value2: Float,
    value3: Float,
    value4: Float,
    onValue0Change: (Float) -> Unit,
    onValue1Change: (Float) -> Unit,
    onValue2Change: (Float) -> Unit,
    onValue3Change: (Float) -> Unit,
    onValue4Change: (Float) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(250.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(Modifier
            .fillMaxWidth()
            .height(4.dp)
            .background(Color.White))
        SliderWithValue(viewModel, 0, "V", value = value0, onValue0Change)
        Spacer(Modifier
            .fillMaxWidth()
            .height(10.dp)
            .background(Color.White))
        SliderWithValue(viewModel, 1, "M1", value = value1, onValue1Change)
        SliderWithValue(viewModel, 2, "M2", value = value2, onValue2Change)
        SliderWithValue(viewModel, 3, "M3", value = value3, onValue3Change)
        SliderWithValue(viewModel, 4, "M4", value = value4, onValue4Change)
    }
}
//@Composable
//fun SliderWithValue(
//    viewModel: BleViewModel,
//    index: Int,
//    name: String,
//    value: Float,
//    onValueChange: (Float) -> Unit,
//    modifier: Modifier = Modifier,
//    interactionSource: InteractionSource = remember { MutableInteractionSource() } // Create a new interaction source
//) {
//    Row(
//        modifier = modifier
//            .fillMaxWidth()
//            .height(50.dp),
//        horizontalArrangement = Arrangement.SpaceBetween,
//        verticalAlignment = Alignment.CenterVertically
//    ) {
//        Text(
//            text = name,
//            modifier = Modifier.padding(start = 8.dp)
//        )
//        Slider(
//            value = value,
//            onValueChange =  onValueChange {
//                println("Slider val: $value")
//            },
//            interactionSource = interactionSource // Pass it here
//        )
//
//
//
////        Slider(
////            value = value,
////            onValueChange = onValueChange,
////            valueRange = 0f..100f,
////            modifier = Modifier
////                .weight(2f)
////                .height(50.dp),
////            enabled = viewModel.updateSlider(index, value)
////        )
//
//        Text(
//            text = viewModel.getSlider(index).toString(),
//            modifier = Modifier.padding(start = 12.dp)
//        )
//
//    }
//}

@Composable
fun SliderRow1(
    viewModel: BleViewModel,
    value0: Float,
    value1: Float,
    value2: Float,
    value3: Float,
    value4: Float,
    onValue0Change: (Float) -> Unit,
    onValue1Change: (Float) -> Unit,
    onValue2Change: (Float) -> Unit,
    onValue3Change: (Float) -> Unit,
    onValue4Change: (Float) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(250.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(Modifier
            .fillMaxWidth()
            .height(4.dp)
            .background(Color.White))
        SliderWithValue(viewModel, 0, "V", value = value0, onValue0Change)
        Spacer(Modifier
            .fillMaxWidth()
            .height(10.dp)
            .background(Color.White))
        SliderWithValue(viewModel, 1, "M1", value = value1, onValue1Change)
        SliderWithValue(viewModel, 2, "M2", value = value2, onValue2Change)
        SliderWithValue(viewModel, 3, "M3", value = value3, onValue3Change)
        SliderWithValue(viewModel, 4, "M4", value = value4, onValue4Change)
    }
}

@Composable
fun SliderWithValue(
    viewModel: BleViewModel,
    index: Int,
    name: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = name,
            modifier = Modifier.padding(start = 8.dp)
        )

        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0f..100f,
            modifier = Modifier
                .weight(2f)
                .height(50.dp),
            enabled = viewModel.updateSlider(index, value)
        )

        Text(
            text = viewModel.getSlider(index).toString(),
            modifier = Modifier.padding(start = 12.dp)
        )

    }
}
