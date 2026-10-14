package engine;

import java.awt.Color;
import java.awt.Graphics;

/**
 * Comet trail behind a player bullet: soft glow plus fading orange
 * blocks below it. Look only.
 * AUTHORED BY: VFX TEAM (Effection)
 */
public final class BulletTrail {

    /** Trail colours, hot to cool. */
    private static final Color[] COLORS = {
			new Color(255, 240, 150), new Color(255, 205, 60),
			new Color(255, 165, 30), new Color(255, 135, 20),
			new Color(250, 105, 10), new Color(230, 75, 10),
			new Color(190, 45, 5) };
    /** Height of one trail block in pixels. */
    private static final int SEGMENT = 4;
    /** Glow colour around the bullet. */
    private static final Color GLOW = new Color(255, 150, 40, 90);

    /** Not instantiable. */
    private BulletTrail() {
    }

    /**
     * Draws the trail. Call before the bullet so the bullet stays on top.
     *
     * @param g      Graphics to draw on.
     * @param x      Bullet x.
     * @param y      Bullet y.
     * @param width  Bullet width.
     * @param height Bullet height.
     */
    public static void draw(final Graphics g, final int x, final int y,
                            final int width, final int height) {
        if (g == null)
            return;
        g.setColor(GLOW);
        g.fillRect(x - 2, y - 2, width + 4, height + 4);
        int centerX = x + width / 2;
        for (int i = 1; i <= COLORS.length; i++) {
            Color base = COLORS[i - 1];
            int alpha = (int) (200 * (1f - i / (COLORS.length + 1f)));
            int w = Math.max(2, width - i);
            g.setColor(new Color(base.getRed(), base.getGreen(),
                    base.getBlue(), alpha));
            g.fillRect(centerX - w / 2, y + height + (i - 1) * SEGMENT, w,
                    SEGMENT);
        }
    }
}