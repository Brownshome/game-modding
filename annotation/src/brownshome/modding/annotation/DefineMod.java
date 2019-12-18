package brownshome.modding.annotation;

import java.lang.annotation.*;

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
@Documented
public @interface DefineMod {
	String name();
	String version();
	Requirement[] requirements() default {};
}