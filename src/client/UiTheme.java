package client;

import java.awt.Color;
import java.awt.Font;

import javax.swing.UIManager;
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
    public static final Color GOLD = new Color(196, 149, 27);
    public static final Color SILVER = new Color(120, 124, 128);
    public static final Color BRONZE = new Color(158, 96, 45);

    public static final Font TITLE_FONT = new Font("SansSerif", Font.BOLD, 26);
    public static final Font HEADING_FONT = new Font("SansSerif", Font.BOLD, 16);

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
    }
}
