package brownshome.modding.util;

import brownshome.modding.ModDependency;
import brownshome.modding.ModVersion;

public class RuleModDependency implements ModDependency {
	private final String modName;

	private final ModVersion version;
	private final DependencyRule rule;

	/**
	 * Creates a dependency object.
	 * @param modName The name of the mod that this dependency is against.
	 * @param version A version to test against, this must be the lowest version that meets the requirement.
	 * @param rule The required relationship between the mods
	 */
	public RuleModDependency(String modName, ModVersion version, DependencyRule rule) {
		this.modName = modName;
		this.version = version;
		this.rule = rule;
	}

	public boolean isMetBy(ModVersion version) {
		return rule.meets(this.version, version);
	}

	public String modName() {
		return modName;
	}

	@Override
	public String toString() {
		return String.format("%s %s@%s", rule, modName(), version);
	}

}
