package brownshome.modding.modsource;

import brownshome.modding.Mod;
import brownshome.modding.ModInfo;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.ServiceLoader;
import java.util.stream.Collectors;

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

	public static ModSource fromClasspath() {
		return new ModSource() {
			final ServiceLoader<ModInfo> modInfoLoader = ServiceLoader.load(ModInfo.class, ClassLoader.getSystemClassLoader());
			final ServiceLoader<Mod> modLoader = ServiceLoader.load(Mod.class, ClassLoader.getSystemClassLoader());

			@Override
			public Collection<ModInfo> availableMods(String modName) {
				return modInfoLoader.stream()
						.map(ServiceLoader.Provider::get)
						.filter(info -> info.name().equals(modName))
						.collect(Collectors.toList());
			}

			@Override
			public ModuleLayer loadLayer(ModInfo info, List<ModuleLayer> parentLayers) {
				for (var parent : parentLayers) {
					if (parent != ModuleLayer.boot() && parent != ModuleLayer.empty()) {
						throw new IllegalStateException("A classpath mod cannot use non-classpath mods as dependencies");
					}
				}

				return ModuleLayer.boot();
			}
		};
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
	@SuppressWarnings("unchecked")
	public <MOD_CLASS extends Mod> MOD_CLASS loadMod(ModInfo info, List<ModuleLayer> parentLayers) {
		var layer = loadLayer(info, parentLayers);

		for (var mod : ServiceLoader.load(layer, Mod.class)) {
			if (mod.info().equals(info)) {
				return (MOD_CLASS) mod;
			}
		}

		return null;
	}

	/**
	 * Loads a mod matching a given modInfo, but does not create the Mod object, instead only creates a layer that
	 * can be used to load it.
	 */
	public abstract ModuleLayer loadLayer(ModInfo modInfo, List<ModuleLayer> parentLayers);
}
