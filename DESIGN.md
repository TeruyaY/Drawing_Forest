# 設計図（クラス図・オブジェクト図・ユースケース図・シーケンス図）

このドキュメントは、現在の実装（`src/` 以下、`README.md` / `PRODUCT.md` の仕様）を踏まえて、
**本来は開発着手前に作成しておくべきだった設計成果物** を事後的にまとめ直したものです。
今後の機能追加・リファクタリング時に「全体像を確認する共通の地図」として使ってください。

> Mermaidには UML の「ユースケース図」「オブジェクト図」を描く専用記法が存在しないため、
> この2つは `flowchart` 記法を使い、UMLの見た目（アクター＝棒人間アイコン、ユースケース＝角丸ノード、
> オブジェクト＝`インスタンス名 : クラス名` 表記）に寄せて表現しています。

---

## 1. ユースケース図

「誰が」「何をできるか」を、部屋管理（担当A）・お絵描き（担当B）・ゲーム進行（担当C）の3領域を横断してまとめたものです。
`描き手` `回答者` はラウンドごとにシステムが動的に割り当てる **プレイヤーの一時的な役割**（`GAME_ROUND_START` で通知される `DRAWER` / `GUESSER`）であるため、`プレイヤー` の特化として表現しています。

```mermaid
flowchart LR
    player(["🧑 プレイヤー"])
    drawer(["🖊️ 描き手（Drawer役）"])
    guesser(["💬 回答者（Guesser役）"])
    timer(["⏱ ラウンドタイマー（システム）"])

    drawer -.->|ロール・その回のみ| player
    guesser -.->|ロール・その回のみ| player

    subgraph SYS["システム境界：お絵描きの森"]
        direction TB
        uc1(["部屋を作成する"])
        uc2(["部屋一覧を見る"])
        uc3(["部屋に参加する"])
        uc4(["ゲーム開始を押して準備完了にする"])
        uc11(["ラウンドを開始する"])
        uc5(["絵を描く"])
        uc6(["キャンバスを全消しする"])
        uc7(["チャットで回答を送信する"])
        uc8(["チャット・進行状況を閲覧する"])
        uc9(["スコアを確認する"])
        uc12(["制限時間でラウンドを終了する"])
        uc10(["最終結果を見てロビーに戻る"])
    end

    player --> uc1
    player --> uc2
    player --> uc3
    player --> uc4
    player --> uc8
    player --> uc9
    player --> uc10
    drawer --> uc5
    drawer --> uc6
    guesser --> uc7
    timer --> uc12

    uc4 -.->|"«include» 全員準備完了で発火"| uc11
    uc6 -.->|"«include»"| uc5
    uc7 -.->|"«extend» 正解した時のみ"| uc9
    uc12 -.->|"«include» 誰も正解しなくても発生"| uc9
```

### 補足（実装との対応）
- `uc1〜uc3` は `Protocol.ROOM_CREATE / ROOM_JOIN / ROOM_LIST_REQUEST`、`server.room.RoomManager` に対応。
- `uc4 → uc11` は「部屋の全員が『ゲーム開始』を押すまでラウンドを始めない」という `GameManager.handleGameStart` の仕様そのもの。
- `uc5・uc6` は `GameManager.canDraw()` によって **Drawer役以外は実行できない** よう制限されている（描く権限のガード）。
- `uc7` の正誤判定は `Theme.isCorrect()`、得点計算は `GameManager.calculatePoints()`。
- `uc12` では、誰も正解しなくても描き手に加点が発生しないだけで、ラウンドは時間切れで終了する。

---

## 2. クラス図

Javaのパッケージ構成（`common` / `server.*` / `client.*`）をそのまま名前空間として使い、
クラスの責務・主要メンバー・依存関係を整理しています。`RoomManager` `DrawManager` `GameManager` `*Controller` は
すべて **static メソッドのみを持つユーティリティクラス**（インスタンス化されない、部屋やゲームの状態をクラス変数として共有する設計）です。

```mermaid
classDiagram
    direction LR

    namespace common {
        class Protocol {
            <<utility>>
            +ROOM_CREATE : String
            +ROOM_JOIN : String
            +DRAW_DATA : String
            +DRAW_CLEAR : String
            +GAME_START : String
            +GAME_ROUND_START : String
            +CHAT_SUBMIT : String
            +GAME_SCORE_UPDATE : String
            +GAME_READY_UPDATE : String
        }
        class WordManager {
            <<utility>>
            -WORDS : String[]
            +getRandomWord() String
        }
    }

    namespace server {
        class GameServer {
            <<entrypoint>>
            +PORT : int
            +main(args) void
        }
        class ClientHandler {
            -socket : Socket
            -userName : String
            +run() void
            +sendMessage(message) void
            +getUserName() String
            +setUserName(name) void
        }
    }

    namespace server_room {
        class RoomManager {
            <<utility>>
            -rooms : Map~String, Room~
            -clientRooms : Map~ClientHandler, String~
            +handleCreateRoom(client, data) void
            +handleJoinRoom(client, data) void
            +handleRoomListRequest(client) void
            +broadcastToRoom(roomName, message) void
            +getRoomMembers(roomName) List~ClientHandler~
            +getRoomOf(client) String
            +removeClient(client) void
        }
        class Room {
            -name : String
            -members : List~ClientHandler~
            +addMember(client) void
            +removeMember(client) void
            +memberNames() List~String~
        }
    }

    namespace server_draw {
        class DrawManager {
            <<utility>>
            -roomMembers : Map~String, Set~ClientHandler~~
            +joinRoom(roomId, client) void
            +leaveRoom(roomId, client) void
            +handleDrawData(client, data) void
            +handleClear(client, roomId) void
        }
    }

    namespace server_game {
        class GameManager {
            <<utility>>
            -games : Map~String, RoomGame~
            -readySets : Map~String, Set~String~~
            +handleGameStart(client, data) void
            +handleChatSubmit(client, data) void
            +canDraw(client, roomName) bool
            +isGameActive(roomName) bool
            +onRoomMembershipChanged(roomName) void
            -startRoundLocked(game) void
            -finishRoundLocked(game, reason) void
            -awardDrawerBonusLocked(game) void
        }
        class RoomGame {
            -roomName : String
            -drawingOrder : List~String~
            -activePlayers : Set~String~
            -correctUsers : Set~String~
            -scores : Map~String, Integer~
            -drawerName : String
            -currentTheme : Theme
            -roundActive : bool
            +addScore(userName, points) void
            +buildScoreText() String
        }
        class Theme {
            -displayName : String
            -acceptedAnswers : List~String~
            +isCorrect(answer) bool
            +getDisplayName() String
        }
    }

    namespace client {
        class GameClient {
            -socket : Socket
            +connect(host, port) void
            +sendMessage(message) void
            -startListening() void
        }
        class UiTheme {
            <<utility>>
            +installGlobalDefaults() void
            +primaryButton(text) JButton
        }
        class FeedbackEffect {
            +play(type, title, message) void
            +stop() void
        }
    }

    namespace client_room {
        class RoomController {
            <<utility>>
            -currentRoom : String
            -userName : String
            +createRoom(roomName, userName) void
            +joinRoom(roomName, userName) void
            +startGame() void
            +onRoomMessage(command, data) void
            +getCurrentRoom() String
        }
        class RoomPanel {
            +setRooms(rooms) void
            +setMembers(members) void
            +setReadyStatus(message) void
            +showStatus(message) void
        }
        class RoomLauncher {
            <<entrypoint>>
            +main(args) void
        }
        class RoomInfo {
            -name : String
            -memberCount : int
        }
    }

    namespace client_draw {
        class DrawController {
            <<utility>>
            -roomId : String
            +sendLine(x1, y1, x2, y2, color, width) void
            +requestClear() void
            +onDrawReceived(data) void
            +onClearReceived() void
            +setDrawingEnabled(enabled) void
        }
        class DrawPanel {
            -canvas : BufferedImage
            -drawingEnabled : bool
            +drawRemoteLine(x1, y1, x2, y2, color, width) void
            +clearCanvas() void
            +clearCanvasFromRemote() void
            +setDrawingEnabled(enabled) void
        }
        class DrawLauncher {
            <<entrypoint>>
            +main(args) void
            +buildToolDock(canvas) JPanel
        }
    }

    namespace client_game {
        class GameController {
            <<utility>>
            +startGame() void
            +submitChat(text) void
            +onGameUpdate(command, data) void
            +setRoundStartedListener(listener) void
            +setGameEndListener(listener) void
        }
        class GamePanel {
            +setRoundInfo(round, role, drawer, theme) void
            +setScores(scoreText) void
            +setTimeRemaining(seconds) void
            +showRoundTransition(theme) void
        }
        class ChatPanel {
            +addChatMessage(message) void
            +setRoundInfo(role, drawer, theme) void
            +showResult(text) void
            -send(event) void
        }
        class ResultPanel {
            +setScores(scoreText) void
            +setOnClose(listener) void
        }
    }

    %% ---- サーバー側の依存関係 ----
    GameServer ..> ClientHandler : 生成しスレッド起動
    ClientHandler ..> Protocol : コマンド判定
    ClientHandler ..> RoomManager : 委譲
    ClientHandler ..> DrawManager : 委譲
    ClientHandler ..> GameManager : 委譲

    RoomManager "1" *-- "many" Room : 管理
    RoomManager ..> DrawManager : 入退室を連携
    RoomManager ..> GameManager : メンバー変更を通知

    DrawManager ..> RoomManager : 所属部屋を確認
    DrawManager ..> GameManager : 描画権限を確認

    GameManager "1" *-- "many" RoomGame : 進行管理
    GameManager ..> RoomManager : 全員へ配信
    RoomGame --> "1" Theme : currentTheme
    GameManager ..> Theme : 出題

    %% ---- クライアント側の依存関係 ----
    GameClient ..> Protocol : コマンド組立/判定
    GameClient ..> RoomController : 受信振り分け
    GameClient ..> DrawController : 受信振り分け
    GameClient ..> GameController : 受信振り分け

    RoomController o-- RoomPanel : 画面更新
    RoomController "1" *-- "many" RoomInfo : 部屋一覧
    RoomController ..> DrawController : 部屋ID共有
    RoomController ..> GameClient : 送信

    RoomLauncher ..> RoomController : 初期化
    RoomLauncher ..> DrawController : 初期化
    RoomLauncher ..> GameController : 初期化
    RoomLauncher *-- RoomPanel
    RoomLauncher *-- DrawPanel
    RoomLauncher *-- GamePanel
    RoomLauncher *-- ResultPanel

    DrawController o-- DrawPanel : 描画反映
    DrawController ..> GameClient : 送信
    DrawLauncher ..> DrawController : 初期化
    DrawLauncher *-- DrawPanel

    GameController o-- GamePanel : 画面更新
    GameController o-- ChatPanel : 画面更新
    GameController ..> DrawController : 描画可否切替
    GameController ..> RoomController : 現在の部屋取得
    GameController ..> GameClient : 送信
    ChatPanel ..> GameController : submitChat()

    GamePanel *-- ChatPanel : 内包
    GamePanel *-- FeedbackEffect
    ChatPanel *-- FeedbackEffect
    ResultPanel *-- FeedbackEffect
```

---

## 3. オブジェクト図

「ライオン部屋」に Alice・Bob が参加し、Alice が描き手としてラウンド1が進行中、というある瞬間のインスタンス状態のスナップショットです。
`RoomManager` `DrawManager` `RoomController` `DrawController` は static ユーティリティなので “1プロセスに1つだけ存在する状態のかたまり” として、あえてインスタンスのように図示しています。

```mermaid
flowchart TB
    subgraph SERVER["サーバー側メモリ状態（ライオン部屋・ラウンド1進行中）"]
        direction TB
        chA["client1 : ClientHandler<br/>userName = &quot;Alice&quot;"]
        chB["client2 : ClientHandler<br/>userName = &quot;Bob&quot;"]
        room["lion : Room<br/>name = &quot;ライオン部屋&quot;<br/>members = [client1, client2]"]
        game["game_lion : RoomGame<br/>roomName = &quot;ライオン部屋&quot;<br/>drawerName = &quot;Alice&quot;<br/>drawerIndex = 1<br/>roundActive = true<br/>guesserCount = 1<br/>correctCount = 0<br/>scores = {Alice:0, Bob:0}"]
        theme["appleTheme : Theme<br/>displayName = &quot;apple&quot;<br/>acceptedAnswers = [&quot;apple&quot;]"]
        drawSet["roomMembers[&quot;ライオン部屋&quot;] : Set&lt;ClientHandler&gt;<br/>= {client1, client2}"]

        room -->|members| chA
        room -->|members| chB
        game -.->|drawerName = Alice| chA
        game -->|currentTheme| theme
        drawSet --> chA
        drawSet --> chB
    end

    subgraph CLIENT_A["Aliceのクライアント（Drawer役）"]
        direction TB
        gcA["gameClientA : GameClient"]
        rcA["RoomController の状態<br/>currentRoom = &quot;ライオン部屋&quot;<br/>userName = &quot;Alice&quot;"]
        dcA["DrawController の状態<br/>roomId = &quot;ライオン部屋&quot;"]
        panelA["drawPanelA : DrawPanel<br/>drawingEnabled = true"]
        dcA --> panelA
    end

    subgraph CLIENT_B["Bobのクライアント（Guesser役）"]
        direction TB
        gcB["gameClientB : GameClient"]
        rcB["RoomController の状態<br/>currentRoom = &quot;ライオン部屋&quot;<br/>userName = &quot;Bob&quot;"]
        dcB["DrawController の状態<br/>roomId = &quot;ライオン部屋&quot;"]
        panelB["drawPanelB : DrawPanel<br/>drawingEnabled = false"]
        dcB --> panelB
    end

    chA <-->|Socket| gcA
    chB <-->|Socket| gcB
```

### 補足
- `RoomManager.ensureUniqueName()` により、同名（例：初期値`Player`のまま）で入室した場合はサーバー側が `Player2` のように自動採番する。この図では既に一意な名前 `Alice` `Bob` を使っている状態。
- `DrawManager.roomMembers` は「描画データを送ってよい相手」の集合であり、`RoomManager` が持つ `Room.members`（部屋の正式メンバー）とは別に管理されている（担当A/Bの責務分離のため）。

---

## 4. シーケンス図

1本の図に全フローを詰め込むと可読性が落ちるため、代表的な3つのシナリオに分けています。

### 4-1. 部屋参加〜全員準備完了でラウンド開始

```mermaid
sequenceDiagram
    autonumber
    actor Alice
    participant RC_A as RoomController(Alice)
    participant Srv as サーバー<br/>(ClientHandler / RoomManager / GameManager)
    participant RC_B as RoomController(Bob)
    actor Bob
    participant GC_A as GameController(Alice)
    participant GC_B as GameController(Bob)

    Alice->>RC_A: 部屋名「ライオン部屋」で作成ボタン
    RC_A->>Srv: ROOM_CREATE:ライオン部屋,Alice
    Srv-->>RC_A: ROOM_CREATED_NOTIFY / ROOM_JOINED_NOTIFY
    Srv-->>RC_A: ROOM_MEMBERS:ライオン部屋,Alice

    Bob->>RC_B: 一覧から「ライオン部屋」を選び参加
    RC_B->>Srv: ROOM_JOIN:ライオン部屋,Bob
    Srv-->>RC_A: ROOM_MEMBERS:ライオン部屋,Alice|Bob
    Srv-->>RC_B: ROOM_JOINED_NOTIFY / ROOM_MEMBERS

    Alice->>RC_A: 「ゲーム開始」ボタン
    RC_A->>Srv: GAME_START:ライオン部屋
    Note over Srv: readySet = {Alice} (1/2)
    Srv-->>RC_A: GAME_READY_UPDATE:1,2,Alice
    Srv-->>RC_B: GAME_READY_UPDATE:1,2,Alice

    Bob->>RC_B: 「ゲーム開始」ボタン
    RC_B->>Srv: GAME_START:ライオン部屋
    Note over Srv: readySet = {Alice,Bob} = 全員<br/>→ RoomGame生成・ラウンド開始
    Srv-->>GC_A: GAME_ROUND_START:...,Alice,DRAWER,apple,60
    Srv-->>GC_B: GAME_ROUND_START:...,Alice,GUESSER,,60
    GC_A->>Alice: お絵描き画面へ切替（お題:apple・描画可）
    GC_B->>Bob: お絵描き画面へ切替（お題:非表示・描画不可）
```

### 4-2. 描画の同期とキャンバスクリア

```mermaid
sequenceDiagram
    autonumber
    actor Alice
    participant PanelA as DrawPanel(Alice)
    participant DC_A as DrawController(Alice)
    participant Srv as サーバー(DrawManager)
    participant DC_B as DrawController(Bob)
    participant PanelB as DrawPanel(Bob)
    actor Bob

    Alice->>PanelA: マウスドラッグ
    PanelA->>PanelA: ローカルに即座に線を描画
    PanelA->>DC_A: sendLine(x1,y1,x2,y2,color,width)
    DC_A->>Srv: DRAW_DATA:room,x1,y1,x2,y2,color,width
    Srv->>Srv: 送信者がDrawerか検証（canDraw）
    Srv-->>DC_B: DRAW_RCV:x1,y1,x2,y2,color,width
    DC_B->>PanelB: drawRemoteLine(...)
    PanelB->>Bob: 同じ線が表示される

    Alice->>PanelA: 「ボードを全消し」を確認込みで押下
    PanelA->>DC_A: requestClear()
    DC_A->>PanelA: clearCanvas()（自分の画面は即時反映）
    DC_A->>Srv: DRAW_CLEAR:room
    Srv-->>DC_B: DRAW_CLR:
    DC_B->>PanelB: clearCanvasFromRemote()
```

### 4-3. 回答〜正解判定〜ラウンド終了〜描き手への加点

```mermaid
sequenceDiagram
    autonumber
    actor Bob
    participant ChatB as ChatPanel(Bob)
    participant GC_B as GameController(Bob)
    participant Srv as サーバー(GameManager)
    participant GC_A as GameController(Alice)
    actor Alice

    Bob->>ChatB: 「apple」と入力して送信
    ChatB->>GC_B: submitChat("apple")
    GC_B->>Srv: CHAT_SUBMIT:room,apple
    Srv->>Srv: Theme.isCorrect("apple") == true
    Srv->>Srv: 得点計算 100+残り秒×10<br/>correctUsersにBobを追加
    Srv-->>GC_B: GAME_JUDGE_RESULT:CORRECT,180
    Srv-->>GC_A: GAME_SCORE_UPDATE:Alice=0;Bob=180
    Srv-->>GC_B: GAME_SCORE_UPDATE:Alice=0;Bob=180

    alt 残っていたGuesser全員が正解した
        Srv->>Srv: finishRoundLocked → awardDrawerBonusLocked(Alice)
        Srv-->>GC_A: GAME_ROUND_END:all_correct,apple
        Srv-->>GC_B: GAME_ROUND_END:all_correct,apple
        Srv-->>GC_A: GAME_SCORE_UPDATE:Alice=180;Bob=180
        Note over Srv: 3秒後、次のDrawerでラウンド再開<br/>（全員描き終えていれば代わりに GAME_END）
        Srv-->>GC_A: GAME_ROUND_START:...(次のラウンド)
        Srv-->>GC_B: GAME_ROUND_START:...(次のラウンド)
    end
```

---

## 参考にした資料

- `README.md` の「担当A/B/C/D」役割分担、通信仕様（Protocol一覧）
- `PRODUCT.md` の対象ユーザー・デザイン原則
- `src/common` `src/server` `src/client` 以下の実装（本ドキュメント作成時点の最新コード）
