/**
 * 관리자 채팅 팝업 스크립트
 * - GET /admin/chat/messages?threadId&sinceId&limit
 * - POST /admin/chat/send (threadId, content)
 *
 * 구현 방식:
 * - sinceId 기반 폴링(2초)으로 신규 메시지 수신
 * - 전송 성공 시 즉시 리스트에 append
 *
 * 주의:
 * - 서버 응답은 { ok, message, data } 형태를 기대
 */
(() => {
  "use strict";

  const ctx = window.__MEETING__?.ctx || "";
  const threadId = Number(window.__MEETING__?.threadId || 0);

  const elList = document.getElementById("messageList");
  const elTxt = document.getElementById("txtMessage");
  const elBtnSend = document.getElementById("btnSend");
  const elBtnClose = document.getElementById("btnClose");

  let sinceId = 0;
  let pollingTimer = null;
  let polling = false;

  // -------------------------
  // 공통 유틸
  // -------------------------
  function escapeHtml(s) {
    return String(s ?? "")
      .replaceAll("&", "&amp;")
      .replaceAll("<", "&lt;")
      .replaceAll(">", "&gt;")
      .replaceAll('"', "&quot;")
      .replaceAll("'", "&#39;");
  }

  async function fetchJson(url, options) {
    const res = await fetch(url, options);
    const text = await res.text();
    let json = null;

    try {
      json = text ? JSON.parse(text) : null;
    } catch (e) {
      throw new Error("서버 응답이 JSON 형식이 아닙니다.");
    }

    if (!res.ok || !json || json.ok !== true) {
      const msg = (json && json.message) ? json.message : ("HTTP " + res.status);
      throw new Error(msg);
    }
    return json;
  }

  function scrollToBottom() {
    elList.scrollTop = elList.scrollHeight;
  }

  // -------------------------
  // 렌더
  // -------------------------
  function renderMessageItem(item) {
    const role = item.senderRole || "USER";
    const content = escapeHtml(item.content || "");
    const createdAt = escapeHtml(item.createdAt || "");

    // ✅ 말풍선 방향: ADMIN=오른쪽, USER=왼쪽
    const sideClass = (role === "ADMIN") ? "msg-right" : "msg-left";

    return `
      <div class="msg ${sideClass}" data-id="${escapeHtml(item.id)}">
        <div class="bubble">
          <div class="bubble-content">${content}</div>
          <div class="bubble-meta">${createdAt}</div>
        </div>
      </div>
    `;
  }

  function appendMessages(items) {
    if (!Array.isArray(items) || items.length === 0) return;

    // 첫 로딩 시 "empty" 메시지 제거
    const empty = elList.querySelector(".chat-empty");
    if (empty) empty.remove();

    const html = items.map(renderMessageItem).join("");
    elList.insertAdjacentHTML("beforeend", html);
    scrollToBottom();
  }

  // -------------------------
  // API
  // -------------------------
  async function apiGetMessages() {
    const url = ctx + "/admin/chat/messages?threadId=" + encodeURIComponent(threadId)
      + "&sinceId=" + encodeURIComponent(sinceId)
      + "&limit=50";

    return await fetchJson(url, { method: "GET" });
  }

  async function apiSendMessage(content) {
    const url = ctx + "/admin/chat/send";
    const params = new URLSearchParams();
    params.set("threadId", String(threadId));
    params.set("content", content);

    return await fetchJson(url, {
      method: "POST",
      headers: {
        "Content-Type": "application/x-www-form-urlencoded; charset=UTF-8",
      },
      body: params.toString(),
    });
  }

  // -------------------------
  // 폴링
  // -------------------------
  async function pollOnce() {
    if (polling) return;
    polling = true;

    try {
      const res = await apiGetMessages();
      const data = res.data || {};
      const items = Array.isArray(data.items) ? data.items : [];
      const next = Number(data.nextSinceId || sinceId);

      if (items.length > 0) {
        appendMessages(items);
      }
      if (next > sinceId) sinceId = next;

    } catch (e) {
      // ✅ 폴링 에러는 조용히 (필요하면 배너로 변경)
      console.warn("[poll error]", e.message);

    } finally {
      polling = false;
    }
  }

  function startPolling() {
    if (pollingTimer) return;
    pollingTimer = setInterval(pollOnce, 2000);
  }

  function stopPolling() {
    if (!pollingTimer) return;
    clearInterval(pollingTimer);
    pollingTimer = null;
  }

  // -------------------------
  // 이벤트
  // -------------------------
  async function onSend() {
    const msg = (elTxt.value || "").trim();
    if (!msg) return;

    elBtnSend.disabled = true;

    try {
      const res = await apiSendMessage(msg);
      const saved = res.data?.item;

      if (saved) {
        appendMessages([saved]);
        if (Number(saved.id) > sinceId) sinceId = Number(saved.id);
      }

      elTxt.value = "";

    } catch (e) {
      alert(e.message || "전송 실패");

    } finally {
      elBtnSend.disabled = false;
      elTxt.focus();
    }
  }

  function bindEvents() {
    elBtnClose?.addEventListener("click", () => window.close());
    elBtnSend?.addEventListener("click", onSend);

    // Enter 전송(Shift+Enter 줄바꿈)
    elTxt?.addEventListener("keydown", (e) => {
      if (e.key === "Enter" && !e.shiftKey) {
        e.preventDefault();
        onSend();
      }
    });

    // 창 닫힐 때 폴링 정리
    window.addEventListener("beforeunload", () => {
      stopPolling();
    });
  }

  // -------------------------
  // 초기화
  // -------------------------
  function init() {
    if (!ctx || !threadId) {
      alert("채팅 초기화 실패: ctx/threadId가 없습니다.");
      return;
    }

    bindEvents();

    // ✅ 첫 로딩: 전체 한번 가져오기 (sinceId=0)
    pollOnce().then(() => {
      startPolling();
    });
  }

  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", init);
  } else {
    init();
  }
})();
