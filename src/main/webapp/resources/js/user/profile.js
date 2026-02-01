/**
 * user/profile.js
 * - 내정보수정(이메일/비번/프로필 업로드/삭제)
 *
 * 전제:
 * - common.js에 fetchJson / showModal 존재
 * - JSP에서 window.__MEETING__.ctx 제공
 */
(function () {
  "use strict";

  const ctx = (window.__MEETING__ && window.__MEETING__.ctx) ? window.__MEETING__.ctx : "";

  const API_UPDATE = ctx + "/user/profile/update";
  const API_UPLOAD = ctx + "/user/profile/upload";

  const DEFAULT_PROFILE_DB_PATH = "/resources/uploads/profile/default-profile.svg";
  const DEFAULT_PROFILE_URL = ctx + DEFAULT_PROFILE_DB_PATH;

  // DOM
  const previewImg = document.getElementById("profilePreview");
  const fileInput = document.getElementById("profileFile");
  const rowCurrent = document.getElementById("currentProfileRow");
  const linkDownload = document.getElementById("currentProfileDownload");
  const chkDelete = document.getElementById("deleteProfile");

  const inputEmail = document.getElementById("email");
  const inputPw = document.getElementById("newPassword");
  const btnSave = document.getElementById("btnSave");

  let selectedFile = null;

  document.addEventListener("DOMContentLoaded", function () {
    if (typeof fetchJson !== "function") {
      alert("fetchJson이 없습니다. resources/js/common.js에 fetchJson을 추가하세요.");
      return;
    }
    if (typeof showModal !== "function") {
      alert("showModal이 없습니다. resources/js/common.js에 showModal을 추가하세요.");
      return;
    }

    bindEvents();
  });

  function bindEvents() {
    // 파일 선택
    fileInput.addEventListener("change", function (e) {
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

      // 새 파일 선택 시 삭제 체크는 의미 없음 → 잠금
      if (chkDelete) {
        chkDelete.checked = false;
        chkDelete.disabled = true;
      }

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
    try {
      const email = (inputEmail.value || "").trim();
      const newPassword = (inputPw.value || "").trim();

      // deleteProfile: "파일 업로드 안함" + "삭제 체크"일 때만 true
      const deleteProfile = (!selectedFile && chkDelete && chkDelete.checked) ? true : false;

      // 1) 파일 업로드가 있으면 먼저 업로드(서버가 DB profile_image까지 반영)
      if (selectedFile) {
        await uploadProfile(selectedFile);

        // 업로드 후엔 삭제 체크 잠금 해제(다음 편집을 위해 초기화)
        if (chkDelete) {
          chkDelete.disabled = false;
          chkDelete.checked = false;
        }
        selectedFile = null;
        fileInput.value = "";
      }

      // 2) email/password/deleteProfile 업데이트
      await updateProfile(email, newPassword, deleteProfile);

      showModal("저장되었습니다.", "success");

      // 헤더(프로필/라벨) 갱신을 위해 리로드
      setTimeout(function () {
        window.location.reload();
      }, 250);

    } catch (e) {
      showModal("처리 실패: " + (e.message || e), "error");
    }
  }

  async function updateProfile(email, newPassword, deleteProfile) {
    const body = new URLSearchParams({
      email: email || "",
      newPassword: newPassword || "",
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

  async function uploadProfile(file) {
    const form = new FormData();
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

    // 서버는 data에 DB 저장 경로를 반환: "/resources/uploads/profile/xxx.png"
    const dbPath = json.data;

    // 화면 갱신
    const url = (dbPath && String(dbPath).startsWith("http"))
      ? dbPath
      : (ctx + dbPath);

    previewImg.src = url;

    // 커스텀 프로필 영역 노출 + 다운로드 링크 갱신
    if (rowCurrent) rowCurrent.style.display = "flex";
    if (linkDownload) {
      linkDownload.href = url;
      linkDownload.textContent = (dbPath || "").split("/").pop() || "profile";
    }

    return dbPath;
  }

})();
