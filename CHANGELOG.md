# Changelog

Notable changes to **TownyElections-2** are recorded here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and the project uses
semantic versioning. Older releases are listed on the
[releases page](https://github.com/mnagingvi/TownyElections-2/releases).

## [2.0.0] — unreleased

The repository release: 2.0.0 puts the plugin version in line with the
**TownyElections-2** name and turns the repo around it into something that can
take contributions. Nothing about elections, config or permissions changed -
`config-version` stays at **2** and existing installs need no migration.

### Added

- **Unit tests** for the logic that is pure Java: the instant-runoff counter,
  ballot handling and tallies, duration parsing and colour codes. `mvn package`
  runs them, CI uploads the Surefire reports.
- A **build report** on every workflow run and as a sticky comment on pull
  requests: version, jar size and a per-class test table.
- A **Lint** workflow running `scripts/check_messages.py` (locale parity,
  `{placeholders}`, message keys used from Java, `config.yml`/`plugin.yml`
  sanity) plus actionlint over `.github/workflows`.
- **CodeQL** security-and-quality scanning on pull requests, on `main` and
  weekly, and **Dependabot** for Maven and GitHub Actions updates.
- Repository automation: a managed label set with path-based pull-request
  labelling, a welcome comment for first-time contributors, and a stale bot for
  quiet issues and pull requests.
- **AI-written release notes**: `scripts/ai_release_notes.py` summarises the
  commits since the previous tag through OpenRouter, and a manual
  *Release notes preview* workflow lets the wording be checked before a tag.
- `SECURITY.md`, `CODE_OF_CONDUCT.md`, a pull request template, `RELEASING.md`,
  and a table in `CONTRIBUTING.md` describing what each workflow checks.

### Fixed

- `plugin.yml` declared `townyelections.player` one indentation level too deep,
  which turned it into a stray key inside the `townyelections.*` permission
  instead of a permission of its own.
- `messages_es.yml` still carried the profile and nation keys removed in 1.3.0,
  plus a dead `{profile}` placeholder on nine strings.
- `gui.stale-menu-closed` was missing from the German, Spanish, French and
  Brazilian Portuguese files, so those locales fell back to English.

### Changed

- Pushing a `v*` tag refuses to publish when the tag disagrees with the
  `<version>` in `pom.xml`, and now attaches the generated notes and the jar.
- Workflows run on `checkout@v7`, `setup-java@v6`, `upload-artifact@v7`,
  `action-gh-release@v3` and `ubuntu-24.04`.

## [1.3.0] — 2026-11-08

### Added

- **Election Night** — concluded elections can be presented as a staged live
  results reveal instead of an instant winner message:
  - an opening title card with the turnout,
  - candidate-by-candidate reveals with a party-coloured bar chart and a boss-bar
    scoreboard,
  - round-by-round counting for `RANKED_CHOICE` elections, including eliminations
    and exhausted ballots,
  - a winner reveal with titles, sound cues, a server-wide announcement and
    party-coloured fireworks above every viewer,
  - a closing summary with the final turnout and any ranks/mayor the winner
    received.
- `/election night` to replay the last recorded result as a show, and
  `/election night skip [town]` (admin) to fast-forward a running show. Every
  sub-command literal stays configurable via `commands.night`.
- A **firework button** on the election desk GUI that replays the last result.
- PlaceholderAPI placeholders `%townyelections_last_winner_votes%`,
  `%townyelections_last_turnout%` and `%townyelections_night_running%`.
- `election-night` configuration section: reveal order, stage/intro/winner
  delays, boss bar, titles, sounds, fireworks count, server-wide broadcast, and
  `defer-announcements` to withhold the usual instant winner messages.
- 30 new message keys, translated for `en`, `de`, `es`, `fr` and `pt-br`.

### Changed

- Winner rewards are applied before any announcement and returned as a summary,
  so the show can echo them at the end of its reveal instead of spoiling it.
- bStats metrics now report to plugin id **35211**.
- The update checker defaults to `mnagingvi/TownyElections-2`, and its GitHub
  User-Agent follows the configured repository instead of a hardcoded path.
- `plugin.yml` website, README badges, CONTRIBUTING links and the Discussions
  contact link now point at this repository.
- `config-version` bumped to **2**.
- README defaults refreshed so the documented values match the shipped config.

### Fixed

- `plugin.yml` declared `townyelections.player` one indentation level too deep,
  which turned it into a stray key inside the `townyelections.*` permission
  instead of a permission of its own.

### Tooling

- Unit tests for the parts that are pure Java: the instant-runoff counter,
  ballot handling and tallies, duration parsing and colour codes. They run as
  part of `mvn package`, and CI uploads the Surefire reports.
- CodeQL (security-and-quality queries) on pushes and pull requests to `main`,
  plus weekly. Dependabot keeps Maven and action versions fresh; Paper and Towny
  are bumped by hand.
- Workflows moved to `checkout@v7`, `setup-java@v6`, `upload-artifact@v7` and
  `action-gh-release@v3`, pinned to `ubuntu-24.04`, with superseded runs
  cancelled and Maven errors surfaced as annotations.
- A build report on every run and pull request: version, jar size and a
  per-class test table, written to the job summary and posted once per PR.
- A `Lint` workflow: `scripts/check_messages.py` checks every locale against
  `messages_en.yml` (keys and `{placeholders}`), that message keys used from
  Java exist, and that `config.yml`/`plugin.yml` are sane; actionlint checks
  the workflows themselves.
- A managed label set (`.github/labels.yml` + sync workflow) and path-based
  PR labelling, a welcome comment for first-time contributors, and a stale bot
  for quiet issues and PRs.
- `SECURITY.md`, `CODE_OF_CONDUCT.md`, a pull request template, and repo
  description/topic notes in `.github/REPO_METADATA.md`.

### Removed

- `election.votes-per-resident` and the whole `election.economy` block
  (`candidacy-cost`, `winner-reward`): these were parsed but never used, so the
  matching `ConfigManager` fields and getters were removed as well.
- The unused `commands.profile` and `commands.nation` literals, plus the README
  sections that documented candidate profiles and nation elections.

## Pre-fork versions

Versions up to 1.2.0 are the upstream TownyElections history; see the release
notes on GitHub for their contents.
