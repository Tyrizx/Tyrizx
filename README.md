# Tyrizx

![image](https://github.com/Tyrizx/Tyrizx/actions/workflows/debug.yml/badge.svg)
![MIT](https://img.shields.io/badge/license-MIT-blue.svg)

## The Repository

This repository is where we develop Tyrizx, an Android-optimized distribution of OpenVSCode Server. Our goal is to deliver a fully self-contained, offline-capable code editor as a native Android APK, without requiring Termux, a local server, or any external dependencies.

The source code is available to everyone under the standard MIT license.

## What is Tyrizx

Tyrizx packages the full OpenVSCode Server runtime into a single Android application. It bundles an Android-compatible Node.js binary, all required shared libraries, and the web-based editor frontend into the APK's assets and native libraries.

When the app starts, it extracts the server to internal storage, launches it through Android's system linker, and loads the editor interface in a WebView.

## Features

- Fully offline operation. No network connection or cloud infrastructure required after installation.
- Extension support via the Open VSX Registry.
- Self-contained. No Termux or external server setup.
- Native Android APK targeting ARM64 devices.
- Customizable WebView backend, with an in-progress native alternative called Optima.
- Open source under the MIT license.

## Architecture

| Component | Description |
|-----------|-------------|
| OpenVSCode Server | The upstream editor server, forked and adapted for Android. |
| Node.js (ARM64) | An Android Bionic-compatible Node runtime, sourced from Termux packages. |
| Shared Libraries | Required .so files (libc++, libz, libcrypto, libssl, libuv, and others) bundled alongside the server. |
| Android Wrapper | Kotlin-based MainActivity that extracts assets, configures the environment, and launches the server. |
| WebView Default | Android WebView for rendering the editor interface. A native Rust alternative, Optima, is under development. |

## Building

The project is built automatically through GitHub Actions. The workflow file is located at .github/workflows/debug.yml.

## To build locally:

```bash
./gradlew assembleDebug
```

The resulting APK will be at `app/build/outputs/apk/debug/app-debug.apk`.

## Requirements

- Android 10 (API 29) or newer.
- ARM64 (aarch64) device.
- At least 200 MB of free storage for the app and its runtime files.

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

- OpenVSCode Server – the upstream server this project is based on.
· VSCode – the original source code.
- Optima – a native Rust WebView for Android, developed in parallel.
- Open VSX Registry – the open extension marketplace used by Tyrizx.

## License

Copyright (c) Tyrizx contributors.

Licensed under the MIT license.

Portions of this project are derived from OpenVSCode Server and VS Code, both licensed under the MIT license.
