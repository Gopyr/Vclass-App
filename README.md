# V-Class Gunadarma for Android

Vclass-App is an Android companion app for V-Class Gunadarma. It brings course information, forums, quizzes, grades, calendar items, assignments, account settings, and selected notifications into a Jetpack Compose interface.

## Current status

This is a personal/community project and should be treated as an evolving app rather than an official university product. The application depends on the availability and behavior of the upstream V-Class services. Test accounts and real student data should never be committed to the repository.

## Features

| Area | Description |
| --- | --- |
| Dashboard | Course and deadline overview |
| Courses | Course detail and assignment navigation |
| Forums | Read forum threads and replies |
| Quizzes | Read quiz information using configurable matching rules |
| Grades | View available grade information |
| Calendar | Track academic events and deadlines |
| Remote configuration | Update selected quiz/forum rules and patch notes through JSON metadata |
| Updates | Public release metadata can be consumed by the app's updater |

## Build requirements

| Requirement | Version |
| --- | --- |
| Android Studio | Hedgehog or newer recommended |
| JDK | 17 |
| Android compile SDK | 36 |
| Minimum Android version | API 24 |

The project includes the Gradle wrapper, so a separate Gradle installation is not required.

## Build the debug APK

```bash
./gradlew assembleDebug
```

The APK is written to `app/build/outputs/apk/debug/app-debug.apk`. Install it on a test device or emulator with an appropriate Android debugging setup.

## Configuration and data

The app reads selected behavior from `releases/remote_config.json` and update metadata from `releases/update.json`. These files are public and must contain only non-sensitive configuration. Review changes carefully because a remote configuration update can change how content is classified in the app without shipping a new APK.

Network access and upstream login behavior are implementation details of the current app. Do not add credentials, session cookies, or private API responses to source control.

## Release checklist

Before publishing a release, update `versionCode` and `versionName`, review `CHANGELOG.md`, build and install the APK, verify the update metadata, and confirm that the release artifact does not contain test credentials or private data. Release signing should use a proper private key outside the repository; the debug signing configuration is for development only.

## Known limitations

The app is not affiliated with or endorsed by Universitas Gunadarma unless explicitly stated by the project owner. Upstream APIs may change without notice, and some features may require an authenticated V-Class account. The repository currently does not promise compatibility with every Android device or V-Class deployment.

## License

Released under the [MIT License](LICENSE).
