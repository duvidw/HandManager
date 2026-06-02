package com.davw.handcommandserver

// Tutorial from: https://www.youtube.com/watch?v=37GCI7dwwBs


import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.ButtonDefaults.buttonColors
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Slider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Don't check permissions here with a 'return'
        // Just initialize the ViewModel and set the content
        val viewModel = ViewModelProvider(this)[BleViewModel::class.java]

        setContent {
            MaterialTheme {
                BleScreen(viewModel)
            }
        }
    }
}

@Composable
fun CustomStyledButtonRound(
    text: String,
    onClick: () -> Unit,
    //modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
// 1. Padding: 10px 20px (Converted to 10.dp and 20.dp)
        contentPadding = PaddingValues(
            top = 5.dp,
            bottom = 5.dp,
            start = 10.dp,
            end = 10.dp
        )
    ) {
        Text(
            text = text,
            // 2. Horizontal Centering (for multi-line text)
            textAlign = TextAlign.Center,
            // 3. Line Height: 1.5 (Relative to font size)
            style = LocalTextStyle.current.copy(
                lineHeight = 24.sp // 24.sp // If font is 16sp, 1.5x is 24sp
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
    //modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
// 1. Padding: 10px 20px (Converted to 10.dp and 20.dp)
        contentPadding = PaddingValues(
            top = hPad,
            bottom = hPad,
            start = wPad,
            end = wPad
        )
    ) {
        Text(
            text = text,
            // 2. Horizontal Centering (for multi-line text)
            textAlign = TextAlign.Center,
            // 3. Line Height: 1.5 (Relative to font size)
            style = LocalTextStyle.current.copy(
                lineHeight = 24.sp // 24.sp // If font is 16sp, 1.5x is 24sp
            )
        )
    }
}
@Composable
fun BleScreen(viewModel: BleViewModel) {
    val context = LocalContext.current
    val events by viewModel.events.collectAsState()
    val currentStatus by viewModel.statusText.collectAsState()

    // 1. Define the permissions needed based on Android version
    val permissionsToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_ADVERTISE
        )
    } else {
        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    // 2. Setup the launcher to handle the user's response
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissionsMap ->
        val areGranted = permissionsMap.values.all { it }
        if (areGranted) {
            // Permission granted! Start the server now
            viewModel.startServer()
        }
    }
    // 2. Helper function to wrap the permission check logic
    val performBluetoothAction: (Int) -> Unit = { id ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val isGranted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED

            if (isGranted) {
                viewModel.sendNotification(id)
            } else {
                // FIX: Wrap the single permission in an array
                launcher.launch(arrayOf(Manifest.permission.BLUETOOTH_CONNECT))
            }
        } else {
            viewModel.sendNotification(id)
        }
    }
    // 3. AUTO-REQUEST ON STARTUP
    // This runs once when the screen first loads
    LaunchedEffect(Unit) {
        val allPermissionsGranted = permissionsToRequest.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }

        if (!allPermissionsGranted) {
            launcher.launch(permissionsToRequest)
        } else {
            // If already granted, just start the server
            viewModel.startServer()
        }
    }

    // --- Rest of your UI (Sliders, Buttons, etc.) ---
    var slider1 by remember { mutableFloatStateOf(50f) }
    var slider2 by remember { mutableFloatStateOf(50f) }
    var slider3 by remember { mutableFloatStateOf(50f) }
    var slider4 by remember { mutableFloatStateOf(50f) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            "Android Hand Commander",
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
//            Button(onClick = { performBluetoothAction(1) },
//                modifier = Modifier.width(butWidth1)) {
//                Text("Abs")
//            }
            CustomStyledButtonRound(
                text = "Abs",
                onClick = { performBluetoothAction(1) }
            )
//            Button(onClick = { performBluetoothAction(2) },
//                    modifier = Modifier.width(butWidth1)) {
//                Text("Step")
//            }
            CustomStyledButtonRound(
                text = "Step",
                onClick = { performBluetoothAction(2) }
            )
//            Button(onClick = { viewModel.subscribe() },
//                modifier = Modifier.width(butWidth2)
//            ) {
//                Text("Subscribe")
//            }
            CustomStyledButtonRound(
                text = "Subscribe",
                onClick = { viewModel.subscribe() }
            )

//            Button(onClick = { viewModel.disconnect() },
//                modifier = Modifier.width(butWidth2)) {
//                Text("Disc.")
//            }
            CustomStyledButtonRound(
                text = "Disconnect",
                onClick = { viewModel.disconnect() }
            )

        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
//            Button(onClick = { viewModel.stopMotor() },
//                modifier = Modifier.width(butWidth1)) {
//                Text("Stop")
//            }
            CustomStyledButtonRound(
                text = "Stop",
                onClick = { viewModel.stopMotor() }
            )
            CustomStyledButtonRound(
                text = "Forward",
                onClick = { viewModel.forwardMotor() }
            )

            CustomStyledButtonRound(
                text = "Backward",
                onClick = { viewModel.backwardMotor() }
            )
            CustomStyledButtonRound(
                text = "Abs position",
                onClick = { viewModel.absPosMotor() }
            )
        }
        //////////////////////////////////////////////////////////////

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(
                //onClick = { viewModel.setMotorPos(1, slider1.toInt()) },
                onClick = { viewModel.setMotor(0) },
                colors = buttonColors(
                    containerColor = viewModel.run { getButMotorColor(1) },
                    contentColor = Color.White // Text color
                )

            ) {
                Text("1")
            }

            Button(
                onClick = { viewModel.setMotor(1) },
                colors = buttonColors(
                    containerColor = viewModel.run { getButMotorColor(2) },
                    contentColor = Color.White // Text color
                )
            ) {
                Text("2")
            }

            Button(
                onClick = { viewModel.setMotor(2) },
                colors = buttonColors(
                    containerColor = viewModel.run { getButMotorColor(3) },
                    contentColor = Color.White // Text color
                )
            ) {
                Text("3")
            }

            Button(
                onClick = { viewModel.setMotor(3) },
                colors = buttonColors(
                    containerColor = viewModel.run { getButMotorColor(4) },
                    contentColor = Color.White // Text color
                )

            ) {
                Text("4")
            }

//            Button(
//                onClick = { viewModel.setMotor(4) },
//                colors = buttonColors(
//                    containerColor = viewModel.run { getButMotorColor(5) },
//                    contentColor = Color.White // Text color
//                )
//            ) {
//                Text("5")
//            }
        }
        Text(
            text = currentStatus,
            style = MaterialTheme.typography.bodyLarge, // Overrides just the size
            modifier = Modifier.padding(top = 12.dp)
        )
        ////////////////////////////////////////////////////////////
        ////////////////////////////////////////////////////////////
        SliderRow(
            viewModel,
            value1 = slider1,
            value2 = slider2,
            value3 = slider3,
            value4 = slider4,
            onValue1Change = { slider1 = it },
            onValue2Change = { slider2 = it },
            onValue3Change = { slider3 = it },
            onValue4Change = { slider4 = it }

        )
        //ThreeVerticalSliders()
        ////////////////////////////////////////////////////////////

// 4 columns � 3 rows of buttons labeled 1�12
        val numbers = (1..12).toList()

        val numCmnd = intArrayOf(1,2,3,4,0,5,6,7,8,9,10,11,12,13,14)
        val buttonsText = arrayOf("1", "2", "3", "4",
            "0", "5","6","7",
            "8" ,"9","10","11")


        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(numbers) { num ->
                Button(
                    //onClick = { viewModel.sendIntCommandPos(num) },
                    onClick = { viewModel.sendIntCommandPos(numCmnd[num-1]) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    //Text(num.toString())
                    val txt: String = buttonsText[num-1]
                    Text(txt)
                }
            }
        }
        //////////////////////////////////////////////////////////////
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .height(12.dp)
                .fillMaxWidth()
                .padding(top = 16.dp)
        ) {
            items(events.reversed()) { msg ->
                Text(msg)
            }
        }
    }
}

@Composable

fun viewModel(): BleViewModel {
    TODO("Not yet implemented")
}
@Composable
fun SliderRow(
    viewModel: BleViewModel,
    value1: Float,
    value2: Float,
    value3: Float,
    value4: Float,
    onValue1Change: (Float) -> Unit,
    onValue2Change: (Float) -> Unit,
    onValue3Change: (Float) -> Unit,
    onValue4Change: (Float) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp),
        verticalArrangement   = Arrangement.SpaceBetween
    ) {
        SliderWithValue(viewModel,0,"M1", value = value1,onValue1Change)
        SliderWithValue(viewModel,1,"M2", value = value2,onValue2Change)
        SliderWithValue(viewModel,2,"M3", value = value3,onValue3Change)
        SliderWithValue(viewModel,3,"M4", value = value4,onValue4Change)

    }
}

//@Composable
@Composable
fun SliderWithValue(
    viewModel: BleViewModel,
    index : Int,
    name : String,
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(25.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        CustomStyledButtonRound2("<", { viewModel.sendIntCommand(21, index) })
        Text(
            text = name,
            modifier = Modifier.padding(start = 12.dp)
                .weight(0.5f)
                .height(25.dp),
            style = MaterialTheme.typography.bodyLarge
        )

        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0f..100f,
            modifier = Modifier.weight(2f)
                .height(25.dp),
            enabled = viewModel.updateSlider(index, value)
        )

        Text(
            text = viewModel.getSlider(index).toString(),
            modifier = Modifier.padding(start = 12.dp)
                .weight(0.5f)
                .height(25.dp),
            style = MaterialTheme.typography.bodyLarge
        )
        CustomStyledButtonRound2(">", { viewModel.sendIntCommand(22, index) })

    }
}
//////////////////////////////////////////////////////////////////////////////////
