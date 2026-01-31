<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%
  request.setAttribute("pageTitle", "회원관리 - 관리자");
  request.setAttribute("pageCss", "member.css");
  request.setAttribute("activeMenu", "members");

  String ctx = request.getContextPath();
%>

<jsp:include page="/WEB-INF/views/admin/layout/header.jsp" />

<main class="admin-container">
  <h2 class="section-title">회원관리</h2>

  <div class="card mb-2">
    <div class="member-toolbar">
      <div class="member-toolbar-left">
        <span class="text-muted">회원 목록 관리</span>
      </div>
      <div class="member-toolbar-right">
        <button type="button" class="btn btn-primary" id="btnCreate">회원생성</button>
        <button type="button" class="btn btn-secondary" id="btnDeleteSelected">회원삭제</button>
      </div>
    </div>

    <!-- ✅ 검색바: input (드롭다운) 버튼 -->
    <div class="member-search">
      <input type="text"
             id="searchMember"
             class="member-search-input"
             placeholder="아이디, 이름, 이메일로 검색..."
             autocomplete="off"/>

      <span class="page-size-wrap">
        <select id="pageSize" class="member-page-size">
          <option value="10" selected>10개</option>
          <option value="20">20개</option>
          <option value="50">50개</option>
        </select>
      </span>

      <button type="button"
              class="btn btn-primary member-search-btn"
              id="btnSearch">검색</button>
    </div>
  </div>

  <div class="card">
    <div class="card-title" style="display:flex; justify-content:space-between; align-items:center;">
      <span>회원 목록</span>
      <span class="text-muted">총 <strong id="totalElements">-</strong>명</span>
    </div>

    <div class="table-wrap">
      <table>
        <thead>
        <tr>
          <th style="width:56px; text-align:center;">
            <input type="checkbox" id="chkAll">
          </th>
          <th>아이디</th>
          <th>이름</th>
          <th>이메일</th>
          <th>가입일</th>
          <th style="width:120px;">관리</th>
        </tr>
        </thead>
        <tbody id="memberTableBody">
        <tr>
          <td colspan="6" class="text-center text-muted">데이터를 불러오는 중...</td>
        </tr>
        </tbody>
      </table>
    </div>

    <div id="pagination"></div>
  </div>
</main>

<!-- ✅ JSP는 "설정값"만 주고, 실제 로직은 list.js로 -->
<script>
  window.__MEETING__ = window.__MEETING__ || {};
  window.__MEETING__.ctx = "<%= ctx %>";
</script>

<script src="<%= ctx %>/resources/js/admin/member/list.js"></script>

<jsp:include page="/WEB-INF/views/admin/layout/footer.jsp" />
