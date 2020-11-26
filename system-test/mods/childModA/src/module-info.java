import basemod.api.ChildModProvider;
import brownshome.modding.*;
import childmod.*;

module childmod {
	requires static brownshome.modding.annotation;

	requires browngu.logging;
	requires brownshome.modding;
	requires basemod;
	requires brownshome.modding.systemtest.library;

	provides Mod with ChildMod;
	provides ChildModProvider with ChildMod;
	provides ModInfo with ChildModInfo;
}