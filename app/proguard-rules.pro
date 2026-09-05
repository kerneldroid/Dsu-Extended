-dontobfuscate

# Glance 1.1.1 pulls work-runtime 2.7.1, which predates the R8 full-mode
# constructor-keep fix (work 2.10.0). WorkManagerInitializer auto-runs via
# androidx.startup before Application.onCreate, and Room instantiates
# WorkDatabase_Impl reflectively through its no-arg constructor. AGP 9.x
# runs R8 full mode, which strips that constructor and kills the app at
# startup with "Failed to create an instance of WorkDatabase".
# Keep the reflective surface explicitly.
-keepclassmembers class * extends androidx.room.RoomDatabase {
  <init>();
}
-keep class androidx.work.impl.WorkDatabase_Impl {
  *;
}
-keepclassmembers class * extends androidx.work.InputMerger {
  <init>();
}
-keepclassmembers public class * extends androidx.work.ListenableWorker {
  public <init>(android.content.Context, androidx.work.WorkerParameters);
}
-keep class androidx.work.WorkerParameters

# Glance update/tap paths are reflection- + WorkManager-driven, and R8 full
# mode strips their members: SessionWorker (updates via updateAll) and the
# app's ActionCallback impl (tap -> BroadcastReceiver.newInstance) both die
# silently while direct add (UnmanagedSessionReceiver) keeps working.
# Keep the whole Glance runtime and the widget package with members.
-keep class com.dsu.extended.widget.** {
  *;
}
-keep class androidx.glance.** {
  *;
}
