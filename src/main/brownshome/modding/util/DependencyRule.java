package brownshome.modding.util;

import brownshome.modding.ModVersion;

@FunctionalInterface
public interface DependencyRule {
	boolean meets(ModVersion required, ModVersion v);
}
