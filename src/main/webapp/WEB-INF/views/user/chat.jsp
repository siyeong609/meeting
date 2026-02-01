<%@ page contentType="text/html; charset=UTF-8" %>
<%@ page pageEncoding="UTF-8" %>

<!DOCTYPE html>
<html lang="ko">
<head>
  <meta charset="UTF-8">
  <title>1:1 채팅문의</title>

  <!-- 공통 -->
  <link rel="stylesheet" href="${pageContext.request.contextPath}/resources/css/common.css">
  <link rel="stylesheet" href="${pageContext.request.contextPath}/resources/css/user/layout.css">

  <!-- 채팅 위젯 CSS (JS에서도 삽입하지만, fallback 페이지는 명시 로드해도 됨) -->
  <link rel="stylesheet" href="${pageContext.request.contextPath}/resources/css/user/chat-widget.css">
</head>
<body>

<script>
  // ✅ 공통 ctx 주입 (admin/member/list.js 패턴과 동일)
  window.__MEETING__ = window.__MEETING__ || {};
  window.__MEETING__.ctx = "${pageContext.request.contextPath}";

  // ✅ 이 페이지로 들어오면 위젯 자동 오픈
  window.__CHAT_WIDGET_AUTO_OPEN__ = true;
</script>

<jsp:include page="/WEB-INF/views/user/layout/header.jsp" />

<nav class="user-nav">
  <ul>
    <li><a href="${pageContext.request.contextPath}/user/dashboard">대시보드</a></li>
    <li><a href="${pageContext.request.contextPath}/user/rooms">회의실</a></li>
    <li><a href="${pageContext.request.contextPath}/user/reservations">내 예약</a></li>
    <li><a href="${pageContext.request.contextPath}/user/profile">내정보수정</a></li>
  </ul>
</nav>

<main class="user-container">
  <h2 class="section-title">1:1 채팅문의</h2>

  <div class="card" style="padding:16px;">
    <p class="text-muted" style="margin:0;">
      우측 하단 채팅창이 열립니다. (JS가 차단된 환경이면 위젯이 열리지 않을 수 있습니다.)
    </p>
  </div>
</main>

<jsp:include page="/WEB-INF/views/user/layout/footer.jsp" />

<script src="${pageContext.request.contextPath}/resources/js/common.js"></script>
<script src="${pageContext.request.contextPath}/resources/js/user/chat-widget.js"></script>

</body>
</html>
