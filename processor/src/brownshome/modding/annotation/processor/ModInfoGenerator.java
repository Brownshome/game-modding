package brownshome.modding.annotation.processor;

import brownshome.modding.ModDependency;
import brownshome.modding.ModInfo;
import brownshome.modding.annotation.DefineMod;
import brownshome.modding.util.DependencyRules;
import brownshome.modding.util.RuleModDependency;
import brownshome.modding.util.SemanticModVersion;

import com.squareup.javapoet.*;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.tools.Diagnostic;
import javax.tools.JavaFileManager;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.Writer;
import java.util.*;

@SupportedAnnotationTypes("*")
@SupportedSourceVersion(SourceVersion.RELEASE_11)
public class ModInfoGenerator extends AbstractProcessor {
	private final Map<String, StringBuilder> pendingWrites = new HashMap<>();

	public ModInfoGenerator() {  }

	@Override
	public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
		try {
			for (var element : roundEnv.getElementsAnnotatedWith(DefineMod.class)) {
				var definedModAnnotation = element.getAnnotation(DefineMod.class);

				createModInfo(definedModAnnotation, (TypeElement) element);
			}
		} catch (Exception e) {
			processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Unknown error occured: " + e);
		}

		if (roundEnv.processingOver()) {
			// Flush writes to disk, as service files may be written to multiple times
			flushWrites();
		}

		return false;
	}

	private void createModInfo(DefineMod definedModAnnotation, TypeElement originator) {
		if (originator.getNestingKind().isNested()) {
			processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "@DefineMod cannot be used on a nested class", originator);
			return;
		}

		var className = originator.getSimpleName() + "Info";

		var packageElement = (PackageElement) originator.getEnclosingElement();
		var packageName = packageElement.getQualifiedName().toString();

		var moduleElement = (ModuleElement) packageElement.getEnclosingElement();
		if (moduleElement == null) {
			processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "@DefineMod cannot be used in a non-modular environment", originator);
			return;
		}
		var moduleName = moduleElement.getQualifiedName().toString();

		var requirements = definedModAnnotation.requirements();

		processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, String.format("Generating '%s.%s'", packageName, className));

		var depListType = ParameterizedTypeName.get(List.class, ModDependency.class);
		var depArrayListType = ParameterizedTypeName.get(ArrayList.class, ModDependency.class);

		var requirementsBuilder = MethodSpec.methodBuilder("createRequirementsList")
				.addModifiers(Modifier.PRIVATE, Modifier.STATIC)
				.returns(depListType)
				.addStatement("var result = new $T()", depArrayListType);

		for (var requirement : requirements) {
			requirementsBuilder.addStatement("result.add(new $T($S, $T.createVersion($S), $T.$L))",
					RuleModDependency.class, requirement.name(), SemanticModVersion.class, requirement.version(), DependencyRules.class, requirement.rule());
		}

		requirementsBuilder.addStatement("return result");
		var requirementsMethod = requirementsBuilder.build();

		var infoConstructor = MethodSpec.constructorBuilder()
				.addModifiers(Modifier.PUBLIC)
				.addStatement("super($S, $T.createVersion($S), $S, $N())",
						definedModAnnotation.name(), SemanticModVersion.class, definedModAnnotation.version(), moduleName, requirementsMethod)
				.build();

		var infoClass = TypeSpec.classBuilder(className)
				.superclass(ModInfo.class)
				.addModifiers(Modifier.PUBLIC, Modifier.FINAL)
				.addMethod(infoConstructor)
				.addMethod(requirementsMethod)
				.build();

		var javaFile = JavaFile.builder(packageName, infoClass).build();

		try (Writer writer = processingEnv.getFiler().createSourceFile(packageName + "." + className, originator).openWriter()) {
			javaFile.writeTo(writer);
		} catch(IOException e) {
			processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Unable to write info class", originator);
			return;
		}

		writeLine("META-INF/services/brownshome.modding.Mod", originator.getQualifiedName().toString());
		writeLine("META-INF/services/brownshome.modding.ModInfo", packageName + "." + className);
	}

	private void writeLine(String file, String line) {
		pendingWrites.computeIfAbsent(file, unused -> new StringBuilder()).append(line).append("\n");
	}

	private void flushWrites() {
		for (var entry : pendingWrites.entrySet()) {
			try (Writer writer = processingEnv.getFiler().createResource(StandardLocation.CLASS_OUTPUT, "", entry.getKey()).openWriter()) {
				writer.write(entry.getValue().toString());
			} catch(IOException e) {
				processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Unable to write service data: " + e.getMessage());
				return;
			}
		}

		pendingWrites.clear();
	}
}
