# F-Droid Publishing via GitHub Pages

This guide walks through the one-time setup needed for the `Publish F-Droid Repo` workflow so it can build, sign, and publish releases to a GitHub Pages–hosted custom repository.

## 1. Prepare the `gh-pages` branch

1. Checkout or create the `gh-pages` branch in the same repository as the Android app:
   ```bash
   git checkout --orphan gh-pages
   git reset --hard
   ```
2. Initialise the F-Droid repo structure:
   ```bash
   pip install fdroidserver
   fdroid init
   ```
   This creates `config.yml`, `metadata/`, and `repo/`. Adjust `config.yml` values (e.g., repo name, URL, description) to match your branding.
3. Commit and push the branch:
   ```bash
   git add config.yml metadata repo
   git commit -m "Initial F-Droid repo"
   git push origin gh-pages
   ```
4. Enable GitHub Pages in the repository settings, selecting the `gh-pages` branch as the source. Copy the published URL; you will need it to configure client devices later.

## 2. Configure workflow secrets

Generate the Android signing artifacts and add the required secrets following `docs/FDROID_SECRETS.md`. The workflow expects:

- `FDROID_KEYSTORE_B64`
- `FDROID_KEYSTORE_PASSWORD`
- `FDROID_KEY_ALIAS`
- `FDROID_KEY_PASSWORD`

Ensure the keystore matches the one used for official builds so users receive updates seamlessly.

## 3. Review `.github/workflows/fdroid-publish.yml`

Confirm the workflow settings align with your project:

- `FDROID_APP_ID` matches `applicationId` in `GooberEats/app/build.gradle`.
- `BUILD_TOOLS_VERSION` is available in the Android SDK.
- The workflow uses `gh-pages`; update the branch if you host the repo elsewhere.
- Dependencies (`fdroidserver`, Android SDK) install successfully in your environment (run locally if in doubt).

## 4. Run an initial publish

1. Push a tag matching the `v*` pattern or dispatch the workflow manually. When triggering manually, supply a `release_tag` for the APK filename if desired.
2. Monitor the workflow to ensure it:
   - Builds the release APK (`./gradlew assembleRelease`),
   - Signs it with the secrets,
   - Copies it into `fdroid-repo/repo`,
   - Regenerates metadata/index files via `fdroid update`,
   - Commits and pushes to `gh-pages`.
3. After the workflow completes, verify the GitHub Pages site now includes the updated `index.jar`, `index.xml`, and APK.

## 5. Validate distribution

1. On a test device, add the GitHub Pages URL as a custom repository in an F-Droid client.
2. Refresh the catalogue to confirm your app appears with the expected version.
3. Install the build and verify updates arrive with new releases.

Once these steps are complete, every tagged release (or manual dispatch) will update the F-Droid repository automatically. Refer back to this guide if you rotate signing keys, change branch conventions, or reconfigure GitHub Pages.
