package com.example.ui.admin

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.PdfEntity
import com.example.ui.components.ConfirmActionDialog
import com.example.ui.components.ItiTopAppBar
import com.example.ui.components.StatusBadge
import com.example.ui.library.FeedbackCard
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.SuccessGreen

@Composable
fun AdminPdfScreen(
    viewModel: AdminPdfViewModel,
    onNavigateBack: () -> Unit,
    onSelectPdfForExam: (String) -> Unit
) {
    val activeUser by viewModel.activeUser.collectAsStateWithLifecycle()
    val adminPdfs by viewModel.adminPdfs.collectAsStateWithLifecycle()
    val actionState by viewModel.actionState.collectAsStateWithLifecycle()

    var showUploadDialog by remember { mutableStateOf(false) }
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var pdfToDelete by remember { mutableStateOf<PdfEntity?>(null) }

    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            selectedUri = uri
            showUploadDialog = true
        }
    }

    Scaffold(
        topBar = {
            ItiTopAppBar(
                title = "Admin PDF Library",
                canNavigateBack = true,
                onNavigateBack = onNavigateBack
            )
        },
        floatingActionButton = {
            if (activeUser?.isAdmin == true) {
                ExtendedFloatingActionButton(
                    onClick = {
                        pdfPickerLauncher.launch(arrayOf("application/pdf"))
                    },
                    icon = { Icon(Icons.Default.CloudUpload, contentDescription = null) },
                    text = { Text("Publish Trade PDF") },
                    modifier = Modifier.testTag("admin_publish_pdf_fab")
                )
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Action feedback
            when (val state = actionState) {
                is AdminActionState.Loading -> {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                        ) {
                            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.5.dp)
                                Spacer(modifier = Modifier.width(14.dp))
                                Text(state.message, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
                is AdminActionState.Success -> {
                    item {
                        FeedbackCard(
                            title = "Admin Action Succeeded",
                            message = state.message,
                            color = SuccessGreen,
                            icon = Icons.Default.CheckCircle,
                            onDismiss = { viewModel.dismissMessage() }
                        )
                    }
                }
                is AdminActionState.Error -> {
                    item {
                        FeedbackCard(
                            title = "Error",
                            message = state.message,
                            color = ErrorRed,
                            icon = Icons.Default.Error,
                            onDismiss = { viewModel.dismissMessage() }
                        )
                    }
                }
                is AdminActionState.Idle -> {}
            }

            // Notice Banner
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Authorized ITI Syllabus Distribution",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Text(
                                text = "PDFs marked 'Visible' automatically become available to all local student profiles on this device for taking mock tests.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Official Trade Documents (${adminPdfs.size})",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }

            if (adminPdfs.isEmpty() && actionState !is AdminActionState.Loading) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp).fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("No official trade PDFs uploaded yet.", fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "Publish trade syllabus modules or blueprints for Electrician, Fitter, Welder, COPA, etc.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(adminPdfs) { pdf ->
                    AdminPdfItemCard(
                        pdf = pdf,
                        isAdmin = activeUser?.isAdmin == true,
                        onToggleVisibility = {
                            viewModel.toggleVisibility(pdf.id, pdf.isVisible)
                        },
                        onSelectForExam = { onSelectPdfForExam(pdf.id) },
                        onDelete = { pdfToDelete = pdf }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(72.dp))
            }
        }
    }

    if (showUploadDialog && selectedUri != null) {
        AdminPdfUploadDialog(
            uri = selectedUri!!,
            onDismiss = {
                showUploadDialog = false
                selectedUri = null
            },
            onPublish = { title, subject, trade, topic, description, isVisible ->
                viewModel.importAdminPdf(selectedUri!!, title, subject, trade, topic, description, isVisible)
                showUploadDialog = false
                selectedUri = null
            }
        )
    }

    if (pdfToDelete != null) {
        val target = pdfToDelete!!
        ConfirmActionDialog(
            title = "Delete Admin PDF?",
            message = "Are you sure you want to delete \"${target.title}\"? This will remove it from the Admin library and prevent all students from starting new exams with it.",
            confirmButtonText = "Delete",
            isDestructive = true,
            onConfirm = {
                viewModel.deleteAdminPdf(target.id)
                pdfToDelete = null
            },
            onDismiss = { pdfToDelete = null }
        )
    }
}

@Composable
fun AdminPdfItemCard(
    pdf: PdfEntity,
    isAdmin: Boolean,
    onToggleVisibility: () -> Unit,
    onSelectForExam: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("admin_pdf_item_${pdf.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.tertiaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MenuBook,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = pdf.title,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (pdf.trade.isNotBlank() || pdf.subject.isNotBlank()) {
                            Text(
                                text = listOf(pdf.trade, pdf.subject).filter { it.isNotBlank() }.joinToString(" • "),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (isAdmin) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.testTag("admin_delete_pdf_${pdf.id}")
                    ) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = ErrorRed)
                    }
                }
            }

            if (pdf.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = pdf.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Visibility & Action Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Visibility toggle & badge
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusBadge(status = if (pdf.isVisible) "PUBLISHED" else "HIDDEN")

                    if (isAdmin) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Switch(
                            checked = pdf.isVisible,
                            onCheckedChange = { onToggleVisibility() },
                            modifier = Modifier.testTag("visibility_switch_${pdf.id}")
                        )
                    }
                }

                Button(
                    onClick = onSelectForExam,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("admin_exam_setup_${pdf.id}")
                ) {
                    Text("Exam Setup", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun AdminPdfUploadDialog(
    uri: Uri,
    onDismiss: () -> Unit,
    onPublish: (title: String, subject: String, trade: String, topic: String, description: String, isVisible: Boolean) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var trade by remember { mutableStateOf("Electrician") }
    var subject by remember { mutableStateOf("Trade Theory") }
    var topic by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var isVisible by remember { mutableStateOf(true) }

    val tradeOptions = listOf("Electrician", "Fitter", "Welder", "COPA", "Mechanic Diesel", "Wireman", "Electronic Mechanic", "Turner")
    var tradeDropdownExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Publish Admin Trade PDF", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Document Title (e.g. Module 1 Theory)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("admin_input_title")
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Trade Selector
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = trade,
                        onValueChange = { trade = it },
                        label = { Text("ITI Trade") },
                        modifier = Modifier.fillMaxWidth().testTag("admin_input_trade"),
                        trailingIcon = {
                            IconButton(onClick = { tradeDropdownExpanded = true }) {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                        }
                    )
                    DropdownMenu(
                        expanded = tradeDropdownExpanded,
                        onDismissRequest = { tradeDropdownExpanded = false }
                    ) {
                        tradeOptions.forEach { t ->
                            DropdownMenuItem(
                                text = { Text(t) },
                                onClick = {
                                    trade = t
                                    tradeDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = subject,
                    onValueChange = { subject = it },
                    label = { Text("Subject (e.g. Trade Theory / Practical)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("admin_input_subject")
                )

                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = topic,
                    onValueChange = { topic = it },
                    label = { Text("Topic / Chapter (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Short Description") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = isVisible,
                        onCheckedChange = { isVisible = it },
                        modifier = Modifier.testTag("admin_checkbox_visible")
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Visible to all local student profiles", fontSize = 13.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onPublish(title, subject, trade, topic, description, isVisible)
                },
                modifier = Modifier.testTag("admin_submit_publish_btn")
            ) {
                Text("Publish to Library")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
