#!/usr/bin/env python3
"""把 slo-alerts.yaml 同步到中央 Grafana（provisioning API，保持 UI 可編輯）。

用途：slo-alerts.yaml 是規則的編輯來源；本腳本用 provisioning API upsert 到中央 Grafana，
     並帶 X-Disable-Provenance 讓規則在 UI 仍可手改（provenance=None）。

環境變數：
  GRAFANA_URL   (預設 http://grafana.192.168.1.21.nip.io)
  GRAFANA_USER  (預設 admin)
  GRAFANA_PASS  (必填，如 openstack)
選項：--only <uid> 只推一條（測試用）；--dry 只印不送。

轉換：Grafana provisioning 檔的 rule 物件 ≈ provisioning API 的 rule；補 folderUID/ruleGroup/orgID。
     folderUID 依 folder 名（如 'Lite Bank' / 'LiteBank'）查詢取得。
"""
import os, sys, json, base64, urllib.request, urllib.error
import yaml

SRC = "grafana/alerting/slo-alerts.yaml"
URL = os.environ.get("GRAFANA_URL", "http://grafana.192.168.1.21.nip.io").rstrip("/")
USER = os.environ.get("GRAFANA_USER", "admin")
PASS = os.environ.get("GRAFANA_PASS")
ONLY = None
DRY = "--dry" in sys.argv
if "--only" in sys.argv:
    ONLY = sys.argv[sys.argv.index("--only") + 1]

if not PASS:
    print("ERROR: 需設 GRAFANA_PASS", file=sys.stderr); sys.exit(1)

AUTH = "Basic " + base64.b64encode(f"{USER}:{PASS}".encode()).decode()

def api(method, path, body=None, extra_headers=None):
    req = urllib.request.Request(URL + path, method=method)
    req.add_header("Authorization", AUTH)
    req.add_header("Accept", "application/json")
    if body is not None:
        data = json.dumps(body).encode()
        req.add_header("Content-Type", "application/json")
    else:
        data = None
    for k, v in (extra_headers or {}).items():
        req.add_header(k, v)
    try:
        with urllib.request.urlopen(req, data=data, timeout=15) as resp:
            raw = resp.read().decode()
            return resp.status, (json.loads(raw) if raw.strip() else None)
    except urllib.error.HTTPError as e:
        return e.code, e.read().decode(errors="replace")[:400]

# 1) 取 folder 名 → uid 對照（正規化：去空格、小寫，容忍 'Lite Bank' vs 'LiteBank'）
_, folders = api("GET", "/api/folders")
def _norm(s): return "".join(s.split()).lower()
folder_uid = {_norm(f["title"]): f["uid"] for f in folders}

# 2) 讀來源
doc = yaml.safe_load(open(SRC, encoding="utf-8"))
n_ok = n_fail = 0
for g in doc["groups"]:
    fuid = folder_uid.get(_norm(g["folder"]))
    if not fuid:
        print("SKIP group %s：folder %r 不存在" % (g["name"], g["folder"]), file=sys.stderr)
        continue
    for r in g["rules"]:
        if ONLY and r["uid"] != ONLY:
            continue
        payload = {
            "uid": r["uid"], "title": r["title"], "condition": r.get("condition", "C"),
            "folderUID": fuid, "ruleGroup": g["name"], "orgID": 1,
            "for": str(r.get("for", "0m")), "noDataState": r.get("noDataState", "OK"),
            "execErrState": r.get("execErrState", "Error"),
            "labels": r.get("labels", {}), "annotations": r.get("annotations", {}),
            "data": r["data"], "isPaused": False,
        }
        if DRY:
            print("DRY PUT", r["uid"], "labels=", payload["labels"]); continue
        # upsert：PUT by uid（provisioning API），X-Disable-Provenance 保持 UI 可編輯
        code, resp = api("PUT", "/api/v1/provisioning/alert-rules/" + r["uid"],
                         body=payload, extra_headers={"X-Disable-Provenance": "true"})
        if code in (200, 201):
            n_ok += 1
        else:
            n_fail += 1
            print("FAIL %s HTTP %s: %s" % (r["uid"], code, resp), file=sys.stderr)
print("完成：成功 %d，失敗 %d%s" % (n_ok, n_fail, "（DRY）" if DRY else ""))
