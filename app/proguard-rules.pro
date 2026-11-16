# Add project-specific ProGuard rules here.
# You can control the set of unused code to shrink and the set of obfuscated code
# with these directives.

# Keep data classes and enums
-keep class jp.ruma.freesoft.kotlinlesson.callConfirmation.enum.LockMode { *; }

# Keep CallRedirectionService and Activities so system/telecom framework can instantiate them
-keep public class jp.ruma.freesoft.kotlinlesson.callConfirmation.RedirectionServiceImpl { *; }
-keep public class jp.ruma.freesoft.kotlinlesson.callConfirmation.ui.main.MainActivity { *; }
-keep public class jp.ruma.freesoft.kotlinlesson.callConfirmation.ui.confirm.CallConfirmActivity { *; }

# Strip Log.d, Log.v, Log.i calls in release build
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}

# Strip all calls to SecureLogger in release build
-assumenosideeffects class jp.ruma.freesoft.**.SecureLogger {
    *;
}

# Strip debug function
-assumenosideeffects class jp.ruma.freesoft.kotlinlesson.callConfirmation.service.CallConfirmKeepAliveService {
    private debugKeepAliveMessageTask();
}