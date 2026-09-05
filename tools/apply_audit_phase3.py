from pathlib import Path
import re


def read(path: str) -> str:
    return Path(path).read_text(encoding="utf-8")


def write(path: str, text: str) -> None:
    Path(path).write_text(text, encoding="utf-8")


def replace_once(text: str, old: str, new: str, label: str) -> str:
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f"{label}: expected one match, found {count}")
    return text.replace(old, new, 1)


# Home: remove index-based entrance latency and enlarge the create-folder target.
path = "app/src/main/java/com/example/ui/screens/HomeScreen.kt"
text = read(path)
text = replace_once(
    text,
    '''                        itemsIndexed(sortedFiltered, key = { _, item -> item.id }) { index, item ->
                            var isItemVisible by remember { mutableStateOf(false) }
                            LaunchedEffect(key1 = item.id) {
                                kotlinx.coroutines.delay(index * 45L)
                                isItemVisible = true
                            }
                            androidx.compose.animation.AnimatedVisibility(
                                visible = isItemVisible,
                                enter = slideInVertically(
                                    initialOffsetY = { 60 },
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessLow
                                    )
                                ) + fadeIn(animationSpec = tween(250)),
                                exit = fadeOut(animationSpec = tween(150)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                val dismissState = rememberSwipeToDismissBoxState(''',
    '''                        items(sortedFiltered, key = { item -> item.id }) { item ->
                            val dismissState = rememberSwipeToDismissBoxState(''',
    "remove staggered script-card entrance",
)
text = replace_once(
    text,
    '''                                }
                            )
                            }
                        }
                    }
                }''',
    '''                                }
                            )
                        }
                    }
                }''',
    "remove animated visibility closing brace",
)
text = replace_once(text, '.size(34.dp)\n                                .clip(CircleShape)', '.size(48.dp)\n                                .clip(CircleShape)', 'add-folder touch target')
write(path, text)


# LanguageManager: folder deletion preserves scripts; update every supported locale.
path = "app/src/main/java/com/example/util/LanguageManager.kt"
text = read(path)
translations = [
    "Delete this folder? Scripts inside it will be kept and moved to All Scripts.",
    "¿Eliminar esta carpeta? Los guiones que contiene se conservarán y pasarán a Todos los guiones.",
    "Excluir esta pasta? Os roteiros nela serão mantidos e movidos para Todos os roteiros.",
    "यह फ़ोल्डर हटाएँ? इसके अंदर की स्क्रिप्ट सुरक्षित रहेंगी और सभी स्क्रिप्ट में चली जाएँगी।",
    "حذف هذا المجلد؟ ستبقى النصوص الموجودة بداخله وسيتم نقلها إلى كل النصوص.",
    "Supprimer ce dossier ? Les scripts qu’il contient seront conservés et déplacés vers Tous les scripts.",
    "Diesen Ordner löschen? Die enthaltenen Skripte bleiben erhalten und werden zu Alle Skripte verschoben.",
    "Hapus folder ini? Skrip di dalamnya akan tetap disimpan dan dipindahkan ke Semua Skrip.",
    "Bu klasör silinsin mi? İçindeki metinler korunacak ve Tüm Metinler bölümüne taşınacak.",
    "このフォルダーを削除しますか？中のスクリプトは保持され、「すべてのスクリプト」に移動します。",
]
pattern = re.compile(r'("delete_folder_confirm" to ")[^"]*(")')
index = 0

def replace_translation(match: re.Match[str]) -> str:
    global index
    if index >= len(translations):
        raise RuntimeError("Found more delete_folder_confirm translations than expected")
    value = translations[index]
    index += 1
    return match.group(1) + value + match.group(2)

text = pattern.sub(replace_translation, text)
if index != len(translations):
    raise RuntimeError(f"Expected {len(translations)} delete_folder_confirm translations, updated {index}")
write(path, text)

print("Phase 3 home and localization patch applied")
