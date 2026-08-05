# Project Standards

This rule file contains build tools, commands, and code style constraints for the project. Commands read this file to determine how to build, test, and format code.

- **Build tool:** Maven (wrapper `./mvnw` provided in root — always use it instead of bare `mvn`)
- **Build command:** `./mvnw clean install` (full build with tests) or `./mvnw clean install -Dquickly` (fast build, no tests)
- **Test command:** `./mvnw verify` (JVM integration tests) or `./mvnw verify -pl integration-tests/<module> -Dnative` (native tests — only run for specific modules under `integration-tests/` or `integration-test-groups/`, never from the project root)
- **Format command:** `./mvnw process-resources -Pformat` (run from project root, formats code and updates metadata)
- **Module-specific build:** yes (use `-pl` from root, e.g. `./mvnw clean install -pl extensions/kafka -am`)
- **Parallelized Maven:** no, unless tests are skipped and it is not a native build (e.g. `./mvnw clean install -Dquickly -T1C`). Tests cause port clashes and native builds exhaust CPU, memory and disk I/O
- **Code style restrictions:**
  - Do NOT use Lombok (unless already present in the file)
  - Records are allowed for internal/non-API classes; do NOT convert existing public API classes to Records
  - Do NOT change public API signatures without justification
  - Do NOT add new dependencies without justification
  - Maintain backwards compatibility for public APIs
  - Do NOT directly modify generated files under `docs/modules` or `src/main/generated`
  - Do NOT use dynamic class loading or reflection (impacts native compilation)
  - Run `./mvnw process-resources -Pformat` before committing
