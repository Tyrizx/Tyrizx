# Tyrizx

![Debug APK](https://github.com/Tyrizx/Tyrizx/actions/workflows/debug.yml/badge.svg)
![License](https://img.shields.io/badge/license-MIT-blue.svg)

## The Repository

This repository is where we develop Tyrizx, an Android-optimized distribution of [OpenVSCode Server](https://github.com/gitpod-io/openvscode-server). Our goal is to deliver a fully self-contained, offline-capable code editor as a native Android APK, without requiring Termux, a local server, or any external dependencies.

The source code is available to everyone under the standard [MIT license](LICENSE.txt).

## What is Tyrizx

Tyrizx packages the full OpenVSCode Server runtime into a single Android application. It embeds a Node.js runtime as a shared library, bundles the web-based editor frontend into the APK's assets, and loads the editor interface in a WebView.

When the app starts, it extracts the server to internal storage, launches it through a JNI bridge to `node::Start()`, and loads the editor interface once the server is ready.

## Features

- Fully offline operation. No network connection or cloud infrastructure required after installation.
- Extension support via the [Open VSX Registry](https://open-vsx.org).
- Self-contained. No Termux or external server setup.
- Native Android APK with per-ABI splits for `arm64-v8a`, `armeabi-v7a`, and `x86_64`. no X86 sorry :(
- Minimal APK size through ABI splitting.
- Open source under the MIT license.

## Architecture

| Component | Description |
| :--- | :--- |
| **OpenVSCode Server** | The upstream editor server, bundled as-is from the official ARM64 tarball. |
| **Node.js Runtime** | Provided by [nodejs-mobile](https://github.com/nodejs-mobile/nodejs-mobile), a fork of Node.js designed for embedding on mobile platforms. |
| **JNI Bridge** | A small C++ layer (`native-lib.cpp`) that exposes `node::Start()` to Kotlin. |
| **Shared Libraries** | `libnode.so` for each supported ABI, bundled inside `app/src/main/cpp/libnode/bin/`. |
| **Android Wrapper** | Kotlin-based `MainActivity` that extracts assets, starts the Node.js runtime, and hosts the WebView. |
| **WebView** | Android WebView for rendering the editor interface. A native Rust alternative, [Optima](https://github.com/SolaraStudio/Optima), is under development. |

## How It Works

1. On first launch, `MainActivity` extracts the `nodejs-project/` folder from the APK's assets to internal storage.
2. It then calls `startNodeWithArguments()` — a JNI method implemented in `native-lib.cpp` — which invokes `node::Start()` with `main.js` as the entry point.
3. `main.js` sets `process.argv` and dynamically imports `out/server-main.js`, the OpenVSCode Server entry point.
4. Once the server is listening on `127.0.0.1:8080`, the WebView loads the editor interface.

## Building

The project is built automatically through GitHub Actions. The workflow file is located at `.github/workflows/debug.yml`.

To build locally:

```bash
./gradlew assembleDebug
```

The resulting APKs will be in `app/build/outputs/apk/debug/`, split by ABI:

- `app-arm64-v8a-debug.apk`
- `app-armeabi-v7a-debug.apk`
- `app-x86_64-debug.apk`

Requirements

- Android 10 (API 29) or newer is recommended.
- Supported ABIs: arm64-v8a, armeabi-v7a, x86_64. no X86 sorry :(
- At least 300 MB of free storage for the app and its runtime files.
- NDK 26.1.10909125 (used by the build, available on GitHub Actions runners).

Project Structure

```
Tyrizx/
├── app/
│   ├── build.gradle.kts
│   └── src/main/
│       ├── assets/nodejs-project/     # OpenVSCode Server + main.js
│       ├── cpp/
│       │   ├── CMakeLists.txt
│       │   ├── native-lib.cpp          # JNI bridge
│       │   └── libnode/
│       │       ├── bin/<abi>/libnode.so
│       │       └── include/node/       # Node.js headers
│       └── java/io/tyrizx/
│           └── MainActivity.kt
├── .github/workflows/
│   ├── debug.yml
│   └── release.yml
├── build.gradle.kts
├── gradle.properties
└── settings.gradle.kts
```

## Contributing

There are several ways to participate in this project:

- Submit bugs and feature requests on the Issues page.
- Review source code changes in Pull Requests.
- Improve documentation.
- Test builds on different Android devices and report compatibility.

If you are interested in contributing directly to the codebase, please open an issue first to discuss the change.

## Feedback

- File an issue on GitHub.
- Join the discussion in GitHub Discussions.

## Related Projects

- OpenVSCode Server — the upstream server this project is based on.
- VSCode — the original source code.
- nodejs-mobile — the embedded Node.js runtime used by Tyrizx.
- Optima — a native Rust WebView for Android, developed in parallel.
- Open VSX Registry — the open extension marketplace used by Tyrizx.

## License

Copyright (c) Tyrizx contributors.

Licensed under the MIT license.

Portions of this project are derived from OpenVSCode Server and VS Code, both licensed under the MIT license.