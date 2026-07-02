# Protocol.java

クライアント・サーバー間の通信で使う「コマンド名」を定数として一元定義するファイル。担当A（ルーム関連：`ROOM_CREATE`, `ROOM_JOIN`, `ROOM_LIST_REQUEST`, `ROOM_CREATED_NOTIFY`, `ROOM_JOINED_NOTIFY`, `ROOM_LIST`, `ROOM_MEMBERS`, `ROOM_ERROR`）、担当B（お絵描き関連：`DRAW_DATA`, `DRAW_RECEIVED`）、担当C（ゲーム進行関連：`GAME_START`, `GAME_ROUND_START`, `CHAT_SUBMIT`, `GAME_SCORE_UPDATE`）のコマンドを分類して定義している。新しい通信を追加する際はチームで合意の上、ここに追記するルールになっている。
