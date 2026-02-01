<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%
  // =========================
  // 관리자 1:1 채팅창(팝업/새탭)
  // - 우선 "대화열기" 동작 확인을 위해 최소 화면만 구성
  // - 이후 messages/send API 붙이면 본 채팅 UI로 확장
  // =========================

  request.setAttribute("pageTitle", "대화창 - 관리자");
  request.setAttribute("pageCss", "chat-thread.css");
  request.setAttribute("activeMenu", "chat");

  String ctx = request.getContextPath();
  Object tid = request.getAttribute("threadId");
%>

<jsp:include page="/WEB-INF/views/admin/layout/header.jsp" />

<main class="admin-container">
  <h2 class="section-title">대화창</h2>

  <div class="card">
    <div class="card-title" style="display:flex; justify-content:space-between; align-items:center;">
      <span>Thread</span>
      <span class="text-muted">threadId: <strong><%= tid %></strong></span>
    </div>

    <div class="chat-thread-wrap">
      <div class="chat-thread-placeholder">
        <!-- ✅ 여기까지 뜨면 "대화열기" 연결은 성공 -->
        <p class="text-muted">대화창 페이지가 열렸습니다.</p>
        <p class="text-muted">다음 단계: /admin/chat/messages, /admin/chat/send 구현 후 실제 메시지 UI 연결</p>
      </div>
    </div>
  </div>
</main>

<script>
  window.__MEETING__ = window.__MEETING__ || {};
  window.__MEETING__.ctx = "<%= ctx %>";
  window.__MEETING__.threadId = "<%= tid %>";
</script>

<script src="<%= ctx %>/resources/js/admin/chat/thread.js"></script>

<jsp:include page="/WEB-INF/views/admin/layout/footer.jsp" />
