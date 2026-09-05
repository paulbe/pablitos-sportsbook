package com.pablitosb.sportsbook.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pablitosb.sportsbook.data.dfs.SampleSalaryCsv
import com.pablitosb.sportsbook.theme.AccentGreen
import com.pablitosb.sportsbook.theme.TextMuted

@Composable
fun SalaryImportDialog(
    title: String,
    paste: String,
    onPasteChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onLoadPaste: () -> Unit,
    onLoadExample: () -> Unit,
    onShareSample: () -> Unit,
    pasteConfirmLabel: String = "Load paste",
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                Text(SampleSalaryCsv.HINT, color = TextMuted, fontSize = 12.sp, lineHeight = 16.sp)
                Spacer(Modifier.height(8.dp))
                StubButton(
                    label = "Sample CSV",
                    onClick = onShareSample,
                    modifier = Modifier.fillMaxWidth(),
                    leading = {
                        Icon(Icons.Outlined.Download, null, tint = AccentGreen, modifier = Modifier.size(16.dp))
                    },
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = paste,
                    onValueChange = onPasteChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    placeholder = { Text("Aaron Judge,NYY,OF,4500") },
                    label = { Text("Paste CSV") },
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onLoadPaste) { Text(pasteConfirmLabel, color = AccentGreen) }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onLoadExample) { Text("EXAMPLE file", color = AccentGreen) }
                TextButton(onClick = onDismiss) { Text("Cancel", color = TextMuted) }
            }
        },
    )
}

@Composable
fun SalaryActionLinks(
    onImport: () -> Unit,
    onSample: () -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(
            "Import slate",
            color = AccentGreen,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.clickable(onClick = onImport),
        )
        Text(
            "Sample CSV",
            color = AccentGreen,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.clickable(onClick = onSample),
        )
    }
}
