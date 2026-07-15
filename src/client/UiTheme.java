package client;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.plaf.FontUIResource;

/** アプリ全体で使う配色・フォントをまとめたもの。各画面はここから参照して統一感を出す。 */
public final class UiTheme {
    public static final Color PRIMARY = new Color(46, 125, 87);        // 森をイメージした深緑
    public static final Color PRIMARY_DARK = new Color(30, 90, 62);
    public static final Color PRIMARY_LIGHT = new Color(224, 240, 228);
    public static final Color DRAWER = new Color(230, 126, 34);        // Drawer強調用のオレンジ
    public static final Color GUESSER = new Color(41, 121, 197);       // Guesser強調用の青
    public static final Color DANGER = new Color(198, 40, 40);         // 不正解・警告
    public static final Color SUCCESS = new Color(39, 141, 84);        // 正解
    public static final Color BACKGROUND = new Color(247, 248, 241);
    public static final Color APP_BACKGROUND = new Color(244, 246, 243);
    public static final Color SURFACE = new Color(252, 253, 250);
    public static final Color SURFACE_MUTED = new Color(237, 241, 236);
    public static final Color TEXT = new Color(27, 35, 30);
    public static final Color TEXT_MUTED = new Color(87, 99, 91);
    public static final Color BORDER = new Color(207, 216, 209);
    public static final Color ACCENT = new Color(31, 112, 78);
    public static final Color ACCENT_SOFT = new Color(220, 239, 229);
    public static final Color FOCUS = new Color(23, 94, 164);
    public static final Color GOLD = new Color(196, 149, 27);
    public static final Color SILVER = new Color(120, 124, 128);
    public static final Color BRONZE = new Color(158, 96, 45);

    public static final Font TITLE_FONT = new Font("SansSerif", Font.BOLD, 26);
    public static final Font HEADING_FONT = new Font("SansSerif", Font.BOLD, 16);
    public static final Font BODY = new Font(Font.SANS_SERIF, Font.PLAIN, 14);
    public static final Font LABEL = new Font(Font.SANS_SERIF, Font.BOLD, 13);
    public static final Font TITLE = new Font(Font.SANS_SERIF, Font.BOLD, 20);

    private UiTheme() {
    }

    /**
     * ボタン・ラベル・テキストなど、Swingの全コンポーネントが使う基準フォントを
     * 一括で少し大きくする。文字が小さくて見づらいという問題に、画面ごとに
     * フォントを個別指定しなくても効くようにするための一括設定。
     * UIを組み立てる前(main()の最初)に一度だけ呼ぶ。
     */
    public static void installGlobalDefaults() {
        for (Object key : UIManager.getLookAndFeelDefaults().keySet().toArray()) {
            Object value = UIManager.get(key);
            if (value instanceof FontUIResource) {
                FontUIResource font = (FontUIResource) value;
                UIManager.put(key, new FontUIResource(font.deriveFont(font.getSize2D() + 3f)));
            }
        }
        UIManager.put("Panel.background", APP_BACKGROUND);
        UIManager.put("Label.foreground", TEXT);
        UIManager.put("List.selectionBackground", ACCENT_SOFT);
        UIManager.put("List.selectionForeground", TEXT);
        UIManager.put("TextField.selectionBackground", ACCENT_SOFT);
        UIManager.put("Focus.color", FOCUS);
    }

    public static void install() {
        installGlobalDefaults();
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
