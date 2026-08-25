# Contributing to DSU Extended

Issues and pull requests are open. Quality bar is enforced: low-effort reports and AI-generated filler are closed without discussion.

## Reporting a bug

- Use the **bug report** issue form and fill every required field.
- Attach logs: the app's Logs tab export or `adb logcat` output captured while reproducing.
- State your exact setup: device, Android version/ROM, operation mode (Root/Shizuku/Dhizuku/ADB/System), root solution.
- One problem per issue. No "+1" or "same here" comments — react with 👍 instead.

## Requesting a feature

- Use the **feature request** form. Describe the real-world scenario, not just the idea.
- Read the DSU constraints listed in the form first; requests that hit hard platform walls will be closed.

## Pull requests

- Keep the diff scoped to one change. No drive-by refactors, reformatting, or unrelated fixes.
- Follow existing code style; comments are allowed only where they explain non-obvious logic.
- User-facing strings go into both `values/strings.xml` and `values-ru/strings.xml`.
- Verify your build before pushing: `./gradlew assembleDebug`.
- Fill the PR template completely, including which privilege modes you tested.

By participating you agree to follow the project's code of conduct: be precise, be technical, be respectful.
