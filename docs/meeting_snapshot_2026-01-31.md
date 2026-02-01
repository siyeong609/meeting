# 개발 진행 스냅샷 (meeting) — 업데이트본 (2026-01-31)

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
│  │  └─ member.css                  # 회원관리 전용(검색/페이징/상세모달)
│  ├─ user
│  │  ├─ auth.css
│  │  └─ layout.css
│  ├─ common.css                     # ✅ Google Fonts(Noto Sans KR) 적용
│  └─ index.css
├─ js
│  ├─ common.js                      # ✅ showModal/escapeHtml/fetchJson/pagination
│  └─ admin
│     └─ member
│        └─ list.js                  # ✅ 회원관리 화면 스크립트 분리
└─ uploads
   └─ profile
      └─ default-profile.svg         # ✅ 기본 프로필(삭제 대상 제외)

src/main/webapp/WEB-INF/views
├─ admin
│  ├─ auth
│  │  └─ login.jsp
│  ├─ layout
│  │  ├─ footer.jsp
│  │  └─ header.jsp
│  ├─ member
│  │  └─ list.jsp                    # ✅ 목록은 JSON 렌더링(ajax) 방식으로 동작
│  └─ dashboard.jsp
├─ common
│  └─ error.jsp
└─ user
   ├─ auth
   │  └─ login.jsp
   ├─ layout
   │  ├─ footer.jsp
   │  └─ header.jsp
   └─ index.jsp
```

> 프로필 기본 경로(운영 원칙/현재 기준)
- URL(브라우저에서 접근): `/meeting/resources/uploads/profile/default-profile.svg`
- DB 저장 정책: **커스텀 업로드 파일만** 경로 저장  
  - 기본 프로필은 DB에 저장하지 않고 `profile_image = NULL` 유지
  - 화면 렌더링에서 `profile_image == NULL`이면 default 이미지로 대체 표시
  - default 상태에서는 “파일명/다운로드/삭제 체크박스” UI를 노출하지 않음

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
   │  ├─ AdminMemberListServlet           # GET: JSP / POST: JSON(page/size/q)
   │  ├─ AdminMemberProfileUploadServlet  # POST: multipart 업로드(DB profile_image 갱신)
   │  └─ AdminMemberUpdateServlet         # POST: email/memo/pw/deleteProfile 처리
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
   │     └─ security
   │        └─ PasswordUtil
   ├─ reservation (진행 전)
   ├─ room (진행 전)
   ├─ user (진행 중)
   ├─ RootServlet
   ├─ test
   └─ org
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
- ✅ `email`(NULL), `profile_image`(NULL), `memo`(NULL), `updated_at` 추가
- (선택) `email` unique: `NULL`은 다중 허용

> 운영 원칙(최종 정리)
- `profile_image`: 커스텀 업로드가 있을 때만 DB에 경로 저장(없으면 `NULL`)
- 화면에서는 `profile_image == NULL`이면 default 이미지로 렌더링
- default 이미지는 “파일”로 간주하지 않음(다운로드/삭제 UI 숨김)

### 6.2 data.sql
- `SET NAMES utf8mb4;`
- `SET CHARACTER SET utf8mb4;`
- admin/user 초기 계정 insert

### 6.3 (정리 작업) 기존 디폴트 경로가 DB에 들어간 데이터 정리
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

---

## 8. JDBC 연결(완료)
### 8.1 이슈
- Smart Tomcat + JDK 17 환경에서 DriverManager 자동 인식 실패 발생

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
- CSS:
  - `resources/css/admin/layout.css`
  - `resources/css/admin/dashboard.css`

### 10.2 통계 연동
- [x] 전체 회원 수 (DB 연동 완료)
- [ ] 등록된 회의실 수
- [ ] 오늘 예약 건수
- [ ] 이번 달 예약 건수

---

## 11. 관리자 회원관리 `/admin/members` (핵심 기능 구현)
### 11.1 화면/라우팅
- URL: `/admin/members`
- GET: `list.jsp` forward
- POST: JSON 데이터 반환 (page/size/q 지원)
  - 공통 응답 포맷: `{ ok, message, data, page }`

### 11.2 프론트(UI)
- ✅ 검색바: `검색 input + (페이지사이즈 dropdown) + 검색 버튼`
- ✅ 우측 상단 버튼:
  - 회원 생성(상세 팝업 재사용)
  - 선택 삭제(테이블 체크박스 기반)
- ✅ 목록은 JSP에서 직접 loop 렌더링하지 않고, **ajax(JSON)로 받아서 DOM 렌더링**
- ✅ 상세 팝업:
  - 이메일 수정
  - 비밀번호 변경(입력 시)
  - 관리자 메모(memo) 수정
  - 프로필 업로드
  - **커스텀 프로필 있을 때만**: 파일명 표시 + 다운로드 링크 + “삭제 체크박스” 표시  
    - `profile_image == NULL`(default 상태)면 파일 영역/삭제 UI 숨김

### 11.3 백엔드(User 중심)
- ✅ `UserDAO`:
  - `countUsersByQuery(q)` / `findUsersByQuery(q, offset, size)` 추가
  - 목록 조회 SELECT에 `email/profile_image/memo/updated_at` 포함(비밀번호는 제외)
  - ✅ 생성 시 `profile_image`는 NULL로 저장(디폴트 경로 DB 저장 금지)
- ✅ `UserService`:
  - `authenticate()` 도입: 해시/평문 동시 검증 + 평문이면 해시로 업그레이드 저장
  - `updateEmail()`, `updateMemo()`, `changePassword()`, `clearProfileImage()` 등 확장
  - ✅ 회원 생성 시 memo 저장 처리(생성 후 memo 업데이트 또는 insert 시 포함)
- ✅ `AdminMemberCreateServlet`:
  - ✅ create 파라미터에 `memo` 수신 및 저장 반영
- ✅ `AdminMemberUpdateServlet`:
  - email / memo / newPassword / deleteProfile 처리

### 11.4 공통 JS 개선
- ✅ `resources/js/common.js`의 `fetchJson` 개선:
  - HTTP 400/409 등 **비정상 status여도** response body(JSON)를 파싱
  - `{ ok:false, message:"..." }`의 `message`를 모달에 그대로 출력 가능
  - 결과: “처리 실패: HTTP 400” 대신 서버 메시지 노출

---

## 12. UI 공통(폰트) 변경
- ✅ `resources/css/common.css`에 Google Fonts `Noto Sans KR` 적용
  - 기본 폰트(Segoe UI/Malgun Gothic) 대신 전역 폰트로 통일
  - button/input/select/textarea까지 동일 폰트 스택 적용

---

## 13. 완료 체크리스트(현재까지)

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
- [ ] (선택) 기존 디폴트 경로 저장 레코드 NULL 정리(필요 시 6.3 SQL 실행)

### Admin
- [x] 관리자 로그인(AJAX) 구현
- [x] 세션 저장(`LOGIN_ADMIN`)
- [x] `/admin/*` 보호 필터 적용
- [x] 로그아웃 구현
- [x] 대시보드 화면 연결
- [x] 대시보드: 전체 회원 수 통계 DB 연동
- [x] 회원관리: 목록 화면 진입(`/admin/members`)
- [x] 회원관리: JSON 목록 응답(page/size/q)
- [x] 회원관리: 상세 수정 업데이트(email/memo/pw/deleteProfile)
- [x] 회원관리: 생성(create) memo 반영
- [x] 회원관리: 프로필 디폴트(엑박/삭제UI) 정책 정리 및 반영
- [x] 회원관리: 업로드(profile upload) / 삭제(선택삭제) 기능 구현(서블릿 포함)
- [x] 공통 에러모달: HTTP 오류에서도 서버 message 출력(fetchJson 개선)
- [ ] 공통 페이징 유틸(common.js) “사용처 기준” 최종 정리(함수/옵션 정리, 중복 제거)

---

## 14. 커밋 스냅샷(최근 작업 반영)
- Smart Tomcat 정답 세팅(Deployment directory=`src/main/webapp`) 반영
- 정적 리소스 경로 정리(`webapp/resources/*`)
- 관리자 로그인(AJAX) + 세션 적용
- 관리자 인증 필터 적용(`/admin/*`)
- 관리자 로그아웃 구현
- 관리자 대시보드 연결 + CSS 분리(layout/dashboard/auth)
- 대시보드 통계: 전체 회원 수 DB 연동
- ✅ 회원관리 페이지(`/admin/members`) 추가: GET(JSP) + POST(JSON)
- ✅ 회원관리 UI: ajax 렌더링 + 검색/페이지사이즈 + 상세 팝업 + 메모/이메일/비번 변경
- ✅ 프로필 기본 이미지 경로/정책 확정: DB 기본은 NULL, 화면에서 default 렌더링
- ✅ 회원 생성 memo 저장 반영
- ✅ 공통 fetchJson 개선: HTTP 400/409에서도 서버 message 모달 노출
- ✅ 공통 폰트(Noto Sans KR) 적용

---

## 15. 다음 구현 순서(추천)

### 15.1 대시보드 통계 확장(우선순위 1)
- [ ] 등록된 회의실 수
- [ ] 오늘 예약 건수
- [ ] 이번 달 예약 건수

### 15.2 공통 유틸 정리(우선순위 2)
- [ ] common.js 페이징 유틸 함수/옵션 정리(불필요 옵션 제거, 호출부 통일)
- [ ] 공통 fetch 래퍼 규약 정리(throwOnOkFalse, FormData 처리 등 문서화)

### 15.3 회의실/예약 도메인 구현(우선순위 3)
- [ ] room DAO/Service 구현 + 관리자 CRUD
- [ ] reservation DAO/Service 구현 + 예약 정책(중복예약, 시간검증)

---

## 16. 다음 대화에서 시작 문구(추천)
- `meeting 스냅샷 기준으로 대시보드 통계(회의실/예약)부터 계속하자`
