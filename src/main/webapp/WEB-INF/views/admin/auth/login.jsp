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

      // 입력값 검증
      if (!userId || !password) {
        showModal('아이디와 비밀번호를 입력하세요.', 'warning');
        return;
      }

      // 서버 로그인 요청
      fetch(
        '${pageContext.request.contextPath}/admin/auth/login',
        {
          method: 'POST',
          headers: {
            'Content-Type': 'application/x-www-form-urlencoded'
          },
          // 같은 도메인이라 기본적으로 쿠키가 붙지만,
          // 명시해두면 나중에 설정 바뀌어도 안전함
          credentials: 'same-origin',
          body:
            'userId=' + encodeURIComponent(userId) +
            '&password=' + encodeURIComponent(password)
        }
      )
      .then(function (res) {
        return res.json().then(function (json) {
          // status는 실패여도 body는 내려올 수 있으니 같이 반환
          return { res: res, json: json };
        });
      })
      .then(function (pack) {
        const res = pack.res;
        const json = pack.json;

        // ✅ ApiResponse 포맷(ok) + 이전 포맷(success) 둘 다 호환
        const isOk = (json && (json.ok === true || json.success === true));

        if (res.ok && isOk) {
          showModal('로그인 성공', 'success');

          setTimeout(function () {
            window.location.href =
              '${pageContext.request.contextPath}/admin/dashboard';
          }, 300);

          return;
        }

        // 실패 메시지
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
