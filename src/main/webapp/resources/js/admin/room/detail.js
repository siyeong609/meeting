/**
 * admin/room/detail.js
 * - (관리자) 회의실 상세(정보/운영시간) + 예약 현황(일자 타임테이블 / 월 달력)
 * - ✅ 추가: status/day 응답(reservations)으로 "예약 목록 테이블"도 함께 렌더
 *
 * 전제:
 * - window.__MEETING__.ctx
 * - common.js: fetchJson / showModal / escapeHtml 존재
 */
(function () {
  "use strict";

  const ctx = (window.__MEETING__ && window.__MEETING__.ctx) ? window.__MEETING__.ctx : "";

  // ✅ Room detail
  const API_DETAIL = ctx + "/admin/rooms/detail";
    // ✅ 예약 CRUD (관리자, rooms/detail 하위로 통일)
    const API_CREATE = ctx + "/admin/rooms/detail/create";
    const API_UPDATE = ctx + "/admin/rooms/detail/update";
    const API_CANCEL = ctx + "/admin/rooms/detail/cancel";

    // ✅ 유저 드롭다운
    const API_USER_OPTIONS = ctx + "/admin/users/options";

  // ✅ Status API (관리자)
  const API_STATUS_DAY = ctx + "/admin/rooms/status/day";
  const API_STATUS_MONTH = ctx + "/admin/rooms/status/month";

  // ✅ 페이지 이동
  const PAGE_ROOM_LIST = ctx + "/admin/rooms";

  // DOM
  const roomIdEl = document.getElementById("roomId");
  const roomNameEl = document.getElementById("roomName");
  const roomMetaEl = document.getElementById("roomMeta");
  const hoursWrap = document.getElementById("hoursWrap"); // 운영시간 영역(없으면 무시)

  const btnBack = document.getElementById("btnBackToList");
  const btnCreateReservation = document.getElementById("btnCreateReservation");

  // ✅ 예약 목록 테이블 DOM (JSP에 있음)
  const reservationTbody = document.getElementById("reservationTableBody");

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
  let lastDayStatus = null;

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

    // 목록 버튼
    if (btnBack) {
      btnBack.addEventListener("click", () => {
        location.href = PAGE_ROOM_LIST;
      });
    }

    if (btnCreateReservation) {
      btnCreateReservation.addEventListener("click", () => {
        openCreateModal();
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

    // 초기 렌더 (예약목록 placeholder)
    if (reservationTbody) {
      reservationTbody.innerHTML =
        "<tr><td colspan='6' class='text-muted' style='text-align:center;'>일자를 선택하면 예약 목록이 표시됩니다.</td></tr>";
    }

    // 1) 상세 로드 -> 2) 일자 현황 로드
    loadRoomDetail()
      .then(() => {
        switchTab("DAY");
        loadDayStatus();
      })
      .catch((e) => showModal("회의실 상세 로드 실패: " + (e.message || e), "error"));
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

    const roomName = String(currentRoomDetail.name || "회의실 상세");
    if (roomNameEl) roomNameEl.textContent = roomName;

    renderRoomMeta(currentRoomDetail);
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
   * 운영시간 테이블 렌더
   * - d.operatingHours: [{dow, closed, openTime, closeTime}, ...]
   */
  function renderOperatingHours(d) {
    if (!hoursWrap) return;

    const list = Array.isArray(d.operatingHours) ? d.operatingHours : [];
    const map = {};
    list.forEach((it) => {
      const dow = parseInt(String(it.dow || "0"), 10);
      if (dow >= 1 && dow <= 7) map[dow] = it;
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

    if (dayControl) dayControl.style.display = (tab === "DAY") ? "flex" : "none";
    if (monthControl) monthControl.style.display = (tab === "MONTH") ? "flex" : "none";

    if (dayTimelineWrap) dayTimelineWrap.style.display = (tab === "DAY") ? "block" : "none";
    if (monthCalendarWrap) monthCalendarWrap.style.display = (tab === "MONTH") ? "block" : "none";

    if (statusHint) {
      statusHint.textContent = (tab === "DAY")
        ? "해당 일자의 타임테이블/예약목록을 표시합니다."
        : "해당 월의 일자별 예약 건수를 표시합니다.";
    }
  }

  // =========================================================
  // Status - Day (타임테이블 + 예약목록)
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
    if (reservationTbody) {
      reservationTbody.innerHTML =
        "<tr><td colspan='6' class='text-muted' style='text-align:center;'>로딩 중...</td></tr>";
    }

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

      // ✅ 1) 타임테이블
      lastDayStatus = json.data;
      renderDayTimeline(json.data);

      // ✅ 2) 예약 목록 테이블 (핵심 추가)
      lastDayStatus = json.data;
      renderReservationTable(json.data);

      if (statusHint) statusHint.textContent = "일자 기준으로 타임테이블/예약목록을 표시합니다.";

    } catch (e) {
      dayTimelineWrap.innerHTML = "<div class='text-muted'>일자 현황 조회 실패</div>";
      if (reservationTbody) {
        reservationTbody.innerHTML =
          "<tr><td colspan='6' class='text-muted' style='text-align:center;'>조회 실패</td></tr>";
      }
      if (statusHint) statusHint.textContent = "일자 예약 현황을 불러오지 못했습니다.";
      showModal("일자 예약 현황 조회 실패: " + (e.message || e), "error");
    }
  }

  /**
   * ✅ 예약목록 테이블 렌더
   * - data.reservations 배열을 그대로 사용
   * - id/title/startTime/endTime/status 필드명 흔들림 흡수
   */
  function renderReservationTable(data) {
    if (!reservationTbody) return;

    const list = Array.isArray(data.reservations) ? data.reservations : [];

    if (list.length === 0) {
      reservationTbody.innerHTML =
        "<tr><td colspan='6' class='text-muted' style='text-align:center;'>해당 일자 예약이 없습니다.</td></tr>";
      return;
    }

    reservationTbody.innerHTML = list.map((r) => {
      const id = escapeHtml(String(r.id ?? "-"));
      const title = escapeHtml(String(r.title ?? "(제목 없음)"));

      const st = escapeHtml(String(pickStart(r) || "-"));
      const et = escapeHtml(String(pickEnd(r) || "-"));

      const status = escapeHtml(String(r.status ?? r.state ?? "BOOKED"));

      // ✅ 관리 버튼(일단 자리만)
      // - 이후 "취소/상세" 같은 관리자 기능 붙일 때 여기서 data-id로 이벤트 연결
      const actions =
        "<button type='button' class='btn btn-primary btn-sm' data-resv-id='" + id + "'>상세</button>"
        + " <button type='button' class='btn btn-danger btn-sm' data-resv-cancel-id='" + id + "'>취소</button>";

      return ""
        + "<tr>"
        + "  <td>" + id + "</td>"
        + "  <td>" + title + "</td>"
        + "  <td>" + st + "</td>"
        + "  <td>" + et + "</td>"
        + "  <td>" + status + "</td>"
        + "  <td>" + actions + "</td>"
        + "</tr>";
    }).join("");

    reservationTbody.querySelectorAll("[data-resv-id]").forEach((btn) => {
      btn.addEventListener("click", () => {
        const id = parseInt(btn.getAttribute("data-resv-id"), 10) || 0;
        if (!id) return;

        // ✅ 행에서 start/end/title을 읽어서 모달 초기값 구성(별도 detail API 없이)
        const tr = btn.closest("tr");
        openUpdateModalFromRow(id, tr);
      });
    });

    reservationTbody.querySelectorAll("[data-resv-cancel-id]").forEach((btn) => {
      btn.addEventListener("click", async () => {
        const id = parseInt(btn.getAttribute("data-resv-cancel-id"), 10) || 0;
        if (!id) return;

        if (!confirm("해당 예약을 취소하시겠습니까?")) return;

        try {
          await callCancel(id);
          await loadDayStatus();
          showModal("취소되었습니다.", "info");
        } catch (e) {
          showModal("취소 실패: " + (e.message || e), "error");
        }
      });
    });

  }

  /**
   * data: { open, close, slotMinutes, reservations:[{startTime,endTime,title}] }
   */
  function renderDayTimeline(data) {
    const open = String(data.open || "").trim();
    const close = String(data.close || "").trim();
    const slotMinutes = parseInt(String(data.slotMinutes || "60"), 10) || 60;

    if (!open || !close) {
      dayTimelineWrap.innerHTML = "<div class='text-muted'>해당 일자는 운영시간이 없거나 휴무입니다.</div>";
      return;
    }

    const resv = Array.isArray(data.reservations) ? data.reservations : [];

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

    const openMin = parseHHMMToMinutes(open);
    const closeMin = parseHHMMToMinutes(close);

    if (openMin == null || closeMin == null || openMin >= closeMin) {
      dayTimelineWrap.innerHTML = "<div class='text-muted'>운영시간 데이터가 올바르지 않습니다.</div>";
      return;
    }

    const rows = [];
    for (let t = openMin; t < closeMin; t += slotMinutes) {
      const slotStart = t;
      const slotEnd = Math.min(t + slotMinutes, closeMin);

      const hit = blocks.find((b) => isOverlap(slotStart, slotEnd, b.startMin, b.endMin));
      const timeLabel = minutesToHHMM(slotStart) + " ~ " + minutesToHHMM(slotEnd);

      if (hit) {
        rows.push(
          "<div class='timetable-row'>"
          + "  <div class='timetable-time'>" + escapeHtml(timeLabel) + "</div>"
          + "  <div class='timetable-cell busy'>예약됨 - " + escapeHtml(hit.title || "(제목 없음)") + "</div>"
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

    dayTimelineWrap.innerHTML =
      "<div class='timeline-wrap'>"
      + "  <div class='timetable'>"
      + rows.join("")
      + "  </div>"
      + "</div>";
  }

  // =========================================================
  // Status - Month
  // =========================================================
  async function loadMonthStatus() {
    if (!statusMonth || !monthCalendarWrap) return;

    const month = String(statusMonth.value || "").trim();
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
    const mon = first.getMonth();
    const lastDay = new Date(year, mon + 1, 0).getDate();

    const firstDow = jsDayToMondayStart(first.getDay());
    const blanks = firstDow - 1;

    const cells = [];
    for (let i = 0; i < blanks; i++) cells.push("<div class='cal-cell blank'></div>");

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
  function pickStart(r) {
    return r.startTime || r.start || r.start_time || r.begin || "";
  }

  function pickEnd(r) {
    return r.endTime || r.end || r.end_time || r.finish || "";
  }

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

    /**
     * 기존 선택(start HH:mm)이 새 옵션 목록에 있으면 유지,
     * 없으면 가장 가까운 옵션을 선택해서 반환한다.
     */
    function pickClosestStart(prevStart, options) {
      if (!Array.isArray(options) || options.length === 0) return "";

      const prev = String(prevStart || "").trim();
      if (prev && options.includes(prev)) return prev;

      const prevMin = parseHHMMToMinutes(prev);
      if (prevMin == null) return options[0];

      let best = options[0];
      let bestDiff = Math.abs(parseHHMMToMinutes(best) - prevMin);

      for (let i = 1; i < options.length; i++) {
        const m = parseHHMMToMinutes(options[i]);
        if (m == null) continue;
        const diff = Math.abs(m - prevMin);
        if (diff < bestDiff) {
          bestDiff = diff;
          best = options[i];
        }
      }
      return best;
    }

    /**
     * select 엘리먼트에 옵션을 세팅하면서 selectedValue를 유지한다.
     */
    function setSelectOptions(selectEl, options, selectedValue) {
      if (!selectEl) return;

      const safeSelected = pickClosestStart(selectedValue, options);

      selectEl.innerHTML = options
        .map((t) => {
          const sel = (t === safeSelected) ? " selected" : "";
          return "<option value='" + escapeHtml(t) + "'" + sel + ">" + escapeHtml(t) + "</option>";
        })
        .join("");
    }


  function isOverlap(aStart, aEnd, bStart, bEnd) {
    return aStart < bEnd && aEnd > bStart;
  }

  function jsDayToMondayStart(jsDay) {
    return jsDay === 0 ? 7 : jsDay;
  }

    // =========================================================
    // ✅ Simple Modal(오버레이) - 유틸 추가 없이 detail.js 내부에서 처리
    // =========================================================
    function openOverlay(html) {
      closeOverlay();

      const wrap = document.createElement("div");
      wrap.id = "adminOverlay";
      wrap.innerHTML =
        "<div class='m-overlay'>"
        + "  <div class='m-dialog'>"
        + "    <div class='m-body'>" + html + "</div>"
        + "  </div>"
        + "</div>";

      document.body.appendChild(wrap);

      // 바깥 클릭 닫기
      wrap.querySelector(".m-overlay").addEventListener("click", (e) => {
        if (e.target.classList.contains("m-overlay")) closeOverlay();
      });
    }

    function closeOverlay() {
      const el = document.getElementById("adminOverlay");
      if (el) el.remove();
    }

    async function openCreateModal() {
      if (!lastDayStatus) {
        showModal("먼저 일자 현황을 조회해주세요.", "warning");
        return;
      }

      const date = String(statusDate ? statusDate.value : "").trim();
      if (!date) {
        showModal("예약일을 선택하세요.", "warning");
        return;
      }

      // ✅ 운영시간/슬롯 기반 옵션 생성
      const open = String(lastDayStatus.open || "").trim();
      const close = String(lastDayStatus.close || "").trim();
      const slotMinutes = parseInt(String(lastDayStatus.slotMinutes || "60"), 10) || 60;

      if (!open || !close) {
        showModal("해당 일자는 휴무이거나 운영시간이 없습니다.", "warning");
        return;
      }

      // ✅ duration 옵션(기본: slot 단위로 60~240 정도, 서버가 최종 검증)
      const durationOptions = buildDurationOptions(slotMinutes);

      // ✅ 시작시간 옵션(기본 duration=첫 옵션으로 계산)
      const startOptions = buildStartTimeOptions(open, close, slotMinutes, durationOptions[0]);

      // ✅ 유저 옵션 로드
      const users = await fetchUserOptions("");

      const html =
        "<h3 style='margin:0 0 12px 0;'>예약 생성</h3>"
        + "<div class='m-form'>"
        + "  <label>유저</label>"
        + "  <select id='m_userId'>" + users.map(u => "<option value='" + escapeHtml(String(u.id)) + "'>" + escapeHtml(String(u.name)) + "</option>").join("") + "</select>"
        + "  <label>예약일</label>"
        + "  <input id='m_date' type='date' value='" + escapeHtml(date) + "' />"
        + "  <label>시작시간</label>"
        + "  <select id='m_start'>" + startOptions.map(t => "<option value='" + escapeHtml(t) + "'>" + escapeHtml(t) + "</option>").join("") + "</select>"
        + "  <label>예약시간</label>"
        + "  <select id='m_duration'>" + durationOptions.map(m => "<option value='" + m + "'>" + m + "분</option>").join("") + "</select>"
        + "  <label>제목(선택)</label>"
        + "  <input id='m_title' type='text' placeholder='예: 주간 회의' />"
        + "</div>"
        + "<div class='m-actions'>"
        + "  <button type='button' class='btn btn-secondary' id='m_close'>닫기</button>"
        + "  <button type='button' class='btn btn-primary' id='m_save'>저장</button>"
        + "</div>";

      openOverlay(html);

      document.getElementById("m_close").addEventListener("click", closeOverlay);

      // duration 변경 시 시작시간 옵션 재생성(끝나는 시간이 close 넘지 않게)
      document.getElementById("m_duration").addEventListener("change", () => {
        const dur = parseInt(document.getElementById("m_duration").value, 10) || slotMinutes;

        const sel = document.getElementById("m_start");
        const prevStart = sel ? sel.value : "";

        const opts = buildStartTimeOptions(open, close, slotMinutes, dur);

        // ✅ 기존 start 유지(가능하면), 불가능하면 가장 가까운 값으로 선택
        setSelectOptions(sel, opts, prevStart);
      });


      document.getElementById("m_save").addEventListener("click", async () => {
        try {
          const userId = parseInt(document.getElementById("m_userId").value, 10) || 0;
          const d = String(document.getElementById("m_date").value || "").trim();
          const st = String(document.getElementById("m_start").value || "").trim();
          const dur = parseInt(document.getElementById("m_duration").value, 10) || 0;
          const title = String(document.getElementById("m_title").value || "").trim();

          if (!userId) throw new Error("유저를 선택하세요.");
          if (!d) throw new Error("예약일이 필요합니다.");
          if (!st) throw new Error("시작시간이 필요합니다.");
          if (!dur) throw new Error("예약시간이 올바르지 않습니다.");

          await callCreate(userId, d, st, dur, title);
          closeOverlay();
          await loadDayStatus();
          showModal("예약이 생성되었습니다.", "info");
        } catch (e) {
          showModal("생성 실패: " + (e.message || e), "error");
        }
      });
    }

    function openUpdateModalFromRow(reservationId, tr) {
      if (!lastDayStatus) {
        showModal("먼저 일자 현황을 조회해주세요.", "warning");
        return;
      }
      if (!tr) {
        showModal("예약 정보를 찾을 수 없습니다.", "error");
        return;
      }

      const tds = tr.querySelectorAll("td");
      const title = tds && tds[1] ? String(tds[1].textContent || "").trim() : "";
      const startText = tds && tds[2] ? String(tds[2].textContent || "").trim() : ""; // yyyy-MM-dd HH:mm
      const endText = tds && tds[3] ? String(tds[3].textContent || "").trim() : "";

      const parsed = parseStartEndToModalValues(startText, endText);
      if (!parsed) {
        showModal("시간 파싱 실패: " + startText + " ~ " + endText, "error");
        return;
      }

      const date = parsed.date;
      const startTime = parsed.startTime;
      const durationMinutes = parsed.durationMinutes;

      const open = String(lastDayStatus.open || "").trim();
      const close = String(lastDayStatus.close || "").trim();
      const slotMinutes = parseInt(String(lastDayStatus.slotMinutes || "60"), 10) || 60;

      const durationOptions = buildDurationOptions(slotMinutes);
      const startOptions = buildStartTimeOptions(open, close, slotMinutes, durationMinutes);

      const html =
        "<h3 style='margin:0 0 12px 0;'>예약 수정</h3>"
        + "<div class='m-form'>"
        + "  <label>예약일</label>"
        + "  <input id='m_date' type='date' value='" + escapeHtml(date) + "' />"
        + "  <label>시작시간</label>"
        + "  <select id='m_start'>" + startOptions.map(t => "<option value='" + escapeHtml(t) + "'" + (t === startTime ? " selected" : "") + ">" + escapeHtml(t) + "</option>").join("") + "</select>"
        + "  <label>예약시간</label>"
        + "  <select id='m_duration'>" + durationOptions.map(m => "<option value='" + m + "'" + (m === durationMinutes ? " selected" : "") + ">" + m + "분</option>").join("") + "</select>"
        + "  <label>제목(선택)</label>"
        + "  <input id='m_title' type='text' value='" + escapeHtml(title) + "' />"
        + "</div>"
        + "<div class='m-actions'>"
        + "  <button type='button' class='btn btn-secondary' id='m_close'>닫기</button>"
        + "  <button type='button' class='btn btn-primary' id='m_save'>저장</button>"
        + "</div>";

      openOverlay(html);

      document.getElementById("m_close").addEventListener("click", closeOverlay);

      // duration 변경 시 시작시간 옵션 재생성
      document.getElementById("m_duration").addEventListener("change", () => {
        const dur = parseInt(document.getElementById("m_duration").value, 10) || slotMinutes;

        const sel = document.getElementById("m_start");
        const prevStart = sel ? sel.value : "";

        const opts = buildStartTimeOptions(open, close, slotMinutes, dur);

        setSelectOptions(sel, opts, prevStart);
      });


      document.getElementById("m_save").addEventListener("click", async () => {
        try {
          const d = String(document.getElementById("m_date").value || "").trim();
          const st = String(document.getElementById("m_start").value || "").trim();
          const dur = parseInt(document.getElementById("m_duration").value, 10) || 0;
          const newTitle = String(document.getElementById("m_title").value || "").trim();

          if (!d) throw new Error("예약일이 필요합니다.");
          if (!st) throw new Error("시작시간이 필요합니다.");
          if (!dur) throw new Error("예약시간이 올바르지 않습니다.");

          await callUpdate(reservationId, d, st, dur, newTitle);
          closeOverlay();
          await loadDayStatus();
          showModal("수정되었습니다.", "info");
        } catch (e) {
          showModal("수정 실패: " + (e.message || e), "error");
        }
      });
    }

    // =========================================================
    // ✅ API calls
    // =========================================================
    async function callCreate(userId, date, startTime, durationMinutes, title) {
      const body = new URLSearchParams({
        roomId: String(currentRoomId),
        userId: String(userId),
        date: String(date),
        startTime: String(startTime),
        durationMinutes: String(durationMinutes),
        title: String(title || "")
      });

      return fetchJson(API_CREATE, {
        method: "POST",
        headers: { "Content-Type": "application/x-www-form-urlencoded; charset=UTF-8" },
        credentials: "same-origin",
        body: body.toString(),
        throwOnOkFalse: true
      });
    }

    async function callUpdate(id, date, startTime, durationMinutes, title) {
      const body = new URLSearchParams({
        id: String(id),
        roomId: String(currentRoomId),
        date: String(date),
        startTime: String(startTime),
        durationMinutes: String(durationMinutes),
        title: String(title || "")
      });

      return fetchJson(API_UPDATE, {
        method: "POST",
        headers: { "Content-Type": "application/x-www-form-urlencoded; charset=UTF-8" },
        credentials: "same-origin",
        body: body.toString(),
        throwOnOkFalse: true
      });
    }

    async function callCancel(id) {
      const body = new URLSearchParams({ id: String(id) });

      return fetchJson(API_CANCEL, {
        method: "POST",
        headers: { "Content-Type": "application/x-www-form-urlencoded; charset=UTF-8" },
        credentials: "same-origin",
        body: body.toString(),
        throwOnOkFalse: true
      });
    }

    async function fetchUserOptions(q) {
      const body = new URLSearchParams({ q: String(q || "") });

      const json = await fetchJson(API_USER_OPTIONS, {
        method: "POST",
        headers: { "Content-Type": "application/x-www-form-urlencoded; charset=UTF-8" },
        credentials: "same-origin",
        body: body.toString(),
        throwOnOkFalse: true
      });

      if (!json || json.ok !== true || !Array.isArray(json.data)) {
        throw new Error((json && json.message) ? json.message : "유저 목록을 불러오지 못했습니다.");
      }
      return json.data;
    }

    // =========================================================
    // ✅ option builders
    // =========================================================
    function buildDurationOptions(slotMinutes) {
      // ✅ MVP: 60~240, slot 단위 (서버가 최종 검증)
      const list = [];
      const min = Math.max(30, slotMinutes);
      const max = 240;

      for (let m = min; m <= max; m += slotMinutes) {
        list.push(m);
      }
      if (list.length === 0) list.push(slotMinutes);
      return list;
    }

    function buildStartTimeOptions(open, close, slotMinutes, durationMinutes) {
      const openMin = parseHHMMToMinutes(open);
      const closeMin = parseHHMMToMinutes(close);
      const dur = Math.max(1, parseInt(String(durationMinutes), 10) || slotMinutes);

      const list = [];
      for (let t = openMin; t + dur <= closeMin; t += slotMinutes) {
        list.push(minutesToHHMM(t));
      }
      return list;
    }

    function parseStartEndToModalValues(startText, endText) {
      // startText/endText: "yyyy-MM-dd HH:mm"
      const s = String(startText || "").trim();
      const e = String(endText || "").trim();
      const sm = s.match(/^(\d{4}-\d{2}-\d{2})\s+(\d{2}:\d{2})$/);
      const em = e.match(/^(\d{4}-\d{2}-\d{2})\s+(\d{2}:\d{2})$/);
      if (!sm || !em) return null;

      const date = sm[1];
      const st = sm[2];
      const et = em[2];

      const a = parseHHMMToMinutes(st);
      const b = parseHHMMToMinutes(et);
      if (a == null || b == null || b <= a) return null;

      return { date: date, startTime: st, durationMinutes: (b - a) };
    }


})();
