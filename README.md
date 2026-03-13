# KMP Navigation

A **Kotlin Multiplatform navigation library** that enables shared navigation logic across multiple platforms.

The library is designed to allow navigation to be triggered directly from **ViewModels**, avoiding UI-driven "ping-pong navigation patterns" and enabling navigation decisions based on complex business logic.

## Overview

KMP Navigation provides a unified way to handle navigation across different platforms in a Kotlin Multiplatform project.

Instead of relying solely on UI layers to control navigation, this library allows navigation to be handled within **ViewModels**, making it possible to react to complex application logic and state changes.

This approach helps avoid situations where the UI needs to repeatedly delegate navigation decisions back to the ViewModel (the so-called *ping-pong behavior*).

## Features

* Kotlin Multiplatform compatible
* Centralized navigation logic
* ViewModel-driven navigation
* Optional **Koin integration**
* Works with custom Dependency Injection setups
* Avoids UI/ViewModel navigation ping-pong patterns
* Platform-agnostic navigation handling

## Dependency Injection

The library can be integrated into your project in two different ways.

### 1. Using the provided Koin modules

If your project uses **Koin**, the library provides ready-to-use modules that can simply be added to your DI setup.

```kotlin
modules(
    navigationModule()
)
```

### 2. Using a custom DI setup

If you are not using Koin, the navigation system can be created manually and integrated into your own dependency injection framework.

```kotlin
val navigation = NavigationFactory.create()
```

This allows the navigation instance to be injected into **ViewModels** or other components that require navigation capabilities.

## Example Usage

Example inside a ViewModel:

```kotlin
class ExampleViewModel(
    private val navigation: Navigation
) {

    fun onLoginSuccess() {
        navigation.navigate(Destination.Home)
    }
}
```

Example inside a Composable:

```kotlin
@Composable
fun Foo() { 
    val navigation = rememberNavigation()
    
    navigation.navigateTo(Destination.Home)
}
```

This enables navigation to be triggered from application logic instead of relying solely on UI events.

## Supported Platforms

The library is designed for **Kotlin Multiplatform** and can be used on:

* Android
* iOS
* JVM
* Other supported KMP targets depending on your project configuration

## Contributing

This repository currently does not accept public contributions.

If you have questions, suggestions, or encounter issues, please open an issue or contact the author directly.

## License / Usage Restrictions

Copyright (c) 2026 [Emrah Cicek]

All rights reserved.

This library and its source code are the intellectual property of the author.

No part of this software may be used, copied, modified, merged, published, distributed, sublicensed, or integrated into other software without **explicit prior permission from the author**.

Permission must be obtained before:

* Using this library in private or personal projects
* Using this library in commercial applications
* Redistributing the source code or binaries
* Creating derivative works

To request permission for usage, please contact the author.

Unauthorized use of this software is strictly prohibited.

## Disclaimer

This software is provided **"as is"**, without warranty of any kind, express or implied.
The author shall not be liable for any damages arising from the use of this software.

## License

Copyright (c) 2026 DevMrE

All Rights Reserved.

This library may not be used, copied, modified, modified, distributed,
or integrated into other software without explicit permission from the author.

See the [LICENSE](./LICENSE) file for more details.