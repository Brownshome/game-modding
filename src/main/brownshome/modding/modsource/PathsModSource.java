package brownshome.modding.modsource;

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
 * Uses a collection of paths as a source of mods. The paths can be directories containing exploded class structures or
 * point to single JAR files. Paths pointing to directories of JAR files will not be explored.
 */
final class PathsModSource extends ModSource {
	private final ModuleFinder moduleFinder;
	private final ServiceLoader<ModInfo> infoServiceLoader;
	private final ClassLoader infoClassLoader;
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
		infoClassLoader = new URLClassLoader(String.format("URLClassLoader %s", Arrays.toString(urls)), urls, Thread.currentThread().getContextClassLoader());
		infoServiceLoader = ServiceLoader.load(ModInfo.class, infoClassLoader);
		name = String.format("URL %s", Arrays.toString(urls));
	}

	@Override
	public Collection<ModInfo> availableMods(String modName) {
		List<ModInfo> availableVersions = new ArrayList<>();

		for (var info : infoServiceLoader) {
			if (info.getClass().getClassLoader() != infoClassLoader) {
				// Ignore any mods loaded from the parent classpath
				continue;
			}

			if (!info.name().equals(modName)) {
				continue;
			}

			availableVersions.add(info);
		}

		availableVersions.sort(null);

		return availableVersions;
	}

	@Override
	public ModuleLayer loadLayer(ModInfo info, List<ModuleLayer> parents) {
		// A new service loader and module layer is needed for each mod, as the parent module layers need to be reconfigured

		parents = new ArrayList<>(parents);
		parents.add(ModuleLayer.boot());

		var parentClassLoader = Thread.currentThread().getContextClassLoader();

		var parentConfigurations = parents.stream().map(ModuleLayer::configuration).collect(Collectors.toList());
		var configuration = Configuration.resolve(moduleFinder, parentConfigurations, ModuleFinder.of(), List.of(info.moduleName()));
		return ModuleLayer.defineModulesWithManyLoaders(configuration, parents, parentClassLoader).layer();
	}

	@Override
	public String toString() {
		return name;
	}
}
