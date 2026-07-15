package client.draw;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import client.GameClient;
import client.ui.UITheme;

/** 担当B（お絵描き）の単体起動用UI。 */
public class DrawLauncher {
    private static final Color[] PALETTE = {
        new Color(28, 34, 30),
        new Color(204, 57, 63),
        new Color(32, 101, 181),
        new Color(36, 132, 84),
        new Color(226, 174, 35),
        new Color(121, 76, 167)
    };
    private static final String[] COLOR_NAMES = {"BLACK", "RED", "BLUE", "GREEN", "YELLOW", "PURPLE"};
    private static final String[] COLOR_LABELS = {"黒", "赤", "青", "緑", "黄", "紫"};

    public static void main(String[] args) {
        String host = args.length > 0 ? args[0] : "localhost";
        int port = args.length > 1 ? parsePort(args[1]) : 8080;
        String roomId = args.length > 2 ? args[2] : "testroom";

        GameClient client = new GameClient();
        boolean connected = true;
        try {
            client.connect(host, port);
        } catch (Exception exception) {
            System.out.println("サーバーに接続できませんでした（ローカル描画のみ）: " + exception.getMessage());
            client = null;
            connected = false;
        }

        GameClient connectedClient = client;
        boolean connectionState = connected;
        SwingUtilities.invokeLater(() -> buildAndShow(connectedClient, roomId, connectionState));
    }

    private static void buildAndShow(GameClient client, String roomId, boolean connected) {
        UITheme.install();
        DrawPanel canvas = new DrawPanel();
        DrawController.init(client, canvas);
        DrawController.setRoomId(roomId);

        JFrame frame = new JFrame("Drawing Forest | " + roomId);
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(UITheme.APP_BACKGROUND);
        root.add(buildHeader(roomId, connected), BorderLayout.NORTH);
        root.add(buildToolDock(canvas), BorderLayout.WEST);
        root.add(canvas, BorderLayout.CENTER);
        root.add(buildFooter(connected), BorderLayout.SOUTH);

        installShortcuts(root, canvas);
        frame.setContentPane(root);
        frame.setSize(1280, 820);
        frame.setMinimumSize(new Dimension(900, 600));
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }

    private static JPanel buildHeader(String roomId, boolean connected) {
        JPanel header = new JPanel(new BorderLayout(18, 0));
        header.setBackground(UITheme.SURFACE);
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, UITheme.BORDER),
                BorderFactory.createEmptyBorder(14, 20, 14, 20)));

        JPanel titleGroup = new JPanel(new BorderLayout(10, 0));
        titleGroup.setOpaque(false);
        JLabel title = new JLabel("Drawing Forest");
        title.setFont(UITheme.TITLE);
        titleGroup.add(title, BorderLayout.WEST);

        JLabel room = new JLabel("ルーム  " + roomId);
        room.setFont(UITheme.LABEL);
        room.setOpaque(true);
        room.setBackground(UITheme.SURFACE_MUTED);
        room.setBorder(BorderFactory.createEmptyBorder(7, 11, 7, 11));
        titleGroup.add(room, BorderLayout.CENTER);
        header.add(titleGroup, BorderLayout.WEST);

        JLabel role = new JLabel("あなたが描く番です", SwingConstants.CENTER);
        role.setFont(UITheme.LABEL.deriveFont(15f));
        role.setForeground(UITheme.ACCENT);
        header.add(role, BorderLayout.CENTER);

        JLabel connection = new JLabel(connected ? "●  オンライン" : "○  ローカルモード");
        connection.setFont(UITheme.LABEL);
        connection.setForeground(connected ? UITheme.ACCENT : UITheme.TEXT_MUTED);
        header.add(connection, BorderLayout.EAST);
        return header;
    }

    private static JPanel buildToolDock(DrawPanel canvas) {
        JPanel dock = new JPanel();
        dock.setLayout(new javax.swing.BoxLayout(dock, javax.swing.BoxLayout.Y_AXIS));
        dock.setBackground(UITheme.SURFACE);
        dock.setPreferredSize(new Dimension(184, 0));
        dock.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 0, 1, UITheme.BORDER),
                BorderFactory.createEmptyBorder(18, 14, 18, 14)));

        JLabel toolsLabel = sectionLabel("ツール");
        dock.add(toolsLabel);
        dock.add(verticalGap(8));

        ButtonGroup toolGroup = new ButtonGroup();
        JToggleButton penButton = toolButton("ペン   P", "線を描く（P）");
        JToggleButton eraserButton = toolButton("消しゴム   E", "描いた線を消す（E）");
        toolGroup.add(penButton);
        toolGroup.add(eraserButton);
        penButton.setSelected(true);
        updateToolSelection(penButton, true);
        penButton.addActionListener(e -> {
            canvas.usePen();
            updateToolSelection(penButton, true);
            updateToolSelection(eraserButton, false);
        });
        eraserButton.addActionListener(e -> {
            canvas.setColorName("WHITE");
            updateToolSelection(penButton, false);
            updateToolSelection(eraserButton, true);
        });
        canvas.putClientProperty("penButton", penButton);
        canvas.putClientProperty("eraserButton", eraserButton);
        dock.add(penButton);
        dock.add(verticalGap(7));
        dock.add(eraserButton);
        dock.add(verticalGap(22));

        dock.add(sectionLabel("カラー"));
        dock.add(verticalGap(8));
        JPanel colors = new JPanel(new GridLayout(3, 2, 7, 7));
        colors.setOpaque(false);
        ButtonGroup colorGroup = new ButtonGroup();
        for (int i = 0; i < PALETTE.length; i++) {
            final String colorName = COLOR_NAMES[i];
            JToggleButton swatch = colorButton(PALETTE[i], COLOR_LABELS[i]);
            colorGroup.add(swatch);
            if (i == 0) {
                swatch.setSelected(true);
            }
            swatch.addActionListener(e -> {
                canvas.setColorName(colorName);
                penButton.setSelected(true);
                updateToolSelection(penButton, true);
                updateToolSelection(eraserButton, false);
                updateColorSelection(colors, swatch);
            });
            colors.add(swatch);
        }
        updateColorSelection(colors, (JToggleButton) colors.getComponent(0));
        colors.setMaximumSize(new Dimension(Integer.MAX_VALUE, 116));
        colors.setAlignmentX(Component.LEFT_ALIGNMENT);
        dock.add(colors);
        dock.add(verticalGap(22));

        dock.add(sectionLabel("線の太さ"));
        dock.add(verticalGap(8));
        JPanel sizes = new JPanel(new GridLayout(1, 3, 6, 0));
        sizes.setOpaque(false);
        ButtonGroup sizeGroup = new ButtonGroup();
        JToggleButton thinButton = addSizeButton(sizes, sizeGroup, canvas, "細", 3.0f, false);
        JToggleButton mediumButton = addSizeButton(sizes, sizeGroup, canvas, "中", 7.0f, true);
        JToggleButton thickButton = addSizeButton(sizes, sizeGroup, canvas, "太", 15.0f, false);
        canvas.putClientProperty("thinButton", thinButton);
        canvas.putClientProperty("mediumButton", mediumButton);
        canvas.putClientProperty("thickButton", thickButton);
        sizes.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        sizes.setAlignmentX(Component.LEFT_ALIGNMENT);
        dock.add(sizes);
        dock.add(javax.swing.Box.createVerticalGlue());

        JButton clearButton = UITheme.secondaryButton("ボードを全消し");
        clearButton.setForeground(UITheme.DANGER);
        clearButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        clearButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        UITheme.setAccessibleText(clearButton, "ボードを全消し", "全員のキャンバスを白紙に戻します");
        clearButton.addActionListener(e -> confirmClear(clearButton));
        dock.add(clearButton);
        return dock;
    }

    private static JPanel buildFooter(boolean connected) {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(UITheme.SURFACE);
        footer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, UITheme.BORDER),
                BorderFactory.createEmptyBorder(9, 16, 9, 16)));
        JLabel help = new JLabel("キャンバス上をドラッグして描く   ·   P ペン   E 消しゴム   1〜3 線の太さ");
        help.setForeground(UITheme.TEXT_MUTED);
        footer.add(help, BorderLayout.WEST);
        JLabel sync = new JLabel(connected ? "描画はルーム全員に同期されます" : "接続すると描画が共有されます");
        sync.setForeground(UITheme.TEXT_MUTED);
        footer.add(sync, BorderLayout.EAST);
        return footer;
    }

    private static void installShortcuts(JComponent root, DrawPanel canvas) {
        bind(root, "P", "pen", () -> clickStoredButton(canvas, "penButton"));
        bind(root, "E", "eraser", () -> clickStoredButton(canvas, "eraserButton"));
        bind(root, "1", "thin", () -> clickStoredButton(canvas, "thinButton"));
        bind(root, "2", "medium", () -> clickStoredButton(canvas, "mediumButton"));
        bind(root, "3", "thick", () -> clickStoredButton(canvas, "thickButton"));
    }

    private static void clickStoredButton(DrawPanel canvas, String property) {
        Object value = canvas.getClientProperty(property);
        if (value instanceof JToggleButton) {
            ((JToggleButton) value).doClick();
        }
    }

    private static void bind(JComponent root, String key, String name, Runnable action) {
        root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(key), name);
        root.getActionMap().put(name, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                action.run();
            }
        });
    }

    private static JToggleButton toolButton(String text, String description) {
        JToggleButton button = new JToggleButton(text);
        button.setFont(UITheme.LABEL);
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setFocusPainted(true);
        button.setOpaque(true);
        button.setBackground(UITheme.SURFACE);
        button.setForeground(UITheme.TEXT);
        button.setBorder(UITheme.compoundBorder(UITheme.BORDER, 10, 11));
        button.setAlignmentX(Component.LEFT_ALIGNMENT);
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        UITheme.setAccessibleText(button, text, description);
        return button;
    }

    private static void updateToolSelection(JToggleButton button, boolean selected) {
        button.setBackground(selected ? UITheme.ACCENT_SOFT : UITheme.SURFACE);
        button.setForeground(selected ? UITheme.ACCENT : UITheme.TEXT);
        button.setBorder(UITheme.compoundBorder(selected ? UITheme.ACCENT : UITheme.BORDER, 10, 11));
    }

    private static JToggleButton colorButton(Color color, String label) {
        JToggleButton button = new JToggleButton(label);
        button.putClientProperty("colorLabel", label);
        button.setFont(UITheme.LABEL);
        button.setBackground(color);
        button.setForeground(contrastColor(color));
        button.setFocusPainted(true);
        button.setOpaque(true);
        button.setBorder(UITheme.compoundBorder(UITheme.BORDER, 8, 6));
        UITheme.setAccessibleText(button, label + "色", label + "色のペンを選択");
        return button;
    }

    private static void updateColorSelection(JPanel panel, JToggleButton selected) {
        for (Component component : panel.getComponents()) {
            JToggleButton button = (JToggleButton) component;
            String label = String.valueOf(button.getClientProperty("colorLabel"));
            boolean isSelected = button == selected;
            button.setText((isSelected ? "✓ " : "") + label);
            button.setBorder(UITheme.compoundBorder(isSelected ? UITheme.FOCUS : UITheme.BORDER, 8, 6));
        }
    }

    private static Color contrastColor(Color color) {
        double luminance = 0.2126 * color.getRed() + 0.7152 * color.getGreen() + 0.0722 * color.getBlue();
        return luminance < 145 ? UITheme.SURFACE : UITheme.TEXT;
    }

    private static JToggleButton addSizeButton(JPanel panel, ButtonGroup group, DrawPanel canvas,
            String label, float width, boolean selected) {
        JToggleButton button = new JToggleButton(label);
        button.setFont(UITheme.LABEL);
        button.setFocusPainted(true);
        button.setBackground(selected ? UITheme.ACCENT_SOFT : UITheme.SURFACE);
        button.setBorder(UITheme.compoundBorder(selected ? UITheme.ACCENT : UITheme.BORDER, 8, 4));
        button.setSelected(selected);
        button.addActionListener(e -> {
            canvas.setStrokeWidth(width);
            updateSizeSelection(panel, button);
        });
        UITheme.setAccessibleText(button, label + "い線", label + "い線を選択");
        group.add(button);
        panel.add(button);
        if (selected) {
            canvas.setStrokeWidth(width);
        }
        return button;
    }

    private static void updateSizeSelection(JPanel panel, JToggleButton selected) {
        for (Component component : panel.getComponents()) {
            JToggleButton button = (JToggleButton) component;
            boolean isSelected = button == selected;
            button.setBackground(isSelected ? UITheme.ACCENT_SOFT : UITheme.SURFACE);
            button.setForeground(isSelected ? UITheme.ACCENT : UITheme.TEXT);
            button.setBorder(UITheme.compoundBorder(isSelected ? UITheme.ACCENT : UITheme.BORDER, 8, 4));
        }
    }

    private static JLabel sectionLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(UITheme.LABEL);
        label.setForeground(UITheme.TEXT_MUTED);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private static Component verticalGap(int height) {
        return javax.swing.Box.createRigidArea(new Dimension(0, height));
    }

    private static void confirmClear(JButton button) {
        if (!Boolean.TRUE.equals(button.getClientProperty("confirming"))) {
            button.putClientProperty("confirming", true);
            button.setText("もう一度押して全消し");
            button.setBackground(new Color(249, 231, 231));
            Timer timer = new Timer(3000, e -> resetClearButton(button));
            timer.setRepeats(false);
            timer.start();
            return;
        }
        resetClearButton(button);
        DrawController.sendClear();
    }

    private static void resetClearButton(JButton button) {
        button.putClientProperty("confirming", false);
        button.setText("ボードを全消し");
        button.setBackground(UITheme.SURFACE);
    }

    private static int parsePort(String value) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException exception) {
            return 8080;
        }
    }
}
