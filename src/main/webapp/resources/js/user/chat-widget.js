/**
 * user/chat-widget.js
 * - 우측 하단 플로팅 채팅 위젯
 * - 2초 폴링으로 신규 메시지 수신
 *
 * 전제:
 * - common.js: fetchJson / showModal / escapeHtml 존재(없어도 최소 동작하도록 방어)
 * - header.jsp에서 window.__MEETING__.ctx, window.__MEETING__.loginUser 제공 권장
 */
(function () {
  "use strict";

  const ctx = (window.__MEETING__ && window.__MEETING__.ctx) ? window.__MEETING__.ctx : "";
  const API_MESSAGES = ctx + "/user/chat/messages";
  const API_SEND = ctx + "/user/chat/send";

  // ✅ CSS를 페이지마다 직접 넣기 귀찮으니, JS가 자동 삽입(없으면)
  function ensureCss() {
    const id = "chatWidgetCss";
    if (document.getElementById(id)) return;

    const link = document.createElement("link");
    link.id = id;
    link.rel = "stylesheet";
    link.href = ctx + "/resources/css/user/chat-widget.css";
    document.head.appendChild(link);
  }

  // ✅ common.js가 늦게 로드되는 페이지도 있으니, 준비될 때까지 대기
  function waitForCommon(maxTry, cb) {
    let n = 0;
    const t = setInterval(function () {
      n++;
      const ok = (typeof fetchJson === "function") && (typeof showModal === "function");
      if (ok) {
        clearInterval(t);
        cb();
        return;
      }
      if (n >= maxTry) {
        clearInterval(t);
        // 최소 기능만이라도 돌아가게 alert로 대체
        cb();
      }
    }, 150);
  }

  // 상태
  let isOpen = false;
  let pollTimer = null;
  let lastId = 0;
  let isLoading = false;

  // DOM refs
  let widgetEl, bodyEl, inputEl, btnSendEl;

  document.addEventListener("DOMContentLoaded", function () {
    ensureCss();

    waitForCommon(20, function () {
      buildWidgetIfNeeded();
      bindMenuOpen();
    });
  });

  /**
   * “1:1채팅문의” 클릭을 위젯 오픈으로 바인딩
   * - JS 실패 시에는 기존 href(/user/chat)로 자연스럽게 이동(폴백)
   */
  function bindMenuOpen() {
    const link = document.getElementById("menuChatLink"); // ✅ header.jsp에서 id 부여 권장
    if (!link) return;

    link.addEventListener("click", function (e) {
      // ✅ 위젯 방식 사용: 페이지 이동 막고 오픈
      e.preventDefault();
      openWidget();
    });
  }

  function buildWidgetIfNeeded() {
    if (document.getElementById("chatWidget")) {
      widgetEl = document.getElementById("chatWidget");
      bodyEl = document.getElementById("chatWidgetBody");
      inputEl = document.getElementById("chatWidgetInput");
      btnSendEl = document.getElementById("chatWidgetSend");
      return;
    }

    widgetEl = document.createElement("div");
    widgetEl.id = "chatWidget";
    widgetEl.className = "chat-widget";

    widgetEl.innerHTML =
      "<div class='chat-widget__header'>" +
      "  <div class='chat-widget__title'>1:1 채팅문의</div>" +
      "  <button type='button' class='chat-widget__close' id='chatWidgetClose' aria-label='close'>&times;</button>" +
      "</div>" +
      "<div class='chat-widget__body' id='chatWidgetBody'></div>" +
      "<div class='chat-widget__input'>" +
      "  <textarea class='chat-widget__textarea' id='chatWidgetInput' placeholder='메시지를 입력하세요'></textarea>" +
      "  <button type='button' class='btn btn-primary chat-widget__send' id='chatWidgetSend'>전송</button>" +
      "</div>";

    document.body.appendChild(widgetEl);

    bodyEl = document.getElementById("chatWidgetBody");
    inputEl = document.getElementById("chatWidgetInput");
    btnSendEl = document.getElementById("chatWidgetSend");

    // 닫기
    document.getElementById("chatWidgetClose").addEventListener("click", function () {
      closeWidget();
    });

    // 전송
    btnSendEl.addEventListener("click", function () {
      sendMessage();
    });

    // Enter 전송(Shift+Enter 줄바꿈)
    inputEl.addEventListener("keydown", function (e) {
      if (e.key === "Enter" && !e.shiftKey) {
        e.preventDefault();
        sendMessage();
      }
    });
  }

  function openWidget() {
    if (isOpen) return;

    isOpen = true;
    widgetEl.classList.add("open");

    // 최초 로드
    lastId = 0;
    bodyEl.innerHTML = "<div style='color:rgba(0,0,0,0.7); font-size:13px;'>불러오는 중...</div>";

    loadMessages(true);

    // 폴링 시작
    pollTimer = setInterval(function () {
      loadMessages(false);
    }, 2000);
  }

  function closeWidget() {
    isOpen = false;
    widgetEl.classList.remove("open");

    if (pollTimer) {
      clearInterval(pollTimer);
      pollTimer = null;
    }
  }

  async function loadMessages(isFirst) {
    if (isLoading) return;
    isLoading = true;

    try {
      // fetchJson이 없으면 최소 처리
      if (typeof fetchJson !== "function") {
        isLoading = false;
        return;
      }

      const body = new URLSearchParams({ sinceId: String(lastId) });

      const json = await fetchJson(API_MESSAGES, {
        method: "POST",
        headers: { "Content-Type": "application/x-www-form-urlencoded" },
        credentials: "same-origin",
        body: body.toString(),
        throwOnOkFalse: true
      });

      const data = json.data || {};
      const items = Array.isArray(data.items) ? data.items : [];

      if (isFirst) bodyEl.innerHTML = "";

      if (items.length > 0) {
        // ✅ 스크롤이 하단 근처면 자동 스크롤
        const nearBottom = isScrollNearBottom(bodyEl);

        items.forEach(function (m) {
          appendMessage(m);
          lastId = Math.max(lastId, Number(m.id || 0));
        });

        if (nearBottom) scrollToBottom(bodyEl);
      }

    } catch (e) {
      // 최초 1번만 에러를 사용자에게 알려도 충분
      if (isFirst && typeof showModal === "function") {
        showModal("채팅 데이터를 불러오는 중 오류가 발생했습니다. (" + (e.message || e) + ")", "error");
      }
    } finally {
      isLoading = false;
    }
  }

  async function sendMessage() {
    const text = (inputEl.value || "").trim();
    if (!text) {
      if (typeof showModal === "function") showModal("메시지를 입력하세요.", "warning");
      return;
    }

    try {
      btnSendEl.disabled = true;

      const body = new URLSearchParams({ content: text });

      const json = await fetchJson(API_SEND, {
        method: "POST",
        headers: { "Content-Type": "application/x-www-form-urlencoded" },
        credentials: "same-origin",
        body: body.toString(),
        throwOnOkFalse: true
      });

      // 서버가 저장된 1건을 반환
      const saved = json.data;
      if (saved) {
        appendMessage(saved);
        lastId = Math.max(lastId, Number(saved.id || 0));
        scrollToBottom(bodyEl);
      }

      inputEl.value = "";

    } catch (e) {
      if (typeof showModal === "function") showModal("전송 실패: " + (e.message || e), "error");
    } finally {
      btnSendEl.disabled = false;
    }
  }

  function appendMessage(m) {
    const senderRole = String(m.senderRole || "").toUpperCase();

    // ✅ 카톡 스타일: 왼쪽=관리자, 오른쪽=나(USER)
    const isRight = (senderRole === "USER");

    const senderName = safeText(m.senderName) || "-";
    const senderLoginId = safeText(m.senderLoginId) || "-";
    const meta =
      (isRight ? "나" : "관리자") +
      " | " + senderName + "(" + senderLoginId + ")";

    // 시간 표시(간단히 HH:mm만)
    const createdAt = safeText(m.createdAt) || "";
    const timeLabel = formatTime(createdAt);

    const contentHtml = nl2br(escapeSafe(m.content));

    const row = document.createElement("div");
    row.className = "chat-msg " + (isRight ? "right" : "left");

    // ✅ 왼쪽만 아바타 표시
    // - senderId가 없으니 정책 고정:
    //   1) /resources/uploads/profile/1.jpg 먼저 로드 시도
    //   2) 없으면 default-profile.svg
    let avatarHtml = "";
    if (!isRight) {
      const adminAvatarTry = ctx + "/resources/uploads/profile/1.jpg";
      const defaultAdminAvatar = ctx + "/resources/uploads/profile/default-profile.svg";

      avatarHtml =
        "<img class='chat-avatar' " +
        " src='" + adminAvatarTry + "' alt='admin' " +
        " onerror=\"this.onerror=null; this.src='" + defaultAdminAvatar + "';\">" ;
    }

    // 구조
    row.innerHTML =
      (isRight ? "" : avatarHtml) +
      "<div>" +
      "  <div class='chat-meta'>" + escapeSafe(meta) + "</div>" +
      "  <div class='chat-bubble'>" + contentHtml + "</div>" +
      "  <div class='chat-time'>" + escapeSafe(timeLabel) + "</div>" +
      "</div>";

    bodyEl.appendChild(row);
  }

  // ===== util =====

  function scrollToBottom(el) {
    el.scrollTop = el.scrollHeight;
  }

  function isScrollNearBottom(el) {
    const threshold = 120; // px
    return (el.scrollHeight - el.scrollTop - el.clientHeight) < threshold;
  }

  function safeText(v) {
    if (v == null) return "";
    return String(v);
  }

  function escapeSafe(v) {
    // common.js의 escapeHtml이 있으면 사용
    if (typeof escapeHtml === "function") return escapeHtml(String(v == null ? "" : v));
    // 최소 escape
    return String(v == null ? "" : v)
      .replace(/&/g, "&amp;")
      .replace(/</g, "&lt;")
      .replace(/>/g, "&gt;")
      .replace(/"/g, "&quot;")
      .replace(/'/g, "&#39;");
  }

  function nl2br(s) {
    return String(s).replace(/\n/g, "<br>");
  }

  function formatTime(createdAt) {
    // createdAt: "yyyy-MM-dd HH:mm:ss"
    if (!createdAt) return "";
    const parts = createdAt.split(" ");
    if (parts.length < 2) return createdAt;
    const hhmm = parts[1].substring(0, 5);
    return hhmm;
  }

})();
