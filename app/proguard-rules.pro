# CueFlow project-specific R8 rules.
#
# AndroidX Room, CameraX, Compose, OkHttp, ZXing, JSoup and PDFBox Android ship their own
# consumer rules where required. Avoid broad -keep rules here so release shrinking remains useful.

# Keep enough source metadata to make Play Console and locally symbolicated crash traces useful.
-keepattributes SourceFile,LineNumberTable

# Keep annotations used by Android/Jetpack tooling and generated code.
-keepattributes RuntimeVisibleAnnotations,RuntimeInvisibleAnnotations,AnnotationDefault

# These integrations are optional at runtime. PDFBox can decode common PDFs without
# the optional JPEG-2000 provider, and OkHttp selects platform TLS providers only when
# their optional classes are present. R8 should not fail the release build for absent
# provider implementations that are never bundled by CueFlow.
-dontwarn com.gemalto.jp2.JP2Decoder
-dontwarn org.bouncycastle.jsse.BCSSLParameters
-dontwarn org.bouncycastle.jsse.BCSSLSocket
-dontwarn org.bouncycastle.jsse.provider.BouncyCastleJsseProvider
-dontwarn org.conscrypt.Conscrypt$Version
-dontwarn org.conscrypt.Conscrypt
-dontwarn org.conscrypt.ConscryptHostnameVerifier
-dontwarn org.openjsse.javax.net.ssl.SSLParameters
-dontwarn org.openjsse.javax.net.ssl.SSLSocket
-dontwarn org.openjsse.net.ssl.OpenJSSE
