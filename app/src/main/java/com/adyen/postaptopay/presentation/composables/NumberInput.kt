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
    var textFieldValue by remember { mutableStateOf(value) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = "€",
            fontSize = 40.sp,
            modifier = Modifier.padding(end = 4.dp)
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .border(BorderStroke(1.dp, Color.Gray), shape = MaterialTheme.shapes.small)
                .padding(8.dp)
        ) {
            BasicTextField(
                value = textFieldValue,
                onValueChange = { newValue ->
                    // Restrict input to valid decimal numbers with max 2 decimal places and max value of 1000
                    val filteredValue = newValue.filter { it.isDigit() || it == '.' }
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
                textStyle = LocalTextStyle.current.copy(fontSize = 40.sp, textAlign = TextAlign.Start),
                singleLine = true,
                modifier = Modifier
                    .align(Alignment.CenterStart)

            )
            if (textFieldValue.isEmpty()) {
                Text(
                    text = "0.00",
                    fontSize = 40.sp,
                    color = Color.Gray,
                    modifier = Modifier.align(Alignment.CenterStart)
                )
            }
        }
    }
}
