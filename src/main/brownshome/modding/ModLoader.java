package brownshome.modding;

import brownshome.modding.dependencygraph.VersionSelector;
import brownshome.modding.modsource.ModSource;

import java.lang.module.Configuration;
import java.lang.module.ModuleFinder;
import java.util.*;
import java.util.stream.Collectors;

/**
 * This class takes a collection of ModSources and loads the mods
 */
public final class ModLoader {
	private final ModSource source;

	// Local variables used for stages
	private Map<String, ModuleLayer> modLayers;
	private Map<String, Mod> loadedMods;

	private ModuleLayer childOfAllLayer;
	private Map<Class<?>, ServiceLoader<?>> serviceLoaders;

	private Mod currentlyLoadingMod = null;

	/**
	 * Creates a modloaded from a collection of sources
	 * @param source The mod source to load from
	 */
	public ModLoader(ModSource source) {
		this.source = source;
	}

	/**
	 * Loads the network of mods
	 *
	 * @param rootRequirements a list of requirements that the loader must meet.
	 */
	public void loadMods(Collection<ModDependency> rootRequirements) throws ModLoadingException {
		solveDependencyGraph(rootRequirements);
		initMods();
	}

	private void solveDependencyGraph(Collection<ModDependency> rootRequirements) throws ModLoadingException {
		VersionSelector selector = new VersionSelector(source, rootRequirements);

		var selectedModInfos = selector.selectModVersions();
		var allLayers = new ArrayList<ModuleLayer>();

		modLayers = new HashMap<>();
		loadedMods = new HashMap<>();

		for (var modInfo : selectedModInfos.values()) {
			allLayers.add(loadMod(modInfo, selectedModInfos));
		}

		createChildLayer(allLayers);
	}

	private void createChildLayer(List<ModuleLayer> layers) {
		var allConfigurations = layers.stream().map(ModuleLayer::configuration).collect(Collectors.toList());
		var childOfAllConfig = Configuration.resolve(ModuleFinder.of(), allConfigurations, ModuleFinder.of(), Collections.emptyList());
		childOfAllLayer = ModuleLayer.defineModulesWithOneLoader(childOfAllConfig, layers, ClassLoader.getSystemClassLoader()).layer();
		serviceLoaders = new HashMap<>();
	}

	private ModuleLayer loadMod(ModInfo modInfo, Map<String, ModInfo> selectedModInfos) {
		var layer = modLayers.get(modInfo.name());

		if (layer != null) {
			return layer;
		}

		var parentLayers = new ArrayList<ModuleLayer>();

		for (var dep : modInfo.dependencies()) {
			var depName = dep.modName();
			var depInfo = selectedModInfos.get(depName);

			var parentLayer = modLayers.get(depName);
			if (parentLayer == null) {
				parentLayer = loadMod(depInfo, selectedModInfos);
			}

			parentLayers.add(parentLayer);
		}

		if (modInfo.hasModFile()) {
			var mod = source.loadMod(modInfo, parentLayers);
			mod.loader(this);
			loadedMods.put(modInfo.name(), mod);
			layer = mod.getClass().getModule().getLayer();
		} else {
			layer = source.loadLayer(modInfo, parentLayers);
		}

		if (layer == null) {
			// We must be an unnamed module, use the boot layer as a parent
			layer = ModuleLayer.boot();
		}

		modLayers.put(modInfo.name(), layer);

		return layer;
	}

	private void initMods() throws ModLoadingException {
		Collection<LoadingStage> stages = new ArrayList<>();

		// Configure all of the mods
		for (var mod : loadedMods.values()) {
			stages.addAll(mod.configureLoadingProcess());
			stages.add(mod.startStage());
			stages.add(mod.endStage());
		}

		for (var stage : stages) {
			stage.computeExecutionTree(stages);
		}

		Collection<LoadingStage> roots = new ArrayList<>();

		// Executing corrupts the root field. Query the field first
		for (var stage : stages) {
			if (stage.root()) {
				roots.add(stage);
			}
		}

		// Load mods
		for (var root : roots) {
			root.execute();
		}

		// Find any mods that didn't load
		var deadlockedMods = stages.stream()
				.filter(s -> !s.root())
				.collect(Collectors.toList());

		if (!deadlockedMods.isEmpty()) {
			throw new ModDeadlockException(deadlockedMods);
		}
	}

	/**
	 * Gets a named mod.
	 *
	 * @throws ClassCastException if the mod is not the expected class. This will never be thrown if {@link MOD_TYPE} is {@link Mod}.
	 */
	@SuppressWarnings("unchecked")
	public <MOD_TYPE extends Mod> MOD_TYPE namedMod(String name) {
		return (MOD_TYPE) loadedMods.get(name);
	}

	Mod currentlyLoadingMod() {
		return currentlyLoadingMod;
	}

	void signalIsLoading(Mod mod) {
		currentlyLoadingMod = mod;
	}

	/**
	 * Gets a service loader that returns all service providers loaded currently. This loader will load from all classpath
	 * sources and loaded mods.
	 *
	 * @param serviceClass the class to load services for
	 * @param <TYPE> the type of the service provider
	 * @return a service loader that will load from all mod and classpath sources.
	 */
	@SuppressWarnings("unchecked")
	<TYPE> ServiceLoader<TYPE> serviceLoader(Class<TYPE> serviceClass) {
		return (ServiceLoader<TYPE>) serviceLoaders.computeIfAbsent(serviceClass, this::createServiceClass);
	}

	private <TYPE> ServiceLoader<TYPE> createServiceClass(Class<TYPE> serviceClass) {
		ModLoader.class.getModule().addUses(serviceClass);
		return ServiceLoader.load(childOfAllLayer, serviceClass);
	}
}