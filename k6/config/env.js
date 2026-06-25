// ============================================
// 環境配置與常數
// ============================================

// 結尾斜線一律剝掉，避免與 `${BASE_URL}/api/...` 組出 `//api`(ingress 會回 404)
export const BASE_URL = (__ENV.BASE_URL || 'http://localhost:9000').replace(/\/+$/, '');

// 基礎測試用戶（只定義帳密，帳戶資料由 API 動態取得）
export const BASE_USERS = [
  { username: 'alice',   password: 'password123' },
  { username: 'bob',     password: 'password123' },
  { username: 'charlie', password: 'password123' },
];

// 支援的幣別（用於建立新帳戶時的選擇）
export const SUPPORTED_CURRENCIES = ['USD', 'EUR', 'TWD', 'JPY', 'GBP', 'CNY'];

// 查詢匯率時使用的幣別
export const RATE_CURRENCIES = ['USD', 'EUR', 'TWD', 'JPY'];

// 動態用戶池：register 建立的新用戶，每個 VU 獨立，上限 50 個
export const dynamicUsers = [];
