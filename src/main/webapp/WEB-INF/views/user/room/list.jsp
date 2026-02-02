<%@ page contentType="text/html; charset=UTF-8" %>
<%@ page pageEncoding="UTF-8" %>

<%
  request.setAttribute("pageTitle", "회의실 목록");
  request.setAttribute("pageCss", "room.css");
  String ctx = request.getContextPath();
%>

<jsp:include page="/WEB-INF/views/user/layout/header.jsp" />

<main class="user-container">

  <h2 class="section-title">회의실</h2>

  <!-- ✅ 상단 검색 카드 (admin 룸 목록과 동일한 패턴) -->
  <div class="room-card" style="margin-top: 12px;">
    <div class="room-toolbar">
      <div class="room-toolbar-left">
        <span class="text-muted">회의실 목록 관리</span>
      </div>

      <!-- ✅ 우측 버튼 영역은 제거(요구사항) -->
      <div class="room-toolbar-right">
        <%-- empty --%>
      </div>
    </div>

    <div class="room-search">
      <input type="text"
             id="searchRoom"
             class="room-search-input"
             placeholder="회의실명, 위치로 검색..."
             autocomplete="off" />

      <select id="pageSize" class="room-page-size">
        <option value="10" selected>10개</option>
        <option value="20">20개</option>
        <option value="50">50개</option>
      </select>

      <button type="button"
              class="btn btn-primary room-search-btn"
              id="btnSearchRoom">검색</button>
    </div>
  </div>

  <!-- ✅ 목록 카드 -->
  <div class="room-card" style="margin-top: 16px;">
    <div class="room-list-head">
      <div style="font-weight: 900;">회의실 목록</div>
      <div class="count">총 <strong id="totalElements">-</strong>개</div>
    </div>

    <div class="user-table-wrap">
      <table class="user-table">
        <thead>
          <tr>
            <th>회의실명</th>
            <th style="width: 120px;">수용인원</th>
            <th>위치</th>
            <th style="width: 120px;">상태</th>
            <th style="width: 140px;">이동</th>
          </tr>
        </thead>
        <tbody id="roomTableBody">
          <tr>
            <td colspan="5" class="text-muted" style="text-align:center;">데이터를 불러오는 중...</td>
          </tr>
        </tbody>
      </table>
    </div>

    <div id="pagination"></div>
  </div>

  <!-- ✅ 예약 현황 카드(탭: 일자/달력) -->
  <section class="room-card" style="margin-top:14px;">
    <div class="room-status-head">
      <div style="font-weight:900;">
        예약 현황
        <span class="text-muted" style="font-weight:700; margin-left:8px;" id="statusRoomLabel"></span>
      </div>

      <div class="room-status-tabs">
        <button type="button" class="tab-btn active" id="tabDay">일자</button>
        <button type="button" class="tab-btn" id="tabMonth">달력</button>
      </div>
    </div>

    <!-- 컨트롤 영역 -->
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

    <!-- 뷰 -->
    <div id="dayView">
      <div id="timetableWrap" style="margin-top:10px;"></div>
    </div>

    <div id="monthView" style="display:none;">
      <div id="calendarWrap" style="margin-top:10px;"></div>
    </div>
  </section>


</main>

<script>
  // ✅ ctx 공통 주입
  window.__MEETING__ = window.__MEETING__ || {};
  window.__MEETING__.ctx = "<%= ctx %>";
</script>

<script src="<%= ctx %>/resources/js/user/room/list.js"></script>

<jsp:include page="/WEB-INF/views/user/layout/footer.jsp" />
