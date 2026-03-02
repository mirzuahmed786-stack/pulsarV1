# Keep kotlinx serialization
-keepclassmembers class ** { @kotlinx.serialization.SerialName *; }
-keepclassmembers class ** { @kotlinx.serialization.Serializable *; }
