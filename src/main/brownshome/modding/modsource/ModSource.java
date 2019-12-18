package brownshome.modding.modsource;

import brownshome.modding.Mod;
import brownshome.modding.ModInfo;

import java.net.URL;
import java.util.Collection;

/**
 * Represents a source from which mods can be acquired.
 */
public abstract class ModSource {
	/**
	 * Creates a mod source from a given classloader
	 */
	public static ModSource fromURL(URL url) {
		return new URLModSource(url);
	}

	public static ModSource combine(Collection<ModSource> sources) {
		return new CombinedModSource(sources);
	}

	/**
	 * Returns a list of modinfo objects that are accessible by this source.
	 */
	public abstract Collection<ModInfo> availableMods(String modName);

	/**
	 * Sets the parent class loader that will be used if the main loader cannot find the classes.
	 */
	public abstract void setParentLoader(ClassLoader delegate);

	/**
	 * Returns a class loader that can be used to load classes associated with this mod.
	 */
	public abstract ClassLoader classLoader(ModInfo info);

	/**
	 * Loads a mod class by name and version
	 **/
	public abstract <MOD_CLASS extends Mod> MOD_CLASS loadMod(ModInfo info);
}
