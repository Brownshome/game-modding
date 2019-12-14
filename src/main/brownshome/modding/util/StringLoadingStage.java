package brownshome.modding.util;

import brownshome.modding.LoadingStageName;

import java.util.Objects;

public class StringLoadingStage implements LoadingStageName {
	private final String name;

	public StringLoadingStage(String name) {
		this.name = name;
	}

	@Override
	public boolean equals(Object other) {
		if (other == this) {
			return true;
		}

		if (!(other instanceof StringLoadingStage)) {
			return false;
		}

		return Objects.equals(name, ((StringLoadingStage) other).name);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(name);
	}
}
