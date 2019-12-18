package brownshome.modding.annotation;

import brownshome.modding.util.DependencyRules;

public @interface Requirement {
	String name();
	String version();
	DependencyRules rule() default DependencyRules.COMPATIBLE;
}
