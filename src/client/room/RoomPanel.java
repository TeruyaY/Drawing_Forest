package client.room;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;

import client.ui.UITheme;

/** ゲーム開始までの流れが一画面で分かるロビーUI。 */
public class RoomPanel extends JPanel {
    private final JTextField userNameField = new JTextField("Player", 14);
    private final JTextField roomNameField = new JTextField(14);
    private final DefaultListModel<RoomController.RoomInfo> roomListModel = new DefaultListModel<>();
    private final JList<RoomController.RoomInfo> roomList = new JList<>(roomListModel);
    private final DefaultListModel<String> memberListModel = new DefaultListModel<>();
    private final JList<String> memberList = new JList<>(memberListModel);
    private final JLabel currentRoomLabel = new JLabel("未入室");
    private final JLabel statusLabel = new JLabel("準備ができたら部屋を作成するか、一覧から参加してください");

    public RoomPanel() {
        setLayout(new BorderLayout(0, 18));
        setBackground(UITheme.APP_BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(24, 28, 18, 28));
        add(buildTopSection(), BorderLayout.NORTH);
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
        if (rooms.isEmpty()) {
            showStatus("まだ部屋がありません。新しい部屋を作成できます");
        }
    }

    public void setMembers(List<String> members) {
        memberListModel.clear();
        for (String member : members) {
            memberListModel.addElement(member);
        }
    }

    public void setCurrentRoom(String roomName) {
        boolean joined = roomName != null && !roomName.isEmpty();
        currentRoomLabel.setText(joined ? "●  " + roomName : "未入室");
        currentRoomLabel.setForeground(joined ? UITheme.ACCENT : UITheme.TEXT_MUTED);
        selectRoomByName(roomName);
    }

    public void showStatus(String message) {
        statusLabel.setText(message == null || message.isEmpty() ? " " : message);
    }

    private JPanel buildTopSection() {
        JPanel top = new JPanel(new BorderLayout(20, 18));
        top.setOpaque(false);

        JPanel heading = new JPanel(new BorderLayout(0, 4));
        heading.setOpaque(false);
        JLabel title = new JLabel("一緒に描くルームを選ぼう");
        title.setFont(UITheme.TITLE.deriveFont(24f));
        JLabel subtitle = new JLabel("名前を決めて、部屋を作るか参加するとゲームを始められます");
        subtitle.setForeground(UITheme.TEXT_MUTED);
        heading.add(title, BorderLayout.NORTH);
        heading.add(subtitle, BorderLayout.SOUTH);
        top.add(heading, BorderLayout.NORTH);
        top.add(buildInputPanel(), BorderLayout.CENTER);
        return top;
    }

    private JPanel buildInputPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(UITheme.SURFACE);
        panel.setBorder(UITheme.panelBorder(16, 18));
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridy = 0;
        constraints.insets = new Insets(0, 0, 0, 10);
        constraints.anchor = GridBagConstraints.WEST;

        JLabel userLabel = fieldLabel("あなたの名前");
        userLabel.setLabelFor(userNameField);
        constraints.gridx = 0;
        panel.add(userLabel, constraints);

        prepareTextField(userNameField, "プレイヤー名", "ゲーム内で表示する名前を入力");
        constraints.gridx = 1;
        constraints.weightx = 0.35;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        panel.add(userNameField, constraints);

        JLabel roomLabel = fieldLabel("新しい部屋名");
        roomLabel.setLabelFor(roomNameField);
        constraints.gridx = 2;
        constraints.weightx = 0;
        constraints.fill = GridBagConstraints.NONE;
        constraints.insets = new Insets(0, 10, 0, 10);
        panel.add(roomLabel, constraints);

        prepareTextField(roomNameField, "新しい部屋名", "作成する部屋の名前を入力");
        constraints.gridx = 3;
        constraints.weightx = 0.45;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(0, 0, 0, 10);
        panel.add(roomNameField, constraints);

        JButton createButton = UITheme.primaryButton("部屋を作る");
        createButton.addActionListener(e -> RoomController.createRoom(roomNameField.getText(), userNameField.getText()));
        UITheme.setAccessibleText(createButton, "部屋を作る", "入力した名前で新しい部屋を作成");
        constraints.gridx = 4;
        constraints.weightx = 0;
        constraints.fill = GridBagConstraints.NONE;
        constraints.insets = new Insets(0, 0, 0, 0);
        panel.add(createButton, constraints);
        return panel;
    }

    private JPanel buildCenterPanel() {
        roomList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        roomList.setCellRenderer(new RoomCellRenderer());
        roomList.setFixedCellHeight(54);
        roomList.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        roomList.getAccessibleContext().setAccessibleName("参加できる部屋一覧");
        roomList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                if (event.getClickCount() == 2) {
                    joinSelectedRoom();
                }
            }
        });

        memberList.setCellRenderer(new MemberCellRenderer());
        memberList.setFixedCellHeight(44);
        memberList.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        memberList.getAccessibleContext().setAccessibleName("待機中のメンバー一覧");

        JPanel roomsPanel = sectionPanel("参加できる部屋", "ダブルクリックでも参加できます");
        roomsPanel.add(new JScrollPane(roomList), BorderLayout.CENTER);
        JButton refreshButton = UITheme.secondaryButton("一覧を更新");
        refreshButton.addActionListener(e -> RoomController.requestRoomList());
        JButton joinButton = UITheme.primaryButton("選んだ部屋に参加");
        joinButton.addActionListener(e -> joinSelectedRoom());
        JPanel roomActions = actionRow(refreshButton, joinButton);
        roomsPanel.add(roomActions, BorderLayout.SOUTH);

        JPanel membersPanel = sectionPanel("現在のルーム", "参加者がそろったらゲームを開始できます");
        JPanel roomState = new JPanel(new BorderLayout());
        roomState.setOpaque(false);
        roomState.setBorder(BorderFactory.createEmptyBorder(4, 4, 10, 4));
        JLabel stateLabel = new JLabel("ルーム");
        stateLabel.setForeground(UITheme.TEXT_MUTED);
        currentRoomLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        currentRoomLabel.setFont(UITheme.LABEL);
        currentRoomLabel.setForeground(UITheme.TEXT_MUTED);
        roomState.add(stateLabel, BorderLayout.WEST);
        roomState.add(currentRoomLabel, BorderLayout.CENTER);
        membersPanel.add(roomState, BorderLayout.NORTH);
        membersPanel.add(new JScrollPane(memberList), BorderLayout.CENTER);
        JButton startButton = UITheme.primaryButton("ゲームを開始");
        startButton.addActionListener(e -> RoomController.startGame());
        membersPanel.add(actionRow(startButton), BorderLayout.SOUTH);

        JPanel center = new JPanel(new GridLayout(1, 2, 16, 0));
        center.setOpaque(false);
        center.add(roomsPanel);
        center.add(membersPanel);
        return center;
    }

    private JPanel sectionPanel(String titleText, String hintText) {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setBackground(UITheme.SURFACE);
        panel.setBorder(UITheme.panelBorder(16, 16));
        JPanel heading = new JPanel(new BorderLayout(0, 3));
        heading.setOpaque(false);
        JLabel title = new JLabel(titleText);
        title.setFont(UITheme.TITLE.deriveFont(17f));
        JLabel hint = new JLabel(hintText);
        hint.setForeground(UITheme.TEXT_MUTED);
        heading.add(title, BorderLayout.NORTH);
        heading.add(hint, BorderLayout.SOUTH);
        panel.add(heading, BorderLayout.NORTH);
        return panel;
    }

    private JPanel actionRow(JButton... buttons) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        row.setOpaque(false);
        row.setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 0));
        for (JButton button : buttons) {
            row.add(button);
        }
        return row;
    }

    private JPanel buildStatusPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(UITheme.SURFACE_MUTED);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER),
                BorderFactory.createEmptyBorder(10, 13, 10, 13)));
        statusLabel.setForeground(UITheme.TEXT_MUTED);
        statusLabel.getAccessibleContext().setAccessibleName("現在の状態");
        panel.add(statusLabel, BorderLayout.CENTER);
        return panel;
    }

    private JLabel fieldLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(UITheme.LABEL);
        return label;
    }

    private void prepareTextField(JTextField field, String name, String description) {
        field.setMinimumSize(new Dimension(130, 40));
        field.setPreferredSize(new Dimension(180, 40));
        field.setBorder(UITheme.compoundBorder(UITheme.BORDER, 8, 10));
        UITheme.setAccessibleText(field, name, description);
    }

    private void joinSelectedRoom() {
        RoomController.RoomInfo selectedRoom = roomList.getSelectedValue();
        if (selectedRoom == null) {
            showStatus("参加する部屋を一覧から選んでください");
            roomList.requestFocusInWindow();
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

    private static class RoomCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                boolean isSelected, boolean cellHasFocus) {
            RoomController.RoomInfo room = (RoomController.RoomInfo) value;
            JLabel label = (JLabel) super.getListCellRendererComponent(
                    list, room.getName() + "     " + room.getMemberCount() + "人", index, isSelected, cellHasFocus);
            label.setFont(UITheme.BODY.deriveFont(Font.BOLD, 14f));
            label.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
            label.setBackground(isSelected ? UITheme.ACCENT_SOFT : UITheme.SURFACE);
            label.setForeground(UITheme.TEXT);
            return label;
        }
    }

    private static class MemberCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                boolean isSelected, boolean cellHasFocus) {
            JLabel label = (JLabel) super.getListCellRendererComponent(
                    list, "●  " + value, index, isSelected, cellHasFocus);
            label.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
            label.setForeground(UITheme.TEXT);
            label.setBackground(isSelected ? UITheme.ACCENT_SOFT : UITheme.SURFACE);
            return label;
        }
    }
}
