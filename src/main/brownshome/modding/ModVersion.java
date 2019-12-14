package brownshome.modding;

/**
 * Represents the versioning information for a mod
 */
public abstract class ModVersion implements Comparable<ModVersion> {
	/**
	 * Returns true if this version is compatible with the supplied version. Compatibility means that this version could
	 * be used by someone expecting <em>other</em>.
	 * <br>
	 * Backwards compatibility implies that <code>A.isCompatibleWith(B)</code> where A is newer than B. API compatibility
	 * means that the relation is still true with A and B swapped.
	 */
	public abstract boolean isCompatibleWith(ModVersion other);

	/**
	 * Return true if this version is newer than the supplied version.
	 */
	public abstract boolean isNewerThan(ModVersion other);

	/**
	 * Return true if this versions can be considered identical for dependency purposes.
	 * <br>
	 * Note: This is not the same as equals.
	 */
	public abstract boolean matches(ModVersion other);

	@Override
	public final int compareTo(ModVersion other) {
		if (this.isNewerThan(other)) {
			return 1;
		}

		if (other.isNewerThan(this)) {
			return -1;
		}

		return 0;
	}
}
