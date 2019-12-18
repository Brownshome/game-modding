import javax.annotation.processing.Processor;

import brownshome.modding.annotation.processor.ModInfoGenerator;

module brownshome.modding.annotation.processor {
	requires brownshome.modding.annotation;
	requires java.compiler;
	requires com.squareup.javapoet;

	provides Processor with ModInfoGenerator;
}
