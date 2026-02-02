/**
 * user/reservation/new.js
 * - 예약 생성 화면
 * - room 정책/운영시간을 서버가 최종 검증하므로, 프론트는 UX용으로 선택지 구성
 *
 * 전제:
 * - common.js: fetchJson / showModal / escapeHtml
 * - window.__MEETING__.ctx 존재
 */
(function () {
  "use strict";

  const ctx = (window.__MEETING__ && window.__MEETING__.ctx) ? window.__MEETING__.ctx : "";

  const API_ROOM_DETAIL = ctx + "/user/rooms/detail-data";
  const API_CREATE = ctx + "/user/reservations/create";

  const elRoomId = document.getElementById("roomId");
  const elRoomInfo = document.getElementById("roomInfo");
  const elDate = document.getElementById("dateInput");
  const elTime = document.getElementById("timeSelect");
  const elDuration = document.getElementById("durationSelect");
  const elTitle = document.getElementById("titleInput");

  const btnBack = document.getElementById("btnBack");
  const btnCreate = document.getElementById("btnCreate");

  let roomDetail = null;

  document.addEventListener("DOMContentLoaded", async () => {
    if (typeof fetchJson !== "function") return alert("fetchJson 없음(common.js 확인)");
    if (typeof showModal !== "function") return alert("showModal 없음(common.js 확인)");
    if (typeof escapeHtml !== "function") return alert("escapeHtml 없음(common.js 확인)");

    btnBack.addEventListener("click", () => history.back());
    btnCreate.addEventListener("click", create);

    const roomId = parseInt((elRoomId.value || "0"), 10);
    if (!roomId) {
      showModal("회의실 ID가 없습니다. 목록에서 다시 진입하세요.", "error");
      return;
    }

    // 기본 날짜: 오늘
    elDate.value = toDateInputValue(new Date());
    elDate.addEventListener("change", () => rebuildTimeOptions());

    await loadRoomDetail(roomId);

    // 운영시간/slot 기반으로 시간/기간 옵션 구성
    rebuildDurationOptions();
    rebuildTimeOptions();
  });

  async function loadRoomDetail(roomId) {
    try {
      const body = new URLSearchParams({ id: String(roomId) });

      const json = await fetchJson(API_ROOM_DETAIL, {
        method: "POST",
        headers: { "Content-Type": "application/x-www-form-urlencoded" },
        credentials: "same-origin",
        body: body.toString(),
        throwOnOkFalse: true
      });

      roomDetail = json && json.data ? json.data : null;
      if (!roomDetail) throw new Error("회의실 상세 데이터가 없습니다.");

      const name = roomDetail.name || "-";
      const loc = roomDetail.location ? (" / " + roomDetail.location) : "";
      elRoomInfo.innerHTML = "<strong>" + escapeHtml(name + loc) + "</strong>";

    } catch (e) {
      showModal("회의실 정보를 불러오지 못했습니다: " + (e.message || e), "error");
    }
  }

  function rebuildDurationOptions() {
    elDuration.innerHTML = "";

    // roomDetail에 min/max/slot이 있다고 가정(없으면 기본값)
    const slot = parseInt(roomDetail && roomDetail.slotMinutes ? roomDetail.slotMinutes : 60, 10);
    const min = parseInt(roomDetail && roomDetail.minMinutes ? roomDetail.minMinutes : 60, 10);
    const max = parseInt(roomDetail && roomDetail.maxMinutes ? roomDetail.maxMinutes : 240, 10);

    for (let m = min; m <= max; m += slot) {
      const opt = document.createElement("option");
      opt.value = String(m);
      opt.textContent = m + "분";
      elDuration.appendChild(opt);
    }
  }

  function rebuildTimeOptions() {
    elTime.innerHTML = "";

    // 운영시간은 "선택한 날짜" 기준이지만,
    // 프론트에서 예외를 다 반영하려면 추가 API가 필요하므로(딥해짐),
    // 여기서는 기본 운영시간(요일별)만 기준으로 옵션을 잡고,
    // 최종 검증은 create API에서 실패 메시지를 받는 구조로 간다.
    const dateStr = (elDate.value || "").trim();
    if (!dateStr) return;

    const dow = getDow1MonTo7Sun(dateStr); // 1=월..7=일 (스키마 규약)
    const hours = pickHoursByDow(dow);

    if (!hours || hours.closed) {
      const opt = document.createElement("option");
      opt.value = "";
      opt.textContent = "휴무";
      elTime.appendChild(opt);
      elTime.disabled = true;
      return;
    }

    elTime.disabled = false;

    const slot = parseInt(roomDetail && roomDetail.slotMinutes ? roomDetail.slotMinutes : 60, 10);
    const openMin = toMinutes(hours.open);
    const closeMin = toMinutes(hours.close);

    // 시간 옵션: open ~ close-slot 까지 (start+slot <= close 여야 의미 있음)
    for (let t = openMin; t + slot <= closeMin; t += slot) {
      const opt = document.createElement("option");
      opt.value = minutesToHHMM(t);
      opt.textContent = minutesToHHMM(t);
      elTime.appendChild(opt);
    }

    if (elTime.options.length === 0) {
      const opt = document.createElement("option");
      opt.value = "";
      opt.textContent = "예약 가능한 시간이 없음";
      elTime.appendChild(opt);
      elTime.disabled = true;
    }
  }

  function pickHoursByDow(dow) {
    // roomDetail.operatingHours: [{dow, closed, openTime, closeTime}] 형태를 기대
    const list = (roomDetail && Array.isArray(roomDetail.operatingHours)) ? roomDetail.operatingHours : [];

    const found = list.find((x) => String(x.dow) === String(dow));
    if (!found) return { closed: true, open: null, close: null };

    // 필드명 흡수
    const closed = (found.closed === true) || (found.isClosed === true);
    const open = String(found.openTime || found.open || "").trim();
    const close = String(found.closeTime || found.close || "").trim();

    return { closed, open, close };
  }

  async function create() {
    try {
      const roomId = parseInt((elRoomId.value || "0"), 10);
      const dateStr = (elDate.value || "").trim();
      const timeStr = (elTime.value || "").trim();
      const durationMinutes = parseInt((elDuration.value || "0"), 10);

      if (!roomId) return showModal("회의실 ID가 올바르지 않습니다.", "warning");
      if (!dateStr) return showModal("예약일을 선택하세요.", "warning");
      if (!timeStr) return showModal("시작 시간을 선택하세요.", "warning");
      if (!durationMinutes) return showModal("예약 시간을 선택하세요.", "warning");

      const startAt = dateStr + " " + timeStr;

      const body = new URLSearchParams({
        roomId: String(roomId),
        startAt: startAt,
        durationMinutes: String(durationMinutes),
        title: (elTitle.value || "").trim()
      });

      const json = await fetchJson(API_CREATE, {
        method: "POST",
        headers: { "Content-Type": "application/x-www-form-urlencoded" },
        credentials: "same-origin",
        body: body.toString(),
        throwOnOkFalse: true
      });

      showModal("예약이 완료되었습니다.", "success");

      // 완료 후 내 예약으로 이동
      window.location.href = ctx + "/user/reservations";

    } catch (e) {
      showModal("예약 실패: " + (e.message || e), "error");
    }
  }

  // ===== util =====

  function toDateInputValue(d) {
    const y = d.getFullYear();
    const m = String(d.getMonth() + 1).padStart(2, "0");
    const dd = String(d.getDate()).padStart(2, "0");
    return y + "-" + m + "-" + dd;
  }

  // dateStr(YYYY-MM-DD) -> 1=월..7=일
  function getDow1MonTo7Sun(dateStr) {
    const d = new Date(dateStr + "T00:00:00");
    const js = d.getDay(); // 0=일..6=토
    if (js === 0) return 7; // 일요일 -> 7
    return js;              // 월(1)~토(6)
  }

  function toMinutes(hhmm) {
    const m = String(hhmm || "").match(/^(\d{2}):(\d{2})$/);
    if (!m) return 0;
    return parseInt(m[1], 10) * 60 + parseInt(m[2], 10);
  }

  function minutesToHHMM(min) {
    const h = String(Math.floor(min / 60)).padStart(2, "0");
    const m = String(min % 60).padStart(2, "0");
    return h + ":" + m;
  }
})();
