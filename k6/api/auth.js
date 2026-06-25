// ============================================
// Auth API（user-service）
// ============================================
import http from 'k6/http';
import { check } from 'k6';
import { BASE_URL, dynamicUsers } from '../config/env.js';
import { jsonHeaders } from '../helpers/http.js';
import { errorRate, loginDuration, registerDuration } from '../helpers/metrics.js';

// 登入：回傳 { token, userId } 或 null
export function login(user) {
  const startTime = new Date();
  const res = http.post(
    `${BASE_URL}/api/v1/auth/login`,
    JSON.stringify({ username: user.username, password: user.password }),
    { headers: jsonHeaders }
  );

  loginDuration.add(new Date() - startTime);

  const success = check(res, {
    'login successful': (r) => r.status === 200,
    'has token': (r) => {
      try {
        const body = JSON.parse(r.body);
        return body.data && body.data.token;
      } catch (e) {
        return false;
      }
    },
  });

  if (!success) {
    errorRate.add(1);
    console.log(`Login failed for ${user.username}: ${res.status}`);
    return null;
  }

  errorRate.add(0);
  const body = JSON.parse(res.body);
  return { token: body.data.token, userId: body.data.userId };
}

// 完整開戶：register 新用戶並加入動態用戶池
export function registerNewUser() {
  const uniqueId = `${__VU}_${__ITER}_${Date.now() % 1000000}`;
  const newUser = {
    username: `k6_${uniqueId}`,
    email: `k6_${uniqueId}@loadtest.local`,
    password: 'K6Test123!',
  };

  const startTime = new Date();
  const res = http.post(
    `${BASE_URL}/api/v1/auth/register`,
    JSON.stringify(newUser),
    { headers: jsonHeaders }
  );

  registerDuration.add(new Date() - startTime);

  const success = check(res, {
    'register successful': (r) => r.status === 201,
  });
  errorRate.add(success ? 0 : 1);

  if (!success) {
    console.log(`Register failed: ${res.status} ${res.body}`);
    return;
  }

  // 加入動態用戶池（下次被選到時 login 後動態查詢帳戶）
  if (dynamicUsers.length < 50) {
    dynamicUsers.push({ username: newUser.username, password: newUser.password });
    console.log(`Registered: ${newUser.username}`);
  }
}
