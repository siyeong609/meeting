<%@ page contentType="text/html; charset=UTF-8" %>
<%@ page pageEncoding="UTF-8" %>

<%
  String ctx = request.getContextPath();
  Object ridObj = request.getAttribute("roomId");
  String roomId = (ridObj == null) ? "" : String.valueOf(ridObj);
  request.setAttribute("activeMenu", "rooms");
%>

<jsp:include page="/WEB-INF/views/admin/layout/header.jsp" />

<!-- ✅ 이 페이지 전용 CSS를 확실히 로드 -->
<link rel="stylesheet" href="<%=ctx%>/resources/css/admin/room.css" />

<main class="admin-container">

  <!-- ✅ 카드 1: 회의실 기본 정보 -->
  <section class="room-card">
    <div class="room-detail-header">
      <h2 id="roomName">회의실 예약 현황</h2>

      <div class="room-detail-actions">
        <button type="button" class="btn btn-secondary" id="btnBackToList">목록</button>
        <button type="button" class="btn btn-primary" id="btnCreateReservation">예약 생성</button>
      </div>
    </div>

    <!-- ✅ JS가 메타(위치/수용인원/상태/슬롯/버퍼 등) 채움 -->
    <div id="roomMeta" class="text-muted">로딩 중...</div>

    <!-- ✅ JS가 roomId를 읽을 수 있게 hidden 제공 -->
    <input type="hidden" id="roomId" value="<%= roomId %>" />
  </section>

  <!-- ✅ 카드 2: 운영시간(요일별) -->
  <section class="room-card room-detail-section">
    <div class="room-detail-subtitle">운영시간</div>

    <!-- ✅ user/detail.js와 동일한 id (admin/detail.js도 동일하게 쓰면 됨) -->
    <div id="hoursWrap">
      <div class="text-muted">로딩 중...</div>
    </div>
  </section>

  <!-- ✅ 카드 3: 예약 목록(간단 리스트) -->
  <section class="room-card room-detail-section">
    <div class="room-detail-subtitle">예약 목록</div>

    <div class="user-table-wrap">
      <table class="user-table">
        <thead>
          <tr>
            <th style="width:100px;">ID</th>
            <th>제목</th>
            <th style="width:160px;">시작</th>
            <th style="width:160px;">종료</th>
            <th style="width:160px;">상태</th>
            <th style="width:200px;">관리</th>
          </tr>
        </thead>
        <tbody id="reservationTableBody">
          <tr>
            <td colspan="6" class="text-muted" style="text-align:center;">데이터를 불러오는 중...</td>
          </tr>
        </tbody>
      </table>
    </div>
  </section>

  <!-- ✅ 카드 4: 예약 현황(탭: 일자/달력) -->
  <section class="room-card room-detail-section">
    <div class="room-status-head">
      <div class="room-detail-subtitle" style="margin:0;">예약 현황</div>

      <div class="room-status-tabs" role="tablist" aria-label="예약현황 탭">
        <button type="button" class="tab-btn active" id="tabDay" aria-selected="true">일자</button>
        <button type="button" class="tab-btn" id="tabMonth" aria-selected="false">달력</button>
      </div>
    </div>

    <div class="room-status-controls">
      <div class="control-group" id="dayControl">
        <input type="date" id="statusDate" class="status-input">
        <button type="button" class="btn btn-primary" id="btnLoadDay">조회</button>
      </div>

      <div class="control-group" id="monthControl" style="display:none;">
        <input type="month" id="statusMonth" class="status-input">
        <button type="button" class="btn btn-primary" id="btnLoadMonth">조회</button>
      </div>
    </div>

    <div id="statusHint" class="text-muted" style="margin-top:10px;">
      예약 현황을 불러오는 중...
    </div>

    <div id="dayTimelineWrap" style="margin-top:12px;"></div>
    <div id="monthCalendarWrap" style="margin-top:12px; display:none;"></div>
  </section>

</main>

<script>
  window.__MEETING__ = window.__MEETING__ || {};
  window.__MEETING__.ctx = "<%= ctx %>";
</script>

<script src="<%= ctx %>/resources/js/admin/room/detail.js"></script>

<jsp:include page="/WEB-INF/views/admin/layout/footer.jsp" />
