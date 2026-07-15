package client.room;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import client.GameClient;
import client.UiTheme;
import client.draw.DrawController;
import client.draw.DrawLauncher;
import client.draw.DrawPanel;
import client.game.ChatPanel;
import client.game.GameController;
import client.game.GamePanel;
import client.game.ResultPanel;

public class RoomLauncher {
    private static final String CARD_ROOM = "ROOM";
    private static final String CARD_PLAY = "PLAY";
    private static final String CARD_RESULT = "RESULT";
    // チャット欄が狭くなりすぎないよう、描画側にウェイトを寄せすぎない比率にしている
    private static final double PLAY_SPLIT_RATIO = 0.66;

    public static void main(String[] args) {
        UiTheme.installGlobalDefaults();

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
        DrawPanel drawPanel = new DrawPanel();
        ChatPanel chatPanel = new ChatPanel();
        GamePanel gamePanel = new GamePanel(chatPanel);
        ResultPanel resultPanel = new ResultPanel();
        JLabel currentRoomLabel = new JLabel();

        RoomController.init(client, roomPanel);
        DrawController.init(client, drawPanel);
        GameController.init(client, chatPanel, gamePanel);

        JSplitPane playSplitPane = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT, buildDrawPanel(drawPanel), gamePanel);
        playSplitPane.setResizeWeight(PLAY_SPLIT_RATIO);

        CardLayout cardLayout = new CardLayout();
        JPanel cards = new JPanel(cardLayout);
        cards.add(roomPanel, CARD_ROOM);
        cards.add(buildPlayPanel(playSplitPane, currentRoomLabel), CARD_PLAY);
        cards.add(resultPanel, CARD_RESULT);

        // ルーム参加(作成含む)が成功したらラベルだけ更新し、部屋画面のまま待機。
        // 全員が「ゲーム開始」を押してラウンドが始まったら描画+チャット画面へ、
        // ゲームが終了したら結果画面を表示し、「終了」ボタンでルーム画面へ戻る
        RoomController.setRoomJoinedListener(() -> {
            currentRoomLabel.setText("部屋: " + RoomController.getCurrentRoom());
        });
        GameController.setRoundStartedListener(() -> {
            cardLayout.show(cards, CARD_PLAY);
            // setDividerLocationはその瞬間の実サイズを元に計算されるため、
            // ウィンドウがまだ表示されていない構築時点で呼んでも意味がない。
            // フレーム表示後、画面を切り替えるたびに呼び直して確実に反映させる。
            playSplitPane.setDividerLocation(PLAY_SPLIT_RATIO);
        });
        GameController.setGameEndListener(finalScores -> {
            resultPanel.setScores(finalScores);
            cardLayout.show(cards, CARD_RESULT);
        });
        resultPanel.setOnClose(() -> {
            cardLayout.show(cards, CARD_ROOM);
            RoomController.requestRoomList();
        });

        JFrame frame = new JFrame("お絵描きの森");
        frame.setLayout(new BorderLayout());
        frame.add(buildTitleBar(), BorderLayout.NORTH);
        frame.add(cards, BorderLayout.CENTER);
        frame.getContentPane().setBackground(UiTheme.APP_BACKGROUND);
        frame.setSize(1400, 900);
        frame.setMinimumSize(new Dimension(1100, 700));
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);

        cardLayout.show(cards, CARD_ROOM);
        roomPanel.showStatus(initialStatus);
        if (client != null) {
            RoomController.requestRoomList();
        }
    }

    private static JPanel buildTitleBar() {
        JLabel title = new JLabel("お絵描きの森", SwingConstants.LEFT);
        title.setFont(UiTheme.TITLE);
        title.setForeground(UiTheme.TEXT);
        title.setBorder(BorderFactory.createEmptyBorder(14, 20, 14, 20));

        JLabel step = new JLabel("ロビー → お絵描き → 結果");
        step.setForeground(UiTheme.TEXT_MUTED);
        step.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 20));

        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(UiTheme.SURFACE);
        bar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, UiTheme.BORDER));
        bar.add(title, BorderLayout.WEST);
        bar.add(step, BorderLayout.EAST);
        return bar;
    }

    private static JPanel buildPlayPanel(JSplitPane splitPane, JLabel currentRoomLabel) {
        JPanel playPanel = new JPanel(new BorderLayout(8, 8));
        playPanel.setBackground(UiTheme.APP_BACKGROUND);
        playPanel.add(buildPlayHeader(currentRoomLabel), BorderLayout.NORTH);
        playPanel.add(splitPane, BorderLayout.CENTER);
        return playPanel;
    }

    private static JPanel buildPlayHeader(JLabel currentRoomLabel) {
        currentRoomLabel.setFont(UiTheme.HEADING_FONT);
        currentRoomLabel.setForeground(UiTheme.ACCENT);

        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
        header.setBackground(UiTheme.SURFACE);
        header.setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 14));
        header.add(currentRoomLabel);
        return header;
    }

    private static JPanel buildDrawPanel(DrawPanel drawPanel) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(UiTheme.APP_BACKGROUND);
        panel.add(DrawLauncher.buildToolDock(drawPanel), BorderLayout.WEST);
        panel.add(drawPanel, BorderLayout.CENTER);
        DrawLauncher.installShortcuts(panel, drawPanel);
        return panel;
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
