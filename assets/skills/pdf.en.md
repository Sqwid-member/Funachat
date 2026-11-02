# Skill: PDF

Goal: a document that reads well on screen and on paper.
PDF is produced by the `doc` tool (`"format":"pdf"`).

## 1. Before you write
1. Clarify the purpose: reading on a phone, printing, sending.
   If unstated, make a clean document for phone reading.
2. Outline: 3–7 sections, each with 2–5 paragraphs.
3. Verify facts. Never invent numbers, dates, sources or links.
4. Draft the full text, then call `doc` once. Do not build the PDF in pieces.

## 2. What the generator supports
- A4, 46 pt margins, sans-serif: title 18 pt, subheading 13.5 pt, body 11.5 pt.
- `# Subheading` — bold section line.
- `- item` or `* item` — bullet `•`.
- A blank line — spacing between paragraphs.
- Automatic line wrapping and page breaks.

**Not supported:** tables, images, colors, links, headers/footers, page numbers,
a table of contents, inline bold/italic, nested lists. `**`, `__`, `` ` ``
are stripped. Use a “Parameter — value” or “Step 1 — result” list instead.

## 3. Limits
- Text up to 1.5M characters; beyond that a truncation note is added.
- At most 250 pages; page 251 says the document was cut.
- Very long URLs and words are split by character — shorten them.
- One call = one file.

## 4. Structure
1. Introduction: topic, audience, purpose (2–3 sentences).
2. Body: general to specific.
3. Practice: steps, examples, commands.
4. Conclusion or next steps.
`title` becomes the first-page heading — do not repeat it in `text`.

## 5. Call
```json
{"tool":"doc","format":"pdf","title":"Document title","text":"# Section\n\nText…","path":"docs/name.pdf"}
```
`path` is optional: without it the file lands in `docs/<title>.pdf`.
The extension is always reconciled with the format.

## 6. After creation
- 1–3 lines: what is inside and roughly how many pages.
- File card:
```attach
/sdcard/Download/FunaFiles/docs/name.pdf
```

## Checklist
- [ ] No tables, images or Markdown symbols that would become garbage.
- [ ] Section titles are specific, not “General”.
- [ ] No invented facts or sources.
- [ ] The document was created once.
- [ ] A file card with an absolute path was added.
