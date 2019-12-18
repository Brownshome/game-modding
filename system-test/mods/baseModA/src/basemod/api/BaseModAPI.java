package basemod.api;

import browngu.logging.Logger;
import browngu.logging.Severity;

public interface BaseModAPI {
	default void callAPI() {
		Logger.logger().log(Severity.INFO, "BaseMod API called A");
	}
}
