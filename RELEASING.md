# Releasing TownyElections

The short version: **the release is created by pushing a `v*` tag.** Nothing
publishes on a normal commit, and nothing here tags anything for you.

```bash
git tag -a v2.0.0 -m "TownyElections 2.0.0"
git push origin v2.0.0
```

That runs `.github/workflows/release.yml`, which builds the jar, checks the tag
against `pom.xml`, writes the release notes, and publishes the release with the
jar attached.

---

## One-time setup: the OpenRouter key

The release notes are written by an LLM through
[OpenRouter](https://openrouter.ai). The workflow reads it from a repository
secret called `OPENROUTER_API_KEY`.

**With the GitHub CLI**

```bash
gh secret set OPENROUTER_API_KEY --repo mnagingvi/TownyElections-2
# paste the key when prompted, then press Ctrl-D
```

**In the web UI**

1. <https://github.com/mnagingvi/TownyElections-2/settings/secrets/actions>
2. **New repository secret** → name `OPENROUTER_API_KEY`, value `sk-or-v1-...`
3. **Add secret**

Optional: pin a model instead of letting OpenRouter choose. Add a repository
**variable** (same page, **Variables** tab) called `OPENROUTER_MODEL`, for
example `openai/gpt-4.1-mini` or `anthropic/claude-sonnet-4.5`. If the variable
is unset or empty, `openrouter/auto` is used.

Without the secret nothing breaks: the notes fall back to the `CHANGELOG.md`
section for the version, with a one-line note saying so.

The key is only ever read from the environment inside the workflow step, is
never written to a file, and GitHub masks it in logs. The script sends nothing
but the prompt (commit subjects plus the changelog entry) to OpenRouter.

---

## The normal flow

1. **Land your changes on `main`.** CI must be green: `Build` (tests) and `Lint`
   both run on every pull request.

2. **Bump the version in `pom.xml`** if you have not already. `plugin.yml`
   inherits it as `${project.version}`, so that is the only place to change.

3. **Write the changelog entry** in `CHANGELOG.md`:

   ```markdown
   ## [2.1.0] — 2026-12-01
   ```

   The release-notes script reads this section and feeds it to the model, so the
   bullets you write here shape the announcement.

4. **Preview the notes** (optional but recommended):

   - Web UI: **Actions → Release notes preview → Run workflow**, set *version*
     to `2.1.0`, run it, then read the run summary.
   - CLI: `gh workflow run release-notes.yml -f version=2.1.0`
   - Locally, with your own key:

     ```bash
     OPENROUTER_API_KEY=sk-or-... python3 scripts/ai_release_notes.py --version 2.1.0
     ```

   Uncheck *use_ai* (or pass `--no-ai`) to see what the changelog-only fallback
   looks like.

5. **Tag and push**, from `main`:

   ```bash
   git checkout main && git pull
   git tag -a v2.1.0 -m "TownyElections 2.1.0"
   git push origin v2.1.0
   ```

   An annotated tag (`-a`) is what this repo uses: `git tag -l` then shows a
   message alongside the version.

6. **Watch it run:** `gh run watch` or
   <https://github.com/mnagingvi/TownyElections-2/actions/workflows/release.yml>.

---

## What the Release workflow does

| Step | Why |
| --- | --- |
| Checkout with `fetch-depth: 0` | the notes need the tags and the full history |
| `mvn clean package` | builds and runs the unit tests |
| Tag vs `pom.xml` check | a `v2.1.0` tag on a `2.0.0` pom would ship a misleadingly named jar, so the job fails instead |
| `scripts/ai_release_notes.py` | commits since the previous tag + changelog section → OpenRouter → `release-notes.md`, also shown in the run summary |
| `softprops/action-gh-release` | publishes the release: our summary first, GitHub's generated "What's Changed" list after, and the jar attached |

The jar is also uploaded as the `TownyElections` workflow artifact on that run.

## If something goes wrong

**The job failed on the tag/pom check.** Delete the tag, fix `pom.xml`, push the
tag again:

```bash
git push origin :refs/tags/v2.1.0   # delete the remote tag
git tag -d v2.1.0                   # delete it locally
```

**The release exists but you want different wording.** Edit the release body in
the UI — or regenerate and update it:

```bash
gh workflow run release-notes.yml -f version=2.1.0
# then paste the artifact into the release, or:
gh release edit v2.1.0 --notes-file release-notes.md
```

**You want to redo the whole release.**

```bash
gh release delete v2.1.0 --yes --cleanup-tag
git tag -d v2.1.0 && git push origin :refs/tags/v2.1.0
# fix, then tag again
```

`--cleanup-tag` also deletes the remote tag, so the retag can be pushed.

**The notes are missing or in the wrong language.** Check that the changelog
heading for that version exists (`## [2.1.0]`), and that `OPENROUTER_API_KEY` is
set under **Settings → Secrets and variables → Actions**. The run summary of the
Release job shows the exact Markdown that was published, plus an
`openrouter: model=... tokens=...` line, or the reason the fallback was used.

## Other releases

- **Nightly** — the `Build` workflow deletes and recreates a `nightly`
  prerelease on every push to `main`, with a jar built from that commit. It does
  not touch the version or the changelog.
- **Older versions** — tags up to `v1.3.0` predate the AI summary step; their
  bodies were generated by GitHub.
