# Skill: DOCX (Word)

Goal: a clean, **editable** document, not a text file full of markup.
DOCX is produced by the `doc` tool (`"format":"docx"`).

## 1. Before you write
1. Define purpose, audience and length. If the user did not specify, make a
   reasonable assumption and state it in one line.
2. Outline: 3–7 sections, each with 2–5 paragraphs.
3. Gather facts. Never invent numbers, dates, quotes, sources or links.
4. Draft the full text first, then call `doc` once — do not build the file in
   pieces.

## 2. Markup the generator understands
- `# Heading` — section subheading (bold, larger). Use a single `#`.
- `- item` or `* item` — bulleted list with `•`.
- A blank line — spacing between paragraphs.
- Write numbering as text: `1. …`, `2. …`.

**Not supported:** tables, images, real Heading styles, inline bold/italic,
HTML, nested lists. Do not write `**`, `*`, `` ` ``, `<b>`, `|table|` — the
generator strips them and the structure is lost. Use a “Parameter — value” or
“Step 1 — result” list instead of a table.

## 3. Structure of a good document
1. Introduction: why this document exists and who it is for (2–3 sentences).
2. Body: sections from general to specific.
3. Practice: steps, examples, commands.
4. Conclusion or next steps.
The `title` becomes the document title — do not repeat it as the first line.

## 4. Length and style
- 3–10 pages is normal. The hard ceiling is 1.5M characters, but nobody reads
  such a document.
- One paragraph = one idea, 3–6 sentences.
- Active voice, concrete verbs, no filler.
- Keep terminology consistent; expand an abbreviation on first use.

## 5. Call
```json
{"tool":"doc","format":"docx","title":"Document title","text":"# Section\n\nText…","path":"docs/name.docx"}
```
`path` is optional — the file lands in `docs/<title>.docx`. The extension is
always reconciled with the format automatically.

## 6. After creation
- Say what is inside in one–three lines.
- Add the file card:
```attach
/sdcard/Download/FunaFiles/docs/name.docx
```

## Checklist
- [ ] There is an introduction and a conclusion/next steps.
- [ ] No Markdown symbols that would become garbage.
- [ ] No invented facts, sources or links.
- [ ] Section titles are specific, not “General”.
- [ ] The file was created once, not five times under different names.
