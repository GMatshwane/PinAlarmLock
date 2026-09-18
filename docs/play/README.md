# Play Store distribution

PinAlarmLock is set up to ship as a signed Android App Bundle (`com.pinalarmlock.app`) to Google Play. This file is the operator checklist. Console clicks and the $25 developer registration still have to be done in a browser with your Google account.

Privacy policy URL after GitHub Pages is enabled:

`https://gmatshwane.github.io/PinAlarmLock/privacy.html`

## What the repo already does

- Play application id `com.pinalarmlock.app` (not `com.example.*`).
- Adaptive launcher icon.
- Release builds minify, shrink resources, and sign from `keystore.properties`.
- CI `quality` job runs ktlint, Android lint, unit tests, and `assembleDebug` on every pull request and every push to `main`.
- `Play release` workflow builds the upload-key AAB on `v*` tags or manual dispatch, then publishes to Play when `PLAY_SERVICE_ACCOUNT_JSON` is set.
- Listing copy, Data safety answers, permission declarations, and screenshots live under `play/`.

## Policy notes before you pay the Play fee

App lockers that overlay other apps are reviewed strictly. This project already avoids `QUERY_ALL_PACKAGES` (enrolment uses the launcher/home `<queries>` declarations). You still must declare Usage access, Display over other apps, and the special-use foreground service. Google can reject overlay lockers even when the forms are complete.

## 1. Create the Play app

1. Open [Play Console](https://play.google.com/console) and create an app named **PIN Alarm Lock**, default language English (US), app type **App**, free.
2. Category: **Tools** (or Productivity). Email: your developer contact.
3. Complete the declarations: ads = no, news = no, COVID = no, data-safety (copy `docs/play/data-safety.md`), target audience (not primarily children; 13+), content rating (`docs/play/content-rating.md`), news/newsstand = no.

## 2. Upload key and Play App Signing

On your machine (once):

```bash
chmod +x scripts/create-upload-keystore.sh
./scripts/create-upload-keystore.sh
```

That writes gitignored `upload-keystore.jks` and `keystore.properties`. Back them up. Then:

```bash
./gradlew :app:bundleRelease
```

Upload `app/build/outputs/bundle/release/app-release.aab` to an **internal testing** release. Let Play App Signing generate the app signing key. Keep the local JKS as the **upload** key.

GitHub secrets (Settings → Secrets and variables → Actions):

| Secret | Value |
| --- | --- |
| `ANDROID_KEYSTORE_BASE64` | `base64 < upload-keystore.jks \| tr -d '\n'` |
| `ANDROID_KEYSTORE_PASSWORD` | keystore password |
| `ANDROID_KEY_ALIAS` | `upload` |
| `ANDROID_KEY_PASSWORD` | key password |
| `PLAY_SERVICE_ACCOUNT_JSON` | optional; Play Developer API service-account JSON |

Without `PLAY_SERVICE_ACCOUNT_JSON`, the workflow still uploads the signed AAB as a GitHub artifact for manual Console upload.

## 3. Play Developer API (optional auto-upload)

1. In Play Console: Setup → API access → create/link a Google Cloud project.
2. Create a service account with **Release to testing tracks** (and later production if you want).
3. Download the JSON key into `PLAY_SERVICE_ACCOUNT_JSON`.
4. Enable **Google Play Android Developer API**.
5. First API upload must stay `draft` until the Play app leaves the Draft state (listing, rating, and a manual testing-track release).

## 4. Store listing assets

Paste from `play/listing/en-US/`:

- Title, short description, full description
- High-res icon `play/listing/en-US/images/icon/512.png`
- Feature graphic `play/listing/en-US/images/featureGraphic/1024x500.png`
- Phone screenshots `play/listing/en-US/images/phoneScreenshots/`
- Privacy policy URL above
- App category / contact email / website (GitHub repo is fine)

Regenerate graphics with `python3 scripts/generate-play-graphics.py`.

## 5. Restricted permissions

Fill Play Console forms using `docs/play/permissions-declaration.md`. Short video for Usage access / overlay: record enrolment → open an enrolled app → PIN pad covers it.

## 6. Ship a version

- Manual: `./gradlew :app:bundleRelease` and upload the AAB.
- CI: tag `v1.0.0` (or run **Play release**) after secrets are set.
- `VERSION_CODE` is `github.run_number`. `VERSION_NAME` is the tag without the leading `v`, or `1.0.<run_number>` on manual dispatch.

Do not upload the CI job artifact named `app-release-ci`; that bundle is signed with a throwaway key.

If Play Console says the certificate **expires too soon**, the AAB was signed with that CI key (`CN=PinAlarmLock CI`, 2-day validity). Create a real upload key (`./scripts/create-upload-keystore.sh`, 10000-day validity), run `./gradlew :app:bundleRelease`, and upload the new `app-release.aab`. Google requires the upload certificate to remain valid after 22 October 2033.
