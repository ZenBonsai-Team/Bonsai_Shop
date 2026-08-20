/**
 * ============================================================
 *  BSMS – k6 Load Test Script
 *  Bonsai Shop Management System (bonsaihaihau.com)
 * ============================================================
 */

import http from 'k6/http';
import { sleep, check, group } from 'k6';
import { Trend, Rate, Counter } from 'k6/metrics';

// ═══════════════════════════════════════════════════════════
// CONFIG
// ═══════════════════════════════════════════════════════════

const BASE_URL = __ENV.BASE_URL || 'https://bonsaihaihau.com';
const SCENARIO = __ENV.SCENARIO || 'load'; // 'load' | 'stress'

// ProductID lấy từ DB thật trên Raspberry Pi 5 (MariaDB bonsai_shop)
// Query: SELECT ProductID FROM product WHERE IsVisible = 1 AND ProductStatus = 'AVAILABLE' LIMIT 10
const PRODUCT_IDS = [8, 15, 16, 18, 25, 26, 42, 46, 49, 50];
const SEARCH_KEYWORDS = ['bonsai', 'sanh', 'thông', 'cây', 'phôi', 'tùng', 'nam điền'];
// Segment names khớp đúng với DB: INSERT INTO product_segment VALUES (1,'Budget'),(2,'Mid'),(3,'Elite')
const SEGMENTS = ['Budget', 'Mid', 'Elite'];

// ═══════════════════════════════════════════════════════════
// CUSTOM METRICS
// ═══════════════════════════════════════════════════════════

const pageDuration   = new Trend('page_duration_ms',   true);
const searchDuration = new Trend('search_duration_ms', true);
const errorRate      = new Rate('http_error_rate');
const totalRequests  = new Counter('total_requests');

// ═══════════════════════════════════════════════════════════
// SCENARIO DEFINITION
// ═══════════════════════════════════════════════════════════

const allScenarios = {
  load: {
    executor: 'constant-vus',
    vus: 100,
    duration: '2m',
    exec: 'default',
    tags: { nfr: 'NFR-P03' },
  },
  stress: {
    executor: 'ramping-vus',
    startVUs: 0,
    stages: [
      { duration: '30s', target: 100 },
      { duration: '1m',  target: 300 },
      { duration: '1m',  target: 500 },
      { duration: '30s', target: 0 },
    ],
    exec: 'default',
    tags: { nfr: 'NFR-S01' },
  },
};

export const options = {
  scenarios: {
    [SCENARIO]: allScenarios[SCENARIO] || allScenarios.load,
  },

  thresholds: {
    'search_duration_ms': ['p(95)<1500'],
    'page_duration_ms':   ['p(95)<2000'],
    'http_error_rate':    ['rate<0.01'],
    'http_req_duration':  ['p(95)<3000'],
    'http_req_failed':    ['rate<0.05'],
  },
};

// ═══════════════════════════════════════════════════════════
// DEFAULT ENTRYPOINT – User Flow (Đã loại bỏ browseContact)
// ═══════════════════════════════════════════════════════════

export default function () {
  const r = Math.random();

  if      (r < 0.25) visitHomepage();        // 25% Xem trang chủ
  else if (r < 0.50) browseProductList();    // 25% Xem Marketplace
  else if (r < 0.70) searchAndFilter();      // 20% Tìm kiếm / Lọc sản phẩm
  else if (r < 0.85) viewProductDetail();    // 15% Xem chi tiết cây
  else if (r < 0.95) browseCommunity();      // 10% Xem bảng tin cộng đồng
  else               browseBonsaiLuxury();   // 5%  Xem mục Bonsai Luxury

  // Giãn cách thao tác đọc trang của người dùng thực tế: 2 - 4 giây
  sleep(Math.random() * 2 + 2);
}

// ═══════════════════════════════════════════════════════════
// ACTIONS
// ═══════════════════════════════════════════════════════════

function visitHomepage() {
  group('01 – Homepage (/)', () => {
    const res = http.get(`${BASE_URL}/`, { tags: { page: 'homepage' } });
    const ms  = res.timings.duration;

    pageDuration.add(ms);
    totalRequests.add(1);

    const ok = check(res, {
      'homepage – status 200': (r) => r.status === 200,
    });
    errorRate.add(!ok);
  });
}

function browseProductList() {
  group('02 – Marketplace (/marketplace)', () => {
    const res = http.get(`${BASE_URL}/marketplace`, { tags: { page: 'marketplace' } });
    const ms  = res.timings.duration;

    pageDuration.add(ms);
    totalRequests.add(1);

    const ok = check(res, {
      'marketplace – status 200': (r) => r.status === 200,
    });
    errorRate.add(!ok);
  });
}

function searchAndFilter() {
  // 1. Tìm kiếm theo từ khóa
  group('03 – Search (/marketplace?keyword=...)', () => {
    const kw  = randomItem(SEARCH_KEYWORDS);
    const url = `${BASE_URL}/marketplace?keyword=${encodeURIComponent(kw)}`;
    const res = http.get(url, { tags: { page: 'search' } });
    const ms  = res.timings.duration;

    searchDuration.add(ms);
    pageDuration.add(ms);
    totalRequests.add(1);

    const ok = check(res, {
      'search – status 200': (r) => r.status === 200,
    });

    if (!ok && __ITER < 2) {
      console.warn(`[SEARCH FAIL] URL: ${url} | Status: ${res.status}`);
    }

    errorRate.add(!ok);
  });

  sleep(0.5);

  // 2. Lọc theo phân khúc (Budget / Mid / Elite) và sắp xếp
  group('03b – Filter (/marketplace?sort=price_asc&segment=...)', () => {
    const seg  = randomItem(SEGMENTS);  // 'Budget' | 'Mid' | 'Elite'
    const sort = randomItem(['price_asc', 'price_desc', 'age_desc']);
    const url  = `${BASE_URL}/marketplace?sort=${sort}&segment=${encodeURIComponent(seg)}`;
    const res  = http.get(url, { tags: { page: 'filter' } });
    const ms   = res.timings.duration;

    searchDuration.add(ms);
    totalRequests.add(1);

    const ok = check(res, {
      'filter – status 200': (r) => r.status === 200,
    });

    if (!ok && __ITER < 2) {
      console.warn(`[FILTER FAIL] URL: ${url} | Status: ${res.status}`);
    }

    errorRate.add(!ok);
  });
}

function viewProductDetail() {
  group('04 – Product Detail (/bonsai-luxury-detail/:id)', () => {
    const id  = randomItem(PRODUCT_IDS);
    const res = http.get(`${BASE_URL}/bonsai-luxury-detail/${id}`, { tags: { page: 'product_detail' } });
    const ms  = res.timings.duration;

    pageDuration.add(ms);
    totalRequests.add(1);

    const ok = check(res, {
      'detail – status 200 or 404': (r) => r.status === 200 || r.status === 404,
      'detail – no 500 error':      (r) => r.status !== 500,
    });
    errorRate.add(!ok);
  });
}

function browseCommunity() {
  group('05 – Community Feed (/community)', () => {
    const res = http.get(`${BASE_URL}/community`, { tags: { page: 'community' } });
    const ms  = res.timings.duration;

    pageDuration.add(ms);
    totalRequests.add(1);

    const ok = check(res, {
      'community – status 200': (r) => r.status === 200,
    });
    errorRate.add(!ok);
  });
}

function browseBonsaiLuxury() {
  group('07 – Bonsai Luxury (/bonsai-luxury)', () => {
    const res = http.get(`${BASE_URL}/bonsai-luxury`, { tags: { page: 'luxury' } });
    const ms  = res.timings.duration;

    pageDuration.add(ms);
    totalRequests.add(1);

    const ok = check(res, {
      'luxury – status 200': (r) => r.status === 200,
    });
    errorRate.add(!ok);
  });
}

// ═══════════════════════════════════════════════════════════
// HELPERS
// ═══════════════════════════════════════════════════════════

function randomItem(arr) {
  return arr[Math.floor(Math.random() * arr.length)];
}