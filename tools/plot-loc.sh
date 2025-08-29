#!/usr/bin/env bash
set -euo pipefail

# Paths (works from any subfolder of the repo)
REPO_ROOT="$(git rev-parse --show-toplevel)"
CSV="$REPO_ROOT/metrics/loc_history.csv"
OUT="$REPO_ROOT/metrics/loc_history.html"

if [ ! -f "$CSV" ]; then
  echo "ERROR: $CSV not found. Run a snapshot first (e.g.,: git loc)."
  exit 1
fi

# Build JS arrays from CSV
# header: datetime,branch,tag,commit,kotlin_loc,xml_loc,total_loc
labels=$(awk -F, 'NR>1 { gsub(/\r/,""); printf("%s\"%s\"",sep,$1); sep="," }' "$CSV")
kotlin=$(awk -F, 'NR>1 { gsub(/\r/,""); printf("%s%s",sep,$5); sep="," }' "$CSV")
xml=$(awk   -F, 'NR>1 { gsub(/\r/,""); printf("%s%s",sep,$6); sep="," }' "$CSV")
total=$(awk -F, 'NR>1 { gsub(/\r/,""); printf("%s%s",sep,$7); sep="," }' "$CSV")

mkdir -p "$(dirname "$OUT")"

cat > "$OUT" <<EOF
<!doctype html>
<html lang="en">
<head>
<meta charset="utf-8"/>
<meta name="viewport" content="width=device-width,initial-scale=1"/>
<title>Nido LOC History</title>
<style>
  body { font-family: system-ui, -apple-system, Segoe UI, Roboto, Arial, sans-serif; margin:24px; color:#111; }
  h1 { margin:0 0 12px; font-size:20px; }
  .wrap { max-width:1100px; }
  #chart { width:100%; height:520px; }
  .meta { margin:8px 0 16px; color:#444; font-size:12px; }
  button { padding:6px 10px; border:1px solid #ddd; background:#f8f8f8; cursor:pointer; border-radius:6px; }
  button:hover { background:#eee; }
</style>
</head>
<body>
<div class="wrap">
  <h1>LOC History</h1>
  <div class="meta">Source: metrics/loc_history.csv · Toggle series via legend. Click “Download PNG” to export.</div>
  <canvas id="chart"></canvas>
  <div style="margin-top:10px;">
    <button id="dl">Download PNG</button>
  </div>
</div>

<script src="https://cdn.jsdelivr.net/npm/chart.js@4.4.3"></script>
<script>
const labels = [${labels}];
const kotlin = [${kotlin}];
const xml     = [${xml}];
const total   = [${total}];

const ctx = document.getElementById('chart').getContext('2d');
const chart = new Chart(ctx, {
  type: 'line',
  data: {
    labels,
    datasets: [
      { label: 'Total LOC',  data: total,  tension: 0.2, borderWidth: 2, pointRadius: 2 },
      { label: 'Kotlin LOC', data: kotlin, tension: 0.2, borderWidth: 2, pointRadius: 2 },
      { label: 'XML LOC',    data: xml,    tension: 0.2, borderWidth: 2, pointRadius: 2 },
    ]
  },
  options: {
    responsive: true,
    interaction: { mode: 'nearest', intersect: false },
    plugins: { legend: { position: 'top' } },
    scales: {
      x: { title: { display: true, text: 'Snapshot datetime' } },
      y: { title: { display: true, text: 'Lines of code' }, beginAtZero: true }
    }
  }
});

document.getElementById('dl').addEventListener('click', () => {
  const a = document.createElement('a');
  a.href = chart.toBase64Image();
  a.download = 'loc_history.png';
  a.click();
});
</script>
</body>
</html>
EOF

echo "Wrote: $OUT"
echo "Open it manually in your browser."
