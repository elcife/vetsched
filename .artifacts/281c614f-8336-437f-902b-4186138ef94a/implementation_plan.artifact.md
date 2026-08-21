# VETSCHED Project Cleanup & Organization Plan

Since we are sticking with **XML and Fragments**, this plan will remove the unused Jetpack Compose files and organize your code so it's easier for you and your team to work on.

## User Review Required

> [!IMPORTANT]
> I will be deleting the `ui/theme` folder. This folder contains Jetpack Compose code that is not being used in your XML-based project.

## Proposed Changes

### 1. Cleanup (Removing Compose Clutter)
- **[DELETE]** `ui/theme/Color.kt`
- **[DELETE]** `ui/theme/Theme.kt`
- **[DELETE]** `ui/theme/Type.kt`
- **[MODIFY]** [build.gradle.kts](file:///E:/flutterprojects/VETSCHED/app/build.gradle.kts): Remove Compose plugins and dependencies to simplify the build.

### 2. File Organization
- I will move the Fragments into a sub-package to keep the `ui` folder clean once you add more screens.
- **[NEW]** `ui/fragments/` package for `LoginFragment`, `RegisterFragment`, and `StartFragment`.

### 3. Adding Functionality (The "Brain")
- To fix the "mess" of having no logic, I will introduce a `ViewModel`. This is the standard Android way to handle logic (like checking passwords) outside of the UI files.
- **[NEW]** `ui/viewmodels/AuthViewModel.kt`: A central place to handle Login and Registration logic.

## Verification Plan

### Automated Tests
- I will run `./gradlew assembleDebug` to ensure the project still builds after removing the Compose clutter.

### Manual Verification
- Verify the app still opens to the "Start" screen.
- Verify that clicking "Get Started" and "Register" still works.
