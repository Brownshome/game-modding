package brownshome.modding.dependencygraph;

import brownshome.modding.*;
import brownshome.modding.util.*;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class VersionSelectorTest {
	@Test
	void selectModVersions() throws ModLoadingException, IOException {
		List<ModDependency> requirements = List.of(
				new RuleModDependency("baseMod", SemanticModVersion.createVersion("1.0.0"), DependencyRules.NEWER_OR_EQUAL)
		);

		VersionSelector selector = new VersionSelector(TestModSource.newModSource(), requirements);

		assertTrue(isValid(selector.selectModVersions(), requirements));
	}

	private boolean isValid(Map<String, ModInfo> mods, Collection<ModDependency> requirements) {
		var external = requirements.stream();
		var internal = mods.values().stream().flatMap(mod -> mod.dependencies().stream());

		var combined = Stream.concat(external, internal);

		return combined.allMatch(dep -> dep.isMetBy(mods.get(dep.modName()).version()));
	}
}