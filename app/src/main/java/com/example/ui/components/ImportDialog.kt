package com.example.ui.components

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.CosmicBorder
import com.example.ui.theme.CosmicSurface
import com.example.ui.theme.CosmicSurfaceElevated
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ElectricPurple
import com.example.ui.theme.SlateTextMuted
import com.example.ui.theme.SlateTextPrimary
import com.example.ui.theme.SlateTextSecondary
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup

private const val MAX_TEXT_FILE_BYTES = 2L * 1024L * 1024L
private const val MAX_PDF_FILE_BYTES = 8L * 1024L * 1024L
private const val MAX_NETWORK_BYTES = 2L * 1024L * 1024L
private const val MAX_IMPORTED_CHARACTERS = 500_000

private class ImportTooLargeException(message: String) : IOException(message)

private val importHttpClient = OkHttpClient.Builder()
    .connectTimeout(12, TimeUnit.SECONDS)
    .readTimeout(20, TimeUnit.SECONDS)
    .callTimeout(30, TimeUnit.SECONDS)
    .followRedirects(true)
    .followSslRedirects(true)
    .build()

private fun Context.queryContentSize(uri: Uri): Long? {
    return runCatching {
        contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { cursor ->
            val index = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (index >= 0 && cursor.moveToFirst() && !cursor.isNull(index)) cursor.getLong(index) else null
        }
    }.getOrNull()
}

private fun readBytesWithLimit(input: InputStream, maxBytes: Long): ByteArray {
    val output = ByteArrayOutputStream()
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    var total = 0L
    while (true) {
        val count = input.read(buffer)
        if (count < 0) break
        total += count
        if (total > maxBytes) {
            throw ImportTooLargeException("The selected content is too large to import safely on this device.")
        }
        output.write(buffer, 0, count)
    }
    return output.toByteArray()
}

private fun validateImportedText(text: String): String {
    val cleaned = text.replace("\u0000", "").trim()
    if (cleaned.isBlank()) throw IOException("No readable text was found.")
    if (cleaned.length > MAX_IMPORTED_CHARACTERS) {
        throw ImportTooLargeException(
            "This source contains more than ${MAX_IMPORTED_CHARACTERS / 1000}k characters. Split it into smaller scripts before importing.",
        )
    }
    return cleaned
}

private fun extractArticleText(html: String): String {
    val document = Jsoup.parse(html)
    document.select(
        "script,style,nav,footer,header,head,iframe,noscript,svg,aside,.sidebar,#sidebar,.menu,#menu,.comments,#comments,.ads,#ads",
    ).remove()

    val likelyContainers = document.select(
        "article,[itemprop=articleBody],.post-content,.entry-content,main,#main,#content",
    )
    for (container in likelyContainers) {
        val candidate = container.select("p,h1,h2,h3,h4,h5,h6")
            .map { it.text().trim() }
            .filter { it.isNotBlank() }
            .joinToString("\n\n")
            .trim()
        if (candidate.length >= 120) return candidate
    }

    val paragraphs = document.select("p")
        .map { it.text().trim() }
        .filter { it.length >= 15 }
    if (paragraphs.isNotEmpty()) return paragraphs.joinToString("\n\n")

    return document.body()?.text()?.trim().orEmpty()
}

private fun normalizedHttpsUrl(raw: String): String {
    val trimmed = raw.trim()
    if (trimmed.startsWith("http://", ignoreCase = true)) {
        throw IOException("For privacy and integrity, CueFlow imports web content over HTTPS only.")
    }
    val candidate = if (trimmed.contains("://")) trimmed else "https://$trimmed"
    val parsed = candidate.toHttpUrlOrNull() ?: throw IOException("Enter a valid web address.")
    if (parsed.scheme != "https") throw IOException("Only HTTPS links are supported.")
    return parsed.toString()
}

private fun fetchHttpsText(url: String): String {
    val normalized = normalizedHttpsUrl(url)
    val request = Request.Builder()
        .url(normalized)
        .header("User-Agent", "CueFlow/1.0 Android script importer")
        .build()

    importHttpClient.newCall(request).execute().use { response ->
        if (!response.isSuccessful) throw IOException("Server returned HTTP ${response.code}.")
        if (response.request.url.scheme != "https") throw IOException("The server redirected to an insecure connection.")
        val body = response.body ?: throw IOException("The server returned an empty response.")
        val declaredLength = body.contentLength()
        if (declaredLength > MAX_NETWORK_BYTES) {
            throw ImportTooLargeException("The downloaded page is larger than 2 MB and was not imported.")
        }
        val bytes = body.byteStream().use { readBytesWithLimit(it, MAX_NETWORK_BYTES) }
        return bytes.toString(Charsets.UTF_8)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportDialog(
    onDismiss: () -> Unit,
    onImportText: (text: String, mode: String) -> Unit,
    hasExistingText: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    var importMode by remember { mutableStateOf<String?>(null) }
    var inputUrl by remember { mutableStateOf("") }
    var pendingContent by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    fun finishImport(text: String) {
        if (hasExistingText) {
            pendingContent = text
        } else {
            onImportText(text, "replace")
        }
    }

    fun showError(error: Throwable, fallback: String) {
        errorMessage = when (error) {
            is ImportTooLargeException -> error.message
            else -> error.message?.takeIf { it.isNotBlank() } ?: fallback
        }
    }

    val textPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        isLoading = true
        errorMessage = null
        scope.launch {
            try {
                val text = withContext(Dispatchers.IO) {
                    val declaredSize = context.queryContentSize(uri)
                    if (declaredSize != null && declaredSize > MAX_TEXT_FILE_BYTES) {
                        throw ImportTooLargeException("Text files larger than 2 MB are not imported to protect memory on lower-end devices.")
                    }
                    val bytes = context.contentResolver.openInputStream(uri)?.use {
                        readBytesWithLimit(it, MAX_TEXT_FILE_BYTES)
                    } ?: throw IOException("The selected file could not be opened.")
                    validateImportedText(bytes.toString(Charsets.UTF_8))
                }
                finishImport(text)
            } catch (error: Throwable) {
                showError(error, "The selected text file could not be read.")
            } finally {
                isLoading = false
            }
        }
    }

    val pdfPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        isLoading = true
        errorMessage = null
        scope.launch {
            try {
                val text = withContext(Dispatchers.IO) {
                    val declaredSize = context.queryContentSize(uri)
                    if (declaredSize != null && declaredSize > MAX_PDF_FILE_BYTES) {
                        throw ImportTooLargeException("PDF files larger than 8 MB are not imported on-device. Split the PDF and try again.")
                    }
                    val pdfBytes = context.contentResolver.openInputStream(uri)?.use {
                        readBytesWithLimit(it, MAX_PDF_FILE_BYTES)
                    } ?: throw IOException("The selected PDF could not be opened.")

                    PDFBoxResourceLoader.init(context.applicationContext)
                    PDDocument.load(pdfBytes).use { document ->
                        validateImportedText(PDFTextStripper().getText(document))
                    }
                }
                finishImport(text)
            } catch (error: Throwable) {
                showError(
                    error,
                    "This PDF could not be read. It may be password-protected, corrupted, image-only, or unsupported.",
                )
            } finally {
                isLoading = false
            }
        }
    }

    fun importWebArticle() {
        if (inputUrl.isBlank()) {
            errorMessage = "Enter an HTTPS web address."
            return
        }
        isLoading = true
        errorMessage = null
        scope.launch {
            try {
                val text = withContext(Dispatchers.IO) {
                    val html = fetchHttpsText(inputUrl)
                    validateImportedText(extractArticleText(html))
                }
                finishImport(text)
                importMode = null
            } catch (error: Throwable) {
                showError(error, "The page could not be imported.")
            } finally {
                isLoading = false
            }
        }
    }

    fun importGoogleDoc() {
        if (inputUrl.isBlank()) {
            errorMessage = "Paste a Google Docs sharing link."
            return
        }
        isLoading = true
        errorMessage = null
        scope.launch {
            try {
                val text = withContext(Dispatchers.IO) {
                    val normalized = normalizedHttpsUrl(inputUrl)
                    val parsed = normalized.toHttpUrlOrNull() ?: throw IOException("Invalid Google Docs link.")
                    if (!parsed.host.equals("docs.google.com", ignoreCase = true)) {
                        throw IOException("Use a docs.google.com document link.")
                    }
                    val docId = Regex("/document/d/([A-Za-z0-9_-]+)")
                        .find(parsed.encodedPath)
                        ?.groupValues
                        ?.getOrNull(1)
                        ?: throw IOException("The Google Docs document ID could not be found in this link.")
                    val exportUrl = "https://docs.google.com/document/d/$docId/export?format=txt"
                    validateImportedText(fetchHttpsText(exportUrl))
                }
                finishImport(text)
                importMode = null
            } catch (error: Throwable) {
                showError(
                    error,
                    "The Google Doc could not be downloaded. Make sure the document can be viewed by anyone with the link.",
                )
            } finally {
                isLoading = false
            }
        }
    }

    Dialog(onDismissRequest = { if (!isLoading) onDismiss() }) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .heightIn(max = 680.dp)
                .border(1.dp, CosmicBorder, RoundedCornerShape(22.dp))
                .testTag("import_source_dialog"),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = CosmicSurfaceElevated),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (importMode != null) {
                            IconButton(
                                onClick = {
                                    importMode = null
                                    inputUrl = ""
                                    errorMessage = null
                                },
                                enabled = !isLoading,
                                modifier = Modifier.size(48.dp),
                            ) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                            }
                        }
                        Text(
                            text = when (importMode) {
                                "web" -> "Import web article"
                                "gdoc" -> "Import Google Doc"
                                else -> "Import script"
                            },
                            color = SlateTextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        enabled = !isLoading,
                        modifier = Modifier.size(48.dp),
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close import dialog", tint = SlateTextSecondary)
                    }
                }

                if (errorMessage != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFEF4444).copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                            .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                            .testTag("import_error_banner"),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = errorMessage.orEmpty(),
                            color = Color(0xFFFFB4AB),
                            fontSize = 12.sp,
                            lineHeight = 17.sp,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                if (pendingContent != null) {
                    ConflictResolution(
                        characterCount = pendingContent.orEmpty().length,
                        onAppend = {
                            onImportText(pendingContent.orEmpty(), "append")
                            pendingContent = null
                        },
                        onReplace = {
                            onImportText(pendingContent.orEmpty(), "replace")
                            pendingContent = null
                        },
                        onCancel = { pendingContent = null },
                    )
                } else if (isLoading) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        CircularProgressIndicator(color = ElectricCyan)
                        Spacer(Modifier.height(14.dp))
                        Text("Reading source safely…", color = SlateTextSecondary, fontSize = 13.sp)
                    }
                } else if (importMode == null) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(
                            "Files are processed on this device. Web and Google Docs imports use the internet only when you choose them.",
                            color = SlateTextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 17.sp,
                        )

                        ImportSourceButton(
                            icon = Icons.Default.ContentPaste,
                            title = "Paste from clipboard",
                            subtitle = "Fastest for text copied from Notes, Docs, or a browser",
                            testTag = "import_clipboard_button",
                        ) {
                            val text = clipboardManager.getText()?.text.orEmpty()
                            runCatching { validateImportedText(text) }
                                .onSuccess(::finishImport)
                                .onFailure { showError(it, "Clipboard does not contain readable text.") }
                        }
                        ImportSourceButton(
                            icon = Icons.Default.Description,
                            title = "Text file",
                            subtitle = "TXT and other plain-text files up to 2 MB",
                            testTag = "import_text_file_button",
                        ) { textPicker.launch("text/*") }
                        ImportSourceButton(
                            icon = Icons.Default.PictureAsPdf,
                            title = "PDF",
                            subtitle = "Text-based PDFs up to 8 MB; scanned-image PDFs need OCR elsewhere",
                            testTag = "import_pdf_button",
                        ) { pdfPicker.launch("application/pdf") }
                        ImportSourceButton(
                            icon = Icons.Default.Article,
                            title = "Web article",
                            subtitle = "Fetch readable text over HTTPS",
                            testTag = "import_web_button",
                        ) {
                            importMode = "web"
                            errorMessage = null
                        }
                        ImportSourceButton(
                            icon = Icons.Default.Language,
                            title = "Google Docs",
                            subtitle = "Imports a link-accessible document as plain text",
                            testTag = "import_gdoc_button",
                        ) {
                            importMode = "gdoc"
                            errorMessage = null
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Text(
                            if (importMode == "gdoc") {
                                "Paste a docs.google.com link. The document must be accessible through the link."
                            } else {
                                "Paste an HTTPS page. CueFlow downloads at most 2 MB and extracts readable article text."
                            },
                            color = SlateTextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 17.sp,
                        )
                        OutlinedTextField(
                            value = inputUrl,
                            onValueChange = { inputUrl = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("import_url_input"),
                            label = { Text(if (importMode == "gdoc") "Google Docs link" else "HTTPS URL") },
                            placeholder = {
                                Text(
                                    if (importMode == "gdoc") "https://docs.google.com/document/d/…" else "https://example.com/article",
                                )
                            },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = SlateTextPrimary,
                                unfocusedTextColor = SlateTextPrimary,
                                focusedBorderColor = ElectricPurple,
                                unfocusedBorderColor = CosmicBorder,
                                focusedLabelColor = ElectricPurple,
                                unfocusedLabelColor = SlateTextMuted,
                            ),
                        )
                        Button(
                            onClick = if (importMode == "gdoc") ::importGoogleDoc else ::importWebArticle,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("import_url_confirm_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricPurple),
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Text("Import text", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ImportSourceButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    testTag: String,
    onClick: () -> Unit,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .testTag(testTag),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, CosmicBorder),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = CosmicSurface),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Icon(icon, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = SlateTextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text(subtitle, color = SlateTextMuted, fontSize = 11.sp, lineHeight = 15.sp)
        }
    }
}

@Composable
private fun ConflictResolution(
    characterCount: Int,
    onAppend: () -> Unit,
    onReplace: () -> Unit,
    onCancel: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            "This script already has text.",
            color = SlateTextPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
        )
        Text(
            "Imported source: $characterCount characters. Choose whether to add it after the current script or replace the current text.",
            color = SlateTextSecondary,
            fontSize = 12.sp,
            lineHeight = 17.sp,
        )
        HorizontalDivider(color = CosmicBorder)
        Button(
            onClick = onAppend,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("import_append_button"),
            colors = ButtonDefaults.buttonColors(containerColor = ElectricPurple),
            shape = RoundedCornerShape(12.dp),
        ) {
            Text("Append to script", fontWeight = FontWeight.Bold)
        }
        OutlinedButton(
            onClick = onReplace,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("import_replace_button"),
            border = androidx.compose.foundation.BorderStroke(1.dp, CosmicBorder),
            shape = RoundedCornerShape(12.dp),
        ) {
            Text("Replace current text", color = SlateTextPrimary)
        }
        TextButton(onClick = onCancel, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text("Cancel", color = SlateTextSecondary)
        }
    }
}
