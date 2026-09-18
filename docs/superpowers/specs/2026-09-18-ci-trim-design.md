# CI trim

PR and `main` CI stop building throwaway-signed Android App Bundles. Quality stays the merge gate. Signed AABs come only from **Play release**.

## Workflows

| Workflow | When it runs | What it does |
| --- | --- | --- |
| `CI` (`.github/workflows/ci.yml`) | every PR and every push to `main` | `quality` only: `:app:ktlintCheck`, `:app:lintDebug`, `:app:testDebugUnitTest`, `:app:assembleDebug` |
| `Play release` | `v*` tags or manual dispatch | upload-key AAB; optional Play publish |
| `Privacy policy pages` | `main` when `docs/play/site/**` (or the workflow file) changes | GitHub Pages deploy |

Remove the `play-bundle` job from `ci.yml`. No throwaway key generation, no `ALLOW_CI_SIGNING`, no `app-release-ci` artifact.

`Play release` and `Privacy policy pages` stay unchanged.

If GitHub branch protection still requires a check named `play-bundle`, drop that required check after merge. Otherwise PRs stay blocked.

## Leftover throwaway-key protection

The CI escape hatch goes away. The guard stays.

In `app/build.gradle.kts`:

- Delete `ALLOW_CI_SIGNING` / `allowCiSigning`.
- Keep fail-fast on `signReleaseBundle`, `packageRelease`, and `assembleRelease` when `keystore.properties` points at `ci-keystore` or alias `ci`.
- Error text tells the operator to run `./scripts/create-upload-keystore.sh`. It does not mention a CI override.

`scripts/create-upload-keystore.sh` already refuses to overwrite a CI keystore; leave that as-is. Keep `ci-keystore/` in `.gitignore`.

Release signing for Play is unchanged: **Play release** decodes the upload key from GitHub secrets. Local `./gradlew :app:bundleRelease` still requires a real `keystore.properties`.

## Docs

Update `docs/play/README.md`:

- PR/`main` CI is quality only.
- Signed AABs come from **Play release** (`v*` tags or manual dispatch), or from a local `bundleRelease` with the upload key.
- Remove the “do not upload `app-release-ci`” warning. That artifact will not exist.
- Keep a leftover-key recovery note: if Play Console says the certificate expires too soon, an old `ci-keystore` (`CN=PinAlarmLock CI`, 2-day validity) was used. Create a real upload key (`./scripts/create-upload-keystore.sh`, 10000-day validity) and rebuild. Google still requires the upload certificate to remain valid after 22 October 2033. Do not describe this as a current CI artifact.

Root `README.md` build commands stay as they are.

## Out of scope

- Path-filtering or otherwise shrinking the `quality` job
- Combining or renaming workflows
- Changing when **Play release** runs
- Android product code

## Verification

1. `ci.yml` has a single `quality` job.
2. `play-release.yml` and `pages.yml` are unchanged.
3. `./gradlew :app:ktlintCheck :app:lintDebug :app:testDebugUnitTest :app:assembleDebug` still matches what CI runs.
4. A `keystore.properties` that points at `ci-keystore` / alias `ci` still fails `bundleRelease`, with no `ALLOW_CI_SIGNING` bypass.
5. After merge, drop `play-bundle` from required checks if it is still listed.
