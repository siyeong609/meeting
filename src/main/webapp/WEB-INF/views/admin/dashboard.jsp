<%@ page contentType="text/html; charset=UTF-8" %>
<%@ page pageEncoding="UTF-8" %>

<!DOCTYPE html>
<html lang="ko">
<head>
  <meta charset="UTF-8">
  <title>관리자 대시보드 - 회의실 예약</title>

  <!-- 공통 CSS -->
  <link rel="stylesheet"
        href="${pageContext.request.contextPath}/resources/css/common.css">

  <!-- 관리자 레이아웃 -->
  <link rel="stylesheet"
        href="${pageContext.request.contextPath}/resources/css/admin/layout.css">

  <!-- 관리자 대시보드 -->
  <link rel="stylesheet"
        href="${pageContext.request.contextPath}/resources/css/admin/dashboard.css">
</head>
<body>

<header class="admin-header">
  <h1>회의실 예약 시스템 - 관리자</h1>
  <div class="user-info">
    <span>${sessionScope.LOGIN_ADMIN.name}님</span>
    <a href="${pageContext.request.contextPath}/admin/logout"
       class="btn btn-outline mt-1">
      로그아웃
    </a>
  </div>
</header>

<nav class="admin-nav">
  <ul>
    <li><a href="${pageContext.request.contextPath}/admin/dashboard"
           class="active">대시보드</a></li>
    <li><a href="${pageContext.request.contextPath}/admin/members">회원관리</a></li>
    <li><a href="${pageContext.request.contextPath}/admin/rooms">회의실 관리</a></li>
    <li><a href="${pageContext.request.contextPath}/admin/chat">채팅관리</a></li>
  </ul>
</nav>

<main class="admin-container">
  <h2 class="section-title">대시보드</h2>

  <!-- 일단 화면만 유지 (데이터는 다음 단계) -->
  <div class="dashboard-grid">
    <div class="dashboard-card">
        <div class="dashboard-card-title">전체 회원 수</div>
        <div class="dashboard-card-value">
            ${totalUserCount}
        </div>
        <div class="dashboard-card-change">
            기준일 누적
        </div>
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

</body>
</html>
