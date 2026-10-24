package com.dsu.extended.ui.sdialogs

import android.text.format.Formatter
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.FactCheck
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import com.dsu.extended.R
import com.dsu.extended.compat.CompatibilityFinding
import com.dsu.extended.compat.CompatibilityLevel
import com.dsu.extended.compat.DataSafety
import com.dsu.extended.compat.FindingSeverity
import com.dsu.extended.compat.GsiCompatibilityReport
import com.dsu.extended.compat.ImageContainer
import com.dsu.extended.compat.ImageFileSystem
import com.dsu.extended.ui.components.CustomBottomSheet
import com.dsu.extended.ui.components.buttons.PrimaryButton
import com.dsu.extended.ui.components.buttons.SecondaryButton

@Composable
fun GsiCompatibilitySheet(
    report: GsiCompatibilityReport,
    onContinue: () -> Unit,
    onCancel: () -> Unit,
) {
    val scope = rememberCoroutineScope()

    CustomBottomSheet(
        title = stringResource(id = R.string.compat_title),
        icon = Icons.AutoMirrored.Rounded.FactCheck,
        onDismiss = onCancel,
    ) { hideSheet ->
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SummaryCard(report)
            SafetyCard(report)
            DetailsCard(report)

            Text(
                text = stringResource(id = R.string.compat_checks_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            report.findings.forEach { finding ->
                FindingRow(finding)
            }

            Text(
                text = stringResource(id = R.string.compat_dismiss_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SecondaryButton(
                    text = stringResource(id = R.string.compat_cancel_delete),
                    onClick = {
                        scope.launch {
                            hideSheet()
                            onCancel()
                        }
                    },
                )
                Spacer(modifier = Modifier.width(8.dp))
                PrimaryButton(
                    text = stringResource(
                        id = if (report.level == CompatibilityLevel.INCOMPATIBLE) {
                            R.string.compat_install_anyway
                        } else {
                            R.string.compat_continue
                        },
                    ),
                    glow = report.level != CompatibilityLevel.INCOMPATIBLE,
                    onClick = {
                        scope.launch {
                            hideSheet()
                            onContinue()
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun SummaryCard(report: GsiCompatibilityReport) {
    val color = levelColor(report.level)
    Surface(
        color = color.copy(alpha = 0.14f),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = levelLabel(report.level),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = color,
            )
            Text(
                text = stringResource(id = R.string.compat_boot_chance, report.estimatedBootChancePercent),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 4.dp),
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
                    .height(7.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(report.estimatedBootChancePercent.coerceIn(0, 100) / 100f)
                        .height(7.dp)
                        .clip(CircleShape)
                        .background(color),
                )
            }
            Text(
                text = stringResource(id = R.string.compat_boot_chance_caption),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun SafetyCard(report: GsiCompatibilityReport) {
    val limited = report.dataSafety == DataSafety.LIMITED
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(modifier = Modifier.padding(14.dp)) {
            Icon(
                imageVector = Icons.Rounded.Shield,
                contentDescription = null,
                tint = if (limited) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(22.dp)
                    .padding(top = 2.dp),
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = stringResource(
                        id = if (limited) R.string.compat_data_limited_title else R.string.compat_data_safe_title,
                    ),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(
                        id = if (limited) R.string.compat_data_limited_body else R.string.compat_data_safe_body,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 3.dp),
                )
            }
        }
    }
}

@Composable
private fun DetailsCard(report: GsiCompatibilityReport) {
    val facts = report.facts
    val device = report.device
    val imageAndroid = facts.androidVersion
        ?.let { version -> facts.sdk?.let { "$version (API $it)" } ?: version }
        ?: stringResource(id = R.string.compat_detail_unknown)
    val deviceAndroid = "${device.androidVersion} (API ${device.sdk})"
    val fileSystem = when (facts.fileSystem) {
        ImageFileSystem.EXT4 -> "ext4"
        ImageFileSystem.EROFS -> "EROFS"
        ImageFileSystem.F2FS -> "F2FS"
        ImageFileSystem.UNKNOWN -> stringResource(id = R.string.compat_detail_unknown)
    }
    val container = buildString {
        append(facts.container.name.lowercase())
        if (facts.sparse) append(" · sparse")
        if (facts.container == ImageContainer.ZIP && facts.zipImages.isNotEmpty()) {
            append(" · ${facts.zipImages.size} image(s)")
        }
    }

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = stringResource(id = R.string.compat_details_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            DetailRow(
                label = stringResource(id = R.string.compat_detail_device),
                value = "${device.displayName.ifBlank { stringResource(id = R.string.compat_detail_unknown) }} · $deviceAndroid",
            )
            DetailRow(
                label = stringResource(id = R.string.compat_detail_image),
                value = "${report.imageLabel} · $imageAndroid",
            )
            DetailRow(
                label = stringResource(id = R.string.compat_detail_abi),
                value = facts.abi ?: stringResource(id = R.string.compat_detail_unknown),
            )
            DetailRow(
                label = stringResource(id = R.string.compat_detail_filesystem),
                value = fileSystem,
            )
            DetailRow(
                label = stringResource(id = R.string.compat_detail_container),
                value = container,
            )
            DetailRow(
                label = stringResource(id = R.string.compat_detail_vndk),
                value = "${facts.vndkVersion ?: stringResource(id = R.string.compat_detail_unknown)} / " +
                    "${device.vndkVersion ?: stringResource(id = R.string.compat_detail_unknown)}",
            )
            DetailRow(
                label = stringResource(id = R.string.compat_detail_size),
                value = sizeText(facts.effectiveRawSize),
            )
            DetailRow(
                label = stringResource(id = R.string.compat_detail_stored),
                value = sizeText(facts.storedSizeBytes),
            )
            DetailRow(
                label = stringResource(id = R.string.compat_detail_free),
                value = sizeText(report.freeStorageBytes),
            )
            DetailRow(
                label = stringResource(id = R.string.compat_detail_required),
                value = sizeText(report.requiredStorageBytes),
            )
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(108.dp),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun FindingRow(finding: CompatibilityFinding) {
    val text = compatibilityText(finding.code)
    val title = stringResource(text.titleRes)
    val detail = if (finding.args.isEmpty()) {
        stringResource(text.detailRes)
    } else {
        stringResource(text.detailRes, *finding.args.toTypedArray())
    }
    val tint = when (finding.severity) {
        FindingSeverity.BLOCKER -> MaterialTheme.colorScheme.error
        FindingSeverity.WARNING -> MaterialTheme.colorScheme.tertiary
        FindingSeverity.INFO -> MaterialTheme.colorScheme.onSurfaceVariant
        FindingSeverity.PASS -> MaterialTheme.colorScheme.primary
    }
    val icon = when (finding.severity) {
        FindingSeverity.BLOCKER -> Icons.Rounded.Error
        FindingSeverity.WARNING -> Icons.Rounded.Warning
        FindingSeverity.INFO -> Icons.Rounded.Info
        FindingSeverity.PASS -> Icons.Rounded.CheckCircle
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(10.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier
                .size(18.dp)
                .padding(top = 1.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

@Composable
private fun levelLabel(level: CompatibilityLevel): String {
    return stringResource(
        id = when (level) {
            CompatibilityLevel.COMPATIBLE -> R.string.compat_level_compatible
            CompatibilityLevel.LIKELY_COMPATIBLE -> R.string.compat_level_likely
            CompatibilityLevel.RISKY -> R.string.compat_level_risky
            CompatibilityLevel.INCOMPATIBLE -> R.string.compat_level_incompatible
        },
    )
}

@Composable
private fun levelColor(level: CompatibilityLevel): Color {
    return when (level) {
        CompatibilityLevel.COMPATIBLE -> MaterialTheme.colorScheme.primary
        CompatibilityLevel.LIKELY_COMPATIBLE -> MaterialTheme.colorScheme.primary
        CompatibilityLevel.RISKY -> MaterialTheme.colorScheme.tertiary
        CompatibilityLevel.INCOMPATIBLE -> MaterialTheme.colorScheme.error
    }
}

@Composable
private fun sizeText(bytes: Long): String {
    val context = LocalContext.current
    if (bytes <= 0L) return stringResource(id = R.string.compat_detail_unknown)
    return Formatter.formatFileSize(context, bytes)
}
