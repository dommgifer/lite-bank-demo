#!/usr/bin/env python3
"""從 Grafana-managed 告警 provisioning 產生 K8s PrometheusRule 的 spec 資料檔。

單一事實來源 = slo-alerts.yaml（本機 docker-compose Grafana 用）；K8s 產物由本腳本衍生，
改規則只改 slo-alerts.yaml 後重跑本腳本。

用法：python grafana/alerting/gen_prometheusrule.py
輸出：helm/litebank/files/slo-prometheusrule.spec.yaml（純 groups: 資料，含 Alertmanager
      的 {{ $labels.* }} 模板字面量——故用 Helm .Files.Get 讀取，避免被 Helm 當 template 解析）。
Helm 包裝：helm/litebank/templates/prometheusrules.yaml（靜態，metadata + .Files.Get）。

轉換：Grafana rule 的 refId=A model.expr 已內嵌 threshold（回 series 即代表燃燒），
     直接當 PrometheusRule 的 expr；for/labels/annotations 照搬。
"""
import re, sys, yaml

SRC = "grafana/alerting/slo-alerts.yaml"
OUT = "helm/litebank/files/slo-prometheusrule.spec.yaml"

def slug(s):
    s = s.lower().replace("litebank", "").strip()
    return "litebank-" + re.sub(r"[^a-z0-9]+", "-", s).strip("-")

def camel(uid):
    return "".join(p.capitalize() for p in re.split(r"[^a-zA-Z0-9]+", uid) if p)

doc = yaml.safe_load(open(SRC, encoding="utf-8"))
groups_out, n_rules = [], 0
for g in doc["groups"]:
    rules_out = []
    for r in g["rules"]:
        exprA = next((d["model"]["expr"].strip() for d in r.get("data", [])
                      if d.get("refId") == "A"), None)
        if exprA is None:
            print("WARN: rule %s 無 refId A，跳過" % r.get("uid"), file=sys.stderr)
            continue
        rule = {"alert": camel(r["uid"]), "expr": exprA}
        f = str(r.get("for", "0m")).strip()
        if f and f != "0m":
            rule["for"] = f
        if r.get("labels"):
            rule["labels"] = dict(r["labels"])
        if r.get("annotations"):
            rule["annotations"] = dict(r["annotations"])
        rules_out.append(rule)
        n_rules += 1
    groups_out.append({"name": slug(g["name"]),
                       "interval": str(g.get("interval", "1m")),
                       "rules": rules_out})

banner = ("# 由 grafana/alerting/gen_prometheusrule.py 從 slo-alerts.yaml 自動生成，請勿手改。\n"
          "# 改規則 → 改 slo-alerts.yaml → 重跑該腳本。經 templates/prometheusrules.yaml 的\n"
          "# .Files.Get 併入 PrometheusRule（保留 {{ $labels.* }} 字面量不被 Helm 解析）。\n")
body = yaml.dump({"groups": groups_out}, sort_keys=False, allow_unicode=True,
                 default_flow_style=False, width=100000)
open(OUT, "w", encoding="utf-8").write(banner + body)
print("生成 %s：%d 條規則、%d 個 group" % (OUT, n_rules, len(groups_out)))
