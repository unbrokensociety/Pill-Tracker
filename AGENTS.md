# Project Rules & Rules for Updates

1. **Fixed Application ID**: Never change `applicationId` in `app/build.gradle.kts`. It must remain `com.aistudio.meditracker.zqxpr` to ensure seamless APK updates without package signature/ID conflicts on Android devices.
2. **Sequential Versioning**: Version code and name are dynamically controlled via GitHub Actions (`GITHUB_RUN_NUMBER`) resulting in `v1.x` releases.
3. **Database & Backward Compatibility**: Maintain Room schema compatibility so local user data is never wiped during updates.
