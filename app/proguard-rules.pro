# Suppress warnings for HTTP server and Desktop classes
-dontwarn com.sun.net.httpserver.Headers
-dontwarn com.sun.net.httpserver.HttpContext
-dontwarn com.sun.net.httpserver.HttpExchange
-dontwarn com.sun.net.httpserver.HttpHandler
-dontwarn com.sun.net.httpserver.HttpServer
-dontwarn java.awt.Desktop$Action
-dontwarn java.awt.Desktop


# Gson specific rules
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**

# Keep Gson classes
-keep class com.google.gson.** { *; }
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken

# Keep your data models
-keep class com.mytaskpro.data.** { *; }
-keep class com.mytaskpro.models.** { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao interface *

# Firebase
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }

# Keep generic signatures
-keepattributes Signature
-keepattributes SourceFile,LineNumberTable
-keepattributes RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations
-keepattributes AnnotationDefault

# Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.lifecycle.HiltViewModel

# Vico Chart Library
-keep class com.patrykandpatrick.vico.** { *; }
-keepclassmembers class com.patrykandpatrick.vico.** { *; }
-keepnames class com.patrykandpatrick.vico.** { *; }

# Keep chart-related classes
-keep class * extends com.patrykandpatrick.vico.core.chart.Chart
-keep class * extends com.patrykandpatrick.vico.core.component.Component
-keep class * extends com.patrykandpatrick.vico.core.entry.ChartEntry

# Compose UI
-keep class androidx.compose.** { *; }
-keepclassmembers class androidx.compose.** { *; }
-keep class androidx.compose.material.** { *; }
-keep class androidx.compose.runtime.** { *; }
-keep class androidx.compose.ui.** { *; }

# Keep your UI components
-keep class com.mytaskpro.ui.** { *; }
-keepclassmembers class com.mytaskpro.ui.** { *; }
-keep class com.mytaskpro.ui.components.** { *; }
-keep class com.mytaskpro.ui.screens.** { *; }
-keep class com.mytaskpro.ui.theme.** { *; }

# ViewModels
-keep class com.mytaskpro.viewmodel.** { *; }
-keepclassmembers class com.mytaskpro.viewmodel.** { *; }

# Keep Note related classes specifically
-keep class com.mytaskpro.data.Note { *; }
-keep class com.mytaskpro.data.NoteDao { *; }
-keepclassmembers class com.mytaskpro.data.Note { *; }
-keepclassmembers class com.mytaskpro.data.NoteDao { *; }

# Keep WorkManager
-keep class androidx.work.** { *; }
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context,androidx.work.WorkerParameters);
}

# Billing
-keep class com.android.billingclient.** { *; }
-keep class com.android.vending.billing.** { *; }
-keep class com.mytaskpro.billing.BillingManager { *; }
-keepclassmembers class com.mytaskpro.billing.BillingManager { *; }

# Keep Coroutines
-keep class kotlinx.coroutines.** { *; }
-keepclassmembers class kotlinx.coroutines.** { *; }

# Keep Calendar related classes
-keep class com.kizitonwose.calendar.** { *; }
-keepclassmembers class com.kizitonwose.calendar.** { *; }

# Keep DataStore
-keep class androidx.datastore.** { *; }
-keep class * extends androidx.datastore.preferences.core.Preferences$Key { *; }

# Keep UI enums and their values
-keepclassmembers enum com.mytaskpro.ui.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
    **[] $VALUES;
    public *;
}

# Keep data models
-keep class com.mytaskpro.data.RepetitiveTaskSettings { *; }
-keepclassmembers class com.mytaskpro.data.RepetitiveTaskSettings {
    *;
}

# Keep the rest of your existing rules...

# Google Calendar API
-keepclassmembers class * {
    @com.google.api.client.util.Key <fields>;
}
-keepclassmembers class * extends com.google.api.client.json.GenericJson {
    <fields>;
}
-keep class com.google.api.services.** { *; }
-keep interface com.google.api.services.** { *; }
-keep class com.google.api.client.** { *; }
-keep class com.google.api.client.json.** { *; }
-keep class com.google.api.client.http.** { *; }
-keep class * extends com.google.api.client.json.GenericJson { *; }
-keepclassmembers class * {
    @com.google.api.client.util.Key <fields>;
    @com.google.api.client.util.Value <fields>;
}

# OAuth specific rules
-keep class com.google.android.gms.auth.** { *; }
-keep class com.google.android.gms.common.** { *; }
-keep class com.google.api.client.googleapis.** { *; }
-keep class com.google.api.client.auth.** { *; }