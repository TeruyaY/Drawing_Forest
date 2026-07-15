package client.game;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;

import client.UiTheme;
import client.FeedbackEffect;

/** 最終順位を読みやすく表示し、次のゲームへ戻る結果画面。 */
public class ResultPanel extends JPanel {
    private final DefaultListModel<String> rankingModel = new DefaultListModel<>();
    private final JList<String> rankingList = new JList<>(rankingModel);
    private final JButton closeButton = UiTheme.primaryButton("ロビーへ戻る");
    private final FeedbackEffect feedbackEffect = new FeedbackEffect(this);

    public ResultPanel() {
        setLayout(new BorderLayout(0, 24));
        setBorder(BorderFactory.createEmptyBorder(42, 120, 36, 120));
        setBackground(UiTheme.APP_BACKGROUND);
        add(buildHeading(), BorderLayout.NORTH);

        rankingList.setCellRenderer(new RankRenderer());
        rankingList.setFixedCellHeight(62);
        rankingList.setBackground(UiTheme.SURFACE);
        rankingList.getAccessibleContext().setAccessibleName("最終ランキング");
        JScrollPane scroll = new JScrollPane(rankingList);
        scroll.setBorder(UiTheme.panelBorder(8, 8));
        scroll.setPreferredSize(new Dimension(640, 360));
        add(scroll, BorderLayout.CENTER);

        UiTheme.setAccessibleText(closeButton, "ロビーへ戻る", "結果を閉じてルーム一覧へ戻る");
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.setOpaque(false);
        buttonPanel.add(closeButton);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    public void setScores(String scoreText) {
        rankingModel.clear();
        for (String line : buildRankingLines(scoreText)) {
            rankingModel.addElement(line);
        }
        if (rankingModel.isEmpty()) {
            rankingModel.addElement("スコアを取得できませんでした");
        }
        feedbackEffect.play(FeedbackEffect.Type.CELEBRATION);
    }

    public void setOnClose(Runnable listener) {
        for (java.awt.event.ActionListener existing : closeButton.getActionListeners()) {
            closeButton.removeActionListener(existing);
        }
        closeButton.addActionListener(event -> listener.run());
    }

    private JPanel buildHeading() {
        JPanel heading = new JPanel(new BorderLayout(0, 8));
        heading.setOpaque(false);
        JLabel eyebrow = new JLabel("GAME FINISHED", SwingConstants.CENTER);
        eyebrow.setFont(UiTheme.LABEL);
        eyebrow.setForeground(UiTheme.ACCENT);
        JLabel title = new JLabel("最終結果", SwingConstants.CENTER);
        title.setFont(UiTheme.TITLE.deriveFont(30f));
        JLabel subtitle = new JLabel("みんなの順位を確認して、次のゲームへ進もう", SwingConstants.CENTER);
        subtitle.setForeground(UiTheme.TEXT_MUTED);
        heading.add(eyebrow, BorderLayout.NORTH);
        heading.add(title, BorderLayout.CENTER);
        heading.add(subtitle, BorderLayout.SOUTH);
        return heading;
    }

    private List<String> buildRankingLines(String scoreText) {
        List<String[]> entries = new ArrayList<>();
        if (scoreText != null) {
            for (String entry : scoreText.split(";")) {
                String trimmed = entry.trim();
                if (trimmed.isEmpty()) continue;
                String[] parts = trimmed.split("=", 2);
                entries.add(new String[] {parts[0], parts.length > 1 ? parts[1] : "0"});
            }
        }
        entries.sort((left, right) -> Integer.compare(parseScore(right[1]), parseScore(left[1])));

        List<String> lines = new ArrayList<>();
        for (int i = 0; i < entries.size(); i++) {
            String prefix = i == 0 ? "★" : String.valueOf(i + 1);
            lines.add(prefix + "     " + entries.get(i)[0] + "     " + entries.get(i)[1] + "点");
        }
        return lines;
    }

    private int parseScore(String value) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException exception) {
            return 0;
        }
    }

    @Override
    protected void paintChildren(Graphics graphics) {
        super.paintChildren(graphics);
        feedbackEffect.paint((Graphics2D) graphics, getWidth(), getHeight());
    }

    private static class RankRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                boolean isSelected, boolean cellHasFocus) {
            JLabel label = (JLabel) super.getListCellRendererComponent(
                    list, value, index, isSelected, cellHasFocus);
            label.setBorder(BorderFactory.createEmptyBorder(10, 24, 10, 24));
            label.setBackground(isSelected ? UiTheme.ACCENT_SOFT
                    : (index == 0 ? new java.awt.Color(249, 242, 211) : UiTheme.SURFACE));
            label.setForeground(index == 0 ? new java.awt.Color(130, 91, 13) : UiTheme.TEXT);
            label.setFont(UiTheme.BODY.deriveFont(Font.BOLD, index == 0 ? 18f : 16f));
            return label;
        }
    }
}
