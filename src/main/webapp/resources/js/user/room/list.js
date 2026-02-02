/**
 * user/room/list.js
 * - 사용자 회의실 목록/검색/페이징
 * - ✅ 목록 하단에 "예약 현황" 카드(탭: 일자/달력) 렌더링
 *
 * 전제:
 * - JSP에서 window.__MEETING__.ctx 제공
 * - common.js에 fetchJson / showModal / escapeHtml / renderPagination 존재
 *
 * 서버 규약:
 * - POST /user/rooms  (목록 JSON)
 * - POST /user/rooms/status/day   (일자 현황)
 * - POST /user/rooms/status/month (월 현황)
 */
(function () {
  "use strict";

  const ctx = (window.__MEETING__ && window.__MEETING__.ctx) ? window.__MEETING__.ctx : "";

  // 목록
  const API_LIST = ctx + "/user/rooms";
  const PAGE_DETAIL = ctx + "/user/rooms/detail";

  // 예약 현황
  const API_STATUS_DAY = ctx + "/user/rooms/status/day";
  const API_STATUS_MONTH = ctx + "/user/rooms/status/month";

  // state
  const state = {
    page: 1,
    size: 10,
    q: "",
    selectedRoomId: 0,
    selectedRoomName: ""
  };

  // list DOM
  const tbody = document.getElementById("roomTableBody");
  const totalEl = document.getElementById("totalElements");
  const inputQ = document.getElementById("searchRoom");
  const btnSearch = document.getElementById("btnSearchRoom");
  const selectSize = document.getElementById("pageSize");

  // status DOM
  const statusRoomLabel = document.getElementById("statusRoomLabel");
  const tabDay = document.getElementById("tabDay");
  const tabMonth = document.getElementById("tabMonth");
  const dayControl = document.getElementById("dayControl");
  const monthControl = document.getElementById("monthControl");
  const statusDate = document.getElementById("statusDate");
  const statusMonth = document.getElementById("statusMonth");
  const btnLoadDay = document.getElementById("btnLoadDay");
  const btnLoadMonth = document.getElementById("btnLoadMonth");
  const dayView = document.getElementById("dayView");
  const monthView = document.getElementById("monthView");
  const timetableWrap = document.getElementById("timetableWrap");
  const calendarWrap = document.getElementById("calendarWrap");

  document.addEventListener("DOMContentLoaded", () => {
    // 의존 유틸 체크
    if (typeof fetchJson !== "function") {
      alert("fetchJson이 없습니다. resources/js/common.js를 확인하세요.");
      return;
    }
    if (typeof escapeHtml !== "function") {
      alert("escapeHtml이 없습니다. resources/js/common.js를 확인하세요.");
      return;
    }
    if (typeof showModal !== "function") {
      alert("showModal이 없습니다. resources/js/common.js를 확인하세요.");
      return;
    }

    // 기본 날짜 세팅
    if (statusDate) statusDate.value = todayISO();
    if (statusMonth) statusMonth.value = todayISO().slice(0, 7);

    loadRooms();

    // 검색 버튼
    if (btnSearch) {
      btnSearch.addEventListener("click", () => {
        state.q = (inputQ ? inputQ.value : "").trim();
        state.page = 1;
        loadRooms();
      });
    }

    // 엔터 검색
    if (inputQ) {
      inputQ.addEventListener("keydown", (e) => {
        if (e.key === "Enter") {
          e.preventDefault();
          state.q = (inputQ.value || "").trim();
          state.page = 1;
          loadRooms();
        }
      });
    }

    // 페이지 사이즈 변경
    if (selectSize) {
      selectSize.addEventListener("change", () => {
        state.size = parseInt(selectSize.value, 10) || 10;
        state.page = 1;
        loadRooms();
      });
    }

    // 탭
    if (tabDay) tabDay.addEventListener("click", () => switchTab("day"));
    if (tabMonth) tabMonth.addEventListener("click", () => switchTab("month"));

    // 조회 버튼
    if (btnLoadDay) btnLoadDay.addEventListener("click", () => {
      if (!state.selectedRoomId) return showModal("먼저 회의실을 선택하세요.", "warning");
      loadDayStatus(state.selectedRoomId);
    });

    if (btnLoadMonth) btnLoadMonth.addEventListener("click", () => {
      if (!state.selectedRoomId) return showModal("먼저 회의실을 선택하세요.", "warning");
      loadMonthStatus(state.selectedRoomId);
    });
  });

  // =========================================================
  // 목록 로드
  // =========================================================
  async function loadRooms() {
    if (!tbody) return;

    tbody.innerHTML =
      "<tr><td colspan='5' class='text-muted' style='text-align:center;'>데이터를 불러오는 중...</td></tr>";

    try {
      const body = new URLSearchParams({
        page: String(state.page),
        size: String(state.size),
        q: state.q
      });

      const json = await fetchJson(API_LIST, {
        method: "POST",
        headers: { "Content-Type": "application/x-www-form-urlencoded; charset=UTF-8" },
        credentials: "same-origin",
        body: body.toString(),
        throwOnOkFalse: true
      });

      const items = (json && json.data && Array.isArray(json.data.items)) ? json.data.items : [];
      const total = extractTotal(json, items.length);

      if (totalEl) totalEl.textContent = String(total);

      renderRooms(items);

      if (typeof renderPagination === "function") {
        const pageForUi = normalizePageForRenderPagination(json && json.page ? json.page : null);
        renderPagination("#pagination", pageForUi, (newPage) => {
          state.page = newPage;
          loadRooms();
        });
      }
    } catch (e) {
      if (totalEl) totalEl.textContent = "0";
      tbody.innerHTML =
        "<tr><td colspan='5' class='text-muted' style='text-align:center;'>데이터 로드 실패</td></tr>";

      showModal("회의실 목록을 불러오는 중 오류가 발생했습니다. (" + (e.message || e) + ")", "error");
    }
  }

  // =========================================================
  // 목록 렌더링 + 선택 이벤트
  // =========================================================
  function renderRooms(rows) {
    if (!tbody) return;

    if (!rows || rows.length === 0) {
      tbody.innerHTML =
        "<tr><td colspan='5' class='text-muted' style='text-align:center;'>회의실이 없습니다.</td></tr>";
      return;
    }

    tbody.innerHTML = rows.map((r) => {
      const id = r.id ?? 0;
      const name = escapeHtml(r.name || "-");
      const location = escapeHtml(r.location || "-");
      const capacity = escapeHtml(String(r.capacity ?? "-"));

      const active = (r.active === true) || (r.isActive === true) || (r.is_active === 1);
      const statusText = active ? "예약가능" : "중지";

      const detailUrl = PAGE_DETAIL + "?id=" + encodeURIComponent(String(id));

      // ✅ row 클릭으로 선택 가능하게 data 속성 부여
      return ""
        + "<tr data-room-id='" + escapeHtml(String(id)) + "' data-room-name='" + name + "' style='cursor:pointer;'>"
        +   "<td class='room-name'>" + name + "</td>"
        +   "<td style='width:120px;'>" + capacity + "</td>"
        +   "<td>" + location + "</td>"
        +   "<td style='width:140px;'>" + escapeHtml(statusText) + "</td>"
        +   "<td style='width:160px;'>"
        +     "<a class='btn btn-primary' href='" + escapeHtml(detailUrl) + "' onclick='event.stopPropagation();'>상세/예약</a>"
        +   "</td>"
        + "</tr>";
    }).join("");

    // ✅ row 클릭 -> 현황 카드 로드
    Array.from(tbody.querySelectorAll("tr[data-room-id]")).forEach((tr) => {
      tr.addEventListener("click", () => {
        const rid = parseInt(tr.getAttribute("data-room-id") || "0", 10);
        const rname = tr.getAttribute("data-room-name") || "";

        if (!rid) return;

        state.selectedRoomId = rid;
        state.selectedRoomName = rname;

        if (statusRoomLabel) statusRoomLabel.textContent = rname ? ("(" + rname + ")") : ("(ID:" + rid + ")");

        // 현재 탭 기준으로 바로 조회
        if (isMonthTab()) loadMonthStatus(rid);
        else loadDayStatus(rid);
      });
    });
  }

  // =========================================================
  // 예약 현황: 일자(타임테이블)
  // =========================================================
  async function loadDayStatus(roomId) {
    if (!timetableWrap) return;

    const date = (statusDate && statusDate.value) ? statusDate.value : todayISO();

    timetableWrap.innerHTML = "<div class='text-muted'>불러오는 중...</div>";

    try {
      const body = new URLSearchParams({
        roomId: String(roomId),
        date: String(date)
      });

      const json = await fetchJson(API_STATUS_DAY, {
        method: "POST",
        headers: { "Content-Type": "application/x-www-form-urlencoded; charset=UTF-8" },
        credentials: "same-origin",
        body: body.toString(),
        throwOnOkFalse: true
      });

      const data = json && json.data ? json.data : null;
      if (!data) throw new Error("응답 데이터가 없습니다.");

      renderTimetable(data);
    } catch (e) {
      timetableWrap.innerHTML = "";
      showModal("일자 현황 조회 실패: " + (e.message || e), "error");
    }
  }

  function renderTimetable(data) {
    if (!timetableWrap) return;

    const closed = data.closed === true;
    const open = String(data.open || "").trim();
    const close = String(data.close || "").trim();
    const slotMinutes = parseInt(String(data.slotMinutes || "60"), 10) || 60;

    if (closed) {
      const reason = data.reason ? (" (" + escapeHtml(String(data.reason)) + ")") : "";
      timetableWrap.innerHTML = "<div class='text-muted'>휴무일입니다." + reason + "</div>";
      return;
    }

    if (!open || !close) {
      timetableWrap.innerHTML = "<div class='text-muted'>운영시간 정보가 없습니다.</div>";
      return;
    }

    const resv = Array.isArray(data.reservations) ? data.reservations : [];

    const openMin = toMinutes(open);
    const closeMin = toMinutes(close);

    if (openMin == null || closeMin == null || openMin >= closeMin) {
      timetableWrap.innerHTML = "<div class='text-muted'>운영시간 형식이 올바르지 않습니다.</div>";
      return;
    }

    // 슬롯 생성
    const rows = [];
    for (let m = openMin; m + slotMinutes <= closeMin; m += slotMinutes) {
      const s = minutesToHHMM(m);
      const e = minutesToHHMM(m + slotMinutes);

      const busyInfo = findOverlap(resv, s, e);
      rows.push({ s, e, busy: busyInfo != null, title: busyInfo ? busyInfo.title : "" });
    }

    const html = []
    html.push("<div class='timetable'>");

    rows.forEach((r) => {
      html.push("<div class='timetable-row'>");
      html.push("<div class='timetable-time'>" + escapeHtml(r.s + " ~ " + r.e) + "</div>");
      html.push("<div class='timetable-cell " + (r.busy ? "busy" : "free") + "'>");
      html.push(r.busy ? ("예약됨" + (r.title ? (" - " + escapeHtml(r.title)) : "")) : "비어있음");
      html.push("</div>");
      html.push("</div>");
    });

    html.push("</div>");

    timetableWrap.innerHTML = html.join("");
  }

  function findOverlap(resvList, hhmmStart, hhmmEnd) {
    // resv.startTime/endTime: "yyyy-MM-dd HH:mm"
    const sMin = toMinutes(hhmmStart);
    const eMin = toMinutes(hhmmEnd);
    if (sMin == null || eMin == null) return null;

    for (let i = 0; i < resvList.length; i++) {
      const it = resvList[i];
      const st = String(it.startTime || "");
      const et = String(it.endTime || "");

      const stHH = st.slice(11, 16);
      const etHH = et.slice(11, 16);

      const stMin = toMinutes(stHH);
      const etMin = toMinutes(etHH);

      if (stMin == null || etMin == null) continue;

      // 겹침 판단: start < slotEnd && end > slotStart
      if (stMin < eMin && etMin > sMin) {
        return { title: it.title || "" };
      }
    }
    return null;
  }

  // =========================================================
  // 예약 현황: 달력(월)
  // =========================================================
  async function loadMonthStatus(roomId) {
    if (!calendarWrap) return;

    const month = (statusMonth && statusMonth.value) ? statusMonth.value : todayISO().slice(0, 7);

    calendarWrap.innerHTML = "<div class='text-muted'>불러오는 중...</div>";

    try {
      const body = new URLSearchParams({
        roomId: String(roomId),
        month: String(month)
      });

      const json = await fetchJson(API_STATUS_MONTH, {
        method: "POST",
        headers: { "Content-Type": "application/x-www-form-urlencoded; charset=UTF-8" },
        credentials: "same-origin",
        body: body.toString(),
        throwOnOkFalse: true
      });

      const data = json && json.data ? json.data : null;
      if (!data) throw new Error("응답 데이터가 없습니다.");

      renderCalendar(data);
    } catch (e) {
      calendarWrap.innerHTML = "";
      showModal("월 현황 조회 실패: " + (e.message || e), "error");
    }
  }

  /**
   * data: {
   *   month: "2026-02",
   *   days: [ { date:"2026-02-01", count:2 }, ... ]
   * }
   * - calendarWrap에 월 달력(월요일 시작) 렌더링
   */
  function renderCalendar(data) {
    if (!calendarWrap) return;

    const month = String((data && data.month) ? data.month : "").trim(); // YYYY-MM
    if (!month) {
      calendarWrap.innerHTML = "<div class='text-muted'>month 값이 없습니다.</div>";
      return;
    }

    // days -> countMap(YYYY-MM-DD => count)
    const days = Array.isArray(data.days) ? data.days : [];
    const countMap = {};
    days.forEach((d) => {
      const key = String(d && d.date ? d.date : "").slice(0, 10);
      const cnt = parseInt(String(d && d.count != null ? d.count : "0"), 10) || 0;
      if (key) countMap[key] = cnt;
    });

    const first = new Date(month + "-01T00:00:00");
    if (isNaN(first.getTime())) {
      calendarWrap.innerHTML = "<div class='text-muted'>월 데이터가 올바르지 않습니다.</div>";
      return;
    }

    const year = first.getFullYear();
    const mon = first.getMonth(); // 0-based
    const lastDay = new Date(year, mon + 1, 0).getDate();

    // ✅ 월요일 시작(월=1..일=7)
    const firstDow = jsDayToMondayStart(first.getDay()); // 1..7
    const blanks = firstDow - 1;

    const cells = [];

    // 앞 공백
    for (let i = 0; i < blanks; i++) {
      cells.push("<div class='cal-cell blank'></div>");
    }

    // 날짜
    for (let day = 1; day <= lastDay; day++) {
      const dateKey = year + "-" + pad2(mon + 1) + "-" + pad2(day);
      const cnt = countMap[dateKey] || 0;

      cells.push(
        "<div class='cal-cell'>"
        + "  <div class='cal-day'>" + escapeHtml(String(day)) + "</div>"
        + "  <div class='cal-count " + (cnt > 0 ? "has" : "") + "'>"
        +       (cnt > 0 ? (escapeHtml(String(cnt)) + "건") : "")
        + "  </div>"
        + "</div>"
      );
    }

    // 요일 헤더(월~일)
    const weekHead =
      "<div class='cal-weekhead'>"
      + "  <div>월</div><div>화</div><div>수</div><div>목</div><div>금</div><div>토</div><div>일</div>"
      + "</div>";

    calendarWrap.innerHTML =
      weekHead
      + "<div class='cal-grid'>"
      + cells.join("")
      + "</div>";
  }

    /**
     * JS: 0=일..6=토 -> 월=1..일=7
     * - 일(0) -> 7, 월(1) -> 1 ...
     */
    function jsDayToMondayStart(jsDay) {
      return jsDay === 0 ? 7 : jsDay;
    }

    /** 2자리 패딩 */
    function pad2(n) {
      return String(n).padStart(2, "0");
    }

  // =========================================================
  // 탭 전환
  // =========================================================
  function switchTab(mode) {
    if (!tabDay || !tabMonth || !dayControl || !monthControl || !dayView || !monthView) return;

    if (mode === "month") {
      tabDay.classList.remove("active");
      tabMonth.classList.add("active");
      dayControl.style.display = "none";
      monthControl.style.display = "flex";
      dayView.style.display = "none";
      monthView.style.display = "block";

      if (state.selectedRoomId) loadMonthStatus(state.selectedRoomId);
      return;
    }

    tabMonth.classList.remove("active");
    tabDay.classList.add("active");
    monthControl.style.display = "none";
    dayControl.style.display = "flex";
    monthView.style.display = "none";
    dayView.style.display = "block";

    if (state.selectedRoomId) loadDayStatus(state.selectedRoomId);
  }

  function isMonthTab() {
    return tabMonth && tabMonth.classList.contains("active");
  }

  // =========================================================
  // 페이지 메타 정규화
  // =========================================================
  function normalizePageForRenderPagination(p) {
    const page = p && p.page != null ? parseInt(p.page, 10) : 1;
    const size = p && p.size != null ? parseInt(p.size, 10) : state.size;

    const totalPages =
      (p && p.totalPages != null) ? parseInt(p.totalPages, 10)
      : (p && p.total_pages != null) ? parseInt(p.total_pages, 10)
      : 1;

    const totalElements =
      (p && p.total != null) ? parseInt(p.total, 10)
      : (p && p.totalElements != null) ? parseInt(p.totalElements, 10)
      : 0;

    return { page, size, totalPages, totalElements };
  }

  function extractTotal(json, fallback) {
    if (json && json.page) {
      if (json.page.total != null) return Number(json.page.total) || 0;
      if (json.page.totalElements != null) return Number(json.page.totalElements) || 0;
    }
    if (json && json.data) {
      if (json.data.total != null) return Number(json.data.total) || 0;
    }
    return fallback || 0;
  }

  // =========================================================
  // util
  // =========================================================
  function todayISO() {
    const d = new Date();
    const y = d.getFullYear();
    const m = pad2(d.getMonth() + 1);
    const day = pad2(d.getDate());
    return y + "-" + m + "-" + day;
  }

  function pad2(n) {
    return String(n).padStart(2, "0");
  }

  function toMinutes(hhmm) {
    const m = String(hhmm || "").match(/^(\d{1,2}):(\d{2})$/);
    if (!m) return null;
    const hh = parseInt(m[1], 10);
    const mm = parseInt(m[2], 10);
    if (Number.isNaN(hh) || Number.isNaN(mm)) return null;
    if (hh < 0 || hh > 23) return null;
    if (mm < 0 || mm > 59) return null;
    return hh * 60 + mm;
  }

  function minutesToHHMM(min) {
    const hh = Math.floor(min / 60);
    const mm = min % 60;
    return pad2(hh) + ":" + pad2(mm);
  }

  function parseYearMonth(s) {
    const m = String(s || "").match(/^(\d{4})-(\d{2})$/);
    if (!m) return null;
    const y = parseInt(m[1], 10);
    const mo = parseInt(m[2], 10);
    if (Number.isNaN(y) || Number.isNaN(mo)) return null;
    if (mo < 1 || mo > 12) return null;
    return { y: y, m: mo };
  }

})();
