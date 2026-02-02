/**
 * user/profile.js
 * - 내정보수정(이메일/비번/프로필 업로드/삭제)
 *
 * 전제:
 * - common.js에 fetchJson / showModal 존재
 * - footer.jsp에서 window.__MEETING__.ctx 주입됨
 *
 * 실패 원인 TOP:
 * 1) DOM id 불일치로 이벤트 미바인딩
 * 2) upload/update 응답이 JSON이 아니라 HTML(세션 만료/에러페이지) -> res.json() 실패
 * 3) 서버가 기대하는 파라미터명이 다른 경우(newPassword vs pw/password)
 */
(function () {
  "use strict";

  const ctx = (window.__MEETING__ && window.__MEETING__.ctx) ? window.__MEETING__.ctx : "";

  const API_UPDATE = ctx + "/user/profile/update";
  const API_UPLOAD = ctx + "/user/profile/upload";

  const DEFAULT_PROFILE_DB_PATH = "/resources/uploads/profile/default-profile.svg";
  const DEFAULT_PROFILE_URL = ctx + DEFAULT_PROFILE_DB_PATH;

  // DOM (※ 실제 JSP와 id가 반드시 일치해야 함)
  const previewImg = document.getElementById("profilePreview");
  const fileInput = document.getElementById("profileFile");
  const rowCurrent = document.getElementById("currentProfileRow");
  const linkDownload = document.getElementById("currentProfileDownload");
  const chkDelete = document.getElementById("deleteProfile");

  const inputEmail = document.getElementById("email");
  const inputPw = document.getElementById("newPassword");
  const btnSave = document.getElementById("btnSave");

  let selectedFile = null;
  let saving = false;

  document.addEventListener("DOMContentLoaded", function () {
    // 의존성 체크
    if (typeof fetchJson !== "function") {
      alert("fetchJson이 없습니다. resources/js/common.js를 확인하세요.");
      return;
    }
    if (typeof showModal !== "function") {
      alert("showModal이 없습니다. resources/js/common.js를 확인하세요.");
      return;
    }

    // DOM 체크 (여기서 하나라도 null이면 실제로는 이벤트가 안 붙음)
    const missing = [];
    if (!previewImg) missing.push("profilePreview");
    if (!fileInput) missing.push("profileFile");
    if (!inputEmail) missing.push("email");
    if (!inputPw) missing.push("newPassword");
    if (!btnSave) missing.push("btnSave");

    if (missing.length > 0) {
      console.warn("[PROFILE] missing DOM ids:", missing.join(", "));
      showModal("profile.jsp의 id가 맞지 않아 저장이 동작하지 않습니다: " + missing.join(", "), "error");
      return;
    }

    bindEvents();
  });

  function bindEvents() {
    // 파일 선택
    fileInput.addEventListener("change", function (e) {
      const file = e.target.files && e.target.files[0];
      if (!file) return;

      // 확장자 체크
      const fileNameLower = String(file.name || "").toLowerCase();
      const dot = fileNameLower.lastIndexOf(".");
      const ext = dot >= 0 ? fileNameLower.substring(dot + 1) : "";

      if (ext !== "jpg" && ext !== "jpeg" && ext !== "png") {
        showModal("JPG 또는 PNG 파일만 업로드 가능합니다.", "error");
        fileInput.value = "";
        selectedFile = null;
        return;
      }

      // 용량 체크
      if (file.size > 5 * 1024 * 1024) {
        showModal("파일 크기는 5MB 이하여야 합니다.", "error");
        fileInput.value = "";
        selectedFile = null;
        return;
      }

      selectedFile = file;

      // 새 파일 선택 시 삭제 체크는 의미 없음 → 잠금
      if (chkDelete) {
        chkDelete.checked = false;
        chkDelete.disabled = true;
      }

      // 프리뷰
      const reader = new FileReader();
      reader.onload = function (ev) {
        previewImg.src = ev.target.result;
      };
      reader.readAsDataURL(file);
    });

    // 저장
    btnSave.addEventListener("click", onSave);
  }

  async function onSave() {
    if (saving) return; // 더블 클릭 방지
    saving = true;
    btnSave.disabled = true;

    try {
      const email = (inputEmail.value || "").trim();
      const newPassword = (inputPw.value || "").trim();

      // deleteProfile: 파일 업로드가 없고 + 체크되어있을 때만 true
      const deleteProfile = (!selectedFile && chkDelete && chkDelete.checked) ? true : false;

      console.log("[PROFILE] save start", { hasFile: !!selectedFile, deleteProfile, email });

      // 1) 업로드가 있으면 먼저 업로드
      if (selectedFile) {
        const dbPath = await uploadProfile(selectedFile);
        console.log("[PROFILE] upload ok:", dbPath);

        // 업로드 후 초기화
        selectedFile = null;
        fileInput.value = "";

        if (chkDelete) {
          chkDelete.disabled = false;
          chkDelete.checked = false;
        }
      } else {
        // 파일 없을 때는 delete 체크는 다시 활성화 (사용자가 다시 토글할 수 있게)
        if (chkDelete) chkDelete.disabled = false;
      }

      // 2) 업데이트(이메일/비번/삭제)
      const updateJson = await updateProfile(email, newPassword, deleteProfile);
      console.log("[PROFILE] update ok:", updateJson);

      // 비번 입력값은 성공 시 비움
      inputPw.value = "";

      showModal("저장되었습니다.", "success");

      // 헤더(프로필 이미지 등) 반영 필요하면 리로드
      setTimeout(function () {
        window.location.reload();
      }, 250);

    } catch (e) {
      console.error("[PROFILE] save failed:", e);
      showModal("처리 실패: " + (e.message || e), "error");
    } finally {
      saving = false;
      btnSave.disabled = false;
    }
  }

  /**
   * update 파라미터명이 서버랑 다를 가능성이 높아서
   * - newPassword / password / pw 를 같이 보내서 흡수
   * - deleteProfile도 true/false 뿐 아니라 1/0도 같이 제공 가능하게 함
   */
  async function updateProfile(email, newPassword, deleteProfile) {
    const body = new URLSearchParams({
      email: email || "",

      // ✅ 서버가 어떤 키를 쓰든 흡수
      newPassword: newPassword || "",
      password: newPassword || "",
      pw: newPassword || "",

      // ✅ 서버가 true/false 또는 1/0을 기대할 수 있음
      deleteProfile: deleteProfile ? "true" : "false",
      delete_profile: deleteProfile ? "1" : "0"
    });

    return await fetchJson(API_UPDATE, {
      method: "POST",
      headers: { "Content-Type": "application/x-www-form-urlencoded; charset=UTF-8" },
      credentials: "same-origin",
      body: body.toString(),
      throwOnOkFalse: true
    });
  }

  /**
   * 업로드 응답이 JSON이 아닐 수 있음(세션 만료 -> 로그인 HTML)
   * -> res.json() 직전에 content-type 확인 + fallback으로 text 읽어서 에러 메시지 강화
   */
  async function uploadProfile(file) {
    const form = new FormData();

    // ✅ 서버가 "profile"이 아니라 "file" / "upload" 등을 기대할 수 있어 같이 넣어둠(안전)
    form.append("profile", file);
    form.append("file", file);

    const res = await fetch(API_UPLOAD, {
      method: "POST",
      credentials: "same-origin",
      body: form
    });

    const ct = (res.headers.get("content-type") || "").toLowerCase();

    // JSON 아닌 경우(대부분 로그인 페이지/에러 페이지)
    if (ct.indexOf("application/json") === -1) {
      const text = await res.text();
      throw new Error("업로드 응답이 JSON이 아닙니다. (status=" + res.status + ") 서버 응답 일부: " + text.slice(0, 120));
    }

    const json = await res.json();

    if (!res.ok || !json || json.ok !== true) {
      throw new Error((json && json.message) ? json.message : "프로필 업로드 실패");
    }

    // 서버는 data에 DB 저장 경로를 반환한다고 가정: "/resources/uploads/profile/xxx.png"
    const dbPath = json.data;
    if (!dbPath) {
      throw new Error("업로드 성공 응답에 data(dbPath)가 없습니다.");
    }

    // 화면 갱신 (캐시 때문에 갱신 안 되는 경우가 많아서 ts 붙임)
    const url = (String(dbPath).startsWith("http"))
      ? dbPath
      : (ctx + dbPath);

    const bust = url + (url.indexOf("?") >= 0 ? "&" : "?") + "ts=" + Date.now();

    previewImg.src = bust;
    previewImg.onerror = function () {
      previewImg.onerror = null;
      previewImg.src = DEFAULT_PROFILE_URL;
    };

    // 커스텀 프로필 영역 노출 + 다운로드 링크 갱신
    if (rowCurrent) rowCurrent.style.display = "flex";
    if (linkDownload) {
      linkDownload.href = url;
      linkDownload.textContent = String(dbPath).split("/").pop() || "profile";
    }

    return dbPath;
  }

})();
