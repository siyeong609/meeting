/**
 * 공통 유틸 (modal / escape / fetch / pagination)
 *
 * 목적:
 * - 모든 페이지에서 "JSON 데이터 렌더링"을 쓰더라도
 *   공통 함수(fetchJson, renderPagination)로 일관되게 구현
 *
 * 주의:
 * - renderPagination은 "UI 생성"만 담당
 * - 실제 데이터 로딩은 페이지별 load 함수가 담당 (onPageChange 콜백)
 */

/**
 * 모달 알림 (성공/오류/경고/정보)
 * @param {string} message - 표시 메시지
 * @param {string} type - success|error|warning|info
 */
function showModal(message, type) {
  type = type || 'success'; // success, error, warning, info

  const overlay = document.createElement('div');
  overlay.className = 'modal-overlay';

  const iconMap = {
    success: '✓',
    error: '✕',
    warning: '!',
    info: 'i'
  };

  const titleMap = {
    success: '성공',
    error: '오류',
    warning: '경고',
    info: '알림'
  };

  // message는 escapeHtml로 XSS 방지
  overlay.innerHTML = `
    <div class="modal">
      <div class="modal-header ${type}">
        <div class="modal-icon ${type}">${iconMap[type] || iconMap.info}</div>
        <div class="modal-title">${titleMap[type] || titleMap.info}</div>
        <button class="modal-close" type="button" onclick="this.closest('.modal-overlay').remove()">&times;</button>
      </div>
      <div class="modal-body">${escapeHtml(message)}</div>
      <div class="modal-footer">
        <button class="btn btn-primary" type="button" onclick="this.closest('.modal-overlay').remove()">확인</button>
      </div>
    </div>
  `;

  document.body.appendChild(overlay);

  setTimeout(function () {
    overlay.classList.add('show');
  }, 10);

  overlay.addEventListener('click', function (e) {
    if (e.target === overlay) {
      overlay.remove();
    }
  });
}

/**
 * HTML escape (XSS 방지)
 * @param {string} text
 * @returns {string}
 */
function escapeHtml(text) {
  // ✅ string 아니어도 안전하게 처리 (Error 객체 등)
  if (text === null || text === undefined) return '';
  const div = document.createElement('div');
  div.textContent = String(text);
  return div.innerHTML;
}

/**
 * (내부) 응답 body를 안전하게 JSON 파싱
 * - 서버가 JSON을 주긴 하는데, 예외적으로 빈 body / text 응답이 올 수 있으니 방어
 * - JSON 파싱 실패 시 { ok:false, message:<raw text> } 형태로라도 반환
 */
async function safeParseJson(res) {
  const text = await res.text();
  if (!text) return null;

  try {
    return JSON.parse(text);
  } catch (e) {
    return { ok: false, message: text };
  }
}

/**
 * 공통 JSON fetch 유틸
 *
 * 특징:
 * - ✅ res.ok=false(HTTP 400/409 등)여도 body(JSON)를 파싱해서 message를 에러로 던짐
 * - JSON 파싱
 * - 서버가 { ok:false, message:"..." } 형태를 줄 경우 자동 에러 처리 가능
 *
 * @param {string} url
 * @param {object} options fetch 옵션
 * @param {boolean} options.throwOnOkFalse - 응답에 ok:false면 throw (기본 true)
 * @returns {Promise<any>}
 */
async function fetchJson(url, options) {
  options = options || {};
  const throwOnOkFalse = (options.throwOnOkFalse !== false);

  const fetchOptions = {
    method: options.method || 'GET',
    headers: Object.assign(
      {
        'Accept': 'application/json'
      },
      options.headers || {}
    ),
    body: options.body
  };

  // ✅ FormData면 브라우저가 boundary 포함 Content-Type을 자동 세팅하므로 건드리지 않음
  const isFormData = (typeof FormData !== 'undefined') && (fetchOptions.body instanceof FormData);
  if (!isFormData && fetchOptions.body && !fetchOptions.headers['Content-Type']) {
    // URLSearchParams / string / JSON 등 일반 body일 때 기본 Content-Type 세팅
    fetchOptions.headers['Content-Type'] = 'application/x-www-form-urlencoded; charset=UTF-8';
  }

  const res = await fetch(url, fetchOptions);

  // ✅ 핵심: HTTP 에러여도 body를 먼저 읽어서 message를 살린다
  const json = await safeParseJson(res);

  if (!res.ok) {
    // 서버 공통 응답 포맷이면 message 우선 사용
    const msg =
      (json && typeof json.message === 'string' && json.message.trim() !== '')
        ? json.message
        : ('HTTP ' + res.status);

    const err = new Error(msg);
    // 디버깅용 정보 (필요 시 콘솔에서 확인)
    err.status = res.status;
    err.body = json;
    throw err;
  }

  // 정상(2xx)인데 body가 비어있으면 그대로 null 반환
  if (json === null) return null;

  // 서버가 공통 응답 { ok, message }를 준다면 여기서 처리
  if (throwOnOkFalse && json && json.ok === false) {
    throw new Error(json.message || '요청 실패');
  }

  return json;
}

/**
 * 쿼리스트링 생성 유틸
 * - 값이 null/undefined/빈문자열이면 제외(기본)
 *
 * @param {object} params
 * @param {boolean} skipEmpty
 * @returns {string} 예) "?page=1&size=10"
 */
function buildQueryString(params, skipEmpty) {
  skipEmpty = (skipEmpty !== false); // 기본 true
  const usp = new URLSearchParams();

  Object.keys(params || {}).forEach(function (k) {
    const v = params[k];

    if (skipEmpty) {
      if (v === null || v === undefined) return;
      if (typeof v === 'string' && v.trim() === '') return;
    }

    usp.append(k, String(v));
  });

  const qs = usp.toString();
  return qs ? ('?' + qs) : '';
}

/**
 * 공통 페이징 렌더 함수
 *
 * pageInfo 규격:
 * {
 *   page: 1,
 *   size: 10,
 *   totalElements: 123,
 *   totalPages: 13
 * }
 *
 * @param {HTMLElement|string} container - element 또는 selector
 * @param {object} pageInfo - page meta
 * @param {function} onPageChange - (newPage) => void
 * @param {object} opts - 옵션
 * @param {number} opts.maxButtons - 페이지 번호 버튼 최대 표시(기본 7)
 */
function renderPagination(container, pageInfo, onPageChange, opts) {
  opts = opts || {};
  const maxButtons = opts.maxButtons || 7;

  // container 처리
  let el = container;
  if (typeof container === 'string') {
    el = document.querySelector(container);
  }
  if (!el) return;

  // totalPages가 0/1이면 페이징 숨김
  if (!pageInfo || !pageInfo.totalPages || pageInfo.totalPages <= 1) {
    el.innerHTML = '';
    return;
  }

  const current = Number(pageInfo.page || 1);
  const totalPages = Number(pageInfo.totalPages || 1);

  // 안전 처리
  const safeCurrent = Math.min(Math.max(current, 1), totalPages);

  el.innerHTML = '';

  const wrap = document.createElement('div');
  wrap.className = 'pagination';

  // 버튼 생성 헬퍼
  function makeBtn(label, page, disabled, active) {
    const b = document.createElement('button');
    b.type = 'button';
    b.className = 'pagination-btn' + (active ? ' active' : '');
    b.textContent = label;

    if (disabled) {
      b.disabled = true;
      b.classList.add('disabled');
      return b;
    }

    b.addEventListener('click', function () {
      if (page === safeCurrent) return;
      onPageChange(page);
    });

    return b;
  }

  // first / prev
  wrap.appendChild(makeBtn('«', 1, safeCurrent === 1, false));
  wrap.appendChild(makeBtn('‹', safeCurrent - 1, safeCurrent === 1, false));

  // 번호 범위 계산
  const half = Math.floor(maxButtons / 2);
  let start = safeCurrent - half;
  let end = safeCurrent + half;

  if (totalPages <= maxButtons) {
    start = 1;
    end = totalPages;
  } else {
    if (start < 1) {
      start = 1;
      end = start + maxButtons - 1;
    }
    if (end > totalPages) {
      end = totalPages;
      start = end - maxButtons + 1;
    }
  }

  // 앞 생략
  if (start > 1) {
    wrap.appendChild(makeBtn('1', 1, false, safeCurrent === 1));
    if (start > 2) {
      const dots = document.createElement('span');
      dots.className = 'pagination-dots';
      dots.textContent = '...';
      wrap.appendChild(dots);
    }
  }

  // 번호 버튼
  for (let p = start; p <= end; p++) {
    wrap.appendChild(makeBtn(String(p), p, false, p === safeCurrent));
  }

  // 뒤 생략
  if (end < totalPages) {
    if (end < totalPages - 1) {
      const dots = document.createElement('span');
      dots.className = 'pagination-dots';
      dots.textContent = '...';
      wrap.appendChild(dots);
    }
    wrap.appendChild(makeBtn(String(totalPages), totalPages, false, safeCurrent === totalPages));
  }

  // next / last
  wrap.appendChild(makeBtn('›', safeCurrent + 1, safeCurrent === totalPages, false));
  wrap.appendChild(makeBtn('»', totalPages, safeCurrent === totalPages, false));

  el.appendChild(wrap);
}

/**
 * (선택) 페이징 기본 CSS를 런타임에 주입
 * - 기존 CSS를 건드리지 않고도 페이징이 "안 보이는 문제"를 막기 위한 안전장치
 */
function injectDefaultPaginationStyles() {
  if (document.getElementById('default-pagination-style')) return;

  const style = document.createElement('style');
  style.id = 'default-pagination-style';
  style.textContent = `
    .pagination { display:flex; gap:6px; align-items:center; justify-content:center; margin-top:16px; flex-wrap:wrap; }
    .pagination-btn { padding:6px 10px; border:1px solid #ddd; background:#fff; cursor:pointer; border-radius:6px; }
    .pagination-btn.active { font-weight:700; border-color:#333; }
    .pagination-btn.disabled { opacity:0.5; cursor:not-allowed; }
    .pagination-dots { padding:0 4px; color:#777; }
  `;
  document.head.appendChild(style);
}
