<%@ page contentType="text/html; charset=UTF-8" %>
<%@ page pageEncoding="UTF-8" %>

<%
  request.setAttribute("pageTitle", "회의실 상세");
  request.setAttribute("pageCss", "room.css");

  String ctx = request.getContextPath();
  String roomId = request.getParameter("id");
  if (roomId == null) roomId = "";
%>

<jsp:include page="/WEB-INF/views/user/layout/header.jsp" />

<main class="user-container">

  <!-- ✅ 카드 1: 회의실 정보 + 액션 -->
  <section class="room-card">
    <div class="room-detail-header">
      <h2 id="roomName">회의실 상세</h2>

      <div class="room-detail-actions">
        <button type="button" class="btn btn-secondary" id="btnBackToList">목록</button>
        <button type="button" class="btn btn-primary" id="btnStartBooking">예약 시작</button>
      </div>
    </div>

    <div id="roomMeta" class="text-muted">로딩 중...</div>

    <!-- ✅ JS가 roomId를 읽을 수 있게 hidden 제공 -->
    <input type="hidden" id="roomId" value="<%= roomId %>" />
  </section>

  <!-- ✅ 카드 2: 운영시간 -->
  <section class="room-card room-detail-section">
    <div class="room-detail-subtitle">운영시간</div>
    <div id="hoursWrap">
      <div class="text-muted">운영시간을 불러오는 중...</div>
    </div>
  </section>

  <!-- ✅ 카드 3: 예약 현황(탭: 일자/달력) -->
  <section class="room-card room-detail-section">
    <div class="room-status-head">
      <div class="room-detail-subtitle" style="margin:0;">예약 현황</div>

      <div class="room-status-tabs" role="tablist" aria-label="예약현황 탭">
        <button type="button" class="tab-btn active" id="tabDay" aria-selected="true">일자</button>
        <button type="button" class="tab-btn" id="tabMonth" aria-selected="false">달력</button>
      </div>
    </div>

    <!-- 컨트롤 영역 -->
    <div class="room-status-controls">
      <!-- 일자 -->
      <div class="control-group" id="dayControl">
        <input type="date" id="statusDate" class="status-input">
        <button type="button" class="btn btn-primary" id="btnLoadDay">조회</button>
      </div>

      <!-- 달력(월) -->
      <div class="control-group" id="monthControl" style="display:none;">
        <input type="month" id="statusMonth" class="status-input">
        <button type="button" class="btn btn-primary" id="btnLoadMonth">조회</button>
      </div>
    </div>

    <!-- 안내/상태 -->
    <div id="statusHint" class="text-muted" style="margin-top:10px;">
      회의실 예약 현황을 불러오는 중...
    </div>

    <!-- 일자 타임테이블 -->
    <div id="dayTimelineWrap" style="margin-top:12px;"></div>

    <!-- 월 달력(일자별 예약건수) -->
    <div id="monthCalendarWrap" style="margin-top:12px; display:none;"></div>
  </section>

</main>

<script>
  window.__MEETING__ = window.__MEETING__ || {};
  window.__MEETING__.ctx = "<%= ctx %>";
</script>

<script src="<%= ctx %>/resources/js/user/room/detail.js"></script>

<jsp:include page="/WEB-INF/views/user/layout/footer.jsp" />
