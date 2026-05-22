You are preparing the CHANGELOG entry for a new release of the Snowplow Android tracker.

Inputs you will be given:
- `VERSION`: the new version string, e.g. `6.3.3`.
- `RELEASE_DATE`: today's date in `YYYY-MM-DD` form.
- `COMMITS`: a list of merge / squash commits going into this release, one per line, formatted as `<short-sha> <subject>`. Subjects usually end with ` (#NNN)` or ` (close #NNN)`.
- `PREVIOUS_ENTRY`: the most recent existing CHANGELOG entry (header + body), provided verbatim as a style example.

Produce exactly the new CHANGELOG entry — nothing else. No preamble, no code fences, no trailing commentary.

Format (match `PREVIOUS_ENTRY` exactly):

```
Version <VERSION> (<RELEASE_DATE>)
----------------------------------
<one short line per change>

```

Rules:
- The underline row is hyphens, the same length as the header line above it.
- One line per change. Keep each line short (ideally under 100 chars).
- Preserve the PR/issue reference at the end of the line in parentheses — e.g. `(#723)` or `(close #720)`. If a commit subject contains one, keep it; do not invent one if it is missing.
- Do **not** add `thanks to @user` attribution in the CHANGELOG — that belongs only in the PR body.
- Do not group by type (no "Bug fixes:" / "New features:" headings). The CHANGELOG is a flat list.
- Skip commits that are pure chores: dependency bumps with no behaviour change, CI-only changes, docs-only changes, and any commit whose subject begins with "Prepare for". When in doubt, include.
- Rewrite subjects only when they are ungrammatical or unclear; otherwise keep them close to the original wording.
- End the entry with a single trailing newline so it can be prepended to the existing CHANGELOG.
