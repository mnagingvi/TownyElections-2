## What does this PR change?

<!-- One or two sentences. Link the issue it fixes, e.g. "Fixes #12". -->

## Which part of the plugin does it touch?

<!-- Tick everything that applies. -->

- [ ] Election logic / counting (`model/`)
- [ ] Election Night presentation (`night/`)
- [ ] Commands, menus or messages
- [ ] Config, permissions or storage
- [ ] Placeholders or integrations (Towny / PlaceholderAPI / bStats)
- [ ] CI, docs or repo plumbing

## How was it tested?

<!--
CI compiles the plugin and runs the unit tests, but it cannot join a server.
Please say what you actually ran, e.g.:
  - `mvn package` locally
  - a paper 1.21.4 test server with Towny 0.102.x and two towns
  - a fresh config vs. an upgraded 1.2 config
-->

## Checklist

- [ ] `mvn package` passes (or CI is green on this branch).
- [ ] New or changed behaviour has a test where the logic is pure Java
      (`InstantRunoff`, `Election`, `DurationUtil`, `TextUtil`).
- [ ] Config changes are reflected in `config.yml`, the README table and
      `config-version` was bumped if existing installs need migrating.
- [ ] User-facing text goes through the message files, not hard-coded strings.
- [ ] `CHANGELOG.md` has an entry under `Unreleased` or the version being cut.
- [ ] I have read [CONTRIBUTING.md](../CONTRIBUTING.md) and the
      [Code of Conduct](../CODE_OF_CONDUCT.md).
