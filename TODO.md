This is a file of TODO's
These are not guaranteed to be added, these are just ideas which you can use if you want to make a pull request or smth


- [ ] geyserMC extension - make an extension for geyserMC for a better experience for bedrock users

- [ ] compatibility with other plugins - maybe compatibility for some towny war plugins?

- [ ] candidacy fees and winner rewards - the old `election.economy` config options were
      removed because they were never wired up; a real implementation needs a Vault
      (or PlayerPoints) economy hook, a fee charged on `/election run`, and a payout
      with a note on the winner announcement.

- [ ] candidate profiles - `/election profile` was documented but never implemented;
      a long-form bio on `Candidate` shown on the candidate head tooltip would pair
      well with the existing campaign message.

- [ ] election night extras - a pre-close countdown boss bar ("polls close in 10m"),
      a configurable fireworks palette, and an optional results webhook for Discord.

- [ ] Modrinth / Hangar publishing - the Release workflow only attaches the jar to
      a GitHub release. A publish job for Modrinth (and maybe Hangar) needs API
      tokens as repository secrets and a version-scoped changelog body.

- [ ] coverage reporting - the unit tests run in CI but nothing measures them; JaCoCo
      plus a Codecov (or Code Climate) upload would show which counting paths are
      still untested.

- [ ] integration tests against a Paper runtime - MockBukkit or a headless Paper
      instance could cover the parts the current tests skip: config loading,
      message files, command parsing and the Towny hand-off on conclusion.

- [ ] discussion / feature-request triage - a stale-issue workflow would close
      bug reports that never got a `/election version` reply after a few weeks.
