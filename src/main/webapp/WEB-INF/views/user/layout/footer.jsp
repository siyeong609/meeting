<%@ page contentType="text/html; charset=UTF-8" %>
<%@ page pageEncoding="UTF-8" %>

<footer style="padding: 24px 32px; color: var(--text-muted); font-size: 12px;">
  © Meeting Reservation
</footer>

<script>
  // ✅ ctx 공통 주입
  window.__MEETING__ = window.__MEETING__ || {};
  window.__MEETING__.ctx = "${pageContext.request.contextPath}";
</script>

<!-- ✅ 공통 JS -->
<script src="${pageContext.request.contextPath}/resources/js/common.js"></script>

<!-- ✅ 채팅 위젯 JS -->
<script src="${pageContext.request.contextPath}/resources/js/user/chat-widget.js"></script>

</body>
</html>
