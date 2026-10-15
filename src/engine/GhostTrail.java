package engine;

import java.awt.Color;
import java.util.ArrayDeque;
import java.util.Iterator;

import engine.DrawManager.SpriteType;
import entity.Ship;

/**
 * Ghost Trail ship effect (shop item "effect_ghost_trail"). While the
 * player's ship moves, a few fading copies of it follow behind. When the
 * ship stops, the copies disappear. Look only, no gameplay change.
 * AUTHORED BY: VFX TEAM (Effection)
 */
public final class GhostTrail {

	/** How many past positions are remembered (one per frame). */
	private static final int HISTORY = 17;
    /** How many frames back each ghost is drawn, nearest first. */
	private static final int[] GHOST_FRAMES = { 4, 8, 12, 16 };
	/** Opacity of each ghost (0-255), nearest first. */
	private static final int[] GHOST_ALPHA = { 150, 115, 80, 45 };
	/** Ghost colours, used in turn. */
	private static final Color[] GHOST_COLORS = {
			new Color(0, 220, 255), new Color(160, 90, 255) };
	/** Pixels the ship must move over the history to count as moving. */
	private static final int MIN_MOVE = 4;

	/** Past ship positions, newest first: {x, y}. */
	private final ArrayDeque<int[]> history = new ArrayDeque<int[]>();

	/**
	 * Remembers where the ship is this frame. Call once per frame.
	 *
	 * @param x
	 *            Ship x.
	 * @param y
	 *            Ship y.
	 */
	public void record(final int x, final int y) {
		this.history.addFirst(new int[] { x, y });
		if (this.history.size() > HISTORY)
			this.history.removeLast();
	}

	/** @return True if the ship moved enough lately to show ghosts. */
	public boolean isMoving() {
		if (this.history.size() < 2)
			return false;
		return Math.abs(this.history.getFirst()[0]
				- this.history.getLast()[0]) >= MIN_MOVE;
	}

    /**
	 * Records the ship's position, then draws its ghosts behind it.
	 * Call every frame, just before the ship is drawn.
	 *
	 * @param drawManager
	 *            Draw manager.
	 * @param ship
	 *            Player ship.
	 */
	public void draw(final DrawManager drawManager, final Ship ship) {
		record(ship.getPositionX(), ship.getPositionY());
		if (!isMoving())
			return;
		// Farthest ghost first, so nearer ghosts are drawn on top.
		for (int k = GHOST_FRAMES.length - 1; k >= 0; k--) {
			int[] pos = positionFramesAgo(GHOST_FRAMES[k]);
			if (pos == null)
				continue;
			Color base = GHOST_COLORS[k % GHOST_COLORS.length];
			drawManager.drawSprite(ship.getSpriteType(), pos[0], pos[1],
					new Color(base.getRed(), base.getGreen(),
							base.getBlue(), GHOST_ALPHA[k]));
		}
	}

	/**
	 * @param frames
	 *            Frames back (0 = now).
	 * @return Ship position that many frames ago, or null if too new.
	 */
	private int[] positionFramesAgo(final int frames) {
		if (frames >= this.history.size())
			return null;
		Iterator<int[]> it = this.history.iterator();
		for (int i = 0; i < frames; i++)
			it.next();
		return it.next();
	}
    
	/**
	 * Shop preview: the ship slides left and right with its ghosts behind.
	 *
	 * @param drawManager
	 *            Draw manager.
	 * @param sprite
	 *            Ship sprite to show.
	 * @param shipColor
	 *            Ship colour (selected skin).
	 * @param centerX
	 *            Horizontal centre of the preview.
	 * @param top
	 *            Top of the preview.
	 */
	public static void drawPreview(final DrawManager drawManager,
			final SpriteType sprite, final Color shipColor,
			final int centerX, final int top) {
		final int pixel = 3;
		final int frameMs = 16;
		long now = System.currentTimeMillis();
		int[] size = drawManager.getSpriteSize(sprite);
		int shipY = top + 20;
		for (int k = GHOST_FRAMES.length - 1; k >= 0; k--) {
			Color base = GHOST_COLORS[k % GHOST_COLORS.length];
			drawManager.drawSpriteScaled(sprite, previewX(centerX,
					size[0] * pixel, now - GHOST_FRAMES[k] * frameMs), shipY,
					pixel, new Color(base.getRed(), base.getGreen(),
							base.getBlue(), GHOST_ALPHA[k]));
		}
		drawManager.drawSpriteScaled(sprite, previewX(centerX,
				size[0] * pixel, now), shipY, pixel, shipColor);
	}

	/**
	 * @return Left edge of the preview ship at a given time: it slides
	 *         36 px each way and pauses at the ends.
	 */
	private static int previewX(final int centerX, final int width,
			final long time) {
		double phase = (time % 2400) / 2400.0 * 2 * Math.PI;
		double s = Math.max(-1, Math.min(1, Math.sin(phase) * 1.4));
		return centerX - width / 2 + (int) Math.round(s * 36);
	}
}
