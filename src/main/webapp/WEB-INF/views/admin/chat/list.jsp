<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%
  request.setAttribute("pageTitle", "대화관리 - 관리자");
  request.setAttribute("pageCss", "chat.css");
  request.setAttribute("activeMenu", "chat");

  String ctx = request.getContextPath();
%>

<jsp:include page="/WEB-INF/views/admin/layout/header.jsp" />

<main class="admin-container">
  <h2 class="section-title">대화관리</h2>

  <div class="card mb-2">
    <div class="chat-toolbar">
      <div class="chat-toolbar-left">
        <span class="text-muted">1:1 문의 대화(스레드) 목록 관리</span>
      </div>
    </div>

    <div class="chat-search">
      <input type="text"
             id="searchUser"
             class="chat-search-input"
             placeholder="회원 아이디로 검색..."
             autocomplete="off"/>

      <span class="status-wrap">
        <select id="statusFilter" class="chat-status">
          <option value="" selected>전체</option>
          <option value="OPEN">OPEN</option>
          <option value="CLOSED">CLOSED</option>
        </select>
      </span>

      <span class="page-size-wrap">
        <select id="pageSize" class="chat-page-size">
          <option value="10" selected>10개</option>
          <option value="20">20개</option>
          <option value="50">50개</option>
        </select>
      </span>

      <button type="button"
              class="btn btn-primary chat-search-btn"
              id="btnSearchChat">검색</button>
    </div>
  </div>

  <div class="card">
    <div class="card-title" style="display:flex; justify-content:space-between; align-items:center;">
      <span>대화(스레드) 목록</span>
      <span class="text-muted">총 <strong id="totalElements">-</strong>개</span>
    </div>

    <div class="table-wrap">
      <table>
        <thead>
        <tr>
          <th style="width:180px;">회원ID</th>
          <th style="width:120px;">상태</th>
          <th>최근메시지</th>
          <th style="width:180px;">최근시간</th>
          <th style="width:180px;">생성일</th>
          <th style="width:160px;">관리</th>
        </tr>
        </thead>

        <tbody id="chatTableBody">
        <tr>
          <td colspan="6" class="text-center text-muted">데이터를 불러오는 중...</td>
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

<script src="<%= ctx %>/resources/js/admin/chat/list.js"></script>

<jsp:include page="/WEB-INF/views/admin/layout/footer.jsp" />
