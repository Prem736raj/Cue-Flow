from pathlib import Path

path = Path("app/src/main/java/com/example/ui/components/ScriptDialogs.kt")
text = path.read_text(encoding="utf-8")

old = '''                                                    if (!hasCameraPermission) {
                                                        cameraPermissionLauncher.launch(
                                                            arrayOf(
                                                                android.Manifest.permission.CAMERA,
                                                                android.Manifest.permission.RECORD_AUDIO
                                                            )
                                                        )
                                                    } else {
                                                        val usableSpace = context.filesDir.usableSpace
                                                        if (!isRecording && usableSpace < 50 * 1024 * 1024) {
                                                            storageWarningMessage = "Could not start recording because phone storage space is critically low (less than 50MB free)."
                                                        } else {
                                                            isRecording = !isRecording
                                                            if (isRecording && !isPlaying) {
                                                                isPlaying = true
                                                            }
                                                        }
                                                    }'''

new = '''                                                    if (!hasCameraPermission) {
                                                        cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                                                    } else if (isRecording) {
                                                        isRecording = false
                                                    } else {
                                                        requestRecordingStart()
                                                    }'''

count = text.count(old)
if count != 1:
    raise RuntimeError(f"Expected one remaining duplicated record-control branch, found {count}")

text = text.replace(old, new, 1)
path.write_text(text, encoding="utf-8")
print("Fixed remaining duplicated recording control")
