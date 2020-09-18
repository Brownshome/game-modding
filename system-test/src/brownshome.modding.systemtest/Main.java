package brownshome.modding.systemtest;

import browngu.logging.Logger;
import browngu.logging.Severity;
import brownshome.modding.ModLoader;
import brownshome.modding.ModLoadingException;
import brownshome.modding.modsource.ModSource;
import brownshome.modding.util.AnyMod;
import brownshome.modding.util.DependencyRules;
import brownshome.modding.util.RuleModDependency;
import brownshome.modding.util.SemanticModVersion;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.module.ModuleFinder;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Main {
	public static void main(String... args) throws ModLoadingException, URISyntaxException, IOException {
		Logger.logger().addLoggingOutput(System.out, Severity.DEBUG);

		var modFolder = Paths.get("build/mods");

		ModLoader modLoader = new ModLoader(ModSource.fromFolder(modFolder));

		modLoader.loadMods(List.of(
				new RuleModDependency("childMod", SemanticModVersion.createVersion("2.0.0"), DependencyRules.EXACTLY),
				new AnyMod("nosyMod")
		));
	}
}
