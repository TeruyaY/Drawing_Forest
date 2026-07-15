package server.game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import common.Protocol;
import server.ClientHandler;
import server.room.RoomManager;

public class GameManager {
    private static final int ROUND_TIME_SECONDS = 60;
    private static final int MAX_CHAT_LENGTH = 120;
    private static final Object LOCK = new Object();
    private static final Map<String, RoomGame> games = new HashMap<>();
    private static final Map<String, Set<String>> readySets = new HashMap<>();
    private static final Random RANDOM = new Random();
    private static final String[] THEMES = {
        "apple", "dog", "car", "tree", "house", "book", "soccer", "piano",
        "mountain", "coffee", "robot", "flower"
    };

    /** ルーム移動や途中入室を拒否するためにRoomManagerから参照する。 */
    public static boolean isGameActive(String roomName) {
        synchronized (LOCK) {
            return roomName != null && games.containsKey(roomName);
        }
    }

    /** 描画は待機中の正式メンバー、または進行中ラウンドのDrawerだけに許可する。 */
    public static boolean canDraw(ClientHandler client, String roomName) {
        synchronized (LOCK) {
            RoomGame game = games.get(roomName);
            if (game == null) {
                return true;
            }
            return game.roundActive && client.getUserName().equals(game.drawerName);
        }
    }

    /** 入退室後に準備済み人数を現在のメンバー構成へ同期する。 */
    public static void onRoomMembershipChanged(String roomName) {
        if (roomName == null || roomName.isEmpty()) {
            return;
        }
        synchronized (LOCK) {
            if (games.containsKey(roomName)) {
                return;
            }
            Set<String> ready = readySets.get(roomName);
            if (ready == null) {
                return;
            }
            Set<String> liveNames = liveMemberNames(roomName, null);
            ready.retainAll(liveNames);
            if (liveNames.isEmpty()) {
                readySets.remove(roomName);
                return;
            }
            broadcastReady(roomName, ready, liveNames.size());
        }
    }

    public static void handleGameStart(ClientHandler client, String data) {
        String roomName = clean(data);
        if (!isMemberOfRoom(client, roomName)) {
            client.sendMessage(Protocol.GAME_JUDGE_RESULT + ":ERROR,room membership required");
            return;
        }

        synchronized (LOCK) {
            if (games.containsKey(roomName)) {
                // 既にラウンドが進行中なら準備状態の変更は不要
                return;
            }

            List<ClientHandler> members = liveMembers(roomName);
            if (members.size() < 2) {
                client.sendMessage(Protocol.GAME_JUDGE_RESULT + ":ERROR,need at least 2 players");
                return;
            }

            Set<String> liveNames = new HashSet<>();
            for (ClientHandler member : members) {
                liveNames.add(member.getUserName());
            }

            Set<String> ready = readySets.computeIfAbsent(roomName, key -> new HashSet<>());
            ready.add(client.getUserName());
            ready.retainAll(liveNames);
            broadcastReady(roomName, ready, liveNames.size());

            if (!ready.containsAll(liveNames)) {
                // 部屋にいる全員が「ゲーム開始」を押すまで開始しない
                return;
            }

            readySets.remove(roomName);
            RoomGame game = new RoomGame(roomName, members);
            games.put(roomName, game);
            startRoundLocked(game);
        }
    }

    private static void broadcastReady(String roomName, Set<String> ready, int total) {
        List<String> names = new ArrayList<>(ready);
        Collections.sort(names);
        RoomManager.broadcastToRoom(roomName,
                Protocol.GAME_READY_UPDATE + ":" + ready.size() + "," + total + "," + String.join("|", names));
    }

    public static void handleChatSubmit(ClientHandler client, String data) {
        ChatRequest request = parseChatRequest(data);
        if (!isMemberOfRoom(client, request.roomName)) {
            client.sendMessage(Protocol.GAME_JUDGE_RESULT + ":ERROR,room membership required");
            return;
        }

        String message = limit(clean(request.message), MAX_CHAT_LENGTH);
        if (message.isEmpty()) {
            return;
        }

        synchronized (LOCK) {
            RoomGame game = games.get(request.roomName);
            if (game == null || !game.roundActive) {
                RoomManager.broadcastToRoom(request.roomName,
                        Protocol.CHAT_BROADCAST + ":" + client.getUserName() + "," + message);
                return;
            }

            refreshMembersLocked(game);
            if (!game.activePlayers.contains(client.getUserName())) {
                client.sendMessage(Protocol.GAME_JUDGE_RESULT + ":ERROR,not in active game");
                return;
            }

            if (client.getUserName().equals(game.drawerName)) {
                RoomManager.broadcastToRoom(request.roomName,
                        Protocol.CHAT_BROADCAST + ":" + client.getUserName() + "," + message);
                return;
            }

            if (game.correctUsers.contains(client.getUserName())) {
                client.sendMessage(Protocol.GAME_JUDGE_RESULT + ":ALREADY_CORRECT,0");
                return;
            }

            if (game.currentTheme.isCorrect(message)) {
                int points = calculatePoints(game);
                game.correctUsers.add(client.getUserName());
                game.correctCount++;
                game.addScore(client.getUserName(), points);
                game.roundPointsAwarded += points;
                boolean roundComplete = allGuessersCorrect(game);

                client.sendMessage(Protocol.GAME_JUDGE_RESULT + ":CORRECT," + points);
                broadcastScores(game);

                if (roundComplete) {
                    finishRoundAsync(request.roomName, game, "all_correct");
                }

                if (!roundComplete) {
                    RoomManager.broadcastToRoom(request.roomName,
                            Protocol.CHAT_BROADCAST + ":" + client.getUserName() + ",guessed correctly");
                }
            } else {
                client.sendMessage(Protocol.GAME_JUDGE_RESULT + ":WRONG,0");
                RoomManager.broadcastToRoom(request.roomName,
                        Protocol.CHAT_BROADCAST + ":" + client.getUserName() + "," + message);
            }
        }
    }

    public static void removeClient(ClientHandler client) {
        synchronized (LOCK) {
            String roomName = RoomManager.getRoomOf(client);
            String userName = client.getUserName();
            Set<String> ready = readySets.get(roomName);
            if (ready != null) {
                ready.remove(userName);
                Set<String> remainingNames = liveMemberNames(roomName, client);
                ready.retainAll(remainingNames);
                if (remainingNames.isEmpty()) {
                    readySets.remove(roomName);
                } else {
                    broadcastReady(roomName, ready, remainingNames.size());
                }
            }

            RoomGame game = games.get(roomName);
            if (game == null || !game.activePlayers.remove(userName)) {
                return;
            }

            game.correctUsers.remove(userName);
            game.correctCount = game.correctUsers.size();
            game.guesserCount = Math.max(0, game.activePlayers.size() - 1);

            if (game.activePlayers.size() < 2) {
                abortGameLocked(game, "player_left");
            } else if (userName.equals(game.drawerName)) {
                finishRoundLocked(game, "drawer_left");
            } else if (game.roundActive && allGuessersCorrect(game)) {
                finishRoundLocked(game, "all_correct");
            }
        }
    }

    private static void startRoundLocked(RoomGame game) {
        refreshMembersLocked(game);
        if (game.activePlayers.size() < 2 || game.drawerIndex >= game.drawingOrder.size()) {
            finishGameLocked(game);
            return;
        }

        String nextDrawer = findNextDrawer(game);
        if (nextDrawer == null) {
            finishGameLocked(game);
            return;
        }

        game.drawerName = nextDrawer;
        game.currentTheme = randomTheme();
        game.correctUsers.clear();
        game.correctCount = 0;
        game.roundPointsAwarded = 0;
        game.guesserCount = Math.max(0, game.activePlayers.size() - 1);
        game.roundStartMillis = System.currentTimeMillis();
        game.roundActive = true;

        game.cancelTimer();
        game.timer = new Timer(true);
        game.timer.schedule(new TimerTask() {
            @Override
            public void run() {
                onTimer(game.roomName);
            }
        }, 0, 1000);

        sendRoundStart(game);
        broadcastScores(game);
    }

    private static String findNextDrawer(RoomGame game) {
        while (game.drawerIndex < game.drawingOrder.size()) {
            String candidate = game.drawingOrder.get(game.drawerIndex);
            game.drawerIndex++;
            if (game.activePlayers.contains(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private static void onTimer(String roomName) {
        synchronized (LOCK) {
            RoomGame game = games.get(roomName);
            if (game == null || !game.roundActive) {
                return;
            }

            refreshMembersLocked(game);
            int remaining = remainingSeconds(game);
            RoomManager.broadcastToRoom(roomName, Protocol.GAME_TIME_UPDATE + ":" + remaining);

            if (remaining <= 0) {
                finishRoundLocked(game, "time_up");
            }
        }
    }

    private static void finishRoundLocked(RoomGame game, String reason) {
        if (!game.roundActive) {
            return;
        }

        game.roundActive = false;
        // 描いた人にも、そのお題で回答者たちが稼いだ得点の平均を加点する
        awardDrawerBonusLocked(game);
        RoomManager.broadcastToRoom(game.roomName,
                Protocol.GAME_ROUND_END + ":" + reason + "," + game.currentTheme.getDisplayName());
        broadcastScores(game);

        if (game.drawerIndex >= game.drawingOrder.size()) {
            finishGameLocked(game);
            game.cancelTimer();
            return;
        }

        game.cancelTimer();
        Timer nextRoundTimer = new Timer(true);
        nextRoundTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                synchronized (LOCK) {
                    RoomGame current = games.get(game.roomName);
                    if (current == game && !current.roundActive) {
                        startRoundLocked(current);
                    }
                }
            }
        }, 3000);
    }

    /** 描いた人に、その回で回答者たちが稼いだ得点の平均を加点する。 */
    private static void awardDrawerBonusLocked(RoomGame game) {
        if (game.drawerName == null || game.correctUsers.isEmpty()) {
            return;
        }

        int bonus = game.roundPointsAwarded / game.correctUsers.size();
        if (bonus > 0) {
            game.addScore(game.drawerName, bonus);
        }
    }

    private static void finishRoundAsync(String roomName, RoomGame expectedGame, String reason) {
        Thread thread = new Thread(() -> {
            synchronized (LOCK) {
                RoomGame current = games.get(roomName);
                if (current == expectedGame) {
                    finishRoundLocked(current, reason);
                }
            }
        }, "game-round-finish-" + roomName);
        thread.setDaemon(true);
        thread.start();
    }

    private static void finishGameLocked(RoomGame game) {
        game.cancelTimer();
        games.remove(game.roomName);
        RoomManager.broadcastToRoom(game.roomName, Protocol.GAME_END + ":" + game.buildScoreText());
    }

    private static void abortGameLocked(RoomGame game, String reason) {
        game.cancelTimer();
        if (game.roundActive && game.currentTheme != null) {
            game.roundActive = false;
            RoomManager.broadcastToRoom(game.roomName,
                    Protocol.GAME_ROUND_END + ":" + reason + "," + game.currentTheme.getDisplayName());
            broadcastScores(game);
        }
        games.remove(game.roomName);
        RoomManager.broadcastToRoom(game.roomName, Protocol.GAME_END + ":" + game.buildScoreText());
    }

    private static void sendRoundStart(RoomGame game) {
        int totalRounds = game.drawingOrder.size();
        int roundNumber = Math.min(game.drawerIndex, totalRounds);

        for (ClientHandler member : RoomManager.getRoomMembers(game.roomName)) {
            String role = member.getUserName().equals(game.drawerName) ? "DRAWER" : "GUESSER";
            String theme = "DRAWER".equals(role) ? game.currentTheme.getDisplayName() : "";
            member.sendMessage(Protocol.GAME_ROUND_START + ":"
                    + game.roomName + "," + roundNumber + "," + totalRounds + ","
                    + game.drawerName + "," + role + "," + theme + "," + ROUND_TIME_SECONDS);
        }
    }

    private static void broadcastScores(RoomGame game) {
        RoomManager.broadcastToRoom(game.roomName,
                Protocol.GAME_SCORE_UPDATE + ":" + game.buildScoreText());
    }

    private static boolean allGuessersCorrect(RoomGame game) {
        return game.correctCount >= game.guesserCount;
    }

    private static int calculatePoints(RoomGame game) {
        int elapsed = (int) ((System.currentTimeMillis() - game.roundStartMillis) / 1000L);
        int remaining = Math.max(0, ROUND_TIME_SECONDS - elapsed);
        return 100 + remaining * 10;
    }

    private static int remainingSeconds(RoomGame game) {
        int elapsed = (int) ((System.currentTimeMillis() - game.roundStartMillis) / 1000L);
        return Math.max(0, ROUND_TIME_SECONDS - elapsed);
    }

    private static Theme randomTheme() {
        String word = THEMES[RANDOM.nextInt(THEMES.length)];
        List<String> accepted = new ArrayList<>();
        accepted.add(word);
        return new Theme(word, accepted);
    }

    private static void refreshMembersLocked(RoomGame game) {
        Set<String> liveNames = new HashSet<>();
        for (ClientHandler member : RoomManager.getRoomMembers(game.roomName)) {
            liveNames.add(member.getUserName());
            game.scores.putIfAbsent(member.getUserName(), 0);
        }
        game.activePlayers.retainAll(liveNames);
    }

    private static List<ClientHandler> liveMembers(String roomName) {
        List<ClientHandler> members = RoomManager.getRoomMembers(roomName);
        return members == null ? Collections.emptyList() : members;
    }

    private static Set<String> liveMemberNames(String roomName, ClientHandler excluded) {
        Set<String> names = new HashSet<>();
        for (ClientHandler member : RoomManager.getRoomMembers(roomName)) {
            if (member != excluded) {
                names.add(member.getUserName());
            }
        }
        return names;
    }

    private static boolean isMemberOfRoom(ClientHandler client, String roomName) {
        return roomName != null && !roomName.isEmpty() && roomName.equals(RoomManager.getRoomOf(client));
    }

    private static ChatRequest parseChatRequest(String data) {
        String[] parts = data == null ? new String[0] : data.split(",", 2);
        String roomName = parts.length > 0 ? clean(parts[0]) : "";
        String message = parts.length > 1 ? parts[1] : "";
        return new ChatRequest(roomName, message);
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private static String limit(String value, int maxLength) {
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private static class ChatRequest {
        private final String roomName;
        private final String message;

        private ChatRequest(String roomName, String message) {
            this.roomName = roomName;
            this.message = message;
        }
    }

    private static class RoomGame {
        private final String roomName;
        private final List<String> drawingOrder = new ArrayList<>();
        private final Set<String> activePlayers = new HashSet<>();
        private final Set<String> correctUsers = new HashSet<>();
        private final Map<String, Integer> scores = new HashMap<>();
        private int drawerIndex;
        private String drawerName;
        private Theme currentTheme;
        private long roundStartMillis;
        private boolean roundActive;
        private int guesserCount;
        private int correctCount;
        private int roundPointsAwarded;
        private Timer timer;

        private RoomGame(String roomName, List<ClientHandler> members) {
            this.roomName = roomName;
            for (ClientHandler member : members) {
                drawingOrder.add(member.getUserName());
                activePlayers.add(member.getUserName());
                scores.put(member.getUserName(), 0);
            }
            Collections.shuffle(drawingOrder, RANDOM);
        }

        private void addScore(String userName, int points) {
            scores.put(userName, scores.getOrDefault(userName, 0) + points);
        }

        private String buildScoreText() {
            List<String> names = new ArrayList<>(scores.keySet());
            Collections.sort(names);
            List<String> entries = new ArrayList<>();
            for (String name : names) {
                entries.add(name + "=" + scores.getOrDefault(name, 0));
            }
            return String.join(";", entries);
        }

        private void cancelTimer() {
            if (timer != null) {
                timer.cancel();
                timer = null;
            }
        }
    }
}
