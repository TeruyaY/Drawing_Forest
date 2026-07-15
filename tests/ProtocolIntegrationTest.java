package tests;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import common.Protocol;
import server.ClientHandler;

/** 実ソケットで主要プロトコルを通す、外部ライブラリ不要の統合テスト。 */
public final class ProtocolIntegrationTest {
    private static final String ROOM = "日本語テスト部屋";

    public static void main(String[] args) throws Exception {
        try (EmbeddedServer server = new EmbeddedServer();
             TestClient alice = new TestClient(server.getPort());
             TestClient bob = new TestClient(server.getPort());
             TestClient outsider = new TestClient(server.getPort())) {

            outsider.send(Protocol.GAME_START + ":" + ROOM);
            outsider.expectPrefix(Protocol.GAME_JUDGE_RESULT + ":ERROR", 2_000);

            alice.send(Protocol.ROOM_CREATE + ":" + ROOM + ",アリス");
            alice.expectPrefix(Protocol.ROOM_CREATED_NOTIFY + ":" + ROOM, 2_000);
            alice.expectPrefix(Protocol.ROOM_JOINED_NOTIFY + ":" + ROOM, 2_000);

            bob.send(Protocol.ROOM_LIST_REQUEST + ":");
            assertContains(bob.expectPrefix(Protocol.ROOM_LIST + ":", 2_000), ROOM + ",1");
            bob.send(Protocol.ROOM_JOIN + ":" + ROOM + ",ボブ");
            bob.expectPrefix(Protocol.ROOM_JOINED_NOTIFY + ":" + ROOM, 2_000);
            assertContains(bob.expectPrefix(Protocol.ROOM_MEMBERS + ":" + ROOM, 2_000), "アリス|ボブ");
            alice.expectPrefixContaining(Protocol.ROOM_MEMBERS + ":" + ROOM, "アリス|ボブ", 2_000);

            // 未入室クライアントは描画・全消しを注入できない。
            outsider.send(Protocol.DRAW_DATA + ":" + ROOM + ",1,2,3,4,RED");
            alice.assertNoPrefix(Protocol.DRAW_RECEIVED + ":", 400);
            bob.assertNoPrefix(Protocol.DRAW_RECEIVED + ":", 400);
            outsider.send(Protocol.DRAW_CLEAR + ":" + ROOM);
            alice.assertNoPrefix(Protocol.DRAW_CLEAR_RECEIVED + ":", 400);

            alice.send(Protocol.DRAW_DATA + ":" + ROOM + ",10,20,30,40,BLUE");
            assertContains(bob.expectPrefix(Protocol.DRAW_RECEIVED + ":", 2_000), "10,20,30,40,BLUE");
            alice.send(Protocol.DRAW_CLEAR + ":" + ROOM);
            bob.expectPrefix(Protocol.DRAW_CLEAR_RECEIVED + ":", 2_000);

            alice.send(Protocol.GAME_START + ":" + ROOM);
            alice.expectPrefix(Protocol.GAME_READY_UPDATE + ":1,2", 2_000);
            bob.expectPrefix(Protocol.GAME_READY_UPDATE + ":1,2", 2_000);
            bob.send(Protocol.GAME_START + ":" + ROOM);
            alice.expectPrefix(Protocol.GAME_READY_UPDATE + ":2,2", 2_000);
            bob.expectPrefix(Protocol.GAME_READY_UPDATE + ":2,2", 2_000);

            Round first = readRound(alice, bob, 2_000);

            // ゲーム中の途中参加と、Guesserによる描画は拒否される。
            outsider.send(Protocol.ROOM_JOIN + ":" + ROOM + ",イブ");
            outsider.expectPrefix(Protocol.ROOM_ERROR + ":", 2_000);
            first.guesser.send(Protocol.DRAW_DATA + ":" + ROOM + ",5,6,7,8,GREEN");
            first.drawer.assertNoPrefix(Protocol.DRAW_RECEIVED + ":", 400);
            first.drawer.send(Protocol.DRAW_DATA + ":" + ROOM + ",11,12,13,14,BLACK");
            first.guesser.expectPrefix(Protocol.DRAW_RECEIVED + ":11,12,13,14,BLACK", 2_000);

            first.guesser.send(Protocol.CHAT_SUBMIT + ":" + ROOM + "," + first.theme);
            first.guesser.expectPrefix(Protocol.GAME_JUDGE_RESULT + ":CORRECT", 2_000);
            alice.expectPrefix(Protocol.GAME_ROUND_END + ":all_correct", 2_000);
            bob.expectPrefix(Protocol.GAME_ROUND_END + ":all_correct", 2_000);

            Round second = readRound(alice, bob, 6_000);
            second.guesser.send(Protocol.CHAT_SUBMIT + ":" + ROOM + "," + second.theme);
            second.guesser.expectPrefix(Protocol.GAME_JUDGE_RESULT + ":CORRECT", 2_000);
            alice.expectPrefix(Protocol.GAME_ROUND_END + ":all_correct", 2_000);
            bob.expectPrefix(Protocol.GAME_ROUND_END + ":all_correct", 2_000);
            alice.expectPrefix(Protocol.GAME_READY_UPDATE + ":0,2", 2_000);
            bob.expectPrefix(Protocol.GAME_READY_UPDATE + ":0,2", 2_000);
            alice.expectPrefix(Protocol.GAME_END + ":", 2_000);
            bob.expectPrefix(Protocol.GAME_END + ":", 2_000);

            alice.end();
            bob.end();
            Thread.sleep(250);
            outsider.drain(500);
            outsider.send(Protocol.ROOM_LIST_REQUEST + ":");
            String finalRooms = outsider.expectPrefix(Protocol.ROOM_LIST + ":", 2_000);
            if (finalRooms.contains(ROOM)) {
                throw new AssertionError("切断後もテスト部屋が残っています: " + finalRooms);
            }
        }

        verifyDrawerDisconnectEndsGameCleanly();

        System.out.println("ProtocolIntegrationTest: PASS");
    }

    private static void verifyDrawerDisconnectEndsGameCleanly() throws Exception {
        String room = "切断テスト部屋";
        try (EmbeddedServer server = new EmbeddedServer();
             TestClient alice = new TestClient(server.getPort());
             TestClient bob = new TestClient(server.getPort())) {
            alice.send(Protocol.ROOM_CREATE + ":" + room + ",Alice");
            alice.expectPrefix(Protocol.ROOM_JOINED_NOTIFY + ":" + room, 2_000);
            bob.send(Protocol.ROOM_JOIN + ":" + room + ",Bob");
            bob.expectPrefix(Protocol.ROOM_JOINED_NOTIFY + ":" + room, 2_000);

            alice.send(Protocol.GAME_START + ":" + room);
            alice.expectPrefix(Protocol.GAME_READY_UPDATE + ":1,2", 2_000);
            bob.expectPrefix(Protocol.GAME_READY_UPDATE + ":1,2", 2_000);
            bob.send(Protocol.GAME_START + ":" + room);
            alice.expectPrefix(Protocol.GAME_READY_UPDATE + ":2,2", 2_000);
            bob.expectPrefix(Protocol.GAME_READY_UPDATE + ":2,2", 2_000);
            Round round = readRound(alice, bob, 2_000);

            round.drawer.end();
            round.guesser.expectPrefix(Protocol.GAME_ROUND_END + ":player_left", 2_000);
            round.guesser.expectPrefix(Protocol.GAME_READY_UPDATE + ":0,1", 2_000);
            round.guesser.expectPrefix(Protocol.GAME_END + ":", 2_000);

            // 切断処理後も残ったクライアントのハンドラーが応答できることを確認する。
            round.guesser.send(Protocol.ROOM_LIST_REQUEST + ":");
            round.guesser.expectPrefix(Protocol.ROOM_LIST + ":", 2_000);
        }
    }

    private static Round readRound(TestClient alice, TestClient bob, int timeoutMillis) throws IOException {
        String aliceStart = alice.expectPrefix(Protocol.GAME_ROUND_START + ":", timeoutMillis);
        String bobStart = bob.expectPrefix(Protocol.GAME_ROUND_START + ":", timeoutMillis);
        String[] aliceParts = payload(aliceStart).split(",", -1);
        String[] bobParts = payload(bobStart).split(",", -1);
        if (aliceParts.length < 7 || bobParts.length < 7) {
            throw new AssertionError("ラウンド開始データが不正です: " + aliceStart + " / " + bobStart);
        }
        if ("DRAWER".equals(aliceParts[4])) {
            return new Round(alice, bob, aliceParts[5]);
        }
        if ("DRAWER".equals(bobParts[4])) {
            return new Round(bob, alice, bobParts[5]);
        }
        throw new AssertionError("Drawerが割り当てられていません");
    }

    private static String payload(String message) {
        int colon = message.indexOf(':');
        return colon < 0 ? "" : message.substring(colon + 1);
    }

    private static void assertContains(String actual, String expected) {
        if (!actual.contains(expected)) {
            throw new AssertionError("期待値がありません: " + expected + " / actual=" + actual);
        }
    }

    private static final class Round {
        private final TestClient drawer;
        private final TestClient guesser;
        private final String theme;

        private Round(TestClient drawer, TestClient guesser, String theme) {
            this.drawer = drawer;
            this.guesser = guesser;
            this.theme = theme;
        }
    }

    private static final class EmbeddedServer implements AutoCloseable {
        private final ServerSocket serverSocket;
        private final Thread acceptThread;

        private EmbeddedServer() throws IOException {
            serverSocket = new ServerSocket(0);
            acceptThread = new Thread(this::acceptLoop, "test-server-accept");
            acceptThread.setDaemon(true);
            acceptThread.start();
        }

        private int getPort() {
            return serverSocket.getLocalPort();
        }

        private void acceptLoop() {
            while (!serverSocket.isClosed()) {
                try {
                    ClientHandler handler = new ClientHandler(serverSocket.accept());
                    Thread thread = new Thread(handler, "test-client-handler");
                    thread.setDaemon(true);
                    thread.start();
                } catch (IOException e) {
                    if (!serverSocket.isClosed()) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }

        @Override
        public void close() throws IOException {
            serverSocket.close();
        }
    }

    private static final class TestClient implements AutoCloseable {
        private final Socket socket;
        private final BufferedReader in;
        private final PrintWriter out;
        private boolean ended;

        private TestClient(int port) throws IOException {
            socket = new Socket("127.0.0.1", port);
            socket.setSoTimeout(200);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            out = new PrintWriter(new BufferedWriter(
                    new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8)), true);
        }

        private void send(String message) {
            out.println(message);
        }

        private String expectPrefix(String prefix, int timeoutMillis) throws IOException {
            long deadline = System.currentTimeMillis() + timeoutMillis;
            List<String> seen = new ArrayList<>();
            while (System.currentTimeMillis() < deadline) {
                try {
                    String line = in.readLine();
                    if (line == null) {
                        throw new AssertionError("接続が切断されました。待機中: " + prefix);
                    }
                    seen.add(line);
                    if (line.startsWith(prefix)) {
                        return line;
                    }
                } catch (SocketTimeoutException ignored) {
                    // 期限まで再試行する
                }
            }
            throw new AssertionError("受信タイムアウト: " + prefix + " / seen=" + seen);
        }

        private String expectPrefixContaining(String prefix, String fragment, int timeoutMillis) throws IOException {
            long deadline = System.currentTimeMillis() + timeoutMillis;
            List<String> seen = new ArrayList<>();
            while (System.currentTimeMillis() < deadline) {
                try {
                    String line = in.readLine();
                    if (line == null) {
                        throw new AssertionError("接続が切断されました。待機中: " + prefix);
                    }
                    seen.add(line);
                    if (line.startsWith(prefix) && line.contains(fragment)) {
                        return line;
                    }
                } catch (SocketTimeoutException ignored) {
                    // 期限まで再試行する
                }
            }
            throw new AssertionError("受信タイムアウト: " + prefix + " + " + fragment + " / seen=" + seen);
        }

        private void assertNoPrefix(String prefix, int durationMillis) throws IOException {
            long deadline = System.currentTimeMillis() + durationMillis;
            while (System.currentTimeMillis() < deadline) {
                try {
                    String line = in.readLine();
                    if (line == null) {
                        return;
                    }
                    if (line.startsWith(prefix)) {
                        throw new AssertionError("受信してはいけないメッセージです: " + line);
                    }
                } catch (SocketTimeoutException ignored) {
                    // 検査時間が終わるまで継続する
                }
            }
        }

        private void drain(int durationMillis) throws IOException {
            long deadline = System.currentTimeMillis() + durationMillis;
            while (System.currentTimeMillis() < deadline) {
                try {
                    if (in.readLine() == null) {
                        return;
                    }
                } catch (SocketTimeoutException ignored) {
                    // 残っているメッセージを期限まで読み捨てる
                }
            }
        }

        private void end() {
            if (!ended) {
                ended = true;
                send("END");
            }
        }

        @Override
        public void close() throws IOException {
            end();
            socket.close();
        }
    }
}
