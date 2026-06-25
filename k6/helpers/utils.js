// ============================================
// 通用工具函式（與業務邏輯無關）
// ============================================

export function randomItem(array) {
  return array[Math.floor(Math.random() * array.length)];
}

export function randomInt(min, max) {
  return Math.floor(Math.random() * (max - min + 1)) + min;
}

export function randomAmount(min, max) {
  return (Math.random() * (max - min) + min).toFixed(2);
}
