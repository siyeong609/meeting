/**
 * user/room/detail.js
 * - 회의실 상세(정보/운영시간) + 예약 현황(일자 타임테이블 / 월 달력)
 *
 * 전제:
 * - window.__MEETING__.ctx
 * - common.js: fetchJson / showModal / escapeHtml 존재
 *
 * 서버(권장) 엔드포인트 규약:
 * - POST /user/rooms/detail
 *   body: id
 *   resp: ApiResponse.ok(RoomDetail)
 *
 * - POST /user/rooms/status/day
 *   body: roomId, date(YYYY-MM-DD)
 *   resp: ApiResponse.ok({
 *     date: "2026-02-02",
 *     open: "09:00",
 *     close: "18:00",
 *     slotMinutes: 60,
 *     reservations: [
 *       { start: "2026-02-02 10:00", end: "2026-02-02 11:00", title:"..." }
 *     ]
 *   })
 *
 * - POST /user/rooms/status/month
 *   body: roomId, month(YYYY-MM)
 *   resp: ApiResponse.ok({
 *     month: "2026-02",
 *     days: [ { date:"2026-02-01", count:2 }, ... ]
 *   })
 */
(function () {
  "use strict";

  const ctx = (window.__MEETING__ && window.__MEETING__.ctx) ? window.__MEETING__.ctx : "";

  // ✅ GET은 JSP 페이지, POST는 JSON을 준다는 전제로 동일 경로 사용
  const API_DETAIL = ctx + "/user/rooms/detail";

  // ✅ 예약 현황 API(네가 아직 안 만들었을 수 있음)
  const API_STATUS_DAY = ctx + "/user/rooms/status/day";
  const API_STATUS_MONTH = ctx + "/user/rooms/status/month";

  // ✅ 페이지 이동 URL
  const PAGE_ROOM_LIST = ctx + "/user/rooms";
  const PAGE_RESERVATION_NEW = ctx + "/user/reservations/new"; // 필요하면 create로 바꿔도 됨

  // DOM
  const roomIdEl = document.getElementById("roomId");
  const roomNameEl = document.getElementById("roomName");
  const roomMetaEl = document.getElementById("roomMeta");
  const hoursWrap = document.getElementById("hoursWrap");

  const btnBack = document.getElementById("btnBackToList");
  const btnStart = document.getElementById("btnStartBooking");

  // status UI
  const tabDay = document.getElementById("tabDay");
  const tabMonth = document.getElementById("tabMonth");
  const dayControl = document.getElementById("dayControl");
  const monthControl = document.getElementById("monthControl");
  const statusDate = document.getElementById("statusDate");
  const statusMonth = document.getElementById("statusMonth");
  const btnLoadDay = document.getElementById("btnLoadDay");
  const btnLoadMonth = document.getElementById("btnLoadMonth");
  const statusHint = document.getElementById("statusHint");
  const dayTimelineWrap = document.getElementById("dayTimelineWrap");
  const monthCalendarWrap = document.getElementById("monthCalendarWrap");

  // state
  let currentRoomId = 0;
  let currentRoomDetail = null;
  let activeTab = "DAY"; // DAY | MONTH

  document.addEventListener("DOMContentLoaded", () => {
    // 의존성 체크
    if (typeof fetchJson !== "function" || typeof escapeHtml !== "function" || typeof showModal !== "function") {
      alert("common.js 의 fetchJson/escapeHtml/showModal 이 필요합니다.");
      return;
    }

    currentRoomId = parseInt(String(roomIdEl ? roomIdEl.value : "0"), 10) || 0;
    if (!currentRoomId) {
      showModal("roomId가 없습니다. 목록에서 다시 진입해주세요.", "error");
      return;
    }

    // 버튼
    if (btnBack) {
      btnBack.addEventListener("click", () => {
        location.href = PAGE_ROOM_LIST;
      });
    }

    if (btnStart) {
      btnStart.addEventListener("click", () => {
        // ✅ roomId를 쿼리로 넘겨서 예약 생성 화면에서 미리 선택 가능
        location.href = PAGE_RESERVATION_NEW + "?roomId=" + encodeURIComponent(String(currentRoomId));
      });
    }

    // 탭
    if (tabDay) tabDay.addEventListener("click", () => {
        switchTab("DAY");
        loadDayStatus();
    });
    if (tabMonth) tabMonth.addEventListener("click", () => {
        switchTab("MONTH");
        loadMonthStatus();
    });

    // 기본 날짜/월 세팅(오늘)
    const now = new Date();
    if (statusDate) statusDate.value = toDateInputValue(now);
    if (statusMonth) statusMonth.value = toMonthInputValue(now);

    // 조회 버튼
    if (btnLoadDay) btnLoadDay.addEventListener("click", () => loadDayStatus());
    if (btnLoadMonth) btnLoadMonth.addEventListener("click", () => loadMonthStatus());

    // 1) 상세 로드
    loadRoomDetail()
      .then(() => {
        // 2) 예약현황 기본 로드(일자)
        switchTab("DAY");
        loadDayStatus();
      })
      .catch((e) => {
        showModal("회의실 상세 로드 실패: " + (e.message || e), "error");
      });
  });

  // =========================================================
  // Room Detail
  // =========================================================
  async function loadRoomDetail() {
    if (roomMetaEl) roomMetaEl.innerHTML = "<span class='text-muted'>로딩 중...</span>";

    const body = new URLSearchParams({ id: String(currentRoomId) });

    const json = await fetchJson(API_DETAIL, {
      method: "POST",
      headers: { "Content-Type": "application/x-www-form-urlencoded; charset=UTF-8" },
      credentials: "same-origin",
      body: body.toString(),
      throwOnOkFalse: true
    });

    if (!json || json.ok !== true || !json.data) {
      throw new Error((json && json.message) ? json.message : "상세 데이터가 없습니다.");
    }

    currentRoomDetail = json.data;

    // 제목
    const roomName = String(currentRoomDetail.name || "회의실 상세");
    if (roomNameEl) roomNameEl.textContent = roomName;

    // 메타(그리드)
    renderRoomMeta(currentRoomDetail);

    // 운영시간
    renderOperatingHours(currentRoomDetail);
  }

  function renderRoomMeta(d) {
    if (!roomMetaEl) return;

    const capacity = (d.capacity != null) ? String(d.capacity) : "-";
    const location = d.location ? String(d.location) : "-";
    const active = (d.active === true) ? "사용" : "중지";
    const slot = (d.slotMinutes != null) ? String(d.slotMinutes) + "분" : "-";
    const buffer = (d.bufferMinutes != null) ? String(d.bufferMinutes) + "분" : "-";

    roomMetaEl.innerHTML =
      "<div class='room-meta-grid'>"
      + "  <div class='room-meta-label'>위치</div><div class='room-meta-value'>" + escapeHtml(location) + "</div>"
      + "  <div class='room-meta-label'>수용인원</div><div class='room-meta-value'>" + escapeHtml(capacity) + "</div>"
      + "  <div class='room-meta-label'>상태</div><div class='room-meta-value'>" + escapeHtml(active) + "</div>"
      + "  <div class='room-meta-label'>슬롯</div><div class='room-meta-value'>" + escapeHtml(slot) + "</div>"
      + "  <div class='room-meta-label'>버퍼</div><div class='room-meta-value'>" + escapeHtml(buffer) + "</div>"
      + "</div>";
  }

  /**
   * 운영시간: thead를 요일(월~일)로 두는 형태
   * - data.operatingHours: [{dow, closed, openTime, closeTime}, ...]
   */
  function renderOperatingHours(d) {
    if (!hoursWrap) return;

    const list = Array.isArray(d.operatingHours) ? d.operatingHours : [];
    const map = {};
    list.forEach((it) => {
      const dow = parseInt(String(it.dow || "0"), 10);
      if (dow >= 1 && dow <= 7) {
        map[dow] = it;
      }
    });

    const labels = ["월", "화", "수", "목", "금", "토", "일"];
    const ths = labels.map((x) => "<th>" + x + "</th>").join("");

    const tds = [];
    for (let dow = 1; dow <= 7; dow++) {
      const it = map[dow] || null;
      const closed = it ? (it.closed === true || it.isClosed === true) : true;
      const open = it && it.openTime ? String(it.openTime) : "";
      const close = it && it.closeTime ? String(it.closeTime) : "";

      if (closed) tds.push("<td class='text-muted'>휴무</td>");
      else tds.push("<td><strong>" + escapeHtml(open) + "</strong> ~ <strong>" + escapeHtml(close) + "</strong></td>");
    }

    hoursWrap.innerHTML =
      "<div class='user-table-wrap'>"
      + "  <table class='user-hours-table'>"
      + "    <thead><tr>" + ths + "</tr></thead>"
      + "    <tbody><tr>" + tds.join("") + "</tr></tbody>"
      + "  </table>"
      + "</div>";
  }

  // =========================================================
  // Tab
  // =========================================================
  function switchTab(tab) {
    activeTab = tab;

    // 버튼 active
    if (tabDay && tabMonth) {
      if (tab === "DAY") {
        tabDay.classList.add("active");
        tabMonth.classList.remove("active");
        tabDay.setAttribute("aria-selected", "true");
        tabMonth.setAttribute("aria-selected", "false");
      } else {
        tabMonth.classList.add("active");
        tabDay.classList.remove("active");
        tabMonth.setAttribute("aria-selected", "true");
        tabDay.setAttribute("aria-selected", "false");
      }
    }

    // 컨트롤 show/hide
    if (dayControl) dayControl.style.display = (tab === "DAY") ? "flex" : "none";
    if (monthControl) monthControl.style.display = (tab === "MONTH") ? "flex" : "none";

    // 컨텐츠 show/hide
    if (dayTimelineWrap) dayTimelineWrap.style.display = (tab === "DAY") ? "block" : "none";
    if (monthCalendarWrap) monthCalendarWrap.style.display = (tab === "MONTH") ? "block" : "none";

    if (statusHint) {
      statusHint.textContent = (tab === "DAY")
        ? "회의실을 선택하면 해당 일자의 타임테이블이 표시됩니다."
        : "해당 월의 일자별 예약 건수를 표시합니다.";
    }
  }

  // =========================================================
  // Status - Day
  // =========================================================
  async function loadDayStatus() {
    if (!statusDate || !dayTimelineWrap) return;

    const date = String(statusDate.value || "").trim();
    if (!date) {
      showModal("날짜를 선택하세요.", "warning");
      return;
    }

    if (statusHint) statusHint.textContent = "일자 예약 현황을 불러오는 중...";
    dayTimelineWrap.innerHTML = "<div class='text-muted'>로딩 중...</div>";

    try {
      const body = new URLSearchParams({
        roomId: String(currentRoomId),
        date: date
      });

      const json = await fetchJson(API_STATUS_DAY, {
        method: "POST",
        headers: { "Content-Type": "application/x-www-form-urlencoded; charset=UTF-8" },
        credentials: "same-origin",
        body: body.toString(),
        throwOnOkFalse: true
      });

      if (!json || json.ok !== true || !json.data) {
        throw new Error((json && json.message) ? json.message : "일자 현황 데이터가 없습니다.");
      }

      renderDayTimeline(json.data);

      if (statusHint) statusHint.textContent = "일자 타임테이블 기준으로 예약 여부를 표시합니다.";

    } catch (e) {
      dayTimelineWrap.innerHTML = "<div class='text-muted'>일자 현황 조회 실패</div>";
      if (statusHint) statusHint.textContent = "일자 예약 현황을 불러오지 못했습니다.";
      // ✅ API 아직 없을 때도 사용자에게 이유를 보여주기
      showModal("일자 예약 현황 조회 실패: " + (e.message || e), "error");
    }
  }

  /**
   * data: { open, close, slotMinutes, reservations:[{start,end,title}] }
   */
  function renderDayTimeline(data) {
    // ✅ 운영시간/슬롯 정보
    const open = String(data.open || "").trim();
    const close = String(data.close || "").trim();
    const slotMinutes = parseInt(String(data.slotMinutes || "60"), 10) || 60;

    // 운영시간이 없거나 휴무인 케이스
    if (!open || !close) {
      dayTimelineWrap.innerHTML =
        "<div class='text-muted'>해당 일자는 운영시간이 없거나 휴무입니다.</div>";
      return;
    }

    // ✅ 예약 목록 (서버가 reservations로 주는 전제)
    const resv = Array.isArray(data.reservations) ? data.reservations : [];

    // ✅ 예약 구간을 "일자 내 분(minute)"으로 변환
    // - 서버가 start/end 또는 startTime/endTime 등으로 내려줄 수 있으니 흡수
    const blocks = resv
      .map((r) => {
        const startRaw = pickStart(r);
        const endRaw = pickEnd(r);

        const s = parseDateTimeToMinutes(startRaw);
        const e = parseDateTimeToMinutes(endRaw);

        return {
          startMin: s,
          endMin: e,
          title: String(r.title || ""),
          rawStart: String(startRaw || ""),
          rawEnd: String(endRaw || "")
        };
      })
      .filter((x) => x.startMin != null && x.endMin != null);

    // ✅ 운영시간 분 변환
    const openMin = parseHHMMToMinutes(open);
    const closeMin = parseHHMMToMinutes(close);

    if (openMin == null || closeMin == null || openMin >= closeMin) {
      dayTimelineWrap.innerHTML =
        "<div class='text-muted'>운영시간 데이터가 올바르지 않습니다.</div>";
      return;
    }

    // ✅ 슬롯별 row 생성
    const rows = [];
    for (let t = openMin; t < closeMin; t += slotMinutes) {
      const slotStart = t;
      const slotEnd = Math.min(t + slotMinutes, closeMin);

      // 슬롯과 겹치는 예약 찾기
      const hit = blocks.find((b) => isOverlap(slotStart, slotEnd, b.startMin, b.endMin));
      const timeLabel = minutesToHHMM(slotStart) + " ~ " + minutesToHHMM(slotEnd);

      if (hit) {
        rows.push(
          "<div class='timetable-row'>"
          + "  <div class='timetable-time'>" + escapeHtml(timeLabel) + "</div>"
          + "  <div class='timetable-cell busy'>"
          + "    예약됨 - "
          +      escapeHtml(hit.title || "(제목 없음)")
          + "  </div>"
          + "</div>"
        );
      } else {
        rows.push(
          "<div class='timetable-row'>"
          + "  <div class='timetable-time'>" + escapeHtml(timeLabel) + "</div>"
          + "  <div class='timetable-cell free'>비어있음</div>"
          + "</div>"
        );
      }
    }

    // ✅ HTML 최종 조립 (세미콜론/닫는태그 정상)
    dayTimelineWrap.innerHTML =
      "<div class='timeline-wrap'>"
      + "  <div class='timetable'>"
      + rows.join("")
      + "  </div>"
      + "</div>";
  }

  /**
   * ✅ 예약 시작/끝 필드명 흡수
   * - 서버 응답 DTO가 start/end 또는 startTime/endTime 등으로 바뀌어도 대응
   */
  function pickStart(r) {
    return r.start || r.startTime || r.start_time || r.begin || "";
  }

  function pickEnd(r) {
    return r.end || r.endTime || r.end_time || r.finish || "";
  }


  // =========================================================
  // Status - Month
  // =========================================================
  async function loadMonthStatus() {
    if (!statusMonth || !monthCalendarWrap) return;

    const month = String(statusMonth.value || "").trim(); // YYYY-MM
    if (!month) {
      showModal("월을 선택하세요.", "warning");
      return;
    }

    if (statusHint) statusHint.textContent = "월별 예약 현황을 불러오는 중...";
    monthCalendarWrap.innerHTML = "<div class='text-muted'>로딩 중...</div>";

    try {
      const body = new URLSearchParams({
        roomId: String(currentRoomId),
        month: month
      });

      const json = await fetchJson(API_STATUS_MONTH, {
        method: "POST",
        headers: { "Content-Type": "application/x-www-form-urlencoded; charset=UTF-8" },
        credentials: "same-origin",
        body: body.toString(),
        throwOnOkFalse: true
      });

      if (!json || json.ok !== true || !json.data) {
        throw new Error((json && json.message) ? json.message : "월 현황 데이터가 없습니다.");
      }

      renderMonthCalendar(month, json.data);

      if (statusHint) statusHint.textContent = "일자별 예약 건수를 표시합니다.";

    } catch (e) {
      monthCalendarWrap.innerHTML = "<div class='text-muted'>월 현황 조회 실패</div>";
      if (statusHint) statusHint.textContent = "월별 예약 현황을 불러오지 못했습니다.";
      showModal("월별 예약 현황 조회 실패: " + (e.message || e), "error");
    }
  }

  /**
   * data: { days:[{date, count}] }
   */
  function renderMonthCalendar(month, data) {
    const days = Array.isArray(data.days) ? data.days : [];
    const countMap = {};
    days.forEach((d) => {
      const key = String(d.date || "").slice(0, 10);
      const cnt = parseInt(String(d.count || "0"), 10) || 0;
      if (key) countMap[key] = cnt;
    });

    const first = new Date(month + "-01T00:00:00");
    if (isNaN(first.getTime())) {
      monthCalendarWrap.innerHTML = "<div class='text-muted'>월 데이터가 올바르지 않습니다.</div>";
      return;
    }

    const year = first.getFullYear();
    const mon = first.getMonth(); // 0-based
    const lastDay = new Date(year, mon + 1, 0).getDate();

    // 월요일 시작(월=1..일=7) 형태로 맞춤
    const firstDow = jsDayToMondayStart(first.getDay()); // 1..7
    const blanks = firstDow - 1;

    const cells = [];

    // 앞 공백
    for (let i = 0; i < blanks; i++) cells.push("<div class='cal-cell blank'></div>");

    // 날짜
    for (let day = 1; day <= lastDay; day++) {
      const dateKey = year + "-" + pad2(mon + 1) + "-" + pad2(day);
      const cnt = countMap[dateKey] || 0;

      cells.push(
        "<div class='cal-cell'>"
        + "  <div class='cal-day'>" + day + "</div>"
        + "  <div class='cal-count " + (cnt > 0 ? "has" : "") + "'>" + (cnt > 0 ? (cnt + "건") : "") + "</div>"
        + "</div>"
      );
    }

    // 요일 헤더
    const weekHead =
      "<div class='cal-weekhead'>"
      + "  <div>월</div><div>화</div><div>수</div><div>목</div><div>금</div><div>토</div><div>일</div>"
      + "</div>";

    monthCalendarWrap.innerHTML =
      weekHead
      + "<div class='cal-grid'>"
      + cells.join("")
      + "</div>";
  }

  // =========================================================
  // Util
  // =========================================================
  function toDateInputValue(d) {
    return d.getFullYear() + "-" + pad2(d.getMonth() + 1) + "-" + pad2(d.getDate());
  }

  function toMonthInputValue(d) {
    return d.getFullYear() + "-" + pad2(d.getMonth() + 1);
  }

  function pad2(n) {
    const s = String(n);
    return s.length >= 2 ? s : ("0" + s);
  }

  function parseHHMMToMinutes(hhmm) {
    const m = String(hhmm || "").trim().match(/^(\d{1,2}):(\d{2})$/);
    if (!m) return null;
    const h = parseInt(m[1], 10);
    const mm = parseInt(m[2], 10);
    if (isNaN(h) || isNaN(mm) || h < 0 || h > 23 || mm < 0 || mm > 59) return null;
    return h * 60 + mm;
  }

  /**
   * "YYYY-MM-DD HH:mm" -> 하루 분단위로 변환(타임테이블 비교용)
   * - 날짜가 다르면 비교가 엉킬 수 있으니, 서버는 같은 date 기준으로 내려주는게 안전
   */
  function parseDateTimeToMinutes(dt) {
    const s = String(dt || "").trim().replace("T", " ");
    const m = s.match(/^\d{4}-\d{2}-\d{2}\s+(\d{1,2}):(\d{2})/);
    if (!m) return null;
    const h = parseInt(m[1], 10);
    const mm = parseInt(m[2], 10);
    if (isNaN(h) || isNaN(mm)) return null;
    return h * 60 + mm;
  }

  function minutesToHHMM(min) {
    const h = Math.floor(min / 60);
    const m = min % 60;
    return pad2(h) + ":" + pad2(m);
  }

  function isOverlap(aStart, aEnd, bStart, bEnd) {
    // [aStart, aEnd) vs [bStart, bEnd)
    return aStart < bEnd && aEnd > bStart;
  }

  function jsDayToMondayStart(jsDay) {
    // JS: 0=일..6=토 -> 월=1..일=7
    // jsDay=0(일) -> 7
    // jsDay=1(월) -> 1
    return jsDay === 0 ? 7 : jsDay;
  }

})();
