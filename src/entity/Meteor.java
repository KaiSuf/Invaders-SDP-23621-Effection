package entity;

import java.awt.Color;
import java.util.Random;
/**
 * AUTHORED BY: VFX TEAM (Effection)
 * A rare meteor that falls diagonally across the screen, from the left
 * side to the right side or the other way round. Shooting it makes it burst into sparks and gives bonus diamond.
 * It never damages the player and ignores enemy bullets.
 *
 * The meteor has two phases:
 * - flying: moves on a curved falling path until shot or off screen;
 * - bursting: stays in place while its sparks and diamond popup play, then
 *   reports itself finished so the screen can remove it.
 */
public class Meteor extends Entity {

    /** Size of the meteor in pixels (9x9 sprite at the 2-pixel scale). */
	public static final int SIZE = 9 * 2;
	/** Time the burst (sparks and diamond popup) lasts, in milliseconds. */
	public static final int BURST_DURATION = 900;
	/** Number of sparks in the burst. */
	private static final int SPARK_COUNT = 12;
	/** Slowest spark speed, in pixels per millisecond. */
	private static final float SPARK_MIN_SPEED = 0.04f;
	/** Extra random spark speed, in pixels per millisecond. */
	private static final float SPARK_SPEED_VARIANCE = 0.06f;
	/** Longest time step used for one move, so a lag spike can't teleport it. */
	private static final long MAX_STEP = 50;
	/** Slowest spin, in radians per millisecond (about half a turn a second). */
	private static final float SPIN_MIN = 0.003f;
	/** Extra random spin, in radians per millisecond. */
	private static final float SPIN_VARIANCE = 0.006f;
	/** 1 when flying to the right, -1 when flying to the left. */
	private final int direction;
	/** Signed horizontal speed in pixels per millisecond. */
	private final float speed;
	/** Downward speed when entering, in pixels per millisecond. */
	private final float fallSpeedStart;
	/** Downward speed when leaving, in pixels per millisecond. */
	private final float fallSpeedEnd;
	/** Time the flight across the screen takes, in milliseconds. */
	private final float flightTime;
	/** Time flown so far, in milliseconds. */
	private float flightElapsed;
	/** Current downward speed, in pixels per millisecond. */
	private float fallSpeed;
	/** Spin speed in radians per millisecond (sign = spin direction). */
	private final float spinSpeed;
	/** Current rotation of the rock, in radians. */
	private float rotation;
	/** Exact horizontal position, kept as a float for smooth movement. */
	private float exactX;
	/** Exact vertical position, kept as a float for smooth movement. */
	private float exactY;
	/** Time of the last movement update. */
	private long lastUpdate;
	/** diamond given when the meteor is shot. */
	private final int diamondValue;
	/** True once the meteor has been shot. */
	private boolean bursting;
	/** Time the burst started. */
	private long burstStart;
	/** Horizontal speed of each spark, in pixels per millisecond. */
	private final float[] sparkSpeedX;
	/** Vertical speed of each spark, in pixels per millisecond. */
	private final float[] sparkSpeedY;
	/** Random source for the spark directions. */
	private final Random random;
	/**
	 * Creates a meteor just outside one side of the screen.
	 *
	 * @param fromLeft
	 *            True to enter from the left side, false from the right.
	 * @param positionY
	 *            Height of the flight path (top of the meteor).
	 * @param screenWidth
	 *            Screen width, used to place the meteor and set its speed.
	 * @param crossTime
	 *            Time to cross the whole screen, in milliseconds.
	 * @param startAngle
	 *            Downward angle when entering, in degrees from the
	 *            horizontal (0 = flat).
	 * @param endAngle
	 *            Downward angle when leaving; larger than startAngle makes
	 *            the path curve down, like gravity.
	 * @param diamondValue
	 *            diamond given when the meteor is shot.
	 * @param random
	 *            Random source for the spin and the spark directions.
	 */
	public Meteor(final boolean fromLeft, final int positionY,
			final int screenWidth, final int crossTime,
			final double startAngle, final double endAngle,
			final int diamondValue, final Random random) {
		super(fromLeft ? -SIZE : screenWidth, positionY, SIZE, SIZE,
				new Color(120, 100, 90));
		this.direction = fromLeft ? 1 : -1;
		this.speed = this.direction * (float) (screenWidth + SIZE)
				/ Math.max(1, crossTime);
		this.fallSpeedStart = Math.abs(this.speed)
				* (float) Math.tan(Math.toRadians(startAngle));
		this.fallSpeedEnd = Math.abs(this.speed)
				* (float) Math.tan(Math.toRadians(endAngle));
		this.fallSpeed = this.fallSpeedStart;
		this.flightTime = Math.max(1, crossTime);
		this.exactX = this.positionX;
		this.exactY = this.positionY;
		this.lastUpdate = System.currentTimeMillis();
		this.diamondValue = diamondValue;
		this.random = random == null ? new Random() : random;
		// Rolls forward in its flight direction, at a random speed.
		this.spinSpeed = this.direction * (SPIN_MIN
				+ this.random.nextFloat() * SPIN_VARIANCE);
		this.rotation = this.random.nextFloat() * (float) (2 * Math.PI);
		this.sparkSpeedX = new float[SPARK_COUNT];
		this.sparkSpeedY = new float[SPARK_COUNT];
	}

	/** Moves the meteor while it is flying. Call once per frame. */
	public final void update() {
		long now = System.currentTimeMillis();
		long step = Math.min(MAX_STEP, now - this.lastUpdate);
		this.lastUpdate = now;
		if (this.bursting)
			return;
		// The fall speed grows smoothly from start to end, so the path
		// bends downward over the flight.
		this.flightElapsed += step;
		float progress = Math.min(1f, this.flightElapsed / this.flightTime);
		this.fallSpeed = this.fallSpeedStart
				+ (this.fallSpeedEnd - this.fallSpeedStart) * progress;
		this.exactX += this.speed * step;
		this.exactY += this.fallSpeed * step;
		this.rotation += this.spinSpeed * step;
		this.positionX = Math.round(this.exactX);
		this.positionY = Math.round(this.exactY);
	}

	/**
	 * Gets how far a meteor falls while crossing the screen.
	 *
	 * @param screenWidth
	 *            Screen width.
	 * @param startAngle
	 *            Downward angle when entering, in degrees.
	 * @param endAngle
	 *            Downward angle when leaving, in degrees.
	 * @return Total drop in pixels from entering to leaving.
	 */
	public static int getTotalDrop(final int screenWidth,
			final double startAngle, final double endAngle) {
		// The fall speed changes evenly, so the drop uses the average slope.
		return (int) Math.ceil((screenWidth + SIZE)
				* (Math.tan(Math.toRadians(startAngle))
						+ Math.tan(Math.toRadians(endAngle))) / 2);
	}

	/** @return Current rotation of the spinning rock, in radians. */
	public final float getRotation() {
		return this.rotation;
	}

	/** @return Downward speed divided by sideways speed (tail slope). */
	public final float getFallSlope() {
		return this.speed == 0 ? 0 : this.fallSpeed / Math.abs(this.speed);
	}
	/** Starts the burst: the meteor stops and its sparks fly out. */
	public final void burst() {
		if (this.bursting)
			return;
		this.bursting = true;
		this.burstStart = System.currentTimeMillis();
		for (int i = 0; i < SPARK_COUNT; i++) {
			// Evenly spread angles with a little jitter, so the burst
			// always looks round but never identical.
			double angle = 2 * Math.PI * (i + this.random.nextDouble() * 0.6)
					/ SPARK_COUNT;
			float sparkSpeed = SPARK_MIN_SPEED
					+ this.random.nextFloat() * SPARK_SPEED_VARIANCE;
			this.sparkSpeedX[i] = (float) Math.cos(angle) * sparkSpeed;
			this.sparkSpeedY[i] = (float) Math.sin(angle) * sparkSpeed;
		}
	}

	/** @return True once the meteor has been shot. */
	public final boolean isBursting() {
		return this.bursting;
	}

	/** @return Milliseconds since the burst started, 0 while flying. */
	public final long getBurstElapsed() {
		if (!this.bursting)
			return 0;
		return System.currentTimeMillis() - this.burstStart;
	}

	/** @return True when the burst animation has fully played. */
	public final boolean isBurstFinished() {
		return this.bursting && getBurstElapsed() >= BURST_DURATION;
	}

	/**
	 * Checks if a flying meteor has completely left the screen.
	 *
	 * @param screenWidth
	 *            Screen width.
	 * @return True when the meteor is past the far side.
	 */
	public final boolean isOffScreen(final int screenWidth) {
		if (this.bursting)
			return false;
		return this.direction > 0 ? this.positionX > screenWidth
				: this.positionX + this.width < 0;
	}
	
	/** @return Sideways speed in pixels per millisecond (sign = direction). */
	public final float getSpeedX() {
		return this.speed;
	}
	/** @return Vertical speed in pixels per millisecond. */
	public final float getSpeedY() {
		return this.fallSpeed;
	}

	/** @return 1 when flying right, -1 when flying left. */
	public final int getDirection() {
		return this.direction;
	}

	/** @return diamond given when the meteor is shot. */
	public final int getDiamondValue() {
		return this.diamondValue;
	}

	/** @return Number of sparks in the burst. */
	public final int getSparkCount() {
		return SPARK_COUNT;
	}

	/**
	 * @param index
	 *            Spark index.
	 * @return Horizontal speed of the spark, in pixels per millisecond.
	 */
	public final float getSparkSpeedX(final int index) {
		return this.sparkSpeedX[index];
	}
	/**
	 * @param index
	 *            Spark index.
	 * @return Vertical speed of the spark, in pixels per millisecond.
	 */
	public final float getSparkSpeedY(final int index) {
		return this.sparkSpeedY[index];
	}
}
