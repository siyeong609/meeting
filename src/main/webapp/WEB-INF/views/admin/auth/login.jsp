<%@ page contentType="text/html; charset=UTF-8" %>
<%@ page pageEncoding="UTF-8" %>

<!DOCTYPE html>
<html lang="ko">
<head>
  <meta charset="UTF-8">
  <title>관리자 로그인 - 회의실 예약</title>

  <!-- 공통 CSS -->
  <link rel="stylesheet"
        href="${pageContext.request.contextPath}/resources/css/common.css">

  <!-- 관리자 로그인 CSS -->
  <link rel="stylesheet"
        href="${pageContext.request.contextPath}/resources/css/admin/auth.css">
</head>
<body>

<div class="admin-login-wrap">
  <div class="card admin-login-box">
    <h1>관리자 로그인</h1>
    <p class="subtitle">회의실 예약 시스템 관리자</p>

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

      <button type="submit"
              class="btn btn-primary btn-block">
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

      // 입력값 검증
      if (!userId || !password) {
        showModal('아이디와 비밀번호를 입력하세요.', 'warning');
        return;
      }

      // 서버 로그인 요청 (검증 완료된 방식)
      fetch(
        '${pageContext.request.contextPath}/admin/auth/login',
        {
          method: 'POST',
          headers: {
            'Content-Type': 'application/x-www-form-urlencoded'
          },
          body:
            'userId=' + encodeURIComponent(userId) +
            '&password=' + encodeURIComponent(password)
        }
      )
      .then(function (res) {
        if (!res.ok) {
          throw new Error('서버 응답 오류');
        }
        return res.json();
      })
      .then(function (json) {

        if (json.success) {
          showModal('로그인 성공', 'success');

          // 모달 보여준 뒤 이동
          setTimeout(function () {
            window.location.href =
              '${pageContext.request.contextPath}/admin/dashboard';
          }, 500);

          return;
        }

        showModal(
          json.message || '아이디 또는 비밀번호가 올바르지 않습니다.',
          'error'
        );
      })
      .catch(function () {
        showModal('로그인 처리 중 오류가 발생했습니다.', 'error');
      });
    });

  })();
</script>

</body>
</html>
