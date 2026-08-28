# Project Guidelines

This rule file contains branching, commit, PR, and task-finding conventions for the project. Commands read this file to determine how to name branches, format commits, and search for tasks.

- **Fix branch:** `fix/<ISSUE_NUMBER>`
- **Feature branch:** `feature/<ISSUE_NUMBER>-<short-slug>`
- **Bugfix branch:** `bugfix/<ISSUE_NUMBER>`
- **Quick-fix branch:** `quick-fix/<short-slug>`
- **SonarCloud branch:** _(not configured)_
- **Commit format (fix):** `Fixes #<ISSUE_NUMBER>. <brief description>`
- **Commit format (quick-fix):** `chore: <brief description>`
- **CI-issue branch:** `ci-issue/<short-slug>`
- **Commit format (ci-issue):** `ci: <brief description>`
- **PR creation:** always
- **Backport targets:** `main`, `3.39.x` (current release line), `3.33.x` (LTS), `3.27.x` (LTS). Camel Quarkus follows the **Quarkus LTS cadence**, so each line pairs with a Quarkus version (3.33.x ↔ Quarkus 3.33.x, 3.27.x ↔ Quarkus 3.27.x). `3.20.x` and `3.15.x` are older LTS lines that are no longer routinely maintained — do not open backport PRs against them without checking first. **A branch existing in `git branch -r` is not evidence that it is supported**: every `3.N.x` branch ever released is still present. Confirm against this list, or sort by real activity rather than by name:

  ```sh
  # %cd (committer date), not %ad: a cherry-picked backport keeps the original
  # author date, so %ad makes an actively maintained line look stale.
  for b in $(git branch -r | grep -oE 'origin/3\.[0-9]+\.x' | sort -u); do
    printf "%-16s %s\n" "${b#origin/}" "$(git log -1 --format='%cd %s' --date=short "$b")"
  done | sort -k2 -r
  ```

- **Backport method:** cherry-pick, preserving the original author and commit message. The backport lands as a new SHA on the target branch.
- **Backport applicability check:** confirm the *defect* exists on the target, not just the file — extensions differ between lines (for example `extensions-jvm/diagram` does not exist on `3.33.x` or `3.27.x`). Use `git show origin/<branch>:<path>`.
- **Backport migration-guide policy:** `docs/modules/ROOT/pages/migration-guide/<version>.adoc` is per release line. A change carrying a migration note on `main` must not backport that file; write an equivalent note for the target line's own version and add it to `migration-guide/index.adoc` on that branch.
- **Find-task source:** GitHub labels
- **Find-task beginner label:** `good first issue`
- **Find-task experienced label:** `help wanted`
- **Find-task intermediate:** _(none)_
- **Scope-too-large redirect:** `/oss-create-issue`
