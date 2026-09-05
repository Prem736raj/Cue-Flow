from pathlib import Path
import re


def read(path: str) -> str:
    return Path(path).read_text(encoding="utf-8")


def write(path: str, text: str) -> None:
    Path(path).write_text(text, encoding="utf-8")


def replace_once(text: str, old: str, new: str, label: str) -> str:
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f"{label}: expected exactly one match, found {count}")
    return text.replace(old, new, 1)


def sub_once(text: str, pattern: str, repl: str, label: str) -> str:
    new_text, count = re.subn(pattern, repl, text, count=1, flags=re.S)
    if count != 1:
        raise RuntimeError(f"{label}: expected exactly one regex match, found {count}")
    return new_text


# Home: remove client-side AI entry points and correct trust-sensitive copy.
path = "app/src/main/java/com/example/ui/screens/HomeScreen.kt"
text = read(path)
text = replace_once(text, '    var showAiPromptGenerator by remember { mutableStateOf(false) }\n', '', 'home ai state')
text = replace_once(
    text,
    '''                LogoHeader(\n                    onImportClick = { showImportDialog = true },\n                    onAiGenerateClick = { showAiPromptGenerator = true },\n                    onVoiceRecordClick = { showVoiceToScriptDialog = true },\n                    onSettingsClick = { showSettingsDialog = true }\n                )''',
    '''                LogoHeader(\n                    onVoiceRecordClick = { showVoiceToScriptDialog = true },\n                    onSettingsClick = { showSettingsDialog = true }\n                )''',
    'home header args',
)
text = replace_once(text, ',\n                                onAiGenerateClick = { showAiPromptGenerator = true }', '', 'home empty ai callback')
text = sub_once(
    text,
    r'\n    if \(showAiPromptGenerator\) \{.*?\n    \}\n\n    if \(showVoiceToScriptDialog\)',
    '\n\n    if (showVoiceToScriptDialog)',
    'home ai dialog',
)
text = replace_once(text, 'Text("Rate 5 Stars on Play Store", color = CosmicBackground, fontWeight = FontWeight.Bold, fontSize = 13.sp)', 'Text("Rate CueFlow on Play Store", color = CosmicBackground, fontWeight = FontWeight.Bold, fontSize = 13.sp)', 'neutral rating button')
text = text.replace('whats_new_dismissed_v1_3', 'whats_new_dismissed_v1_0')
text = replace_once(
    text,
    'text = "${LanguageManager.get("delete_folder_confirm")}\\n\\n(${folder.name})",',
    'text = "Delete folder \\\"${folder.name}\\\"? Scripts in this folder will be kept and moved back to All Scripts.",',
    'folder deletion copy',
)
write(path, text)

# Empty state: remove the unsafe cloud-AI CTA while keeping create/import/templates.
path = "app/src/main/java/com/example/ui/components/EmptyState.kt"
text = read(path)
text = text.replace('import androidx.compose.foundation.BorderStroke\n', '')
text = text.replace('import androidx.compose.material.icons.filled.AutoAwesome\n', '')
text = text.replace('import androidx.compose.material3.OutlinedButton\n', '')
text = replace_once(text, '    onTemplateSelect: (title: String, content: String, speed: Int, size: Int) -> Unit,\n    onAiGenerateClick: () -> Unit,\n', '    onTemplateSelect: (title: String, content: String, speed: Int, size: Int) -> Unit,\n', 'empty ai callback')
text = replace_once(text, 'Create your first script or let AI draft one for your camera presentation.', 'Create your first script, import existing notes, or start from a quick template.', 'empty copy')
text = sub_once(
    text,
    r'\n            // 2\. Secondary Action: AI Writer\n            OutlinedButton\(.*?\n            \}\n\n            // 3\. Subtle Import Option',
    '\n\n            // Secondary import option',
    'empty ai button',
)
write(path, text)

# Editor: remove all APK-embedded Gemini functionality and fix save/not-found behavior.
path = "app/src/main/java/com/example/ui/screens/EditorScreen.kt"
text = read(path)
for line in [
    'import com.example.BuildConfig\n',
    'import okhttp3.MediaType.Companion.toMediaType\n',
    'import okhttp3.RequestBody.Companion.toRequestBody\n',
    'import org.json.JSONArray\n',
    'import org.json.JSONObject\n',
]:
    text = text.replace(line, '')
text = sub_once(
    text,
    r'\n    // AI Edit/Enhancement UI States\n.*?\n    val coroutineScope = rememberCoroutineScope\(\)',
    '\n\n    val coroutineScope = rememberCoroutineScope()',
    'editor ai state',
)
text = replace_once(
    text,
    '''            if (script != null) {\n                existingScript = script''',
    '''            if (script == null) {\n                isLoading = false\n                onBack()\n                return@LaunchedEffect\n            }\n            existingScript = script''',
    'editor not found',
)
text = replace_once(text, '            }\n            isLoading = false\n        }\n    }\n\n    // Direct save function', '            isLoading = false\n        }\n    }\n\n    // Direct save function', 'editor fetch closing brace')
text = replace_once(text, '        if (title.isNotBlank() || content.isNotBlank()) {', '        if (existingScript != null || title.isNotBlank() || content.isNotBlank()) {', 'editor empty existing save')
text = sub_once(
    text,
    r'\n    // AI Refinement and Text Generation Execution Block\n.*?\n    // Premium Auto-Save debouncer',
    '\n\n    // Auto-save debouncer',
    'editor ai execution',
)
text = sub_once(
    text,
    r'\n                        // Undo/Revert Banner for AI modifications\n.*?\n                        // Script Core Body Text Area',
    '\n\n                        // Script Core Body Text Area',
    'editor ai toolbar',
)
text = sub_once(
    text,
    r'\n    // AI Tone Selection sub-dialog\n.*?\n}\n\nfun parseColorSafely',
    '\n}\n\nfun parseColorSafely',
    'editor ai dialogs',
)
write(path, text)

# Wi-Fi remote: fix header parsing so blank-line termination never reads into the body.
path = "app/src/main/java/com/example/util/WifiRemoteServer.kt"
text = read(path)
old = '''                val headers = linkedMapOf<String, String>()\n                repeat(MAX_HEADERS) {\n                    val line = reader.readLine() ?: return@repeat\n                    if (line.isEmpty()) return@repeat\n                    if (line.length > MAX_HEADER_LINE) {\n                        writeResponse(writer, 431, "text/plain; charset=utf-8", "Request header too large")\n                        return\n                    }\n                    val colon = line.indexOf(':')\n                    if (colon > 0) {\n                        headers[line.substring(0, colon).trim().lowercase()] = line.substring(colon + 1).trim()\n                    }\n                }'''
new = '''                val headers = linkedMapOf<String, String>()\n                var headerCount = 0\n                while (headerCount < MAX_HEADERS) {\n                    val line = reader.readLine() ?: break\n                    if (line.isEmpty()) break\n                    if (line.length > MAX_HEADER_LINE) {\n                        writeResponse(writer, 431, "text/plain; charset=utf-8", "Request header too large")\n                        return\n                    }\n                    val colon = line.indexOf(':')\n                    if (colon > 0) {\n                        headers[line.substring(0, colon).trim().lowercase()] = line.substring(colon + 1).trim()\n                    }\n                    headerCount++\n                }'''
text = replace_once(text, old, new, 'remote header parser')
write(path, text)

print('Phase 1 patches applied successfully')
