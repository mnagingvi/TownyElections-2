# Repository metadata

Copy-paste settings for the GitHub repo page. The values below are the ones the
project uses, so the sidebar, topic list and search results all describe the
plugin consistently.

> The GitHub app used for automation in this repo does not have the
> **administration** permission, so description/topics/homepage cannot be set
> from a commit or a workflow. Run the command below yourself (needs `gh auth
> login` with `repo` + admin access), or edit the fields in the web UI.

## One command

```bash
gh repo edit mnagingvi/TownyElections-2 \
  --description "Run real elections on your Paper 1.21.4+ Minecraft server: Towny residents file candidacy, campaign, and vote in plurality, ranked-choice (instant-runoff) or approval elections, with automatic rank and mayor handover on election night." \
  --homepage "https://github.com/mnagingvi/TownyElections-2/releases" \
  --add-topic minecraft,paper,papermc,minecraft-plugin,towny,towny-plugin,elections,voting,ranked-choice,instant-runoff,democracy,bukkit,java,java21,placeholderapi,bstats,election-night,politics \
  --enable-discussions
```

## Field by field

| Field | Value |
| --- | --- |
| Description | `Run real elections on your Paper 1.21.4+ Minecraft server: Towny residents file candidacy, campaign, and vote in plurality, ranked-choice (instant-runoff) or approval elections, with automatic rank and mayor handover on election night.` |
| Short version (≤ 90 chars, for links) | `Formal election system for Towny towns on Paper 1.21.4+ - campaigns, ballots, election night.` |
| Website | `https://github.com/mnagingvi/TownyElections-2/releases` (no external site yet; the bStats page also works) |
| Discussions | On — the issue templates already send questions there |
| Social preview | Use a 1280×640 still of the Election Night winner card; the animated header in the README is too tall |

## Topics

`minecraft` `paper` `papermc` `minecraft-plugin` `towny` `towny-plugin`
`elections` `voting` `ranked-choice` `instant-runoff` `democracy` `bukkit`
`java` `java21` `placeholderapi` `bstats` `election-night` `politics`

Twenty is GitHub's limit; these eighteen are the ones worth keeping. They cover
the platform (`paper`, `bukkit`, `java`, `java21`), the integration
(`towny`, `placeholderapi`, `bstats`) and what the plugin actually does
(`elections`, `voting`, `ranked-choice`, `instant-runoff`, `election-night`).

## Web UI

1. **Settings → General** → paste the description, set the website.
2. On the repo landing page click the ⚙️ next to **About** → paste the topics
   (space-separated), then **Save changes**.
3. **Settings → General → Features** → tick **Discussions**.
4. **Settings → General → Social preview** → upload the 1280×640 image.

## Releases and tags

Tags are annotated and match `pom.xml` (`1.3.0` → `v1.3.0`); pushing the tag
runs `.github/workflows/release.yml`, which builds the jar and attaches it to
the release with generated notes. `main` additionally keeps a rolling
`nightly` prerelease that is replaced on every push.
