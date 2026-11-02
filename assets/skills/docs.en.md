# Skill: documentation

Goal: text that lets a person **reproduce an action**, not a list of obvious
statements. Documentation is written for a specific reader and product version.

## 1. Before writing
1. Identify the reader: beginner, experienced user, developer.
2. Identify the goal: install, configure, integrate, troubleshoot.
3. Gather facts from existing files, code and configuration. Never invent
   commands, paths, flags, versions, limits or behavior.
4. If something is unknown, verify it or mark it “not verified”.

## 2. Structure
1. What it is and why it matters (2–4 sentences).
2. Requirements: OS, versions, permissions.
3. Installation or connection — step by step.
4. Quick start: the smallest working example.
5. Main usage scenarios.
6. Reference: parameters, commands, fields, formats.
7. Limits and security.
8. Troubleshooting.
9. Changelog or next steps.

Start each step with a verb: “Tap”, “Paste”, “Run”.
One step = one action. Do not mix verification and configuration in one bullet.

## 3. Format
- Markdown: `.md` via `write`; DOCX/PDF via `doc`.
- Headings `#`/`##`/`###`, no skipped levels.
- Code in fenced blocks with a language: ```bash, ```json, ```http.
- Paths and names in monospace: `/sdcard/Download/FunaFiles`.
- Warnings as a separate block **Warning:** or **Security:**, not buried in a
  paragraph.
- Markdown tables are fine; DOCX/PDF are not (see the docx/pdf skills).
- Link to real files and sections, never to invented URLs.
- One language per document; consistent terminology.

## 4. README example
```markdown
# Name

One sentence: what the product does.

## Requirements
- Android 8.0+
- 50 MB free space

## Quick start
1. Install the APK.
2. Open the app and sign in.
3. Send your first message.

## Configuration
- **Address** — defaults to `http://127.0.0.1:8787`.
- **Key** — issued in Settings.

## Troubleshooting
### The server does not respond
Check that the app is open and the port is free.
```

## 5. Accuracy
- Commands are copy-paste exact: no “approximately”, no stray spaces.
- If you show output, mark it as an example and do not fabricate a full log.
- Secrets (tokens, passwords, keys) only as `fk-…`, `sk-…`, never real values.
- Numbers and limits come from code or configuration, not memory.
- Versions come from `build.sh`/manifest, not guesses.

## 6. Review before delivery
- Walk through it as a beginner: are there enough steps to reach the result?
- Every command states where to run it (phone, terminal, PC).
- Every link points to an existing file or section.
- No contradictions between sections.
- No invented future features without a “planned” label.

## 7. Checklist
- [ ] Purpose, reader and requirements are stated.
- [ ] A minimal working example exists.
- [ ] Steps are ordered and start with verbs.
- [ ] Commands, paths and limits match the real product.
- [ ] No secrets or invented sources.
- [ ] A troubleshooting and next-steps section exists.
