package brownshome.modding.util;

import brownshome.modding.ModDependency;
import brownshome.modding.ModVersion;

public class AnyMod implements ModDependency {
	private final String modName;

	public AnyMod(String modName) {
		this.modName = modName;
	}

	@Override
	public String modName() {
		return modName;
	}

	@Override
	public boolean isMetBy(ModVersion version) {
		return true;
	}

	@Override
	public String toString() {
		return String.format("ANY %s", modName);
	}
}
