# Sample

Template feature module targeting **Android**, **JavaScript in the browser**, and **Kotlin/Wasm in the browser**. Its
Kotlin API can be consumed from Kotlin Multiplatform projects, and its exported function can be consumed from
JavaScript/TypeScript.

## JavaScript / TypeScript

The `js` and `wasmJs` targets are configured as libraries with generated TypeScript definitions. `sampleMessage()` is
annotated with `@JsExport` in `commonMain`, so the same function is exported by both web targets:

```ts
import { sampleMessage } from "@infomaniak/multiplatform-sample";

const message: string = sampleMessage();
```

The common implementation delegates platform-specific behavior to `platform()`. Its Android implementation is in
`androidMain`; the JS and Wasm targets share the `webMain` implementation.

Build the JavaScript npm package:

```bash
./gradlew :features:Sample:jsBrowserProductionLibraryDistribution
```

The package (`package.json`, `.mjs` and `.d.mts` files) is generated in
`features/Sample/build/dist/js/productionLibrary/`. To try it locally in another project:
`npm install <path-to>/features/Sample/build/dist/js/productionLibrary`.

The Wasm library can be built with:

```bash
./gradlew :features:Sample:wasmJsBrowserProductionLibraryDistribution
```

## Publishing

The module is released independently (Gradle artifact `com.infomaniak.multiplatform_core:sample`, tag
`sample-<version>`), see [Publishing](../../docs/PUBLISHING.md). Publication of the npm package
`@infomaniak/multiplatform-sample` is enabled in `release-please-config.json`.

## Tests

`commonTest` contains tests shared by the targets. `webTest` contains tests run by both browser targets:

```bash
./gradlew :features:Sample:allTests
```

The browser tests (`jsBrowserTest` and `wasmJsBrowserTest`) run headless via Karma and require Chrome or Chromium. The
PR workflow runs `wasmJsBrowserTest` in a separate job with Chrome for Testing.

## Yarn lock file

The Kotlin/JS tooling resolves npm dependencies with Yarn and stores the lock file in `kotlin-js-store/yarn.lock`
(committed). If the JS dependencies change, update it with:

```bash
./gradlew kotlinUpgradeYarnLock
```
