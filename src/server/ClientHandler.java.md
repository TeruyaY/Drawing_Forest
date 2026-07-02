# ClientHandler.java

各接続プレイヤー専属の通信処理を担うクラス（担当D：基盤、`Runnable`）。ソケットの入出力ストリームを保持し、クライアントから送られてくる「コマンド:データ」形式のメッセージを常時受信・解析する。`Protocol` の定数をもとに `switch` 文で担当A（`RoomManager`）・担当B（`DrawManager`）・担当C（`GameManager`）の各処理へ振り分ける。切断時には各Managerへのクライアント登録解除とストリーム・ソケットのクローズを行う。ユーザー名の保持・取得や、このクライアントへメッセージを送る `sendMessage()` も提供する。
