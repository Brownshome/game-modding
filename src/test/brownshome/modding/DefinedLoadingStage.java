package brownshome.modding;

import java.util.Collection;

class DefinedLoadingStage {
	final String name;

	final Collection<ModStage> after, before;

	DefinedLoadingStage(String name, Collection<ModStage> after, Collection<ModStage> before) {
		this.name = name;
		this.before = before;
		this.after = after;
	}
}
