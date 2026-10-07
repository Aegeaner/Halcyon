# Release minification keeps everything the app uses through direct references, so no keep rules
# are needed. OkHttp probes optional platform integrations at runtime; silence those references.
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
