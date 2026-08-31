package com.example.spectroom.ui.screens.profile

import android.app.DatePickerDialog
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import java.util.Calendar

data class DateOfBirthPickerRequest(
    val initialDateMillis: Long,
    val onSelected: (Long) -> Unit,
    val onDismissed: () -> Unit = {},
)

fun maxDateOfBirthMillis(): Long =
    Calendar.getInstance().apply { add(Calendar.YEAR, -18) }.timeInMillis

fun minDateOfBirthMillis(): Long =
    Calendar.getInstance().apply { add(Calendar.YEAR, -100) }.timeInMillis

@Composable
fun ProfileDateOfBirthPickerHost(
    request: DateOfBirthPickerRequest?,
    activity: ComponentActivity?,
    onDismiss: () -> Unit,
) {
    val currentRequest = request ?: return
    val host = activity ?: return

    DisposableEffect(currentRequest, host) {
        val calendar = Calendar.getInstance().apply { timeInMillis = currentRequest.initialDateMillis }
        val dialog = DatePickerDialog(
            host,
            { _, year, month, day ->
                val pickedMillis = Calendar.getInstance().apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, day)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                currentRequest.onSelected(pickedMillis)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH),
        )
        dialog.datePicker.maxDate = maxDateOfBirthMillis()
        dialog.datePicker.minDate = minDateOfBirthMillis()
        dialog.setOnDismissListener {
            currentRequest.onDismissed()
            onDismiss()
        }
        dialog.show()
        onDispose {
            if (dialog.isShowing) {
                dialog.dismiss()
            }
        }
    }
}
