# F-Droid CI Secret Preparation

Follow these steps to generate and capture the values required by the `fdroid-publish.yml` workflow.

## 1. Create or reuse the release keystore

Generate a new keystore (replace prompts as needed):

```bash
keytool -genkeypair \
  -v \
  -keystore fdroid-release.keystore \
  -alias goober \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000
```

Record the keystore password you enter during the prompts. This becomes the `FDROID_KEYSTORE_PASSWORD` secret. The alias you choose (e.g., `goober`) becomes the `FDROID_KEY_ALIAS` secret. If you set a dedicated key password that differs from the keystore password, capture it for the `FDROID_KEY_PASSWORD` secret; otherwise reuse the keystore password value.

## 2. Base64-encode the keystore

Encode the keystore so it can be stored in a GitHub secret:

```bash
base64 -w0 fdroid-release.keystore > fdroid-release.keystore.b64
```

Open the generated `.b64` file and copy its single-line contents into the `FDROID_KEYSTORE_B64` repository secret. After uploading, delete the temporary `.b64` artifact:

```bash
rm fdroid-release.keystore.b64
```

## 3. Add secrets in GitHub

Create repository secrets with the captured values:

- `FDROID_KEYSTORE_B64`: contents of `fdroid-release.keystore.b64`
- `FDROID_KEYSTORE_PASSWORD`: password set when generating the keystore
- `FDROID_KEY_ALIAS`: alias specified via `-alias`
- `FDROID_KEY_PASSWORD`: key password if different; otherwise reuse the keystore password

With these secrets configured, the CI workflow can sign and publish the APK to the F-Droid Pages repository.
