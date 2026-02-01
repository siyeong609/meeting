<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%
  // =========================
  // 관리자 채팅 팝업 전용 화면
  // - admin/layout/header/footer include ❌
  // - 채팅 UI만 렌더링 ✅
  // =========================
  String ctx = request.getContextPath();
  Object tid = request.getAttribute("threadId");
%>

<!doctype html>
<html lang="ko">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1" />
  <title>대화창(threadId: <%= tid %>)</title>

  <!-- 공통 폰트/기본 스타일을 쓰고 싶으면 common.css만 가져오고, 레이아웃은 제외 -->
  <link rel="stylesheet" href="<%= ctx %>/resources/css/common.css" />
  <link rel="stylesheet" href="<%= ctx %>/resources/css/admin/chat-thread-popup.css" />
</head>

<body class="chat-popup-body">

<div class="chat-popup">
  <!-- 상단 바(작게) -->
  <div class="chat-popup-top">
    <div class="chat-popup-title">
      <strong>1:1 채팅</strong>
      <span class="chat-popup-sub">threadId: <%= tid %></span>
    </div>

    <div class="chat-popup-actions">
      <!-- 필요 시 상태 뱃지/닫기 버튼 -->
      <button type="button" class="btn btn-secondary btn-sm" id="btnClose">닫기</button>
    </div>
  </div>

  <!-- 메시지 영역(스크롤) -->
  <div class="chat-popup-messages" id="messageList">
    <div class="chat-empty text-muted">
      아직 메시지가 없습니다. (다음 단계: /admin/chat/messages 연동)
    </div>
  </div>

  <!-- 입력 영역 -->
  <div class="chat-popup-input">
    <textarea id="txtMessage" class="chat-input" rows="2" placeholder="메시지를 입력하세요..."></textarea>
    <button type="button" class="btn btn-primary" id="btnSend">전송</button>
  </div>
</div>

<script>
  // =========================
  // 팝업에서 사용할 전역 값
  // =========================
  window.__MEETING__ = window.__MEETING__ || {};
  window.__MEETING__.ctx = "<%= ctx %>";
  window.__MEETING__.threadId = "<%= tid %>";
  window.__MEETING__.popup = true;
</script>

<script src="<%= ctx %>/resources/js/admin/chat/thread.js"></script>
</body>
</html>
