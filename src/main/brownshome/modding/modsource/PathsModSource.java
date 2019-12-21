package brownshome.modding.modsource;

import brownshome.modding.Mod;
import brownshome.modding.ModInfo;

import java.lang.module.Configuration;
import java.lang.module.ModuleFinder;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Uses a url as a source of mods. The URL should point to a jar containing one mod. Additionally, there are
 * some nuances regarding the loading of Mod classes and non-mod classes.
 *
 * When a class is required to be loaded by a mod, that mod's specific classloader should be checked first, after that,
 * if the class cannot be found, the class-loaders of all selected mods should be queried (so that APIs from other mods
 * can be loaded).
 */
final class PathsModSource extends ModSource {
	private final ModuleFinder moduleFinder;
	private final ServiceLoader<ModInfo> infoLoader;
	private final String name;

	PathsModSource(Collection<Path> paths) {
		this.moduleFinder = ModuleFinder.of(paths.toArray(new Path[0]));

		var urls = paths.stream().map(f -> {
			try {
				return f.toUri().toURL();
			} catch (MalformedURLException e) {
				throw new RuntimeException(e);
			}
		}).toArray(URL[]::new);

		// Load the mods outside of the module system as an unnamed module, as we can't load dependencies yet.
		infoLoader = ServiceLoader.load(ModInfo.class, new URLClassLoader(String.format("URLClassLoader[%s]", (Object) urls), urls, Thread.currentThread().getContextClassLoader()));
		name = String.format("ModSource[%s]", (Object) urls);
	}

	@Override
	public Collection<ModInfo> availableMods(String modName) {
		List<ModInfo> availableVersions = new ArrayList<>();

		for (var info : infoLoader) {
			if (!info.name().equals(modName)) {
				continue;
			}

			availableVersions.add(info);
		}

		availableVersions.sort(null);

		return availableVersions;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <MOD_CLASS extends Mod> MOD_CLASS loadMod(ModInfo info, List<ModuleLayer> parents) {
		// A new service loader and module layer is needed for each mod, as the parent module layers need to be reconfigured

		parents = new ArrayList<>(parents);
		parents.add(ModuleLayer.boot());

		var parentClassLoader = Thread.currentThread().getContextClassLoader();

		var parentConfigurations = parents.stream().map(ModuleLayer::configuration).collect(Collectors.toList());
		var configuration = Configuration.resolveAndBind(moduleFinder, parentConfigurations, ModuleFinder.of(), List.of(info.moduleName()));
		var moduleLayer = ModuleLayer.defineModulesWithManyLoaders(configuration, parents, parentClassLoader).layer();

		for (var mod : ServiceLoader.load(moduleLayer, Mod.class)) {
			if (mod.info().name().equals(info.name())) {
				return (MOD_CLASS) mod;
			}
		}

		return null;
	}

	@Override
	public String toString() {
		return name;
	}
}
