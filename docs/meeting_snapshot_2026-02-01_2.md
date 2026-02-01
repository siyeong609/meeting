# 개발 진행 스냅샷 (meeting) — 업데이트본 (2026-02-01)

## 0. 문서 규칙
- 이 문서는 “현재 상태”를 빠르게 복원하기 위한 운영 문서
- 커밋 단위로 체크박스 업데이트 권장
- 다음 대화/작업 PC 변경 시 이 문서만 보면 바로 이어서 진행 가능

---

## 1. 개발 환경(확정)
- OS: Windows
- IDE: IntelliJ IDEA Community
- JDK: 17 LTS
- Build: Maven (packaging=war)
- Server: Tomcat 10.1.x + Smart Tomcat
- DB: MySQL 8.0 (Docker Compose)
- DB Client: HeidiSQL
- Context Path: `/meeting`

---

## 2. 프로젝트 구조(현재)

### 2.1 루트 파일
- `pom.xml`
- `docker-compose.yml`
- `db/schema.sql`
- `db/data.sql`

### 2.2 Web root (정답 구조)
- 정적 리소스(css/js)는 반드시 `src/main/webapp/resources` 아래
- JSP는 전부 `src/main/webapp/WEB-INF/views` 아래(직접 접근 차단)

```text
src/main/webapp/resources
├─ css
│  ├─ admin
│  │  ├─ auth.css
│  │  ├─ dashboard.css
│  │  ├─ layout.css
│  │  ├─ member.css                  # 회원관리 전용(검색/페이징/상세모달)
│  │  └─ room.css                    # 회의실관리 전용(회원관리 UI 패턴 재사용)
│  ├─ user
│  │  ├─ auth.css
│  │  ├─ layout.css                  # 회원 레이아웃(헤더/네비/드롭다운/프로필)
│  │  └─ chat-widget.css             # 우측 하단 1:1 채팅 위젯 스타일(MVP)
│  ├─ common.css                     # Google Fonts(Noto Sans KR) 적용
│  └─ index.css
├─ js
│  ├─ common.js                      # showModal/escapeHtml/fetchJson/pagination
│  ├─ admin
│  │  ├─ member
│  │  │  └─ list.js                  # 회원관리 화면 스크립트 분리
│  │  └─ room
│  │     └─ list.js                  # 회의실관리 화면 스크립트 분리(회원관리 코드 구조 기반)
│  └─ user
│     ├─ profile.js
│     └─ chat-widget.js              # 우측 하단 1:1 채팅 위젯(MVP: 조회/전송/폴링)
└─ uploads
   └─ profile
      └─ default-profile.svg         # 기본 프로필(삭제 대상 제외)

src/main/webapp/WEB-INF/views
├─ admin
│  ├─ auth
│  │  └─ login.jsp
│  ├─ layout
│  │  ├─ footer.jsp
│  │  └─ header.jsp
│  ├─ member
│  │  └─ list.jsp                    # 목록은 JSON 렌더링(ajax) 방식으로 동작
│  ├─ room
│  │  └─ list.jsp                    # 회의실관리 목록 페이지(회원관리 형태로 구성)
│  └─ dashboard.jsp
├─ common
│  └─ error.jsp
└─ user
   ├─ auth
   │  └─ login.jsp
   ├─ layout
   │  ├─ header.jsp                  # 문서 시작 + 헤더 + 네비 포함
   │  └─ footer.jsp                  # 공통 스크립트 로드 + </body></html> 닫기
   ├─ dashboard.jsp                  # 회원 대시보드(컨텐츠만)
   ├─ profile.jsp                    # 내정보수정(컨텐츠만)
   └─ (옵션) chat.jsp                # (MVP에서는 위젯 사용, 폴백 용도)
```

### 2.3 프로필 기본 경로(운영 원칙/현재 기준)
- URL(브라우저에서 접근): `/meeting/resources/uploads/profile/default-profile.svg`
- DB 저장 정책: 커스텀 업로드 파일만 경로 저장
  - 기본 프로필은 DB에 저장하지 않고 `profile_image = NULL` 유지
  - 화면 렌더링에서 `profile_image == NULL`이면 default 이미지로 대체 표시
  - default 상태에서는 “파일명/다운로드/삭제 체크박스” UI를 노출하지 않음

### 2.4 회원 레이아웃 규칙(최근 변경)
- `user/layout/header.jsp`
  - 문서 시작(doctype/head/body) + 상단 헤더 + 상단 네비까지 포함
  - 우측: 프로필 이미지(작게) + 아이디 드롭다운 메뉴(내정보수정/내예약확인/1:1채팅문의/로그아웃)
  - 기존 “로그인: user” 텍스트 제거
  - JSP 내장 객체 `session` 재선언 금지(“Duplicate local variable session” 방지)
- `user/layout/footer.jsp`
  - 공통 스크립트 로드(`common.js`, `user/chat-widget.js`) + `</body></html>` 닫기
- 각 페이지(`dashboard.jsp`, `profile.jsp` 등)
  - 컨텐츠만 작성하고 header/footer include로 공통 레이아웃 재사용

---

## 3. Java 패키지(현재 기준)

```text
src/main/java
└─ com/company/meeting
   ├─ admin
   │  ├─ dto
   │  │  └─ AdminMemberListItem
   │  ├─ AdminAuthFilter
   │  ├─ AdminDashboardServlet
   │  ├─ AdminLoginServlet
   │  ├─ AdminLogoutServlet
   │  ├─ AdminMemberCreateServlet
   │  ├─ AdminMemberDeleteServlet
   │  ├─ AdminMemberListServlet
   │  ├─ AdminMemberProfileUploadServlet
   │  ├─ AdminMemberUpdateServlet
   │  ├─ AdminRoomCreateServlet
   │  ├─ AdminRoomDeleteServlet
   │  ├─ AdminRoomDetailServlet
   │  ├─ AdminRoomListServlet
   │  ├─ AdminRoomUpdateServlet
   │  └─ chat
   │     ├─ dao
   │     │  └─ ChatDAO
   │     ├─ dto
   │     │  ├─ ChatMessageDTO
   │     │  ├─ ChatMessageItem
   │     │  └─ ChatThreadDTO
   │     └─ service
   │        └─ ChatService
   ├─ common
   │  ├─ db
   │  │  └─ DBConnection
   │  └─ util
   │     ├─ api
   │     │  └─ ApiResponse
   │     ├─ json
   │     │  └─ JsonUtil
   │     ├─ paging
   │     │  ├─ PageInfo
   │     │  └─ PageRequest
   │     ├─ policy
   │     │  └─ RoomPolicy
   │     └─ security
   │        └─ PasswordUtil
   ├─ reservation
   │  ├─ dao
   │  │  └─ ReservationDAO
   │  ├─ dto
   │  │  └─ ReservationDTO
   │  └─ service
   │     └─ ReservationService
   ├─ room
   │  ├─ dao
   │  │  └─ RoomDAO
   │  ├─ dto
   │  │  ├─ RoomDetail
   │  │  ├─ RoomDTO
   │  │  ├─ RoomListItem
   │  │  └─ RoomOperatingHour
   │  └─ service
   │     └─ RoomService
   ├─ user
   │  ├─ dao
   │  │  └─ UserDAO
   │  ├─ dto
   │  │  └─ UserDTO
   │  └─ service
   │     ├─ UserAuthFilter
   │     ├─ UserChatMessagesServlet
   │     ├─ UserChatPageServlet
   │     ├─ UserChatSendServlet
   │     ├─ UserDashboardServlet
   │     ├─ UserLoginServlet
   │     ├─ UserLogoutServlet
   │     ├─ UserProfileServlet
   │     ├─ UserProfileUpdateServlet
   │     └─ UserProfileUploadServlet
   ├─ test
   └─ RootServlet

```

- 참고: 위 채팅 패키지/클래스명은 “MVP 구조 기준 권장 형태”
  - 실제 파일명이 다르면, 스냅샷과 맞춰서 표기만 조정하면 됨

---

## 4. Smart Tomcat 설정(현재 정답 세팅)
- Tomcat: 10.1.x
- Context path: `/meeting`
- Deployment directory: `src/main/webapp`
- Use classpath of module: `meeting`

### 확인 URL
- 루트 진입: `http://localhost:8080/meeting/home`
- 관리자 로그인: `http://localhost:8080/meeting/admin/auth/login`
- 관리자 대시보드: `http://localhost:8080/meeting/admin/dashboard`
- 관리자 회원관리: `http://localhost:8080/meeting/admin/members`
- 관리자 회의실관리: `http://localhost:8080/meeting/admin/rooms`
- 회원 로그인: `http://localhost:8080/meeting/user/auth/login`
- 회원 대시보드: `http://localhost:8080/meeting/user/dashboard`
- 정적 리소스 확인:
  - `http://localhost:8080/meeting/resources/js/user/chat-widget.js`
  - `http://localhost:8080/meeting/resources/css/user/chat-widget.css`

---

## 5. Docker(MySQL) 세팅(완료)

### 5.1 docker-compose.yml 핵심
- MySQL 8.0
- meeting_room DB 자동 생성
- TZ: Asia/Seoul
- charset/collation: utf8mb4
- schema.sql / data.sql 초기화 자동 실행
- volume: mysql_data

### 5.2 초기화 명령
- 최초 기동:
  - `docker-compose up -d`
- 완전 초기화(데이터까지 리셋):
  - `docker-compose down -v`
  - `docker-compose up -d`

---

## 6. DB 스키마/시드(업데이트 반영)

### 6.1 schema.sql (user 테이블 확장)
- `email`(NULL), `profile_image`(NULL), `memo`(NULL), `updated_at` 추가
- (선택) `email` unique: `NULL`은 다중 허용

### 6.2 schema.sql (room / 운영시간 테이블)
- room: 기본정보 + 예약정책(slot/buffer 등)
- 운영시간은 별도 테이블로 분리: 요일별 7행(정규화)
- 체크 포인트(규약 확정 필요)
  - `dow` 규약을 프로젝트 전체에서 1가지로 고정
    - 케이스 A: 1=월 ... 7=일
    - 케이스 B: 1=일 ... 7=토

### 6.3 schema.sql (1:1 채팅 - MVP 추가)
- `chat_thread`
  - 회원당 1개 문의 스레드 정책: `UNIQUE(user_id)`
  - `status`(OPEN 등), `last_message_at`, `created_at/updated_at`
- `chat_message`
  - `thread_id` FK(`chat_thread.id`), `ON DELETE CASCADE`
  - `sender_role`(USER|ADMIN), `sender_id`, `content`, `created_at`

### 6.4 data.sql
- `SET NAMES utf8mb4;`
- `SET CHARACTER SET utf8mb4;`
- admin/user 초기 계정 insert

### 6.5 (정리 작업) 기존 디폴트 경로가 DB에 들어간 데이터 정리
```sql
UPDATE user
SET profile_image = NULL
WHERE profile_image = '/resources/uploads/profile/default-profile.svg';
```

---

## 7. 회원(User) 영역 진행 상황(최근 작업 반영)

### 7.1 회원 레이아웃/헤더 UX 개선
- 헤더 우측 “로그인:” 텍스트 제거
- 프로필 이미지(작게) + 아이디 드롭다운 메뉴 적용
  - 내정보수정 / 내예약확인 / 1:1채팅문의 / 로그아웃
- header.jsp에 네비 포함(페이지들은 컨텐츠만 작성)
- footer.jsp에서 공통 스크립트 로드 + html 마감

### 7.2 JSP 컴파일 오류 해결
- `Duplicate local variable session` 해결
  - JSP 내장 객체 `session`을 재선언하지 않음
- `getUserId() is undefined` 대응
  - DTO 게터명 확정 전까지 리플렉션(우선순위: getUserId → getLoginId → getId)로 안전 표시
  - DTO 확정 후 리플렉션 제거 권장

### 7.3 내정보수정
- 드롭다운 “내정보수정” 진입 및 수정 동작 확인(현 상태: 완료로 보고)

### 7.4 1:1 채팅문의(MVP)
- “1:1채팅문의” 클릭 시 페이지 이동이 아니라 우측 하단 위젯(open)만 표시
- DB: `chat_thread`, `chat_message` 추가
- 프론트: `user/chat-widget.js` + `user/chat-widget.css`
- API(권장): `/user/chat/messages`, `/user/chat/send`
- 폴링: 2초 간격(sinceId 기반 신규 조회)

- 주의(필수)
  - header.jsp에서 `window.openChatWidget()`를 호출한다면
    - `chat-widget.js`에서 `window.openChatWidget = openWidget;` 형태로 글로벌 노출 필요
    - 또는 header가 직접 함수 호출하지 말고, 위젯 스크립트가 `#menuChatLink`를 바인딩하도록 고정

---

## 8. 완료 체크리스트(현재까지)

### User
- [x] 회원 로그인/세션(`LOGIN_USER`) + `/user/*` 보호 필터(UserAuthFilter)
- [x] 회원 대시보드(`/user/dashboard`) 라우팅
- [x] 회원 레이아웃 구조(header에 네비 포함, footer에서 html 마감)로 정리
- [x] 헤더 드롭다운(내정보수정/내예약확인/1:1채팅문의/로그아웃) 적용
- [x] 내정보수정 동작 확인(현 상태: 완료로 보고)
- [ ] 1:1 채팅 위젯: DB 연동/입력/기존 메시지 로딩 완전 정상화(잔여 점검)
  - API 응답 포맷/쿼리/세션 user_id/폴링 sinceId 동작 확인

---

## 9. 커밋 스냅샷(최근 작업 반영)
- 회원(User) 레이아웃 구조 재정리: header에 네비 포함, footer에서 html 마감
- JSP 오류 해결: session 중복 선언 제거 + DTO 게터 미정 문제(리플렉션 기반 안전 표시)
- 헤더 UX 개선: 프로필 이미지 + 아이디 드롭다운(내정보수정/내예약확인/1:1채팅문의/로그아웃)
- 내정보수정 페이지/흐름 연결(동작 확인 상태)
- 1:1 채팅 MVP 착수
  - DB 스키마 추가: chat_thread / chat_message
  - user/chat-widget.js + user/chat-widget.css 추가
  - 위젯: 우측 하단 플로팅 + 2초 폴링 + sinceId 기반 신규 조회 + 전송(POST)

---

## 10. 다음 구현 순서(추천)
- 회원 예약 플로우(rooms → reservation) 우선 진행
- 채팅은 API 응답 포맷부터 고정해서 위젯 안정화
- 관리자 답변 UI는 “thread 목록 + 메시지 송수신” 형태로 확장
