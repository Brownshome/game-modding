package brownshome.modding;

import java.util.Collection;
import java.util.Comparator;
import java.util.Objects;

/**
 * This class is loaded prior to the main mod. Care must be taken to avoid any direct references to an classes that
 * are found is dependency mod classes as these classes will not have been loaded yet.
 */
public abstract class ModInfo {
	public final static Comparator<ModInfo> VERSION_COMPARATOR = Comparator.comparing(ModInfo::version);

	private final ModVersion version;
	private final String name;
	private final Collection<ModDependency> dependencies;

	protected ModInfo(String name, ModVersion version, Collection<ModDependency> dependencies) {
		this.version = version;
		this.name = name;
		this.dependencies = dependencies;
	}

	/**
	 * A unique name for this Mod.
	 */
	public String name() {
		return name;
	}

	/**
	 * The version of this mod.
	 */
	public final ModVersion version() {
		return version;
	}

	/**
	 * A list of dependencies that are required for this mod to function. Loading of this mod will fail if any of these
	 * dependencies cannot be met.
	 */
	public Collection<ModDependency> dependencies() {
		return dependencies;
	}

	@Override
	public String toString() {
		return String.format("%s@%s", name(), version());
	}

	@Override
	public final boolean equals(Object o) {
		if(this == o) return true;
		if(!(o instanceof ModInfo)) return false;
		ModInfo modInfo = (ModInfo) o;
		return version.equals(modInfo.version) &&
				name.equals(modInfo.name);
	}

	@Override
	public final int hashCode() {
		return Objects.hash(version, name);
	}
}
