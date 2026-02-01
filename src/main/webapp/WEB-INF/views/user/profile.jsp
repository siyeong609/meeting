<%@ page contentType="text/html; charset=UTF-8" %>
<%@ page pageEncoding="UTF-8" %>
<%@ page import="com.company.meeting.user.dto.UserDTO" %>
<%@ page import="java.lang.reflect.Method" %>

<%!
  // ✅ HTML Escape (JSTL 없이 안전하게 렌더링)
  private String esc(String s) {
    if (s == null) return "";
    return s.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;");
  }

  private String tryGetString(Object obj, String methodName) {
    if (obj == null) return null;
    try {
      Method m = obj.getClass().getMethod(methodName);
      Object v = m.invoke(obj);
      return v == null ? null : String.valueOf(v);
    } catch (Exception ignore) {
      return null;
    }
  }

  private String firstNonEmpty(String... arr) {
    if (arr == null) return null;
    for (String s : arr) {
      if (s != null && !s.trim().isEmpty()) return s.trim();
    }
    return null;
  }
%>

<%
  // ✅ JSP 내장 session 사용
  UserDTO loginUser = (UserDTO) session.getAttribute("LOGIN_USER");

  String ctx = request.getContextPath();
  String defaultDb = "/resources/uploads/profile/default-profile.svg";
  String defaultUrl = ctx + defaultDb;

  // 표시 라벨(아이디)
  String loginId = firstNonEmpty(
          tryGetString(loginUser, "getLoginId"),
          tryGetString(loginUser, "getUserId"),
          tryGetString(loginUser, "getId")
  );
  if (loginId == null) loginId = "-";

  // 이름/이메일
  String name = firstNonEmpty(tryGetString(loginUser, "getName"));
  if (name == null) name = "-";

  String email = firstNonEmpty(tryGetString(loginUser, "getEmail"));
  if (email == null) email = "";

  // 프로필 이미지(DB 경로)
  String profileDb = firstNonEmpty(
          tryGetString(loginUser, "getProfileImage"),
          tryGetString(loginUser, "getProfile_image")
  );

  boolean hasCustomProfile = (profileDb != null && !profileDb.isEmpty());
  String profileUrl = hasCustomProfile ? (profileDb.startsWith("http") ? profileDb : (ctx + (profileDb.startsWith("/") ? profileDb : ("/" + profileDb)))) : defaultUrl;

  String fileName = "";
  if (hasCustomProfile) {
    int idx = profileDb.lastIndexOf("/");
    fileName = (idx >= 0) ? profileDb.substring(idx + 1) : profileDb;
  }
%>

<!DOCTYPE html>
<html lang="ko">
<head>
  <meta charset="UTF-8">
  <title>내정보수정 - 회의실 예약</title>

  <link rel="stylesheet" href="${pageContext.request.contextPath}/resources/css/common.css">
  <link rel="stylesheet" href="${pageContext.request.contextPath}/resources/css/user/layout.css">
</head>
<body>

<script>
  // ✅ admin/member/list.js 패턴과 동일하게 ctx 제공
  window.__MEETING__ = window.__MEETING__ || {};
  window.__MEETING__.ctx = "<%= ctx %>";
</script>

<jsp:include page="/WEB-INF/views/user/layout/header.jsp" />

<nav class="user-nav">
  <ul>
    <li><a href="${pageContext.request.contextPath}/user/dashboard">대시보드</a></li>
    <li><a href="${pageContext.request.contextPath}/user/rooms">회의실</a></li>
    <li><a href="${pageContext.request.contextPath}/user/reservations">내 예약</a></li>
  </ul>
</nav>

<main class="user-container">
  <h2 class="section-title">내정보수정</h2>

  <div class="card" style="padding:16px;">
    <div style="display:flex; gap:16px; align-items:flex-start; flex-wrap:wrap;">

      <!-- 프로필 영역 -->
      <div style="min-width:220px;">
        <div style="font-weight:700; margin-bottom:10px;">프로필 이미지</div>

        <div style="width:120px; height:120px; border-radius:999px; overflow:hidden; border:1px solid var(--border); background:#fff;">
          <img id="profilePreview"
               src="<%= profileUrl %>"
               alt="profile"
               style="width:100%; height:100%; object-fit:cover;"
               onerror="this.onerror=null; this.src='<%= defaultUrl %>';">
        </div>

        <div class="form-group" style="margin-top:12px; display: grid;">
          <label>이미지 업로드</label>
          <input type="file" id="profileFile" accept="image/jpeg,image/jpg,image/png" style="padding:8px;">
          <small class="text-muted">JPG, PNG만 가능 (최대 5MB)</small>

          <!-- 커스텀 프로필일 때만 다운로드/삭제 표시 -->
          <div id="currentProfileRow"
               style="margin-top:8px; display:<%= (hasCustomProfile ? "flex" : "none") %>; align-items:center; gap:10px; flex-wrap:wrap;">
            <a id="currentProfileDownload" href="<%= (hasCustomProfile ? (ctx + profileDb) : "#") %>" download><%= esc(fileName) %></a>

            <label style="user-select:none;">
              <input type="checkbox" id="deleteProfile"> 삭제
            </label>
          </div>
        </div>
      </div>

      <!-- 기본 정보 -->
      <div style="flex:1; min-width:260px;">
        <div style="font-weight:700; margin-bottom:10px;">기본 정보</div>

        <div class="form-group">
          <label>아이디</label>
          <input type="text" value="<%= esc(loginId) %>" readonly>
        </div>

        <div class="form-group">
          <label>이름</label>
          <input type="text" value="<%= esc(name) %>" readonly>
        </div>

        <div class="form-group">
          <label>이메일</label>
          <input type="email" id="email" value="<%= esc(email) %>" placeholder="이메일 (선택)">
        </div>

        <div style="font-weight:700; margin:18px 0 10px;">비밀번호 변경</div>
        <div class="form-group">
          <label>새 비밀번호</label>
          <input type="password" id="newPassword" placeholder="변경 시에만 입력">
        </div>

        <div style="display:flex; gap:10px; margin-top:16px;">
          <button type="button" class="btn btn-primary" id="btnSave">저장</button>
          <a href="${pageContext.request.contextPath}/user/dashboard" class="btn btn-secondary">취소</a>
        </div>
      </div>

    </div>
  </div>
</main>

<jsp:include page="/WEB-INF/views/user/layout/footer.jsp" />

<script src="${pageContext.request.contextPath}/resources/js/common.js"></script>
<script src="${pageContext.request.contextPath}/resources/js/user/profile.js"></script>

</body>
</html>
