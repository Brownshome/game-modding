package brownshome.modding;

import java.util.Collection;

public class ModDeadlockException extends ModLoadingException {
	private final Collection<LoadingStage> deadlockedStages;

	ModDeadlockException(Collection<LoadingStage> stagesToLoad) {
		super("Unable to execute " + stagesToLoad + " due to deadlock.");
		deadlockedStages = stagesToLoad;
	}

	public Collection<LoadingStage> deadlockedStages() {
		return deadlockedStages;
	}
}
