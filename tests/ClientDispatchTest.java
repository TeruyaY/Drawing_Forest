package tests;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;

import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import client.GameClient;
import client.draw.DrawController;
import client.draw.DrawPanel;
import client.game.ChatPanel;
import client.game.GameController;
import client.game.GamePanel;
import client.room.RoomController;
import client.room.RoomPanel;

/** GameClientの受信振り分けからSwing UI更新までを確認する統合テスト。 */
public final class ClientDispatchTest {
    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true");

        RoomPanel roomPanel = new RoomPanel();
        DrawPanel drawPanel = new DrawPanel();
        ChatPanel chatPanel = new ChatPanel();
        GamePanel gamePanel = new GamePanel(chatPanel);
        AtomicReference<String> finalScores = new AtomicReference<>();

        GameClient client = new GameClient();
        RoomController.init(client, roomPanel);
        DrawController.init(client, drawPanel);
        GameController.init(client, chatPanel, gamePanel);
        GameController.setGameEndListener(finalScores::set);

        try (ScriptServer server = new ScriptServer()) {
            client.connect("127.0.0.1", server.getPort());
            server.awaitClient();

            server.send("ROOM_JOINED_NOTIFY:日本語部屋,成功");
            server.send("ROOM_MEMBERS:日本語部屋,アリス|ボブ");
            server.send("GAME_READY:1,2,アリス");
            server.send("G_R_START:日本語部屋,1,2,アリス,DRAWER,dog,60");
            server.send("G_SCORE:アリス=500;ボブ=0");
            server.send("G_TIME:42");
            server.send("G_JUDGE:CORRECT,500");
            server.send("G_R_END:all_correct,dog");
            server.send("GAME_END:アリス=500;ボブ=0");

            waitFor(() -> "日本語部屋".equals(RoomController.getCurrentRoom()), 2_000);
            waitFor(() -> "アリス=500;ボブ=0".equals(finalScores.get()), 2_000);
            SwingUtilities.invokeAndWait(() -> { });

            assertEquals("日本語部屋", DrawController.getRoomId(), "描画ルームID");
            assertTrue(drawPanel.isDrawingEnabled(), "Drawerの描画権限");
            assertEquals(2, model(roomPanel, "memberListModel").getSize(), "メンバー数");
            assertEquals("準備完了: 1 / 2", label(roomPanel, "readyStatusLabel").getText(), "準備表示");
            assertEquals("Time: 42", label(gamePanel, "timerLabel").getText(), "タイマー表示");
            assertTrue(area(gamePanel, "scoreArea").getText().contains("アリス: 500"), "スコア表示");
            assertEquals("Correct! +500", label(chatPanel, "resultLabel").getText(), "正解表示");
        } finally {
            client.close();
        }

        System.out.println("ClientDispatchTest: PASS");
    }

    @SuppressWarnings("unchecked")
    private static DefaultListModel<Object> model(Object target, String name) {
        return (DefaultListModel<Object>) field(target, name);
    }

    private static JLabel label(Object target, String name) {
        return (JLabel) field(target, name);
    }

    private static JTextArea area(Object target, String name) {
        return (JTextArea) field(target, name);
    }

    private static Object field(Object target, String name) {
        try {
            Field field = target.getClass().getDeclaredField(name);
            field.setAccessible(true);
            return field.get(target);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("UIフィールドを確認できません: " + name, e);
        }
    }

    private static void waitFor(BooleanSupplier condition, int timeoutMillis) throws Exception {
        long deadline = System.currentTimeMillis() + timeoutMillis;
        while (System.currentTimeMillis() < deadline) {
            SwingUtilities.invokeAndWait(() -> { });
            if (condition.getAsBoolean()) {
                return;
            }
            Thread.sleep(20);
        }
        throw new AssertionError("UI更新がタイムアウトしました");
    }

    private static void assertEquals(Object expected, Object actual, String label) {
        if (!expected.equals(actual)) {
            throw new AssertionError(label + ": expected=" + expected + ", actual=" + actual);
        }
    }

    private static void assertTrue(boolean value, String label) {
        if (!value) {
            throw new AssertionError(label + "を確認できません");
        }
    }

    private static final class ScriptServer implements AutoCloseable {
        private final ServerSocket serverSocket;
        private final Thread acceptThread;
        private volatile Socket clientSocket;
        private volatile PrintWriter out;

        private ScriptServer() throws IOException {
            serverSocket = new ServerSocket(0);
            acceptThread = new Thread(() -> {
                try {
                    clientSocket = serverSocket.accept();
                    out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(
                            clientSocket.getOutputStream(), StandardCharsets.UTF_8)), true);
                } catch (IOException e) {
                    if (!serverSocket.isClosed()) {
                        throw new RuntimeException(e);
                    }
                }
            }, "client-dispatch-test-server");
            acceptThread.setDaemon(true);
            acceptThread.start();
        }

        private int getPort() {
            return serverSocket.getLocalPort();
        }

        private void awaitClient() throws InterruptedException {
            long deadline = System.currentTimeMillis() + 2_000;
            while (out == null && System.currentTimeMillis() < deadline) {
                Thread.sleep(10);
            }
            if (out == null) {
                throw new AssertionError("テスト用クライアントが接続されませんでした");
            }
        }

        private void send(String line) {
            out.println(line);
        }

        @Override
        public void close() throws IOException {
            if (clientSocket != null) {
                clientSocket.close();
            }
            serverSocket.close();
        }
    }
}
