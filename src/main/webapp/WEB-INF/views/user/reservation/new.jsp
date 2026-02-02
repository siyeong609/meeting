<%@ page contentType="text/html; charset=UTF-8" %>
<%@ page pageEncoding="UTF-8" %>
<%
  request.setAttribute("pageTitle", "예약 생성");
  String ctx = request.getContextPath();

  String roomId = request.getParameter("roomId");
  if (roomId == null) roomId = "";
%>

<jsp:include page="/WEB-INF/views/user/layout/header.jsp" />

<main class="user-container">

  <section class="room-card reservation-card">
    <div class="reservation-head">
      <h2 style="margin:0; font-size:18px; font-weight:900;">예약 생성</h2>
      <div class="reservation-actions">
        <button type="button" class="btn btn-secondary" id="btnBack">뒤로</button>
        <button type="button" class="btn btn-primary" id="btnCreate">예약하기</button>
      </div>
    </div>

    <input type="hidden" id="roomId" value="<%= roomId %>" />

    <div class="reservation-grid">
      <div class="reservation-label">회의실</div>
      <div class="reservation-value" id="roomInfo">로딩 중...</div>

      <div class="reservation-label">예약일</div>
      <div class="reservation-value">
        <input type="date" id="dateInput" class="reservation-input" />
      </div>

      <div class="reservation-label">시작시간</div>
      <div class="reservation-value">
        <select id="timeSelect" class="reservation-input"></select>
      </div>

      <div class="reservation-label">예약시간</div>
      <div class="reservation-value">
        <select id="durationSelect" class="reservation-input"></select>
      </div>

      <div class="reservation-label">제목(선택)</div>
      <div class="reservation-value">
        <input type="text" id="titleInput" class="reservation-input" placeholder="예: 주간 회의" maxlength="200"/>
      </div>
    </div>

    <div class="text-muted" style="margin-top:12px; font-size:12px;">
      * 운영시간/버퍼/슬롯 규칙은 서버에서 최종 검증됩니다.
    </div>
  </section>

</main>

<script>
  window.__MEETING__ = window.__MEETING__ || {};
  window.__MEETING__.ctx = "<%= ctx %>";
</script>

<script src="<%= ctx %>/resources/js/user/reservation/new.js"></script>

<jsp:include page="/WEB-INF/views/user/layout/footer.jsp" />
