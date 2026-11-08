
![ownyElections](https://capsule-render.vercel.app/api?type=blur&height=300&color=gradient&section=header&reversal=false&text=TownyElections&textBg=false&fontSize=70&fontAlign=50&fontAlignY=50&rotate=0&strokeWidth=0&desc=Add+Actual+Elections+To+Your+Minecraft+Server.&descSize=20&descAlign=50&descAlignY=70)

[![Build](https://github.com/mnagingvi/TownyElections-2/actions/workflows/build.yml/badge.svg)](https://github.com/mnagingvi/TownyElections-2/actions/workflows/build.yml)
[![Latest release](https://img.shields.io/github/v/release/mnagingvi/TownyElections-2?label=release&logo=github)](https://github.com/mnagingvi/TownyElections-2/releases)
[![Downloads](https://img.shields.io/github/downloads/mnagingvi/TownyElections-2/total?label=downloads&logo=github)](https://github.com/mnagingvi/TownyElections-2/releases)
[![Contributors](https://img.shields.io/github/contributors/mnagingvi/TownyElections-2?label=contributors&logo=github)](https://github.com/mnagingvi/TownyElections-2/graphs/contributors)
[![Last commit](https://img.shields.io/github/last-commit/mnagingvi/TownyElections-2?logo=git)](https://github.com/mnagingvi/TownyElections-2/commits/main)
[![Issues](https://img.shields.io/github/issues/mnagingvi/TownyElections-2?logo=github)](https://github.com/mnagingvi/TownyElections-2/issues)
[![bStats servers](https://img.shields.io/bstats/servers/35211?label=servers&logo=googleanalytics&logoColor=white)](https://bstats.org/plugin/bukkit/TownyElection/35211)
[![Java 21](https://img.shields.io/badge/Java-21-f89820?logo=openjdk&logoColor=white)](https://adoptium.net/)
[![Paper 1.21.4+](https://img.shields.io/badge/Paper-1.21.4%2B-34d399)](https://papermc.io/)
[![Towny](https://img.shields.io/badge/Towny-required-8b5cf6)](https://github.com/TownyAdvanced/Towny)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

A **Paper 1.21.4+** plugin that adds a formal, fully-configurable **election system**
to towns managed by the [Towny](https://github.com/TownyAdvanced/Towny) plugin.

Residents run for office, publish a campaign message, and their fellow townsfolk
vote. When the election concludes, the winner is automatically granted the
Towny town ranks (plot management, etc.) and/or mayorship you configure.

**Links:** [Releases](https://github.com/mnagingvi/TownyElections-2/releases)
| [Issues](https://github.com/mnagingvi/TownyElections-2/issues)
| [bStats](https://bstats.org/plugin/bukkit/TownyElection/35211)
| [Towny](https://github.com/TownyAdvanced/Towny)

---

## Features

- **Structured election lifecycle** — Nomination → Voting → (optional Runoff) → Concluded.
- **Candidacy** — residents register with `/election run` and set a custom campaign
  message shown to voters.
- **Political parties** — candidates can organize under configurable party labels,
  voters can inspect party standings with `/election parties`, and results include
  party-level vote totals alongside individual winners.
- **Election Night** — a configurable live results reveal: a title card, a boss-bar
  scoreboard, candidate-by-candidate reveals (or round-by-round ranked-choice
  counting), sound cues, and party-coloured fireworks for the winner. Instantly
  announcements can be deferred so nothing spoils the show, and the last result
  can be replayed with `/election night`.
- **Inventory GUI** — players can run `/election` with no arguments to open a
  click-driven election desk, inspect candidates, and vote from player-head icons.
- **Voting** — one command to cast (and optionally change) a vote, with eligibility
  restricted to town residents. Supports secret ballots (hidden tallies).
- **Per-IP vote protection** — optionally limit each active election
  to a configured number of distinct IP fingerprints to reduce alt-account abuse.
- **Three electoral systems** — classic **plurality**, **ranked-choice**
  (instant-runoff with automatic elimination rounds and round-by-round results),
  and **approval voting**, selected with `election.voting-system`. Ballots,
  commands, tab completion, the GUI, results, and persistence all adapt to the
  chosen system.
- **Automatic winner rewards** — grants configurable Towny **town ranks**, optionally
  transfers **mayorship**, runs custom console commands, and can revoke the previous
  holder's ranks.
- **Highly configurable** — durations, minimum/maximum candidates, tie-breaking
  strategy, campaign rules, notifications, and **renamable sub-commands**.
- **Tie-breakers** — `RANDOM`, `EARLIEST`, `INCUMBENT`, `RUNOFF`, or `NONE`.
- **Auto-scheduling** — optionally run recurring elections in every eligible town.
- **Persistence** — active elections and results survive restarts (`data.yml`).
- **Integrations** — optional **PlaceholderAPI** placeholders and **bStats** metrics.
- **Update notifications** — asynchronously checks GitHub Releases for a newer
  stable release (draft/prerelease/nightly ignored), logs to console, and can
  notify admins on join.
- **Localised, colourised messages** — legacy `&` and hex `&#RRGGBB` colours.

---

## Project stats

These images update automatically from GitHub and bStats.

<p align="center">
  <a href="https://github.com/mnagingvi/TownyElections-2/graphs/contributors">
    <img src="https://contrib.rocks/image?repo=mnagingvi/TownyElections-2&max=1000" alt="TownyElections contributors" />
  </a>
</p>

![bStats](https://bstats.org/signatures/bukkit/TownyElection.svg)

---

## Requirements

| Dependency      | Version              | Required |
|-----------------|----------------------|----------|
| Paper           | 1.21.4 or newer      | Yes      |
| Java            | 21+                  | Yes      |
| Towny           | 0.100.0.0+ (0.103 recommended) | Yes |
| PlaceholderAPI  | any recent           | Optional |

---

## Installation

1. Download the latest jar from the
   [releases page](https://github.com/mnagingvi/TownyElections-2/releases).
2. Place it in the server `plugins/` folder.
3. Make sure [Towny](https://github.com/TownyAdvanced/Towny) is installed.
4. Restart the server, then edit `plugins/TownyElections/config.yml`.
5. Run `/election reload` after config or message changes.

PlaceholderAPI is optional. If it is present, TownyElections registers the
placeholders listed below.

---

## Building

```bash
mvn clean package
```

The shaded jar is produced at `target/TownyElections-version.number.jar`. Drop it into
your server's `plugins/` folder alongside Towny.

> The build depends on the Paper API, Towny (via JitPack), and PlaceholderAPI
> repositories, all declared in `pom.xml`.

---

## Commands

The root command is `/election` (aliases: `/elec`, `/te`). Every sub-command
literal below is **configurable** in `config.yml` under the `commands:` section.

| Command                         | Permission                | Description                          |
|---------------------------------|---------------------------|--------------------------------------|
| `/election`                     | Town resident             | Open the election GUI.               |
| `/election run`                 | `townyelections.candidate`| Stand as a candidate.                |
| `/election withdraw`            | `townyelections.candidate`| Withdraw from the race.              |
| `/election campaign <message>`  | `townyelections.candidate`| Set your campaign message.           |
| `/election party <name>`       | `townyelections.candidate`| Join or create a political party.    |
| `/election parties`            | `townyelections.info`     | List current parties and standings.  |
| `/election vote <candidate...>` | `townyelections.vote`     | Cast (or change) your vote or ballot. |
| `/election status`              | `townyelections.info`     | View the current election.           |
| `/election candidates`          | `townyelections.info`     | List candidates, parties & campaigns. |
| `/election results`             | `townyelections.info`     | View the last concluded results.     |
| `/election night`               | `townyelections.info`     | Replay the last result as an Election Night show. |
| `/election night skip [town]`   | `townyelections.admin`    | Fast-forward a running show.         |
| `/election start [town]`        | `townyelections.admin`    | Start an election.                   |
| `/election stop [town]`         | `townyelections.admin`    | End voting early and tally.          |
| `/election cancel [town]`       | `townyelections.admin`    | Cancel with no winner.               |
| `/election reload`              | `townyelections.admin`    | Reload configuration.                |

Admins may target another town by name; otherwise the sender's own town is used.

### Inventory GUI

Running `/election` as a player opens the election desk for your town. The menu
shows the current phase, remaining time, candidate count, vote count, your current
vote, and one-page access to the candidate ballot, party standings, last results,
candidacy tools, and admin controls when the player has `townyelections.admin`.

The candidate roster uses player heads as vote buttons. Under plurality a click
casts your vote; under ranked choice each click adds that candidate as your next
preference (heads show your current rank and full ballot); under approval each
click toggles your approval. A clear-ballot button resets ranked or approval
ballots. Hovering a candidate head shows their party, campaign message, and
visible vote data when live results are enabled. Campaign messages and party
names can be set from the menu by clicking the matching item and typing the new
value in chat; the plugin consumes that one chat message and applies the normal
validation.

The GUI still enforces the same permissions, Towny residency checks, voting phase
checks, self-vote setting, vote-change setting, campaign limits, and party limits
as the chat commands. Menu clicks and drags are cancelled inside plugin
inventories so players cannot take, place, shift-click, or drag GUI items into
their own inventory.

### Election Night

When an election concludes, TownyElections can present the tally as a staged
**results broadcast** rather than printing the winner instantly. Only residents
who are online at that moment see the show:

1. a title card announces the election and its turnout,
2. candidates are revealed one at a time from the bottom of the field up — each
   step prints a party-coloured bar chart and updates a boss-bar scoreboard,
3. for `RANKED_CHOICE` elections the *counting rounds* are replayed instead:
   every round's tallies, the candidate(s) eliminated, and the exhausted ballots,
4. the winner is crowned with a title, a server-wide announcement (optional),
   sound cues, and fireworks above every viewer in the winner's party colours,
5. a closing summary reports the final turnout and any ranks/mayor status the
   winner received.

The reveal is purely presentational: the result is recorded and the winner's
rewards are granted before the first stage runs, so skipping or replaying a show
can never change the outcome. With `defer-announcements: true` (the default) the
usual instant "winner" broadcasts are withheld while the show is running, so
nothing gives the result away early.

```yaml
election-night:
  enabled: true
  minimum-votes: 1            # smaller elections are announced normally
  defer-announcements: true   # let the show narrate the result
  reveal-order: "WORST_FIRST" # WORST_FIRST | BEST_FIRST | RANDOM
  stage-delay-ticks: 40       # 20 ticks = 1 second
  intro-delay-ticks: 60
  winner-hold-ticks: 100
  boss-bar: true
  titles: true
  sounds: true
  fireworks: true
  firework-count: 6
  broadcast-server-wide: true
```

Command and GUI access:

- `/election night` replays the last recorded result for your town (also
  available as a firework button on the election desk).
- `/election night skip [town]` (admin) fast-forwards a running show, which is
  handy on a busy server or when testing.
- Turning `election-night.enabled` off restores the classic instant
  announcements.

### Political parties

Parties are labels that residents can join, create, leave, or admin-rename
without changing the core individual-winner election rules. Parties **persist
between elections**: a resident can join or create a party at any time — even
when no election is running — and their membership carries over so they are
already in that party when the next election starts. When an election is
active, candidates' party choices are also written back to the standing
registry so they survive the election ending.

```text
/election party Reform Coalition      # join or create a party (works without an active election)
/election party color gold            # colour your party (name, &code, or #RRGGBB; requires an active election)
/election party leave                 # leave your standing party / return to the default party
/election parties                     # list current parties and standings (shows standing parties when no election is active)
/election party rename Reform Unity   # admin: rename a party in this election
```

When a resident leaves their town, their standing-party membership
is automatically cleared so no stale parties linger. Standing parties are
persisted in `data.yml` under the `standing-parties` section and reload safely
across restarts; old data files without that section load unchanged.

**Party colours** are customizable: any candidate in a (non-default) party can
set that party's colour with `/election party color <colour>`, accepting a colour
name (`red`, `dark_blue`, ...), a legacy code (`&c`), or hex (`&#RRGGBB`). The
colour is applied everywhere the party is shown — candidate lists, standings,
the GUI, broadcasts, results, and placeholders — and is preserved in recorded
results.

When a resident registers with `/election run`, the plugin also sends them a
short list of recommended next steps (set a campaign message, join a party,
and colour it).

Candidate lists and final results show each candidate's party. Party standings
show candidate counts, and show vote totals whenever live results are public or
after results are recorded. Example result output:

```text
Party Results - Oakvale
 - Reform Coalition (18 votes, 2 candidates) Alex, Mira
 - Independent (7 votes, 1 candidate) Rowan
```

### Voting systems

`election.voting-system` in `config.yml` selects how ballots are collected and
counted. The system is locked in when an election starts, so a config change
never re-interprets the ballots of a running election.

| System          | Ballot                                   | Count                                   |
|-----------------|------------------------------------------|-----------------------------------------|
| `PLURALITY`     | One candidate.                           | Most votes wins (default, classic).      |
| `RANKED_CHOICE` | Candidates in order of preference.       | Instant-runoff elimination rounds.       |
| `APPROVAL`      | Every candidate the voter approves of.   | Most approvals wins.                     |

With **ranked choice**, `/election vote Alex Mira Rowan` ranks Alex first,
Mira second, Rowan third. Counting eliminates the weakest candidate each round
and transfers their ballots to each voter's next surviving preference until a
candidate holds a majority of the continuing ballots. Every round (tallies,
eliminations, and exhausted ballots) is recorded and shown in
`/election results`:

```text
Runoff Round 1
   - Alex: 4 vote(s)
   - Mira: 3 vote(s)
   - Rowan: 2 vote(s)
   Eliminated: Rowan
Runoff Round 2
   - Mira: 5 vote(s)
   - Alex: 4 vote(s)
```

With **approval**, `/election vote Alex Mira` approves both candidates; list as
many as you support.

In the GUI, plurality clicks vote directly, while ranked-choice and approval
clicks build the ballot: each click adds the candidate as your next preference
(or approves them), clicking again removes them, and a bucket button clears the
whole ballot. Adding to a ballot is always allowed; removing or clearing
entries requires `allow-vote-changes: true`. Live first-preference and approval
tallies follow your `public-live-results` setting, exactly like plurality.

If the final count ends in a dead heat, your configured `tie-breaker`
(`RANDOM`, `EARLIEST`, `INCUMBENT`, `RUNOFF`, or `NONE`) resolves it under
every system.

### Permissions

| Node                        | Default | Grants                               |
|-----------------------------|---------|--------------------------------------|
| `townyelections.candidate`  | true    | Running & managing a campaign.       |
| `townyelections.vote`       | true    | Voting.                              |
| `townyelections.info`       | true    | Viewing status/candidates/results.   |
| `townyelections.admin`      | op      | Start/stop/cancel/reload.            |
| `townyelections.*`          | op      | Everything.                          |

---

## Winner rewards

When an election concludes, the winner receives whatever you configure under
`winner:` in `config.yml`:

```yaml
winner:
  set-as-mayor: false          # transfer mayorship to the winner
  grant-town-ranks:            # Towny ranks from your townyperms.yml
    - "councillor"
    - "helper"
  revoke-previous-winner-ranks: true
  commands-on-win:
    - "lp user {winner} parent addtemp mayor 30d"
  commands-on-loss: []
```

Ranks map to Towny permission nodes defined in **`townyperms.yml`** (this is where
plot-management and other role permissions live). Invalid ranks are skipped with a
console warning. Command placeholders: `{winner}`, `{winner_uuid}`, `{town}`,
`{votes}`, `{total_votes}`, `{winner_party}` (and `{loser}`, `{loser_uuid}`, `{loser_party}` for loss commands).

---

## Configuration highlights

Durations accept friendly strings like `30s`, `10m`, `2h`, `3d`, `1w`, or
combinations such as `1w3d12h`. Candidates set a campaign message and can join an
existing party or create a new party label with `/election party <name>`; tab
completion suggests current parties. Party standings are available with
`/election parties`, and final results include party-level vote totals plus
winner command placeholders.

By default (`campaign.lock-edits-during-voting: true`), candidates cannot change
their campaign message, party, or party colour once the voting phase has begun —
these can only be edited during the nomination phase. Set it to `false` to allow
edits at any time.

```yaml
election:
  nomination-duration: "2d"
  voting-duration: "3d"
  min-candidates: 2
  max-candidates: 0            # 0 = unlimited
  auto-win-single-candidate: true
  allow-vote-changes: true
  public-live-results: true    # false = secret ballot
  voting-system: "PLURALITY"   # PLURALITY | RANKED_CHOICE | APPROVAL
  tie-breaker: "RUNOFF"        # RANDOM | EARLIEST | INCUMBENT | RUNOFF | NONE
  auto-schedule:
    enabled: true
    interval: "14d"
  ip-vote-limit:
    enabled: false
    max-votes: 0                 # 0 = unlimited
```

When `election.ip-vote-limit.enabled` is true and `max-votes` is greater than zero,
each active election tracks distinct player IP addresses independently. IPs are
normalized by the server connection and represented only by an in-memory SHA-256
fingerprint; raw addresses are never logged or persisted. Ballot changes and
clearing an existing ballot do not consume additional capacity. Because tracking
is intentionally not persisted, a server restart resets the fingerprints for
active elections. Shared networks and proxies may cause multiple players to be
counted together, while changing IPs can result in separate fingerprints.

See the generated `config.yml` for the full, commented set of options, and
`messages_en.yml` for every editable message.

---

## PlaceholderAPI

If PlaceholderAPI is installed, these placeholders are available (identifier
`townyelections`), resolved for the viewing player's town:

| Placeholder                         | Description                          |
|-------------------------------------|--------------------------------------|
| `%townyelections_phase%`            | Current phase, or `none`.            |
| `%townyelections_voting_system%`    | Active election's system, or `none`. |
| `%townyelections_time_left%`        | Time until the current phase ends.   |
| `%townyelections_candidates%`       | Number of candidates.                |
| `%townyelections_votes%`            | Number of votes cast.                |
| `%townyelections_has_voted%`        | `true`/`false` for the player.       |
| `%townyelections_my_party%`         | Your candidate party, if running.     |
| `%townyelections_leading_party%`    | Leading current-election party.       |
| `%townyelections_last_winner%`      | Name of the last winner in the town. |
| `%townyelections_last_winner_party%`| Party of the last winner in the town. |
| `%townyelections_last_winner_votes%`| Votes received by the last winner.   |
| `%townyelections_last_turnout%`     | Turnout of the last election, in percent. |
| `%townyelections_night_running%`    | `true` while a night show is live.   |

---

## Update notifications

On startup TownyElections asynchronously checks GitHub Releases for a newer
**stable** version of the plugin using each release's `tag_name`. Draft,
prerelease, and nightly releases are ignored, the check never blocks the server,
and it never downloads or installs anything. When a newer release is found it is logged to the console, and players with
`townyelections.admin` can be notified when they join.

Configure it under `update-checker` in `config.yml`:

- `update-checker.enabled` — turn the GitHub Releases check on or off.
- `update-checker.github-repository` — GitHub repository in `owner/name` form
  (default: `mnagingvi/TownyElections-2`). Draft, prerelease, and nightly releases are ignored.
- `update-checker.notify-admins-on-join` — message admins on join when an update
  is available.

---

## How it works

1. An admin (or the auto-scheduler) starts an election in a town.
2. **Nomination phase**: residents `/election run`, set campaign messages, and choose parties.
3. When nominations end, if there are enough candidates the **voting phase** opens
   (otherwise the election is cancelled, or a lone candidate may auto-win).
4. Residents `/election vote <candidate...>` (one name under plurality, several
   to rank or approve). A reminder is sent to non-voters before voting closes.
5. When voting ends, ballots are counted under the election's voting system
   (including instant-runoff rounds for ranked choice), ties are resolved per
   your strategy, the result is recorded, and the winner's rewards are applied
   through Towny.

State is checked once per second and persisted to `data.yml`, so elections resume
correctly across restarts.

---

## Contributing

Bug reports, feature ideas, and pull requests are welcome. Please use the
[issue tracker](https://github.com/mnagingvi/TownyElections-2/issues) and keep
changes focused on the existing Paper/Towny workflow.

Before submitting a code change, run:

```bash
mvn clean package
```

---

## License

Licensed under the [MIT License](LICENSE). Adapt freely for your server.
