<%@ page contentType="text/html; charset=UTF-8" %>
<%@ page pageEncoding="UTF-8" %>
<%@ page import="com.company.meeting.user.dto.UserDTO" %>

<%
  /**
   * ✅ 레이아웃 헤더
   * - 문서 시작(doctype/head/body) + 헤더 + 유저 네비까지 포함
   * - 각 페이지는 "컨텐츠만" 작성 후 footer.jsp에서 닫는다.
   *
   * ✅ 주의
   * - JSP 내장 객체 session을 그대로 사용(중복 session 변수 선언 금지)
   */

  // 컨텍스트/경로
  String ctx = request.getContextPath();
  String uri = request.getRequestURI();
  String path = uri.startsWith(ctx) ? uri.substring(ctx.length()) : uri;

  // ✅ active 메뉴 처리(필요한 메뉴만 추가)
  boolean isDashboard = path.startsWith("/user/dashboard");
  boolean isRooms = path.startsWith("/user/rooms");
  boolean isReservations = path.startsWith("/user/reservations");
  boolean isProfile = path.startsWith("/user/profile");
  boolean isChat = path.startsWith("/user/chat");

  // ✅ title은 서블릿에서 req.setAttribute("pageTitle", "...")로 주입 가능
  String pageTitle = (String) request.getAttribute("pageTitle");
  if (pageTitle == null || pageTitle.isBlank()) pageTitle = "회의실 예약";

  // 로그인 유저
  UserDTO loginUser = (UserDTO) session.getAttribute("LOGIN_USER");

  // ✅ 표시 라벨 (DTO 게터가 확정 전이라 리플렉션으로 안전 처리)
  // 우선순위: getUserId() -> getLoginId() -> getId() -> toString()
  String loginLabel = "-";
  if (loginUser != null) {
    try {
      java.lang.reflect.Method m = null;

      try { m = loginUser.getClass().getMethod("getUserId"); } catch (Exception ignore) {}
      if (m == null) {
        try { m = loginUser.getClass().getMethod("getLoginId"); } catch (Exception ignore) {}
      }
      if (m == null) {
        try { m = loginUser.getClass().getMethod("getId"); } catch (Exception ignore) {}
      }

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
  // - getProfileImage() -> getProfile_image() -> null
  // - null/blank면 default-profile.svg
  String defaultProfile = ctx + "/resources/uploads/profile/default-profile.svg";

  String profileImage = null;
  if (loginUser != null) {
    try {
      java.lang.reflect.Method pm = null;

      try { pm = loginUser.getClass().getMethod("getProfileImage"); } catch (Exception ignore) {}
      if (pm == null) {
        try { pm = loginUser.getClass().getMethod("getProfile_image"); } catch (Exception ignore) {}
      }

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

  // 최종 URL 결정(외부 URL 허용, 내부 경로면 ctx 붙이기)
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
</head>
<body>

<header class="user-header">
  <h1>회의실 예약</h1>

  <!-- ✅ 우측: 프로필 + 아이디(드롭다운 트리거) -->
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

      <!-- ✅ 드롭다운 메뉴 -->
      <div class="user-dropdown" id="userDropdown" role="menu" aria-label="user menu">
        <a href="<%= ctx %>/user/profile" role="menuitem" class="<%= isProfile ? "active" : "" %>">내정보수정</a>
        <a href="<%= ctx %>/user/reservations" role="menuitem" class="<%= isReservations ? "active" : "" %>">내예약확인</a>

        <!-- ✅ 채팅: 위젯만 뜨게 할 거지만, JS 실패 시 fallback 페이지(/user/chat)로 이동 -->
        <a href="javascript:void(0);"
           id="menuChatLink"
           role="menuitem"
           class="<%= isChat ? "active" : "" %>">
          1:1채팅문의
        </a>

        <div class="divider"></div>

        <!-- ✅ 로그아웃은 드롭다운 안에 포함(네가 고민한 부분은 이 쪽이 UX 깔끔함) -->
        <a href="<%= ctx %>/user/logout" class="logout" role="menuitem">로그아웃</a>
      </div>
    </div>
  </div>
</header>

<!-- ✅ 네비게이션: header에 포함(요구사항 반영) -->
<nav class="user-nav">
  <ul>
    <li>
      <a href="<%= ctx %>/user/dashboard" class="<%= isDashboard ? "active" : "" %>">대시보드</a>
    </li>
    <li>
      <a href="<%= ctx %>/user/rooms" class="<%= isRooms ? "active" : "" %>">회의실</a>
    </li>
    <li>
      <a href="<%= ctx %>/user/reservations" class="<%= isReservations ? "active" : "" %>">내 예약</a>
    </li>
  </ul>
</nav>

<script>
  /*
    ✅ 드롭다운 + 채팅 위젯 오픈 로직
    - 버튼 클릭: 열기/닫기
    - 바깥 클릭: 닫기
    - ESC: 닫기
    - 채팅 클릭: 드롭다운 닫고 위젯 열기
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

    // 드롭다운 토글
    btn.addEventListener('click', function (e) {
      e.preventDefault();
      e.stopPropagation();
      toggle();
    });

    // 드롭다운 내부 클릭은 닫힘 방지
    dd.addEventListener('click', function (e) {
      e.stopPropagation();
    });

    // 바깥 클릭 시 닫기
    document.addEventListener('click', function () {
      close();
    });

    // ESC 닫기
    document.addEventListener('keydown', function (e) {
      if (e.key === 'Escape') close();
    });

    // ✅ 채팅 클릭: 드롭다운 닫고 위젯 열기
    if (chatLink) {
      chatLink.addEventListener('click', function (e) {
        e.preventDefault();
        e.stopPropagation();

        close();

        if (typeof window.openChatWidget === 'function') {
          window.openChatWidget();
        } else {
          // ✅ 여기 로그가 뜨면 footer에서 chat-widget.js 로드가 안 된 것
          console.warn('openChatWidget not found');
        }
      });
    }
  })();
</script>

