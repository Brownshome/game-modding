package brownshome.modding.systemtest.library;

import browngu.logging.Logger;
import browngu.logging.Severity;

public class Library {
	public static void libraryCall() {
		Logger.logger().log(Severity.INFO, "Library call made");
	}
}
