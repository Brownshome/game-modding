package brownshome.modding.modsource;

import brownshome.modding.Mod;
import brownshome.modding.ModInfo;
import brownshome.modding.ModLoader;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
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
	/**
	 * Creates a {@link ModSource} from the provided folder. Each sub-folder in the folder is given a separate loader and act
	 * as separators for classes with the same name. Modules in sub-folders will not read modules in other sub-folders, everything
	 * can read libraries in its own folder and the root folder. This method does not support more reading from sub-folders in
	 * sub-folders.
	 *
	 * @param modFolder a path to the mod folder
	 * @throws IOException if the path is not a readable folder, or any of the sub-folders is not readable.
	 */
	public static ModSource fromFolder(Path modFolder) throws IOException {
		if (!Files.exists(modFolder) || !Files.isDirectory(modFolder)) {
			throw new IOException("The provided mod folder must exist and be a folder");
		}

		var commonJars = collectJars(modFolder);

		var sources = Files.list(modFolder)
				.filter(Files::isDirectory)
				.map(modDir -> {
					var combinedList = new ArrayList<>(commonJars);
					combinedList.addAll(collectJars(modDir));

					return ModSource.fromPaths(combinedList);
				})
				.collect(Collectors.toList());

		return ModSource.combine(sources);
	}

	private static List<Path> collectJars(Path modFolder) {
		try {
			return Files.list(modFolder)
					.filter(Files::isRegularFile)
					.filter(path -> path.toString().endsWith(".jar"))
					.collect(Collectors.toList());
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public static ModSource fromPaths(Path... paths) {
		return fromPaths(List.of(paths));
	}

	/**
	 * Creates a {@link ModSource} from a collection of paths. Only mods residing on these paths will be loaded, but
	 * any class accessible via {@link Thread#getContextClassLoader()} will be able to be read by the loaded mods, in addition
	 * to any class on the list of paths.
	 * <br>
	 * The paths can be directories of classes or JAR files, but not directories of JAR files. In addition this method does
	 * not support loading multiple classes with the same name. If that is required use {@link #fromFolder(Path)} or {@link #combine(Collection<ModSource>)}
	 */
	public static ModSource fromPaths(Collection<Path> paths) {
		return new PathsModSource(paths);
	}

	public static ModSource fromClasspath() {
		return new ModSource() {
			final ServiceLoader<ModInfo> modInfoLoader = ServiceLoader.load(ModInfo.class, ClassLoader.getSystemClassLoader());

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

			@Override
			public String toString() {
				return "Classpath";
			}
		};
	}

	public static ModSource combine(ModSource... sources) {
		return combine(List.of(sources));
	}

	/**
	 * Combines several mod sources into one. Each mod source will not load classes from the other mod sources during
	 * module resolution.
	 * @param sources a collection of sources to combine
	 */
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
