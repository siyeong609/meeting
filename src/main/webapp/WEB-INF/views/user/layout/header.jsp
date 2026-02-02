<%@ page contentType="text/html; charset=UTF-8" %>
<%@ page pageEncoding="UTF-8" %>
<%@ page import="com.company.meeting.user.dto.UserDTO" %>

<%
  /**
   * ✅ 유저 레이아웃 헤더
   * - 문서 시작(doctype/head/body) + 헤더 + 유저 네비 포함
   * - 각 페이지는 "컨텐츠만" 작성 후 footer.jsp에서 문서 종료
   *
   * ✅ 확장 포인트
   * - pageTitle: 서블릿에서 req.setAttribute("pageTitle", "...")로 주입
   * - pageCss:   서블릿/JSP에서 req.setAttribute("pageCss", "room.css")로 주입
   */

  String ctx = request.getContextPath();
  String uri = request.getRequestURI();
  String path = uri.startsWith(ctx) ? uri.substring(ctx.length()) : uri;

  boolean isDashboard = path.startsWith("/user/dashboard");
  boolean isRooms = path.startsWith("/user/rooms");
  boolean isReservations = path.startsWith("/user/reservations");
  boolean isProfile = path.startsWith("/user/profile");
  boolean isChat = path.startsWith("/user/chat");

  String pageTitle = (String) request.getAttribute("pageTitle");
  if (pageTitle == null || pageTitle.isBlank()) pageTitle = "회의실 예약";

  // ✅ 페이지 전용 CSS 파일명(user 하위)
  // 예: "room.css" -> /resources/css/user/room.css
  String pageCss = (String) request.getAttribute("pageCss");

  UserDTO loginUser = (UserDTO) session.getAttribute("LOGIN_USER");

  // ✅ 표시 라벨 (DTO 게터 미확정 대응: 리플렉션)
  String loginLabel = "-";
  if (loginUser != null) {
    try {
      java.lang.reflect.Method m = null;
      try { m = loginUser.getClass().getMethod("getUserId"); } catch (Exception ignore) {}
      if (m == null) { try { m = loginUser.getClass().getMethod("getLoginId"); } catch (Exception ignore) {} }
      if (m == null) { try { m = loginUser.getClass().getMethod("getId"); } catch (Exception ignore) {} }

      if (m != null) {
        Object v = m.invoke(loginUser);
        if (v != null) loginLabel = String.valueOf(v);
      } else {
        loginLabel = loginUser.toString();
      }
    } catch (Exception ignore) {
      loginLabel = "USER";
    }
  }

  // ✅ 프로필 이미지 URL 구성
  String defaultProfile = ctx + "/resources/uploads/profile/default-profile.svg";

  String profileImage = null;
  if (loginUser != null) {
    try {
      java.lang.reflect.Method pm = null;
      try { pm = loginUser.getClass().getMethod("getProfileImage"); } catch (Exception ignore) {}
      if (pm == null) { try { pm = loginUser.getClass().getMethod("getProfile_image"); } catch (Exception ignore) {} }

      if (pm != null) {
        Object pv = pm.invoke(loginUser);
        if (pv != null) {
          String s = String.valueOf(pv).trim();
          if (!s.isEmpty()) profileImage = s;
        }
      }
    } catch (Exception ignore) {
      profileImage = null;
    }
  }

  String profileUrl = defaultProfile;
  if (profileImage != null) {
    if (profileImage.startsWith("http://") || profileImage.startsWith("https://")) {
      profileUrl = profileImage;
    } else {
      if (!profileImage.startsWith("/")) profileImage = "/" + profileImage;
      profileUrl = ctx + profileImage;
    }
  }
%>

<!DOCTYPE html>
<html lang="ko">
<head>
  <meta charset="UTF-8" />
  <title><%= pageTitle %></title>

  <!-- ✅ 공통 CSS -->
  <link rel="stylesheet" href="<%= ctx %>/resources/css/common.css" />
  <link rel="stylesheet" href="<%= ctx %>/resources/css/user/layout.css" />

  <!-- ✅ 페이지 전용 CSS (선택) -->
  <%
    if (pageCss != null && !pageCss.isBlank()) {
  %>
  <link rel="stylesheet" href="<%= ctx %>/resources/css/user/<%= pageCss %>" />
  <%
    }
  %>
</head>
<body>

<header class="user-header">
  <h1>회의실 예약</h1>

  <div class="user-info">
    <div class="user-account" id="userAccount">
      <img class="user-avatar"
           src="<%= profileUrl %>"
           alt="profile"
           onerror="this.onerror=null; this.src='<%= defaultProfile %>';" />

      <a href="javascript:void(0)"
         class="user-account-btn"
         id="userAccountBtn"
         aria-haspopup="true"
         aria-expanded="false">
        <span><%= loginLabel %></span>
        <span class="user-caret"></span>
      </a>

      <div class="user-dropdown" id="userDropdown" role="menu" aria-label="user menu">
        <a href="<%= ctx %>/user/profile" role="menuitem" class="<%= isProfile ? "active" : "" %>">내정보수정</a>
        <a href="<%= ctx %>/user/reservations" role="menuitem" class="<%= isReservations ? "active" : "" %>">내예약확인</a>

        <a href="javascript:void(0);"
           id="menuChatLink"
           role="menuitem"
           class="<%= isChat ? "active" : "" %>">
          1:1채팅문의
        </a>

        <div class="divider"></div>
        <a href="<%= ctx %>/user/logout" class="logout" role="menuitem">로그아웃</a>
      </div>
    </div>
  </div>
</header>

<nav class="user-nav">
  <ul>
    <li><a href="<%= ctx %>/user/reservations" class="<%= isReservations ? "active" : "" %>">내 예약</a></li>
    <li><a href="<%= ctx %>/user/rooms" class="<%= isRooms ? "active" : "" %>">회의실</a></li>
  </ul>
</nav>

<script>
  /*
    ✅ 드롭다운 + 채팅 위젯 오픈
    - 버튼 클릭: 토글
    - 바깥 클릭: 닫기
    - ESC: 닫기
    - 채팅 클릭: 드롭다운 닫고 위젯 오픈
  */
  (function () {
    const btn = document.getElementById('userAccountBtn');
    const dd = document.getElementById('userDropdown');
    const chatLink = document.getElementById('menuChatLink');

    if (!btn || !dd) return;

    function open() {
      dd.classList.add('open');
      btn.setAttribute('aria-expanded', 'true');
    }

    function close() {
      dd.classList.remove('open');
      btn.setAttribute('aria-expanded', 'false');
    }

    function toggle() {
      if (dd.classList.contains('open')) close();
      else open();
    }

    btn.addEventListener('click', function (e) {
      e.preventDefault();
      e.stopPropagation();
      toggle();
    });

    dd.addEventListener('click', function (e) {
      e.stopPropagation();
    });

    document.addEventListener('click', function () {
      close();
    });

    document.addEventListener('keydown', function (e) {
      if (e.key === 'Escape') close();
    });

    if (chatLink) {
      chatLink.addEventListener('click', function (e) {
        e.preventDefault();
        e.stopPropagation();
        close();

        if (typeof window.openChatWidget === 'function') {
          window.openChatWidget();
        } else {
          console.warn('openChatWidget not found');
        }
      });
    }
  })();
</script>
