package brownshome.modding;

import brownshome.modding.util.StringLoadingStage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

class TestMod extends Mod {
	private static List<ModStage> executedStages = new ArrayList<>();

	static void clearStageRecord() {
		executedStages = new ArrayList<>();
	}

	static List<ModStage> getStageRecord() {
		var result = executedStages;

		executedStages = new ArrayList<>();

		return result;
	}

	private final Collection<DefinedLoadingStage> loadingStages;

	TestMod(ModInfo info, Collection<DefinedLoadingStage> definedLoadingStages) {
		super(info);

		this.loadingStages = definedLoadingStages;
	}

	@Override
	protected Collection<LoadingStage> configureLoadingProcess() {
		return loadingStages.stream().map(stage -> {
			var request = createLoadingStageRequest(new StringLoadingStage(stage.name), () -> {
				executedStages.add(new ModStage(info().name(), stage.name));
				return null;
			});

			for (var after : stage.after) {
				request.after(getNamedMod(after.mod), new StringLoadingStage(after.stage));
			}

			for (var before : stage.before) {
				request.before(getNamedMod(before.mod), new StringLoadingStage(before.stage));
			}

			return request;
		}).collect(Collectors.toList());
	}
}
