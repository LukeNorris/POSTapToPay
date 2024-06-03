# POS Tap to Pay App

## Setup Instructions

To set up the project with your Adyen credentials, you need to add your API key and merchant account to the `local.properties` file.

### Step 1: Create or Update `local.properties` File

In the root directory of your project, locate the `local.properties` file. If it doesn't exist, create it manually.

### Step 2: Add Your Adyen Credentials

Open the `local.properties` file and add your Adyen API key and merchant account name as shown below:
ADYEN_API_KEY=AQExhmfxL4LIaBNE...

ADYEN_MERCHANT_ACCOUNT=MyMerchantAccountName

### Note

The `local.properties` file is included in the `.gitignore` file, so it won't be tracked by Git. This means that each person who clones the repository will need to create or update this file with their own Adyen credentials.

### Example `local.properties` File
sdk.dir=/path/to/android/sdk

ADYEN_API_KEY=AQExhmfxL4LIaBNE...

ADYEN_MERCHANT_ACCOUNT=MyMerchantAccountName

### Additional Information

Ensure you do not commit your `local.properties` file to the repository as it contains sensitive information.