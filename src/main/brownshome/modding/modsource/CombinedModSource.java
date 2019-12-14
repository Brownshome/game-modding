package brownshome.modding.modsource;

import brownshome.modding.Mod;
import brownshome.modding.ModInfo;
import brownshome.modding.modsource.ModSource;

import java.util.*;

/**
 * A mod source that delegates to a list of other mod sources
 */
final class CombinedModSource extends ModSource {
	private final Collection<ModSource> subSources;

	private final Map<String, Map<ModInfo, ModSource>> sourceMap;

	CombinedModSource(Collection<ModSource> subSources) {
		this.subSources = subSources;
		sourceMap = new HashMap<>();
	}

	@Override
	public Collection<ModInfo> availableMods(String modName) {
		return sourceMap.computeIfAbsent(modName, key -> {
			Map<ModInfo, ModSource> modSourceMap = new HashMap<>();

			for(var source : subSources) {
				var versionsAtSource = source.availableMods(modName);

				for(var version : versionsAtSource) {
					modSourceMap.putIfAbsent(version, source);
				}
			}

			return modSourceMap;
		}).keySet();
	}

	@Override
	@SuppressWarnings("unchecked")
	public <MOD_CLASS extends Mod> MOD_CLASS loadMod(ModInfo info) {
		assert sourceMap.get(info.name()).get(info) != null;

		var source = sourceMap.get(info.name()).get(info);

		return (MOD_CLASS) source.loadMod(info);
	}
}
