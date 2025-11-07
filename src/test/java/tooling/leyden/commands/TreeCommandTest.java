package tooling.leyden.commands;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import tooling.leyden.aotcache.Element;
import tooling.leyden.aotcache.Information;
import tooling.leyden.commands.logparser.AOTMapParser;
import tooling.leyden.commands.logparser.TrainingLogParser;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest

class TreeCommandTest extends DefaultTest {

	@Test
	void getReferencedFromSymbolGraph() {
		final var loadFile = new LoadFileCommand();
		loadFile.setParent(getDefaultCommand());
		final var parser = new TrainingLogParser(loadFile);
		final var aotParser = new AOTMapParser(loadFile);

		aotParser.accept("0x00000008018dfad0: @@ Class             696 org.infinispan.rest.framework.impl.InvocationImpl");
		aotParser.accept("0x0000000800772850: @@ Class             528 java.lang.Object");
		aotParser.accept("0x0000000800e6d478: @@ Class             592 org.infinispan.security.AuthorizationPermission");
		aotParser.accept("0x000000080077e3b0: @@ Class             520 java.util.Set");
		aotParser.accept("0x00000008003483b0: @@ Class             520 java.lang.String");
		aotParser.accept("0x00000008003483b0: @@ Class             520 java.util.function.Function");
		aotParser.accept("0x0000000801e022e8: @@ Symbol            24 org/infinispan/rest/framework/impl/InvocationImpl");
		aotParser.accept("0x0000000801e022e8: @@ Symbol            24 Ljava/util/Set;");
		aotParser.accept("0x0000000801e022e8: @@ Symbol            24 java/lang/Object");

		parser.accept("[trace][aot,resolve              ] archived klass  CP entry [  2]: org/infinispan/rest/framework/impl/InvocationImpl unreg => java/lang/Object boot");
		parser.accept("[trace][aot,resolve              ] archived klass  CP entry [  8]: org/infinispan/rest/framework/impl/InvocationImpl unreg => org/infinispan/rest/framework/impl/InvocationImpl unreg");
		parser.accept("[trace][aot,resolve              ] archived field  CP entry [  7]: org/infinispan/rest/framework/impl/InvocationImpl => org/infinispan/rest/framework/impl/InvocationImpl.methods:Ljava/util/Set;");
		parser.accept("[trace][aot,resolve              ] archived field  CP entry [ 13]: org/infinispan/rest/framework/impl/InvocationImpl => org/infinispan/rest/framework/impl/InvocationImpl.paths:Ljava/util/Set;");
		parser.accept("[trace][aot,resolve              ] archived field  CP entry [ 16]: org/infinispan/rest/framework/impl/InvocationImpl => org/infinispan/rest/framework/impl/InvocationImpl.handler:Ljava/util/function/Function;");
		parser.accept("[trace][aot,resolve              ] archived field  CP entry [ 20]: " +
				"org/infinispan/rest/framework/impl/InvocationImpl => org/infinispan/rest/framework/impl/InvocationImpl.action:Ljava/lang/String;");
		parser.accept("[trace][aot,resolve              ] archived field  CP entry [ 24]: " +
				"org/infinispan/rest/framework/impl/InvocationImpl => org/infinispan/rest/framework/impl/InvocationImpl.name:Ljava/lang/String;");
		parser.accept("[trace][aot,resolve              ] archived field  CP entry [ 27]: " +
				"org/infinispan/rest/framework/impl/InvocationImpl => org/infinispan/rest/framework/impl/InvocationImpl.anonymous:Z");
		aotParser.accept("0x0000000801e022e8: @@ Symbol            24 java/lang/function/Function");
		aotParser.accept("0x0000000801e022e8: @@ Symbol            24 java/lang/String");
		aotParser.accept("0x0000000800e6d478: @@ Symbol            592 org/infinispan/security/AuthorizationPermission");

		TreeCommand command = new TreeCommand();
		command.parent = getDefaultCommand();
		command.parameters = new CommonParameters();
		command.parameters.types = new String[]{"Class", "Object"};
		command.reverse = false;
		command.level = 2;
		command.max = 100;

		Element root = Information.getMyself()
				.getElements("org.infinispan.rest.framework.impl.InvocationImpl", null, null, true, true, "Class")
				.findAny().get();
		Set<Element> elements = command.getElementsReferencingThisOne(root, new HashSet<>());
		assertEquals(4, elements.size());
		elements.stream().allMatch(e -> e.getType().equalsIgnoreCase("Class") || e.getType().equalsIgnoreCase("Object"));

		Element reversedRoot = Information.getMyself()
				.getElements("java.util.Set", null, null, true, true, "Class")
				.findAny().get();
		command.reverse = true;
		elements = command.getElementsReferencingThisOne(reversedRoot, new HashSet<>());
		assertEquals(1, elements.size());
		assertTrue(elements.contains(root));

		//Now include Symbols
		command.parameters.types = new String[]{"Class", "Object", "Symbol"};
		command.reverse = false;
		elements = command.getElementsReferencingThisOne(root, new HashSet<>());
		assertEquals(1, elements.size());
		elements.stream().allMatch(e -> e.getType().equalsIgnoreCase("Symbol"));
		elements.stream().allMatch(e -> e.getKey().equalsIgnoreCase("org/infinispan/rest/framework/impl/InvocationImpl"));

		//Next level (based on Symbol)
		elements = command.getElementsReferencingThisOne(elements.iterator().next(), new HashSet<>());
		assertEquals(12, elements.size());
		elements.stream().allMatch(e -> e.getType().equalsIgnoreCase("Class") || e.getType().equalsIgnoreCase("Symbol"));

		command.reverse = true;
		elements = command.getElementsReferencingThisOne(reversedRoot, new HashSet<>());
		assertEquals(1, elements.size());
		elements.stream().allMatch(e -> e.getType().equalsIgnoreCase("Symbol"));
		elements.stream().allMatch(e -> e.getKey().equalsIgnoreCase("java/util/Set"));

		//Next level (based on Symbol)
		elements = command.getElementsReferencingThisOne(elements.iterator().next(), new HashSet<>());
		assertEquals(2, elements.size());
		elements.stream().anyMatch(e -> e.getType().equalsIgnoreCase("Class"));
		elements.stream().anyMatch(e -> e.getType().equalsIgnoreCase("Symbol"));
	}
}