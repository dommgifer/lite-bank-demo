-- V10: Seed bulk load-test users for k6 traffic simulation.
--
-- 動機：原本只有 alice/bob/charlie 三個 demo 用戶，k6 的 50 個 VU 全部擠在這
-- 幾個帳戶上做存提款，造成「假熱點」——transaction-service 的悲觀鎖把同一帳戶
-- 的寫入序列化，P99 飆到接近 timeout(~10s)。這對「個人帳戶」並不真實。
-- 這裡預先 seed 一個夠大的用戶池，讓 k6 用 __VU 綁定不同用戶、分散打各自帳戶，
-- 還原真實的低碰撞流量。熱點情境改由 k6 的 MODE=hot 刻意模擬，不再是副作用。
--
-- 用戶：loadtest_0001 .. loadtest_0200，密碼皆為 'password123'
--       (沿用 V1 demo 用戶的 bcrypt(cost=10) hash，k6 登入邏輯不必改)

INSERT INTO users (username, email, password_hash)
SELECT
    'loadtest_' || LPAD(g::text, 4, '0'),
    'loadtest_' || LPAD(g::text, 4, '0') || '@loadtest.local',
    '$2a$10$qec4Yv7E8SELBzIInFacROUk3.k82Zag3/c2hLbSuCCR85qneH9h2'
FROM generate_series(1, 200) AS g;

-- 帳戶：每個 loadtest 用戶 4 個帳戶 (USD/EUR/TWD/JPY)，符合 V5 的
--       unique_user_currency 約束；餘額給到 1 億，確保 2 小時持續提款也不會
--       觸發 chk_balance_positive 而失敗、污染可用性數據。
--
-- account_number：使用 branch '900'(demo 用戶是 '001')做物理隔離，
--                 配合全域遞增序號，保證 V6 的 unique_account_number 不衝突。
--                 type code 沿用 V6 慣例：TWD=01(本幣儲蓄)，其餘=03(外幣)。

INSERT INTO accounts (user_id, currency, balance, account_number)
SELECT
    u.user_id,
    c.currency,
    c.balance,
    '900-' || c.type || '-' || LPAD((ROW_NUMBER() OVER (ORDER BY u.user_id, c.currency))::text, 7, '0')
FROM (SELECT user_id FROM users WHERE username LIKE 'loadtest_%') AS u
CROSS JOIN (VALUES
    ('USD', '03', 100000000.00),
    ('EUR', '03', 100000000.00),
    ('TWD', '01', 100000000.00),
    ('JPY', '03', 100000000.00)
) AS c(currency, type, balance);
