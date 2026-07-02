# GameClient.java

クライアント側の通信窓口となるクラス（担当D：基盤）。指定したサーバーアドレス・ポートへソケット接続し、送信用の `sendMessage()` を各Controller（Room/Draw/Game）に提供する。また、受信専用のバックグラウンドスレッドを起動し、サーバーから届いたメッセージを「コマンド:データ」の形式で解析、`Protocol` の定数をもとに `switch` 文で該当するController（`RoomController` / `DrawController` / `GameController`）へ振り分ける。
