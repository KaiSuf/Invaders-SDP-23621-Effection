package engine;

import java.util.Random;
/**
 * AUTHORED BY: VFX TEAM (Effection)
 *
 * Decides when the bonus meteor appears, so it stays rare and works for any number of levels.
 *
 * Rules (all tunable with the constants below):
 * - No meteors before {@link #METEOR_START_LEVEL} (players still learning).
 * - From then on, at most {@link #METEOR_MAX_PER_LEVEL} meteor per level,
 *   up to {@link #METEOR_MAX_LATE_LEVEL} in the
 *   {@link #METEOR_LATE_LEVELS} levels just before the final level, and
 *   none in the final level.
 * - Each meteor drops {@link #METEOR_DIAMOND_VALUE} diamond, which the
 *   player has to catch; caught diamonds are added to the run's pending
 *   diamonds (kept on cash-out, lost on death).
 * - The chance of each meteor starts at {@link #METEOR_BASE_CHANCE} and
 *   grows by {@link #METEOR_CHANCE_STEP} per level, up to
 *   {@link #METEOR_MAX_CHANCE}.
 * - Never in the first {@link #METEOR_SAFE_START} ms of play, never when
 *   fewer than {@link #METEOR_MIN_ENEMIES} enemies are left, never while
 *   another meteor is on screen, and two meteors in one level are at least
 *   {@link #METEOR_MIN_GAP} ms apart.
 * 	Every meteor is different so the player can't predict it: random side, entry angle between {@link #METEOR_MIN_ANGLE} and
 * 	{@link #METEOR_MAX_ANGLE}, a random downward curve up to
 * 	{@link #METEOR_MAX_CURVE}, a random height and a slightly random speed.
 *
 * 	How many meteors a level gets is rolled once, when the level starts.
 */
public class MeteorSpawner {
/** First level where meteors can appear. */
	public static final int METEOR_START_LEVEL = 3;
	/** Chance of a meteor in the start level. */
	public static final double METEOR_BASE_CHANCE = 0.40;
	/** Extra chance for each level after the start level. */
	public static final double METEOR_CHANCE_STEP = 0.10;
	/** Highest chance a meteor can reach. */
	public static final double METEOR_MAX_CHANCE = 0.80;
	/** Most meteors in a normal level. */
	public static final int METEOR_MAX_PER_LEVEL = 1;
	/** Most meteors in the late levels just before the final level. */
	public static final int METEOR_MAX_LATE_LEVEL = 2;
	/** How many levels just before the final level count as late levels. */
	public static final int METEOR_LATE_LEVELS = 2;
	/** No meteor during this many milliseconds after play starts. */
	public static final int METEOR_SAFE_START = 10000;
	/** Random extra wait for the first meteor, after the safe start. */
	public static final int METEOR_FIRST_VARIANCE = 15000;
	/** Shortest time between two meteors in the same level. */
	public static final int METEOR_MIN_GAP = 20000;
	/** Random extra wait added to the gap between two meteors. */
	public static final int METEOR_GAP_VARIANCE = 10000;
	/** No meteor when fewer enemies than this are left. */
	public static final int METEOR_MIN_ENEMIES = 5;
	/** Time to cross the screen in the start level (slow and easy). */
	public static final int METEOR_BASE_CROSS_TIME = 5000;
	/** Crossing gets this many milliseconds faster each level. */
	public static final int METEOR_CROSS_TIME_STEP = 200;
	/** Fastest crossing time, so it never becomes too hard to hit. */
	public static final int METEOR_MIN_CROSS_TIME = 4000;
	/** Gentlest entry angle, in degrees below the horizontal. */
	public static final double METEOR_MIN_ANGLE = 10;
	/** Steepest entry angle, in degrees below the horizontal. */
	public static final double METEOR_MAX_ANGLE =30;
	/** Most extra downward bend over the flight (gravity), in degrees. */
	public static final double METEOR_MAX_CURVE = 10;
	/** Random change of the crossing time, e.g. 0.2 = up to 20% faster or slower. */
	public static final double METEOR_SPEED_VARIANCE = 0.15;
	/** Diamonds given for shooting a meteor. */
	public static final int METEOR_DIAMOND_VALUE = 1;
	/** Current level number. */
	private final int level;
	/** Random source. */
	private final Random random;
	/** Meteors this level will show, rolled when the level starts. */
	private final int plannedMeteors;
	/** Meteors already shown this level. */
	private int spawnedMeteors;
	/** Time play started, 0 until the first call. */
	private long playStart;
	/** Earliest time the next meteor can appear. */
	private long nextSpawnAt;

	/**
	 * Creates the spawner for one level and rolls its meteors.
	 *
	 * @param level
	 *            Current level number, starting at 1.
	 * @param finalLevel
	 *            Number of the last level of the game.
	 * @param random
	 *            Random source.
	 */
	public MeteorSpawner(final int level, final int finalLevel,
			final Random random) {
		this.level = level;
		this.random = random == null ? new Random() : random;

		int planned = 0;
		double chance = chanceForLevel(level);
		for (int i = 0; i < maxForLevel(level, finalLevel); i++)
			if (this.random.nextDouble() < chance)
				planned++;
		this.plannedMeteors = planned;
	}

	/**
	 * Creates the meteor spawner for a level of this game, using the game's
	 * real number of levels so it knows which level is the final one.
	 * AUTHORED BY: VFX TEAM (Effection)
	 *
	 * @param level
	 *            Current level number, starting at 1.
	 * @return Spawner for that level.
	 */
	public static MeteorSpawner forLevel(final int level) {
		return new MeteorSpawner(level, Core.getNumLevels(), new Random());
	}

	/**
	 * Gets the chance of each meteor slot in a level.
	 *
	 * @param level
	 *            Level number.
	 * @return Chance between 0 and 1; 0 before the start level.
	 */
	public static double chanceForLevel(final int level) {
		if (level < METEOR_START_LEVEL)
			return 0;
		return Math.min(METEOR_MAX_CHANCE, METEOR_BASE_CHANCE
				+ METEOR_CHANCE_STEP * (level - METEOR_START_LEVEL));
	}

	/**
	 * Gets the most meteors a level can have.
	 *
	 * @param level
	 *            Level number.
	 * @param finalLevel
	 *            Number of the last level.
	 * @return Meteor limit; 0 before the start level and in the final level.
	 */
	public static int maxForLevel(final int level, final int finalLevel) {
		if (level < METEOR_START_LEVEL || level >= finalLevel)
			return 0;
		if (level >= finalLevel - METEOR_LATE_LEVELS)
			return METEOR_MAX_LATE_LEVEL;
		return METEOR_MAX_PER_LEVEL;
	}

	/**
	 * Gets how long a meteor takes to cross the screen in a level.
	 *
	 * @param level
	 *            Level number.
	 * @return Crossing time in milliseconds.
	 */
	public static int crossTimeForLevel(final int level) {
		int levelsIn = Math.max(0, level - METEOR_START_LEVEL);
		return Math.max(METEOR_MIN_CROSS_TIME, METEOR_BASE_CROSS_TIME
				- METEOR_CROSS_TIME_STEP * levelsIn);
	}

	/**
	 * Checks if a meteor should appear now. Call every gameplay frame; the
	 * first call marks the start of play.
	 *
	 * @param now
	 *            Current time in milliseconds.
	 * @param enemiesLeft
	 *            Enemies still alive.
	 * @param meteorOnScreen
	 *            True if a meteor (flying or bursting) is on screen.
	 * @return True if a meteor should be created now; then call
	 *         {@link #markSpawned(long)}.
	 */
	public final boolean shouldSpawn(final long now, final int enemiesLeft,
			final boolean meteorOnScreen) {
		if (this.playStart == 0) {
			this.playStart = now;
			this.nextSpawnAt = now + METEOR_SAFE_START
					+ this.random.nextInt(METEOR_FIRST_VARIANCE + 1);
		}
		return this.spawnedMeteors < this.plannedMeteors
				&& !meteorOnScreen
				&& now >= this.nextSpawnAt
				&& enemiesLeft >= METEOR_MIN_ENEMIES;
	}

	/**
	 * Records that a meteor appeared and schedules the earliest next one.
	 *
	 * @param now
	 *            Current time in milliseconds.
	 */
	public final void markSpawned(final long now) {
		this.spawnedMeteors++;
		this.nextSpawnAt = now + METEOR_MIN_GAP
				+ this.random.nextInt(METEOR_GAP_VARIANCE + 1);
	}

	/** @return True to make the next meteor enter from the left. */
	public final boolean rollFromLeft() {
		return this.random.nextBoolean();
	}

	/**
	 * Picks a random height between two limits.
	 *
	 * @param minY
	 *            Highest allowed position (smallest Y).
	 * @param maxY
	 *            Lowest allowed position (largest Y).
	 * @return Y position between the limits.
	 */
	public final int rollHeight(final int minY, final int maxY) {
		return minY + this.random.nextInt(Math.max(0, maxY - minY) + 1);
	}

	/**
	 * Gets the crossing time for this meteor: the level's time, made a
	 * little faster or slower at random.
	 *
	 * @return Crossing time in milliseconds.
	 */
	public final int getCrossTime() {
		double change = (this.random.nextDouble() * 2 - 1)
				* METEOR_SPEED_VARIANCE;
		return (int) Math.round(crossTimeForLevel(this.level) * (1 + change));
	}

	/** @return Random entry angle, in degrees below the horizontal. */
	public final double rollStartAngle() {
		return METEOR_MIN_ANGLE + this.random.nextDouble()
				* (METEOR_MAX_ANGLE - METEOR_MIN_ANGLE);
	}

	/**
	 * Picks the angle when leaving: the entry angle plus a random bend.
	 *
	 * @param startAngle
	 *            Entry angle in degrees.
	 * @return Exit angle in degrees, never steeper than 80.
	 */
	public final double rollEndAngle(final double startAngle) {
		return Math.min(80, startAngle
				+ this.random.nextDouble() * METEOR_MAX_CURVE);
	}

	/** @return Diamonds each meteor drops. */
	public final int getDiamondValue() {
		return METEOR_DIAMOND_VALUE;
	}

	/** @return Meteors rolled for this level. */
	public final int getPlannedMeteors() {
		return this.plannedMeteors;
	}
}
