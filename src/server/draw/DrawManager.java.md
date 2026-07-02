# DrawManager.java

担当B（お絵描き）のサーバー側処理。`handleDrawData()` で `ClientHandler` から「部屋ID,X1,Y1,X2,Y2,色」形式の描画データを受け取り、同じ部屋にいる自分以外の全メンバーへ `DRAW_RECEIVED` コマンドとして転送（ブロードキャスト）する。部屋ごとのメンバー集合を `ConcurrentHashMap`/`CopyOnWriteArraySet` でスレッドセーフに管理し、`joinRoom()`/`leaveRoom()` を担当A（`RoomManager`）向けの連携APIとして公開している。Aとの連携が未実装でも単体テストできるよう、描画データを送った本人を自動的にその部屋へ登録するフォールバック処理も持つ。
