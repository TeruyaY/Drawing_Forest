# client/

プレイヤーのPC上で動くクライアント側のコード一式。通信の土台となる `GameClient.java` を頂点に、担当A（ルーム管理）用の `room/`、担当B（お絵描き）用の `draw/`、担当C（ゲーム進行・チャット）用の `game/` の各サブパッケージで構成される。サーバーから受信したメッセージは `GameClient` が各パッケージのControllerへ振り分ける。

`Main.java` はロビー画面とお絵描き画面を1本の`GameClient`・1つのウィンドウで繋ぐ統合エントリポイント。担当Cの実装が済むまでは、「ゲーム開始」ボタン押下をトリガーにした暫定的な画面遷移で代用している（詳細は`Main.java.md`参照）。個別の動作確認用には引き続き`room/RoomLauncher.java`・`draw/DrawLauncher.java`も利用できる。
