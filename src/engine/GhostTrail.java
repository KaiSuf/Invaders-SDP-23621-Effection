package engine;

import java.util.ArrayDeque;
import java.util.Iterator;

/**
 * Ghost Trail ship effect (shop item "effect_ghost_trail"). While the
 * player's ship moves, a few fading copies of it follow behind. When the
 * ship stops, the copies disappear. Look only, no gameplay change.
 * AUTHORED BY: VFX TEAM (Effection)
 */
public final class GhostTrail {

	/** How many past positions are remembered (one per frame). */
	private static final int HISTORY = 17;
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
}
