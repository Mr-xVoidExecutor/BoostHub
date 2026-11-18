package com.boosthub.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.boosthub.app.ui.theme.*

/* ---------- Page layout ---------- */

@Composable
fun PageHeader(title: String, onBack: (() -> Unit)? = null) {
    Column(Modifier.fillMaxWidth().background(SurfaceWhite).statusBarsPadding()) {
        Row(
            Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = TextBlack)
                }
            } else {
                Spacer(Modifier.width(8.dp))
            }
            Text(title, color = TextBlack, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        HorizontalDivider(color = BorderGray)
    }
}

@Composable
fun ScreenColumn(
    modifier: Modifier = Modifier,
    title: String? = null,
    onBack: (() -> Unit)? = null,
    content: LazyListScope.() -> Unit
) {
    Column(modifier.fillMaxSize().background(BgLight)) {
        if (title != null) PageHeader(title, onBack)
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content
        )
    }
}

/* ---------- Cards ---------- */

@Composable
fun Panel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceWhite)
            .border(1.dp, BorderGray, RoundedCornerShape(16.dp))
            .padding(16.dp),
        content = content
    )
}

@Composable
fun IconTile(icon: ImageVector, tint: Color, size: Dp = 44.dp) {
    Box(
        Modifier.size(size).clip(RoundedCornerShape(12.dp)).background(tint.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(size * 0.55f))
    }
}

@Composable
fun CardHeader(icon: ImageVector, tint: Color, title: String, subtitle: String? = null) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconTile(icon, tint)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Body(title, bold = true)
            if (subtitle != null) Muted(subtitle)
        }
    }
    Spacer(Modifier.height(12.dp))
}

@Composable
fun StatBox(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier.clip(RoundedCornerShape(12.dp)).background(BgLight).padding(12.dp)) {
        Muted(label)
        Spacer(Modifier.height(2.dp))
        Text(value, color = TextBlack, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

@Composable
fun StatusChip(text: String, color: Color) {
    Text(
        text,
        color = color,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    )
}

@Composable
fun Notice(text: String, isError: Boolean = false) {
    val c = if (isError) BrandRed else BrandBlue
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(c.copy(alpha = 0.08f)).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Rounded.Info, contentDescription = null, tint = c, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text(text, color = TextBlack, fontSize = 14.sp, modifier = Modifier.weight(1f))
    }
}

@Composable
fun ChatBubble(text: String, isUser: Boolean) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Text(
            text,
            color = TextBlack,
            fontSize = 14.sp,
            modifier = Modifier
                .widthIn(max = 300.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(if (isUser) Color(0xFFDCE9FD) else SurfaceWhite)
                .border(1.dp, BorderGray, RoundedCornerShape(16.dp))
                .padding(12.dp)
        )
    }
}

@Composable
fun Separator() {
    HorizontalDivider(color = BorderGray, modifier = Modifier.padding(vertical = 4.dp))
}

/* ---------- Text ---------- */

@Composable
fun H(text: String) = Text(text, color = TextBlack, fontSize = 22.sp, fontWeight = FontWeight.Bold)

@Composable
fun SectionTitle(text: String) = Text(text, color = TextBlack, fontSize = 16.sp, fontWeight = FontWeight.Bold)

@Composable
fun Muted(text: String, modifier: Modifier = Modifier) =
    Text(text, color = TextSoft, fontSize = 13.sp, modifier = modifier)

@Composable
fun Body(text: String, modifier: Modifier = Modifier, bold: Boolean = false) =
    Text(
        text,
        color = TextBlack,
        fontSize = 15.sp,
        fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
        modifier = modifier
    )

/* ---------- Buttons ---------- */

@Composable
fun BrandButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = BrandBlue,
            contentColor = Color.White,
            disabledContainerColor = Color(0xFFCBD5E1),
            disabledContentColor = Color(0xFF475569)
        ),
        contentPadding = PaddingValues(horizontal = 14.dp)
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, maxLines = 1)
    }
}

@Composable
fun OutlineButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFFB8BEC8)),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = SurfaceWhite,
            contentColor = TextBlack,
            disabledContentColor = Color(0xFF9AA1AD)
        ),
        contentPadding = PaddingValues(horizontal = 14.dp)
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, maxLines = 1)
    }
}

/* ---------- Inputs ---------- */

@Composable
fun Field(label: String, value: String, onChange: (String) -> Unit, secret: Boolean = false, lines: Int = 1) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = lines == 1,
        minLines = lines,
        shape = RoundedCornerShape(12.dp),
        visualTransformation = if (secret) PasswordVisualTransformation() else VisualTransformation.None,
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = TextBlack,
            unfocusedTextColor = TextBlack,
            focusedBorderColor = BrandBlue,
            unfocusedBorderColor = Color(0xFFB8BEC8),
            focusedLabelColor = BrandBlue,
            unfocusedLabelColor = TextSoft,
            cursorColor = BrandBlue,
            focusedContainerColor = SurfaceWhite,
            unfocusedContainerColor = SurfaceWhite
        )
    )
}

@Composable
fun SwitchRow(label: String, checked: Boolean, subtitle: String? = null, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Body(label)
            if (subtitle != null) Muted(subtitle)
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = BrandBlue,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color(0xFFCBD5E1),
                uncheckedBorderColor = Color(0xFFCBD5E1)
            )
        )
    }
}

/* ---------- Dialog ---------- */

@Composable
fun AppDialog(
    title: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceWhite,
        title = { Text(title, color = TextBlack, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                content = content
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(confirmText, color = BrandBlue, fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = TextBlack) }
        }
    )
}
