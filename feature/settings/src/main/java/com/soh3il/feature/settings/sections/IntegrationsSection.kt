package com.soh3il.feature.settings.sections

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soh3il.core.designsystem.components.FarsiCard
import com.soh3il.feature.settings.components.SettingsSectionHeader

@Composable
fun IntegrationsSection(
    contactBirthdaysEnabled: Boolean,
    contactsPermissionGranted: Boolean,
    onContactBirthdaysToggled: (Boolean) -> Unit
) {
    SettingsSectionHeader(
        title = "مخاطبان و تقویم‌های بیرونی",
        subtitle = "تولد مخاطبان و تقویم دستگاه، از جمله حساب Google همگام‌شده",
        icon = Icons.Default.Contacts
    )
    Spacer(modifier = Modifier.height(6.dp))
    FarsiCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(role = Role.Switch) { onContactBirthdaysToggled(!contactBirthdaysEnabled) }
                    .semantics(mergeDescendants = true) {},
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "تولد مخاطبان",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (contactsPermissionGranted) {
                            "تولدها به‌صورت محلی و سالانه در تقویم خورشیدی نمایش داده می‌شوند."
                        } else {
                            "برای خواندن تولدها، مجوز مخاطبان لازم است."
                        },
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                Switch(checked = contactBirthdaysEnabled, onCheckedChange = null)
            }
        }
    }
}
