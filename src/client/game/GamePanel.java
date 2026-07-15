package client.game;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.border.TitledBorder;

import client.UiTheme;

public class GamePanel extends JPanel {
    private final JLabel roundLabel = new JLabel("Round: -");
    private final JLabel roleLabel = new JLabel("Role: -");
    private final JLabel drawerLabel = new JLabel("Drawer: -");
    private final JLabel themeLabel = new JLabel("Theme: -");
    private final JLabel timerLabel = new JLabel("Time: -");
    private final JTextArea scoreArea = new JTextArea(10, 20);
    private final ChatPanel chatPanel;

    public GamePanel(ChatPanel chatPanel) {
        this.chatPanel = chatPanel;
        roundLabel.setFont(roundLabel.getFont().deriveFont(Font.BOLD));
        roleLabel.setFont(roleLabel.getFont().deriveFont(Font.BOLD));
        themeLabel.setFont(themeLabel.getFont().deriveFont(Font.BOLD));
        timerLabel.setFont(timerLabel.getFont().deriveFont(Font.BOLD, 15f));
        setLayout(new BorderLayout(8, 8));
        setBackground(UiTheme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        add(buildStatusPanel(), BorderLayout.NORTH);
        add(chatPanel, BorderLayout.CENTER);
        add(buildScorePanel(), BorderLayout.EAST);
    }

    public ChatPanel getChatPanel() {
        return chatPanel;
    }

    public void setRoundInfo(String roundText, String role, String drawer, String theme) {
        roundLabel.setText("Round: " + roundText);
        roleLabel.setText("Role: " + role);
        roleLabel.setForeground("DRAWER".equals(role) ? UiTheme.DRAWER : UiTheme.GUESSER);
        drawerLabel.setText("Drawer: " + drawer);
        themeLabel.setText(theme == null || theme.isEmpty() ? "Theme: (guessers can't see this)" : "Theme: " + theme);
        refreshStatusLayout();
    }

    public void setTimeRemaining(int seconds) {
        timerLabel.setText("Time: " + seconds);
        timerLabel.setForeground(seconds <= 10 ? UiTheme.DANGER : UiTheme.PRIMARY_DARK);
        refreshStatusLayout();
    }

    public void setScores(String scoreText) {
        scoreArea.setText(formatScores(scoreText));
    }

    private JPanel buildStatusPanel() {
        // 1行のFlowLayoutは狭い分割ペインで折り返した行が親の高さからはみ出すため、
        // 情報を明示的に2行へ分け、Theme/Timeを常に表示領域内へ収める。
        JPanel firstRow = statusRow(16);
        firstRow.add(roundLabel);
        firstRow.add(roleLabel);
        firstRow.add(drawerLabel);

        JPanel secondRow = statusRow(16);
        secondRow.add(themeLabel);
        secondRow.add(timerLabel);

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(UiTheme.BACKGROUND);
        panel.add(firstRow);
        panel.add(secondRow);
        return panel;
    }

    private JPanel statusRow(int horizontalGap) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, horizontalGap, 2));
        row.setAlignmentX(LEFT_ALIGNMENT);
        row.setBackground(UiTheme.BACKGROUND);
        return row;
    }

    private void refreshStatusLayout() {
        revalidate();
        repaint();
    }

    private JPanel buildScorePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(UiTheme.PRIMARY_LIGHT);
        TitledBorder border = BorderFactory.createTitledBorder("Scores");
        border.setTitleFont(border.getTitleFont().deriveFont(Font.BOLD));
        border.setTitleColor(UiTheme.PRIMARY_DARK);
        panel.setBorder(border);
        scoreArea.setEditable(false);
        scoreArea.setBackground(UiTheme.PRIMARY_LIGHT);
        scoreArea.setFont(scoreArea.getFont().deriveFont(15f));
        panel.add(scoreArea, BorderLayout.CENTER);
        return panel;
    }

    private String formatScores(String scoreText) {
        if (scoreText == null || scoreText.trim().isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        String[] entries = scoreText.split(";");
        for (String entry : entries) {
            if (entry.trim().isEmpty()) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(System.lineSeparator());
            }
            sb.append(entry.replace("=", ": "));
        }
        return sb.toString();
    }
}
