# Repository Guidance

This repository implements the Jakarta Interceptors specification on top of Micronaut's compile-time AOP. It was
generated from [micronaut-project-template](https://github.com/micronaut-projects/micronaut-project-template); files
under `.agents/skills/`, `.github/`, `config/`, and the Gradle wrapper are synced from that template.

## Repository Shape

- `jakarta-interceptors/` is the runtime module: the `InvocationContext` implementation and the Micronaut interceptor
  adapters.
- `jakarta-interceptors-processor/` is the annotation processor that validates interceptor methods, resolves bindings,
  and drives proxy generation at compile time.
- `jakarta-interceptors-bom/` publishes the BOM. Keep dependency-management changes separate from implementation
  changes when possible.
- `buildSrc/src/main/groovy/io.micronaut.build.internal.jakarta-interceptors-*.gradle` holds the convention plugins.
- `cdi-tck-interceptors/` and `cdi-tck-support/` run the CDI TCK interceptor tests against this implementation.
- `test-suite-java/`, `test-suite-groovy/`, `test-suite-kotlin/`, and `test-suite-graalvm/` cover each supported
  language and the native image.
- `.agents/skills/` is shared agent guidance synced from the template.

## Template And Sync Rules

- `.github/workflows/`, `.agents/skills/`, `config/`, and the files listed in the template's `files-sync.yml` are
  overwritten by the upstream sync. Fix them in `micronaut-project-template`, not here.
- `.github/workflows/.rsync-filter` records which template workflows are deliberately not synced downstream.
- To pin a synced file against the sync, add a sibling `<file>.lock`.

## Contributing Guidelines

- Before opening or updating a pull request, read `CONTRIBUTING.md` and follow every repo-specific PR requirement it
  names.
- Treat contributor-checklist items as handoff requirements. If a requirement is not applicable, state that explicitly
  in the PR description or handoff note.

## Compile-Time Contract

- Interception is resolved during annotation processing. An interceptor method must be reachable as a Micronaut
  executable method; the runtime never falls back to reflection.
- Anything the specification requires to be rejected should be rejected by the processor with a compile error, and the
  rejection should be covered by a processor test.
- New behavior needs coverage in `test-suite-java` at a minimum, plus the Groovy and Kotlin suites when the behavior
  depends on the language's element model.

## Documentation

- User guide sources live in `src/main/docs/guide`, with navigation in `src/main/docs/guide/toc.yml`.
- Build the guide with `./gradlew publishGuide` (or `./gradlew pG`); build the guide plus Javadocs with `./gradlew docs`.
- `src/main/docs/guide/conformance.adoc` and `limitations.adoc` track where this implementation stands against the
  specification. Update them when conformance changes.
- Release-note behavior is maintained through `.github/release.yml`, `.github/workflows/release.yml`, and the release
  process documented in `MAINTAINING.md`.

## Verification

- Use `./gradlew check` for general validation.
- Use `./gradlew :cdi-tck-interceptors:test` for the TCK.
- Use `./gradlew :test-suite-graalvm:nativeTest` to confirm nothing reached for reflection.
- Use `./gradlew publishGuide` after guide or `toc.yml` changes.
