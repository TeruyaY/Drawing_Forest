package client.game;

/**
 * 担当C（ゲーム進行・チャット）のクライアント側コントローラ。
 *
 * GameClient.java がこのクラスへ GAME_ROUND_START / GAME_SCORE_UPDATE を振り分ける実装に
 * なっているため、このファイルが存在しないとプロジェクト全体がコンパイルできない。
 * 担当Cの実装が始まるまでのビルド用最小スタブとして、受信内容をログ出力するだけにしてある。
 * 本実装（お題・役割の表示、スコア更新など）は担当Cが置き換える想定。
 */
public class GameController {
    public static void onGameUpdate(String command, String data) {
        System.out.println("[GameController] 未実装のゲーム通知: " + command + ":" + data);
    }
}
