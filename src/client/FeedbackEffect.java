package client;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.Random;

import javax.swing.JComponent;
import javax.swing.Timer;

/**
 * 状態変化を短く伝える、レイアウトを動かさない軽量エフェクト。
 * -Ddrawingforest.reduceMotion=true で全アニメーションを無効化できる。
 */
public final class FeedbackEffect {
    public enum Type { SUCCESS, ERROR, INFO, SCORE, ROUND_TRANSITION, CELEBRATION }

    private static final int FRAME_MILLIS = 16;
    private static final int FEEDBACK_DURATION = 420;
    private static final int SUCCESS_DURATION = 1_400;
    private static final int ROUND_TRANSITION_DURATION = 2_800;
    private static final int CELEBRATION_DURATION = 900;
    private static final int PARTICLE_COUNT = 30;

    private final JComponent host;
    private final Timer timer;
    private final Particle[] particles = new Particle[PARTICLE_COUNT];
    private Type type;
    private String title = "";
    private String subtitle = "";
    private long startedAt;
    private int durationMillis;

    public FeedbackEffect(JComponent host) {
        this.host = host;
        Random random = new Random(20260716L);
        Color[] colors = {
            UiTheme.ACCENT, UiTheme.GUESSER, UiTheme.DRAWER,
            new Color(226, 174, 35), new Color(121, 76, 167)
        };
        for (int i = 0; i < particles.length; i++) {
            particles[i] = new Particle(random.nextDouble(), random.nextDouble(),
                    0.7 + random.nextDouble() * 1.1, random.nextDouble() * Math.PI * 2,
                    5 + random.nextInt(5), colors[i % colors.length]);
        }
        timer = new Timer(FRAME_MILLIS, event -> tick());
        timer.setCoalesce(true);
    }

    public void play(Type requestedType) {
        play(requestedType, "", "");
    }

    public void play(Type requestedType, String requestedTitle, String requestedSubtitle) {
        if (prefersReducedMotion()) {
            type = null;
            timer.stop();
            host.repaint();
            return;
        }
        type = requestedType;
        title = requestedTitle == null ? "" : requestedTitle;
        subtitle = requestedSubtitle == null ? "" : requestedSubtitle;
        durationMillis = durationFor(requestedType);
        startedAt = System.nanoTime();
        if (!timer.isRunning()) {
            timer.start();
        }
        host.repaint();
    }

    public void stop() {
        timer.stop();
        type = null;
        host.repaint();
    }

    public boolean isRunning() {
        return type != null && timer.isRunning();
    }

    public void paint(Graphics2D graphics, int width, int height) {
        if (type == null || width <= 0 || height <= 0) {
            return;
        }
        double progress = progress();
        if (progress >= 1.0) {
            return;
        }
        double eased = 1.0 - Math.pow(1.0 - progress, 4.0);
        double fade = trailingFade(progress, type == Type.SUCCESS ? 0.72 : 0.58);

        Graphics2D g2 = (Graphics2D) graphics.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        switch (type) {
            case SUCCESS:
                paintFlash(g2, width, height, UiTheme.SUCCESS, fade, 52);
                paintConfetti(g2, width, height, eased, fade, false);
                paintMessage(g2, width, height, title, subtitle, UiTheme.SUCCESS, fade, false);
                break;
            case CELEBRATION:
                paintConfetti(g2, width, height, eased, fade, true);
                break;
            case ERROR:
                paintOutline(g2, width, height, UiTheme.DANGER, fade);
                break;
            case INFO:
                paintOutline(g2, width, height, UiTheme.GUESSER, fade);
                break;
            case SCORE:
                paintScoreSweep(g2, width, height, eased, fade);
                break;
            case ROUND_TRANSITION:
                paintRoundTransition(g2, width, height, progress, fade);
                break;
            default:
                break;
        }
        g2.dispose();
    }

    public static boolean prefersReducedMotion() {
        if (Boolean.getBoolean("drawingforest.reduceMotion")) {
            return true;
        }
        String environment = System.getenv("DRAWING_FOREST_MOTION");
        return "reduce".equalsIgnoreCase(environment) || "off".equalsIgnoreCase(environment);
    }

    private void tick() {
        if (type == null || progress() >= 1.0) {
            timer.stop();
            type = null;
        }
        host.repaint();
    }

    private double progress() {
        double elapsedMillis = (System.nanoTime() - startedAt) / 1_000_000.0;
        return Math.min(1.0, Math.max(0.0, elapsedMillis / durationMillis));
    }

    private int durationFor(Type requestedType) {
        if (requestedType == Type.SUCCESS) return SUCCESS_DURATION;
        if (requestedType == Type.ROUND_TRANSITION) return ROUND_TRANSITION_DURATION;
        if (requestedType == Type.CELEBRATION) return CELEBRATION_DURATION;
        return FEEDBACK_DURATION;
    }

    private double trailingFade(double progress, double holdUntil) {
        if (progress <= holdUntil) return 1.0;
        return Math.max(0.0, (1.0 - progress) / (1.0 - holdUntil));
    }

    private void paintFlash(Graphics2D g2, int width, int height, Color color, double fade, int maxAlpha) {
        g2.setColor(withAlpha(color, (int) Math.round(maxAlpha * fade)));
        g2.fillRoundRect(1, 1, width - 2, height - 2, 14, 14);
    }

    private void paintOutline(Graphics2D g2, int width, int height, Color color, double fade) {
        g2.setStroke(new BasicStroke(3f));
        g2.setColor(withAlpha(color, (int) Math.round(190 * fade)));
        g2.drawRoundRect(2, 2, Math.max(0, width - 5), Math.max(0, height - 5), 14, 14);
    }

    private void paintScoreSweep(Graphics2D g2, int width, int height, double eased, double fade) {
        int sweepWidth = (int) Math.round(width * 0.55 * (1.0 - eased));
        int x = Math.max(0, width - sweepWidth);
        g2.setColor(withAlpha(UiTheme.ACCENT, (int) Math.round(60 * fade)));
        g2.fillRect(x, Math.max(0, height - 110), sweepWidth, Math.min(110, height));
        paintOutline(g2, width, height, UiTheme.ACCENT, fade * 0.7);
    }

    private void paintRoundTransition(Graphics2D g2, int width, int height, double progress, double fade) {
        g2.setColor(new Color(20, 32, 25, (int) Math.round(112 * fade)));
        g2.fillRect(0, 0, width, height);
        paintMessage(g2, width, height, title, subtitle, UiTheme.ACCENT, fade, true);

        int barWidth = Math.min(280, Math.max(120, width - 120));
        int barX = (width - barWidth) / 2;
        int barY = height / 2 + 68;
        g2.setColor(withAlpha(UiTheme.BORDER, (int) Math.round(210 * fade)));
        g2.fillRoundRect(barX, barY, barWidth, 5, 5, 5);
        g2.setColor(withAlpha(UiTheme.ACCENT, (int) Math.round(235 * fade)));
        g2.fillRoundRect(barX, barY, (int) Math.round(barWidth * (1.0 - progress)), 5, 5, 5);
    }

    private void paintMessage(Graphics2D g2, int width, int height, String heading, String detail,
            Color accent, double fade, boolean elevated) {
        if ((heading == null || heading.isEmpty()) && (detail == null || detail.isEmpty())) return;

        int cardWidth = Math.min(410, Math.max(250, width - 48));
        int cardHeight = detail == null || detail.isEmpty() ? 78 : 104;
        int x = (width - cardWidth) / 2;
        int y = Math.max(18, height / 2 - cardHeight / 2 - (elevated ? 18 : 0));
        g2.setColor(new Color(252, 253, 250, (int) Math.round(244 * fade)));
        g2.fillRoundRect(x, y, cardWidth, cardHeight, 20, 20);
        g2.setStroke(new BasicStroke(2f));
        g2.setColor(withAlpha(accent, (int) Math.round(220 * fade)));
        g2.drawRoundRect(x, y, cardWidth, cardHeight, 20, 20);

        g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 24));
        g2.setColor(withAlpha(accent, (int) Math.round(255 * fade)));
        drawCentered(g2, heading, width / 2, y + 39);
        if (detail != null && !detail.isEmpty()) {
            g2.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
            g2.setColor(withAlpha(UiTheme.TEXT, (int) Math.round(230 * fade)));
            drawCentered(g2, detail, width / 2, y + 70);
        }
    }

    private void drawCentered(Graphics2D g2, String text, int centerX, int baselineY) {
        FontMetrics metrics = g2.getFontMetrics();
        g2.drawString(text, centerX - metrics.stringWidth(text) / 2, baselineY);
    }

    private void paintConfetti(Graphics2D g2, int width, int height,
            double eased, double fade, boolean fullScreen) {
        int travel = fullScreen ? Math.max(120, height) : Math.max(90, height / 2);
        for (Particle particle : particles) {
            double local = Math.max(0.0, Math.min(1.0, eased * particle.speed));
            int x = (int) Math.round(particle.x * width + Math.sin(particle.phase + local * 5) * 22);
            int startY = fullScreen ? -18 : Math.max(0, height / 4 - 28);
            int y = startY + (int) Math.round(local * travel + particle.offset * 34);
            int alpha = (int) Math.round(230 * fade);
            g2.setColor(withAlpha(particle.color, alpha));
            int size = particle.size;
            g2.fillRoundRect(x, y, size, Math.max(3, size / 2), 2, 2);
        }
    }

    private Color withAlpha(Color color, int alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), Math.max(0, Math.min(255, alpha)));
    }

    private static final class Particle {
        private final double x;
        private final double offset;
        private final double speed;
        private final double phase;
        private final int size;
        private final Color color;

        private Particle(double x, double offset, double speed, double phase, int size, Color color) {
            this.x = x;
            this.offset = offset;
            this.speed = speed;
            this.phase = phase;
            this.size = size;
            this.color = color;
        }
    }
}
