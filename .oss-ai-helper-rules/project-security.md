# Project Security

This rule file contains the security and CVE-handling workflow for the project — how a vulnerability is reported, triaged, fixed, assigned a CVE, and published. Commands read this file to determine the private reporting channel, the CVE Numbering Authority (CNA), the advisory format and publication location, and the supported release lines a fix must be backported to.

This file is **optional**. Commands that do not deal with security ignore it; the security commands (`/oss-triage-security-report`, `/oss-create-security-advisory`, `/oss-draft-cve`, `/oss-analyze-third-party-cve`) read it when present and fall back to interactive prompts when it is absent.

Apache Camel Quarkus (the Camel extensions for Quarkus) is part of the Apache Camel project and follows the same PMC, CNA, and disclosure process as Camel core; the differences below are the issue tracker (GitHub) and that releases align with the Quarkus platform rather than Camel core's version scheme.

- **Private reporting channel:** `security@apache.org` — the ASF Security Team. Apache Camel does not operate a dedicated `security@camel.apache.org` list, so reports go to the foundation address per https://www.apache.org/security/. Never use Jira, GitHub issues/PRs, or any public mailing list to report an undisclosed vulnerability.
- **GitHub private vulnerability reporting:** not used. `apache/camel-quarkus` is public, but coordination happens on `security@apache.org`, not GitHub Security Advisories. `/oss-create-security-advisory` should direct reporters to `security@apache.org` rather than the GitHub `/reports` endpoint for this project.
- **CVE Numbering Authority (CNA):** The Apache Software Foundation Security Team — the only body that can allocate CVE IDs for ASF projects. Reserve an ID through the internal portal https://cveprocess.apache.org (or email `security@apache.org` with subject `CVE request for ...`). The portal also generates draft announcement text and provides a REVIEW state for Security-Team sign-off. The OSS Helper never reserves, requests, or generates CVE IDs; it only drafts against an already-reserved ID.
- **Severity:** the advisory's `Severity` field is a qualitative rating (Low / Medium / High / Critical). Camel advisory pages do **not** publish a CVSS score or vector string — only the qualitative rating. Compute a CVSS vector solely for the CNA/NVD record if one is required there.
- **Advisory source format:** a Hugo Markdown page named `CVE-YYYY-NNNNN.md`, plus a PGP-clearsigned plaintext `CVE-YYYY-NNNNN.txt.asc` linked from the advisory's `References` section. `/oss-draft-cve` should emit the `.md` page and the matching `.txt` body; the maintainer signs the `.txt` into `.txt.asc` after review.
- **Advisory section structure (exact labels, in order):** `Severity`, `Summary`, `Versions affected`, `Versions fixed`, `Description`, `Notes`, `Mitigation`, `Credit`, `References`. Reproduce these labels exactly when drafting.
- **Advisory template (reference):** https://camel.apache.org/security/CVE-2025-27636.html (rendered) or its source https://github.com/apache/camel-website/blob/main/content/security/CVE-2025-27636.md. The advisory format is shared across all Camel sub-projects. Pass either as the `/oss-draft-cve template=` argument.
- **Publication location:** advisories for all Camel sub-projects are published centrally — commit to `apache/camel-website` under `content/security/` (`CVE-YYYY-NNNNN.md` + `CVE-YYYY-NNNNN.txt.asc`); it renders live at `https://camel.apache.org/security/CVE-YYYY-NNNNN.html`.
- **Signing key:** the Camel release/PMC GPG key published in https://downloads.apache.org/camel/KEYS. `gpg --clearsign CVE-YYYY-NNNNN.txt` produces `CVE-YYYY-NNNNN.txt.asc`. The OSS Helper never runs `gpg` — the maintainer signs after review.
- **Supported release lines / backport branches:** Camel Quarkus aligns with the **Quarkus platform**, not Camel core's version scheme. Each Camel Quarkus release targets a specific Quarkus version and the corresponding Camel core LTS (e.g. Camel Quarkus 3.15.0 ↔ Quarkus 3.15 LTS ↔ Camel 4.8 LTS). Supported lines therefore follow the Quarkus LTS cadence. **Confirm the currently maintained Camel Quarkus lines** via https://camel.apache.org/categories/Roadmap/ and the Camel Quarkus release announcements before choosing backport targets, then derive fixed versions with `git tag --contains <fix-commit> | sort -V` in `apache/camel-quarkus`.
- **Disclosure & announcement:** publish only after the fixed releases are available. Announce to `announce@apache.org` and `users@camel.apache.org`, notify the reporter, and post to `oss-security@lists.openwall.com`; the CVE is pushed to MITRE/NVD through the ASF CNA. The post to `oss-security` is the first public mention of the issue — never disclose specifics before the fix is released.
- **Third-party CVE notes ("not affected" rationale):** where `/oss-analyze-third-party-cve` should record a verified exposure analysis. (TODO: decide whether to track these in release notes, a dedicated security page, or a private PMC tracking issue.)

## CVE Handling Workflow

End-to-end process. The OSS Helper command that assists each step is named in brackets; steps marked *(manual)* are maintainer/PMC actions with no command.

1. **Receipt & confidentiality** — a report arrives privately on `security@apache.org`. Treat all specifics as confidential and acknowledge receipt to the reporter. *(manual)*
2. **Triage** — verify each claim against the current code and git history; assess scope and severity. Decide: valid / invalid / duplicate. [`/oss-triage-security-report`]
3. **Reserve a CVE** — if valid, the PMC reserves a CVE ID through the ASF Security Team via https://cveprocess.apache.org. *(manual — the OSS Helper never reserves IDs)*
4. **Fix privately** — develop the fix without referencing the vulnerability in public commits/PRs; backport to every supported line.
5. **Release** — cut and vote the fixed releases through the normal ASF release process so the patched versions are available before disclosure. *(manual)*
6. **Draft & sign the advisory** — draft `CVE-YYYY-NNNNN.md` and the matching `.txt` body from the triage notes and fix PR, then GPG-clearsign the `.txt` into `.txt.asc`. [`/oss-draft-cve` for the draft; signing is manual]
7. **Publish** — commit the `.md` page and `.txt.asc` to `apache/camel-website` under `content/security/` so the advisory appears at `https://camel.apache.org/security/CVE-YYYY-NNNNN.html`. *(manual)*
8. **Announce & register** — announce to `announce@apache.org`, `users@camel.apache.org`, and `oss-security@lists.openwall.com`, and push the CVE to MITRE/NVD via the ASF CNA. *(manual)*

For a CVE in a third-party dependency (rather than in Camel Quarkus's own code), use [`/oss-analyze-third-party-cve`] to decide exposure and whether a dependency bump or a documented "not affected" note is the right outcome.
