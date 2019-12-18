import brownshome.modding.*;
import childmod.*;

module childmod {
	requires brownshome.modding.annotation;
	requires browngu.logging;
	requires basemod;

	provides Mod with ChildMod;
	provides ModInfo with ChildModInfo;
}