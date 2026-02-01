<%@ page contentType="text/html; charset=UTF-8" %>
<%@ page pageEncoding="UTF-8" %>

<!DOCTYPE html>
<html lang="ko">
<head>
  <meta charset="UTF-8">
  <title>회원 로그인 - 회의실 예약</title>

  <!-- 공통 CSS -->
  <link rel="stylesheet"
        href="${pageContext.request.contextPath}/resources/css/common.css">

  <!-- 회원 로그인 CSS -->
  <link rel="stylesheet"
        href="${pageContext.request.contextPath}/resources/css/user/auth.css">
</head>
<body>

<div class="user-login-wrap">
  <div class="card user-login-box">
    <h1 class="card-title">회원 로그인</h1>
    <p class="subtitle">회의실 예약 시스템</p>

    <!--
      ✅ 관리자 로그인과 동일 패턴
      - userId/password 파라미터로 서버(UserLoginServlet)가 받는다.
    -->
    <form id="loginForm">
      <div class="form-group">
        <label for="userId">아이디</label>
        <input type="text"
               id="userId"
               name="userId"
               required
               autocomplete="username">
      </div>

      <div class="form-group">
        <label for="password">비밀번호</label>
        <input type="password"
               id="password"
               name="password"
               required
               autocomplete="current-password">
      </div>

      <button type="submit" class="btn btn-primary btn-block">
        로그인
      </button>
    </form>
  </div>
</div>

<!-- 공통 JS -->
<script src="${pageContext.request.contextPath}/resources/js/common.js"></script>

<script>
  (function () {

    const form = document.getElementById('loginForm');

    form.addEventListener('submit', function (e) {
      e.preventDefault();

      const userId = document.getElementById('userId').value.trim();
      const password = document.getElementById('password').value;

      // ✅ 입력값 검증
      if (!userId || !password) {
        showModal('아이디와 비밀번호를 입력하세요.', 'warning');
        return;
      }

      // ✅ 서버 로그인 요청
      fetch(
        '${pageContext.request.contextPath}/user/auth/login',
        {
          method: 'POST',
          headers: {
            'Content-Type': 'application/x-www-form-urlencoded'
          },
          credentials: 'same-origin',
          body:
            'userId=' + encodeURIComponent(userId) +
            '&password=' + encodeURIComponent(password)
        }
      )
      .then(function (res) {
        return res.json().then(function (json) {
          return { res: res, json: json };
        });
      })
      .then(function (pack) {
        const res = pack.res;
        const json = pack.json;

        // ✅ ApiResponse 포맷(ok) + 이전 포맷(success) 호환
        const isOk = (json && (json.ok === true || json.success === true));

        if (res.ok && isOk) {
          showModal('로그인 성공', 'success');

          setTimeout(function () {
            window.location.href =
              '${pageContext.request.contextPath}/user/dashboard';
          }, 300);

          return;
        }

        const msg = (json && (json.message || json.msg)) || '아이디 또는 비밀번호가 올바르지 않습니다.';
        showModal(msg, 'error');
      })
      .catch(function () {
        showModal('로그인 처리 중 오류가 발생했습니다.', 'error');
      });
    });

  })();
</script>

</body>
</html>
