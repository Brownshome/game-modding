package brownshome.modding.modsource;

import brownshome.modding.Mod;
import brownshome.modding.ModInfo;

import java.lang.module.ModuleFinder;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

/**
 * Represents a source from which mods can be acquired.
 */
public abstract class ModSource {
	public static ModSource fromPaths(Path... paths) {
		return fromPaths(List.of(paths));
	}

	public static ModSource fromPaths(Collection<Path> paths) {
		return new PathsModSource(paths);
	}

	public static ModSource combine(Collection<ModSource> sources) {
		return new CombinedModSource(sources);
	}

	/**
	 * Returns a list of ModInfo objects that are accessible by this source.
	 */
	public abstract Collection<ModInfo> availableMods(String modName);

	/**
	 * Loads a mod class by name and version
	 **/
	public abstract <MOD_CLASS extends Mod> MOD_CLASS loadMod(ModInfo info, List<ModuleLayer> parentLayers);
}
