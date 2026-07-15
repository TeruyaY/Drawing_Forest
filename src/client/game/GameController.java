package client.game;

import java.util.function.Consumer;

import javax.swing.SwingUtilities;
import javax.swing.Timer;

import client.GameClient;
import client.draw.DrawController;
import client.room.RoomController;
import common.Protocol;

public class GameController {
    private static GameClient client;
    private static ChatPanel chatPanel;
    private static GamePanel gamePanel;
    private static Consumer<String> gameEndListener;
    private static Runnable roundStartedListener;
    private static Timer pendingRoundTransition;
    private static Timer pendingGameEndTransition;
    private static long lastCorrectAtMillis;
    private static final int ROUND_END_HOLD_MILLIS = 1_150;
    private static final int FINAL_RESULT_DELAY_MILLIS = 1_500;

    public static void init(GameClient gameClient, ChatPanel panel) {
        init(gameClient, panel, null);
    }

    public static void init(GameClient gameClient, ChatPanel panel, GamePanel panelContainer) {
        cancelPendingRoundTransition();
        cancelPendingGameEndTransition();
        client = gameClient;
        chatPanel = panel;
        gamePanel = panelContainer;
    }

    // ゲーム終了(GAME_END)を受けたときに呼ばれる画面遷移用フック。最終スコア文字列を受け取る。
    public static void setGameEndListener(Consumer<String> listener) {
        gameEndListener = listener;
    }

    // ラウンド開始(GAME_ROUND_START)を受けたときに呼ばれる画面遷移用フック
    public static void setRoundStartedListener(Runnable listener) {
        roundStartedListener = listener;
    }

    public static void startGame() {
        if (client == null) {
            showResult("Not connected");
            return;
        }
        String room = RoomController.getCurrentRoom();
        if (room == null || room.isEmpty()) {
            showResult("Join a room first");
            return;
        }
        client.sendMessage(Protocol.GAME_START + ":" + room);
    }

    public static void submitChat(String text) {
        if (client == null) {
            showResult("Not connected");
            return;
        }
        String room = RoomController.getCurrentRoom();
        if (room == null || room.isEmpty()) {
            showResult("Join a room first");
            return;
        }
        client.sendMessage(Protocol.CHAT_SUBMIT + ":" + room + "," + sanitize(text));
    }

    public static void onGameUpdate(String command, String data) {
        SwingUtilities.invokeLater(() -> handleGameUpdate(command, data));
    }

    private static void handleGameUpdate(String command, String data) {
        switch (command) {
            case Protocol.GAME_ROUND_START:
                handleRoundStart(data);
                break;
            case Protocol.CHAT_BROADCAST:
                addChat(parseChat(data));
                break;
            case Protocol.GAME_JUDGE_RESULT:
                handleJudge(data);
                break;
            case Protocol.GAME_SCORE_UPDATE:
                if (gamePanel != null) {
                    gamePanel.setScores(data);
                }
                break;
            case Protocol.GAME_ROUND_END:
                handleRoundEnd(data);
                break;
            case Protocol.GAME_TIME_UPDATE:
                handleTime(data);
                break;
            case Protocol.GAME_END:
                handleGameEnd(data);
                break;
            default:
                System.out.println("[GameController] Unknown command: " + command + ":" + data);
        }
    }

    private static void handleRoundStart(String data) {
        cancelPendingRoundTransition();
        cancelPendingGameEndTransition();
        String[] parts = data == null ? new String[0] : data.split(",", -1);
        String round = parts.length > 1 ? parts[1] : "?";
        String total = parts.length > 2 ? parts[2] : "?";
        String drawer = parts.length > 3 ? parts[3] : "";
        String role = parts.length > 4 ? parts[4] : "";
        String theme = parts.length > 5 ? parts[5] : "";
        int seconds = parts.length > 6 ? parseInt(parts[6], 60) : 60;

        DrawController.setDrawingEnabled("DRAWER".equals(role));
        DrawController.clearForNewRound();

        if (chatPanel != null) {
            chatPanel.clearChat();
            chatPanel.setRoundInfo(role, drawer, theme);
            chatPanel.setTimeRemaining(seconds);
            chatPanel.addChatMessage("Round " + round + "/" + total + " started. Drawer: " + drawer);
        }
        if (gamePanel != null) {
            gamePanel.setRoundInfo("Round " + round + "/" + total, role, drawer, theme);
            gamePanel.setTimeRemaining(seconds);
        }

        // 表示内容をすべて反映してからゲーム画面へ切り替える。
        // 先にCardLayoutを切り替えると、初期値の短いラベル幅でレイアウトされ、
        // Theme/Timeが次のリサイズまで見切れることがある。
        if (roundStartedListener != null) {
            roundStartedListener.run();
        }
    }

    private static void handleJudge(String data) {
        String[] parts = data == null ? new String[0] : data.split(",", 2);
        String result = parts.length > 0 ? parts[0] : "";
        String value = parts.length > 1 ? parts[1] : "";

        if ("CORRECT".equals(result)) {
            lastCorrectAtMillis = System.currentTimeMillis();
            showResult("Correct! +" + value);
        } else if ("WRONG".equals(result)) {
            showResult("Wrong");
        } else if ("ALREADY_CORRECT".equals(result)) {
            showResult("Already correct");
        } else if ("ERROR".equals(result)) {
            showResult(value);
        }
    }

    private static void handleRoundEnd(String data) {
        String[] parts = data == null ? new String[0] : data.split(",", 2);
        String reason = parts.length > 0 ? parts[0] : "";
        String theme = parts.length > 1 ? parts[1] : "";
        addChat("Round ended (" + reason + "). Answer: " + theme);
        if (gamePanel != null) {
            cancelPendingRoundTransition();
            pendingRoundTransition = new Timer(ROUND_END_HOLD_MILLIS, event -> {
                pendingRoundTransition = null;
                gamePanel.showRoundTransition(theme);
            });
            pendingRoundTransition.setRepeats(false);
            pendingRoundTransition.start();
        }
    }

    private static void handleTime(String data) {
        int seconds = parseInt(data, 0);
        if (chatPanel != null) {
            chatPanel.setTimeRemaining(seconds);
        }
        if (gamePanel != null) {
            gamePanel.setTimeRemaining(seconds);
        }
    }

    private static void handleGameEnd(String data) {
        cancelPendingRoundTransition();
        addChat("Game finished. Final scores: " + data);
        long elapsed = System.currentTimeMillis() - lastCorrectAtMillis;
        boolean showingCorrectEffect = elapsed >= 0 && elapsed < FINAL_RESULT_DELAY_MILLIS;
        if (gamePanel != null) {
            gamePanel.setScoresWithoutEffect(data);
            if (!showingCorrectEffect) {
                gamePanel.showGameFinishing();
            }
        }

        cancelPendingGameEndTransition();
        pendingGameEndTransition = new Timer(FINAL_RESULT_DELAY_MILLIS, event -> {
            pendingGameEndTransition = null;
            showFinalResult(data);
        });
        pendingGameEndTransition.setRepeats(false);
        pendingGameEndTransition.start();
    }

    private static void cancelPendingRoundTransition() {
        if (pendingRoundTransition != null) {
            pendingRoundTransition.stop();
            pendingRoundTransition = null;
        }
    }

    private static void cancelPendingGameEndTransition() {
        if (pendingGameEndTransition != null) {
            pendingGameEndTransition.stop();
            pendingGameEndTransition = null;
        }
    }

    private static void showFinalResult(String scores) {
        if (gameEndListener != null) {
            gameEndListener.accept(scores);
        }
    }

    private static String parseChat(String data) {
        String[] parts = data == null ? new String[0] : data.split(",", 2);
        String name = parts.length > 0 ? parts[0] : "";
        String text = parts.length > 1 ? parts[1] : "";
        return name + ": " + text;
    }

    private static void addChat(String text) {
        if (chatPanel != null) {
            chatPanel.addChatMessage(text);
        } else {
            System.out.println("[Chat] " + text);
        }
    }

    private static void showResult(String text) {
        if (chatPanel != null) {
            chatPanel.showResult(text);
        } else {
            System.out.println("[Game] " + text);
        }
    }

    private static int parseInt(String text, int fallback) {
        try {
            return Integer.parseInt(text.trim());
        } catch (Exception e) {
            return fallback;
        }
    }

    private static String sanitize(String text) {
        return text == null ? "" : text.replace(':', ' ').replace(',', ' ').replace(';', ' ').replace('|', ' ').trim();
    }
}
