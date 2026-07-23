package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.R
import com.example.data.Medication
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter

enum class DoctorCodeFormat { QR_CODE, BARCODE_1D }

@Composable
fun DoctorQrDialog(
    userAccountName: String,
    medications: List<Medication>,
    onDismiss: () -> Unit
) {
    var selectedFormat by remember { mutableStateOf(DoctorCodeFormat.QR_CODE) }

    val fullPayload = remember(userAccountName, medications) {
        val medSummary = medications.joinToString("; ") { "${it.name} (${it.dosage})" }
        "PATIENT: ${userAccountName.ifBlank { "Guest User" }}\nMEDS: ${medSummary.ifBlank { "None registered" }}"
    }

    val barcodePayload = remember(userAccountName) {
        val cleanName = userAccountName.filter { it.isLetterOrDigit() }.take(12)
        "MED-${cleanName.ifBlank { "PATIENT" }}-${System.currentTimeMillis() / 86400000}"
    }

    val displayBitmap = remember(selectedFormat, fullPayload, barcodePayload) {
        if (selectedFormat == DoctorCodeFormat.QR_CODE) {
            generateBarcodeBitmap(fullPayload, BarcodeFormat.QR_CODE, 512, 512)
        } else {
            generateBarcodeBitmap(barcodePayload, BarcodeFormat.CODE_128, 600, 200)
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.QrCode,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Text(
                        text = stringResource(R.string.qr_dialog_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Format Selector Segmented Control
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    SegmentedButton(
                        selected = selectedFormat == DoctorCodeFormat.QR_CODE,
                        onClick = { selectedFormat = DoctorCodeFormat.QR_CODE },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                    ) {
                        Text(stringResource(R.string.barcode_format_qr), style = MaterialTheme.typography.labelMedium)
                    }
                    SegmentedButton(
                        selected = selectedFormat == DoctorCodeFormat.BARCODE_1D,
                        onClick = { selectedFormat = DoctorCodeFormat.BARCODE_1D },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                    ) {
                        Text(stringResource(R.string.barcode_format_1d), style = MaterialTheme.typography.labelMedium)
                    }
                }

                // High Contrast Code Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (selectedFormat == DoctorCodeFormat.QR_CODE) 200.dp else 120.dp)
                        .background(Color.White, RoundedCornerShape(16.dp))
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (displayBitmap != null) {
                        Image(
                            bitmap = displayBitmap.asImageBitmap(),
                            contentDescription = "Doctor Scanner Code",
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        CircularProgressIndicator()
                    }
                }

                // Smart Patient Clinical Summary Card
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.MedicalServices,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = userAccountName.ifBlank { "Patient / User" },
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        val medListText = if (medications.isEmpty()) "No active prescriptions" else medications.joinToString("\n• ") { "${it.name} (${it.dosage})" }
                        Text(
                            text = "Prescriptions (${medications.size}):\n• $medListText",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        }
    }
}

private fun generateBarcodeBitmap(content: String, format: BarcodeFormat, width: Int, height: Int): Bitmap? {
    return try {
        val writer = MultiFormatWriter()
        val bitMatrix = writer.encode(content, format, width, height)
        val matrixWidth = bitMatrix.width
        val matrixHeight = bitMatrix.height
        val bmp = Bitmap.createBitmap(matrixWidth, matrixHeight, Bitmap.Config.RGB_565)
        for (x in 0 until matrixWidth) {
            for (y in 0 until matrixHeight) {
                bmp.setPixel(x, y, if (bitMatrix.get(x, y)) AndroidColor.BLACK else AndroidColor.WHITE)
            }
        }
        bmp
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

