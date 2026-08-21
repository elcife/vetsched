# VETSCHED UI and Navigation Implementation Plan

This plan outlines the steps to implement the Splash, Login, and Registration screens as shown in the provided UI design using Jetpack Compose.

## Proposed Changes

### 1. Dependencies and Configuration
- Add `androidx.navigation:navigation-compose` to the project.
- Update `Color.kt` with the theme colors (VETSCHED Green).

### 2. UI Components & Theme
- **Theme**: Define a custom color palette in `Theme.kt` and `Color.kt` to match the design's green tones.
- **Shared Components**: Create reusable components like custom `TextField`s and `Button`s that match the rounded design.
- **Assets**: Since I don't have the original SVG/PNG files, I will implement the logo and paw print backgrounds using Compose `Canvas` or `Vector` placeholders.

### 3. Screen Implementations
#### [NEW] [StartScreen.kt](file:///E:/flutterprojects/VETSCHED/app/src/main/java/com/example/vetsched/ui/screens/StartScreen.kt)
- Splash screen with logo, "VETSCHED" title, and "Get Started" button.
- Paw print background decoration.

#### [NEW] [LoginScreen.kt](file:///E:/flutterprojects/VETSCHED/app/src/main/java/com/example/vetsched/ui/screens/LoginScreen.kt)
- "Welcome Back" header.
- Input fields for Student Email and Password.
- "Forgot Password?" and "Register" navigation links.
- "Login" button.

#### [NEW] [RegisterScreen.kt](file:///E:/flutterprojects/VETSCHED/app/src/main/java/com/example/vetsched/ui/screens/RegisterScreen.kt)
- "Create Account" header.
- Input fields for Full Name, Student Email, Student ID, and Password.
- "Login" navigation link.
- "Create Account" button.

### 4. Navigation Logic
- Implement a `NavHost` in `MainActivity.kt`.
- Define routes: `start`, `login`, `register`.
- Wire up buttons to navigate between screens.

## Verification Plan

### Automated Tests
- I will verify the UI using `GreetingPreview` equivalent previews for each screen.
- Basic navigation tests can be added if requested.

### Manual Verification
- Deploy the app to a device/emulator.
- Verify that clicking "Get Started" goes to Login.
- Verify that clicking "Register" on Login goes to Register.
- Verify that clicking "Login" on Register goes back to Login.
