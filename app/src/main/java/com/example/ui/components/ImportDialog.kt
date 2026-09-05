package com.example.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import okhttp3.*
import java.io.IOException

private val okHttpClient = OkHttpClient()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportDialog(
    onDismiss: () -> Unit,
    onImportText: (text: String, mode: String) -> Unit, // mode is either "append" or "replace"
    hasExistingText: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()

    // Screen navigation flow states
    var activeImportType by remember { mutableStateOf<String?>(null) } // null (selection), "url", "gdoc"
    var showConflictResolution by remember { mutableStateOf(false) }

    // Data / Loading states
    var importedContent by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isPdfLoading by remember { mutableStateOf(false) }
    var isWebLoading by remember { mutableStateOf(false) }
    var largeFileWarningMessage by remember { mutableStateOf<String?>(null) }

    // Forms input
    var inputUrl by remember { mutableStateOf("") }

    // File pick launcher for text files
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val text = inputStream.bufferedReader().use { it.readText() }
                    if (text.isNotBlank()) {
                        if (hasExistingText) {
                            importedContent = text
                            showConflictResolution = true
                        } else {
                            onImportText(text, "replace")
                        }
                    } else {
                        errorMessage = "Selected text file is empty."
                    }
                }
            } catch (e: Exception) {
                errorMessage = "Failed to read text file: ${e.localizedMessage}"
            }
        }
    }

    // PDF pick launcher
    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            isPdfLoading = true
            errorMessage = null
            largeFileWarningMessage = null
            
            // Query file size to alert user if it is a large document
            var fileSize = -1L
            try {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val sizeIdx = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
                    if (sizeIdx != -1 && cursor.moveToFirst()) {
                        fileSize = cursor.getLong(sizeIdx)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            
            if (fileSize > 4 * 1024 * 1024) { // over 4MB
                val sizeInMb = String.format("%.1f", fileSize.toFloat() / (1024 * 1024))
                largeFileWarningMessage = "This PDF document is quite large ($sizeInMb MB). Resolving pages may take a few moments..."
            }
            
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    // Initialize PDFBox once in app lifetime or as needed if rendering. We are only extracting plain text, so initialization is non-blocking.
                    context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        val document = PDDocument.load(inputStream)
                        val stripper = PDFTextStripper()
                        // Automatically handles multi-page PDFs by combining pages sequentially
                        val text = stripper.getText(document)
                        document.close()

                        withContext(Dispatchers.Main) {
                            isPdfLoading = false
                            if (!text.isNullOrBlank()) {
                                if (hasExistingText) {
                                    importedContent = text
                                    showConflictResolution = true
                                } else {
                                    onImportText(text, "replace")
                                }
                            } else {
                                errorMessage = "This file appears to be empty. We couldn't find any readable text in this document."
                            }
                        }
                    }
                } catch (e: Throwable) {
                    withContext(Dispatchers.Main) {
                        isPdfLoading = false
                        errorMessage = "Oops! We couldn't read this file. It might be corrupted, secure/password-protected, or formatted in an unreadable layout. Please try another PDF file."
                    }
                }
            }
        }
    }

    // Web Article crawler parser
    fun extractArticleContent(html: String): String {
        val doc = Jsoup.parse(html)
        // Remove junk elements that introduce menus, footer, scripts, navigation, ads
        doc.select("script, style, nav, footer, header, head, iframe, noscript, svg, .sidebar, #sidebar, .menu, #menu, .footer, #footer, .header, #header, .nav, #nav, .comments, #comments, .ads, #ads, advertising").remove()

        // Seek typical main content wrappers
        val articleElements = doc.select("article, [itemprop=articleBody], .post-content, .entry-content, main, #main, #content")
        if (articleElements.isNotEmpty()) {
            for (container in articleElements) {
                val elements = container.select("p, h1, h2, h3, h4, h5, h6")
                val textContent = elements.joinToString("\n\n") { it.text().trim() }.trim()
                if (textContent.length > 120) {
                    return textContent
                }
            }
        }

        // Dropback 1: Collect any readable paragraphs that are non-empty
        val paragraphs = doc.select("p").map { it.text().trim() }.filter { it.length > 15 }
        if (paragraphs.isNotEmpty()) {
            return paragraphs.joinToString("\n\n")
        }

        // Dropback 2: Raw body text stripped
        return doc.body()?.text()?.trim() ?: ""
    }

    // Process generic Web Link
    fun handleUrlImport() {
        val url = inputUrl.trim()
        if (url.isBlank()) {
            errorMessage = "Please enter a valid URL."
            return
        }
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            errorMessage = "URL must start with http:// or https://"
            return
        }

        isWebLoading = true
        errorMessage = null

        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/115.0.0.0 Safari/537.36")
            .build()

        okHttpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                coroutineScope.launch(Dispatchers.Main) {
                    isWebLoading = false
                    errorMessage = "Web importing requires an active internet connection. All other core features of CueFlow operate fully offline, so your scripts, sandboxes, and teleprompter playback can be used anytime!"
                }
            }

            override fun onResponse(call: Call, response: Response) {
                coroutineScope.launch(Dispatchers.Main) {
                    isWebLoading = false
                    response.use {
                        if (!response.isSuccessful) {
                            errorMessage = "Failed to fetch page. Server error: ${response.code}"
                            return@use
                        }
                        val bodyHtml = response.body?.string()
                        if (bodyHtml.isNullOrBlank()) {
                            errorMessage = "Page returned successfully but contains no data."
                            return@use
                        }

                        try {
                            val extractedText = extractArticleContent(bodyHtml)
                            if (extractedText.isBlank()) {
                                errorMessage = "Could not extract any readable article paragraphs from this URL."
                            } else {
                                if (hasExistingText) {
                                    importedContent = extractedText
                                    showConflictResolution = true
                                    activeImportType = null
                                } else {
                                    onImportText(extractedText, "replace")
                                }
                            }
                        } catch (e: Exception) {
                            errorMessage = "Crawl parsing error: ${e.localizedMessage}"
                        }
                    }
                }
            }
        })
    }

    // Process Google Doc plain text export helper
    fun handleGoogleDocImport() {
        val url = inputUrl.trim()
        if (url.isBlank()) {
            errorMessage = "Please enter a Google Docs link."
            return
        }

        // Match Google Doc ID from typical link structures
        val docIdPattern = "document/d/([a-zA-Z0-9-_]+)".toRegex()
        val matchResult = docIdPattern.find(url)
        val docId = matchResult?.groupValues?.get(1)

        if (docId == null) {
            errorMessage = "Invalid Google Doc format. Make sure you copy a standard document sharing link."
            return
        }

        isWebLoading = true
        errorMessage = null

        // Convert sharing link directly to official plain text download export endpoint!
        val exportUrl = "https://docs.google.com/document/d/$docId/export?format=txt"
        val request = Request.Builder()
            .url(exportUrl)
            .build()

        okHttpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                coroutineScope.launch(Dispatchers.Main) {
                    isWebLoading = false
                    errorMessage = "Failed to download Google Doc. Requires internet connection: ${e.localizedMessage}"
                }
            }

            override fun onResponse(call: Call, response: Response) {
                coroutineScope.launch(Dispatchers.Main) {
                    isWebLoading = false
                    response.use {
                        if (response.code == 404) {
                            errorMessage = "Document not found. Ensure link is correct and doc is accessible."
                            return@use
                        }
                        if (!response.isSuccessful) {
                            errorMessage = "Document retrieval failed. Verify Doc privacy is set to 'Anyone with the link can view'."
                            return@use
                        }
                        val bodyText = response.body?.string()
                        if (bodyText.isNullOrBlank()) {
                            errorMessage = "Successfully loaded document but it appears to contain no text."
                        } else {
                            if (hasExistingText) {
                                importedContent = bodyText
                                showConflictResolution = true
                                activeImportType = null
                            } else {
                                onImportText(bodyText, "replace")
                            }
                        }
                    }
                }
            }
        })
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(listOf(ElectricPurple.copy(alpha = 0.8f), ElectricCyan.copy(alpha = 0.5f))),
                    shape = RoundedCornerShape(24.dp)
                )
                .testTag("import_source_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = CosmicSurfaceElevated
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Top Header Row with Close / Back Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (activeImportType != null) {
                            IconButton(
                                onClick = {
                                    activeImportType = null
                                    errorMessage = null
                                    inputUrl = ""
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(CosmicSurface, RoundedCornerShape(10.dp))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowBack,
                                    contentDescription = "Back to selection",
                                    tint = SlateTextPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Text(
                            text = when {
                                showConflictResolution -> "Conflict Options"
                                activeImportType == "url" -> "Import Web Page"
                                activeImportType == "gdoc" -> "Import Google Doc"
                                else -> "Import Script Source"
                            },
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = SlateTextPrimary
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .background(CosmicSurface, RoundedCornerShape(10.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close dialog",
                            tint = SlateTextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Error Messages Display
                if (errorMessage != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFEF4444).copy(alpha = 0.15f))
                            .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                            .testTag("import_error_banner")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Error icon",
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = errorMessage ?: "",
                                color = Color(0xFFEF4444),
                                fontSize = 12.sp,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Loader feedback states
                if (isPdfLoading || isWebLoading) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        LinearProgressIndicator(
                            color = ElectricCyan,
                            trackColor = CosmicBorder,
                            modifier = Modifier
                                .fillMaxWidth(0.8f)
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .testTag("import_loading_indicator")
                        )
                        Text(
                            text = if (isPdfLoading) "Extracting text pages from PDF... 📖" else "Fetching and parsing webpage contents... 🌐",
                            color = SlateTextSecondary,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                        if (isPdfLoading && largeFileWarningMessage != null) {
                            Text(
                                text = largeFileWarningMessage ?: "",
                                color = ElectricPurple,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }
                } else if (!showConflictResolution) {
                    // MAIN ACTION FLOWS
                    when (activeImportType) {
                        null -> {
                            // Display Root choices list
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "Choose an input source. All text is imported into the Sandbox so you can review and tweak it before saving.",
                                    fontSize = 12.sp,
                                    color = SlateTextSecondary,
                                    lineHeight = 16.sp
                                )

                                // 0. BLANK SCRIPT
                                ImportSourceOptionCard(
                                    title = "Create Blank Script",
                                    description = "Start with a clean slate and type your own content",
                                    icon = Icons.Default.Edit,
                                    iconColor = ElectricPurple,
                                    testTag = "import_option_blank",
                                    onClick = {
                                        onDismiss()
                                        onImportText("", "replace")
                                    }
                                )

                                // 1. CLIPBOARD
                                ImportSourceOptionCard(
                                    title = "Paste From Clipboard",
                                    description = "Instantly load what you copied from another application",
                                    icon = Icons.Default.ContentPaste,
                                    iconColor = ElectricPurple,
                                    testTag = "import_option_clipboard",
                                    onClick = {
                                        try {
                                            val text = clipboardManager.getText()?.text
                                            if (!text.isNullOrBlank()) {
                                                if (hasExistingText) {
                                                    importedContent = text
                                                    showConflictResolution = true
                                                } else {
                                                    onImportText(text, "replace")
                                                }
                                            } else {
                                                errorMessage = "Clipboard is empty. Copy script content first!"
                                            }
                                        } catch (e: Exception) {
                                            errorMessage = "Could not access clipboard: ${e.localizedMessage}"
                                        }
                                    }
                                )

                                // 2. TXT FILE PICKER
                                ImportSourceOptionCard(
                                    title = "Browse Text Files (.txt)",
                                    description = "Upload raw .txt document from your device storage",
                                    icon = Icons.Default.FolderOpen,
                                    iconColor = ElectricCyan,
                                    testTag = "import_option_file",
                                    onClick = { filePickerLauncher.launch("text/plain") }
                                )

                                // 3. PDF FILE PICKER
                                ImportSourceOptionCard(
                                    title = "Browse PDF Files (.pdf)",
                                    description = "Extract and combine all text pages from a PDF document",
                                    icon = Icons.Default.Description,
                                    iconColor = WarmAmber,
                                    testTag = "import_option_pdf",
                                    onClick = { pdfPickerLauncher.launch("application/pdf") }
                                )

                                // 4. URL WEB ARTICLES
                                ImportSourceOptionCard(
                                    title = "Import Web Article (URL)",
                                    description = "Fetch article paragraphs cleanly from some blog/news. 🌐 Online only",
                                    icon = Icons.Default.Language,
                                    iconColor = Color(0xFF10B981), // Emerald Green
                                    testTag = "import_option_url",
                                    onClick = {
                                        activeImportType = "url"
                                        errorMessage = null
                                    }
                                )

                                // 5. GOOGLE DOCS IMPORT
                                ImportSourceOptionCard(
                                    title = "Import Google Docs",
                                    description = "Pull document text via Doc Sharing Link. 🌐 Online only",
                                    icon = Icons.Default.CloudDownload,
                                    iconColor = Color(0xFF3B82F6), // Blue Accent
                                    testTag = "import_option_gdoc",
                                    onClick = {
                                        activeImportType = "gdoc"
                                        errorMessage = null
                                    }
                                )
                            }
                        }
                        "url" -> {
                            // URL Subscreen
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(ElectricCyan.copy(alpha = 0.05f))
                                        .border(1.dp, ElectricCyan.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                        .padding(12.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Language,
                                            contentDescription = "Internet network notice",
                                            tint = ElectricCyan,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Column {
                                            Text(
                                                text = "Requires Internet Connection",
                                                color = ElectricCyan,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
                                            )
                                            Text(
                                                text = "The app needs online access to fetch and strip menus/ads from the blog page.",
                                                color = SlateTextSecondary,
                                                fontSize = 11.sp,
                                                lineHeight = 14.sp
                                            )
                                        }
                                    }
                                }

                                OutlinedTextField(
                                    value = inputUrl,
                                    onValueChange = { inputUrl = it },
                                    label = { Text("Web Article or Blog URL", color = SlateTextSecondary) },
                                    placeholder = { Text("https://example.com/blog-post", color = SlateTextSecondary.copy(alpha = 0.5f)) },
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("url_import_input"),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = ElectricCyan,
                                        unfocusedBorderColor = CosmicBorder,
                                        focusedLabelColor = ElectricCyan,
                                        unfocusedLabelColor = SlateTextSecondary,
                                        focusedTextColor = SlateTextPrimary,
                                        unfocusedTextColor = SlateTextPrimary,
                                        focusedContainerColor = CosmicSurface,
                                        unfocusedContainerColor = CosmicSurface
                                    )
                                )

                                Button(
                                    onClick = { handleUrlImport() },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                                    contentPadding = PaddingValues(),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .testTag("url_import_button")
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Brush.linearGradient(listOf(ElectricPurple, DeepViolet))),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("Extract & Review Script", fontWeight = FontWeight.Bold, color = CosmicBackground, fontSize = 14.sp)
                                    }
                                }
                            }
                        }
                        "gdoc" -> {
                            // Google Docs subscreen
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(ElectricCyan.copy(alpha = 0.05f))
                                        .border(1.dp, ElectricCyan.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                        .padding(12.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CloudDownload,
                                            contentDescription = "Google doc online notice",
                                            tint = ElectricCyan,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Column {
                                            Text(
                                                text = "Docs Link: Requires Internet",
                                                color = ElectricCyan,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
                                            )
                                            Text(
                                                text = "Make sure your Google Doc has sharing set to: 'Anyone with the link can view' so the app can sync and export plain text.",
                                                color = SlateTextSecondary,
                                                fontSize = 11.sp,
                                                lineHeight = 14.sp
                                            )
                                        }
                                    }
                                }

                                OutlinedTextField(
                                    value = inputUrl,
                                    onValueChange = { inputUrl = it },
                                    label = { Text("Google Doc Link", color = SlateTextSecondary) },
                                    placeholder = { Text("https://docs.google.com/document/d/.../edit?usp=sharing", color = SlateTextSecondary.copy(alpha = 0.5f)) },
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("gdoc_import_input"),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = ElectricCyan,
                                        unfocusedBorderColor = CosmicBorder,
                                        focusedLabelColor = ElectricCyan,
                                        unfocusedLabelColor = SlateTextSecondary,
                                        focusedTextColor = SlateTextPrimary,
                                        unfocusedTextColor = SlateTextPrimary,
                                        focusedContainerColor = CosmicSurface,
                                        unfocusedContainerColor = CosmicSurface
                                    )
                                )

                                Button(
                                    onClick = { handleGoogleDocImport() },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                                    contentPadding = PaddingValues(),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .testTag("gdoc_import_button")
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Brush.linearGradient(listOf(ElectricPurple, DeepViolet))),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("Download & Review Script", fontWeight = FontWeight.Bold, color = CosmicBackground, fontSize = 14.sp)
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // CONFLICT RESOLUTION UI
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(WarmAmber.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Attention Icon",
                                tint = WarmAmber,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Text(
                            text = "Conflict Detected",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = SlateTextPrimary
                        )

                        Text(
                            text = "You already have drafting content in this script. Do you want to append the imported text to the end, or completely replace the existing text?",
                            fontSize = 13.sp,
                            color = SlateTextSecondary,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    importedContent?.let { onImportText(it, "append") }
                                },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = SlateTextPrimary
                                ),
                                border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                                    brush = Brush.linearGradient(listOf(CosmicBorder, CosmicBorder))
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("resolve_import_append"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Append", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            }

                            Button(
                                onClick = {
                                    importedContent?.let { onImportText(it, "replace") }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.Transparent
                                ),
                                contentPadding = PaddingValues(),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .testTag("resolve_import_replace"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Brush.linearGradient(listOf(ElectricPurple, DeepViolet))),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("Replace", fontWeight = FontWeight.Bold, color = CosmicBackground, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ImportSourceOptionCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    testTag: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CosmicSurface)
            .border(1.dp, CosmicBorder, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(16.dp)
            .testTag(testTag)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = "$title Icon",
                    tint = iconColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = SlateTextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    fontSize = 12.sp,
                    color = SlateTextSecondary
                )
            }
        }
    }
}
