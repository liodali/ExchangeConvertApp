# Shared UI icons

Dinarly vendors only the **54 Material icons used by the shared UI**, instead of
linking the full `compose.materialIconsExtended` catalog into Kotlin/Native.
The legacy Android app's separate Material icon dependencies are unchanged.

## Artwork and license

The definitions and vector builders come from these Maven Central source artifacts:

- `org.jetbrains.compose.material:material-icons-core:1.7.3:sources`
- `org.jetbrains.compose.material:material-icons-extended:1.7.3:sources`

This is the same icon version previously resolved by the shared module. The
upstream Android Open Source Project copyright and Apache-2.0 headers are retained
in each Kotlin file. The license is included in `LICENSE.txt`.

Only package names change in the selected icon definitions. Paths, dimensions,
vector names, lazy reuse, and `autoMirror` flags remain identical. The builder
helpers retain upstream's defaults. `Icons` exposes only the two styles we use.
Existing non-mirrored variants remain non-mirrored; changing their RTL behavior
is outside this dependency-removal change.

## Adding or updating icons

1. Add the desired `Icons.Outlined.Name` or `Icons.AutoMirrored.Outlined.Name`
   usage with an import under `dali.hamza.shared.ui.icons`.
2. Download the pinned sources (only needed for maintenance, never normal builds):

   ```sh
   curl -fL https://repo.maven.apache.org/maven2/org/jetbrains/compose/material/material-icons-core/1.7.3/material-icons-core-1.7.3-sources.jar -o /tmp/icons-core-sources.jar
   curl -fL https://repo.maven.apache.org/maven2/org/jetbrains/compose/material/material-icons-extended/1.7.3/material-icons-extended-1.7.3-sources.jar -o /tmp/icons-extended-sources.jar
   ```

3. From the repository root, regenerate and verify:

   ```sh
   python3 script/vendor-shared-icons.py --core-sources /tmp/icons-core-sources.jar --extended-sources /tmp/icons-extended-sources.jar
   python3 script/vendor-shared-icons.py --core-sources /tmp/icons-core-sources.jar --extended-sources /tmp/icons-extended-sources.jar --check
   ```

   The script verifies the SHA-256 of both sources JARs and rejects unexpected
   versions. It discovers the used icon set from shared UI source, preserves the
   original source definitions, and reports unused vendored files for explicit
   removal. Do not restore the full icon dependency to add a single icon.

## Verification and performance

- The vendor script's `--check` compares every definition against pinned upstream
  sources after package relocation, protecting the artwork and mirroring flags.
- `IconsTest` checks dimensions, mirroring, and vector reuse for representative
  icons, including every auto-mirrored variant.
- Inspect `:shared:dependencies --configuration iosArm64CompileKlibraries` and
  `iosSimulatorArm64CompileKlibraries` to ensure Material icon libraries are absent.
- Measure Release framework linking on the same runner before/after, with a
  framework-cache miss in both cases. No build-time reduction is assumed until
  measured. This change deliberately leaves compiler optimization settings alone.

Rollback is source-only: restore the previous shared dependency/imports and remove
this directory, the vendor script, and its tests. No app data or schema changes.
