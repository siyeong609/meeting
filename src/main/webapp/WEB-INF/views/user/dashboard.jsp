<%@ page contentType="text/html; charset=UTF-8" %>
<%@ page pageEncoding="UTF-8" %>

<%
  // ✅ (선택) 타이틀을 바꾸고 싶으면 서블릿에서 넣는 게 정석.
  // 여기서도 가능하지만, MVC 기준이면 doGet에서 req.setAttribute("pageTitle", "...") 추천.
  request.setAttribute("pageTitle", "회원 대시보드 - 회의실 예약");
%>

<jsp:include page="/WEB-INF/views/user/layout/header.jsp" />

<main class="user-container">
  <h2 class="section-title">회원 대시보드</h2>

  <div class="card" style="padding:16px;">
    <div style="color: var(--text-muted); font-size: 13px;">다음 단계</div>
    <div style="margin-top: 8px;">
      - /user/rooms 화면 생성 후 “예약 시작” 플로우 연결<br/>
      - reservation 도메인 붙이면 “다가오는 예약/내 예약 수” 통계 출력
    </div>
  </div>
</main>

<jsp:include page="/WEB-INF/views/user/layout/footer.jsp" />
