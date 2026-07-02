# GameController.java

担当C（ゲーム進行・チャット）のクライアント側コントローラ。`GameClient`の受信スレッドが`GAME_ROUND_START`・`GAME_SCORE_UPDATE`をこのクラスの`onGameUpdate()`へ振り分ける実装になっているため、このファイルが存在しないとプロジェクト全体がコンパイルできない。

現状はビルドを通すための最小スタブで、受信内容を標準出力にログするだけ（`System.out.println`）。お題・役割の表示、スコア更新などの本実装は担当Cが行う想定。
