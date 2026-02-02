<%@ page contentType="text/html; charset=UTF-8" %>
<%@ page pageEncoding="UTF-8" %>
<%@ page import="com.company.meeting.user.dto.UserDTO" %>
<%@ page import="java.lang.reflect.Method" %>

<%!
  /**
   * ✅ HTML Escape (JSTL 없이 안전하게 렌더링)
   */
  private String esc(String s) {
    if (s == null) return "";
    return s.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;");
  }

  /**
   * ✅ DTO getter 명이 케이스별로 다를 수 있어 reflection으로 흡수
   */
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

  /**
   * ✅ 여러 후보 중 가장 먼저 유효한 문자열을 선택
   */
  private String firstNonEmpty(String... arr) {
    if (arr == null) return null;
    for (String s : arr) {
      if (s != null && !s.trim().isEmpty()) return s.trim();
    }
    return null;
  }
%>

<%
  // =========================================================
  // ✅ header.jsp 확장 포인트
  // =========================================================
  request.setAttribute("pageTitle", "내정보수정");
  request.setAttribute("pageCss", "profile.css"); // /resources/css/user/profile.css

  String ctx = request.getContextPath();

  // ✅ 세션 유저
  UserDTO loginUser = (UserDTO) session.getAttribute("LOGIN_USER");

  // 기본 프로필 이미지
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

  boolean hasCustomProfile = (profileDb != null && !profileDb.trim().isEmpty());

  // ✅ profileDb가 http(s)면 그대로, 아니면 ctx 붙임
  String profileUrl = defaultUrl;
  if (hasCustomProfile) {
    String p = profileDb.trim();
    if (p.startsWith("http://") || p.startsWith("https://")) {
      profileUrl = p;
    } else {
      if (!p.startsWith("/")) p = "/" + p;
      profileUrl = ctx + p;
    }
  }

  // 다운로드 표기용 파일명
  String fileName = "";
  if (hasCustomProfile) {
    String p = profileDb.trim();
    int idx = p.lastIndexOf("/");
    fileName = (idx >= 0) ? p.substring(idx + 1) : p;
  }
%>

<jsp:include page="/WEB-INF/views/user/layout/header.jsp" />

<main class="user-container">

  <!-- ✅ room-card 스타일 재사용 -->
  <section class="room-card profile-card">
    <div class="profile-head">
      <div class="profile-head-left">
        <h2 class="profile-title">내정보수정</h2>
        <div class="text-muted">프로필 이미지 / 이메일 / 비밀번호를 변경할 수 있습니다.</div>
      </div>

      <div class="profile-head-right">
        <button type="button" class="btn btn-primary" id="btnSave">저장</button>
        <a href="<%= ctx %>/user/reservations" class="btn btn-secondary">취소</a>
      </div>
    </div>

    <div class="profile-grid">

      <!-- =========================
           좌측: 프로필 이미지
           ========================= -->
      <div class="profile-left">
        <div class="profile-section-title">프로필 이미지</div>

        <div class="profile-avatar">
          <img id="profilePreview"
               src="<%= profileUrl %>"
               alt="profile"
               onerror="this.onerror=null; this.src='<%= defaultUrl %>';">
        </div>

        <div class="profile-form-group">
          <label class="profile-label" for="profileFile">이미지 업로드</label>
          <input type="file"
                 id="profileFile"
                 accept="image/jpeg,image/jpg,image/png"
                 class="profile-file">
          <div class="text-muted profile-help">JPG, PNG만 가능 (최대 5MB)</div>

          <!-- ✅ 커스텀 프로필일 때만 다운로드/삭제 표시 -->
          <div id="currentProfileRow" class="profile-current-row <%= (hasCustomProfile ? "" : "is-hidden") %>">
            <a id="currentProfileDownload"
               href="<%= (hasCustomProfile ? profileUrl : "#") %>"
               download><%= esc(fileName) %></a>

            <label class="profile-delete">
              <input type="checkbox" id="deleteProfile">
              삭제
            </label>
          </div>
        </div>
      </div>

      <!-- =========================
           우측: 기본 정보 + 비번 변경
           ========================= -->
      <div class="profile-right">
        <div class="profile-section-title">기본 정보</div>

        <div class="profile-form-group">
          <label class="profile-label">아이디</label>
          <input type="text" value="<%= esc(loginId) %>" readonly class="profile-input">
        </div>

        <div class="profile-form-group">
          <label class="profile-label">이름</label>
          <input type="text" value="<%= esc(name) %>" readonly class="profile-input">
        </div>

        <div class="profile-form-group">
          <label class="profile-label" for="email">이메일</label>
          <input type="email"
                 id="email"
                 value="<%= esc(email) %>"
                 placeholder="이메일 (선택)"
                 class="profile-input">
        </div>

        <div class="profile-section-title mt-18">비밀번호 변경</div>

        <div class="profile-form-group">
          <label class="profile-label" for="newPassword">새 비밀번호</label>
          <input type="password"
                 id="newPassword"
                 placeholder="변경 시에만 입력"
                 class="profile-input">
        </div>
      </div>

    </div>
  </section>

</main>

<!-- ✅ profile.js가 ctx를 읽기 전에 먼저 주입 -->
<script>
  window.__MEETING__ = window.__MEETING__ || {};
  window.__MEETING__.ctx = "<%= ctx %>";
</script>

<!-- ✅ footer.jsp가 common.js 로드는 그대로 둬도 됨(DOMContentLoaded 시점엔 common.js 로드 완료) -->
<script src="<%= ctx %>/resources/js/user/profile.js"></script>

<jsp:include page="/WEB-INF/views/user/layout/footer.jsp" />
