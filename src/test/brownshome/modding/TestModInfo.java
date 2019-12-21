package brownshome.modding;

import java.util.Collection;

class TestModInfo extends ModInfo {
	TestModInfo(String name, ModVersion version, Collection<ModDependency> dependencies) {
		super(name, version, name, dependencies);
	}
}
