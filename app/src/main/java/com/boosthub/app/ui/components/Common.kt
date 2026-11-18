package com.boosthub.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.boosthub.app.ui.theme.*

@Composable
fun ScreenColumn(modifier: Modifier = Modifier, content: LazyListScope.() -> Unit) {
    LazyColumn(
        modifier = modifier.fillMaxSize().background(BgDark).statusBarsPadding(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content
    )
}

@Composable
fun Panel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(SurfaceDark).padding(14.dp),
        content = content
    )
}

@Composable
fun BrandButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(containerColor = BrandBlue)
    ) { Text(text) }
}

@Composable
fun H(text: String) = Text(text, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)

@Composable
fun Soft(text: String, modifier: Modifier = Modifier) = Text(text, color = TextSoft, fontSize = 13.sp, modifier = modifier)

@Composable
fun White(text: String, modifier: Modifier = Modifier, bold: Boolean = false) =
    Text(text, color = Color.White, fontSize = 15.sp, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal, modifier = modifier)

@Composable
fun Field(label: String, value: String, onChange: (String) -> Unit, secret: Boolean = false, lines: Int = 1) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = lines == 1,
        minLines = lines,
        visualTransformation = if (secret) PasswordVisualTransformation() else VisualTransformation.None,
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedBorderColor = BrandBlue,
            unfocusedBorderColor = TextSoft,
            focusedLabelColor = BrandBlue,
            unfocusedLabelColor = TextSoft,
            cursorColor = BrandBlue
        )
    )
}

@Composable
fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        Text(label, color = Color.White, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
