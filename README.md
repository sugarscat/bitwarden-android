# Bitwarden Android (BETA)

> [!TIP]
> This repo has the new native Android app, currently in [Beta](https://community.bitwarden.com/t/about-the-beta-program/39185). Looking for the legacy .NET MAUI apps? Head on over to [bitwarden/mobile](https://github.com/bitwarden/mobile)

## Contents

- [Compatibility](#compatibility)
- [Setup](#setup)
- [Dependencies](#dependencies)

## Compatibility

- **Minimum SDK**: 29
- **Target SDK**: 34
- **Device Types Supported**: Phone and Tablet
- **Orientations Supported**: Portrait and Landscape

## Setup


1. Clone the repository:

    ```sh
    $ git clone https://github.com/bitwarden/android
    ```

2. Create a `user.properties` file in the root directory of the project and add the following properties:

    - `gitHubToken`: A "classic" Github Personal Access Token (PAT) with the `read:packages` scope (ex: `gitHubToken=gph_xx...xx`). These can be generated by going to the [Github tokens page](https://github.com/settings/tokens). See [the Github Packages user documentation concerning authentication](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-gradle-registry#authenticating-to-github-packages) for more details.
    - `localSdk`: A boolean value to determine if the SDK should be loaded from the local maven artifactory (ex: `localSdk=true`). This is particularly useful when developing new SDK capabilities. Review [Linking SDK to clients](https://contributing.bitwarden.com/getting-started/sdk/#linking-the-sdk-to-clients) for more details.

3. Setup the code style formatter:

    All code must follow the guidelines described in the [Code Style Guidelines document](docs/STYLE_AND_BEST_PRACTICES.md). To aid in adhering to these rules, all contributors should apply `docs/bitwarden-style.xml` as their code style scheme. In IntelliJ / Android Studio:

    - Navigate to `Preferences > Editor > Code Style`.
    - Hit the `Manage` button next to `Scheme`.
    - Select `Import`.
    - Find the `bitwarden-style.xml` file in the project's `docs/` directory.
    - Import "from" `BitwardenStyle` "to" `BitwardenStyle`.
    - Hit `Apply` and `OK` to save the changes and exit Preferences.

    Note that in some cases you may need to restart Android Studio for the changes to take effect.

    All code should be formatted before submitting a pull request. This can be done manually but it can also be helpful to create a macro with a custom keyboard binding to auto-format when saving. In Android Studio on OS X:

    - Select `Edit > Macros > Start Macro Recording`
    - Select `Code > Optimize Imports`
    - Select `Code > Reformat Code`
    - Select `File > Save All`
    - Select `Edit > Macros > Stop Macro Recording`

    This can then be mapped to a set of keys by navigating to `Android Studio > Preferences` and editing the macro under `Keymap` (ex : shift + command + s).

    Please avoid mixing formatting and logical changes in the same commit/PR. When possible, fix any large formatting issues in a separate PR before opening one to make logical changes to the same code. This helps others focus on the meaningful code changes when reviewing the code.

## Dependencies

### Application Dependencies

The following is a list of all third-party dependencies included as part of the application beyond the standard Android SDK.

- **AndroidX Appcompat**
    - https://developer.android.com/jetpack/androidx/releases/appcompat
    - Purpose: Allows access to new APIs on older API versions.
    - License: Apache 2.0

- **AndroidX Autofill**
    - https://developer.android.com/jetpack/androidx/releases/autofill
    - Purpose: Allows access to tools for building inline autofill UI.
    - License: Apache 2.0

- **AndroidX Biometrics**
    - https://developer.android.com/jetpack/androidx/releases/biometric
    - Purpose: Authenticate with biometrics or device credentials.
    - License: Apache 2.0

- **AndroidX Browser**
    - https://developer.android.com/jetpack/androidx/releases/browser
    - Purpose: Displays webpages with the user's default browser.
    - License: Apache 2.0

- **AndroidX CameraX Camera2**
    - https://developer.android.com/jetpack/androidx/releases/camera
    - Purpose: Display and capture images for barcode scanning.
    - License: Apache 2.0

- **AndroidX Compose**
    - https://developer.android.com/jetpack/androidx/releases/compose
    - Purpose: A Kotlin-based declarative UI framework.
    - License: Apache 2.0

- **AndroidX Core SplashScreen**
    - https://developer.android.com/jetpack/androidx/releases/core
    - Purpose: Backwards compatible SplashScreen API implementation.
    - License: Apache 2.0

- **AndroidX Credentials**
    - https://developer.android.com/jetpack/androidx/releases/credentials
    - Purpose: Unified access to user's credentials.
    - License: Apache 2.0

- **AndroidX Lifecycle**
    - https://developer.android.com/jetpack/androidx/releases/lifecycle
    - Purpose: Lifecycle aware components and tooling.
    - License: Apache 2.0

- **AndroidX Room**
    - https://developer.android.com/jetpack/androidx/releases/room
    - Purpose: A convenient SQLite-based persistence layer for Android.
    - License: Apache 2.0

- **AndroidX Security**
    - https://developer.android.com/jetpack/androidx/releases/security
    - Purpose: Safely manage keys and encrypt files and sharedpreferences.
    - License: Apache 2.0

- **AndroidX WorkManager**
    - https://developer.android.com/jetpack/androidx/releases/work
    - Purpose: The WorkManager is used to schedule deferrable, asynchronous tasks that must be run reliably.
    - License: Apache 2.0

- **Dagger Hilt**
    - https://github.com/google/dagger
    - Purpose: Dependency injection framework.
    - License: Apache 2.0

- **Firebase Cloud Messaging**
    - https://github.com/firebase/firebase-android-sdk
    - Purpose: Allows for push notification support. (**NOTE:** This dependency is not included in builds distributed via F-Droid.)
    - License: Apache 2.0

- **Firebase Crashlytics**
    - https://github.com/firebase/firebase-android-sdk
    - Purpose: SDK for crash and non-fatal error reporting. (**NOTE:** This dependency is not included in builds distributed via F-Droid.)
    - License: Apache 2.0

- **Glide**
    - https://github.com/bumptech/glide
    - Purpose: Image loading and caching.
    - License: BSD, part MIT and Apache 2.0

- **kotlinx.collections.immutable**
    - https://github.com/Kotlin/kotlinx.collections.immutable
    - Purpose: Immutable collection interfaces and implementation prototypes for Kotlin.
    - License: Apache 2.0

- **kotlinx.coroutines**
    - https://github.com/Kotlin/kotlinx.coroutines
    - Purpose: Kotlin coroutines library for asynchronous and reactive code.
    - License: Apache 2.0

- **kotlinx.serialization**
    - https://github.com/Kotlin/kotlinx.serialization/
    - Purpose: JSON serialization library for Kotlin.
    - License: Apache 2.0

- **kotlinx.serialization converter**
    - https://github.com/square/retrofit/tree/trunk/retrofit-converters/kotlinx-serialization
    - Purpose: Converter for Retrofit 2 and kotlinx.serialization.
    - License: Apache 2.0

- **OkHttp 3**
    - https://github.com/square/okhttp
    - Purpose: An HTTP client used by the library to intercept and log traffic.
    - License: Apache 2.0

- **Retrofit 2**
    - https://github.com/square/retrofit
    - Purpose: A networking layer interface.
    - License: Apache 2.0

- **zxcvbn4j**
    - https://github.com/nulab/zxcvbn4j
    - Purpose: Password strength estimation.
    - License: MIT

- **ZXing**
    - https://github.com/zxing/zxing
    - Purpose: Barcode scanning and generation.
    - License: Apache 2.0

### Development Environment Dependencies

The following is a list of additional third-party dependencies used as part of the local development environment. This includes test-related artifacts as well as tools related to code quality and linting. These are not present in the final packaged application.

- **detekt**
    - https://github.com/detekt/detekt
    - Purpose: A static code analysis tool for the Kotlin programming language.
    - License: Apache 2.0

- **JUnit 5**
    - https://github.com/junit-team/junit5
    - Purpose: Unit Testing framework for testing application code.
    - License: Eclipse Public License 2.0

- **MockK**
    - https://github.com/mockk/mockk
    - Purpose: Kotlin-friendly mocking library.
    - License: Apache 2.0

- **Robolectric**
    - https://github.com/robolectric/robolectric
    - Purpose: A unit testing framework for code directly depending on the Android framework.
    - License: MIT

- **Turbine**
    - https://github.com/cashapp/turbine
    - Purpose: A small testing library for kotlinx.coroutine's Flow.
    - License: Apache 2.0

### CI/CD Dependencies

The following is a list of additional third-party dependencies used as part of the CI/CD workflows. These are not present in the final packaged application.

- **Fastlane**
    - https://fastlane.tools/
    - Purpose: Automates building, signing, and distributing applications.
    - License: MIT

- **Kover**
    - https://github.com/Kotlin/kotlinx-kover
    - Purpose: Kotlin code coverage toolset.
    - License: Apache 2.0
