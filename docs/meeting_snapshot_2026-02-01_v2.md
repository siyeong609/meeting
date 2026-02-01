# 개발 진행 스냅샷 (meeting) — 업데이트본 (2026-02-01, v2)

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
│  │  ├─ layout.css                  # 회원 공통 레이아웃(헤더/네비/드롭다운)
│  │  └─ chat-widget.css             # 우측 하단 채팅 위젯 스타일(MVP)
│  ├─ common.css                     # Google Fonts(Noto Sans KR) 적용
│  └─ index.css
├─ js
│  ├─ common.js                      # showModal/escapeHtml/fetchJson/pagination
│  ├─ user
│  │  └─ chat-widget.js              # 우측 하단 채팅 위젯(MVP)
│  └─ admin
│     ├─ member
│     │  └─ list.js                  # 회원관리 화면 스크립트 분리
│     └─ room
│        └─ list.js                  # 회의실관리 화면 스크립트 분리(회원관리 코드 구조 기반)
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
│  │  └─ list.jsp
│  ├─ room
│  │  └─ list.jsp
│  └─ dashboard.jsp
├─ common
│  └─ error.jsp
└─ user
   ├─ auth
   │  └─ login.jsp
   ├─ layout
   │  ├─ header.jsp                  # doctype/head/body + header + user-nav 포함(컨텐츠만 분리)
   │  └─ footer.jsp                  # 공통 스크립트 + </body></html> 닫기 포함
   ├─ dashboard.jsp
   ├─ profile.jsp
   └─ chat.jsp                       # (선택) JS 미동작 시 fallback 페이지
```

> 프로필 기본 경로(운영 원칙/현재 기준)
- URL(브라우저에서 접근): `/meeting/resources/uploads/profile/default-profile.svg`
- DB 저장 정책: 커스텀 업로드 파일만 경로 저장
  - 기본 프로필은 DB에 저장하지 않고 `profile_image = NULL` 유지
  - 화면 렌더링에서 `profile_image == NULL`이면 default 이미지로 대체 표시
  - default 상태에서는 “파일명/다운로드/삭제 체크박스” UI를 노출하지 않음

---

## 3. Java 패키지(현재 기준) ✅ 최신 반영

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
  - `http://localhost:8080/meeting/resources/css/common.css`
  - `http://localhost:8080/meeting/resources/js/common.js`
  - `http://localhost:8080/meeting/resources/uploads/profile/default-profile.svg`

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

## 6. DB 스키마/시드(업데이트 반영 중)

### 6.1 schema.sql (user 테이블 확장)
- `email`(NULL), `profile_image`(NULL), `memo`(NULL), `updated_at` 추가
- (선택) `email` unique: `NULL`은 다중 허용

> 운영 원칙(최종 정리)
- `profile_image`: 커스텀 업로드가 있을 때만 DB에 경로 저장(없으면 `NULL`)
- 화면에서는 `profile_image == NULL`이면 default 이미지로 렌더링
- default 이미지는 “파일”로 간주하지 않음(다운로드/삭제 UI 숨김)

### 6.2 schema.sql (room / 운영시간 테이블)
- room: 기본정보 + 예약정책(slot/buffer 등)
- 운영시간은 별도 테이블로 분리: 요일별 7행(정규화)

> 체크 포인트(규약 확정 필요)
- `dow` 규약을 프로젝트 전체에서 1가지로 고정
  - 케이스 A: 1=월 ... 7=일
  - 케이스 B: 1=일 ... 7=토

### 6.3 schema.sql (1:1 채팅 - MVP) ✅ 추가
- `chat_thread`: 회원당 1개 문의 스레드(UNIQUE user_id), 상태/최종메시지 시간 관리
- `chat_message`: 스레드 메시지 로그(발신자 ROLE: USER|ADMIN, sinceId 폴링 지원을 위한 BIGINT PK)

```sql
CREATE TABLE IF NOT EXISTS chat_thread (
  id INT AUTO_INCREMENT PRIMARY KEY,
  user_id INT NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
  last_message_at DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uq_chat_thread_user_id (user_id),
  INDEX idx_chat_thread_updated_at (updated_at),
  INDEX idx_chat_thread_last_message_at (last_message_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS chat_message (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  thread_id INT NOT NULL,
  sender_role VARCHAR(10) NOT NULL,
  sender_id INT NULL,
  content TEXT NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_chat_message_thread_id (thread_id),
  INDEX idx_chat_message_created_at (created_at),
  CONSTRAINT fk_chat_message_thread
    FOREIGN KEY (thread_id) REFERENCES chat_thread(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
```

### 6.4 data.sql
- `SET NAMES utf8mb4;`
- `SET CHARACTER SET utf8mb4;`
- admin/user 초기 계정 insert

### 6.5 (정리 작업) 기존 디폴트 경로가 DB에 들어간 데이터 정리
- 기존에 `profile_image`에 `/resources/uploads/profile/default-profile.svg` 같은 디폴트 경로를 저장했던 레코드는 NULL로 정리 필요

```sql
UPDATE user
SET profile_image = NULL
WHERE profile_image = '/resources/uploads/profile/default-profile.svg';
```

---

## 7. Maven 의존성(핵심)
- servlet-api:
  - `jakarta.servlet-api` (scope=provided)
- MySQL JDBC:
  - `com.mysql:mysql-connector-j:8.0.33`
- Jackson(JSON):
  - `com.fasterxml.jackson.core:jackson-databind`

---

## 8. JDBC 연결(완료)
### 8.1 이슈
- Smart Tomcat + JDK 17 환경에서 DriverManager 자동 인식 실패

### 8.2 해결
- DBConnection에서 Driver 강제 등록:
  - `DriverManager.registerDriver(new com.mysql.cj.jdbc.Driver());`

### 8.3 결과
- DB 연결 테스트 성공
- DAO 레벨 접근 가능

---

## 9. 관리자 로그인/세션/필터(완료)
### 9.1 관리자 로그인
- URL: `/admin/auth/login`
- GET: 로그인 페이지 forward
- POST: AJAX(fetch) 로그인 처리(JSON 응답)
- 성공 시 세션: `LOGIN_ADMIN` 저장

### 9.2 관리자 권한 보호
- Filter: `AdminAuthFilter`
- 적용: `/admin/*`
- 예외: `/admin/auth/login`
- 미로그인 → `/admin/auth/login` 리다이렉트

### 9.3 로그아웃
- URL: `/admin/logout`
- 세션 invalidate 후 `/admin/auth/login` 이동

---

## 10. 관리자 대시보드(진행 중)
### 10.1 화면
- URL: `/admin/dashboard`
- Servlet: `AdminDashboardServlet` → `/WEB-INF/views/admin/dashboard.jsp` forward

### 10.2 통계 연동
- 전체 회원 수 (DB 연동 완료)
- 등록된 회의실 수 (미구현)
- 오늘 예약 건수 (미구현)
- 이번 달 예약 건수 (미구현)

---

## 11. 관리자 회원관리 `/admin/members` (핵심 기능 구현)
### 11.1 화면/라우팅
- URL: `/admin/members`
- GET: `list.jsp` forward
- POST: JSON 데이터 반환 (page/size/q 지원)
  - 공통 응답 포맷: `{ ok, message, data, page }`

### 11.2 프론트(UI)
- 검색바: `검색 input + (페이지사이즈 dropdown) + 검색 버튼`
- 우측 상단 버튼: 회원 생성 / 선택 삭제
- 목록은 JSP loop 렌더링이 아니라 ajax(JSON)로 받아서 DOM 렌더링
- 상세 팝업: 이메일/비밀번호/메모/프로필 업로드 + (커스텀 프로필일 때만) 다운로드/삭제 UI

### 11.3 백엔드(User 중심)
- `UserDAO`: 목록/검색/페이징 + select 필드 확장(email/profile_image/memo/updated_at)
- `UserService`: authenticate(평문→해시 업그레이드), updateEmail/updateMemo/changePassword/clearProfileImage 등
- `AdminMemberCreateServlet`: memo 수신/저장 반영
- `AdminMemberUpdateServlet`: email/memo/newPassword/deleteProfile 처리

### 11.4 공통 JS 개선
- `resources/js/common.js` fetchJson 개선:
  - HTTP 400/409 등 비정상 status여도 body(JSON) 파싱
  - 서버 message를 모달에 그대로 출력 가능

---

## 12. 관리자 회의실관리 `/admin/rooms` (목록/생성/수정/삭제 완료)
### 12.1 화면/라우팅
- URL: `/admin/rooms`
- GET: `room/list.jsp` forward
- POST: JSON 목록 반환 (page/size/q 지원)

### 12.2 프론트(UI)
- 회원관리 레이아웃 재사용(검색/페이지사이즈/선택삭제/모달)
- 운영시간 UI: 요일별 체크 + open/close 입력
- 운영시간 검증: 문자열 비교 → 분 단위 숫자 비교로 보강

### 12.3 백엔드
- AdminRoom 서블릿 세트(list/create/update/delete)
- 운영시간 파라미터 규약: `dow1~dow7`, 휴무는 `dowN_closed=1`, 운영일은 `dowN_open/dowN_close` 필수

### 12.4 확인 필요(잔여)
- `dow` 규약 확정(1=월 시작 vs 1=일 시작) 및 프론트 라벨/DB 저장 일치 확인
- (권장) 수정 모달 정합성: `/admin/rooms/detail`에서 operatingHours 포함 조회

---

## 13. 회원(User) 로그인/대시보드/프로필/채팅(MVP)

### 13.1 회원 로그인/세션/필터
- URL: `/user/auth/login`
- 성공 시 세션: `LOGIN_USER` 저장
- Filter: `UserAuthFilter`로 `/user/*` 보호(로그인 예외 경로는 별도)

### 13.2 회원 레이아웃 구조 리팩토링 ✅
- 목표: 모든 user 페이지는 “컨텐츠만” 작성
- `user/layout/header.jsp`가 문서 시작 + 헤더 + 네비까지 포함
- `user/layout/footer.jsp`가 공통 스크립트 + 문서 종료 포함

> JSP 컴파일 이슈 대응
- `Duplicate local variable session` 방지: JSP 내장 객체 `session` 그대로 사용(새로 선언 금지)
- `UserDTO` getter 미확정 대응: 헤더에서 로그인 라벨/프로필 경로는 리플렉션으로 안전하게 읽는 방식 적용

### 13.3 헤더 UX 개선(드롭다운) ✅
- 헤더 우측: 작은 프로필 이미지 + 아이디 버튼
- 아이디 클릭 시 드롭다운 메뉴 4개
  - 내정보수정
  - 내예약확인
  - 1:1채팅문의(페이지 이동 X, 위젯 오픈)
  - 로그아웃(드롭다운 내 포함)

### 13.4 내정보수정(회원 프로필) ✅
- `UserProfileServlet` / `UserProfileUpdateServlet` / `UserProfileUploadServlet`
- 프로필 이미지 정책: 커스텀 업로드만 DB 저장, 없으면 NULL + default 이미지 표시

### 13.5 1:1 채팅 위젯(MVP) ✅/진행중
- 요구 UX: 우측 하단 플로팅 위젯(카톡 스타일 좌/우 말풍선)
- 리소스: `resources/js/user/chat-widget.js`, `resources/css/user/chat-widget.css`
- API(예정): 
  - `/user/chat/messages` : sinceId 기반 신규 메시지 조회
  - `/user/chat/send` : 메시지 전송
- 서버/도메인: `admin/chat/*` 패키지로 DAO/Service/DTO 구성(관리자 측 연동 기반)

> 현재 이슈(정리)
- 위젯 표시까지는 됨
- DB의 기존 메시지 로딩/전송 저장이 아직 정합하게 연결되지 않아(DAO/서블릿 응답 형식/쿼리) 추가 작업 필요

---

## 14. 완료 체크리스트(현재까지)

### 환경
- [x] JDK 17 설정
- [x] Maven 프로젝트 생성(war)
- [x] Smart Tomcat 실행
- [x] 정적 리소스 서빙 확인
- [x] JSP forward 구조 확정
- [x] views: admin/user 공통 레이아웃(header/footer) 적용 구조 확정

### DB
- [x] Docker MySQL 기동
- [x] schema.sql 자동 적용
- [x] data.sql 자동 적용
- [x] 한글 깨짐 해결
- [x] JDBC 연결 OK
- [x] user 테이블 확장(email/profile_image/memo/updated_at) 반영 확인
- [x] profile_image 디폴트 저장 정책 수정(기본은 NULL)
- [ ] (선택) 기존 디폴트 경로 저장 레코드 NULL 정리(필요 시 6.5 SQL 실행)
- [ ] chat_thread / chat_message 스키마 반영(schema.sql 병합 및 재기동 확인)

### Admin
- [x] 관리자 로그인(AJAX) + 세션(`LOGIN_ADMIN`)
- [x] `/admin/*` 보호 필터 적용
- [x] 로그아웃 구현
- [x] 대시보드 화면 연결
- [x] 대시보드: 전체 회원 수 통계 DB 연동
- [x] 회원관리: 목록/검색/페이징/상세 수정/생성/선택삭제/프로필 업로드&삭제
- [x] 공통 에러모달: HTTP 오류에서도 서버 message 출력(fetchJson 개선)
- [x] 회의실관리: 목록/생성/수정/삭제 + 운영시간 검증

### User
- [x] 회원 로그인 + 세션(`LOGIN_USER`) + 보호 필터(UserAuthFilter)
- [x] user 레이아웃 리팩토링(header에 네비 포함, footer에서 문서 닫기)
- [x] 헤더 드롭다운(아바타 + 메뉴 4개)
- [x] 내정보수정(프로필 수정/업로드/저장) 1차 동작 확인
- [ ] 1:1 채팅 위젯: DB 조회/전송 정합성(DAO/서블릿/응답 포맷) 마무리

### 공통 유틸
- [ ] common.js 페이징 유틸 “사용처 기준” 최종 정리(함수/옵션 정리, 중복 제거)

---

## 15. 커밋 스냅샷(최근 작업 반영) ✅ 이번 대화 내용 포함
- user 레이아웃 구조 리팩토링(header.jsp에 네비 포함, footer.jsp에서 문서 종료)
- JSP 컴파일 오류 대응(session 중복 선언 제거, UserDTO getter 미확정은 리플렉션 안전 처리)
- user 헤더 UX 개선(아바타 + 드롭다운 메뉴: 내정보수정/내예약확인/채팅/로그아웃)
- 회원 프로필(내정보수정) 서블릿 세트 추가/정리(profile/view/update/upload)
- 1:1 채팅 MVP 스키마(chat_thread/chat_message) 추가 및 위젯 JS/CSS 뼈대 추가
- user 채팅 서블릿 엔드포인트 추가(UserChatMessagesServlet/UserChatSendServlet, fallback page servlet)

---

## 16. 다음 구현 순서(추천)

### 16.1 채팅 MVP 완성(우선순위 1)
- `chat_thread` 자동 생성 정책 확정(최초 메시지 전송 시 생성 vs 최초 위젯 오픈 시 생성)
- `/user/chat/messages` 구현 고정
  - 입력: sinceId
  - 출력: { items: [ {id, senderRole, senderName, senderLoginId, content, createdAt} ] }
- `/user/chat/send` 구현 고정
  - 입력: content
  - 처리: thread 확보 → insert message → last_message_at 갱신
  - 출력: 저장된 메시지 1건 반환
- 응답 포맷을 ApiResponse(ok/message/data)로 통일

### 16.2 예약 도메인(우선순위 2)
- 회원이 예약하는 플로우 구현(/user/rooms → 예약 시작)
- reservation DAO/Service 구현 + 정책(중복예약, 운영시간/slot/buffer 검증)

### 16.3 대시보드 통계 확장(우선순위 3)
- 관리자: 등록된 회의실 수 / 오늘 예약 건수 / 이번 달 예약 건수
- 회원: 다가오는 예약 / 내 예약 수

---

## 17. 다음 대화에서 시작 문구(추천)
- `meeting 스냅샷 v2 기준으로 채팅 API(/user/chat/messages, /user/chat/send) 응답 포맷부터 확정하고, DAO 쿼리/서블릿을 맞추자`
