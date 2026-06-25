// ============================================
// 自訂 k6 指標
// ============================================
import { Rate, Trend } from 'k6/metrics';

export const errorRate = new Rate('errors');
export const loginDuration = new Trend('login_duration');
export const transferDuration = new Trend('transfer_duration');
export const createAccountDuration = new Trend('create_account_duration');
export const registerDuration = new Trend('register_duration');
