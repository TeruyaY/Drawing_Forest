package client.game;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
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

/** ゲーム終了時に最終結果を表示し、「終了」ボタンでロビーへ戻すための画面。 */
public class ResultPanel extends JPanel {
    private final JLabel titleLabel = new JLabel("ゲーム終了！ 最終結果", SwingConstants.CENTER);
    private final DefaultListModel<String> rankingModel = new DefaultListModel<>();
    private final JList<String> rankingList = new JList<>(rankingModel);
    private final JButton closeButton = new JButton("終了");

    public ResultPanel() {
        setLayout(new BorderLayout(16, 16));
        setBorder(BorderFactory.createEmptyBorder(24, 48, 24, 48));
        setBackground(UiTheme.BACKGROUND);

        titleLabel.setFont(UiTheme.TITLE_FONT);
        titleLabel.setForeground(UiTheme.PRIMARY_DARK);
        add(titleLabel, BorderLayout.NORTH);

        rankingList.setCellRenderer(new RankRenderer());
        rankingList.setFixedCellHeight(44);
        rankingList.setBackground(UiTheme.BACKGROUND);
        JScrollPane scroll = new JScrollPane(rankingList);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        add(scroll, BorderLayout.CENTER);

        closeButton.setFont(closeButton.getFont().deriveFont(Font.BOLD, 16f));
        closeButton.setBackground(UiTheme.PRIMARY);
        closeButton.setForeground(Color.WHITE);
        closeButton.setOpaque(true);
        closeButton.setBorderPainted(false);
        closeButton.setBorder(BorderFactory.createEmptyBorder(10, 32, 10, 32));

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.setBackground(UiTheme.BACKGROUND);
        buttonPanel.add(closeButton);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    /** "名前=点数;名前=点数..." を、点数の高い順に並べ替えて表示する。 */
    public void setScores(String scoreText) {
        rankingModel.clear();
        for (String line : buildRankingLines(scoreText)) {
            rankingModel.addElement(line);
        }
    }

    /** 「終了」ボタンが押されたときの処理を登録する。 */
    public void setOnClose(Runnable listener) {
        for (var existing : closeButton.getActionListeners()) {
            closeButton.removeActionListener(existing);
        }
        closeButton.addActionListener(e -> listener.run());
    }

    private List<String> buildRankingLines(String scoreText) {
        List<String> lines = new ArrayList<>();
        if (scoreText == null || scoreText.trim().isEmpty()) {
            return lines;
        }

        List<String[]> entries = new ArrayList<>();
        for (String entry : scoreText.split(";")) {
            String trimmed = entry.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            String[] parts = trimmed.split("=", 2);
            String name = parts[0];
            String scoreStr = parts.length > 1 ? parts[1] : "0";
            entries.add(new String[] { name, scoreStr });
        }

        entries.sort((a, b) -> Integer.compare(parseScore(b[1]), parseScore(a[1])));

        int rank = 1;
        for (String[] entry : entries) {
            lines.add(rank + "位   " + entry[0] + "   -   " + entry[1] + "点");
            rank++;
        }
        return lines;
    }

    private int parseScore(String value) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /** 1〜3位を金・銀・銅色で強調するセル描画。 */
    private static class RankRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                boolean isSelected, boolean cellHasFocus) {
            JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            label.setOpaque(true);
            label.setBorder(BorderFactory.createEmptyBorder(6, 20, 6, 20));

            Color rankColor;
            float fontSize;
            switch (index) {
                case 0:
                    rankColor = UiTheme.GOLD;
                    fontSize = 22f;
                    break;
                case 1:
                    rankColor = UiTheme.SILVER;
                    fontSize = 19f;
                    break;
                case 2:
                    rankColor = UiTheme.BRONZE;
                    fontSize = 17f;
                    break;
                default:
                    rankColor = Color.DARK_GRAY;
                    fontSize = 16f;
            }

            label.setForeground(rankColor);
            label.setFont(label.getFont().deriveFont(Font.BOLD, fontSize));
            label.setBackground(index % 2 == 0 ? UiTheme.PRIMARY_LIGHT : Color.WHITE);
            return label;
        }
    }
}
