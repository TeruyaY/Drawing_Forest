package client.ui;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.UIManager;
import javax.swing.border.Border;

/** アプリ全体で共有する、読みやすさを優先した軽量なSwingテーマ。 */
public final class UITheme {
    public static final Color APP_BACKGROUND = new Color(244, 246, 243);
    public static final Color SURFACE = new Color(252, 253, 250);
    public static final Color SURFACE_MUTED = new Color(237, 241, 236);
    public static final Color TEXT = new Color(27, 35, 30);
    public static final Color TEXT_MUTED = new Color(87, 99, 91);
    public static final Color BORDER = new Color(207, 216, 209);
    public static final Color ACCENT = new Color(31, 112, 78);
    public static final Color ACCENT_HOVER = new Color(24, 91, 63);
    public static final Color ACCENT_SOFT = new Color(220, 239, 229);
    public static final Color DANGER = new Color(174, 52, 58);
    public static final Color FOCUS = new Color(23, 94, 164);

    public static final Font BODY = new Font(Font.SANS_SERIF, Font.PLAIN, 14);
    public static final Font LABEL = new Font(Font.SANS_SERIF, Font.BOLD, 13);
    public static final Font TITLE = new Font(Font.SANS_SERIF, Font.BOLD, 20);

    private UITheme() {
    }

    public static void install() {
        UIManager.put("Panel.background", APP_BACKGROUND);
        UIManager.put("Label.foreground", TEXT);
        UIManager.put("Label.font", BODY);
        UIManager.put("Button.font", LABEL);
        UIManager.put("TextField.font", BODY);
        UIManager.put("List.font", BODY);
        UIManager.put("List.selectionBackground", ACCENT_SOFT);
        UIManager.put("List.selectionForeground", TEXT);
        UIManager.put("TextField.selectionBackground", ACCENT_SOFT);
        UIManager.put("Focus.color", FOCUS);
    }

    public static JButton primaryButton(String text) {
        JButton button = baseButton(text);
        button.setBackground(ACCENT);
        button.setForeground(SURFACE);
        button.setBorder(compoundBorder(ACCENT, 10, 16));
        return button;
    }

    public static JButton secondaryButton(String text) {
        JButton button = baseButton(text);
        button.setBackground(SURFACE);
        button.setForeground(TEXT);
        button.setBorder(compoundBorder(BORDER, 9, 14));
        return button;
    }

    public static JButton baseButton(String text) {
        JButton button = new JButton(text);
        button.setFont(LABEL);
        button.setFocusPainted(true);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setOpaque(true);
        return button;
    }

    public static Border panelBorder(int vertical, int horizontal) {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                BorderFactory.createEmptyBorder(vertical, horizontal, vertical, horizontal));
    }

    public static Border compoundBorder(Color color, int vertical, int horizontal) {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(color),
                BorderFactory.createEmptyBorder(vertical, horizontal, vertical, horizontal));
    }

    public static void setAccessibleText(JComponent component, String name, String description) {
        component.getAccessibleContext().setAccessibleName(name);
        component.getAccessibleContext().setAccessibleDescription(description);
        component.setToolTipText(description);
    }
}
