package client.game;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

import client.UiTheme;

public class ChatPanel extends JPanel {
    private final DefaultListModel<String> chatModel = new DefaultListModel<>();
    private final JList<String> chatList = new JList<>(chatModel);
    private final JTextField inputField = new JTextField(24);
    private final JButton sendButton = new JButton("Send");
    private final JLabel roleLabel = new JLabel("Role: -");
    private final JLabel themeLabel = new JLabel("Theme: -");
    private final JLabel timerLabel = new JLabel("Time: -");
    private final JLabel resultLabel = new JLabel(" ");

    public ChatPanel() {
        setLayout(new BorderLayout(6, 6));
        setBackground(UiTheme.BACKGROUND);
        TitledBorder border = BorderFactory.createTitledBorder("Chat");
        border.setTitleFont(border.getTitleFont().deriveFont(Font.BOLD));
        border.setTitleColor(UiTheme.PRIMARY_DARK);
        setBorder(border);

        roleLabel.setFont(roleLabel.getFont().deriveFont(Font.BOLD));
        themeLabel.setFont(themeLabel.getFont().deriveFont(Font.BOLD));
        timerLabel.setFont(timerLabel.getFont().deriveFont(Font.BOLD, 15f));

        JPanel roleRow = statusRow();
        roleRow.add(roleLabel);
        JPanel themeRow = statusRow();
        themeRow.add(themeLabel);
        JPanel timeRow = statusRow();
        timeRow.add(timerLabel);

        // チャット欄はスコア欄と横幅を分け合うため特に狭い。
        // FlowLayout任せの折り返しでは下段が切れるので、各情報を固定の行に置く。
        JPanel statusPanel = new JPanel();
        statusPanel.setLayout(new BoxLayout(statusPanel, BoxLayout.Y_AXIS));
        statusPanel.setBackground(UiTheme.BACKGROUND);
        statusPanel.add(roleRow);
        statusPanel.add(themeRow);
        statusPanel.add(timeRow);
        add(statusPanel, BorderLayout.NORTH);

        add(new JScrollPane(chatList), BorderLayout.CENTER);

        sendButton.setBackground(UiTheme.PRIMARY);
        sendButton.setForeground(Color.WHITE);
        sendButton.setOpaque(true);
        sendButton.setBorderPainted(false);

        JPanel inputPanel = new JPanel(new BorderLayout(6, 0));
        inputPanel.setBackground(UiTheme.BACKGROUND);
        inputPanel.add(inputField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);
        resultLabel.setFont(resultLabel.getFont().deriveFont(Font.BOLD));
        inputPanel.add(resultLabel, BorderLayout.SOUTH);
        add(inputPanel, BorderLayout.SOUTH);

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
        roleLabel.setText("Role: " + role + " / Drawer: " + drawerName);
        roleLabel.setForeground("DRAWER".equals(role) ? UiTheme.DRAWER : UiTheme.GUESSER);
        themeLabel.setText(theme == null || theme.isEmpty() ? "Theme: (guessers can't see this)" : "Theme: " + theme);
        refreshStatusLayout();
    }

    public void setTimeRemaining(int seconds) {
        timerLabel.setText("Time: " + seconds);
        timerLabel.setForeground(seconds <= 10 ? UiTheme.DANGER : UiTheme.PRIMARY_DARK);
        refreshStatusLayout();
    }

    public void showResult(String message) {
        resultLabel.setText(message == null || message.isEmpty() ? " " : message);
        resultLabel.setForeground(resultColor(message));
    }

    private Color resultColor(String message) {
        if (message == null) {
            return Color.DARK_GRAY;
        }
        if (message.startsWith("Correct")) {
            return UiTheme.SUCCESS;
        }
        if (message.startsWith("Wrong") || message.startsWith("Error") || message.contains("required")
                || message.contains("least")) {
            return UiTheme.DANGER;
        }
        return Color.DARK_GRAY;
    }

    private JPanel statusRow() {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        row.setAlignmentX(LEFT_ALIGNMENT);
        row.setBackground(UiTheme.BACKGROUND);
        return row;
    }

    private void refreshStatusLayout() {
        revalidate();
        repaint();
    }

    private void send(ActionEvent event) {
        String text = inputField.getText().trim();
        if (text.isEmpty()) {
            return;
        }
        inputField.setText("");
        GameController.submitChat(text);
    }
}
