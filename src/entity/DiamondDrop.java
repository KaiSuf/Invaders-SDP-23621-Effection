package entity;

import java.awt.Color;

/**
 * AUTHORED BY: VFX TEAM (Effection)
 *
 * A diamond dropped by a shot bonus meteor. It keeps part of the meteor's sideways speed, so it drifts in an arc (bouncing off the screen edges)
 * while it falls, and the player must move to catch it with their ship.
 * A diamond that falls off the bottom of the screen is lost. Caught diamonds are added to the run's pending diamonds by the game screen
 *
 * After being caught, the drop stays alive for a short time so a "+N" popup can float up from where it was caught, then reports itself finished so the screen can remove it.
 */
public class DiamondDrop extends Entity {

	/** Size of the diamond in pixels. */
	public static final int SIZE = 12;
	/** Time the "+N" popup shows after the diamond is caught, in ms. */
	public static final int POPUP_DURATION = 800;
	/** Falling speed in pixels per millisecond (about 100 px a second). */
	private static final float FALL_SPEED = 0.1f;
	/** Part of the meteor's sideways speed the diamond keeps. */
	public static final float DRIFT = 0.35f;
	/** Longest time step used for one move, so a lag spike can't teleport it. */
	private static final long MAX_STEP = 50;

	/** Diamonds given when the drop is caught. */
	private final int value;
	/** Exact horizontal position, kept as a float for smooth movement. */
	private float exactX;
	/** Exact vertical position, kept as a float for smooth movement. */
	private float exactY;
	/** Sideways speed in pixels per millisecond (sign = direction). */
	private float speedX;
	/** Screen width, so the diamond bounces off the side edges. */
	private final int screenWidth;
	/** Time of the last movement update. */
	private long lastUpdate;
	/** Time the drop was created, used for its twinkle. */
	private final long createdAt;
	/** True once the player's ship has caught the drop. */
	private boolean caught;
	/** Time the drop was caught. */
	private long caughtAt;

	/**
	 * Creates a diamond drop centred on a point.
	 *
	 * @param centerX
	 *            Horizontal centre where the drop appears.
	 * @param centerY
	 *            Vertical centre where the drop appears.
	 * @param value
	 *            Diamonds given when the drop is caught.
	 * @param meteorSpeedX
	 *            Sideways speed of the meteor that dropped it, in pixels
	 *            per millisecond; the diamond keeps {@link #DRIFT} of it.
	 * @param screenWidth
	 *            Screen width, so the diamond stays on screen.
	 */
	public DiamondDrop(final int centerX, final int centerY, final int value,
			final float meteorSpeedX, final int screenWidth) {
		super(centerX - SIZE / 2, centerY - SIZE / 2, SIZE, SIZE, Color.CYAN);
		this.value = value;
		this.exactX = this.positionX;
		this.exactY = this.positionY;
		this.speedX = meteorSpeedX * DRIFT;
		this.screenWidth = screenWidth;
		this.createdAt = System.currentTimeMillis();
		this.lastUpdate = this.createdAt;
	}

	/** Makes the drop fall while it has not been caught. Call once per frame. */
	public final void update() {
		long now = System.currentTimeMillis();
		long step = Math.min(MAX_STEP, now - this.lastUpdate);
		this.lastUpdate = now;
		if (this.caught)
			return;
		this.exactY += FALL_SPEED * step;
		this.exactX += this.speedX * step;
		// Bounce off the side edges so it never leaves the screen.
		if (this.exactX < 0) {
			this.exactX = -this.exactX;
			this.speedX = -this.speedX;
		} else if (this.exactX > this.screenWidth - SIZE) {
			this.exactX = 2 * (this.screenWidth - SIZE) - this.exactX;
			this.speedX = -this.speedX;
		}
		this.positionX = Math.round(this.exactX);
		this.positionY = Math.round(this.exactY);
	}

	/** Marks the drop as caught and starts its "+N" popup. */
	public final void catchIt() {
		if (this.caught)
			return;
		this.caught = true;
		this.caughtAt = System.currentTimeMillis();
	}

	/** @return True once the drop has been caught. */
	public final boolean isCaught() {
		return this.caught;
	}

	/** @return True when the drop was caught and its popup has finished. */
	public final boolean isFinished() {
		return this.caught && getCaughtElapsed() >= POPUP_DURATION;
	}

	/** @return Milliseconds since the drop was caught, 0 before that. */
	public final long getCaughtElapsed() {
		if (!this.caught)
			return 0;
		return System.currentTimeMillis() - this.caughtAt;
	}

	/** @return Milliseconds since the drop appeared. */
	public final long getAge() {
		return System.currentTimeMillis() - this.createdAt;
	}

	/** @return Diamonds given when the drop is caught. */
	public final int getValue() {
		return this.value;
	}
}