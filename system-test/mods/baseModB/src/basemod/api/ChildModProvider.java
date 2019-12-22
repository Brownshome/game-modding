package basemod.api;

import browngu.logging.Logger;

public interface ChildModProvider {
	default void hi() {
		Logger.logger().log("hi");
	}
}
