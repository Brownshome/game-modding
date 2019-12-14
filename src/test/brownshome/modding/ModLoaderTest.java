package brownshome.modding;

import brownshome.modding.util.*;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ModLoaderTest {

	@Test
	void loadMods() throws ModLoadingException, IOException {
		TestMod.clearStageRecord();

		List<ModDependency> requirements = List.of(
				new RuleModDependency("baseMod", SemanticModVersion.createVersion("1.0.0"), DependencyRules.NEWER_OR_EQUAL)
		);

		ModLoader loader = new ModLoader(TestModSource.newModSource());

		loader.loadMods(requirements);

		String[][][] isBefore = {
				{{ "baseMod", "start" }, { "baseMod", "loadImages" }, {"baseMod", "finalizeLoading"}, {"parentMod", "finalizeLoading"}},
				{{ "baseMod", "start" }, { "parentMod", "start" }, {"parentMod", "loadImages"}, {"baseMod", "finalizeLoading"}},
				{{ "baseMod", "start" }, { "libraryMod", "start" }, { "libraryMod", "addCars" }}
		};

		var execOrder = TestMod.getStageRecord();

		for (var run : isBefore) {
			List<ModStage> stages = new ArrayList<>();

			for (var item : run) {
				stages.add(new ModStage(item[0], item[1]));
			}

			var tmp = new ArrayList<>(execOrder);
			tmp.retainAll(stages);

			assertEquals(stages, tmp);
		}
	}
}