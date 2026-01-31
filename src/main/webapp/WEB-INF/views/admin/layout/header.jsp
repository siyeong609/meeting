<%@ page contentType="text/html; charset=UTF-8" %>
<%@ page pageEncoding="UTF-8" %>
<%
  // 레이아웃에서 공통으로 쓸 ctx
  String ctx = request.getContextPath();
%>
<!DOCTYPE html>
<html lang="ko">
<head>
  <meta charset="UTF-8">
  <title><%= (request.getAttribute("pageTitle") != null) ? request.getAttribute("pageTitle") : "관리자" %></title>

  <!-- 공통 CSS -->
  <link rel="stylesheet" href="<%= ctx %>/resources/css/common.css">

  <!-- 관리자 레이아웃 CSS -->
  <link rel="stylesheet" href="<%= ctx %>/resources/css/admin/layout.css">

  <!-- 페이지 전용 CSS (필요한 페이지에서 pageCss를 세팅해서 사용) -->
  <%
    String pageCss = (String) request.getAttribute("pageCss");
    if (pageCss != null && !pageCss.isBlank()) {
  %>
  <link rel="stylesheet" href="<%= ctx %>/resources/css/admin/<%= pageCss %>">
  <%
    }
  %>
</head>
<body>

<header class="admin-header">
  <h1>회의실 예약 시스템 - 관리자</h1>
  <div class="user-info">
    <!-- 세션에 LOGIN_ADMIN(UserDTO)이 들어있다는 전제 -->
    <span>${sessionScope.LOGIN_ADMIN.name}님</span>
    <a href="<%= ctx %>/admin/logout" class="btn btn-outline mt-1">로그아웃</a>
  </div>
</header>

<nav class="admin-nav">
  <ul>
    <li>
      <a href="<%= ctx %>/admin/dashboard"
         class="<%= "dashboard".equals(request.getAttribute("activeMenu")) ? "active" : "" %>">
        대시보드
      </a>
    </li>
    <li>
      <a href="<%= ctx %>/admin/members"
         class="<%= "members".equals(request.getAttribute("activeMenu")) ? "active" : "" %>">
        회원관리
      </a>
    </li>
    <li>
      <a href="<%= ctx %>/admin/rooms"
         class="<%= "rooms".equals(request.getAttribute("activeMenu")) ? "active" : "" %>">
        회의실 관리
      </a>
    </li>
    <li>
      <a href="<%= ctx %>/admin/chat"
         class="<%= "chat".equals(request.getAttribute("activeMenu")) ? "active" : "" %>">
        채팅관리
      </a>
    </li>
  </ul>
</nav>
