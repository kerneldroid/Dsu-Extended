<!-- Title format: "type(scope): short summary" — e.g. "fix(shizuku): don't crash when binder is absent" -->

## Summary
<!-- What does this PR change and why? Link related issues with "Fixes #123". -->

## Type of change
- [ ] Bug fix (non-breaking change that fixes incorrect behavior)
- [ ] New feature (non-breaking change that adds functionality)
- [ ] Refactor (no behavior change)
- [ ] Dependency / build update (gradle, AGP, libraries, CI)
- [ ] Documentation / translations
- [ ] Breaking change (fix or feature that changes existing behavior or requires migration)

## Privilege modes affected
<!-- Tick every mode whose code path this PR touches or that you verified. -->
- [ ] Root
- [ ] Shizuku
- [ ] Dhizuku
- [ ] ADB (unrooted)
- [ ] System (Magisk module / privileged)
- [ ] None / not mode-specific

## How was it tested?
<!-- Commands run and result, e.g. `./gradlew assembleMiniDebug lintMiniDebug` — pass/fail. -->
- Command(s):
- Device / emulator (model, Android version):
- Manual verification:

## Checklist
- [ ] Self-review of the diff completed
- [ ] No debug logging, dead code, or unrelated formatting changes left behind
- [ ] Code comments exist only where they explain non-obvious logic
- [ ] User-facing strings added/changed in **both** `values/strings.xml` and `values-ru/strings.xml`
- [ ] `other/updater.json` / module metadata NOT touched unless this is a release PR
- [ ] Existing features in untouched modes still behave as before
