# Preserve worker names both for migrated and newly scheduled WorkManager jobs.
-keep class com.emmanuela.launcher.DigestWorker { public <init>(android.content.Context, androidx.work.WorkerParameters); }
-keep class com.emmanuela.launcher.platform.DigestWorker { public <init>(android.content.Context, androidx.work.WorkerParameters); }
-keep class com.emmanuela.launcher.platform.SessionReminderWorker { public <init>(android.content.Context, androidx.work.WorkerParameters); }
