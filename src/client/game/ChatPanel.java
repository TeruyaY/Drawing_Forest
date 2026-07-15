package client.game;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

import client.UiTheme;

/** 回答とゲーム内メッセージを一か所で扱うチャットUI。 */
public class ChatPanel extends JPanel {
    private final DefaultListModel<String> chatModel = new DefaultListModel<>();
    private final JList<String> chatList = new JList<>(chatModel);
    private final JTextField inputField = new JTextField();
    private final JButton sendButton = UiTheme.primaryButton("送信");
    private final JLabel roleLabel = new JLabel("ゲーム開始を待っています");
    private final JLabel themeLabel = new JLabel("お題はラウンド開始時に表示されます");
    private final JLabel timerLabel = new JLabel("残り時間  --");
    private final JLabel resultLabel = new JLabel(" ");

    public ChatPanel() {
        setLayout(new BorderLayout(0, 10));
        setBackground(UiTheme.SURFACE);
        setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
        add(buildHeader(), BorderLayout.NORTH);

        chatList.setCellRenderer(new ChatCellRenderer());
        chatList.setFixedCellHeight(38);
        chatList.setBackground(UiTheme.SURFACE);
        chatList.getAccessibleContext().setAccessibleName("回答チャットの履歴");
        JScrollPane scroll = new JScrollPane(chatList);
        scroll.setBorder(BorderFactory.createLineBorder(UiTheme.BORDER));
        add(scroll, BorderLayout.CENTER);
        add(buildInputArea(), BorderLayout.SOUTH);

        sendButton.addActionListener(this::send);
        inputField.addActionListener(this::send);
    }

    public void addChatMessage(String message) {
        chatModel.addElement(message);
        chatList.ensureIndexIsVisible(chatModel.getSize() - 1);
    }

    public void clearChat() {
        chatModel.clear();
        resultLabel.setText(" ");
    }

    public void setRoundInfo(String role, String drawerName, String theme) {
        boolean drawer = "DRAWER".equals(role);
        roleLabel.setText(drawer ? "あなたが描く番です" : drawerName + " さんの絵を当てよう");
        roleLabel.setForeground(drawer ? UiTheme.DRAWER : UiTheme.GUESSER);
        themeLabel.setText(drawer
                ? "お題: " + (theme == null || theme.isEmpty() ? "未設定" : theme)
                : "お題は正解するまで秘密です");
        inputField.setEnabled(!drawer);
        sendButton.setEnabled(!drawer);
        inputField.setToolTipText(drawer ? "描く人は回答できません" : "答えを入力してEnterで送信");
        revalidate();
        repaint();
    }

    public void setTimeRemaining(int seconds) {
        timerLabel.setText("残り時間  " + seconds + "秒");
        timerLabel.setForeground(seconds <= 10 ? UiTheme.DANGER : UiTheme.TEXT);
        repaint();
    }

    public void showResult(String message) {
        String localized = localizeResult(message);
        resultLabel.setText(localized.isEmpty() ? " " : localized);
        resultLabel.setForeground(resultColor(message));
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout(10, 4));
        header.setOpaque(false);
        JLabel title = new JLabel("回答チャット");
        title.setFont(UiTheme.TITLE.deriveFont(17f));
        header.add(title, BorderLayout.NORTH);

        JPanel roundInfo = new JPanel(new BorderLayout(8, 2));
        roundInfo.setOpaque(false);
        roleLabel.setFont(UiTheme.LABEL);
        themeLabel.setForeground(UiTheme.TEXT_MUTED);
        timerLabel.setFont(UiTheme.LABEL);
        roundInfo.add(roleLabel, BorderLayout.NORTH);
        roundInfo.add(themeLabel, BorderLayout.CENTER);
        roundInfo.add(timerLabel, BorderLayout.SOUTH);
        header.add(roundInfo, BorderLayout.CENTER);
        return header;
    }

    private JPanel buildInputArea() {
        JPanel area = new JPanel(new BorderLayout(8, 6));
        area.setOpaque(false);
        inputField.setBorder(UiTheme.compoundBorder(UiTheme.BORDER, 9, 10));
        UiTheme.setAccessibleText(inputField, "答え", "答えを入力してEnterで送信");
        UiTheme.setAccessibleText(sendButton, "答えを送信", "入力した答えを送信");
        area.add(inputField, BorderLayout.CENTER);
        area.add(sendButton, BorderLayout.EAST);
        resultLabel.setFont(UiTheme.LABEL);
        resultLabel.getAccessibleContext().setAccessibleName("回答結果");
        area.add(resultLabel, BorderLayout.SOUTH);
        return area;
    }

    private java.awt.Color resultColor(String message) {
        if (message != null && message.startsWith("Correct")) {
            return UiTheme.SUCCESS;
        }
        if (message != null && (message.startsWith("Wrong") || message.startsWith("Error")
                || message.contains("required") || message.contains("least"))) {
            return UiTheme.DANGER;
        }
        return UiTheme.TEXT_MUTED;
    }

    private String localizeResult(String message) {
        if (message == null) return "";
        if (message.startsWith("Correct!")) return "正解！ " + message.substring("Correct!".length()).trim();
        if (message.startsWith("Wrong")) return "違います。もう一度考えてみよう";
        if (message.startsWith("Already correct")) return "すでに正解しています";
        if (message.startsWith("Not connected")) return "サーバーに接続されていません";
        if (message.startsWith("Join a room first")) return "先にルームへ参加してください";
        return message;
    }

    private void send(ActionEvent event) {
        String text = inputField.getText().trim();
        if (text.isEmpty()) {
            resultLabel.setText("答えを入力してください");
            resultLabel.setForeground(UiTheme.DANGER);
            inputField.requestFocusInWindow();
            return;
        }
        inputField.setText("");
        GameController.submitChat(text);
    }

    private static class ChatCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                boolean isSelected, boolean cellHasFocus) {
            JLabel label = (JLabel) super.getListCellRendererComponent(
                    list, value, index, isSelected, cellHasFocus);
            label.setBorder(BorderFactory.createEmptyBorder(7, 10, 7, 10));
            label.setBackground(isSelected ? UiTheme.ACCENT_SOFT
                    : (index % 2 == 0 ? UiTheme.SURFACE : UiTheme.SURFACE_MUTED));
            label.setForeground(UiTheme.TEXT);
            return label;
        }
    }
}
