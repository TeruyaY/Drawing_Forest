package client.game;

import java.awt.BorderLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;

import client.UiTheme;
import client.FeedbackEffect;

/** ラウンド状況、回答チャット、スコアを縦方向に整理したゲームUI。 */
public class GamePanel extends JPanel {
    private final JLabel roundLabel = new JLabel("ラウンド  --");
    private final JLabel roleLabel = new JLabel("役割  --");
    private final JLabel drawerLabel = new JLabel("描く人  --");
    private final JLabel themeLabel = new JLabel("お題  --");
    private final JLabel timerLabel = new JLabel("残り  --");
    private final JTextArea scoreArea = new JTextArea(4, 20);
    private final ChatPanel chatPanel;
    private final FeedbackEffect feedbackEffect = new FeedbackEffect(this);

    public GamePanel(ChatPanel chatPanel) {
        this.chatPanel = chatPanel;
        setLayout(new BorderLayout(0, 10));
        setBackground(UiTheme.APP_BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(buildStatusPanel(), BorderLayout.NORTH);
        add(chatPanel, BorderLayout.CENTER);
        add(buildScorePanel(), BorderLayout.SOUTH);
    }

    public ChatPanel getChatPanel() {
        return chatPanel;
    }

    public void setRoundInfo(String roundText, String role, String drawer, String theme) {
        roundLabel.setText(roundText.replace("Round", "ラウンド"));
        boolean isDrawer = "DRAWER".equals(role);
        roleLabel.setText(isDrawer ? "描く人" : "回答者");
        roleLabel.setForeground(isDrawer ? UiTheme.DRAWER : UiTheme.GUESSER);
        drawerLabel.setText("描く人  " + drawer);
        themeLabel.setText(isDrawer && theme != null && !theme.isEmpty() ? "お題  " + theme : "お題  秘密");
        revalidate();
        repaint();
    }

    public void setTimeRemaining(int seconds) {
        timerLabel.setText("残り  " + seconds + "秒");
        timerLabel.setForeground(seconds <= 10 ? UiTheme.DANGER : UiTheme.TEXT);
        repaint();
    }

    public void setScores(String scoreText) {
        scoreArea.setText(formatScores(scoreText));
        feedbackEffect.play(FeedbackEffect.Type.SCORE);
    }

    private JPanel buildStatusPanel() {
        JPanel panel = new JPanel(new GridLayout(3, 2, 8, 5));
        panel.setBackground(UiTheme.SURFACE);
        panel.setBorder(UiTheme.panelBorder(12, 12));
        for (JLabel label : new JLabel[] {roundLabel, timerLabel, roleLabel, drawerLabel, themeLabel}) {
            label.setFont(UiTheme.LABEL);
        }
        panel.add(roundLabel);
        panel.add(timerLabel);
        panel.add(roleLabel);
        panel.add(drawerLabel);
        panel.add(themeLabel);
        panel.add(new JLabel(""));
        return panel;
    }

    private JPanel buildScorePanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 7));
        panel.setBackground(UiTheme.SURFACE);
        panel.setBorder(UiTheme.panelBorder(10, 12));
        JLabel title = new JLabel("現在のスコア");
        title.setFont(UiTheme.LABEL);
        panel.add(title, BorderLayout.NORTH);
        scoreArea.setEditable(false);
        scoreArea.setFocusable(false);
        scoreArea.setLineWrap(true);
        scoreArea.setWrapStyleWord(true);
        scoreArea.setBackground(UiTheme.SURFACE);
        scoreArea.setForeground(UiTheme.TEXT);
        scoreArea.getAccessibleContext().setAccessibleName("現在のスコア");
        panel.add(scoreArea, BorderLayout.CENTER);
        return panel;
    }

    private String formatScores(String scoreText) {
        if (scoreText == null || scoreText.trim().isEmpty()) {
            return "まだスコアはありません";
        }
        StringBuilder formatted = new StringBuilder();
        for (String entry : scoreText.split(";")) {
            if (entry.trim().isEmpty()) continue;
            if (formatted.length() > 0) formatted.append("   ");
            formatted.append(entry.replace("=", "  ")).append("点");
        }
        return formatted.toString();
    }

    @Override
    protected void paintChildren(Graphics graphics) {
        super.paintChildren(graphics);
        feedbackEffect.paint((Graphics2D) graphics, getWidth(), getHeight());
    }
}
