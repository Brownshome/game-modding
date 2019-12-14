package brownshome.modding;

import java.util.Objects;

class ModStage {
	final String mod, stage;

	ModStage(String mod, String stage) {
		this.mod = mod;
		this.stage = stage;
	}

	@Override
	public boolean equals(Object o) {
		if(this == o) return true;
		if(!(o instanceof ModStage)) return false;
		ModStage modStage = (ModStage) o;
		return mod.equals(modStage.mod) &&
				stage.equals(modStage.stage);
	}

	@Override
	public int hashCode() {
		return Objects.hash(mod, stage);
	}

	@Override
	public String toString() {
		return String.format("%s@%s", mod, stage);
	}
}
