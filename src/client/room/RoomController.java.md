# RoomController.java

担当A（ルーム管理）のクライアント側コントローラ。部屋の作成（`createRoom()`）・参加（`joinRoom()`）・一覧取得要求（`requestRoomList()`）・ゲーム開始要求（`startGame()`）をサーバーへ送信する。サーバーからの通知（`onRoomMessage()`）を受けて、作成・参加の成功通知、部屋一覧（`ROOM_LIST`）、部屋メンバー一覧（`ROOM_MEMBERS`）、エラー（`ROOM_ERROR`）をそれぞれ解析し、`RoomPanel` の表示を更新する。現在の部屋IDが確定すると `DrawController.setRoomId()` にも反映する。

また、`setOnGameStartRequested()`で登録したコールバックを`startGame()`実行時に呼び出す暫定フックを持つ。本来はサーバーの`GAME_ROUND_START`受信（担当C実装）をきっかけに画面遷移すべきだが、担当Cの実装が済むまでの代用として、`client/Main.java`（統合エントリポイント）がこのフックを使いローカルで画面切り替えを行っている。
