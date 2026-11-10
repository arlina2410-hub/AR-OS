package com.aros.app.feature.sos.ui

import android.app.Activity
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.aros.app.designsystem.components.ArOsButton
import com.aros.app.designsystem.components.ArOsButtonVariant
import com.aros.app.designsystem.components.ArOsCard
import com.aros.app.designsystem.components.ArOsEmptyState
import com.aros.app.designsystem.components.ArOsGroup
import com.aros.app.designsystem.components.ArOsIconButton
import com.aros.app.designsystem.components.ArOsPillTone
import com.aros.app.designsystem.components.ArOsScreenScaffold
import com.aros.app.designsystem.components.ArOsStatusPill
import com.aros.app.designsystem.theme.ArOsTheme
import com.aros.app.feature.sos.domain.ContactRole
import com.aros.app.feature.sos.domain.SosContact

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsScreen(
    contacts: List<SosContact>,
    onAddContact: (String, String, ContactRole) -> Unit,
    onSetPrimary: (Long) -> Unit,
    onDeleteContact: (Long) -> Unit
) {
    val context = LocalContext.current
    var showAddDialog by remember { mutableStateOf(false) }
    var inputName by remember { mutableStateOf("") }
    var inputNumber by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Zero-permission Contact Picker launcher
    val contactPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val contactUri: Uri? = result.data?.data
            if (contactUri != null) {
                var pickedName = ""
                var pickedNumber = ""

                val projection = arrayOf(
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER
                )

                try {
                    val cursor: Cursor? = context.contentResolver.query(
                        contactUri,
                        projection,
                        null,
                        null,
                        null
                    )
                    cursor?.use {
                        if (it.moveToFirst()) {
                            pickedName = it.getString(it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)) ?: ""
                            pickedNumber = it.getString(it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)) ?: ""
                        }
                    }
                } catch (_: Exception) {}

                if (pickedNumber.isNotEmpty()) {
                    inputName = pickedName
                    inputNumber = pickedNumber
                    showAddDialog = true
                }
            }
        }
    }

    ArOsScreenScaffold(
        title = "Trusted Contacts",
        actions = {
            ArOsIconButton(
                icon = Icons.Default.Add,
                contentDescription = "Add Contact",
                onClick = {
                    inputName = ""
                    inputNumber = ""
                    errorMessage = null
                    showAddDialog = true
                },
                modifier = Modifier.testTag("add_manual_contact_button")
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(ArOsTheme.spacing.md)
        ) {
            // Header Info Card
            item {
                ArOsCard(
                    modifier = Modifier.padding(horizontal = ArOsTheme.spacing.screenMargin)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "EMERGENCY ROUTING",
                                style = ArOsTheme.typography.caption,
                                color = ArOsTheme.colors.contentTertiary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Trusted Guardians",
                                style = ArOsTheme.typography.headline,
                                color = ArOsTheme.colors.contentPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "AR SOS dials your Primary contact upon trigger. Official emergency numbers (911/112) are blocked to prevent interference.",
                                style = ArOsTheme.typography.footnote,
                                color = ArOsTheme.colors.contentSecondary
                            )
                        }

                        ArOsStatusPill(
                            label = "${contacts.size}/20",
                            tone = ArOsPillTone.Neutral
                        )
                    }
                }
            }

            // Action Buttons Row
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = ArOsTheme.spacing.screenMargin),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ArOsButton(
                        text = "Import Contact",
                        onClick = {
                            val pickIntent = Intent(
                                Intent.ACTION_PICK,
                                ContactsContract.CommonDataKinds.Phone.CONTENT_URI
                            )
                            contactPickerLauncher.launch(pickIntent)
                        },
                        variant = ArOsButtonVariant.Primary,
                        leadingIcon = Icons.Default.ContactPhone,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("pick_contact_button")
                    )

                    ArOsButton(
                        text = "Manual Entry",
                        onClick = {
                            inputName = ""
                            inputNumber = ""
                            errorMessage = null
                            showAddDialog = true
                        },
                        variant = ArOsButtonVariant.Secondary,
                        leadingIcon = Icons.Default.Add,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            if (contacts.isEmpty()) {
                item {
                    ArOsEmptyState(
                        icon = Icons.Default.ContactPhone,
                        title = "No Trusted Contacts Added",
                        description = "Add at least one trusted friend, family member, or guardian so AR SOS has a target to call.",
                        actionButtonText = "Add Trusted Contact",
                        onActionClick = { showAddDialog = true }
                    )
                }
            } else {
                item {
                    ArOsGroup(
                        header = "Guardians List",
                        footer = "Starred contact is the active Primary destination."
                    ) {
                        contacts.forEachIndexed { index, contact ->
                            val isPrimary = contact.role == ContactRole.PRIMARY
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        horizontal = ArOsTheme.spacing.md,
                                        vertical = ArOsTheme.spacing.sm
                                    ),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isPrimary) ArOsTheme.colors.danger.copy(alpha = 0.15f) else ArOsTheme.colors.surfaceRaised
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = contact.name.take(1).uppercase(),
                                        style = ArOsTheme.typography.headline,
                                        color = if (isPrimary) ArOsTheme.colors.danger else ArOsTheme.colors.contentPrimary
                                    )
                                }

                                Spacer(modifier = Modifier.width(ArOsTheme.spacing.md))

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = contact.name,
                                            style = ArOsTheme.typography.body,
                                            color = ArOsTheme.colors.contentPrimary
                                        )
                                        if (isPrimary) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            ArOsStatusPill(label = "PRIMARY", tone = ArOsPillTone.Danger, showDot = false)
                                        }
                                    }
                                    Text(
                                        text = contact.number,
                                        style = ArOsTheme.typography.numericBody,
                                        color = ArOsTheme.colors.contentSecondary
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (!isPrimary) {
                                        IconButton(onClick = { onSetPrimary(contact.id) }) {
                                            Icon(
                                                imageVector = Icons.Outlined.StarBorder,
                                                contentDescription = "Set Primary",
                                                tint = ArOsTheme.colors.warning
                                            )
                                        }
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = "Primary",
                                            tint = ArOsTheme.colors.warning,
                                            modifier = Modifier
                                                .padding(12.dp)
                                                .size(22.dp)
                                        )
                                    }

                                    IconButton(onClick = { onDeleteContact(contact.id) }) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete",
                                            tint = ArOsTheme.colors.danger.copy(alpha = 0.8f),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = {
                Text(
                    text = "Add Trusted Contact",
                    style = ArOsTheme.typography.headline,
                    color = ArOsTheme.colors.contentPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(ArOsTheme.spacing.sm)) {
                    OutlinedTextField(
                        value = inputName,
                        onValueChange = { inputName = it },
                        label = { Text("Full Name") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ArOsTheme.colors.accent,
                            unfocusedBorderColor = ArOsTheme.colors.hairline,
                            focusedTextColor = ArOsTheme.colors.contentPrimary,
                            unfocusedTextColor = ArOsTheme.colors.contentPrimary,
                            focusedContainerColor = ArOsTheme.colors.surfaceRaised,
                            unfocusedContainerColor = ArOsTheme.colors.surfaceRaised
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("contact_name_input")
                    )

                    OutlinedTextField(
                        value = inputNumber,
                        onValueChange = { inputNumber = it },
                        label = { Text("Phone Number") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ArOsTheme.colors.accent,
                            unfocusedBorderColor = ArOsTheme.colors.hairline,
                            focusedTextColor = ArOsTheme.colors.contentPrimary,
                            unfocusedTextColor = ArOsTheme.colors.contentPrimary,
                            focusedContainerColor = ArOsTheme.colors.surfaceRaised,
                            unfocusedContainerColor = ArOsTheme.colors.surfaceRaised
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("contact_number_input")
                    )

                    if (errorMessage != null) {
                        Text(
                            text = errorMessage ?: "",
                            style = ArOsTheme.typography.footnote,
                            color = ArOsTheme.colors.danger
                        )
                    }
                }
            },
            containerColor = ArOsTheme.colors.surfaceOverlay,
            confirmButton = {
                ArOsButton(
                    text = "Save Contact",
                    onClick = {
                        val validationResult = com.aros.app.feature.sos.domain.ContactValidator.validateContact(
                            name = inputName,
                            number = inputNumber,
                            currentContactCount = contacts.size
                        )
                        if (validationResult.isFailure) {
                            errorMessage = validationResult.exceptionOrNull()?.message ?: "Invalid contact details."
                            return@ArOsButton
                        }

                        val validNumber = validationResult.getOrNull() ?: inputNumber.trim()
                        onAddContact(
                            inputName.trim(),
                            validNumber,
                            if (contacts.isEmpty()) ContactRole.PRIMARY else ContactRole.BACKUP
                        )
                        showAddDialog = false
                    },
                    variant = ArOsButtonVariant.Primary
                )
            },
            dismissButton = {
                ArOsButton(
                    text = "Cancel",
                    onClick = { showAddDialog = false },
                    variant = ArOsButtonVariant.Ghost
                )
            }
        )
    }
}
