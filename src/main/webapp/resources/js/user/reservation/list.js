/**
 * user/reservation/list.js
 * - 내 예약 목록: AJAX + 페이징 + 취소
 *
 * 전제:
 * - common.js: fetchJson / showModal / escapeHtml / renderPagination
 * - window.__MEETING__.ctx 존재
 */
(function () {
  "use strict";

  const ctx = (window.__MEETING__ && window.__MEETING__.ctx) ? window.__MEETING__.ctx : "";

  const API_LIST = ctx + "/user/reservations/list";
  const API_CANCEL = ctx + "/user/reservations/cancel";

  const state = { page: 1, size: 10, q: "" };

  const tbody = document.getElementById("tableBody");
  const totalEl = document.getElementById("totalElements");
  const inputQ = document.getElementById("searchQ");
  const btnSearch = document.getElementById("btnSearch");
  const selectSize = document.getElementById("pageSize");

  document.addEventListener("DOMContentLoaded", () => {
    if (typeof fetchJson !== "function") return alert("fetchJson 없음(common.js 확인)");
    if (typeof showModal !== "function") return alert("showModal 없음(common.js 확인)");
    if (typeof escapeHtml !== "function") return alert("escapeHtml 없음(common.js 확인)");

    load();

    btnSearch.addEventListener("click", () => {
      state.q = (inputQ.value || "").trim();
      state.page = 1;
      load();
    });

    inputQ.addEventListener("keydown", (e) => {
      if (e.key === "Enter") {
        e.preventDefault();
        state.q = (inputQ.value || "").trim();
        state.page = 1;
        load();
      }
    });

    selectSize.addEventListener("change", () => {
      state.size = parseInt(selectSize.value, 10) || 10;
      state.page = 1;
      load();
    });
  });

  async function load() {
    tbody.innerHTML = "<tr><td colspan='6' class='text-center text-muted'>데이터를 불러오는 중...</td></tr>";

    try {
      const body = new URLSearchParams({
        page: String(state.page),
        size: String(state.size),
        q: state.q
      });

      const json = await fetchJson(API_LIST, {
        method: "POST",
        headers: { "Content-Type": "application/x-www-form-urlencoded" },
        credentials: "same-origin",
        body: body.toString(),
        throwOnOkFalse: true
      });

      const items = (json && json.data && Array.isArray(json.data.items)) ? json.data.items : [];
      const total = (json && json.page && json.page.total != null) ? json.page.total : items.length;
      totalEl.textContent = String(total);

      render(items);

      if (typeof renderPagination === "function") {
        const p = normalizePage(json && json.page ? json.page : null);
        renderPagination("#pagination", p, (newPage) => {
          state.page = newPage;
          load();
        });
      }

    } catch (e) {
      totalEl.textContent = "0";
      tbody.innerHTML = "<tr><td colspan='6' class='text-center text-muted'>조회 실패</td></tr>";
      showModal("예약 목록 조회 실패: " + (e.message || e), "error");
    }
  }

  function normalizePage(p) {
    const page = p && p.page != null ? parseInt(p.page, 10) : 1;
    const size = p && p.size != null ? parseInt(p.size, 10) : state.size;
    const totalPages = p && p.totalPages != null ? parseInt(p.totalPages, 10) : 1;
    const totalElements = p && p.total != null ? parseInt(p.total, 10) : 0;
    return { page, size, totalPages, totalElements };
  }

  function render(rows) {
    if (!rows || rows.length === 0) {
      tbody.innerHTML = "<tr><td colspan='6' class='text-center text-muted'>예약이 없습니다.</td></tr>";
      return;
    }

    tbody.innerHTML = rows.map((r) => {
      const id = escapeHtml(String(r.id ?? 0));
      const status = escapeHtml(String(r.status || "-"));
      const room = escapeHtml((r.roomName || "-") + (r.roomLocation ? (" / " + r.roomLocation) : ""));
      const when = escapeHtml((r.startTime || "-") + " ~ " + (r.endTime || "-"));
      const title = escapeHtml(r.title || "-");
      const createdAt = escapeHtml(r.createdAt || "-");

      const canCancel = (String(r.status) === "BOOKED");

      return ""
        + "<tr>"
        +   "<td style='font-weight:900;'>" + status + "</td>"
        +   "<td>" + room + "</td>"
        +   "<td>" + when + "</td>"
        +   "<td>" + title + "</td>"
        +   "<td>" + createdAt + "</td>"
        +   "<td>"
        +     (canCancel
              ? ("<button type='button' class='btn btn-secondary btn-cancel' data-id='" + id + "'>취소</button>")
              : ("<span class='text-muted'>-</span>"))
        +   "</td>"
        + "</tr>";
    }).join("");

    tbody.querySelectorAll(".btn-cancel").forEach((btn) => {
      btn.addEventListener("click", async () => {
        const id = btn.getAttribute("data-id");
        await cancel(id);
      });
    });
  }

  async function cancel(id) {
    if (!confirm("해당 예약을 취소하시겠습니까?")) return;

    try {
      const body = new URLSearchParams({ id: String(id) });

      await fetchJson(API_CANCEL, {
        method: "POST",
        headers: { "Content-Type": "application/x-www-form-urlencoded" },
        credentials: "same-origin",
        body: body.toString(),
        throwOnOkFalse: true
      });

      showModal("예약이 취소되었습니다.", "success");
      load();

    } catch (e) {
      showModal("취소 실패: " + (e.message || e), "error");
    }
  }
})();
