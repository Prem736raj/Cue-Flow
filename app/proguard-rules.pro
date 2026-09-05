# CueFlow project-specific R8 rules.
#
# AndroidX Room, CameraX, Compose, OkHttp, ZXing, JSoup and PDFBox Android ship their own
# consumer rules where required. Avoid broad -keep rules here so release shrinking remains useful.

# Keep enough source metadata to make Play Console and locally symbolicated crash traces useful.
-keepattributes SourceFile,LineNumberTable

# Keep annotations used by Android/Jetpack tooling and generated code.
-keepattributes RuntimeVisibleAnnotations,RuntimeInvisibleAnnotations,AnnotationDefault
