---
name: release
description: Prepare a release of KTamilCalendar — version bump, signed build, demo/README refresh, and (only with the user's approval) commit and push. Use when the user asks to ship, publish, or push changes.
---

# Release checklist

1. Tests + release build pass (build-install skill). Release lint ignores `ExpiredTargetSdkVersion` on purpose (targetSdk 30 is a product requirement; Play needs newer).
2. Bump `versionCode`/`versionName` in `app/build.gradle.kts`.
3. Signed APK needs `keystore.properties` (git-ignored). Never commit keys, `local.properties`, build output.
4. If the UI changed noticeably, re-record `docs/demo.mp4` / `docs/demo.gif` (e2e-test skill) and update README sections.
5. Audit staged files: `git diff --cached --name-only | grep -iE "local\.properties|keystore|\.jks|/build/|\.apk"` must be empty.
6. **Ask the user before committing and before pushing.** Commit message ends with the attribution line given in the session. Push with `git -c credential.helper= -c credential.helper='!gh auth git-credential' push origin main`.
7. Offer to install on the user's TV (`tools/install_tv.sh 10.0.0.174`) — don't do it unasked.
