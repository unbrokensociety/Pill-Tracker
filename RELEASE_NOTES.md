# 🚀 PillTracker v1.22 — Release Notes

### 🌟 What's New in Version 1.22

* 🔐 **Seamless One-Click Email Verification**: Removed manual code entry fields! Simply click the confirmation link in your email inbox.
* ⚡ **Automatic Verification Detection**: The app automatically polls your email status in the background and logs you in instantly the moment your link is confirmed.
* 📜 **Mandatory Privacy & Legal Terms Consent**: Added a mandatory Privacy Policy checkbox during registration to ensure explicit user consent before account creation.
* 🛡 **Enterprise-Grade Legal Privacy Terms**: Updated the Privacy Policy dialog with comprehensive legal clauses covering GDPR/CCPA compliance, medical disclaimers, limitation of liability, and data ownership rights.
* 🗄 **Immutable Consent Audit Logging**: Consent timestamps, device fingerprints, and agreement records are automatically saved to Firebase Cloud Firestore for audit compliance.
* 🚀 **Instant Access (Offline Mode)**: Added a **"Continue (Offline Mode)"** option allowing immediate application access while waiting for verification emails.
* ⚙️ **CI/CD Workflow Repair**: Fixed `.github/workflows/android.yml` to preserve `RELEASE_NOTES.md` across automated GitHub Actions APK release pipelines.

---

### 📦 Build Information
- **App Version**: `1.22`
- **Package Name**: `com.aistudio.meditracker.zqxpr`
- **Target SDK**: Android 14 (API 34)
