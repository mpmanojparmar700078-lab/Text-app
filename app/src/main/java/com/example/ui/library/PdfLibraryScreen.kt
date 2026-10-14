package com.example.ui.library

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WarningOrange
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PdfLibraryScreen(
    viewModel: PdfLibraryViewModel,
    onNavigateBack: () -> Unit,
    onSelectPdfForExam: (String) -> Unit
) {
    val pdfs by viewModel.userPdfs.collectAsStateWithLifecycle()
    val actionState by viewModel.actionState.collectAsStateWithLifecycle()

    var pdfToDelete by remember { mutableStateOf<PdfEntity?>(null) }

    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.importPdf(uri)
        }
    }

    Scaffold(
        topBar = {
            ItiTopAppBar(
                title = "My Private PDFs",
                canNavigateBack = true,
                onNavigateBack = onNavigateBack
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    pdfPickerLauncher.launch(arrayOf("application/pdf"))
                },
                icon = { Icon(Icons.Default.UploadFile, contentDescription = null) },
                text = { Text("Import PDF") },
                modifier = Modifier.testTag("import_pdf_fab")
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Action Feedback Banners
            when (val state = actionState) {
                is LibraryActionState.Importing -> {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.5.dp)
                                Spacer(modifier = Modifier.width(14.dp))
                                Text(
                                    text = state.message,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }
                is LibraryActionState.Success -> {
                    item {
                        FeedbackCard(
                            title = "Success",
                            message = state.message,
                            color = SuccessGreen,
                            icon = Icons.Default.CheckCircle,
                            onDismiss = { viewModel.dismissMessage() }
                        )
                    }
                }
                is LibraryActionState.Warning -> {
                    item {
                        FeedbackCard(
                            title = "OCR Required",
                            message = state.message,
                            color = WarningOrange,
                            icon = Icons.Default.Warning,
                            onDismiss = { viewModel.dismissMessage() }
                        )
                    }
                }
                is LibraryActionState.Error -> {
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
                is LibraryActionState.Idle -> {}
            }

            // Description Header
            item {
                Text(
                    text = "Your Private Study Library",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "PDFs imported here belong exclusively to your profile. Other users on this phone cannot see or access your files.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (pdfs.isEmpty() && actionState !is LibraryActionState.Importing) {
                item {
                    EmptyPdfStateCard(
                        onImportClick = {
                            pdfPickerLauncher.launch(arrayOf("application/pdf"))
                        }
                    )
                }
            } else {
                items(pdfs) { pdf ->
                    UserPdfItemCard(
                        pdf = pdf,
                        onTakeExam = { onSelectPdfForExam(pdf.id) },
                        onDelete = { pdfToDelete = pdf }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(72.dp))
            }
        }
    }

    if (pdfToDelete != null) {
        val target = pdfToDelete!!
        ConfirmActionDialog(
            title = "Delete PDF?",
            message = "Are you sure you want to delete \"${target.title}\"? This will permanently remove the PDF and any generated questions from your local profile.",
            confirmButtonText = "Delete",
            isDestructive = true,
            onConfirm = {
                viewModel.deletePdf(target.id)
                pdfToDelete = null
            },
            onDismiss = { pdfToDelete = null }
        )
    }
}

@Composable
fun UserPdfItemCard(
    pdf: PdfEntity,
    onTakeExam: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }
    val dateString = remember(pdf.createdAt) { dateFormat.format(Date(pdf.createdAt)) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("user_pdf_item_${pdf.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
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
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
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
                        Text(
                            text = "Imported $dateString",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.testTag("delete_pdf_${pdf.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete PDF",
                        tint = ErrorRed
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusBadge(status = pdf.extractionStatus)
                    if (pdf.pageCount != null && pdf.pageCount > 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${pdf.pageCount} pages",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (pdf.extractionStatus == "EXTRACTED") {
                    Button(
                        onClick = onTakeExam,
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("start_exam_btn_${pdf.id}")
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Exam Setup", fontSize = 12.sp)
                    }
                }
            }

            if (pdf.extractionStatus == "OCR_NEEDED") {
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(WarningOrange.copy(alpha = 0.1f))
                        .padding(10.dp)
                ) {
                    Text(
                        text = "Scanned document detected. This PDF contains scanned images without selectable text. Text extraction requires an OCR engine or digital PDF format.",
                        color = WarningOrange,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
fun FeedbackCard(
    title: String,
    message: String,
    color: androidx.compose.ui.graphics.Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.12f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontWeight = FontWeight.Bold, color = color, fontSize = 14.sp)
                Text(text = message, color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp)
            }
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = color)
            }
        }
    }
}

@Composable
fun EmptyPdfStateCard(onImportClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PostAdd,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(34.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No PDFs Imported Yet",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Import ITI theory notes, workshop calculations, or practical books to generate AI practice exams.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = onImportClick,
                modifier = Modifier.testTag("empty_state_import_btn")
            ) {
                Icon(Icons.Default.UploadFile, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Select PDF from Device")
            }
        }
    }
}
