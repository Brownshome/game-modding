package brownshome.modding.dependencygraph;

import brownshome.modding.ModInfo;
import brownshome.modding.ModDependency;
import brownshome.modding.ModLoadingException;
import brownshome.modding.modsource.ModSource;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * This class is a working class used to solve the dependency requirements for a group of mods.
 */
public final class VersionSelector {
	private final Map<String, DependencyNode> nodeCache = new HashMap<>();
	private final ModSource modSource;
	private final Collection<ModDependency> externalRequirements;

	public VersionSelector(ModSource modSource, Collection<ModDependency> externalRequirements) {
		this.modSource = modSource;

		var groupedRequirements = externalRequirements.stream()
				.collect(Collectors.groupingBy(dep -> getNode(dep.modName())));

		for (var entry : groupedRequirements.entrySet()) {
			var node = entry.getKey();
			var depList = entry.getValue();

			node.setExternalDependency(depList);
		}

		this.externalRequirements = externalRequirements;
	}

	public Map<String, ModInfo> selectModVersions() throws ModLoadingException {
		/*

		For now assume that the dependencies that a mod requires don't change

		Given a graph

		Take a mod not yet added to the graph, compute the requirements.

		If the requirement can't be met downgrade the version of the mod that causes the most stringent requirement and
		recompute the graph.

		If the requirement can be met add the highest version that meets the listed requirements

		*/

		try {
			outerLoop:
			while(true) {
				for(var node : nodeCache.values()) {
					if(node.isRequired() && node.chosenVersion() == null) {
						node.chooseVersion();
						continue outerLoop;
					}
				}

				break;
			}

			return nodeCache.values().stream()
					.filter(DependencyNode::isRequired)
					.map(DependencyNode::chosenVersion)
					.collect(Collectors.toMap(
							ModInfo::name,
							Function.identity(),
							(a, b) -> {
								throw new IllegalStateException();
							}));
		} catch (UnsolvableModGraphException udge) {
			throw new ModLoadingException(String.format("Unable to satisfy requirements %s with source '%s'.", externalRequirements, modSource), udge);
		}
	}

	private DependencyNode getNode(String modName) {
		return nodeCache.computeIfAbsent(modName, key -> new DependencyNode(key, modSource, this::getNode));
	}
}
