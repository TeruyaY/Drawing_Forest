# 動作確認手順

このドキュメントは、ローカル環境で「お絵描きの森」をビルドして動作確認するための手順です。

## 前提条件

- JDK 21 以上（`javac -version` / `java -version` で確認）
- ビルドツール（Maven/Gradle等）は未導入。`javac`/`java` を直接使う。

## 1. コンパイル

プロジェクトルート（このファイルがある階層）で実行します。

```bash
javac -d out -encoding UTF-8 $(find src -name "*.java")
```

- `-d out`: コンパイル後の `.class` をパッケージ構造のまま `out/` に出力します。
- `-encoding UTF-8`: 日本語文字列の文字化けを防ぎます。
- `out/` は `.gitignore` 対象のビルド生成物なのでコミットしないでください。

## 2. サーバー起動

ターミナルを1つ開いて実行します。

```bash
java -cp out server.GameServer
```

`サーバーが起動しました。ポート: 8080` と表示されれば成功です（ポートは `src/server/GameServer.java` で `8080` に固定）。

## 3. クライアント起動

サーバーを起動したまま、別のターミナルを人数分（最低2つ、動作確認なら2〜3つ推奨）開いて実行します。

```bash
java -cp out client.room.RoomLauncher
```

引数なしの場合は `localhost:8080` に自動接続します。別マシンのサーバーに接続する場合は以下のように引数を指定します。

```bash
java -cp out client.room.RoomLauncher <サーバーのIPアドレス> <ポート番号>
```

## 4. 確認する流れ

1. 各クライアントで部屋の作成・参加ができるか
2. 同じ部屋に参加したメンバーが待機画面（ロビー）に一覧表示されるか
3. ロビー画面の「ゲーム開始」ボタンは各自の準備完了を送るボタン。1人だけ押しても始まらず、「準備完了: n / m」が更新されることを確認する
4. 部屋にいる**全員**が「ゲーム開始」を押した瞬間にラウンドが始まり、そのタイミングで全員のお絵描き画面へ切り替わることを確認する（誰かが押しただけでは画面遷移しない）
5. その回のDrawer（描く人）だけがキャンバスに線を引けること。Guesser（当てる人）側はキャンバス操作ができず、うっすら網掛け表示になっていることを確認する
6. お題（Theme）はDrawerの画面にだけ表示され、Guesser側は「(guessers can't see this)」と表示されること
7. キャンバスに描いた線が他の全員の画面にリアルタイムで反映されるか
8. チャットに正解のお題を送信すると正誤判定・スコア反映がされるか
9. 制限時間経過やラウンド終了で画面遷移が正しく行われるか（次ラウンドでは再度全員の「ゲーム開始」待ちに戻る）

## 5. 複数端末（別マシン）で遊ぶ場合

同じPC内で複数ターミナルを開いてクライアントを起動するだけでなく、**サーバーを1台のPCで動かし、他のPC（別端末）からクライアントで接続する**こともできます。

### 5.1 サーバー役のPCで確認すること

1. 上記の手順でサーバーを起動する（`java -cp out server.GameServer`）。
2. そのPCのローカルIPアドレスを確認する。

   ```bash
   # macOS / Linux
   ifconfig | grep "inet "
   # または
   ip addr show

   # Windows (コマンドプロンプト)
   ipconfig
   ```

   `192.168.x.x` や `10.x.x.x` のような、同じWi-Fi/LANに属するアドレスを控えます（`127.0.0.1`はこのPC自身を指すだけなので使えません）。

3. ポート`8080`への接続がファイアウォールでブロックされていないか確認する。特にWindowsでは初回起動時に「Windows Defenderファイアウォールでブロックされました」という確認ダイアログが出ることがあるので、「アクセスを許可する」を選択する（プライベートネットワークのみで十分）。

### 5.2 クライアント役のPCで接続する

サーバー役のPCと**同じWi-Fi/LAN**に接続した状態で、サーバーのIPアドレスを指定して起動します。

```bash
java -cp out client.room.RoomLauncher 192.168.x.x 8080
```

`192.168.x.x`はサーバー役PCで確認したIPアドレスに置き換えてください。ポート番号を省略すると`8080`が使われます。

### 5.3 うまく繋がらないときの確認ポイント

- サーバー役・クライアント役の両方のPCが**同じネットワーク**（同じルーター配下）にいるか。異なるWi-Fi、モバイル回線、VPN経由などでは繋がらないことが多い。
- サーバー役PCのファイアウォール／セキュリティソフトがポート8080の受信を許可しているか。
- IPアドレスを打ち間違えていないか（サーバー役PCのネットワーク設定が変わるとIPアドレスも変わることがあるため、繋がらなくなったら再確認する）。
- 各PCで事前にこのプロジェクトを`javac`でビルド済みか（`out/`フォルダはコピーではなく、各PCでソースからビルドすることを推奨。JDKのバージョンが揃っていれば問題は起きにくい）。

### 5.4 WSL2でサーバーを動かす場合

WSL2の`hostname -I`で表示されるアドレスは仮想マシン内部のIPです。別PCから接続する場合は、管理者権限のWindows PowerShellで8080番をWSLへ転送し、クライアントには`ipconfig`で確認したWindowsのWi-Fi/LAN側IPv4アドレスを指定します。

```powershell
# WSL側IPを確認（例: 172.31.32.229）
wsl hostname -I

# 値は上の結果に置き換える
netsh interface portproxy add v4tov4 listenaddress=0.0.0.0 listenport=8080 connectaddress=172.31.32.229 connectport=8080

# 同一LAN内からの接続だけを許可
New-NetFirewallRule -DisplayName "Drawing Forest TCP 8080" -Direction Inbound -Action Allow -Protocol TCP -LocalPort 8080 -Profile Any -RemoteAddress LocalSubnet

# クライアントに指定するWindows側IPを確認
ipconfig
```

WSLを再起動するとWSL側IPが変わる場合があります。その場合は古い転送を削除して、新しいIPで追加し直します。

```powershell
netsh interface portproxy delete v4tov4 listenaddress=0.0.0.0 listenport=8080
```

## 6. 自動統合テスト

外部ライブラリを使わず、実際のTCPソケットで部屋作成、複数人参加、描画同期、権限制御、ゲーム進行、得点、切断処理まで確認できます。

```bash
./scripts/test.sh
```

`ProtocolIntegrationTest: PASS`と`ClientDispatchTest: PASS`が表示されれば成功です。

## 補足

- `client.draw.DrawLauncher` は描画機能単体を試すための簡易起動クラスです。お絵描き部分だけ確認したい場合はこちらも利用できます。

  ```bash
  java -cp out client.draw.DrawLauncher
  ```

- 通信コマンドの一覧・仕様は `common/Protocol.java` および `README.md` を参照してください。
