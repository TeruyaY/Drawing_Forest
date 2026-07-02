# DrawPanel.java

担当B（お絵描き）のキャンバスUI（`JPanel`）。マウスのドラッグ操作を検知して線分をローカルに描画すると同時に `DrawController.sendLine()` を呼んでサーバーへ送信する。描画内容は `BufferedImage` に焼き付けて保持するため、ウィンドウの再描画やリサイズでも消えない。サーバー経由で届いた他人の線は `drawRemoteLine()` で描画し、色の切り替え（`setColorName()`）やキャンバスの全消去（`clearCanvas()`、現状は自分の画面のみ）も提供する。
