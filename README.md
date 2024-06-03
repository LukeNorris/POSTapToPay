# POS Tap to Pay

## Local Properties Setup

To run this project, you need to add your Adyen credentials to the `local.properties` file.

1. Open the `local.properties` file (located in the root directory of the project).
2. Add your Adyen API key and merchant account name:

ADYEN_API_KEY=your_api_key_here

ADYEN_MERCHANT_ACCOUNT=your_merchant_account_here


## How to Run

1. Clone the repository.
2. Open the project in Android Studio.
3. Ensure the `local.properties` file contains your Adyen credentials as described above.
4. Build and run the project.

## Overview

This project follows Clean Architecture principles with a focus on separation of concerns. It consists of the following layers:

- **Data Layer**: Manages all data operations including API calls and local database access.
- **Presentation Layer**: Handles the UI and user interactions using Jetpack Compose and ViewModel.

## Notes

- The `local.properties` file should not be included in version control and is listed in `.gitignore`.
- Ensure you have the appropriate SDK and dependencies installed.