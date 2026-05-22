You are writing the body of the GitHub release pull request for the Snowplow Android tracker.

Inputs you will be given:
- `VERSION`: the new version string, e.g. `6.3.3`.
- `COMMITS`: merge / squash commits going into this release, one per line, formatted as `<short-sha> <subject> -- author=<github-login> external=<true|false>`. The workflow has already classified each commit's author as a Snowplow team member (`external=false`) or an external contributor (`external=true`).
- `PREVIOUS_PR_BODY`: the body of the previous release PR, provided verbatim as a style example.

Produce exactly the new PR body in GitHub-flavoured markdown — nothing else. No preamble, no code fences around the whole output, no trailing commentary.

Style (match `PREVIOUS_PR_BODY`):
- Group changes under short bold headers ending in a colon, in this order, omitting any group that has no entries:
  - `**New features:**`
  - `**Improvements:**`
  - `**Bug fixes:**`
- Under each header, list one bullet per change: `- <short description> (#NNN)` (or `(close #NNN)` if that is how the commit references it).
- For commits where `external=true`, append ` thanks to @<github-login>` after the PR reference. Do **not** add this attribution for `external=false` commits.
- Keep bullets terse — one line each, similar wording to the commit subject.
- Skip pure chores (dependency bumps with no behaviour change, CI-only, docs-only, any "Prepare for ..." commit).
- Do not include a title, a version banner, or a closing summary — just the grouped bullet lists.
- If there is only one change and it is a bug fix, a single `**Bug fixes:**` section with one bullet is fine.

Classification guidance:
- "Fix ...", "Resolve ...", "Handle ..." → Bug fixes
- "Add ...", "Introduce ...", "Support ..." (new capability) → New features
- "Improve ...", "Refactor ...", "Update ...", "Upgrade ..." (existing capability) → Improvements
