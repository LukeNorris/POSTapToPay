package com.adyen.postaptopay.presentation.composables

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun NumberInput(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var textFieldValue by remember { mutableStateOf(value)}

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .width(150.dp)
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = "€",
            fontSize = 40.sp,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(end = 4.dp)
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .border(BorderStroke(1.dp, Color.Transparent), shape = MaterialTheme.shapes.medium)
                .padding(8.dp)
        ) {
            BasicTextField(
                value = textFieldValue,
                onValueChange = { newValue ->
                    // Step 1: Filter the input to allow only digits and a single decimal point
                    var filteredValue = newValue.filter { it.isDigit() || it == '.' }

                    // Step 2: Handle leading zeros
                    if (filteredValue.startsWith("00")) {
                        filteredValue = "0" + filteredValue.trimStart('0')
                    } else if (filteredValue.startsWith("0") && filteredValue.length > 1 && filteredValue[1] != '.') {
                        filteredValue = filteredValue.trimStart('0')
                    }

                    // Step 3: Ensure that decimal numbers have a leading zero
                    if (filteredValue.startsWith(".")) {
                        filteredValue = "0$filteredValue"
                    }

                    // Step 4: Validate the input for a single decimal point, max 2 decimal places, and value <= 1000
                    if (filteredValue.count { it == '.' } <= 1 &&
                        filteredValue.toDoubleOrNull()?.let { it <= 1000 } != false &&
                        (filteredValue.indexOf('.').let { it == -1 || filteredValue.length - it <= 3 })
                    ) {
                        textFieldValue = filteredValue
                        onValueChange(filteredValue)
                    }
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                ),
                textStyle = LocalTextStyle.current.copy(
                    fontSize = 60.sp,
                    textAlign = TextAlign.Start,
                    color = MaterialTheme.colorScheme.onBackground // Use theme color
                ),
                singleLine = true,
                modifier = Modifier
                    .align(Alignment.CenterStart)
            )

            if (textFieldValue.isEmpty()) {
                Text(
                    text = "0.00",
                    fontSize = 60.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f), // Use theme color with transparency
                    modifier = Modifier.align(Alignment.CenterStart)
                )
            }
        }
    }
}

