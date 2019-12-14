package brownshome.modding.modsource;

import brownshome.modding.Mod;
import brownshome.modding.ModInfo;

import java.util.Collection;

/**
 * Represents a source from which mods can be acquired.
 */
public abstract class ModSource {
	/**
	 * Creates a mod source from a given classloader
	 * @param loaders A collection of classloaders that should be queried for mod files
	 */
	public static ModSource fromClassloader(Collection<ClassLoader> loaders) {
		return new ClassloaderModSource(loaders);
	}

	public static ModSource combine(Collection<ModSource> sources) {
		return new CombinedModSource(sources);
	}

	/**
	 * Returns a list of modinfo objects that are accessible by this source.
	 */
	public abstract Collection<ModInfo> availableMods(String modName);

	/**
	 * Loads a mod class by name and version
	 **/
	public abstract <MOD_CLASS extends Mod> MOD_CLASS loadMod(ModInfo info);
}
