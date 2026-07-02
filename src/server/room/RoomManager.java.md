# RoomManager.java

担当A（ルーム管理）のサーバー側処理。部屋の作成（`handleCreateRoom()`）・参加（`handleJoinRoom()`）・一覧要求（`handleRoomListRequest()`）を処理し、部屋名・ユーザー名のバリデーション（禁則文字・文字数制限）や重複部屋名のチェックを行う。部屋一覧・メンバー一覧はサーバーのメモリ上（`ConcurrentHashMap`など）で管理し、変更があるたびに `ROOM_LIST`・`ROOM_MEMBERS` を関係クライアントへブロードキャストする。クライアントの入退室時には担当B（`DrawManager`）の部屋登録も連動して更新する。クライアント切断時のクリーンアップ（`removeClient()`）も担う。
