<!--
  ~ SPDX-FileCopyrightText: 2026 Piotr P. Karwasz <piotr@github.copernik.eu>
  ~ SPDX-License-Identifier: Apache-2.0
  -->

# AGENTS.md

Guidance for AI coding agents working in this repository.

## Formatting

### Javadoc

1. **Wrap at 160 columns**, not 80.
   Count the leading ` * ` prefix as part of the line.
   Keep wrapping natural (do not break inside a `{@link ...}` or across a tag boundary if it can be avoided).
2. **Getter summaries start with "Gets"; setter summaries start with "Sets".**
   Prefer these verb forms over "Returns".
   For non getter/setter methods keep a short verb phrase (for example "Configures", "Hardens", "Dispatches").
3. **Javadoc style**:
   Key rules:
   - Open on its own line with `/**`,
     close on its own line with ` */`,
     align intermediate lines with a single leading ` * `.
   - The first sentence is a short summary fragment.
     It is the only part that appears in class/method index listings, so keep it self-contained.
     Details (what is hardened, thread-safety, edge cases) belong in subsequent paragraphs, not bolted onto the summary.
   - Separate paragraphs with a blank `*` line.
     Prefix every paragraph *except the first* with a `<p>` tag at the start of the first word.
     **Do** close with `</p>`.
   - Block tags appear in the order `@param`, `@return`, `@throws`, `@deprecated`. No empty descriptions.
   - Javadoc is present on every public class and every public or protected member,
     with the standard exceptions (overrides, self-explanatory members, trivial `equals`/`hashCode`).

### Prose

- No em-dashes (`—`) in Javadoc, comments, commit messages, or documentation.
  Use commas, colons, parentheses, or a fresh sentence instead. This applies to
  HTML entities too (`&mdash;`).

### Commits

- Use the `Assisted-By:` trailer (not `Co-Authored-By:`).