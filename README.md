# Image to PDF Converter – Fast

A modern Android application built with Jetpack Compose and Material 3 that allows users to convert multiple images into a single PDF document.

## Features

### Core Features
- **Multi-Image Selection**: Pick multiple images using the native Android image picker
- **Image Rearranging**: Easily reorder selected images before conversion
- **PDF Generation**: Convert images to PDF using Android's PdfDocument API
- **Preview**: View and review images before creating the PDF
- **Save to Device**: Save PDFs directly to device Downloads folder using MediaStore
- **Share PDFs**: Share generated PDFs with other apps using FileProvider
- **Privacy-Friendly**: No storage permissions required - uses modern Android APIs

### Pro Features (In-App Purchase)
- **Unlimited Images**: Free version limited to 5 images, Pro version has no limits
- **No Watermark**: Remove the "FREE VERSION" watermark from generated PDFs
- **Restore Purchases**: Restore your Pro purchase on multiple devices

## Technical Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose with Material 3
- **Architecture**: MVVM (Model-View-ViewModel)
- **Minimum SDK**: 24 (Android 7.0)
- **Target SDK**: 34 (Android 14)
- **Build System**: Gradle (Groovy)
- **Navigation**: Navigation Compose
- **Billing**: Google Play Billing Library 7.x

## Project Structure

```
app/src/main/java/com/zaheer/imagetopdf/
├── MainActivity.kt                    # Single Activity with Navigation
├── billing/
│   └── BillingManager.kt             # Google Play Billing integration
├── domain/
│   └── PdfGenerator.kt               # Core PDF generation logic
├── ui/
│   ├── screens/
│   │   ├── HomeScreen.kt             # Main screen for image selection
│   │   └── PreviewScreen.kt          # PDF preview and save/share
│   ├── theme/
│   │   ├── Color.kt                  # Material 3 color definitions
│   │   ├── Theme.kt                  # App theme configuration
│   │   └── Type.kt                   # Typography definitions
│   └── viewmodel/
│       └── ImageToPdfViewModel.kt    # State management
└── utils/
    ├── FileUtils.kt                  # File operations and MediaStore
    └── ImageUtils.kt                 # Image loading and processing
```

## Dependencies

- **AndroidX Core**: Core Android libraries
- **Jetpack Compose**: Modern UI toolkit
- **Material 3**: Latest Material Design components
- **Navigation Compose**: Type-safe navigation
- **Lifecycle**: ViewModel and StateFlow support
- **Billing-KTX**: Google Play In-App Billing
- **Coroutines**: Asynchronous programming

## Building the Project

1. Clone the repository:
   ```bash
   git clone https://github.com/zaheerabbas7892034214-ai/image-to-pdf-converter.git
   ```

2. Open the project in Android Studio

3. Sync Gradle dependencies

4. Build and run on an emulator or physical device

## Google Play Billing Setup

To test in-app purchases:

1. Set up your app in Google Play Console
2. Create an in-app product with ID: `pdf_pro_unlock`
3. Add test accounts in Google Play Console
4. Build a signed release and upload to internal testing track

## License

This project is created for demonstration purposes.

## Package Information

- **Package Name**: com.zaheer.imagetopdf
- **Application ID**: com.zaheer.imagetopdf
