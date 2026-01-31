<%@ page contentType="text/html; charset=UTF-8" %>
<%@ page pageEncoding="UTF-8" %>
<%
  // 레이아웃에 전달할 값 세팅
  request.setAttribute("pageTitle", "관리자 대시보드 - 회의실 예약");
  request.setAttribute("pageCss", "dashboard.css"); // /resources/css/admin/dashboard.css
  request.setAttribute("activeMenu", "dashboard");  // nav active 처리
%>

<jsp:include page="/WEB-INF/views/admin/layout/header.jsp" />

<main class="admin-container">
  <h2 class="section-title">대시보드</h2>

  <div class="dashboard-grid">
    <div class="dashboard-card">
      <div class="dashboard-card-title">전체 회원 수</div>
      <div class="dashboard-card-value">${totalUserCount}</div>
      <div class="dashboard-card-change">기준일 누적</div>
    </div>

    <div class="dashboard-card">
      <div class="dashboard-card-title">등록된 회의실</div>
      <div class="dashboard-card-value">0</div>
    </div>

    <div class="dashboard-card">
      <div class="dashboard-card-title">오늘 예약 건수</div>
      <div class="dashboard-card-value">0</div>
    </div>

    <div class="dashboard-card">
      <div class="dashboard-card-title">이번 달 예약 건수</div>
      <div class="dashboard-card-value">0</div>
    </div>
  </div>
</main>

<jsp:include page="/WEB-INF/views/admin/layout/footer.jsp" />
