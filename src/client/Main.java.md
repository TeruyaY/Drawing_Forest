# Main.java

ロビー画面（担当A）とお絵描き画面（担当B）を1つのウィンドウ・1本の`GameClient`で繋ぐ統合エントリポイント。`CardLayout`で「ロビー」カードと「ゲーム」カードを切り替える構成で、`RoomController.init()`と`DrawController.init()`を同じクライアントインスタンスに対して呼び出す。

本来は「ゲーム開始」→サーバーが`GAME_ROUND_START`を送信→担当Cの`GameController`が画面遷移する、という流れが正しいが、担当Cの実装（`GameManager.handleGameStart`など）が未着手のため、`RoomController.setOnGameStartRequested()`に登録したコールバックで「ゲーム開始」ボタン押下時にローカルで即座にゲーム画面へ切り替える暫定トリガーを使っている。ゲーム画面内のチャット・お題・スコア表示部分はプレースホルダーのラベルのみで、担当Cの実装待ち。担当Cの実装が入り次第、この暫定トリガーは`GAME_ROUND_START`受信をきっかけにした遷移へ置き換える想定。
