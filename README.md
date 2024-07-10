# POS Tap to Pay

## Local Properties Setup

To run this project, you need to add your Adyen credentials to the `local.properties` file.

1. Open the `local.properties` file (located in the root directory of the project).
2. Add your credentials as shown here:

ADYEN_API_KEY=your_api_key_here

ADYEN_MERCHANT_ACCOUNT=your_merchant_account_here

KEY_IDENTIFIER=your-key-identifier

PASSPHRASE=your-passphrase

KEY_VERSION=1


## How to Run

1. Clone the repository.
2. Open the project in Android Studio.
3. Ensure the `local.properties` file contains your Adyen credentials as described above.
4. Build and run the project.
5. Make sure your API credential also has a Client key created (this is for Monitoring & Attestation purposes)
6. On your merchant account, set the TFM property androidTapToPay.enable = true (Your Adyen contact can enable this)
7. on your API key set the POS permission: Adyen Payments App role(Your Adyen contact can enable this)


## Notes

- The `local.properties` file should not be included in version control and is listed in `.gitignore`.
- Ensure you have the appropriate SDK and dependencies installed.