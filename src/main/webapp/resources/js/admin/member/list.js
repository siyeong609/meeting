/**
 * admin/member/list.js
 * - 회원관리 목록/검색/페이징/상세/생성/삭제
 *
 * 전제:
 * - common.js에 fetchJson / showModal / escapeHtml / renderPagination 존재
 * - JSP에서 window.__MEETING__.ctx 제공
 */
(function () {
  "use strict";

  const ctx = (window.__MEETING__ && window.__MEETING__.ctx) ? window.__MEETING__.ctx : "";

  const API_LIST = ctx + "/admin/members";
  const API_CREATE = ctx + "/admin/members/create";
  const API_UPDATE = ctx + "/admin/members/update";
  const API_DELETE = ctx + "/admin/members/delete";
  const API_UPLOAD = ctx + "/admin/members/profile/upload";

  // ✅ 기본 프로필 (사용자 지정: uploads/profile/default-profile.svg)
  const DEFAULT_PROFILE_DB_PATH = "/resources/uploads/profile/default-profile.svg";
  const DEFAULT_PROFILE_URL = ctx + DEFAULT_PROFILE_DB_PATH;

  const state = { page: 1, size: 10, q: "" };
  let allMembers = [];

  // DOM
  const tbody = document.getElementById("memberTableBody");
  const totalEl = document.getElementById("totalElements");
  const inputQ = document.getElementById("searchMember");
  const btnSearch = document.getElementById("btnSearch");
  const selectSize = document.getElementById("pageSize");
  const btnCreate = document.getElementById("btnCreate");
  const btnDeleteSelected = document.getElementById("btnDeleteSelected");
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

    loadMembers();

    // 검색
    btnSearch.addEventListener("click", () => {
      state.q = (inputQ.value || "").trim();
      state.page = 1;
      loadMembers();
    });

    inputQ.addEventListener("keydown", (e) => {
      if (e.key === "Enter") {
        e.preventDefault();
        state.q = (inputQ.value || "").trim();
        state.page = 1;
        loadMembers();
      }
    });

    // page size
    selectSize.addEventListener("change", () => {
      state.size = parseInt(selectSize.value, 10) || 10;
      state.page = 1;
      loadMembers();
    });

    // create / delete selected
    btnCreate.addEventListener("click", openMemberCreate);
    btnDeleteSelected.addEventListener("click", deleteSelectedMembers);

    // check all
    chkAll.addEventListener("change", () => {
      const checked = chkAll.checked;
      document.querySelectorAll(".member-chk").forEach((chk) => {
        if (chk.disabled) return;
        chk.checked = checked;
      });
    });
  });

  async function loadMembers() {
    tbody.innerHTML = "<tr><td colspan='6' class='text-center text-muted'>데이터를 불러오는 중...</td></tr>";
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

      allMembers = Array.isArray(json.data) ? json.data : [];

      const total = (json.page && json.page.totalElements != null) ? json.page.totalElements : 0;
      totalEl.textContent = String(total);

      renderMembers(allMembers);

      if (typeof renderPagination === "function") {
        renderPagination("#pagination", json.page, (newPage) => {
          state.page = newPage;
          loadMembers();
        });
      }

    } catch (e) {
      allMembers = [];
      totalEl.textContent = "0";
      tbody.innerHTML = "<tr><td colspan='6' class='text-center text-muted'>데이터 로드 실패</td></tr>";
      showModal("회원 데이터를 불러오는 중 오류가 발생했습니다. (" + (e.message || e) + ")", "error");
    }
  }

  function renderMembers(rows) {
    if (!rows || rows.length === 0) {
      tbody.innerHTML = "<tr><td colspan='6' class='text-center text-muted'>회원이 없습니다.</td></tr>";
      return;
    }

    tbody.innerHTML = rows.map((m) => {
      const role = (m.role || "").toUpperCase();
      const isAdmin = role === "ADMIN";

      const loginId = escapeHtml(m.loginId || "-");
      const name = escapeHtml(m.name || "-");
      const email = escapeHtml(m.email || "-");
      const createdAt = escapeHtml(m.createdAt || "-");

      const chkHtml = isAdmin
        ? "<input type='checkbox' class='member-chk' disabled title='ADMIN은 삭제 불가'>"
        : "<input type='checkbox' class='member-chk' value='" + (m.id || 0) + "'>";

      const nameLabel = isAdmin ? (name + " <span class='text-muted'>(ADMIN)</span>") : name;

      return ""
        + "<tr>"
        +   "<td style='text-align:center;'>" + chkHtml + "</td>"
        +   "<td>" + loginId + "</td>"
        +   "<td>" + nameLabel + "</td>"
        +   "<td>" + (email === "" ? "-" : email) + "</td>"
        +   "<td>" + (createdAt === "" ? "-" : createdAt) + "</td>"
        +   "<td>"
        +     "<button type='button' class='btn btn-primary' data-detail-id='" + (m.id || 0) + "'>상세</button>"
        +   "</td>"
        + "</tr>";
    }).join("");

    // 상세 버튼 바인딩
    tbody.querySelectorAll("[data-detail-id]").forEach((btn) => {
      btn.addEventListener("click", () => {
        const id = btn.getAttribute("data-detail-id");
        openMemberDetail(id);
      });
    });

    // 체크박스 동기화
    document.querySelectorAll(".member-chk").forEach((chk) => {
      chk.addEventListener("change", syncChkAll);
    });
  }

  function syncChkAll() {
    const chks = Array.from(document.querySelectorAll(".member-chk")).filter(x => !x.disabled);
    if (chks.length === 0) {
      chkAll.checked = false;
      return;
    }
    chkAll.checked = chks.every(x => x.checked);
  }

  async function deleteSelectedMembers() {
    const ids = Array.from(document.querySelectorAll(".member-chk"))
      .filter(x => !x.disabled && x.checked)
      .map(x => x.value);

    if (ids.length === 0) {
      showModal("삭제할 회원을 선택하세요.", "warning");
      return;
    }

    if (!confirm("선택한 회원을 삭제하시겠습니까?\n(ADMIN 계정은 삭제되지 않습니다)")) {
      return;
    }

    try {
      const body = new URLSearchParams({ ids: ids.join(",") });

      const json = await fetchJson(API_DELETE, {
        method: "POST",
        headers: { "Content-Type": "application/x-www-form-urlencoded" },
        credentials: "same-origin",
        body: body.toString(),
        throwOnOkFalse: true
      });

      showModal("삭제 완료: " + (json.data || 0) + "명", "success");
      loadMembers();

    } catch (e) {
      showModal("삭제 실패: " + (e.message || e), "error");
    }
  }

  function openMemberCreate() {
    openMemberModal({
      mode: "create",
      member: { id: 0, loginId: "", name: "", email: "", role: "USER", profileImage: "", memo: "" }
    });
  }

  function openMemberDetail(memberId) {
    const m = allMembers.find(x => String(x.id) === String(memberId));
    if (!m) return;

    // memo가 null일 수 있으니 보정
    m.memo = m.memo || "";

    openMemberModal({ mode: "detail", member: m });
  }

  function openMemberModal(opt) {
    const mode = opt.mode;
    const m = opt.member;
    const isCreate = mode === "create";

    const profileDbPath = (m.profileImage && String(m.profileImage).trim()) ? m.profileImage : "";
    const hasCustomProfile = !!profileDbPath;

    const profileImageUrl = hasCustomProfile ? (ctx + profileDbPath) : DEFAULT_PROFILE_URL;
    const fileName = hasCustomProfile ? profileDbPath.split("/").pop() : "";
    const fileHref = hasCustomProfile ? (ctx + profileDbPath) : "#";

    const overlay = document.createElement("div");
    overlay.className = "modal-overlay";

    overlay.innerHTML =
      "<div class='modal modal-detail'>"
      + "<div class='modal-header info'>"
      +   "<div class='modal-icon info'>i</div>"
      +   "<div class='modal-title'>" + (isCreate ? "회원 생성" : "회원 상세") + "</div>"
      +   "<button class='modal-close' type='button'>&times;</button>"
      + "</div>"
      + "<div class='modal-body'>"

      + "<div class='detail-section'>"
      +   "<div class='detail-section-title'>프로필 이미지</div>"
      +   "<div class='profile-image-section'>"
      +     "<div class='profile-image-preview'>"
      +       "<img id='memberDetailProfilePreview' src='" + profileImageUrl + "' alt='프로필'"
      +            " onerror=\"this.onerror=null; this.src='" + DEFAULT_PROFILE_URL + "'\">"
      +     "</div>"
      +     "<div class='form-group'>"
      +       "<label>이미지 업로드</label>"
      +       "<input type='file' id='memberDetailProfileFile' accept='image/jpeg,image/jpg,image/png' style='padding: 8px;'>"
      +       "<small class='text-muted'>JPG, PNG만 가능 (최대 5MB)</small>"

      +       "<div id='currentProfileRow' style='margin-top:8px; display:" + (!isCreate && hasCustomProfile ? "flex" : "none") + ";'>"
      +         "<a id='currentProfileDownload' href='" + fileHref + "' download>" + escapeHtml(fileName) + "</a>"
      +         "<label style='margin-left:10px; user-select:none;'>"
      +           "<input type='checkbox' id='memberDetailProfileDelete'> 삭제"
      +         "</label>"
      +       "</div>"

      +     "</div>"
      +   "</div>"
      + "</div>"

      + "<div class='detail-section'>"
      +   "<div class='detail-section-title'>기본 정보</div>"

      +   "<div class='form-group'><label>아이디</label>"
      +     "<input type='text' id='memberInputLoginId' value='" + escapeHtml(m.loginId || "") + "' " + (isCreate ? "" : "readonly") + ">"
      +   "</div>"

      +   "<div class='form-group'><label>이름</label>"
      +     "<input type='text' id='memberInputName' value='" + escapeHtml(m.name || "") + "' " + (isCreate ? "" : "readonly") + ">"
      +   "</div>"

      +   "<div class='form-group'><label>이메일</label>"
      +     "<input type='email' id='memberDetailEmail' value='" + escapeHtml(m.email || "") + "'>"
      +   "</div>"

      +   (isCreate
          ? ("<div class='form-group'><label>권한</label>"
            + "<select id='memberInputRole' style='height:40px; width:100%; padding:0 10px; border:1px solid #ddd; border-radius:8px;'>"
            + "<option value='USER' " + ((String(m.role||"").toUpperCase()==="USER") ? "selected" : "") + ">USER</option>"
            + "<option value='ADMIN' " + ((String(m.role||"").toUpperCase()==="ADMIN") ? "selected" : "") + ">ADMIN</option>"
            + "</select></div>")
          : ("<div class='form-group'><label>권한</label>"
            + "<input type='text' value='" + escapeHtml((m.role||"").toUpperCase()) + "' readonly></div>")
        )

      + "</div>"

      + "<div class='detail-section'>"
      +   "<div class='detail-section-title'>" + (isCreate ? "비밀번호 설정" : "비밀번호 수정") + "</div>"
      +   "<div class='form-group'><label>" + (isCreate ? "비밀번호" : "새 비밀번호") + "</label>"
      +     "<input type='password' id='memberDetailNewPw' placeholder='" + (isCreate ? "필수 입력" : "변경 시에만 입력") + "'>"
      +   "</div>"
      + "</div>"

      // ✅ 메모 영역 추가
      + "<div class='detail-section'>"
      +   "<div class='detail-section-title'>메모</div>"
      +   "<div class='form-group'>"
      +     "<label>관리자 메모</label>"
      +     "<textarea id='memberDetailMemo' placeholder='회원 관련 메모' style='width:100%; min-height:90px; padding:12px 14px; border:1px solid #e2e8f0; border-radius:8px; font-size:14px; resize:vertical;'>"
      +       escapeHtml(m.memo || "")
      +     "</textarea>"
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

    const fileInput = overlay.querySelector("#memberDetailProfileFile");
    const previewImg = overlay.querySelector("#memberDetailProfilePreview");
    const deleteCheckbox = overlay.querySelector("#memberDetailProfileDelete");

    let selectedFile = null;

    fileInput.addEventListener("change", (e) => {
      const file = e.target.files && e.target.files[0];
      if (!file) return;

      const fileNameLower = (file.name || "").toLowerCase();
      const ext = fileNameLower.substring(fileNameLower.lastIndexOf(".") + 1);

      if (ext !== "jpg" && ext !== "jpeg" && ext !== "png") {
        showModal("JPG 또는 PNG 파일만 업로드 가능합니다.", "error");
        fileInput.value = "";
        return;
      }
      if (file.size > 5 * 1024 * 1024) {
        showModal("파일 크기는 5MB 이하여야 합니다.", "error");
        fileInput.value = "";
        return;
      }

      selectedFile = file;

      // 상세 모드: 새 파일 선택 시 기존 삭제 체크는 무의미 → 잠금
      if (!isCreate && deleteCheckbox) {
        deleteCheckbox.checked = false;
        deleteCheckbox.disabled = true;
      }

      const reader = new FileReader();
      reader.onload = (ev) => { previewImg.src = ev.target.result; };
      reader.readAsDataURL(file);
    });

    overlay.querySelector("[data-save]").onclick = async () => {
      try {
        const loginId = (overlay.querySelector("#memberInputLoginId").value || "").trim();
        const name = (overlay.querySelector("#memberInputName").value || "").trim();
        const email = (overlay.querySelector("#memberDetailEmail").value || "").trim();
        const pw = (overlay.querySelector("#memberDetailNewPw").value || "").trim();

        // ✅ 메모 값
        const memo = (overlay.querySelector("#memberDetailMemo").value || "").trim();

        let userId = m.id || 0;

        if (isCreate) {
          const role = (overlay.querySelector("#memberInputRole").value || "USER").trim();

          const createdId = await createMember(loginId, pw, name, email, role, memo);
          userId = createdId;

          if (selectedFile) {
            await uploadProfile(userId, selectedFile);
          }

          overlay.remove();
          showModal("회원이 생성되었습니다.", "success");
          loadMembers();
          return;
        }

        let deleteProfile = false;
        if (!selectedFile && deleteCheckbox && deleteCheckbox.checked) {
          deleteProfile = true;
        }

        if (selectedFile) {
          await uploadProfile(userId, selectedFile);
        }

        await updateMember(userId, email, pw, memo, deleteProfile);

        overlay.remove();
        showModal("저장되었습니다.", "success");
        loadMembers();

      } catch (e) {
        showModal("처리 실패: " + (e.message || e), "error");
      }
    };

    setTimeout(() => overlay.classList.add("show"), 10);
  }

  async function createMember(loginId, password, name, email, role, memo) {
    const body = new URLSearchParams({
      loginId: loginId || "",
      password: password || "",
      name: name || "",
      email: email || "",
      role: role || "USER",
      memo: memo || ""
    });

    const json = await fetchJson(API_CREATE, {
      method: "POST",
      headers: { "Content-Type": "application/x-www-form-urlencoded" },
      credentials: "same-origin",
      body: body.toString(),
      throwOnOkFalse: true
    });

    if (!json.data) throw new Error("생성된 ID를 받지 못했습니다.");
    return parseInt(json.data, 10);
  }

  async function updateMember(userId, email, newPassword, memo, deleteProfile) {
    const body = new URLSearchParams({
      userId: String(userId),
      email: email || "",
      newPassword: newPassword || "",
      memo: memo || "",
      deleteProfile: deleteProfile ? "true" : "false"
    });

    return await fetchJson(API_UPDATE, {
      method: "POST",
      headers: { "Content-Type": "application/x-www-form-urlencoded" },
      credentials: "same-origin",
      body: body.toString(),
      throwOnOkFalse: true
    });
  }

  async function uploadProfile(userId, file) {
    const form = new FormData();
    form.append("userId", String(userId));
    form.append("profile", file);

    const res = await fetch(API_UPLOAD, {
      method: "POST",
      credentials: "same-origin",
      body: form
    });

    const json = await res.json();

    if (!res.ok || !json || json.ok !== true) {
      throw new Error((json && json.message) ? json.message : "프로필 업로드 실패");
    }
    return json.data;
  }

})();
