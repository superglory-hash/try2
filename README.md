# 수도권 온에어 FM 라디오 (Android 네이티브 프로젝트)

수도권 18개 FM 라디오 채널을 실시간 스트리밍으로 청취하고 노래 가사를 검색할 수 있는 Android 네이티브 앱 프로젝트입니다.

## 포함된 주요 기능
1. **18개 수도권 방송국 완벽 지원**: KBS(4개), MBC(2개), SBS(2개), EBS, TBS(2개), YTN, 국악방송, CBS(2개), FEBC, BBS, CPBC.
2. **동적 스트림 토큰 Resolver**: 만료되는 방송사 API 토큰(SBS, MBC, KBS)을 ExoPlayer 재생 직전 자동 해석.
3. **AndroidX Media3 백그라운드 재생**: 화면 꺼짐 및 잠금 화면에서도 끊김 없이 지속 재생되는 Foreground Service.
4. **Jetpack Compose 모던 다크 UI**: 아날로그 감성 다이얼, 5바 애니메이션 이퀄라이저, 카테고리별 필터.
5. **실시간 가사 검색 (LRCLIB 연동)**: 방송에 흐르는 음악의 가사 및 정보 조회.

## APK 빌드 방법 (2가지)

### 방법 A: GitHub Actions를 통한 완전 자동 무설치 빌드 (권장)
1. 본 압축파일의 압축을 풀고, 모든 파일(`.github` 폴더 포함)을 본인의 GitHub 새 저장소(Public/Private)에 업로드(Push)합니다.
2. 저장소의 **Actions** 탭으로 이동하면 `Build Seoul FM Radio APK` 워크플로우가 자동으로 실행됩니다.
3. 빌드가 완료되면 아티팩트(`Seoul-FM-Radio-APK`) 항목에서 빌드된 `.apk` 파일을 바로 다운로드할 수 있습니다.

### 방법 B: Android Studio에서 로컬 빌드
1. Android Studio를 열고 **Open**을 눌러 본 프로젝트 폴더를 선택합니다.
2. Gradle Sync가 완료되면 상단 메뉴에서 `Build` > `Build Bundle(s) / APK(s)` > `Build APK(s)`를 클릭합니다.
3. `app/build/outputs/apk/debug/app-debug.apk` 파일이 생성됩니다.
