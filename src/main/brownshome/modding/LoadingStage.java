package brownshome.modding;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.function.Predicate;

public final class LoadingStage {
	private final Mod mod;
	private final LoadingStageName stageName;
	private final Callable<Void> action;

	@FunctionalInterface
	private interface StageFilter extends Predicate<LoadingStage> {  }

	private final Collection<StageFilter> after = new ArrayList<>();
	private final Collection<StageFilter> before = new ArrayList<>();

	// Variables used during construction
	private Collection<LoadingStage> stagesWaiting = new ArrayList<>();
	private int numberOfParentsLeft = 0;

	LoadingStage(Mod mod, LoadingStageName stageName, Callable<Void> action) {
		this.mod = mod;
		this.stageName = stageName;
		this.action = action;
	}

	// BUILDER METHODS

	/**
	 * Specifies a stage that this one should run after
	 */
	public LoadingStage after(Mod otherMod, LoadingStageName stageName) {
		after.add(exactMatch(otherMod, stageName));
		return this;
	}

	/**
	 * Specifies a stage that this one should run before
	 */
	public LoadingStage before(Mod otherMod, LoadingStageName stageName) {
		before.add(exactMatch(otherMod, stageName));
		return this;
	}

	/**
	 * Specifies that this stage should run after all mods complete this stage.
	 *
	 * @param includeThisMod whether this mod should be considered for determining ordering.
	 */
	public LoadingStage after(LoadingStageName stageName, boolean includeThisMod) {
		after.add(includeThisMod ? allMods(stageName) : allModsExceptThis(stageName));
		return this;
	}

	/**
	 * Specifies that this stage should run before all mods complete this stage.
	 *
	 * @param includeThisMod whether this mod should be considered for determining ordering.
	 */
	public LoadingStage before(LoadingStageName stageName, boolean includeThisMod) {
		before.add(includeThisMod ? allMods(stageName) : allModsExceptThis(stageName));
		return this;
	}

	/**
	 * Specifies that this stage should run after all mods matching the filter complete the named stage.
	 */
	public LoadingStage after(LoadingStageName stageName, Predicate<Mod> mods) {
		after.add(customFilter(stageName, mods));
		return this;
	}

	/**
	 * Specifies that this stage should run before all mods matching the filter complete the named stage.
	 */
	public LoadingStage before(LoadingStageName stageName, Predicate<Mod> mods) {
		before.add(customFilter(stageName, mods));
		return this;
	}

	public LoadingStage after(LoadingStage stage) {
		after.add(stage::equals);
		return this;
	}

	public LoadingStage before(LoadingStage stage) {
		before.add(stage::equals);
		return this;
	}

	// HELPERS

	private static StageFilter exactMatch(Mod mod, LoadingStageName name) {
		return s -> Objects.equals(s.mod, mod) && Objects.equals(s.stageName, name);
	}

	private static StageFilter allMods(LoadingStageName name) {
		return s -> Objects.equals(s.stageName, name);
	}

	private StageFilter allModsExceptThis(LoadingStageName name) {
		return s -> !Objects.equals(s.mod, mod) && Objects.equals(s.stageName, name);
	}

	private static StageFilter customFilter(LoadingStageName name, Predicate<Mod> mods) {
		return s -> Objects.equals(s.stageName, name) && mods.test(s.mod);
	}

	// CONFIGURE EXECUTION

	void computeExecutionTree(Collection<LoadingStage> stages) {
		for (var stage : stages) {
			if (this.isAfter(stage)) {
				numberOfParentsLeft++;
				stage.stagesWaiting.add(this);
			}

			if (this.isBefore(stage)) {
				stagesWaiting.add(stage);
				stage.numberOfParentsLeft++;
			}
		}
	}

	private boolean isBefore(LoadingStage stage) {
		return before.stream().anyMatch(filter -> filter.test(stage));
	}

	private boolean isAfter(LoadingStage stage) {
		return after.stream().anyMatch(filter -> filter.test(stage));
	}

	/**
	 * Returns true if nothing needs to execute before this stage
	 */
	boolean root() {
		return numberOfParentsLeft == 0;
	}

	// EXECUTION

	/**
	 * Runs this task and then runs child tasks, if they are ready to be run.
	 * @throws ModLoadingException If this task or a sub-task throws an exception
	 */
	void execute() throws ModLoadingException {
		assert numberOfParentsLeft == 0;

		mod.signalIsLoading();

		try {
			action.call();
		} catch(Exception e) {
			throw new ModStageExecutionException(mod, stageName, e);
		}

		for (var stage : stagesWaiting) {
			// Traverse DFS into the stages tree.
			// TODO multithread
			stage.signalParentCompleted();
		}
	}

	private void signalParentCompleted() throws ModLoadingException {
		assert numberOfParentsLeft > 0;

		numberOfParentsLeft--;

		if (numberOfParentsLeft == 0) {
			execute();
		}
	}

	@Override
	public String toString() {
		return String.format("%s[%s]", mod, stageName);
	}
}
