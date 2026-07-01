package server.game;

import common.Protocol;
import server.ClientHandler;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class GameManager {

    // 部屋IDごとに「現在のお題」を保存する
    private static final Map<String, String> currentThemes = new HashMap<>();

    // 部屋IDごとに「すでに正解したユーザー」を保存する
    private static final Map<String, Set<String>> correctUsersByRoom = new HashMap<>();

    // 部屋IDごとに「ユーザーごとのスコア」を保存する
    private static final Map<String, Map<String, Integer>> scoresByRoom = new HashMap<>();

    // 出題するお題リスト
    private static final List<String> themes = Arrays.asList(
            "りんご",
            "ねこ",
            "いぬ",
            "くるま",
            "さかな",
            "バナナ",
            "時計",
            "学校",
            "飛行機",
            "パソコン"
    );

    // ランダムにお題を選ぶための部品
    private static final Random random = new Random();

    public static void handleGameStart(ClientHandler client, String data) {
        // dataには部屋IDが入っている想定
        // 例: GAME_START:room1 の場合、data = "room1"
        String roomId = data.trim();

        if (roomId.isEmpty()) {
            client.sendMessage(Protocol.ROOM_ERROR + ":部屋IDが空です");
            return;
        }

        // お題リストからランダムに1つ選ぶ
        String theme = themes.get(random.nextInt(themes.size()));

        // この部屋の現在のお題として保存する
        currentThemes.put(roomId, theme);

        // この部屋の正解済みユーザーを初期化する
        correctUsersByRoom.put(roomId, new HashSet<>());

        // この部屋のスコア表を用意する
        scoresByRoom.putIfAbsent(roomId, new HashMap<>());

        // 仮実装では、ゲーム開始した本人を「描く人」として扱う
        // 本来はRoomManagerからメンバー一覧を取得して、描く人をランダムに選ぶ
        client.sendMessage(Protocol.GAME_ROUND_START + ":DRAWER," + theme);
    }

    public static void handleChatSubmit(ClientHandler client, String data) {
        // dataには「部屋ID,発言内容」が入っている想定
        // 例: CHAT_SUBMIT:room1,りんご の場合、data = "room1,りんご"
        String[] parts = data.split(",", 2);

        if (parts.length < 2) {
            client.sendMessage(Protocol.ROOM_ERROR + ":回答形式が不正です");
            return;
        }

        String roomId = parts[0].trim();
        String answer = parts[1].trim();

        if (roomId.isEmpty()) {
            client.sendMessage(Protocol.ROOM_ERROR + ":部屋IDが空です");
            return;
        }

        if (answer.isEmpty()) {
            client.sendMessage(Protocol.ROOM_ERROR + ":回答が空です");
            return;
        }

        // 現在のお題を取得する
        String currentTheme = currentThemes.get(roomId);

        if (currentTheme == null) {
            client.sendMessage(Protocol.ROOM_ERROR + ":ゲームが開始されていません");
            return;
        }

        // 回答者名を取得する
        String userName = client.getUserName();

        // この部屋の正解済みユーザー一覧を取得する
        Set<String> correctUsers = correctUsersByRoom.get(roomId);

        if (correctUsers == null) {
            correctUsers = new HashSet<>();
            correctUsersByRoom.put(roomId, correctUsers);
        }

        // すでに正解した人には二重加点しない
        if (correctUsers.contains(userName)) {
            client.sendMessage(Protocol.GAME_JUDGE_RESULT + ":" + userName + ",ALREADY_CORRECT," + answer);
            return;
        }

        // 回答とお題を比較する
        boolean isCorrect = normalize(answer).equals(normalize(currentTheme));

        if (isCorrect) {
            // 正解済みユーザーとして登録
            correctUsers.add(userName);

            // スコア表を取得
            Map<String, Integer> roomScores = scoresByRoom.get(roomId);

            if (roomScores == null) {
                roomScores = new HashMap<>();
                scoresByRoom.put(roomId, roomScores);
            }

            // 正解した人に100点加算
            int currentScore = roomScores.getOrDefault(userName, 0);
            roomScores.put(userName, currentScore + 100);

            // 正解通知
            client.sendMessage(Protocol.GAME_JUDGE_RESULT + ":" + userName + ",CORRECT," + answer);

            // スコア更新通知
            client.sendMessage(Protocol.GAME_SCORE_UPDATE + ":" + buildScoreText(roomScores));

        } else {
            // 不正解通知
            client.sendMessage(Protocol.GAME_JUDGE_RESULT + ":" + userName + ",WRONG," + answer);
        }
    }

    private static String normalize(String text) {
        if (text == null) {
            return "";
        }

        return text.trim()
                .replace(" ", "")
                .replace("　", "")
                .toLowerCase();
    }

    private static String buildScoreText(Map<String, Integer> roomScores) {
        StringBuilder sb = new StringBuilder();

        for (Map.Entry<String, Integer> entry : roomScores.entrySet()) {
            if (sb.length() > 0) {
                sb.append(";");
            }

            sb.append(entry.getKey())
                    .append("=")
                    .append(entry.getValue());
        }

        return sb.toString();
    }
}
