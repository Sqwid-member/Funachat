# Skill: HTML artifact design

An artifact is a **single self-contained HTML document** inside a ` ```html `
block. It runs sandboxed on a phone, so external resources, storage and the app
bridge are unavailable.

## 1. When to build an artifact
- Interactive page: calculator, game, demo, visualization, dashboard.
- Do not use HTML for plain text, lists or tables — Markdown is better.

## 2. Required skeleton
```html
<!doctype html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1, viewport-fit=cover">
<title>Short title</title>
<style>/* all CSS here */</style>
</head>
<body>
<!-- markup -->
<script>/* all JS here */</script>
</body>
</html>
```
- All CSS and JS inline. No `<link>`, CDN, Google Fonts, remote images or
  `fetch`/`XMLHttpRequest`.
- Images: inline SVG or CSS gradients.
- `lang` matches the artifact UI language.

## 3. Sandbox: what does not work
- `localStorage`, `sessionStorage`, cookies, IndexedDB throw. Keep state in
  variables; wrap any storage in `try/catch`.
- No file access, clipboard or parent-page access.
- `alert`, `confirm`, `prompt` technically work but look crude — render inline
  messages instead.
- External `http(s)` links open in the browser; internal `#anchor` links work.

## 4. Mobile first
- Target width 360–400 CSS px. No horizontal scroll:
  `*{box-sizing:border-box}`, `img,svg,video{max-width:100%}`,
  `overflow-wrap:anywhere` for long words.
- Wide tables do not fit — use “key — value” cards or scroll only inside the
  table container.
- Primary actions at the bottom, at least 44×44 px, 8–12 px apart.
- Respect `env(safe-area-inset-bottom)` for bottom bars.
- Desktop via `@media (min-width:600px)`, never the other way around.

## 5. FunaChat visual language
Dark, calm, no neon overload.
```
--bg:#0F1216; --surf:#151A20; --on:#E3E7EC; --var:#9BA6B3;
--dim:#6F7A87; --outline:#313A45; --primary:#A8C7FA;
--on-primary:#0B2D5E; --err:#FFB4AB; --ok:#8FD9A8;
```
- Background `--bg`, cards `--surf`, borders `--outline`, accent `--primary`.
- Font: `system-ui, -apple-system, "Segoe UI", Roboto, sans-serif`.
  Base 16 px, `line-height:1.5`, max ~65 characters per line.
- Headings 1.25–1.75 rem, at most two levels.
- Radius 12–16 px, near-invisible shadows, no harsh gradients.
- Text contrast at least 4.5:1.

## 6. Accessibility and behavior
- Semantics: `<button>`, `<label>`, `<main>`, `<nav>`; never a `<div>` button.
- Visible `:focus-visible`, keyboard support, `aria-live` for results.
- Motion 150–250 ms; respect `prefers-reduced-motion: reduce`.
- Always show state: loading, empty, error, success.
- No invented data: label demos as “demo data”.

## 7. Performance
- Aim for under ~100 KB; no heavy libraries.
- Do not create thousands of DOM nodes; use `<canvas>` or SVG for charts.
- Animate with `requestAnimationFrame`, not `setInterval(…, 1)`.
- No infinite loops or 10-second synchronous computations.

## 8. Checklist
- [ ] One file, all CSS/JS inline, no external requests.
- [ ] No horizontal scroll at 360 px.
- [ ] Buttons ≥44 px, contrast ≥4.5:1, focus state present.
- [ ] No unguarded `localStorage`, no remote images.
- [ ] `<title>` is set — it becomes the file name.
- [ ] Everything is readable on a phone without zooming.
