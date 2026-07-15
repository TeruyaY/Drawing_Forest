package client.room;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.border.TitledBorder;

import client.UiTheme;

public class RoomPanel extends JPanel {
    private final JTextField userNameField = new JTextField("Player", 14);
    private final JTextField roomNameField = new JTextField(14);
    private final DefaultListModel<RoomController.RoomInfo> roomListModel = new DefaultListModel<>();
    private final JList<RoomController.RoomInfo> roomList = new JList<>(roomListModel);
    private final DefaultListModel<String> memberListModel = new DefaultListModel<>();
    private final JList<String> memberList = new JList<>(memberListModel);
    private final JLabel currentRoomLabel = new JLabel("未入室");
    private final JLabel statusLabel = new JLabel(" ");
    private final JLabel readyStatusLabel = new JLabel(" ");

    public RoomPanel() {
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        setBackground(UiTheme.BACKGROUND);
        add(buildInputPanel(), BorderLayout.NORTH);
        add(buildCenterPanel(), BorderLayout.CENTER);
        add(buildStatusPanel(), BorderLayout.SOUTH);
    }

    public void setRooms(List<RoomController.RoomInfo> rooms) {
        String selectedName = getSelectedRoomName();
        roomListModel.clear();
        for (RoomController.RoomInfo room : rooms) {
            roomListModel.addElement(room);
        }
        selectRoomByName(selectedName);
    }

    public void setMembers(List<String> members) {
        memberListModel.clear();
        for (String member : members) {
            memberListModel.addElement(member);
        }
    }

    public void setCurrentRoom(String roomName) {
        currentRoomLabel.setText(roomName == null || roomName.isEmpty() ? "未入室" : roomName);
        selectRoomByName(roomName);
    }

    public void showStatus(String message) {
        statusLabel.setText(message == null || message.isEmpty() ? " " : message);
    }

    public void setReadyStatus(String message) {
        readyStatusLabel.setText(message == null || message.isEmpty() ? " " : message);
    }

    private JPanel buildInputPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(UiTheme.BACKGROUND);
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(0, 0, 6, 8);
        c.anchor = GridBagConstraints.WEST;

        c.gridx = 0;
        c.gridy = 0;
        panel.add(new JLabel("ユーザー名"), c);

        c.gridx = 1;
        panel.add(userNameField, c);

        c.gridx = 0;
        c.gridy = 1;
        panel.add(new JLabel("部屋名"), c);

        c.gridx = 1;
        panel.add(roomNameField, c);

        JButton createButton = styledButton("作成", UiTheme.PRIMARY);
        createButton.addActionListener(e -> RoomController.createRoom(roomNameField.getText(), userNameField.getText()));
        c.gridx = 2;
        panel.add(createButton, c);

        JButton refreshButton = new JButton("更新");
        refreshButton.addActionListener(e -> RoomController.requestRoomList());
        c.gridx = 3;
        panel.add(refreshButton, c);

        return panel;
    }

    private JButton styledButton(String text, Color background) {
        JButton button = new JButton(text);
        button.setBackground(background);
        button.setForeground(Color.WHITE);
        button.setOpaque(true);
        button.setBorderPainted(false);
        button.setFont(button.getFont().deriveFont(Font.BOLD));
        return button;
    }

    private JSplitPane buildCenterPanel() {
        roomList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        roomList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    joinSelectedRoom();
                }
            }
        });

        JPanel roomsPanel = new JPanel(new BorderLayout(6, 6));
        roomsPanel.setBackground(UiTheme.BACKGROUND);
        roomsPanel.setBorder(titledBorder("部屋一覧"));
        JScrollPane roomScroll = new JScrollPane(roomList);
        roomScroll.setPreferredSize(new Dimension(380, 420));
        roomsPanel.add(roomScroll, BorderLayout.CENTER);

        JButton joinButton = styledButton("参加", UiTheme.GUESSER);
        joinButton.addActionListener(e -> joinSelectedRoom());
        JPanel roomButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        roomButtons.setBackground(UiTheme.BACKGROUND);
        roomButtons.add(joinButton);
        roomsPanel.add(roomButtons, BorderLayout.SOUTH);

        JPanel membersPanel = new JPanel(new BorderLayout(6, 6));
        membersPanel.setBackground(UiTheme.BACKGROUND);
        membersPanel.setBorder(titledBorder("待機中メンバー"));
        membersPanel.add(buildCurrentRoomPanel(), BorderLayout.NORTH);
        membersPanel.add(new JScrollPane(memberList), BorderLayout.CENTER);
        membersPanel.add(buildStartGamePanel(), BorderLayout.SOUTH);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, roomsPanel, membersPanel);
        splitPane.setResizeWeight(0.45);
        splitPane.setBorder(null);
        return splitPane;
    }

    private TitledBorder titledBorder(String title) {
        TitledBorder border = BorderFactory.createTitledBorder(title);
        border.setTitleFont(border.getTitleFont().deriveFont(Font.BOLD));
        border.setTitleColor(UiTheme.PRIMARY_DARK);
        return border;
    }

    private JPanel buildStartGamePanel() {
        JPanel panel = new JPanel(new BorderLayout(6, 0));
        panel.setBackground(UiTheme.BACKGROUND);
        readyStatusLabel.setHorizontalAlignment(SwingConstants.LEFT);
        readyStatusLabel.setFont(readyStatusLabel.getFont().deriveFont(Font.BOLD));
        panel.add(readyStatusLabel, BorderLayout.CENTER);

        JButton startButton = styledButton("ゲーム開始", UiTheme.DRAWER);
        startButton.addActionListener(e -> RoomController.startGame());
        panel.add(startButton, BorderLayout.EAST);
        return panel;
    }

    private JPanel buildCurrentRoomPanel() {
        JPanel panel = new JPanel(new BorderLayout(6, 0));
        panel.setBackground(UiTheme.BACKGROUND);
        panel.add(new JLabel("現在の部屋"), BorderLayout.WEST);
        currentRoomLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        currentRoomLabel.setForeground(UiTheme.PRIMARY_DARK);
        currentRoomLabel.setFont(currentRoomLabel.getFont().deriveFont(Font.BOLD));
        panel.add(currentRoomLabel, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildStatusPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(UiTheme.BACKGROUND);
        statusLabel.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
        statusLabel.setForeground(UiTheme.PRIMARY_DARK);
        panel.add(statusLabel, BorderLayout.CENTER);
        return panel;
    }

    private void joinSelectedRoom() {
        RoomController.RoomInfo selectedRoom = roomList.getSelectedValue();
        if (selectedRoom == null) {
            showStatus("参加する部屋を選んでください");
            return;
        }
        RoomController.joinRoom(selectedRoom.getName(), userNameField.getText());
    }

    private String getSelectedRoomName() {
        RoomController.RoomInfo selected = roomList.getSelectedValue();
        return selected == null ? null : selected.getName();
    }

    private void selectRoomByName(String roomName) {
        if (roomName == null || roomName.isEmpty()) {
            return;
        }
        for (int i = 0; i < roomListModel.size(); i++) {
            if (roomName.equals(roomListModel.get(i).getName())) {
                roomList.setSelectedIndex(i);
                roomList.ensureIndexIsVisible(i);
                return;
            }
        }
    }
}
