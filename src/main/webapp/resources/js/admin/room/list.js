/**
 * admin/room/list.js
 * - 회의실관리 목록/검색/페이징 + 관리(수정/상세) + 생성/수정 모달
 *
 * ✅ 서버 파라미터 규약(중요)
 * - isActive: "1"이면 active=true
 * - 운영시간: dow1~dow7 고정 파라미터로 받음
 *   - dowN_closed = "1" (휴무)
 *   - 휴무가 아니면 dowN_open / dowN_close 필수
 *
 * 전제:
 * - common.js에 fetchJson / showModal / escapeHtml / renderPagination 존재
 * - JSP에서 window.__MEETING__.ctx 제공
 */
(function () {
  "use strict";

  const ctx = (window.__MEETING__ && window.__MEETING__.ctx) ? window.__MEETING__.ctx : "";

  const API_LIST   = ctx + "/admin/rooms";
  const API_DETAIL = ctx + "/admin/rooms/detail"; // ✅ 추가: 운영시간 포함 상세
  const API_CREATE = ctx + "/admin/rooms/create";
  const API_UPDATE = ctx + "/admin/rooms/update";
  const API_DELETE = ctx + "/admin/rooms/delete";

  const DEFAULTS = {
    slotMinutes: 60,
    minMinutes: 60,
    maxMinutes: 240,
    bufferMinutes: 0,
    bookingOpenDaysAhead: 30
  };

  /**
   * ✅ 요일 라벨 매핑
   * - 현재는 1=월, 7=일 가정
   * - 만약 DB가 1=일, 7=토 규약이면 아래 "일요일 시작" 블록으로 교체
   */
  const DOWS = [
    { dow: 1, label: "월" },
    { dow: 2, label: "화" },
    { dow: 3, label: "수" },
    { dow: 4, label: "목" },
    { dow: 5, label: "금" },
    { dow: 6, label: "토" },
    { dow: 7, label: "일" }
  ];

  /**
  // ✅ 일요일 시작(예: MySQL DAYOFWEEK 기반)일 경우 이걸로 교체
  const DOWS = [
    { dow: 1, label: "일" },
    { dow: 2, label: "월" },
    { dow: 3, label: "화" },
    { dow: 4, label: "수" },
    { dow: 5, label: "목" },
    { dow: 6, label: "금" },
    { dow: 7, label: "토" }
  ];
  */

  const state = { page: 1, size: 10, q: "" };
  let allRooms = [];

  // DOM (JSP id와 맞춰야 함)
  const tbody = document.getElementById("roomTableBody");
  const totalEl = document.getElementById("totalElements");
  const inputQ = document.getElementById("searchRoom");
  const btnSearch = document.getElementById("btnSearchRoom");
  const selectSize = document.getElementById("pageSize");
  const btnCreate = document.getElementById("btnCreateRoom");
  const btnDeleteSelected = document.getElementById("btnDeleteSelectedRoom");
  const chkAll = document.getElementById("chkAll");

  document.addEventListener("DOMContentLoaded", () => {
    if (typeof fetchJson !== "function") {
      alert("fetchJson이 없습니다. resources/js/common.js에 fetchJson을 추가하세요.");
      return;
    }
    if (typeof escapeHtml !== "function") {
      alert("escapeHtml이 없습니다. resources/js/common.js에 escapeHtml을 추가하세요.");
      return;
    }
    if (typeof showModal !== "function") {
      alert("showModal이 없습니다. resources/js/common.js에 showModal을 추가하세요.");
      return;
    }

    loadRooms();

    // 검색
    btnSearch.addEventListener("click", () => {
      state.q = (inputQ.value || "").trim();
      state.page = 1;
      loadRooms();
    });

    inputQ.addEventListener("keydown", (e) => {
      if (e.key === "Enter") {
        e.preventDefault();
        state.q = (inputQ.value || "").trim();
        state.page = 1;
        loadRooms();
      }
    });

    // size 변경
    selectSize.addEventListener("change", () => {
      state.size = parseInt(selectSize.value, 10) || 10;
      state.page = 1;
      loadRooms();
    });

    // 생성/삭제
    btnCreate.addEventListener("click", openRoomCreateModal);
    btnDeleteSelected.addEventListener("click", deleteSelectedRooms);

    // 전체 체크
    chkAll.addEventListener("change", () => {
      const checked = chkAll.checked;
      document.querySelectorAll(".room-chk").forEach((chk) => {
        if (chk.disabled) return;
        chk.checked = checked;
      });
    });
  });

  // =========================================================
  // 목록 로드
  // =========================================================
  async function loadRooms() {
    tbody.innerHTML = "<tr><td colspan='7' class='text-center text-muted'>데이터를 불러오는 중...</td></tr>";
    chkAll.checked = false;

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

      allRooms = (json && json.data && Array.isArray(json.data.items)) ? json.data.items : [];

      const total = (json && json.page && json.page.total != null) ? json.page.total : allRooms.length;
      totalEl.textContent = String(total);

      renderRooms(allRooms);

      if (typeof renderPagination === "function") {
        const pageForUi = normalizePageForRenderPagination(json && json.page ? json.page : null);
        renderPagination("#pagination", pageForUi, (newPage) => {
          state.page = newPage;
          loadRooms();
        });
      }

    } catch (e) {
      allRooms = [];
      totalEl.textContent = "0";
      tbody.innerHTML = "<tr><td colspan='7' class='text-center text-muted'>데이터 로드 실패</td></tr>";
      showModal("회의실 데이터를 불러오는 중 오류가 발생했습니다. (" + (e.message || e) + ")", "error");
    }
  }

  function normalizePageForRenderPagination(p) {
    const page = p && p.page != null ? parseInt(p.page, 10) : 1;
    const size = p && p.size != null ? parseInt(p.size, 10) : state.size;
    const totalPages = p && p.totalPages != null ? parseInt(p.totalPages, 10) : 1;
    const totalElements = p && p.total != null ? parseInt(p.total, 10) : 0;
    return { page, size, totalPages, totalElements };
  }

  // =========================================================
  // 테이블 렌더링
  // =========================================================
  function renderRooms(rows) {
    if (!rows || rows.length === 0) {
      tbody.innerHTML = "<tr><td colspan='7' class='text-center text-muted'>회의실이 없습니다.</td></tr>";
      return;
    }

    tbody.innerHTML = rows.map((r) => {
      const id = r.id ?? 0;
      const name = escapeHtml(r.name || "-");
      const location = escapeHtml(r.location || "-");
      const capacity = escapeHtml(String(r.capacity ?? "-"));
      const activeText = (r.active === true) ? "사용" : "중지";
      const updatedAtView = escapeHtml(formatDateNoSeconds(r.updatedAt || "-"));

      return ""
        + "<tr>"
        +   "<td style='text-align:center;'>"
        +     "<input type='checkbox' class='room-chk' value='" + escapeHtml(String(id)) + "'>"
        +   "</td>"
        +   "<td>" + name + "</td>"
        +   "<td>" + capacity + "</td>"
        +   "<td>" + location + "</td>"
        +   "<td>" + escapeHtml(activeText) + "</td>"
        +   "<td>" + updatedAtView + "</td>"
        +   "<td style='display:flex; gap:6px; justify-content:flex-start;'>"
        +     "<button type='button' class='btn btn-secondary' data-edit-id='" + escapeHtml(String(id)) + "'>수정</button>"
        +     "<button type='button' class='btn btn-primary' data-detail-id='" + escapeHtml(String(id)) + "'>상세</button>"
        +   "</td>"
        + "</tr>";
    }).join("");

    tbody.querySelectorAll("[data-edit-id]").forEach((btn) => {
      btn.addEventListener("click", () => {
        const id = btn.getAttribute("data-edit-id");
        openRoomEditModal(id);
      });
    });

    tbody.querySelectorAll("[data-detail-id]").forEach((btn) => {
      btn.addEventListener("click", () => openRoomReservations(btn.getAttribute("data-detail-id")));
    });

    document.querySelectorAll(".room-chk").forEach((chk) => {
      chk.addEventListener("change", syncChkAll);
    });
  }

  function syncChkAll() {
    const chks = Array.from(document.querySelectorAll(".room-chk")).filter(x => !x.disabled);
    chkAll.checked = (chks.length > 0) && chks.every(x => x.checked);
  }

  // =========================================================
  // 선택 삭제
  // =========================================================
  async function deleteSelectedRooms() {
    const ids = Array.from(document.querySelectorAll(".room-chk"))
      .filter(x => !x.disabled && x.checked)
      .map(x => x.value);

    if (ids.length === 0) {
      showModal("삭제할 회의실을 선택하세요.", "warning");
      return;
    }

    if (!confirm("선택한 회의실을 삭제하시겠습니까?")) return;

    try {
      const body = new URLSearchParams({ ids: ids.join(",") });

      const json = await fetchJson(API_DELETE, {
        method: "POST",
        headers: { "Content-Type": "application/x-www-form-urlencoded" },
        credentials: "same-origin",
        body: body.toString(),
        throwOnOkFalse: true
      });

      showModal("삭제 완료: " + (json.data || 0) + "개", "success");
      loadRooms();

    } catch (e) {
      showModal("삭제 실패: " + (e.message || e), "error");
    }
  }

  // =========================================================
  // 상세(예약 현황) - 다음 단계
  // =========================================================
  function openRoomReservations(roomId) {
    const r = allRooms.find(x => String(x.id) === String(roomId));
    const title = r ? (r.name + " 예약 현황") : ("회의실(" + roomId + ") 예약 현황");

    showModal(
      "<div><strong>" + escapeHtml(title) + "</strong></div>"
      + "<div class='text-muted' style='margin-top:8px;'>"
      + "다음 단계: roomId로 예약 목록 조회 API를 연결하면 됩니다."
      + "</div>",
      "info"
    );
  }

  // =========================================================
  // 생성/수정 모달
  // =========================================================
  function openRoomCreateModal() {
    openRoomModal({
      mode: "create",
      room: {
        id: 0,
        name: "",
        location: "",
        capacity: 1,
        isActive: true,
        slotMinutes: DEFAULTS.slotMinutes,
        bufferMinutes: DEFAULTS.bufferMinutes,
        operatingHours: buildDefaultOperatingHoursByDow()
      }
    });
  }

  /**
   * ✅ 핵심 수정: 수정 모달 열 때 DB 운영시간을 포함한 "상세"를 먼저 받아온다.
   * - 목록 API에는 operatingHours가 없어서 기본값으로 보이던 문제 해결
   */
  async function openRoomEditModal(roomId) {
    const id = parseInt(String(roomId || "0"), 10);
    if (Number.isNaN(id) || id <= 0) {
      showModal("회의실 ID가 올바르지 않습니다.", "warning");
      return;
    }

    try {
      const detail = await fetchRoomDetail(id); // ✅ 운영시간 포함
      const room = convertDetailToModalRoom(detail);
      openRoomModal({ mode: "edit", room });

    } catch (e) {
      showModal("회의실 상세 조회 실패: " + (e.message || e), "error");
    }
  }

  /**
   * /admin/rooms/detail
   * - 응답은 { ok:true, data: RoomDetail } 형태로 가정
   * - RoomDetail 안에 operatingHours(List<RoomOperatingHour>) 포함
   */
  async function fetchRoomDetail(id) {
    const body = new URLSearchParams({ id: String(id) });

    const json = await fetchJson(API_DETAIL, {
      method: "POST",
      headers: { "Content-Type": "application/x-www-form-urlencoded" },
      credentials: "same-origin",
      body: body.toString(),
      throwOnOkFalse: true
    });

    if (!json || json.ok !== true) {
      throw new Error((json && json.message) ? json.message : "상세 조회 실패");
    }
    if (!json.data) {
      throw new Error("상세 데이터(data)가 없습니다.");
    }
    return json.data;
  }

  /**
   * RoomDetail(JSON)를 모달에서 쓰는 형태로 변환
   * - closed=true(휴무) → enabled=false
   * - open/close 필드는 프로젝트마다 이름이 다를 수 있어서 최대한 흡수
   */
  function convertDetailToModalRoom(detail) {
    const room = {
      id: detail.id ?? 0,
      name: detail.name || "",
      location: detail.location || "",
      capacity: (detail.capacity != null) ? detail.capacity : 1,
      isActive: (detail.active === true), // 서버에서 active로 내려오면 이걸 사용
      slotMinutes: (detail.slotMinutes != null) ? detail.slotMinutes : DEFAULTS.slotMinutes,
      bufferMinutes: (detail.bufferMinutes != null) ? detail.bufferMinutes : DEFAULTS.bufferMinutes,
      operatingHours: buildDefaultOperatingHoursByDow() // 기본틀 먼저 만들고, 아래에서 덮어씀
    };

    const list = Array.isArray(detail.operatingHours) ? detail.operatingHours : [];
    if (list.length > 0) {
      room.operatingHours = operatingHoursListToMap(list);
    }

    return room;
  }

  /**
   * List<RoomOperatingHour> → { "1": {enabled,open,close}, ... } 형태로 변환
   */
  function operatingHoursListToMap(list) {
    const map = {};

    // 일단 전부 휴무로 초기화(누락 대비)
    for (let dow = 1; dow <= 7; dow++) {
      map[String(dow)] = { enabled: false, open: "", close: "" };
    }

    list.forEach((it) => {
      const dow = parseInt(String(it.dow ?? it.dayOfWeek ?? "0"), 10);
      if (!dow || dow < 1 || dow > 7) return;

      const closed = (it.closed === true) || (it.isClosed === true);

      // 필드명 흔들림 흡수
      const open = String(it.open ?? it.openTime ?? it.openAt ?? "").trim();
      const close = String(it.close ?? it.closeTime ?? it.closeAt ?? "").trim();

      if (closed) {
        map[String(dow)] = { enabled: false, open: "", close: "" };
      } else {
        map[String(dow)] = { enabled: true, open, close };
      }
    });

    return map;
  }

  function openRoomModal(opt) {
    const isCreate = opt.mode === "create";
    const r = opt.room;

    const overlay = document.createElement("div");
    overlay.className = "modal-overlay";

    const ohRowsHtml = DOWS.map(d => {
      const row = r.operatingHours[String(d.dow)] || null;
      const enabled = row ? (row.enabled === true) : false;
      const open = row && row.open ? row.open : "";
      const close = row && row.close ? row.close : "";

      return ""
        + "<tr>"
        +   "<td style='width:80px; font-weight:700;'>" + escapeHtml(d.label) + "</td>"
        +   "<td style='width:70px; text-align:center;'>"
        +     "<input type='checkbox' id='dow" + d.dow + "_enabled' " + (enabled ? "checked" : "") + ">"
        +   "</td>"
        +   "<td>"
        +     "<input type='time' id='dow" + d.dow + "_open' value='" + escapeHtml(open) + "'"
        +            " style='height:40px; width:140px; padding:0 10px; border:1px solid #ddd; border-radius:8px;'>"
        +   "</td>"
        +   "<td>"
        +     "<input type='time' id='dow" + d.dow + "_close' value='" + escapeHtml(close) + "'"
        +            " style='height:40px; width:140px; padding:0 10px; border:1px solid #ddd; border-radius:8px;'>"
        +   "</td>"
        + "</tr>";
    }).join("");

    overlay.innerHTML =
      "<div class='modal modal-detail'>"
      + "<div class='modal-header info'>"
      +   "<div class='modal-icon info'>i</div>"
      +   "<div class='modal-title'>" + (isCreate ? "회의실 생성" : "회의실 수정") + "</div>"
      +   "<button class='modal-close' type='button'>&times;</button>"
      + "</div>"
      + "<div class='modal-body'>"

      + "<div class='detail-section'>"
      +   "<div class='detail-section-title'>기본 정보</div>"
      +   (!isCreate
          ? ("<div class='form-group'><label>ID</label>"
            + "<input type='text' value='" + escapeHtml(String(r.id || 0)) + "' readonly>"
            + "</div>")
          : "")
      +   "<div class='form-group'><label>회의실명</label>"
      +     "<input type='text' id='roomInputName' value='" + escapeHtml(r.name || "") + "' placeholder='예: A 회의실'>"
      +   "</div>"
      +   "<div class='form-group'><label>위치</label>"
      +     "<input type='text' id='roomInputLocation' value='" + escapeHtml(r.location || "") + "' placeholder='예: 3층'>"
      +   "</div>"
      +   "<div class='form-group'><label>수용인원</label>"
      +     "<input type='number' id='roomInputCapacity' min='1' value='" + escapeHtml(String(r.capacity ?? 1)) + "'>"
      +   "</div>"
      +   "<div class='form-group'><label>사용여부</label>"
      +     "<select id='roomInputIsActive' style='height:40px; width:100%; padding:0 10px; border:1px solid #ddd; border-radius:8px;'>"
      +       "<option value='1' " + (r.isActive === true ? "selected" : "") + ">사용</option>"
      +       "<option value='0' " + (r.isActive !== true ? "selected" : "") + ">중지</option>"
      +     "</select>"
      +   "</div>"
      + "</div>"

      + "<div class='detail-section'>"
      +   "<div class='detail-section-title'>예약 슬롯 설정</div>"
      +   "<div class='form-group'><label>슬롯 단위(분)</label>"
      +     "<input type='number' id='roomInputSlotMinutes' min='5' value='" + escapeHtml(String(r.slotMinutes ?? DEFAULTS.slotMinutes)) + "'>"
      +   "</div>"
      +   "<div class='form-group'><label>버퍼(분)</label>"
      +     "<input type='number' id='roomInputBufferMinutes' min='0' value='" + escapeHtml(String(r.bufferMinutes ?? DEFAULTS.bufferMinutes)) + "'>"
      +   "</div>"
      + "</div>"

      + "<div class='detail-section'>"
      +   "<div class='detail-section-title'>운영시간</div>"
      +   "<div class='table-wrap'>"
      +     "<table style='width:100%;'>"
      +       "<thead><tr><th style='width:80px;'>요일</th><th style='width:70px; text-align:center;'></th><th>오픈</th><th>마감</th></tr></thead>"
      +       "<tbody>" + ohRowsHtml + "</tbody>"
      +     "</table>"
      +   "</div>"
      + "</div>"

      + "</div>"
      + "<div class='modal-footer'>"
      +   "<button class='btn btn-secondary' type='button' data-close>취소</button>"
      +   "<button class='btn btn-primary' type='button' data-save>저장</button>"
      + "</div>"
      + "</div>";

    document.body.appendChild(overlay);

    // 닫기
    overlay.querySelector(".modal-close").onclick = () => overlay.remove();
    overlay.querySelector("[data-close]").onclick = () => overlay.remove();
    overlay.onclick = (e) => { if (e.target === overlay) overlay.remove(); };

    // 운영 체크 토글: OFF면 input 비활성 + 값 제거
    for (let dow = 1; dow <= 7; dow++) {
      const chk = overlay.querySelector("#dow" + dow + "_enabled");
      const openEl = overlay.querySelector("#dow" + dow + "_open");
      const closeEl = overlay.querySelector("#dow" + dow + "_close");

      applyOperatingRowState(chk, openEl, closeEl);

      chk.addEventListener("change", () => {
        applyOperatingRowState(chk, openEl, closeEl);
        if (chk.checked !== true) {
          openEl.value = "";
          closeEl.value = "";
        }
      });
    }

    // 저장
    overlay.querySelector("[data-save]").onclick = async () => {
      try {
        const name = (overlay.querySelector("#roomInputName").value || "").trim();
        const location = (overlay.querySelector("#roomInputLocation").value || "").trim();

        const capacity = parseInt((overlay.querySelector("#roomInputCapacity").value || "").trim(), 10);
        const isActive = (overlay.querySelector("#roomInputIsActive").value || "0").trim();
        const slotMinutes = parseInt((overlay.querySelector("#roomInputSlotMinutes").value || "").trim(), 10);
        const bufferMinutes = parseInt((overlay.querySelector("#roomInputBufferMinutes").value || "").trim(), 10);

        if (!name) {
          showModal("회의실명을 입력하세요.", "warning");
          return;
        }
        if (Number.isNaN(capacity) || capacity < 1) {
          showModal("수용인원은 1 이상 숫자여야 합니다.", "warning");
          return;
        }
        if (Number.isNaN(slotMinutes) || slotMinutes < 5) {
          showModal("슬롯 단위(분)는 5 이상 숫자여야 합니다.", "warning");
          return;
        }
        if (Number.isNaN(bufferMinutes) || bufferMinutes < 0) {
          showModal("버퍼(분)는 0 이상 숫자여야 합니다.", "warning");
          return;
        }

        const dowParams = buildDowParamsFromModal(overlay);

        if (isCreate) {
          const createdId = await createRoom({
            name, location, capacity, isActive, slotMinutes, bufferMinutes,
            dowParams
          });

          overlay.remove();
          showModal("회의실이 생성되었습니다. (ID: " + createdId + ")", "success");
          loadRooms();
          return;
        }

        const id = parseInt(String(r.id || "0"), 10);
        if (Number.isNaN(id) || id <= 0) {
          showModal("수정할 회의실 ID가 올바르지 않습니다.", "error");
          return;
        }

        await updateRoom({
          id, name, location, capacity, isActive, slotMinutes, bufferMinutes,
          dowParams
        });

        overlay.remove();
        showModal("저장되었습니다.", "success");
        loadRooms();

      } catch (e) {
        showModal("처리 실패: " + (e.message || e), "error");
      }
    };

    setTimeout(() => overlay.classList.add("show"), 10);
  }

  function applyOperatingRowState(chk, openEl, closeEl) {
    const enabled = (chk && chk.checked === true);

    if (openEl) openEl.disabled = !enabled;
    if (closeEl) closeEl.disabled = !enabled;

    if (openEl) openEl.style.opacity = enabled ? "1" : "0.6";
    if (closeEl) closeEl.style.opacity = enabled ? "1" : "0.6";
  }

  function buildDefaultOperatingHoursByDow() {
    const map = {};
    for (let dow = 1; dow <= 7; dow++) {
      const weekday = (dow >= 1 && dow <= 5);
      map[String(dow)] = {
        enabled: weekday,
        open: weekday ? "09:00" : "",
        close: weekday ? "18:00" : ""
      };
    }
    return map;
  }

  function buildDowParamsFromModal(overlay) {
    const params = new URLSearchParams();

    for (let dow = 1; dow <= 7; dow++) {
      const chk = overlay.querySelector("#dow" + dow + "_enabled");
      const openEl = overlay.querySelector("#dow" + dow + "_open");
      const closeEl = overlay.querySelector("#dow" + dow + "_close");

      const enabled = chk && chk.checked === true;

      // 값 읽고 정규화 (H:mm / HH:mm / HH:mm:ss 다 흡수)
      const openRaw = String(openEl ? openEl.value : "").trim();
      const closeRaw = String(closeEl ? closeEl.value : "").trim();

      if (!enabled) {
        params.append("dow" + dow + "_closed", "1");
        continue;
      }

      if (!openRaw || !closeRaw) {
        throw new Error("운영시간 입력이 누락되었습니다. (dow=" + dow + ")");
      }

      const open = normalizeTimeHHMM(openRaw);
      const close = normalizeTimeHHMM(closeRaw);

      const openMin = parseTimeToMinutes(open);
      const closeMin = parseTimeToMinutes(close);

      if (openMin === null || closeMin === null) {
        throw new Error("운영시간 형식이 올바르지 않습니다. (dow=" + dow + ")");
      }

      // ✅ 숫자 비교로 정확히 검증
      if (openMin >= closeMin) {
        throw new Error("운영시간이 올바르지 않습니다. (dow=" + dow + ")");
      }

      // 서버가 기대하는 파라미터
      params.append("dow" + dow + "_open", open);
      params.append("dow" + dow + "_close", close);
    }

    return params;
  }

  /**
   * time 문자열을 "HH:mm"로 정규화한다.
   * - "9:00" -> "09:00"
   * - "09:00:00" -> "09:00"
   */
  function normalizeTimeHHMM(t) {
    const s = String(t || "").trim();
    if (!s) return "";

    // HH:mm:ss → HH:mm 로 잘라냄
    const parts = s.split(":");
    if (parts.length < 2) return s;

    const h = String(parseInt(parts[0], 10));
    const m = String(parseInt(parts[1], 10));

    if (Number.isNaN(Number(h)) || Number.isNaN(Number(m))) return s;

    const hh = h.padStart(2, "0");
    const mm = m.padStart(2, "0");

    return `${hh}:${mm}`;
  }

  /**
   * "HH:mm" 또는 "H:mm"을 분(minute)으로 변환
   * - 반환: 0~1439
   * - 파싱 실패: null
   */
  function parseTimeToMinutes(t) {
    const s = String(t || "").trim();
    const m = s.match(/^(\d{1,2}):(\d{2})$/);
    if (!m) return null;

    const hh = parseInt(m[1], 10);
    const mm = parseInt(m[2], 10);

    if (Number.isNaN(hh) || Number.isNaN(mm)) return null;
    if (hh < 0 || hh > 23) return null;
    if (mm < 0 || mm > 59) return null;

    return hh * 60 + mm;
  }

  async function createRoom(payload) {
    const body = new URLSearchParams({
      name: payload.name || "",
      location: payload.location || "",
      capacity: String(payload.capacity ?? 1),
      isActive: String(payload.isActive || "0"),

      slotMinutes: String(payload.slotMinutes ?? DEFAULTS.slotMinutes),
      minMinutes: String(DEFAULTS.minMinutes),
      maxMinutes: String(DEFAULTS.maxMinutes),
      bufferMinutes: String(payload.bufferMinutes ?? DEFAULTS.bufferMinutes),
      bookingOpenDaysAhead: String(DEFAULTS.bookingOpenDaysAhead),

      availableStartDate: "",
      availableEndDate: ""
    });

    for (const [k, v] of payload.dowParams.entries()) {
      body.append(k, v);
    }

    const json = await fetchJson(API_CREATE, {
      method: "POST",
      headers: { "Content-Type": "application/x-www-form-urlencoded" },
      credentials: "same-origin",
      body: body.toString(),
      throwOnOkFalse: true
    });

    const createdId = json && json.data && json.data.id ? parseInt(json.data.id, 10) : 0;
    if (!createdId) throw new Error("생성된 ID를 받지 못했습니다.");
    return createdId;
  }

  async function updateRoom(payload) {
    const body = new URLSearchParams({
      id: String(payload.id),
      name: payload.name || "",
      location: payload.location || "",
      capacity: String(payload.capacity ?? 1),
      isActive: String(payload.isActive || "0"),

      slotMinutes: String(payload.slotMinutes ?? DEFAULTS.slotMinutes),
      minMinutes: String(DEFAULTS.minMinutes),
      maxMinutes: String(DEFAULTS.maxMinutes),
      bufferMinutes: String(payload.bufferMinutes ?? DEFAULTS.bufferMinutes),
      bookingOpenDaysAhead: String(DEFAULTS.bookingOpenDaysAhead),

      availableStartDate: "",
      availableEndDate: ""
    });

    for (const [k, v] of payload.dowParams.entries()) {
      body.append(k, v);
    }

    return await fetchJson(API_UPDATE, {
      method: "POST",
      headers: { "Content-Type": "application/x-www-form-urlencoded" },
      credentials: "same-origin",
      body: body.toString(),
      throwOnOkFalse: true
    });
  }

  function formatDateNoSeconds(v) {
    const s = String(v || "").trim();
    if (!s || s === "-") return "-";

    const normalized = s.replace("T", " ");
    const m = normalized.match(/^(\d{4}-\d{2}-\d{2})(?:\s+(\d{2}:\d{2}))/);
    if (m) return m[1] + " " + m[2];

    const d = normalized.match(/^(\d{4}-\d{2}-\d{2})/);
    if (d) return d[1];

    return normalized;
  }

})();
