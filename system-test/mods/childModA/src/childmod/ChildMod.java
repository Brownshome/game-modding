package childmod;

import basemod.api.BaseModAPI;
import basemod.api.ChildModProvider;
import browngu.logging.Logger;
import browngu.logging.Severity;
import brownshome.modding.LoadingStage;
import brownshome.modding.Mod;
import brownshome.modding.annotation.DefineMod;
import brownshome.modding.annotation.Requirement;
import brownshome.modding.util.DependencyRules;
import brownshome.modding.util.PredefinedLoadingStages;

import java.util.Collection;
import java.util.List;

@DefineMod(
		name = "childMod",
		version = "2.0.0",
		requirements = {
				@Requirement(name = "baseMod", version = "1.0.0", rule = DependencyRules.NEWER_OR_EQUAL)
		}
)
public class ChildMod extends Mod implements ChildModProvider {
	private static final ChildMod INSTANCE = new ChildMod();

	public static ChildMod provider() {
		return INSTANCE;
	}

	private ChildMod() {
		super(new ChildModInfo());
	}

	@Override
	protected Collection<LoadingStage> configureLoadingProcess() {
		Mod baseMod = namedMod("baseMod");

		return List.of(
				createLoadingStageRequest("Log information", () -> {
					Logger.logger().log(Severity.INFO, "%s initialized successfully with %s as parent mod.", this, baseMod);

					((BaseModAPI) baseMod).callAPI();
				}).after(baseMod, PredefinedLoadingStages.END)
		);
	}
}
