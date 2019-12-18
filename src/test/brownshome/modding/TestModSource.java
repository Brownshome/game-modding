package brownshome.modding;

import brownshome.modding.modsource.ModSource;
import brownshome.modding.util.DependencyRules;
import brownshome.modding.util.RuleModDependency;
import brownshome.modding.util.SemanticModVersion;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TestModSource extends ModSource {
	private static Stream<ModInfo> extractModInfos(Entry<String, Map<String, List<Map<String, String>>>> entry) {
		var modName = entry.getKey();
		var versionMap = entry.getValue();

		return versionMap.entrySet().stream().map(versionEntry -> {
			var version = SemanticModVersion.createVersion(versionEntry.getKey());

			List<ModDependency> dependencies;
			var dependencyMaps = versionEntry.getValue();

			if (dependencyMaps == null) {
				dependencies = Collections.emptyList();
			} else {
				dependencies = dependencyMaps.stream()
						.map(TestModSource::extractDependency)
						.collect(Collectors.toList());
			}

			return new TestModInfo(	modName, version, dependencies);
		});
	}

	private static ModDependency extractDependency(Map<String, String> depMap) {
		var depName = depMap.get("mod");
		var version = SemanticModVersion.createVersion(depMap.get("version"));
		var match = DependencyRules.valueOf(depMap.get("match"));

		return new RuleModDependency(depName, version, match);
	}

	public static TestModSource newModSource() throws IOException {
		Yaml yaml = new Yaml();

		try(var depInput = TestModSource.class.getResourceAsStream("moddependencies.yaml"); var loadingInput = TestModSource.class.getResourceAsStream("modloadingstages.yaml")) {
			Map<String, Map<String, List<Map<String, String>>>> loadedDepData = yaml.load(depInput);
			Map<String, Map<String, Map<String, List<Map<String, String>>>>> loadedLoadData = yaml.load(loadingInput);

			return new TestModSource(

					loadedDepData.entrySet().stream()
							.flatMap(TestModSource::extractModInfos)
							.collect(Collectors.groupingBy(ModInfo::name)),

					loadedLoadData.entrySet().stream()
							.collect(Collectors.toMap(Entry::getKey, entry -> {
								var stageMap = entry.getValue();

								if(stageMap == null) {
									return Collections.emptyList();
								}

								return stageMap.entrySet().stream().map(TestModSource::extractLoadingStage).collect(Collectors.toList());
							}))

			);
		}
	}

	private static Collection<ModStage> extractModStageList(List<Map<String, String>> maps) {
		if (maps == null) {
			return Collections.emptyList();
		}

		return maps.stream().map(map -> new ModStage(map.get("mod"), map.get("stage"))).collect(Collectors.toList());
	}

	private final Map<String, List<ModInfo>> mods;
	private final Map<String, List<DefinedLoadingStage>> loadMap;

	private TestModSource(Map<String, List<ModInfo>> modMap, Map<String, List<DefinedLoadingStage>> loadMap) {
		mods = modMap;
		this.loadMap = loadMap;
	}

	private static DefinedLoadingStage extractLoadingStage(Entry<String, Map<String, List<Map<String, String>>>> stageEntry) {
		var stageName = stageEntry.getKey();

		var map = stageEntry.getValue();

		if (map == null) {
			map = Collections.emptyMap();
		}

		var after = extractModStageList(map.get("after"));
		var before = extractModStageList(map.get("before"));

		return new DefinedLoadingStage(stageName, after, before);
	}

	@Override
	public Collection<ModInfo> availableMods(String modName) {
		return mods.getOrDefault(modName, Collections.emptyList());
	}

	@Override
	public ClassLoader classLoader(ModInfo info) {
		return ClassLoader.getPlatformClassLoader();
	}

	@Override
	@SuppressWarnings("unchecked")
	public <MOD_CLASS extends Mod> MOD_CLASS loadMod(ModInfo info, ClassLoader delegate) {
		return (MOD_CLASS) new TestMod(info, loadMap.get(info.name()));
	}

}
