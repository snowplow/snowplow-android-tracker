# Preparing a release

The `Prepare release PR` workflow (`.github/workflows/prepare-release.yml`)
automates the "Prepare for X.Y.Z release" commit and the release PR that
maintainers used to write by hand.

## How a release works now

1. Create the release branch from `master` and merge feature/bug-fix PRs into
   it as usual:
   ```
   git checkout master && git pull
   git checkout -b release/6.3.3
   git push -u origin release/6.3.3
   ```
2. When the branch is ready to ship, go to **Actions → Prepare release PR →
   Run workflow** and fill in:
   - `release_branch`: `release/6.3.3`
   - `dry_run`: leave unchecked for a real run; check it for a preview.
3. The workflow will:
   - Bump `VERSION`, `gradle.properties` (VERSION_NAME), and `build.gradle`
     (subprojects' `version` field) to the version in the branch name.
     The Kotlin `BuildConfig.TRACKER_LABEL` follows automatically at build
     time.
   - Ask Claude to draft the new `CHANGELOG` entry from the commits on the
     branch (using the previous entry as a style example) and prepend it.
   - Commit everything as `Prepare for X.Y.Z release` and push to the release
     branch.
   - Ask Claude to draft the PR body — grouped under
     `**New features:** / **Improvements:** / **Bug fixes:**`, with
     `thanks to @user` attribution for external contributors only.
   - Open (or update) a PR titled `Release/X.Y.Z` against `master`.
4. Review the PR. Edit the CHANGELOG entry or PR body in place if needed.
   Merging the PR and tagging `X.Y.Z` on `master` triggers `deploy.yml`,
   which publishes to Maven Central after verifying the tag matches
   `VERSION`.

## Re-running on the same branch

If you push a small fix to the release branch after the workflow ran, re-run
the workflow. It detects that `HEAD` is already a `Prepare for X.Y.Z release`
commit and skips the bump and changelog step — it only refreshes the PR body.

If you need the bump or CHANGELOG re-done from scratch, drop the prepare
commit locally (`git reset --hard HEAD~1 && git push --force-with-lease`) and
re-run the workflow.

## Dry-run mode

`dry_run: true` runs everything up to (but not including) the push and the PR
write. The full `git diff` and the generated PR body are printed to the
workflow log. Use this the first time you exercise the workflow on a real
release branch.

## Inputs that will make the workflow fail loudly

- A `release_branch` that doesn't match `release/X.Y.Z`.
- A `release_branch` that doesn't exist on origin.
- A previous release tag that can't be found on `master` (the workflow needs
  one to compute the commit list).

## Requirements

- `ANTHROPIC_API_KEY` secret must be set on the repository.
- `GITHUB_TOKEN` (provided automatically) needs `contents: write` and
  `pull-requests: write`, which the workflow declares.
