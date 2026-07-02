# server/

ゲームサーバー側のコード一式。接続受付を行う `GameServer.java`、プレイヤーごとの通信を処理する `ClientHandler.java` を基盤に、担当A（ルーム管理）用の `room/`、担当B（お絵描き）用の `draw/`、担当C（ゲーム進行・チャット）用の `game/` の各Managerパッケージで構成される。`ClientHandler` が受信コマンドを解析し、各Managerへ処理を振り分ける。
