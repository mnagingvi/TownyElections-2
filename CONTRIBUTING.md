# Contributing to TownyElections

Contributions to **TownyElections** whether bug fixes, localized translations, documentation improvements, or new features—are welcome. Every merged pull request adds you to the repository's contributor list on the main README.

---

## Ways to Contribute

You do not need to write Java to contribute to the project. Here are the primary ways to get involved:

- **Translations & Localization:** Translate `messages_en.yml` into another language or refine existing message strings to help localized servers.
- **Documentation:** Improve the `README.md`, clarify command usage, detail configuration options, or fix errors.
- **Bug Reports & Issue Verification:** Submit detailed reports on the [Issue Tracker](https://github.com/mnagingvi/TownyElections-2/issues) with server logs, Paper/Towny versions, and reproduction steps, or help verify existing open issues.
- **Code & Features:** Fix open bugs, optimize performance, or implement new electoral mechanics in Java.

---

## Build Requirements

Before contributing code, ensure your development environment meets the build requirements:

| Tool | Required Version |
|---|---|
| Java Development Kit (JDK) | 21 or newer |
| Build Tool | Maven 3.8+ |
| Target Server | Paper 1.21.4+ |

Build and verify the shaded jar locally using:

```bash
mvn clean package

```

---

## Pull Request Guidelines & Standards

When opening a Pull Request (PR), adhere to the following guidelines:

* **Atomic PRs:** Keep PRs concise and focused on a single feature, translation, or bug fix. Avoid bundling unrelated changes together.
* **Discuss Major Changes:** Open a [Discussion](https://github.com/mnagingvi/TownyElections-2/discussions) before implementing major new features (such as new electoral mechanics, storage alterations, or major GUI rewrites) to discuss design before writing code.
* **State & Data Preservation:** Ensure all state changes maintain backwards compatibility with existing YAML storage (`data.yml`, `config.yml`, `messages_en.yml`). Never break active election persistence or reload safety.
* **Java 21 & Paper API Standards:** Write clean Java utilizing modern features where applicable. Keep heavy operations asynchronous (e.g., update checks) and run all Bukkit/Towny state modifications safely on the main thread.
* **Testing:** Test your built `.jar` thoroughly on a local Paper server running Towny before submitting. Verify that commands, GUI interactions, tab completion, and permissions work as expected.

---

## Step-by-Step Guide to Submitting a Pull Request

### 1. Fork the Repository

Navigate to [TownyElections on GitHub](https://github.com/mnagingvi/TownyElections-2) and click **Fork** in the top-right corner.

### 2. Clone Your Fork

Clone your fork locally:

```bash
git clone [https://github.com/YOUR_USERNAME/TownyElections.git](https://github.com/YOUR_USERNAME/TownyElections.git)
cd TownyElections

```

### 3. Create a Topic Branch

Create a new branch specific to your contribution:

```bash
git checkout -b feature/short-description
# or for translations/docs:
git checkout -b docs/add-danish-translation

```

### 4. Implement and Test

Make your changes. For code contributions, verify the build finishes cleanly:

```bash
mvn clean package

```

Test the compiled output (`target/TownyElections-<version>.jar`) on a local test server.

### 5. Commit and Push

Stage and commit your changes with a clear, descriptive commit message:

```bash
git add .
git commit -m "Add Danish translation for messages_en.yml"
git push origin docs/add-danish-translation

```

### 6. Open the Pull Request

1. Go to the original [TownyElections Repository](https://github.com/mnagingvi/TownyElections-2).
2. Click **Pull requests** > **New pull request**.
3. Select your fork and topic branch in the comparison dropdown.
4. Fill out the PR title and description explaining what was changed, then submit.

## What runs automatically

You do not have to run any of this yourself, but knowing what CI checks makes
the difference between a green PR and a round trip:

| Workflow | Trigger | What it checks |
| --- | --- | --- |
| **Build** | every push and PR | `mvn package`, i.e. compile + the unit tests, then uploads the jar and posts the build report as a comment on the PR |
| **Lint** | every push and PR | `scripts/check_messages.py` (locale parity, `{placeholders}`, message keys used from Java, `config.yml`/`plugin.yml` sanity) and actionlint over `.github/workflows` |
| **CodeQL** | PRs and pushes to `main`, weekly | security-and-quality analysis of the Java sources |
| **Labeler** | PR opened or updated | labels the PR by the paths it touches (`counting`, `translations`, `election-night`, ...) |
| **Welcome** | first issue or PR by a new contributor | a comment with what a good report or PR needs here |
| **AI issue and PR summaries** | issue/PR opened, edited, reopened, or PR updated | posts one OpenRouter summary comment and updates it on changes; closes only high-confidence, evidenced spam |
| **Stale** | daily | marks quiet issues and PRs stale after 45/30 days, closes them 14/10 days later, and skips anything labelled `security` |
| **Release** | `v*` tag | refuses to publish when the tag disagrees with the `<version>` in `pom.xml`, otherwise builds the jar, writes the release notes through OpenRouter and attaches both to the release |
| **Release notes preview** | manual | writes the same notes without publishing anything, so the wording can be checked first - see [RELEASING.md](RELEASING.md) |

Run the lint check locally before pushing:

```bash
python scripts/check_messages.py          # needs PyYAML
python scripts/build_summary.py           # preview the CI build report
python -m unittest discover -s tests -p 'test_*.py' -v  # AI summary tests; standard library only
```

The event-driven workflows only work once they are on `main`, because GitHub
reads `issues`, `pull_request_target` and scheduled workflows from the default
branch: **AI issue and PR summaries**, **Labeler**, **Welcome** and **Stale**.
Everything else runs from the branch you are on.

### AI issue and PR summaries

`.github/workflows/ai-summary.yml` uses the same `OPENROUTER_API_KEY` repository
secret and optional `OPENROUTER_MODEL` variable as the release notes; see
[RELEASING.md](RELEASING.md#one-time-setup-the-openrouter-key) for setup. No extra
secret is needed. Without the key, or if OpenRouter fails, it skips the summary
without changing the issue or PR.

- It posts one identified bot comment and edits that comment on later changes.
  PR summaries use the title, body, and a bounded excerpt of changed files and
  patches; issue summaries use the title and body. This public text is sent to
  OpenRouter. Existing comments and API credentials are not sent.
- Automatic closure requires a spam classification with at least 98% confidence
  and a matching quote from the submission. Missing details, non-English text,
  criticism, or an unclear report alone are not spam. Issues close as
  **not planned**; PRs close without merging. A `spam` label is applied first.
- Maintainer-authored items can be summarized but never automatically closed.
  Bots, draft PRs, closed items, and `security`-labelled items are skipped.
  `pinned` items are never automatically closed. Reopening an item already
  labelled `spam` will not trigger another automatic closure.
- To keep summaries but disable all automatic closures, set the repository
  **variable** `AI_SUMMARY_CLOSE_SPAM` to `false` under **Settings → Secrets and
  variables → Actions → Variables**. Closure is enabled when it is unset.
- After the workflow is on `main`, **Actions → AI issue and PR summaries → Run
  workflow** can summarize an existing issue or PR by number.

For fork PRs, the workflow executes only the trusted base-branch script and
reads contributor content through GitHub's API; it never checks out or runs the
PR's code. AI summaries can be wrong and spam detection is best-effort. Reopen a
mistakenly closed item and remove the `spam` label if appropriate, or disable
automatic closure while reviewing the model's behavior.

Labels live in `.github/labels.yml` and are synced by the **Labels** workflow;
add a new label there rather than in the GitHub UI so the next sync does not
fight you.
