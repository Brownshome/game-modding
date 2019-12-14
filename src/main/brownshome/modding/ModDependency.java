package brownshome.modding;

public interface ModDependency {
	String modName();
	boolean isMetBy(ModVersion version);
}

