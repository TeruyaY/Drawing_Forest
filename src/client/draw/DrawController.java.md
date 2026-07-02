# DrawController.java

担当B（お絵描き）のクライアント側コントローラ。`DrawPanel` で線が引かれると `sendLine()` が呼ばれ、「`DRAW_DATA`:部屋ID,X1,Y1,X2,Y2,色」の形式に文字列化してサーバーへ送信する。逆に `GameClient` の受信スレッドから静的メソッド `onDrawReceived()` が呼ばれると、受け取った座標文字列を数値に戻し、EDT（イベントディスパッチスレッド）上で `DrawPanel.drawRemoteLine()` を呼んで他人の描いた線を描画する。部屋IDは `RoomController` 側が確定させた際に `setRoomId()` で上書きされる。
