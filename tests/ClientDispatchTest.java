package tests;

import java.awt.Component;
import java.awt.Container;
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

            waitFor(() -> "残り  60秒".equals(label(gamePanel, "timerLabel").getText()), 2_000);
            SwingUtilities.invokeAndWait(() -> { });
            assertEquals("お題  dog", label(gamePanel, "themeLabel").getText(), "開始直後のゲームTheme");
            assertEquals("残り  60秒", label(gamePanel, "timerLabel").getText(), "開始直後のゲームTime");
            assertEquals("お題: dog", label(chatPanel, "themeLabel").getText(), "開始直後のチャットTheme");
            assertEquals("残り時間  60秒", label(chatPanel, "timerLabel").getText(), "開始直後のチャットTime");
            assertTrue(drawPanel.isDrawingEnabled(), "Drawerの描画権限");

            // 次ラウンドでGuesserへ切り替わった直後の表示も確認する。
            server.send("G_R_START:日本語部屋,2,2,ボブ,GUESSER,,60");
            waitFor(() -> "回答者".equals(label(gamePanel, "roleLabel").getText()), 2_000);
            SwingUtilities.invokeAndWait(() -> { });
            assertEquals("お題  秘密", label(gamePanel, "themeLabel").getText(),
                    "役割切替後のゲームTheme");
            assertEquals("残り  60秒", label(gamePanel, "timerLabel").getText(), "役割切替後のゲームTime");
            assertEquals("お題は正解するまで秘密です", label(chatPanel, "themeLabel").getText(),
                    "役割切替後のチャットTheme");
            assertEquals("残り時間  60秒", label(chatPanel, "timerLabel").getText(), "役割切替後のチャットTime");
            assertTrue(!drawPanel.isDrawingEnabled(), "Guesserの描画禁止");

            server.send("G_SCORE:アリス=500;ボブ=0");
            server.send("G_TIME:42");
            server.send("G_JUDGE:CORRECT,500");
            server.send("G_R_END:all_correct,dog");
            server.send("GAME_READY:0,2,");
            server.send("GAME_END:アリス=500;ボブ=0");

            waitFor(() -> "日本語部屋".equals(RoomController.getCurrentRoom()), 2_000);
            waitFor(() -> "アリス=500;ボブ=0".equals(finalScores.get()), 2_000);
            SwingUtilities.invokeAndWait(() -> { });

            assertEquals("日本語部屋", DrawController.getRoomId(), "描画ルームID");
            assertEquals(2, model(roomPanel, "memberListModel").getSize(), "メンバー数");
            assertEquals("準備完了: 0 / 2", label(roomPanel, "readyStatusLabel").getText(),
                    "ゲーム終了後の準備表示");
            assertEquals("残り  42秒", label(gamePanel, "timerLabel").getText(), "タイマー表示");
            assertTrue(area(gamePanel, "scoreArea").getText().contains("アリス  500点"), "スコア表示");
            assertEquals("正解！ +500", label(chatPanel, "resultLabel").getText(), "正解表示");

            SwingUtilities.invokeAndWait(() -> gamePanel.showRoundTransition("dog"));
            assertEquals("ラウンド終了", label(gamePanel, "roleLabel").getText(), "ラウンド終了表示");
            assertEquals("正解  dog", label(gamePanel, "themeLabel").getText(), "ラウンド正解表示");
            assertEquals("次のラウンドを準備中", label(gamePanel, "timerLabel").getText(),
                    "次ラウンド準備表示");

            // 実画面ではGamePanelが分割ペイン右側の狭い幅になる。
            // ラウンド情報更新後もTheme/Timeが折り返し先で切れないことを確認する。
            gamePanel.setSize(560, 700);
            layoutRecursively(gamePanel);
            assertFullyVisible(label(gamePanel, "themeLabel"), "ゲーム情報のTheme");
            assertFullyVisible(label(gamePanel, "timerLabel"), "ゲーム情報のTime");
            assertFullyVisible(label(chatPanel, "themeLabel"), "チャット情報のTheme");
            assertFullyVisible(label(chatPanel, "timerLabel"), "チャット情報のTime");
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

    private static void layoutRecursively(Container container) {
        container.doLayout();
        for (Component component : container.getComponents()) {
            if (component instanceof Container) {
                layoutRecursively((Container) component);
            }
        }
    }

    private static void assertFullyVisible(Component component, String label) {
        Container parent = component.getParent();
        boolean visible = component.getWidth() > 0
                && component.getHeight() > 0
                && component.getX() >= 0
                && component.getY() >= 0
                && component.getX() + component.getWidth() <= parent.getWidth()
                && component.getY() + component.getHeight() <= parent.getHeight();
        if (!visible) {
            throw new AssertionError(label + "が表示領域外です: component="
                    + component.getBounds() + ", parent=" + parent.getBounds());
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
