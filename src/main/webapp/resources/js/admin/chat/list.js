/**
 * 관리자 대화관리(채팅문의) - 목록 페이지 스크립트
 * - 최근 메시지 내용(lastMessagePreview) + 최근시간(lastMessageAt)까지 표시
 * - 최근 메시지는 1줄 + overflow 시 CSS로 ... 처리
 */

(() => {
  "use strict";

  const ctx = (window.__MEETING__ && window.__MEETING__.ctx) ? window.__MEETING__.ctx : "";

  const escapeHtml = window.escapeHtml || ((s) => {
    return String(s ?? "")
      .replaceAll("&", "&amp;")
      .replaceAll("<", "&lt;")
      .replaceAll(">", "&gt;")
      .replaceAll('"', "&quot;")
      .replaceAll("'", "&#39;");
  });

  const showModal = window.showModal || ((title, message) => {
    alert((title ? title + "\n\n" : "") + (message ?? ""));
  });

  const fetchJson = window.fetchJson || (async (url, options) => {
    const res = await fetch(url, options);
    const text = await res.text();
    let json = null;

    try {
      json = text ? JSON.parse(text) : null;
    } catch (e) {
      throw new Error("서버 응답이 JSON 형식이 아닙니다.");
    }

    if (!res.ok) {
      const msg = (json && (json.message || json.msg)) ? (json.message || json.msg) : ("HTTP " + res.status);
      throw new Error(msg);
    }
    return json;
  });

  const elSearchUser = document.getElementById("searchUser");
  const elStatusFilter = document.getElementById("statusFilter");
  const elPageSize = document.getElementById("pageSize");
  const elBtnSearch = document.getElementById("btnSearchChat");

  const elTbody = document.getElementById("chatTableBody");
  const elTotal = document.getElementById("totalElements");
  const elPagination = document.getElementById("pagination");

  let state = {
    page: 1,
    size: Number(elPageSize?.value || 10),
    q: "",
    status: "",
    totalPages: 1,
    totalElements: 0,
  };

  function bindEvents() {
    elBtnSearch?.addEventListener("click", () => {
      state.page = 1;
      state.q = (elSearchUser?.value || "").trim();
      state.status = (elStatusFilter?.value || "").trim();
      state.size = Number(elPageSize?.value || 10);
      loadThreads();
    });

    elSearchUser?.addEventListener("keydown", (e) => {
      if (e.key === "Enter") elBtnSearch?.click();
    });

    elStatusFilter?.addEventListener("change", () => {
      state.page = 1;
      state.status = (elStatusFilter?.value || "").trim();
      loadThreads();
    });

    elPageSize?.addEventListener("change", () => {
      state.page = 1;
      state.size = Number(elPageSize?.value || 10);
      loadThreads();
    });
  }

  async function apiListThreads() {
    const params = new URLSearchParams();
    params.set("page", String(state.page));
    params.set("size", String(state.size));
    if (state.q) params.set("q", state.q);
    if (state.status) params.set("status", state.status);

    return await fetchJson(ctx + "/admin/chat/threads", {
      method: "POST",
      headers: {
        "Content-Type": "application/x-www-form-urlencoded; charset=UTF-8",
      },
      body: params.toString(),
    });
  }

  async function loadThreads() {
    renderLoading();

    try {
      const res = await apiListThreads();

      if (!res || res.ok !== true) {
        showModal("오류", (res && res.message) ? res.message : "목록을 불러오지 못했습니다.");
        renderEmpty("데이터를 불러오지 못했습니다.");
        return;
      }

      const data = res.data || {};
      const page = res.page || {};
      const items = Array.isArray(data.items) ? data.items : [];

      state.totalElements = Number(page.totalElements ?? items.length ?? 0);
      state.totalPages = Number(page.totalPages ?? 1);
      state.page = Number(page.page ?? state.page);
      state.size = Number(page.size ?? state.size);

      elTotal.textContent = String(state.totalElements);

      renderRows(items);
      renderPagination();

    } catch (e) {
      showModal("오류", e.message || "요청 처리 중 오류가 발생했습니다.");
      renderEmpty("데이터를 불러오는 중 오류가 발생했습니다.");
    }
  }

  function renderLoading() {
    elTbody.innerHTML = `
      <tr>
        <td colspan="6" class="text-center text-muted">데이터를 불러오는 중...</td>
      </tr>
    `;
  }

  function renderEmpty(message) {
    elTbody.innerHTML = `
      <tr>
        <td colspan="6" class="text-center text-muted">${escapeHtml(message)}</td>
      </tr>
    `;
  }

  /**
   * 서버에서 lastMessagePreview를 주더라도
   * 혹시 대비해서 프론트에서도 1줄 정규화
   */
  function normalizeOneLine(s) {
    const t = String(s ?? "").trim();
    if (!t) return "-";
    return t.replace(/\s+/g, " ");
  }

  function renderRows(items) {
    if (!items || items.length === 0) {
      renderEmpty("조회 결과가 없습니다.");
      return;
    }

    elTbody.innerHTML = items.map((it) => {
      const threadId = it.threadId ?? it.id ?? "";
      const userLoginId = it.userLoginId ?? it.user_login_id ?? "-";
      const status = it.status ?? "OPEN";

      // ✅ 최근 메시지 내용 + 최근시간
      const lastPreview = normalizeOneLine(it.lastMessagePreview ?? it.last_message_preview ?? "-");
      const lastAt = it.lastMessageAt ?? it.last_message_at ?? "-";

      const createdAt = it.createdAt ?? it.created_at ?? "-";
      const badgeClass = (status === "CLOSED") ? "badge-closed" : "badge-open";

      return `
        <tr data-thread-id="${escapeHtml(threadId)}">
          <td>${escapeHtml(userLoginId)}</td>
          <td><span class="badge ${badgeClass}">${escapeHtml(status)}</span></td>

          <!-- ✅ 한 줄 + ... 처리: CSS 클래스 적용 -->
          <td class="td-ellipsis" title="${escapeHtml(lastPreview)}">${escapeHtml(lastPreview)}</td>

          <td>${escapeHtml(lastAt)}</td>
          <td>${escapeHtml(createdAt)}</td>
          <td>
            <button type="button"
                    class="btn btn-primary btn-sm btnOpenChat"
                    data-thread-id="${escapeHtml(threadId)}">대화열기</button>
          </td>
        </tr>
      `;
    }).join("");

    elTbody.querySelectorAll(".btnOpenChat").forEach((btn) => {
      btn.addEventListener("click", (e) => {
        const threadId = e.currentTarget.getAttribute("data-thread-id");
        openChatWindow(threadId);
      });
    });
  }

  function openChatWindow(threadId) {
    if (!threadId) {
      showModal("오류", "threadId가 없습니다.");
      return;
    }

    const url = ctx + "/admin/chat/thread?threadId=" + encodeURIComponent(threadId) + "&popup=1";
    const features = "width=420,height=720,menubar=no,toolbar=no,location=no,status=no,scrollbars=yes,resizable=yes";

    // ✅ 팝업이 차단되면 win이 null
    const win = window.open(url, "chat_thread_" + threadId, features);

    // ✅ 차단 시 폴백: 같은 탭 이동
    if (!win) {
      // 사용자에게 안내 + 탭 이동
      showModal("안내", "팝업이 차단되어 같은 탭에서 대화창을 엽니다.\n(브라우저 팝업 허용 시 새 창으로 열림)");
      window.location.href = url;
    }
  }


  function renderPagination() {
    const totalPages = Math.max(1, Number(state.totalPages || 1));
    const current = Math.min(Math.max(1, Number(state.page || 1)), totalPages);

    if (totalPages <= 1) {
      elPagination.innerHTML = "";
      return;
    }

    const windowSize = 10;
    const start = Math.floor((current - 1) / windowSize) * windowSize + 1;
    const end = Math.min(start + windowSize - 1, totalPages);

    const btn = (label, page, disabled = false, active = false) => {
      const cls = ["pg-btn"];
      if (disabled) cls.push("disabled");
      if (active) cls.push("active");
      return `<button type="button" class="${cls.join(" ")}" data-page="${page}" ${disabled ? "disabled" : ""}>${label}</button>`;
    };

    let html = `<div class="pg">`;
    html += btn("이전", current - 1, current <= 1);
    for (let p = start; p <= end; p++) html += btn(String(p), p, false, p === current);
    html += btn("다음", current + 1, current >= totalPages);
    html += `</div>`;

    elPagination.innerHTML = html;

    elPagination.querySelectorAll("button.pg-btn").forEach((b) => {
      b.addEventListener("click", () => {
        if (b.disabled) return;
        const p = Number(b.getAttribute("data-page"));
        if (!p || p === state.page) return;
        state.page = p;
        loadThreads();
      });
    });
  }

  function init() {
    bindEvents();
    loadThreads();
  }

  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", init);
  } else {
    init();
  }
})();
