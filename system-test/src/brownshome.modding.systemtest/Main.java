package brownshome.modding.systemtest;

import browngu.logging.Logger;
import browngu.logging.Severity;
import brownshome.modding.ModLoader;
import brownshome.modding.ModLoadingException;
import brownshome.modding.modsource.ModSource;
import brownshome.modding.util.DependencyRules;
import brownshome.modding.util.RuleModDependency;
import brownshome.modding.util.SemanticModVersion;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

public class Main {
	public static void main(String... args) throws ModLoadingException, URISyntaxException, IOException {
		Logger.logger.addLoggingOutput(System.out, Severity.DEBUG);

		URL modAURL = Main.class.getResource("/mods");

		var modFolder = Paths.get(modAURL.toURI());

		if (!Files.exists(modFolder) || !Files.isDirectory(modFolder)) {
			throw new RuntimeException("This program must be run from a file system, not a JAR or network environment.");
		}

		var sources = Files.list(modFolder).map(f -> {
			try {
				return f.toUri().toURL();
			} catch (MalformedURLException e) {
				throw new RuntimeException(e);
			}
		}).map(ModSource::fromURL).collect(Collectors.toList());

		ModLoader modLoader = new ModLoader(ModSource.combine(sources));

		modLoader.loadMods(List.of(new RuleModDependency("childMod", SemanticModVersion.createVersion("2.0.0"), DependencyRules.EXACTLY)));
	}
}
