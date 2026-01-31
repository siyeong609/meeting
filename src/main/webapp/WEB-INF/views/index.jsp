<%@ page contentType="text/html; charset=UTF-8" %>
<%@ page pageEncoding="UTF-8" %>

<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <title>회의실 예약 시스템</title>

    <!--
        공통 스타일
        - 색상, 폰트, 버튼, 폼 등 전역 공통
    -->
    <link rel="stylesheet"
          href="${pageContext.request.contextPath}/resources/css/common.css">

    <!--
        index 전용 스타일
        - 관리자 / 사용자 선택 화면 전용
    -->
    <link rel="stylesheet"
          href="${pageContext.request.contextPath}/resources/css/index.css">
</head>
<body>

<!--
    index 페이지
    - RootServlet("/") 에서 forward 전용
    - 직접 URL 접근 대상 아님
    - 로그인 진입 선택 화면만 담당
-->
<div class="index-wrap">
    <div class="index-box">

        <h1 class="index-title">회의실 예약 시스템</h1>

        <div class="index-links">

            <!-- 관리자 로그인 -->
            <a href="${pageContext.request.contextPath}/admin/auth/login"
               class="index-link admin">
                관리자 로그인
            </a>

            <!-- 사용자 로그인 -->
            <a href="${pageContext.request.contextPath}/user/auth/login"
               class="index-link user">
                사용자 로그인
            </a>

        </div>

    </div>
</div>

</body>
</html>
