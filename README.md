scouter-plugin-server-alert-teamup
==================================

### Scouter server plugin to send a alert via teamup

-	본 프로젝트는 스카우터 서버 플러그인으로써 서버에서 발생한 Alert 메시지를 Teamup으로 전송하는 역할을 한다.
-	현재 지원되는 Alert의 종류는 다음과 같다.
	-	Agent의 CPU (warning / fatal)
	-	Agent의 Memory (warning / fatal)
	-	Agent의 Disk (warning / fatal)
	-	신규 Agent 연결
	-	Agent의 연결 해제
	-	Agent의 재접속
	-	Exception 발생

### Properties (스카우터 서버 설치 경로 하위의 conf/scouter.conf)

-	***ext\_plugin\_teampup\_send\_alert*** : teamup 메시지 발송 여부 (true / false) - 기본 값은 false
-	***ext\_plugin\_teampup\_debug*** : 로깅 표시 여부 - 기본 값은 false
-	***ext\_plugin\_teampup\_level*** : 수신 레벨(0 : INFO, 1 : WARN, 2 : ERROR, 3 : FATAL) - 기본 값은 0
-	***ext\_plugin\_teampup\_bot\_client\_id*** : 발급받은 client_id
-	***ext\_plugin\_teampup\_bot\_client\_secret*** : 발급받은 client_secret
-	***ext\_plugin\_teamup\_bot\_username*** : 메시지를 보낼 팀업 계정 ID
-	***ext\_plugin\_teamup\_bot\_password*** : 메시지지를 보낼 팀업 계정 비밀번호
-	***ext\_plugin\_teamup\_xlog\_enabled*** : Exception Alert 여부
-	***ext\_plugin\_teamup\_error\_escape\_method\_patterns*** : Exception Alert를 받지 않을 method patterns
-	***ext\_plugin\_teamup\_room\_id*** : 메세지를 전송할 방 번호

Example

```
# External Interface (teamup)
ext_plugin_teamup_send_alert=true
ext_plugin_teamup_xlog_enabled=true
ext_plugin_teamup_debug=false
ext_plugin_teamup_level=0
ext_plugin_teamup_bot_client_id=발급받은 client_id
ext_plugin_teamup_bot_client_secret=발급받은 client_secret
ext_plugin_teamup_bot_username=메시지를 보낼 팀업 계정 ID
ext_plugin_teamup_bot_password=메시지지를 보낼 팀업 계정 비밀번호
ext_plugin_teamup_room_id=메세지를 전송할 방 번호
ext_plugin_teamup_error_escape_method_patterns=com.zum.front.bot.sensor.*
```

### Dependencies

-	Project
	-	scouter.common
	-	scouter.server
-	Library
	-	commons-codec-1.9.jar
	-	commons-logging-1.2.jar
	-	gson-2.6.2.jar
	-	httpclient-4.5.2.jar
	-	httpcore-4.4.4.jar

### Build & Deploy

-	Build

	-	프로젝트 내의 build.xml을 실행한다.

-	Deploy

	-	빌드 후 프로젝트 하위에 out 디렉토리가 생기며, 디펜던시 라이브러리와 함께 scouter-plugin-server-alert-teamup.jar 파일을 복사하여 스카우터 서버 설치 경로 하위의 lib/ 폴더에 저장한다.

**Example View**<br> ![팀업 봇으로 Exception 알림 받기](/img/bot1.jpg)
