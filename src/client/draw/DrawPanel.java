package client.draw;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

import javax.swing.JPanel;

import client.ui.UITheme;

/**
 * フリーボード型の共同描画キャンバス。
 * 描画データは固定サイズの論理キャンバスに保持し、ウインドウに合わせて拡縮表示する。
 */
public class DrawPanel extends JPanel {
    private static final int CANVAS_W = 1100;
    private static final int CANVAS_H = 680;
    private static final int BOARD_MARGIN = 28;
    private static final Color CANVAS_COLOR = new Color(253, 253, 250);

    private final BufferedImage canvas;
    private final Graphics2D canvasGraphics;
    private Point previousCanvasPoint;
    private Point hoverPoint;
    private String currentColor = "BLACK";
    private String lastInkColor = "BLACK";
    private float strokeWidth = 4.0f;

    public DrawPanel() {
        setPreferredSize(new Dimension(920, 650));
        setMinimumSize(new Dimension(520, 360));
        setBackground(UITheme.APP_BACKGROUND);
        setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
        setFocusable(true);
        getAccessibleContext().setAccessibleName("お絵描きキャンバス");
        getAccessibleContext().setAccessibleDescription("マウスをドラッグして絵を描きます");

        canvas = new BufferedImage(CANVAS_W, CANVAS_H, BufferedImage.TYPE_INT_RGB);
        canvasGraphics = canvas.createGraphics();
        canvasGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        clearCanvas();

        MouseAdapter mouse = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent event) {
                requestFocusInWindow();
                previousCanvasPoint = toCanvasPoint(event.getPoint());
            }

            @Override
            public void mouseDragged(MouseEvent event) {
                Point current = toCanvasPoint(event.getPoint());
                hoverPoint = event.getPoint();
                if (previousCanvasPoint != null && current != null) {
                    drawLine(previousCanvasPoint.x, previousCanvasPoint.y, current.x, current.y,
                            currentColor, strokeWidth);
                    DrawController.sendLine(previousCanvasPoint.x, previousCanvasPoint.y,
                            current.x, current.y, currentColor, strokeWidth);
                }
                previousCanvasPoint = current;
                repaint();
            }

            @Override
            public void mouseMoved(MouseEvent event) {
                hoverPoint = event.getPoint();
                repaint();
            }

            @Override
            public void mouseReleased(MouseEvent event) {
                previousCanvasPoint = null;
            }

            @Override
            public void mouseExited(MouseEvent event) {
                previousCanvasPoint = null;
                hoverPoint = null;
                repaint();
            }
        };
        addMouseListener(mouse);
        addMouseMotionListener(mouse);
    }

    private void drawLine(int x1, int y1, int x2, int y2, String colorName, float width) {
        canvasGraphics.setStroke(new BasicStroke(width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        canvasGraphics.setColor(toColor(colorName));
        canvasGraphics.drawLine(x1, y1, x2, y2);
        repaint();
    }

    public void drawRemoteLine(int x1, int y1, int x2, int y2, String colorName, float width) {
        drawLine(x1, y1, x2, y2, colorName, width);
    }

    /** 旧形式の描画データとの互換用。 */
    public void drawRemoteLine(int x1, int y1, int x2, int y2, String colorName) {
        drawRemoteLine(x1, y1, x2, y2, colorName, 4.0f);
    }

    public void setColorName(String colorName) {
        currentColor = colorName == null ? "BLACK" : colorName;
        if (!"WHITE".equalsIgnoreCase(currentColor)) {
            lastInkColor = currentColor;
        }
        repaint();
    }

    public void usePen() {
        currentColor = lastInkColor;
        repaint();
    }

    public String getColorName() {
        return currentColor;
    }

    public void setStrokeWidth(float width) {
        strokeWidth = Math.max(2.0f, Math.min(24.0f, width));
        repaint();
    }

    public float getStrokeWidth() {
        return strokeWidth;
    }

    public void clearCanvas() {
        canvasGraphics.setColor(CANVAS_COLOR);
        canvasGraphics.fillRect(0, 0, CANVAS_W, CANVAS_H);
        repaint();
    }

    private Color toColor(String name) {
        String normalized = name == null ? "" : name.trim().toUpperCase();
        switch (normalized) {
            case "RED": return new Color(204, 57, 63);
            case "BLUE": return new Color(32, 101, 181);
            case "GREEN": return new Color(36, 132, 84);
            case "YELLOW": return new Color(226, 174, 35);
            case "PURPLE": return new Color(121, 76, 167);
            case "WHITE": return CANVAS_COLOR;
            case "BLACK":
            default: return new Color(28, 34, 30);
        }
    }

    private Rectangle boardBounds() {
        int availableWidth = Math.max(1, getWidth() - BOARD_MARGIN * 2);
        int availableHeight = Math.max(1, getHeight() - BOARD_MARGIN * 2);
        double scale = Math.min((double) availableWidth / CANVAS_W, (double) availableHeight / CANVAS_H);
        int width = Math.max(1, (int) Math.round(CANVAS_W * scale));
        int height = Math.max(1, (int) Math.round(CANVAS_H * scale));
        return new Rectangle((getWidth() - width) / 2, (getHeight() - height) / 2, width, height);
    }

    private Point toCanvasPoint(Point panelPoint) {
        Rectangle board = boardBounds();
        if (!board.contains(panelPoint)) {
            return null;
        }
        double scaleX = (double) CANVAS_W / board.width;
        double scaleY = (double) CANVAS_H / board.height;
        int x = (int) Math.round((panelPoint.x - board.x) * scaleX);
        int y = (int) Math.round((panelPoint.y - board.y) * scaleY);
        return new Point(Math.min(CANVAS_W - 1, x), Math.min(CANVAS_H - 1, y));
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g2 = (Graphics2D) graphics.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        Rectangle board = boardBounds();
        g2.setColor(new Color(27, 35, 30, 22));
        g2.fillRoundRect(board.x + 3, board.y + 7, board.width, board.height, 12, 12);
        g2.drawImage(canvas, board.x, board.y, board.width, board.height, null);
        g2.setColor(UITheme.BORDER);
        g2.drawRoundRect(board.x, board.y, board.width, board.height, 10, 10);

        if (hoverPoint != null && board.contains(hoverPoint)) {
            double visualScale = (double) board.width / CANVAS_W;
            int diameter = Math.max(6, (int) Math.round(strokeWidth * visualScale));
            int radius = diameter / 2;
            g2.setColor(toColor(currentColor));
            g2.setStroke(new BasicStroke(1.4f));
            g2.drawOval(hoverPoint.x - radius, hoverPoint.y - radius, diameter, diameter);
        }
        g2.dispose();
    }
}
