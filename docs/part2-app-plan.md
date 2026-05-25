# [Part 2] Claude Code - 안드로이드 앱 계획서

> Part 1의 GitHub repo에 쌓인 글을 보여주는 안드로이드 앱.
> Readle 클론. 최소 기능부터 시작해서 점진적 확장.

---

## 1. 전제

- Part 1 (데이터 파이프라인)이 안정적으로 돌고 있어야 시작
- 본인은 안드로이드 앱 경험 X
- 본인은 Kotlin은 익숙 (백엔드)
- 본인만 사용. Play Store 업로드 X. APK 사이드로드

---

## 2. 스택 결정

| 항목 | 선택 | 이유 |
|------|------|------|
| 언어 | **Kotlin** | 본인 친숙 |
| UI 프레임워크 | **Jetpack Compose** | 최신 표준, 코드 적음, XML 안 봐도 됨 |
| HTTP | **Ktor Client** | Kotlin 친화적. Retrofit도 OK |
| JSON | **Kotlinx Serialization** | Kotlin 표준 |
| 이미지 | **Coil** | Compose 친화적 |
| 로컬 캐시 | **DataStore** + **Room** | DataStore는 메타, Room은 글 |
| 비동기 | **Kotlin Coroutines + Flow** | Compose와 자연스럽게 |
| DI | **없음 (수동)** | MVP 단계엔 오버킬 |
| 빌드 | **Android Studio** + Gradle | |

---

## 3. 화면 구성 (MVP)

### 3.1 화면 3개만

1. **HomeScreen**: 최근 7일 글 카드 리스트. 상단에 레벨 탭 (A2/B1/B2/C1)
2. **ArticleScreen**: 글 본문. 단어 탭하면 뜻 팝업. 하단에 vocab/key_phrases
3. **SettingsScreen**: 기본 레벨 설정, 글자 크기, 다크모드

### 3.2 와이어프레임 (텍스트)

```
[HomeScreen]
┌─────────────────────────┐
│  Reading                ⚙ │
│  ┌─[A2]─[B1✓]─[B2]─[C1]─┐│
│  └────────────────────────┘│
│                          │
│  Today · 2026-05-21      │
│  ┌─────────────────────┐ │
│  │ 🖥 tech              │ │
│  │ Google Makes Search │ │
│  │ Better              │ │
│  │ 150 words · 1 min   │ │
│  └─────────────────────┘ │
│  ┌─────────────────────┐ │
│  │ 💼 tech_business    │ │
│  │ Apple Buys ...      │ │
│  └─────────────────────┘ │
│  ...                    │
│                          │
│  Yesterday · 2026-05-20  │
│  ...                    │
└─────────────────────────┘

[ArticleScreen]
┌─────────────────────────┐
│ ← Google Makes Search Better│
│   tech · B1 · 1 min     │
│                          │
│  Google has a new way    │
│  to <search>...          │  ← <search>는 vocab 단어, 밑줄
│                          │
│  ...                    │
│                          │
│ ─────────────────────── │
│  📚 New Words            │
│  search 검색             │
│  user 사용자             │
│                          │
│  💡 Key Phrases          │
│  a new way to ~하는 새로운 방법│
└─────────────────────────┘

[탭 시 BottomSheet]
┌─────────────────────────┐
│  search                  │
│  (noun)                  │
│  검색                    │
└─────────────────────────┘
```

---

## 4. 데이터 흐름

```
앱 시작
  ↓
1. DataStore에서 마지막 갱신 시간 확인
  ↓
2. 1시간 이상 지났으면 index.json fetch
   (raw.githubusercontent.com/jongheon/my-readle-content/main/articles/index.json)
  ↓
3. 새 날짜 있으면 해당 JSON들도 fetch
   (raw.githubusercontent.com/.../articles/2026-05-21.json)
  ↓
4. Room DB에 저장
  ↓
5. HomeScreen에서 Room을 Flow로 관찰 → 자동 갱신
```

---

## 5. 프로젝트 구조

```
app/
├── src/main/
│   ├── java/com/jongheon/myreadle/
│   │   ├── MainActivity.kt
│   │   ├── MyReadleApp.kt              ← Application
│   │   ├── data/
│   │   │   ├── remote/
│   │   │   │   ├── GitHubApi.kt       ← Ktor client
│   │   │   │   └── dto/               ← JSON 매핑 클래스
│   │   │   ├── local/
│   │   │   │   ├── ArticleDao.kt
│   │   │   │   ├── ArticleEntity.kt
│   │   │   │   ├── MyReadleDatabase.kt
│   │   │   │   └── SettingsDataStore.kt
│   │   │   └── ArticleRepository.kt   ← Remote + Local 통합
│   │   ├── domain/
│   │   │   └── model/
│   │   │       ├── Article.kt
│   │   │       ├── Level.kt
│   │   │       └── Vocab.kt
│   │   └── ui/
│   │       ├── home/
│   │       │   ├── HomeScreen.kt
│   │       │   └── HomeViewModel.kt
│   │       ├── article/
│   │       │   ├── ArticleScreen.kt
│   │       │   ├── ArticleViewModel.kt
│   │       │   └── components/
│   │       │       ├── ClickableArticleText.kt
│   │       │       └── VocabBottomSheet.kt
│   │       ├── settings/
│   │       │   └── SettingsScreen.kt
│   │       └── theme/
│   │           ├── Theme.kt
│   │           ├── Color.kt
│   │           └── Type.kt
│   └── res/
└── build.gradle.kts
```

---

## 6. 핵심 구현 포인트

### 6.1 단어 탭 인터랙션 (가장 까다로움)

```kotlin
@Composable
fun ClickableArticleText(
    body: String,
    vocabs: List<Vocab>,
    onVocabClick: (Vocab) -> Unit
) {
    val annotatedString = buildAnnotatedString {
        // body를 단어 단위로 쪼개고, vocab에 있는 단어면 annotation 부착
        // SpanStyle로 underline + 색상 표시
    }

    ClickableText(
        text = annotatedString,
        onClick = { offset ->
            annotatedString.getStringAnnotations("VOCAB", offset, offset)
                .firstOrNull()?.let { ann ->
                    val vocab = vocabs.find { it.word == ann.item }
                    vocab?.let(onVocabClick)
                }
        }
    )
}
```

주의:
- 단어 매칭은 lowercase + 구두점 제거 후 비교
- 같은 단어 여러 번 등장 시 모두 클릭 가능하게
- 단어 경계 정확히 (regex `\b` 사용)

### 6.2 레벨 전환

상단 탭에서 레벨 바꿔도 HomeScreen은 같은 글 목록. ArticleScreen만 다른 레벨 body 표시.
DataStore에 선택한 레벨 저장.

### 6.3 캐싱 전략

```kotlin
suspend fun getArticles(): Flow<List<Article>> {
    return articleDao.getAllArticles()
        .onStart {
            // 백그라운드에서 fresh fetch 시도
            refreshIfStale()
        }
}

private suspend fun refreshIfStale() {
    val lastUpdate = settings.lastIndexUpdate.first()
    if (now() - lastUpdate < 1.hours) return

    try {
        val index = api.fetchIndex()
        val localDates = articleDao.getAllDates()
        val newDates = index.dates.map { it.date } - localDates.toSet()

        newDates.forEach { date ->
            val daily = api.fetchDaily(date)
            articleDao.insertAll(daily.toEntities())
        }
        settings.setLastIndexUpdate(now())
    } catch (e: Exception) {
        // 네트워크 실패 → 캐시된 거 그대로 보여줌
    }
}
```

### 6.4 schema_version 처리

```kotlin
@Serializable
data class DailyArticlesDto(
    val schema_version: Int = 1,
    val date: String,
    val topics: List<TopicDto>
)

// Repository에서
if (dto.schema_version > SUPPORTED_SCHEMA) {
    // 사용자에게 "앱 업데이트 필요" 표시
}
```

---

## 7. 작업 순서 (Claude Code한테 시키는 단위)

### Phase A: 프로젝트 셋업

1. Android Studio에서 Empty Compose Activity 프로젝트 생성
2. minSdk 26, targetSdk 34 정도
3. build.gradle.kts에 의존성 추가:
   - compose, ktor-client, kotlinx-serialization
   - room, datastore, coil
4. AndroidManifest에 INTERNET 권한

### Phase B: 데이터 레이어

1. Article, Level, Vocab 데이터 클래스
2. DTO 클래스 (JSON 매핑)
3. GitHubApi (Ktor) - index, daily fetch
4. Room: ArticleEntity, ArticleDao, Database
5. SettingsDataStore: 선택 레벨, 마지막 갱신 시간
6. ArticleRepository - 위 둘 통합

### Phase C: 홈 화면

1. HomeViewModel: articles Flow, 선택 레벨 상태
2. HomeScreen: 레벨 탭 + 날짜별 글 카드 리스트
3. 네비게이션 (글 카드 클릭 → ArticleScreen)

### Phase D: 글 화면

1. ArticleViewModel: 글 1개 로드
2. ArticleScreen: 본문 + vocab 섹션
3. ClickableArticleText: vocab 단어 클릭 가능
4. VocabBottomSheet: 단어 뜻 팝업

### Phase E: 설정 + 다크모드

1. SettingsScreen: 기본 레벨, 글자 크기
2. Material 3 dynamic color + dark mode

### Phase F: 배포

1. release 키스토어 생성 (한 번만)
2. `./gradlew assembleRelease`
3. APK를 본인 폰에 설치 (USB 또는 클라우드 드라이브)
4. 매번 업데이트 시 같은 키로 서명

---

## 8. Claude Code 활용 팁

### 8.1 한 번에 너무 많이 시키지 않기

Phase 단위로 끊어서 시키고, 매번 빌드 → 폰에서 실행 → 동작 확인 → 다음 Phase.

### 8.2 효과적인 프롬프트 패턴

```
"Phase B를 진행해줘.
- @docs/part2-app-plan.md 의 6.3 캐싱 전략 참고
- @app/src/main/java/com/jongheon/myreadle/data 아래에 파일 생성
- JSON 스키마는 @docs/part1-cowork-pipeline.md 의 4.1, 4.2 참고
- 완료 후 ./gradlew assembleDebug 까지 통과시켜줘"
```

스키마 정의 파일을 항상 컨텍스트에 포함시키는 게 핵심.

### 8.3 모르는 영역은 학습 모드

본인 안드로이드 처음이니까 Claude Code한테 코드만 받지 말고 **왜 그렇게 했는지 설명도 같이** 요구. 안 그러면 디버깅 못함.

```
"이 코드 설명해줘. 특히 StateFlow vs SharedFlow 왜 이걸 골랐는지."
```

### 8.4 의존성 관리

Android 의존성 버전 매번 검색해서 최신 것 쓰지 말고, **검증된 BOM이나 Compose BOM** 사용. Claude Code가 최신 버전 권장하면 일단 의심.

---

## 9. 위험 요소

| 위험 | 대응 |
|------|------|
| 안드로이드 처음이라 막힘 | Claude Code한테 설명 같이 받기. Compose 공식 codelabs 1개 미리 |
| Gradle 빌드 에러 지옥 | 첫 셋업은 새 프로젝트 그대로 두고 점진적 추가 |
| Room 마이그레이션 | MVP는 그냥 fallbackToDestructiveMigration. 데이터 날아가도 GitHub에서 다시 받음 |
| 화면 회전 시 상태 손실 | ViewModel + rememberSaveable 사용 |
| 큰 JSON 메모리 부담 | 한 번에 7일치만 로드. 더 이전 건 별도 화면 |

---

## 10. v2 (시간 남으면)

- 단어장: 봤던 vocab 누적 저장. 검색/복습
- Anki export: vocab를 .apkg로 내보내기
- TTS: Android 내장 TTS로 본문 읽기
- 진도 추적: 읽은 글 표시
- 위젯: 홈 화면에 오늘 글 1개
- Yorkie 연동 (?): 멀티 디바이스 진도 동기화 - OSSCA 지원서 토이로 엮으면 일석이조

---

## 11. 시작 시점

Part 1이 1주일 안정적으로 돌면 시작. 그 전에는 본인이 raw URL로 직접 봄.

OSSCA 마감 (6월 중순) 전엔 Yorkie 준비가 우선. 앱 본격 개발은 OSSCA 지원서 제출 후.
