# Dsu Extended

Modern, feature-rich DSU (Dynamic System Update) installer for Android. Designed for ease of use and maximum compatibility.

| Install | Partitions | Inspector | Logs |
| :---: | :---: | :---: | :---: |
| ![Install](docs/images/install.webp) | ![Partitions](docs/images/partitions.webp) | ![Inspector](docs/images/inspector.webp) | ![Logs](docs/images/logs.webp) |

## Features
- **Root/Shizuku/Dhizuku Support:** Works across various privilege levels.
- **Expressive UI:** Modern Material 3 Design with fluid animations.
- **Custom Font Support:** Includes Google Sans Code and Flex integration.
- **Pre-install compatibility check:** Reads the image, verifies Treble/ABI/SDK/VNDK, storage and partition layout before writing anything, then shows an explicit risk report.
- **GSI-only diagnostics:** Logs capture DSU/GSI-related logcat and kernel lines instead of the whole system log.
- **SDK 37 Ready:** Built with the latest Android 17 toolchain.

## Build Requirements
- **JDK 26**
- **Gradle 9.7**
- **Android SDK 37 (Cinnamon Bun)**

## Credits
- **Main Developer:** [@kerneldroid](https://github.com/kerneldroid)
- **Original Fork Author:** [@senodroid](https://github.com/senodroid)
- **Partition Manager inspiration:** [DSU-Sideloader-Plus](https://github.com/yangFenTuoZi/DSU-Sideloader-Plus) by [@yangFenTuoZi](https://github.com/yangFenTuoZi)
