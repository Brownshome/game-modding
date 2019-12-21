package brownshome.modding;

import brownshome.modding.dependencygraph.VersionSelector;
import brownshome.modding.modsource.ModSource;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * This class takes a collection of ModSources and loads the mods
 */
public final class ModLoader {
	private final ModSource source;

	// Local variables used for stages
	private Map<String, Mod> chosenMods;

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
		chosenMods = new HashMap<String, Mod>();

		for (var modInfo : selectedModInfos.values()) {
			loadMod(modInfo, selectedModInfos).loader(this);
		}
	}

	private Mod loadMod(ModInfo modInfo, Map<String, ModInfo> selectedModInfos) {
		var mod = chosenMods.get(modInfo.name());

		if (mod != null) {
			return mod;
		}

		var parentLayers = new ArrayList<ModuleLayer>();

		for (var modDependency : modInfo.dependencies()) {
			var modName = modDependency.modName();
			var depInfo = selectedModInfos.get(modName);
			var depMod = loadMod(depInfo, selectedModInfos);
			var layer = depMod.getClass().getModule().getLayer();
			parentLayers.add(layer);
		}

		mod = source.loadMod(modInfo, parentLayers);
		chosenMods.put(modInfo.name(), mod);
		return mod;
	}

	private void initMods() throws ModLoadingException {
		Collection<LoadingStage> stages = new ArrayList<>();

		// Configure all of the mods
		for (var mod : chosenMods.values()) {
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
	 * @throws NullPointerException if the named mod was not loaded.
	 */
	Mod getNamedMod(String name) {
		return chosenMods.get(name);
	}
}