package brownshome.modding.dependencygraph;

import brownshome.modding.ModInfo;
import brownshome.modding.ModDependency;
import brownshome.modding.modsource.ModSource;
import brownshome.modding.ModVersion;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * A collection of modinfo, available versions and dependency information for a mod.
 * <br>
 * This class also keeps track of the versions that are possible given the current dependency structure.
 */
final class DependencyNode {

	// Shared among all nodes
	private final Function<String, DependencyNode> nodeSource;

	// Local constants
	private final String name;

	/** List of mod infos sorted by version number */
	private final List<ModInfo> modInfos;

	// Working variables
	private final class IncomingDep {
		/** Null if the source is external */
		final DependencyNode source;
		final SortedSet<ModInfo> versions;

		IncomingDep(DependencyNode source, Collection<ModDependency> deps) {
			this.source = source;

			versions = new TreeSet<>(ModInfo.VERSION_COMPARATOR.reversed());

			infoLoop:
			for (var info : modInfos) {
				for (var dep : deps) {
					if (!dep.isMetBy(info.version())) {
						continue infoLoop;
					}
				}

				versions.add(info);
			}
		}

		boolean isExternal() {
			return source == null;
		}
	}

	private final Map<DependencyNode, IncomingDep> incomingDeps = new HashMap<>();

	/**
	 * The current chosen version. If this is null it indicates that we have not chosen a chosenVersion.
	 **/
	private ModInfo chosenVersion;

	/**
	 * It is known, with the current incoming mod version searchFrom values that there is no valid mod
	 * version higher than this index
	 **/
	private int searchFrom;

	DependencyNode(String name, ModSource modSource, Function<String, DependencyNode> nodeSource) {
		this.nodeSource = nodeSource;
		this.name = name;

		modInfos = new ArrayList<>(modSource.availableMods(name));
		modInfos.sort(ModInfo.VERSION_COMPARATOR.reversed());

		chosenVersion = null;
		searchFrom = 0;
	}

	/**
	 * Returns true if there is some dependency on this mod, and it therefore needs to be installed.
	 */
	boolean isRequired() {
		return !incomingDeps.isEmpty();
	}

	/**
	 * Returns a node that needs to choose a version for this node to be satisfied.
	 *
	 * @return the node, or null if all dependencies are met.
	 */
	DependencyNode getMissingDependency() {
		for (var dep : chosenVersion.dependencies()) {
			var node = nodeSource.apply(dep.modName());

			if (node.chosenVersion() == null) {
				return node;
			}
		}

		return null;
	}

	/**
	 * Attempts to pick a version for this mod.
	 */
	void chooseVersion() throws UnsolvableModGraphException {
		Collection<DependencyNode> outgoingNodesAtLastSet = new ArrayList<>();

		if (chosenVersion != null) {
			outgoingNodesAtLastSet = chosenVersion.dependencies().stream()
					.map(ModDependency::modName)
					.map(nodeSource)
					.collect(Collectors.toSet());
		}

		do {
			ModInfo newChosenVersion = null;

			versionSearch:
			for (int i = searchFrom; i < modInfos.size(); i++) {
				var modInfo = modInfos.get(i);

				for(var dep : incomingDeps.values()) {
					if(!dep.versions.contains(modInfo)) {
						continue versionSearch;
					}
				}

				searchFrom = i + 1;
				newChosenVersion = modInfo;
				break;
			}

			if(newChosenVersion != null) {
				setVersionChoice(newChosenVersion, outgoingNodesAtLastSet);
				return;
			}

			clearVersionChoice();

			// If there is no such version relax the highest incoming constraint until there is a version, or failure
			// Each dep has a lower and upper mod it will accept, additionally deps may reject mods in-between these ranges
			// The dep with the highest lower bound is the one that is relaxed.
			var nodeToRelax = getNodeToRelax();

			if(nodeToRelax == null) {
				// We need to relax an external requirement, not possible.
				// A detail message is not that useful here, further up the tree we will fill in details.
				throw new UnsolvableModGraphException();
			}

			nodeToRelax.relaxRequirements();

			// Relaxations can cause several things to happen

			// > A parent mod drops in version number
			// > A grand-parent mod drops in version number
			// > A dependency being removed from this mod
			// > This mod no-longer being required
		} while (isRequired());
	}

	private void setVersionChoice(ModInfo newChosenVersion, Collection<DependencyNode> oldOutgoingNodes) {
		chosenVersion = newChosenVersion;

		var outgoingLists = chosenVersion.dependencies().stream()
				.collect(Collectors.groupingBy(dep -> nodeSource.apply(dep.modName())));

		// Remove all deps that are not going to be re-added
		for (var node : oldOutgoingNodes) {
			if (!outgoingLists.containsKey(node)) {
				node.removeIncomingDependency(this);

				// Our new version does not have a dependency on this mod, restart the search
				node.restartVersionSearch();
			}
		}

		for (var entry : outgoingLists.entrySet()) {
			List<ModDependency> dependencies = entry.getValue();
			DependencyNode node = entry.getKey();

			// This also removes any old deps that we had on this node.
			node.setIncomingDependency(this, dependencies);
		}
	}

	/**
	 * Gets a node that must be relaxed before a version can be chosen.
	 */
	private DependencyNode getNodeToRelax() {
		// This value is always written to...
		DependencyNode highestLowerBoundSourceNode = null;

		if (incomingDeps.isEmpty()) {
			throw new IllegalStateException();
		}

		ModVersion highestLowerBound = null;
		for(var entry : incomingDeps.entrySet()) {
			var node = entry.getKey();
			var dep = entry.getValue();

			if(dep.versions.isEmpty()) {
				// This source cannot be met with the available mods, relax it
				return dep.source;
			}

			var lowerBound = dep.versions.first().version();

			if(highestLowerBound == null || lowerBound.compareTo(highestLowerBound) > 0) {
				highestLowerBound = lowerBound;
				highestLowerBoundSourceNode = dep.source;
			}
		}

		return highestLowerBoundSourceNode;
	}

	/**
	 * Lowers the version of this mod by one. This also may trigger a
	 * relaxation in any of the mods that depend on this one.
	 *
	 * @throws UnsolvableModGraphException if the relaxation could not be performed
	 */
	private void relaxRequirements() throws UnsolvableModGraphException {
		// This picks a new version, that must be lower than the current one.
		chooseVersion();
	}

	/** This removes the old external dependencies and sets it to the argument */
	void setExternalDependency(Collection<ModDependency> dependencies) {
		setIncomingDependency(null, dependencies);
	}

	/** This removes the old dependencies from this node and adds the new ones */
	private void setIncomingDependency(DependencyNode node, Collection<ModDependency> dependencies) {
		incomingDeps.put(node, new IncomingDep(node, dependencies));

		// If the new dep is not met by the current version step then invalidate the chosen version
		if (chosenVersion != null && dependencies.stream().anyMatch(dep -> !dep.isMetBy(chosenVersion.version()))) {
			clearVersionChoice();
		}
	}

	private void removeIncomingDependency(DependencyNode incoming) {
		assert incoming != null;

		incomingDeps.remove(incoming);

		// We leave the search pointer intact as this may only be a temporary removal due to relaxation
	}

	/**
	 * Restarts the search from the first modInfo
	 */
	private void restartVersionSearch() {
		searchFrom = 0;
	}

	private void clearVersionChoice() {
		if (chosenVersion == null) {
			// We have nothing to clear.
			return;
		}

		for (var outgoingDep : chosenVersion.dependencies()) {
			nodeSource.apply(outgoingDep.modName()).removeIncomingDependency(this);
		}

		chosenVersion = null;
	}

	/**
	 * Returns the modinfo that is currently chosen, this may be null if no version has currently been chosen
	 * or if this is the root node.
	 */
	ModInfo chosenVersion() {
		return chosenVersion;
	}

	/**
	 * Returns the name of this mod. This will be null if this is the root node.
	 */
	private String name() {
		return name;
	}

	@Override
	public String toString() {
		return modInfos.toString();
	}
}