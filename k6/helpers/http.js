// ============================================
// HTTP 輔助工具
// ============================================

export function getHeaders(token) {
  return {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${token}`,
  };
}

export const jsonHeaders = {
  'Content-Type': 'application/json',
};
