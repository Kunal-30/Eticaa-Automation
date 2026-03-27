import json
import html
from pathlib import Path
from datetime import datetime


def fmt_ms(ms):
    if ms is None:
        return "-"
    seconds = ms / 1000.0
    if seconds < 60:
        return f"{seconds:.2f}s"
    minutes = int(seconds // 60)
    rem = seconds % 60
    return f"{minutes}m {rem:.1f}s"


def fmt_ts(ts):
    if ts is None:
        return "-"
    try:
        return datetime.fromtimestamp(ts / 1000.0).strftime("%Y-%m-%d %H:%M:%S")
    except Exception:
        return "-"


def get_label(labels, key):
    for lb in labels or []:
        if lb.get("name") == key:
            return lb.get("value", "")
    return ""


def flatten_steps(steps, depth=0):
    out = []
    for st in steps or []:
        out.append(
            {
                "name": st.get("name", "Unnamed Step"),
                "status": st.get("status", "unknown"),
                "duration": (st.get("stop", 0) - st.get("start", 0)) if st.get("start") and st.get("stop") else None,
                "depth": depth,
            }
        )
        out.extend(flatten_steps(st.get("steps", []), depth + 1))
    return out


def read_results(allure_dir):
    results = []
    for f in sorted(allure_dir.glob("*-result.json")):
        try:
            data = json.loads(f.read_text(encoding="utf-8"))
        except Exception:
            continue

        labels = data.get("labels", [])
        start = data.get("start")
        stop = data.get("stop")
        duration = (stop - start) if start and stop else None
        status = (data.get("status") or "unknown").lower()

        results.append(
            {
                "uuid": data.get("uuid", f.stem),
                "name": data.get("name", "Unnamed Test"),
                "fullName": data.get("fullName", ""),
                "status": status,
                "statusDetails": (data.get("statusDetails", {}) or {}).get("message", ""),
                "trace": (data.get("statusDetails", {}) or {}).get("trace", ""),
                "suite": get_label(labels, "suite"),
                "parentSuite": get_label(labels, "parentSuite"),
                "subSuite": get_label(labels, "subSuite"),
                "feature": get_label(labels, "feature"),
                "story": get_label(labels, "story"),
                "severity": get_label(labels, "severity"),
                "package": get_label(labels, "package"),
                "testClass": get_label(labels, "testClass"),
                "method": get_label(labels, "testMethod"),
                "start": start,
                "stop": stop,
                "duration": duration,
                "steps": flatten_steps(data.get("steps", [])),
                "attachments": [a.get("name", "attachment") for a in data.get("attachments", [])],
                "parameters": data.get("parameters", []),
            }
        )
    return results


def build_html(results):
    total = len(results)
    passed = sum(1 for r in results if r["status"] == "passed")
    failed = sum(1 for r in results if r["status"] == "failed")
    broken = sum(1 for r in results if r["status"] == "broken")
    skipped = sum(1 for r in results if r["status"] == "skipped")
    unknown = total - passed - failed - broken - skipped
    pass_rate = (passed / total * 100.0) if total else 0.0

    duration_sum = sum((r["duration"] or 0) for r in results)
    longest = sorted(results, key=lambda x: x["duration"] or 0, reverse=True)[:10]

    rows_html = []
    for i, r in enumerate(results, start=1):
        status_cls = f"status-{r['status']}"
        filters = " ".join(
            [
                r["status"],
                r["suite"].lower(),
                r["feature"].lower(),
                r["name"].lower(),
                r["testClass"].lower(),
            ]
        )
        params = ", ".join(f"{p.get('name')}={p.get('value')}" for p in r["parameters"] if p.get("name"))
        steps_preview = " | ".join(
            f"{'  ' * s['depth']}{s['name']} ({s['status']}, {fmt_ms(s['duration'])})" for s in r["steps"][:6]
        )
        if len(r["steps"]) > 6:
            steps_preview += " | ..."

        detail_block = []
        if r["statusDetails"]:
            detail_block.append(f"<div><b>Error:</b> {html.escape(r['statusDetails'])}</div>")
        if r["trace"]:
            detail_block.append(f"<details><summary>Stack Trace</summary><pre>{html.escape(r['trace'])}</pre></details>")
        if steps_preview:
            detail_block.append(f"<div><b>Step Trail:</b> {html.escape(steps_preview)}</div>")
        if r["attachments"]:
            detail_block.append(
                "<div><b>Attachments:</b> "
                + html.escape(", ".join(r["attachments"]))
                + "</div>"
            )

        rows_html.append(
            f"""
            <tr class="data-row {status_cls}" data-filter="{html.escape(filters)}">
              <td>{i}</td>
              <td class="status-pill {status_cls}">{html.escape(r["status"].upper())}</td>
              <td>{html.escape(r["name"])}</td>
              <td>{html.escape(r["suite"] or r["parentSuite"] or "-")}</td>
              <td>{html.escape(r["feature"] or "-")}</td>
              <td>{html.escape(r["testClass"] or "-")}</td>
              <td>{html.escape(r["method"] or "-")}</td>
              <td>{html.escape(params or "-")}</td>
              <td>{fmt_ms(r["duration"])}</td>
              <td>{fmt_ts(r["start"])}</td>
              <td><button class="detail-btn" onclick="toggleDetails('d{i}')">View</button></td>
            </tr>
            <tr id="d{i}" class="detail-row">
              <td colspan="11">
                <div class="detail-card">
                  {' '.join(detail_block) if detail_block else '<i>No additional details.</i>'}
                </div>
              </td>
            </tr>
            """
        )

    longest_rows = []
    for r in longest:
        longest_rows.append(
            f"<tr><td>{html.escape(r['name'])}</td><td>{html.escape(r['status'])}</td><td>{fmt_ms(r['duration'])}</td><td>{html.escape(r['suite'] or '-')}</td></tr>"
        )

    generated_at = datetime.now().strftime("%Y-%m-%d %H:%M:%S")

    return f"""<!doctype html>
<html lang="en">
<head>
  <meta charset="utf-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1" />
  <title>Detailed Allure Executive Report</title>
  <style>
    :root {{
      --bg: #0f172a;
      --card: #111827;
      --muted: #94a3b8;
      --text: #e5e7eb;
      --accent: #22d3ee;
      --pass: #16a34a;
      --fail: #dc2626;
      --skip: #f59e0b;
      --broken: #7c3aed;
      --unknown: #64748b;
      --line: #1f2937;
    }}
    * {{ box-sizing: border-box; }}
    body {{
      margin: 0;
      background: radial-gradient(circle at 20% 0%, #1e293b, var(--bg) 55%);
      color: var(--text);
      font-family: "Segoe UI", Tahoma, sans-serif;
    }}
    .container {{ max-width: 1600px; margin: 0 auto; padding: 22px; }}
    .header {{ display: flex; justify-content: space-between; align-items: end; flex-wrap: wrap; gap: 12px; }}
    h1 {{ margin: 0; font-size: 1.8rem; }}
    .meta {{ color: var(--muted); font-size: 0.95rem; }}
    .grid {{
      margin-top: 18px;
      display: grid;
      grid-template-columns: repeat(6, minmax(140px, 1fr));
      gap: 10px;
    }}
    .card {{
      background: linear-gradient(180deg, #111827, #0b1220);
      border: 1px solid var(--line);
      border-radius: 12px;
      padding: 12px;
      box-shadow: 0 8px 20px rgba(0,0,0,0.25);
    }}
    .k {{ color: var(--muted); font-size: 0.82rem; }}
    .v {{ margin-top: 6px; font-size: 1.4rem; font-weight: 700; }}
    .toolbar {{ margin-top: 18px; display: flex; gap: 8px; flex-wrap: wrap; }}
    .btn, .input {{
      border: 1px solid var(--line);
      background: #0b1220;
      color: var(--text);
      border-radius: 10px;
      padding: 8px 12px;
      font-size: 0.9rem;
    }}
    .btn {{ cursor: pointer; }}
    .btn.active {{ border-color: var(--accent); box-shadow: 0 0 0 1px var(--accent); }}
    .table-wrap {{
      margin-top: 14px;
      border: 1px solid var(--line);
      border-radius: 12px;
      overflow: auto;
      background: var(--card);
    }}
    table {{ width: 100%; border-collapse: collapse; min-width: 1400px; }}
    th, td {{ border-bottom: 1px solid var(--line); padding: 10px 8px; text-align: left; vertical-align: top; }}
    th {{ position: sticky; top: 0; background: #0b1220; z-index: 1; }}
    tr:hover td {{ background: rgba(255,255,255,0.02); }}
    .status-pill {{
      font-weight: 700;
      display: inline-block;
      border-radius: 999px;
      padding: 3px 8px;
      font-size: 0.75rem;
    }}
    .status-passed .status-pill {{ background: rgba(22,163,74,0.18); color: #22c55e; }}
    .status-failed .status-pill {{ background: rgba(220,38,38,0.18); color: #f87171; }}
    .status-broken .status-pill {{ background: rgba(124,58,237,0.2); color: #a78bfa; }}
    .status-skipped .status-pill {{ background: rgba(245,158,11,0.2); color: #fbbf24; }}
    .status-unknown .status-pill {{ background: rgba(100,116,139,0.2); color: #cbd5e1; }}
    .detail-row {{ display: none; }}
    .detail-card {{
      border: 1px solid #243043;
      border-radius: 10px;
      padding: 10px;
      background: #0a1020;
      line-height: 1.5;
    }}
    pre {{
      background: #020617;
      border: 1px solid #1e293b;
      padding: 10px;
      border-radius: 8px;
      max-height: 300px;
      overflow: auto;
      white-space: pre-wrap;
      word-break: break-word;
    }}
    .detail-btn {{
      border: 1px solid #334155;
      color: #e2e8f0;
      background: #0f172a;
      border-radius: 8px;
      padding: 5px 9px;
      cursor: pointer;
    }}
    .section-title {{ margin-top: 24px; font-size: 1.2rem; }}
  </style>
</head>
<body>
  <div class="container">
    <div class="header">
      <div>
        <h1>Detailed Automation Report (Allure Data)</h1>
        <div class="meta">Generated at: {generated_at}</div>
      </div>
      <div class="meta">Audience: Engineering + Leadership Review</div>
    </div>

    <div class="grid">
      <div class="card"><div class="k">Total Tests</div><div class="v">{total}</div></div>
      <div class="card"><div class="k">Passed</div><div class="v" style="color:#22c55e">{passed}</div></div>
      <div class="card"><div class="k">Failed</div><div class="v" style="color:#f87171">{failed}</div></div>
      <div class="card"><div class="k">Broken</div><div class="v" style="color:#a78bfa">{broken}</div></div>
      <div class="card"><div class="k">Skipped</div><div class="v" style="color:#fbbf24">{skipped}</div></div>
      <div class="card"><div class="k">Pass Rate</div><div class="v">{pass_rate:.2f}%</div></div>
      <div class="card"><div class="k">Unknown</div><div class="v" style="color:#cbd5e1">{unknown}</div></div>
      <div class="card"><div class="k">Total Runtime</div><div class="v">{fmt_ms(duration_sum)}</div></div>
    </div>

    <div class="toolbar">
      <button class="btn active" onclick="setFilter('all', this)">All</button>
      <button class="btn" onclick="setFilter('passed', this)">Passed</button>
      <button class="btn" onclick="setFilter('failed', this)">Failed</button>
      <button class="btn" onclick="setFilter('broken', this)">Broken</button>
      <button class="btn" onclick="setFilter('skipped', this)">Skipped</button>
      <input id="searchBox" class="input" type="text" placeholder="Search suite/feature/test/method..." oninput="applyFilters()" />
    </div>

    <div class="table-wrap">
      <table id="reportTable">
        <thead>
          <tr>
            <th>#</th>
            <th>Status</th>
            <th>Test Name</th>
            <th>Suite</th>
            <th>Feature</th>
            <th>Class</th>
            <th>Method</th>
            <th>Parameters</th>
            <th>Duration</th>
            <th>Start Time</th>
            <th>Details</th>
          </tr>
        </thead>
        <tbody>
          {''.join(rows_html)}
        </tbody>
      </table>
    </div>

    <h2 class="section-title">Top 10 Longest Tests</h2>
    <div class="table-wrap">
      <table>
        <thead>
          <tr>
            <th>Test Name</th>
            <th>Status</th>
            <th>Duration</th>
            <th>Suite</th>
          </tr>
        </thead>
        <tbody>
          {''.join(longest_rows) if longest_rows else '<tr><td colspan="4">No test results found.</td></tr>'}
        </tbody>
      </table>
    </div>
  </div>

  <script>
    let currentStatus = "all";
    function toggleDetails(id) {{
      const row = document.getElementById(id);
      row.style.display = (row.style.display === "table-row") ? "none" : "table-row";
    }}

    function setFilter(status, btn) {{
      currentStatus = status;
      document.querySelectorAll(".toolbar .btn").forEach(b => b.classList.remove("active"));
      btn.classList.add("active");
      applyFilters();
    }}

    function applyFilters() {{
      const q = (document.getElementById("searchBox").value || "").toLowerCase().trim();
      document.querySelectorAll("tr.data-row").forEach((row) => {{
        const text = row.getAttribute("data-filter") || "";
        const statusOk = currentStatus === "all" || text.includes(currentStatus);
        const searchOk = q.length === 0 || text.includes(q);
        const show = statusOk && searchOk;
        row.style.display = show ? "table-row" : "none";
        const detail = row.nextElementSibling;
        if (!show && detail && detail.classList.contains("detail-row")) {{
          detail.style.display = "none";
        }}
      }});
    }}
  </script>
</body>
</html>
"""


def main():
    base = Path(__file__).resolve().parents[1]
    allure_dir = base / "allure-results"
    report_dir = base / "custom-report"
    report_dir.mkdir(parents=True, exist_ok=True)
    out = report_dir / "detailed-allure-report.html"

    if not allure_dir.exists():
        raise SystemExit(f"allure-results not found at: {allure_dir}")

    results = read_results(allure_dir)
    html_doc = build_html(results)
    out.write_text(html_doc, encoding="utf-8")
    print(f"Detailed report generated: {out}")


if __name__ == "__main__":
    main()
