package client;

import java.awt.BasicStroke;
import java.awt.Color;
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
    public enum Type { SUCCESS, ERROR, INFO, SCORE, CELEBRATION }

    private static final int FRAME_MILLIS = 16;
    private static final int FEEDBACK_DURATION = 420;
    private static final int CELEBRATION_DURATION = 500;
    private static final int PARTICLE_COUNT = 30;

    private final JComponent host;
    private final Timer timer;
    private final Particle[] particles = new Particle[PARTICLE_COUNT];
    private Type type;
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
        if (prefersReducedMotion()) {
            type = null;
            timer.stop();
            host.repaint();
            return;
        }
        type = requestedType;
        durationMillis = requestedType == Type.CELEBRATION ? CELEBRATION_DURATION : FEEDBACK_DURATION;
        startedAt = System.nanoTime();
        if (!timer.isRunning()) {
            timer.start();
        }
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
        double fade = 1.0 - progress;

        Graphics2D g2 = (Graphics2D) graphics.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        switch (type) {
            case SUCCESS:
                paintFlash(g2, width, height, UiTheme.SUCCESS, fade, 52);
                paintConfetti(g2, width, height, eased, fade, false);
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
