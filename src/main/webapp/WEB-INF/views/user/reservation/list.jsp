<%@ page contentType="text/html; charset=UTF-8" %>
<%@ page pageEncoding="UTF-8" %>
<%
  request.setAttribute("pageTitle", "내 예약");
  request.setAttribute("pageCss", "room.css"); // ✅ room.css 재사용(리스트/디테일 공용)
  String ctx = request.getContextPath();
%>

<jsp:include page="/WEB-INF/views/user/layout/header.jsp" />

<main class="user-container">

  <!-- ✅ 상단 카드: 검색바(회의실 list와 동일 패턴) -->
  <section class="room-card">
    <div class="room-toolbar">
      <div class="room-toolbar-left">
        <span class="text-muted">내 예약 목록</span>
      </div>
    </div>

    <div class="room-search">
      <input type="text"
             id="searchQ"
             class="room-search-input"
             placeholder="회의실명/예약제목 검색..."
             autocomplete="off" />

      <select id="pageSize" class="room-page-size">
        <option value="10" selected>10개</option>
        <option value="20">20개</option>
        <option value="50">50개</option>
      </select>

      <button type="button"
              class="btn btn-primary room-search-btn"
              id="btnSearch">검색</button>
    </div>
  </section>

  <!-- ✅ 목록 카드 -->
  <section class="room-card room-section-gap">
    <div class="room-list-head">
      <div class="room-list-title">예약 목록</div>
      <div class="count">총 <strong id="totalElements">-</strong>개</div>
    </div>

    <div class="user-table-wrap">
      <table class="user-table">
        <thead>
        <tr>
          <th style="width:80px;">상태</th>
          <th>회의실</th>
          <th>예약시간</th>
          <th>제목</th>
          <th style="width:160px;">신청일</th>
          <th style="width:120px;">관리</th>
        </tr>
        </thead>
        <tbody id="tableBody">
        <tr>
          <td colspan="6" class="text-center text-muted">데이터를 불러오는 중...</td>
        </tr>
        </tbody>
      </table>
    </div>

    <div id="pagination"></div>
  </section>

</main>

<script>
  // ✅ ctx 공통 주입
  window.__MEETING__ = window.__MEETING__ || {};
  window.__MEETING__.ctx = "<%= ctx %>";
</script>

<script src="<%= ctx %>/resources/js/user/reservation/list.js"></script>

<jsp:include page="/WEB-INF/views/user/layout/footer.jsp" />
