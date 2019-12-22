package basemod.api;

import browngu.logging.Logger;
import browngu.logging.Severity;

public interface ChildModProvider {
	default void hi() {
		Logger.logger().log("hi");
	}
}
