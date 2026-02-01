<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%
  request.setAttribute("pageTitle", "회의실관리 - 관리자");
  request.setAttribute("pageCss", "room.css");
  request.setAttribute("activeMenu", "rooms");

  String ctx = request.getContextPath();
%>

<jsp:include page="/WEB-INF/views/admin/layout/header.jsp" />

<main class="admin-container">
  <h2 class="section-title">회의실관리</h2>

  <div class="card mb-2">
    <div class="room-toolbar">
      <div class="room-toolbar-left">
        <span class="text-muted">회의실 목록 관리</span>
      </div>
      <div class="room-toolbar-right">
        <button type="button" class="btn btn-primary" id="btnCreateRoom">회의실생성</button>
        <button type="button" class="btn btn-secondary" id="btnDeleteSelectedRoom">회의실삭제</button>
      </div>
    </div>

    <!-- ✅ 검색바: input (드롭다운) 버튼 -->
    <div class="room-search">
      <input type="text"
             id="searchRoom"
             class="room-search-input"
             placeholder="회의실명, 위치로 검색..."
             autocomplete="off"/>

      <span class="page-size-wrap">
        <select id="pageSize" class="room-page-size">
          <option value="10" selected>10개</option>
          <option value="20">20개</option>
          <option value="50">50개</option>
        </select>
      </span>

      <button type="button"
              class="btn btn-primary room-search-btn"
              id="btnSearchRoom">검색</button>
    </div>
  </div>

  <div class="card">
    <div class="card-title" style="display:flex; justify-content:space-between; align-items:center;">
      <span>회의실 목록</span>
      <span class="text-muted">총 <strong id="totalElements">-</strong>개</span>
    </div>

    <div class="table-wrap">
      <table>
        <thead>
        <tr>
          <th style="width:56px; text-align:center;">
            <input type="checkbox" id="chkAll">
          </th>
          <th>회의실명</th>
          <th style="width:120px;">수용인원</th>
          <th>위치</th>
          <th style="width:120px;">사용여부</th>
          <th style="width:160px;">등록일</th>
          <th style="width:180px;">관리</th>
        </tr>
        </thead>
        <tbody id="roomTableBody">
        <tr>
          <td colspan="7" class="text-center text-muted">데이터를 불러오는 중...</td>
        </tr>
        </tbody>
      </table>
    </div>

    <div id="pagination"></div>
  </div>
</main>

<script>
  window.__MEETING__ = window.__MEETING__ || {};
  window.__MEETING__.ctx = "<%= ctx %>";
</script>

<script src="<%= ctx %>/resources/js/admin/room/list.js"></script>

<jsp:include page="/WEB-INF/views/admin/layout/footer.jsp" />
