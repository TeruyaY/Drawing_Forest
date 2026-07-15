package client;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import common.Protocol;
import client.room.RoomController;
import client.draw.DrawController;
import client.game.GameController;

public class GameClient {
    private static final int CONNECT_TIMEOUT_MILLIS = 5_000;

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    // サーバーに接続する
    public void connect(String serverAddress, int port) throws IOException {
        Socket newSocket = new Socket();
        try {
            newSocket.connect(new InetSocketAddress(serverAddress, port), CONNECT_TIMEOUT_MILLIS);
        } catch (IOException e) {
            try {
                newSocket.close();
            } catch (IOException ignored) {
                // 接続失敗時の後始末なので、元の例外を優先する
            }
            throw e;
        }
        socket = newSocket;
        out = new PrintWriter(
            new BufferedWriter(
                new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8)
            ),
            true
        );
        in = new BufferedReader(
            new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8)
        );

        // サーバーからのメッセージを常時待ち受ける
        startListening();
    }

    // A・B・CのControllerから呼び出し、サーバーへメッセージを送る
    public void sendMessage(String message) {
        if (out != null && socket != null && !socket.isClosed()) {
            out.println(message);
        }
    }

    public void close() {
        Socket current = socket;
        socket = null;
        out = null;
        in = null;
        if (current != null) {
            try {
                current.close();
            } catch (IOException ignored) {
                // 終了処理なので再送や復旧は不要
            }
        }
    }

    // サーバーからのメッセージをバックグラウンドで受信する
    private void startListening() {
        Thread listener = new Thread(() -> {
            try {
                String message;

                while ((message = in.readLine()) != null) {
                    // 「コマンド:データ」を最初のコロンだけで分割
                    String[] parts = message.split(":", 2);
                    String command = parts[0];
                    String data = parts.length > 1 ? parts[1] : "";

                    // コマンドごとに各Controllerへ振り分ける
                    switch (command) {
                        // A：ルーム関連
                        case Protocol.ROOM_CREATED_NOTIFY:
                        case Protocol.ROOM_JOINED_NOTIFY:
                        case Protocol.ROOM_LIST:
                        case Protocol.ROOM_MEMBERS:
                        case Protocol.ROOM_ERROR:
                        case Protocol.GAME_READY_UPDATE:
                            RoomController.onRoomMessage(command, data);
                            break;

                        // B：お絵描き関連
                        case Protocol.DRAW_RECEIVED:
                            DrawController.onDrawReceived(data);
                            break;
                        case Protocol.DRAW_CLEAR_RECEIVED:
                            DrawController.onClearReceived();
                            break;

                        // C：ゲーム進行・チャット関連
                        case Protocol.GAME_ROUND_START:
                        case Protocol.GAME_JUDGE_RESULT:
                        case Protocol.GAME_SCORE_UPDATE:
                        case Protocol.GAME_ROUND_END:
                        case Protocol.CHAT_BROADCAST:
                        case Protocol.GAME_TIME_UPDATE:
                        case Protocol.GAME_END:
                            GameController.onGameUpdate(command, data);
                            break;

                        default:
                            System.out.println("不明な受信: " + message);
                            break;
                    }
                }
            } catch (IOException e) {
                if (socket != null && !socket.isClosed()) {
                    System.out.println("サーバーから切断されました: " + e.getMessage());
                }
            }
        }, "drawing-forest-client-listener");
        listener.setDaemon(true);
        listener.start();
    }
}
