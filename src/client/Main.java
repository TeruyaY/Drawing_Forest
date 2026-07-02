package client;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import client.draw.DrawController;
import client.draw.DrawPanel;
import client.room.RoomController;
import client.room.RoomPanel;

/**
 * ロビー画面（担当A）とお絵描き画面（担当B）を1つのウィンドウ・1本の GameClient で繋ぐ統合エントリポイント。
 *
 * 本来は「ゲーム開始」→サーバーが GAME_ROUND_START を送信→GameController（担当C）が
 * 画面遷移を行う、という流れが正しい形。しかし担当Cの実装（GameManager.handleGameStart など）が
 * まだ無いため、ここでは「ゲーム開始」ボタンが押された時点でローカルに画面を切り替える
 * 暫定トリガー（RoomController.setOnGameStartRequested）で代用している。
 * 担当Cの実装が入ったら、この暫定トリガーは GAME_ROUND_START 受信時の遷移に置き換える想定。
 */
public class Main {
    private static final String CARD_LOBBY = "LOBBY";
    private static final String CARD_GAME = "GAME";

    public static void main(String[] args) {
        String host = args.length > 0 ? args[0] : "localhost";
        int port = args.length > 1 ? parsePort(args[1]) : 8080;

        GameClient client = new GameClient();
        String initialStatus = "サーバーに接続しました: " + host + ":" + port;
        try {
            client.connect(host, port);
        } catch (Exception e) {
            client = null;
            initialStatus = "サーバーに接続できませんでした: " + e.getMessage();
        }

        GameClient connectedClient = client;
        String status = initialStatus;
        SwingUtilities.invokeLater(() -> buildAndShow(connectedClient, status));
    }

    private static void buildAndShow(GameClient client, String initialStatus) {
        RoomPanel roomPanel = new RoomPanel();
        RoomController.init(client, roomPanel);

        DrawPanel drawPanel = new DrawPanel();
        DrawController.init(client, drawPanel);

        CardLayout cardLayout = new CardLayout();
        JPanel cards = new JPanel(cardLayout);
        cards.add(roomPanel, CARD_LOBBY);
        cards.add(buildGamePanel(drawPanel), CARD_GAME);

        // 暫定トリガー：担当Cの実装が済むまでは、ボタン押下＝ゲーム画面表示として扱う。
        RoomController.setOnGameStartRequested(() -> cardLayout.show(cards, CARD_GAME));

        JFrame frame = new JFrame("お絵描きの森");
        frame.setLayout(new BorderLayout());
        frame.add(cards, BorderLayout.CENTER);
        frame.setSize(720, 540);
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);

        roomPanel.showStatus(initialStatus);
        if (client != null) {
            RoomController.requestRoomList();
        }
    }

    private static JPanel buildGamePanel(DrawPanel drawPanel) {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        addColorButton(toolBar, drawPanel, "黒", "BLACK");
        addColorButton(toolBar, drawPanel, "赤", "RED");
        addColorButton(toolBar, drawPanel, "青", "BLUE");
        addColorButton(toolBar, drawPanel, "緑", "GREEN");
        addColorButton(toolBar, drawPanel, "消しゴム", "WHITE");
        JButton clearButton = new JButton("全消し");
        clearButton.addActionListener(e -> drawPanel.clearCanvas());
        toolBar.add(clearButton);

        // お題・チャット・スコア表示は担当Cの実装待ちのプレースホルダー。
        JLabel placeholder = new JLabel(
                "チャット・お題・スコア機能は担当C実装待ちです（このエリアはプレースホルダー）",
                SwingConstants.CENTER);
        placeholder.setPreferredSize(new Dimension(0, 40));

        panel.add(toolBar, BorderLayout.NORTH);
        panel.add(drawPanel, BorderLayout.CENTER);
        panel.add(placeholder, BorderLayout.SOUTH);
        return panel;
    }

    private static void addColorButton(JToolBar bar, DrawPanel panel, String label, String colorName) {
        JButton button = new JButton(label);
        button.addActionListener(e -> panel.setColorName(colorName));
        bar.add(button);
    }

    private static int parsePort(String s) {
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            System.out.println("ポート番号が不正なので8080を使います: " + s);
            return 8080;
        }
    }
}
