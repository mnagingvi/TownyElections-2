# Security policy

## Supported versions

TownyElections is a small plugin, so only the latest release is supported.
If you report a vulnerability, please make sure it still reproduces on the most
recent version from the
[releases page](https://github.com/mnagingvi/TownyElections-2/releases) before
filing anything.

| Version | Supported |
| ------- | --------- |
| 1.3.x   | ✅        |
| < 1.3   | ❌        |

## Reporting a vulnerability

**Do not open a public issue for a security problem.** Use GitHub's private
reporting instead:

1. Go to the [Security tab](https://github.com/mnagingvi/TownyElections-2/security).
2. Click **Report a vulnerability**.
3. Describe the issue, the version you tested, your server software (Paper
   build), and the steps to reproduce it.

You can expect an initial reply within a week. Once a fix is ready it is
released as a patch version, and the advisory is published with credit unless
you ask to stay anonymous.

## What counts as a security issue here

The plugin has no network listeners, no database, and no authentication of its
own, so most reports look like one of these:

- **Permission or rank escalation** — a player who is not allowed to run a
  command (`townyelections.admin`) can start, stop
  or rig an election, or a winning candidate receives ranks or mayorship
  they should not have.
- **Ballot tampering** — a resident can vote outside their own town, vote
  twice, vote after the polls close, or read the live tally while
  `election.public-live-results` is disabled.
- **Data loss or corruption** — an election or a ballot can be destroyed or
  rewritten through a command, a menu click, or a reload.
- **Update checker** — anything that would make the plugin fetch or execute
  something other than the release JSON from
  `https://api.github.com/repos/mnagingvi/TownyElections-2/releases/latest`.

Ordinary bugs (a wrong message, a miscounted tie, a missing placeholder) are
not security issues — please use the
[bug report template](https://github.com/mnagingvi/TownyElections-2/issues/new/choose)
for those.

## Running untrusted elections

A few settings are worth knowing about if you run a large public server:

- `election.public-live-results: false` hides the running tally from voters
  until the election concludes.
- `election.tie-breaker` decides what happens when two candidates tie; keep it
  on `RUNOFF` if you do not want the plugin to pick a winner by coin flip.
- Only grant `townyelections.admin` to staff you would trust with Towny's own
  admin commands — it can cancel any election in any town.
