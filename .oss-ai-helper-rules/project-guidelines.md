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
- **Backport targets:** `main`, plus the **active LTS lines only**. Camel Quarkus follows the **Quarkus LTS cadence**, so each release branch pairs with the Quarkus stream of the same version. Non-LTS release lines are **not** backport targets. **Neither a branch's presence in `git branch -r` nor how recently it was committed to tells you whether it is a backport target.** List the active LTS branches from the Quarkus registry:

  ```sh
  curl -s https://registry.quarkus.io/client/platforms \
    | jq -r '.platforms[] | select(."platform-key"=="io.quarkus.platform") | .streams[] | select(.lts) | .id + ".x"'
  ```

- **Backport method:** cherry-pick, preserving the original author and commit message. The backport lands as a new SHA on the target branch.
- **Backport applicability check:** confirm the *defect* exists on the target, not just the file — extensions differ between lines (for example `extensions-jvm/diagram` does not exist on `3.33.x` or `3.27.x`). Use `git show origin/<branch>:<path>`.
- **Backport migration-guide policy:** `docs/modules/ROOT/pages/migration-guide/<version>.adoc` is per release line. A change carrying a migration note on `main` must not backport that file; write an equivalent note for the target line's own version and add it to `migration-guide/index.adoc` on that branch.
- **Find-task source:** GitHub labels
- **Find-task beginner label:** `good first issue`
- **Find-task experienced label:** `help wanted`
- **Find-task intermediate:** _(none)_
- **Scope-too-large redirect:** `/oss-create-issue`
