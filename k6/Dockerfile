# ============================================
# Lite Bank k6 流量產生器 — 自帶腳本映像
#
# 官方 grafana/k6 映像不含我們的測試腳本(本地是用 -v 掛載)。
# K8s 上沒有本機目錄可掛，所以把 traffic.js 等腳本 bake 進映像，
# 由 GitHub Action 推到 ghcr.io，K8s 直接 pull 即可跑。
# ============================================
FROM grafana/k6:0.50.0

# grafana/k6 預設以非 root user(12345) 執行，/scripts 由我們建立並放腳本
COPY --chown=12345:12345 . /scripts

WORKDIR /scripts

# 預設行為：跑 traffic.js。實際 VUS/DURATION/MODE 由 K8s env 注入。
ENTRYPOINT ["k6"]
CMD ["run", "/scripts/traffic.js"]
