package com.example.citizenreportai.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.citizenreportai.data.model.ReportCategory
import com.example.citizenreportai.data.model.ReportStatus
import com.example.citizenreportai.ui.theme.AccentSoft
import com.example.citizenreportai.ui.theme.Accent600
import com.example.citizenreportai.ui.theme.DangerSoft
import com.example.citizenreportai.ui.theme.Danger500
import com.example.citizenreportai.ui.theme.InfoSoft
import com.example.citizenreportai.ui.theme.Info500
import com.example.citizenreportai.ui.theme.SuccessSoft
import com.example.citizenreportai.ui.theme.Success500
import com.example.citizenreportai.ui.theme.WarningSoft
import com.example.citizenreportai.ui.theme.Warning500

@Composable
fun StatusChip(status: ReportStatus, modifier: Modifier = Modifier) {
    val (label, bg, fg) = when (status) {
        ReportStatus.PENDIENTE   -> Triple("Pendiente",   WarningSoft, Warning500)
        ReportStatus.EN_REVISION -> Triple("En revisión", InfoSoft,    Info500)
        ReportStatus.PROGRAMADO  -> Triple("Programado",  AccentSoft,  Accent600)
        ReportStatus.RESUELTO    -> Triple("Resuelto",    SuccessSoft, Success500)
        ReportStatus.RECHAZADO   -> Triple("Rechazado",   DangerSoft,  Danger500)
    }
    PillChip(label = label, bg = bg, fg = fg, modifier = modifier)
}

@Composable
fun CategoryChip(
    category: ReportCategory,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val label = when (category) {
        ReportCategory.BACHE     -> "Bache"
        ReportCategory.ALUMBRADO -> "Alumbrado"
        ReportCategory.BASURA    -> "Basura"
        ReportCategory.SEGURIDAD -> "Seguridad"
        ReportCategory.PARQUES   -> "Parques"
        ReportCategory.OTROS     -> "Otros"
    }
    val bg = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val fg = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    PillChip(label = label, bg = bg, fg = fg, modifier = modifier, onClick = onClick)
}

@Composable
fun PillChip(
    label: String,
    bg: Color,
    fg: Color,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    onClick: (() -> Unit)? = null
) {
    val shape = RoundedCornerShape(999.dp)
    val base = modifier
        .clip(shape)
        .background(color = bg, shape = shape)
        .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
        .padding(horizontal = 12.dp, vertical = 6.dp)

    Row(
        modifier = base,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (leadingIcon != null) {
            Icon(leadingIcon, contentDescription = null, tint = fg, modifier = Modifier.size(14.dp))
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = fg
        )
    }
}
