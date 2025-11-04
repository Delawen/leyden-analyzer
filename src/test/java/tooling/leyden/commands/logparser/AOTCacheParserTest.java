package tooling.leyden.commands.logparser;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import tooling.leyden.aotcache.ClassObject;
import tooling.leyden.aotcache.ConstantPoolObject;
import tooling.leyden.aotcache.Element;
import tooling.leyden.aotcache.Information;
import tooling.leyden.aotcache.MethodObject;
import tooling.leyden.aotcache.ReferencingElement;
import tooling.leyden.commands.DefaultTest;
import tooling.leyden.commands.LoadFileCommand;

import java.io.BufferedReader;
import java.io.File;
import java.io.StringReader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class AOTCacheParserTest extends DefaultTest {

	private static AOTMapParser aotCacheParser;
	private static Information information;
	private static LoadFileCommand loadFile;


	@BeforeAll
	static void init() {
		loadFile = new LoadFileCommand();
		loadFile.setParent(getDefaultCommand());
		information = loadFile.getParent().getInformation();
		aotCacheParser = new AOTMapParser(loadFile);
	}

	@Test
	void accept() throws Exception {
		File file = new File(getClass().getResource("aot.map").getPath());
		final var aotCache = getDefaultCommand().getInformation();
		getSystemRegistry().execute("load aotCache " + file.getAbsolutePath());
		assertTrue(aotCache.getAll().size() > 0);
		assertEquals(0, aotCache.getWarnings().size());

		//Now check individual values
		//Skip classes because they may have been indirectly generated
		aotCache.getAll().parallelStream().filter(e -> !e.getType().equalsIgnoreCase("Class")).forEach(e -> {
			assertNotNull(e.getAddress(), "Address of " + e + " shouldn't be null.");
			assertNotNull(e.getKey(), "Key of " + e + " shouldn't be null.");
			assertNotNull(e.getSize(), "Size of " + e + " shouldn't be null.");
			assertNotNull(e.getType(), "Type of " + e + " shouldn't be null.");
			assertEquals(1, e.getSources().size(), "We shouldn't have more than one source here " + e.getSources().stream().reduce((s, s2) -> s + ", " + s2));
		});

		assertEquals(655, aotCache.getElements(null, null, null, true, false, "Symbol").count());
		assertEquals(114, aotCache.getElements(null, null, null, true, false, "ConstantPool").count());
		assertEquals(494 + 5, aotCache.getElements(null, null, null, true, true, "Class").count());
		assertEquals(5927, aotCache.getElements(null, null, null, true, false, "Method").count());
		assertEquals(1385, aotCache.getElements(null, null, null, true, false, "ConstMethod").count());
	}

	@Test
	void acceptMiscData() {
		aotCacheParser.accept("0x00000008049a8410: @@ Misc data 1985520 bytes");
		Element e = information.getByAddress("0x00000008049a8410");
		assertNotNull(e);
		assertEquals("Misc-data", e.getType());
		assertEquals(1985520, e.getSize());

	}

	@Test
	void acceptObjectsWithReferences() {
		var classObject = new ClassObject("java.lang.Float");
		information.addAOTCacheElement(classObject, "test");

		classObject = new ClassObject("java.lang.String");
		information.addAOTCacheElement(classObject, "test");

		classObject = new ClassObject("java.lang.String$CaseInsensitiveComparator");
		information.addAOTCacheElement(classObject, "test");

		aotCacheParser.accept("0x00000000fff63458: @@ Object (0xfff63458) java.lang.String$CaseInsensitiveComparator");
		aotCacheParser.accept("0x00000000fff632f0: @@ Object (0xfff632f0) [I length: 0");
		aotCacheParser.accept("0x00000000fff62900: @@ Object (0xfff62900) java.lang.Float");
		aotCacheParser.accept("0x00000000ffd0a4c8: @@ Object (0xffd0a4c8) java.lang.String \"| resolve\"");
		aotCacheParser.accept("0x00000000ffd11068: @@ Object (0xffd11068) java.lang.String \" (success)\"");
		aotCacheParser.accept("0x00000000ffd07d48: @@ Object (0xffd07d48) java.lang.String \"    \"");

		assertEquals(9, information.getAll().size());
		final var objects = information.getElements(null, null, null, true, false, "Object").toList();
		assertEquals(6, information.getElements(null, null, null, true, false, "Object").count());
		for (Element e : objects) {
			assertTrue(e instanceof ReferencingElement);
			ReferencingElement re = (ReferencingElement) e;
			if (!re.getKey().equals("(0xfff632f0) [I length: 0")) {
				assertTrue(!re.getReferences().isEmpty());
				assertTrue(re.getKey().contains(re.getReferences().getFirst().getKey()));
			}
		}

	}

	@Test
	void acceptSymbol() {
		aotCacheParser.accept("0x0000000800dae648: @@ Class             616 java.security.InvalidAlgorithmParameterException");
		aotCacheParser.accept("0x00000008020acbe8: @@ Symbol            56 java/security/InvalidAlgorithmParameterException");
		aotCacheParser.accept("0x000000080225a980: @@ Symbol            56 Ljava/security/InvalidAlgorithmParameterException;");

		assertEquals(3, information.getAll().size());
		assertEquals(2, information.getElements(null, null, null, true, false, "Symbol").count());
		assertEquals(1, information.getElements(null, null, null, true, false, "Class").count());
		for (Element e : information.getElements(null, null, null, true, false, "Symbol").toList()) {
			assertTrue(e instanceof ReferencingElement);
			assertEquals(0, ((ReferencingElement) e).getReferences().size());
		}
		ClassObject classObject = (ClassObject) information.getElements(null, null, null, true, false, "Class").findAny().get();
		assertEquals(2, classObject.getSymbols().size());

	}


	@Test
	void acceptObjectsWithExplicitReference() {
		var classObject = new ClassObject("java.lang.String");
		information.addAOTCacheElement(classObject, "test");

		aotCacheParser.accept("0x0000000801de8110: @@ Symbol            24 java/lang/String");

		aotCacheParser.accept("0x0000000800a8efe8: @@ Class             536 sun.util.locale.BaseLocale");
		aotCacheParser.accept("0x0000000800a8f258: @@ ConstantPoolCache 64 sun.util.locale.BaseLocale");

		aotCacheParser.accept("0x0000000800a8f3c8: @@ Class             512 [Lsun.util.locale.BaseLocale;");
		aotCacheParser.accept("0x0000000800a98270: @@ Class             568 sun.util.locale.BaseLocale$1");

		aotCacheParser.accept("0x0000000800a98500: @@ ConstantPoolCache 64 sun.util.locale.BaseLocale$1");

		aotCacheParser.accept("0x0000000801f74b70: @@ Symbol            32 sun/util/locale/BaseLocale");
		aotCacheParser.accept("0x0000000801f74bb0: @@ Symbol            40 [Lsun/util/locale/BaseLocale;");
		aotCacheParser.accept("0x0000000801f74bd8: @@ Symbol            72 (Lsun/util/locale/BaseLocale;Lsun/util/locale/LocaleExtensions;)V");
		aotCacheParser.accept("0x0000000801f74c78: @@ Symbol            40 Lsun/util/locale/BaseLocale;");
		aotCacheParser.accept("0x0000000801f74cf8: @@ Symbol            112 (Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Lsun/util/locale/BaseLocale;");
		aotCacheParser.accept("0x0000000801f74e88: @@ Symbol            88 (Lsun/util/locale/BaseLocale;Lsun/util/locale/LocaleExtensions;)Ljava/util/Locale;");
		aotCacheParser.accept("0x0000000801f754d0: @@ Symbol            104 (Lsun/util/locale/BaseLocale;Lsun/util/locale/LocaleExtensions;)Lsun/util/locale/LanguageTag;");
		aotCacheParser.accept("0x0000000801f75708: @@ Symbol            40 ()Lsun/util/locale/BaseLocale;");
		aotCacheParser.accept("0x0000000801f76418: @@ Symbol            72 Ljava/util/Map<Lsun/util/locale/BaseLocale;Ljava/util/Locale;>;");
		aotCacheParser.accept("0x0000000801f77410: @@ Symbol            72 (Ljava/lang/String;Ljava/lang/String;)Lsun/util/locale/BaseLocale;");
		aotCacheParser.accept("0x0000000801f774b0: @@ Symbol            40 sun/util/locale/BaseLocale$1");
		aotCacheParser.accept("0x0000000801f77580: @@ Symbol            104 Ljava/util/function/Supplier<Ljdk/internal/util/ReferencedKeySet<Lsun/util/locale/BaseLocale;>;>;");
		aotCacheParser.accept("0x0000000801f77658: @@ Symbol            40 Lsun/util/locale/BaseLocale$1;");
		aotCacheParser.accept("0x0000000801f77680: @@ Symbol            80 ()Ljdk/internal/util/ReferencedKeySet<Lsun/util/locale/BaseLocale;>;");
		aotCacheParser.accept("0x0000000801f776d0: @@ Symbol            128 Ljava/lang/Object;Ljava/util/function/Supplier<Ljdk/internal/util/ReferencedKeySet<Lsun/util/locale/BaseLocale;>;>;");
		aotCacheParser.accept("0x000000080208db10: @@ Symbol            112 (Lsun/util/locale/BaseLocale;Lsun/util/locale/LocaleExtensions;)Lsun/util/locale/InternalLocaleBuilder;");
		aotCacheParser.accept("0x0000000802093c48: @@ Symbol            112 Ljdk/internal/util/ReferencedKeyMap<Lsun/util/locale/BaseLocale;Ljava/util/List<Ljava/util/Locale;>;>;");
		aotCacheParser.accept("0x0000000802093d78: @@ Symbol            56 (Lsun/util/locale/BaseLocale;)Ljava/util/List;");
		aotCacheParser.accept("0x0000000802093e38: @@ Symbol            72 (Lsun/util/locale/BaseLocale;)Ljava/util/List<Ljava/util/Locale;>;");

		aotCacheParser.accept("0x0000000802ff83e8: @@ ConstantPool      2456 sun.util.locale.BaseLocale");
		aotCacheParser.accept("0x0000000802f88a70: @@ ConstantPool      400 sun.util.locale.BaseLocale$1");

		aotCacheParser.accept("0x00000000ffe06dc0: @@ Object (0xffe06dc0) [Lsun.util.locale.BaseLocale; length: 19");
		aotCacheParser.accept("0x00000000ffe06e20: @@ Object (0xffe06e20) sun.util.locale.BaseLocale");
		aotCacheParser.accept("0x00000000ffe06e58: @@ Object (0xffe06e58) sun.util.locale.BaseLocale");
		aotCacheParser.accept("0x00000000ffe06e90: @@ Object (0xffe06e90) sun.util.locale.BaseLocale");
		aotCacheParser.accept("0x00000000ffe94558: @@ Object (0xffe94558) java.lang.String \"sun.util.locale.BaseLocale\"");
		aotCacheParser.accept("0x00000000ffef4720: @@ Object (0xffef4720) java.lang.Class Lsun/util/locale/BaseLocale$1;");
		aotCacheParser.accept("0x00000000ffefd1e8: @@ Object (0xffefd1e8) java.lang.Class Lsun/util/locale/BaseLocale;");
		aotCacheParser.accept("0x00000000ffefd288: @@ Object (0xffefd288) java.lang.Class [Lsun/util/locale/BaseLocale;");

		assertEquals(2, information.getElements(null, null, null, true, false, "ConstantPool").count());
		assertTrue(information.getElements(null, null, null, true, false, "ConstantPool")
				.allMatch(cp -> ((ConstantPoolObject) cp).getConstantPoolCacheAddress() != null));

		assertEquals(20, information.getElements(null, null, null, true, false, "Symbol").count());
		assertEquals(8, information.getElements(null, null, null, true, false, "Object").count());

		for (Element e : information.getElements(null, null, null, true, false,
				"Object").toList()) {
			assertTrue(e instanceof ReferencingElement);
			assertTrue(((ReferencingElement) e).getReferences().size() > 0, e + " should have at least a reference");
		}

		assertEquals(3 + 1, information.getElements(null, null, null, true, false, "Class").count());
		information.getElements(null, null, null, true, false, "Class")
				.allMatch(c -> ((ClassObject) c).getSymbols().size() > 0);

		//Make sure we didn'0t create unexpected assets in the cache:
		assertEquals(2 + 20 + 8 + 3 + 1, information.getAll().size());

	}


	@Test
	void acceptMethodDataAndMethodCounters() {

		aotCacheParser.accept("0x0000000800772d58: @@ Class             528 java.lang.Object");
		aotCacheParser.accept("0x0000000800799620: @@ Class             528 jdk.internal.misc.CDS");
		aotCacheParser.accept("0x0000000801d92878: @@ Method            88 void jdk.internal.misc.CDS.keepAlive(java.lang.Object)");
		aotCacheParser.accept("0x0000000801d928d0: @@ MethodData        568 void jdk.internal.misc.CDS.keepAlive(java.lang.Object)");
		aotCacheParser.accept("0x0000000801d92b08: @@ MethodCounters    64 void jdk.internal.misc.CDS.keepAlive(java.lang.Object)");
		aotCacheParser.accept("0x00000008048c5c60: @@ ConstMethod       152 void jdk.internal.misc.CDS.keepAlive(java.lang.Object)");

		aotCacheParser.accept("0x0000000801f5fb68: @@ MethodData        296 void java.lang.Long.<init>(long)");
		aotCacheParser.accept("0x0000000801f5fd90: @@ MethodData        288 void java.lang.Short.<init>(short)");
		aotCacheParser.accept("0x0000000801f6a670: @@ MethodData        672 int jdk.internal.util.ArraysSupport.hugeLength(int, int)");
		aotCacheParser.accept("0x0000000801f6a968: @@ MethodData        408 int jdk.internal.util.ArraysSupport.utf16hashCode(int, byte[], int, int)");
		aotCacheParser.accept("0x0000000801f6b188: @@ MethodData        328 int jdk.internal.util.ArraysSupport.hashCode(int, int[], int, int)");
		aotCacheParser.accept("0x0000000801f6b328: @@ MethodData        328 int jdk.internal.util.ArraysSupport.hashCode(int, short[], int, int)");
		aotCacheParser.accept("0x0000000801f6b4c8: @@ MethodData        328 int jdk.internal.util.ArraysSupport.hashCode(int, char[], int, int)");
		aotCacheParser.accept("0x0000000801f6f848: @@ MethodData        584 void java.util.ImmutableCollections$Set12.<init>(java.lang.Object, java.lang.Object)");
		aotCacheParser.accept("0x0000000801f895e0: @@ MethodCounters    64 java.util.Optional java.lang.VersionProps.optional()");
		aotCacheParser.accept("0x0000000801f89678: @@ MethodCounters    64 java.util.Optional java.lang.VersionProps.build()");
		aotCacheParser.accept("0x0000000801f89710: @@ MethodCounters    64 java.util.Optional java.lang.VersionProps.pre()");
		aotCacheParser.accept("0x0000000801f897a8: @@ MethodCounters    64 java.util.List java.lang.VersionProps.versionNumbers()");
		aotCacheParser.accept("0x0000000801f89840: @@ MethodCounters    64 java.util.Optional java.lang.VersionProps.optionalOf(java.lang.String)");
		aotCacheParser.accept("0x0000000801f898d8: @@ MethodCounters    64 java.util.List java.lang.VersionProps.parseVersionNumbers(java.lang.String)");

		var elements = information.getElements(null, null, null, true, false, "MethodData").toList();
		assertEquals(9, elements.size());
		for (Element e : elements) {
			assertTrue(((ReferencingElement) e).getReferences().size() > 0);
		}

		elements = information.getElements(null, null, null, true, false, "MethodCounters").toList();
		assertEquals(7, elements.size());
		for (Element e : elements) {
			assertTrue(((ReferencingElement) e).getReferences().size() > 0);
		}

		elements = information.getElements("void jdk.internal.misc.CDS.keepAlive(java.lang.Object)",
				null, null, true, false, "Method").toList();
		var method = elements.getFirst();
		assertNotNull(method.getClass());
		assertEquals("jdk.internal.misc.CDS", ((MethodObject) method).getClassObject().getKey());
		elements = information.getElements("void jdk.internal.misc.CDS.keepAlive(java.lang.Object)",
				null, null, true, false, "ConstMethod", "MethodData", "MethodCounters").toList();

		assertEquals(3, elements.size());
		for (Element e : elements) {
			if (e instanceof ReferencingElement re) {
				assertTrue(re.getReferences().contains(method));
			}
		}
	}

	@Test
	void acceptLambda() {
		aotCacheParser.accept("0x0000000800ede8d0: @@ Class             1248 sun.security.pkcs11.SunPKCS11");
		aotCacheParser.accept("0x00000008019c1890: @@ Class             584 sun.security.pkcs11.SunPKCS11$$Lambda/0x8000000cf");
		aotCacheParser.accept("0x00000008019c1b48: @@ Method            88 void sun.security.pkcs11.SunPKCS11$$Lambda/0x8000000cf.<init>()");
		aotCacheParser.accept("0x00000008019c1be0: @@ Method            88 java.lang.Object sun.security.pkcs11.SunPKCS11$$Lambda/0x8000000cf.apply(java.lang.Object)");

		var sunPKCS = information.getElements("sun.security.pkcs11.SunPKCS11", null, null, false, false, "Class").toList();
		var lambdaClass = information.getElements("sun.security.pkcs11.SunPKCS11$$Lambda/0x8000000cf", null, null, false, false, "Class").toList();
		assertFalse(lambdaClass.isEmpty());
		ReferencingElement lambda = (ReferencingElement) lambdaClass.getFirst();
		assertEquals(1, lambda.getReferences().size());
		assertEquals(lambda.getReferences().getFirst(), sunPKCS.getFirst());
		var methods = information.getElements(null, null, null, false, false, "Method").toList();
		for (Element method : methods) {
			assertEquals(lambda, ((MethodObject) method).getClassObject());
		}
	}

	@Test
	void acceptTrainingData() {
		aotCacheParser.accept("0x0000000800ede8d0: @@ Class             1248 java.util.logging.LogManager");
		aotCacheParser.accept("0x0000000801bc7e40: @@ KlassTrainingData 40 java.util.logging.LogManager");
		aotCacheParser.accept("0x0000000801bcb1a8: @@ KlassTrainingData 40 java.lang.classfile.AttributeMapper$AttributeStability");
		aotCacheParser.accept("0x00000008019c1b48: @@ Class            88 java.lang.classfile.AttributeMapper$AttributeStability");
		aotCacheParser.accept("0x0000000800b5b3a8: @@ Method            88 org.apache.logging.log4j.spi.LoggerContext org.apache.logging.log4j.LogManager.getContext(boolean)");
		aotCacheParser.accept("0x0000000801a23fc8: @@ MethodTrainingData 96 org.apache.logging.log4j.spi.LoggerContext org.apache.logging.log4j.LogManager.getContext(boolean)");

		var klassTrainingData = information.getElements(null, null, null, false, false, "KlassTrainingData").toList();
		assertEquals(2, klassTrainingData.size());

		for (Element e : klassTrainingData) {
			ReferencingElement re = (ReferencingElement) e;
			assertEquals(1, re.getReferences().size());
			var classObj = re.getReferences().getFirst();
			assertInstanceOf(ClassObject.class, classObj);
			assertEquals(classObj.getKey(), re.getKey());
		}

		var methodTrainingData = information.getElements(null, null, null, false, false, "MethodTrainingData").toList();
		assertEquals(1, methodTrainingData.size());

		for (Element e : methodTrainingData) {
			ReferencingElement re = (ReferencingElement) e;
			assertEquals(1, re.getReferences().size());
			assertEquals(re.getReferences().getFirst().getKey(), re.getKey());
		}

		//Now check we don't break on empty class name
		information.clear();

		aotCacheParser.accept("0x0000000801d14768: @@ KlassTrainingData 40");
		aotCacheParser.accept("0x0000000801cd0518: @@ MethodTrainingData 96");

		var trainingData = information.getElements(null, null, null, false, false, "MethodTrainingData",
				"KlassTrainingData").toList();
		assertEquals(2, trainingData.size());

		for (Element e : trainingData) {
			ReferencingElement re = (ReferencingElement) e;
			assertEquals(0, re.getReferences().size());
		}
	}


	@Test
	void acceptCompileTrainingData() {
		aotCacheParser.accept("0x00000008007cd538: @@ Class             1448 java.util.concurrent.ConcurrentHashMap");
		aotCacheParser.accept("0x00000008019d8e00: @@ MethodTrainingData 96 int java.util.concurrent.ConcurrentHashMap.spread(int)");
		aotCacheParser.accept("0x0000000801cb2218: @@ MethodCounters    64 int java.util.concurrent.ConcurrentHashMap.spread(int)");
		aotCacheParser.accept("0x0000000801cb2258: @@ Method            88 int java.util.concurrent.ConcurrentHashMap.spread(int)");
		aotCacheParser.accept("0x0000000801cb22b0: @@ MethodData        248 int java.util.concurrent.ConcurrentHashMap.spread(int)");
		aotCacheParser.accept("0x0000000801cb23a8: @@ MethodCounters    64 int java.util.concurrent.ConcurrentHashMap.spread(int)");
		aotCacheParser.accept("0x0000000801cb23e8: @@ CompileTrainingData 80 4 int java.util.concurrent.ConcurrentHashMap.spread(int)");
		aotCacheParser.accept("0x0000000801cb2438: @@ CompileTrainingData 80 3 int java.util.concurrent.ConcurrentHashMap.spread(int)");
		aotCacheParser.accept("0x000000080471b9c8: @@ ConstMethod       88 int java.util.concurrent.ConcurrentHashMap.spread(int)");

		var compileTrainingData = information.getElements(null, null, null, false, false, "CompileTrainingData").toList();
		assertEquals(2, compileTrainingData.size());

		MethodObject method =
				(MethodObject) information.getElements("int java.util.concurrent.ConcurrentHashMap.spread(int)",
								null, null, false, false, "Method")
						.findAny().get();
		assertEquals(2, method.getCompileTrainingData().size());
		assertNotNull(method.getCompileTrainingData().get(3));
		assertNotNull(method.getCompileTrainingData().get(4));

		for (Element e : compileTrainingData) {
			ReferencingElement re = (ReferencingElement) e;
			assertEquals(1, re.getReferences().size());
			assertEquals(method, re.getReferences().getFirst());
		}

		//Now check we don't break on empty class name
		information.clear();

		aotCacheParser.accept("0x0000000801cb2438: @@ CompileTrainingData 80");

		var trainingData = information.getElements(null, null, null, true, true, "CompileTrainingData").toList();
		assertEquals(1, trainingData.size());

		for (Element e : trainingData) {
			ReferencingElement re = (ReferencingElement) e;
			assertEquals(0, re.getReferences().size());
		}
	}

	@Test
	void detectClassLoaders() {
		String mapfile =
				"""
						0x00000008007f4290: @@ Class             688 java.lang.String
						0x00000008008057c8: @@ Class             560 java.lang.module.ModuleDescriptor
						0x0000000800932e00: @@ Class             560 java.lang.module.ModuleDescriptor$Version
						0x00000008007eee30: @@ Class             512 [Ljava.lang.Object;
						0x000000080084e2e8: @@ Class             528 jdk.internal.loader.ClassLoaders
						0x0000000800851090: @@ Class             840 jdk.internal.loader.ClassLoaders$AppClassLoader
						0x00000008008518b0: @@ Class             832 jdk.internal.loader.ClassLoaders$PlatformClassLoader
						0x0000000800853708: @@ Class             832 jdk.internal.loader.ClassLoaders$BootClassLoader
						0x00000008008b0a08: @@ Class             1632 java.util.ArrayList
						""";

		BufferedReader reader = new BufferedReader(new StringReader(mapfile));
		reader.lines().forEach(aotCacheParser::accept);

		assertEquals(9, information.getElements(null, null, null, true, true, "Class").count());
		assertEquals(4, information.getElements(null, null, null, true, true, "Class")
				.filter(e -> ((ClassObject)e).isClassLoader()).count());

		assertTrue(information.getElements(null, null, null, true, true, "Class")
				.filter(e -> ((ClassObject)e).isClassLoader())
				.allMatch(e -> ((ClassObject) e).getPackageName().equalsIgnoreCase("jdk.internal.loader")));
	}

	@Test
	void objectBasedDependencyGraph() {
		String mapfile =
				"""
						0x00000008007f4290: @@ Class             688 java.lang.String
						0x00000008007eee30: @@ Class             512 [Ljava.lang.Object;
						0x00000008007f08c0: @@ Class             512 [B
						0x00000008007f4648: @@ Class             512 [Ljava.lang.String;
						0x00000008007f5a48: @@ Class             776 java.lang.Class
						0x00000008008057c8: @@ Class             560 java.lang.module.ModuleDescriptor
						0x000000080081b748: @@ Class             648 java.lang.Integer
						0x000000080081be80: @@ Class             512 [Ljava.lang.Integer;
						0x000000080084e2e8: @@ Class             528 jdk.internal.loader.ClassLoaders
						0x0000000800851090: @@ Class             840 jdk.internal.loader.ClassLoaders$AppClassLoader
						0x00000008008518b0: @@ Class             832 jdk.internal.loader.ClassLoaders$PlatformClassLoader
						0x0000000800853708: @@ Class             832 jdk.internal.loader.ClassLoaders$BootClassLoader
						0x00000008008b0a08: @@ Class             1632 java.util.ArrayList
						0x0000000800932e00: @@ Class             560 java.lang.module.ModuleDescriptor$Version
						
						0x00000000ffd00000: @@ Object (0xffd00000) [Ljava.lang.Object; length: 5391
						 - klass: 'java/lang/Object'[] 0x00000008007eee30
						 root[   0]: 0x00000000ffd05450 (0xffd05450) [Ljava.lang.Integer; length: 256
						
						0x00000000ffd05450: @@ Object (0xffd05450) [Ljava.lang.Integer; length: 256
						 - klass: 'java/lang/Integer'[] 0x000000080081be80
						 -   0: 0x00000000ffe5d0a0 (0xffe5d0a0) java.lang.Integer
						
						0x00000000ffd074c0: @@ Object (0xffd074c0) java.lang.module.ModuleDescriptor
						 - klass: 'java/lang/module/ModuleDescriptor' 0x00000008008057c8
						 - fields (8 words):
						 - private transient 'hash' 'I' @12  -954841806 (0xc7164532)
						 - private final 'open' 'Z' @16  false (0x00)
						 - private final 'automatic' 'Z' @17  false (0x00)
						 - private final 'name' 'Ljava/lang/String;' @20 0x00000000ffd07500 (0xffd07500) java.lang.String "jdk.internal.opt"
						 - private final 'version' 'Ljava/lang/module/ModuleDescriptor$Version;' @24 0x00000000ffd07518 (0xffd07518) java.lang.module.ModuleDescriptor$Version
						 - private final 'rawVersionString' 'Ljava/lang/String;' @28 null
						
						0x00000000ffd07500: @@ Object (0xffd07500) java.lang.String "jdk.internal.opt"
						 - klass: 'java/lang/String' 0x00000008007f4290
						 - fields (3 words):
						 - private 'hash' 'I' @12  -1098610593 (0xbe84885f)
						 - private final 'coder' 'B' @16  0 (0x00)
						 - private 'hashIsZero' 'Z' @17  false (0x00)
						 - injected 'flags' 'B' @18  1 (0x01)
						 - private final 'value' '[B' @20 0x00000000ffe61c80 (0xffe61c80) [B length: 16
						
						0x00000000ffe61c80: @@ Object (0xffe61c80) [B length: 16
						 - klass: {type array byte} 0x00000008007f08c0
						 -   0: 6a j
						 -   1: 64 d
						 -   2: 6b k
						 -   3: 2e .
						 -   4: 69 i
						 -   5: 6e n
						 -   6: 74 t
						 -   7: 65 e
						 -   8: 72 r
						 -   9: 6e n
						 -  10: 61 a
						 -  11: 6c l
						 -  12: 2e .
						 -  13: 6f o
						 -  14: 70 p
						 -  15: 74 t
						
						0x00000000ffd07ba0: @@ Object (0xffd07ba0) [Ljava.lang.Object; length: 2
						 - klass: 'java/lang/Object'[] 0x00000008007eee30
						 -   0: 0x00000000ffd07500 (0xffd07500) java.lang.String "jdk.internal.opt"
						 -   1: null
						
						
						0x00000000ffd07518: @@ Object (0xffd07518) java.lang.module.ModuleDescriptor$Version
						 - klass: 'java/lang/module/ModuleDescriptor$Version' 0x0000000800932e00
						 - fields (4 words):
						 - private final 'sequence' 'Ljava/util/List;' @16 0x00000000ffd07550 (0xffd07550) java.util.ArrayList
						 - resolved_references: 0x00000000ffd07ba0 (0xffd07ba0) [Ljava.lang.Object; length: 18
						0x00000000ffd07518:   00000001 00000000 00932e00 ffd07538 ffd07550 ffd07588 ffd075d0 00000000   ............8u..Pu...u...u......
					
						
						0x00000000ffd07550: @@ Object (0xffd07550) java.util.ArrayList
						 - klass: 'java/util/ArrayList' 0x00000008008b0a08
						 - fields (3 words):
						 - protected transient 'modCount' 'I' @12  1 (0x00000001)
						 - private 'size' 'I' @16  1 (0x00000001)
						 - transient 'elementData' '[Ljava/lang/Object;' @20 0x00000000ffd07568 (0xffd07568) [Ljava.lang.Object; length: 4
						
						0x00000000ffd07568: @@ Object (0xffd07568) [Ljava.lang.Object; length: 4
						 - klass: 'java/lang/Object'[] 0x00000008007eee30
						 -   0: 0x00000000ffe5d0a0 (0xffe5d0a0) java.lang.Integer
						 -   1: null
						 -   2: null
						 -   3: null
						0x00000000ffd07568:   00000001 00000000 007eee30 00000004 ffe5d0a0 00000000 00000000 00000000   ........0.~.....................
						
						0x00000000ffe5d0a0: @@ Object (0xffe5d0a0) java.lang.Integer
						 - klass: 'java/lang/Integer' 0x000000080081b748
						 - fields (2 words):
						 - private final 'value' 'I' @12  26 (0x0000001a)
						  - resolved_references: null- ---- static fields (1):
						
						0x00000000ffdf4f38: @@ Object (0xffdf4f38) java.lang.Class Ljava/util/ArrayList; (aot-inited)
						 - klass: 'java/lang/Class' 0x00000008007f5a48
						 - fields (18 words):
						 - private volatile transient 'classRedefinedCount' 'I' @12  0 (0x00000000)
						 - injected 'klass' 'J' @16  34368850440 (0x00000008008b0a08)
						 - injected 'array_klass' 'J' @24  0 (0x0000000000000000)
						 - injected 'oop_size' 'I' @32  18 (0x00000012)
						 - injected 'static_oop_field_count' 'I' @36  2 (0x00000002)
						 - private final transient 'modifiers' 'C' @40    1 (0x0001)
						 - private final transient 'classFileAccessFlags' 'C' @42  ! 33 (0x0021)
						 - private final transient 'primitive' 'Z' @44  false (0x00)
						 - private transient 'name' 'Ljava/lang/String;' @52 null
						
						""";

		BufferedReader reader = new BufferedReader(new StringReader(mapfile));
		reader.lines().forEach(aotCacheParser::accept);
		aotCacheParser.postProcessing();

		assertEquals(14, information.getElements(null, null, null, true, true, "Class").count());
		assertEquals(11, information.getElements(null, null, null, true, true, "Object").count());

		Element e = information.getByAddress("0x00000000ffd00000");
		assertEquals("Object", e.getType());
		assertTrue(e.isHeapRoot());
		assertTrue(((ReferencingElement)e).getReferences().stream()
				.anyMatch(ref -> ref.getAddress().equalsIgnoreCase("0x00000008007eee30")));
		assertTrue(((ReferencingElement)e).getReferences().stream()
				.anyMatch(ref -> ref.getAddress().equalsIgnoreCase("0x00000000ffd05450")));
		assertEquals(2, ((ReferencingElement)e).getReferences().size());

		e = information.getByAddress("0x00000000ffd05450");
		assertEquals("Object", e.getType());
		assertTrue(e.isHeapRoot());
		assertTrue(((ReferencingElement)e).getReferences().stream()
				.anyMatch(ref -> ref.getAddress().equalsIgnoreCase("0x000000080081be80")));
		assertTrue(((ReferencingElement)e).getReferences().stream()
				.anyMatch(ref -> ref.getAddress().equalsIgnoreCase("0x00000000ffe5d0a0")));
		assertEquals(2, ((ReferencingElement)e).getReferences().size());

		e = information.getByAddress("0x00000000ffd074c0");
		assertEquals("Object", e.getType());
		assertFalse(e.isHeapRoot());
		assertTrue(((ReferencingElement)e).getReferences().stream()
				.anyMatch(ref -> ref.getAddress().equalsIgnoreCase("0x00000008008057c8")));
		assertTrue(((ReferencingElement)e).getReferences().stream()
				.anyMatch(ref -> ref.getAddress().equalsIgnoreCase("0x00000000ffd07500")));
		assertTrue(((ReferencingElement)e).getReferences().stream()
				.anyMatch(ref -> ref.getAddress().equalsIgnoreCase("0x00000000ffd07518")));
		assertTrue(((ReferencingElement)e).getReferences().stream()
				.anyMatch(ref -> ref.getAddress().equalsIgnoreCase("0x00000008007f4290")));
		assertEquals(4, ((ReferencingElement)e).getReferences().size());

		e = information.getByAddress("0x00000000ffd07500");
		assertEquals("Object", e.getType());
		assertFalse(e.isHeapRoot());
		assertTrue(((ReferencingElement)e).getReferences().stream()
				.anyMatch(ref -> ref.getAddress().equalsIgnoreCase("0x00000008007f4290")));
		assertTrue(((ReferencingElement)e).getReferences().stream()
				.anyMatch(ref -> ref.getAddress().equalsIgnoreCase("0x00000000ffe61c80")));
		assertEquals(2, ((ReferencingElement)e).getReferences().size());

		e = information.getByAddress("0x00000000ffe61c80");
		assertEquals("Object", e.getType());
		assertFalse(e.isHeapRoot());
		assertTrue(((ReferencingElement)e).getReferences().stream()
				.anyMatch(ref -> ref.getAddress().equalsIgnoreCase("0x00000008007f08c0")));
		assertEquals(1, ((ReferencingElement)e).getReferences().size());

		e = information.getByAddress("0x00000000ffd07ba0");
		assertEquals("Object", e.getType());
		assertFalse(e.isHeapRoot());
		assertTrue(((ReferencingElement)e).getReferences().stream()
				.anyMatch(ref -> ref.getAddress().equalsIgnoreCase("0x00000008007eee30")));
		assertTrue(((ReferencingElement)e).getReferences().stream()
				.anyMatch(ref -> ref.getAddress().equalsIgnoreCase("0x00000000ffd07500")));
		assertEquals(2, ((ReferencingElement)e).getReferences().size());

		e = information.getByAddress("0x00000000ffd07518");
		assertEquals("Object", e.getType());
		assertFalse(e.isHeapRoot());
		assertTrue(((ReferencingElement)e).getReferences().stream()
				.anyMatch(ref -> ref.getAddress().equalsIgnoreCase("0x0000000800932e00")));
		assertTrue(((ReferencingElement)e).getReferences().stream()
				.anyMatch(ref -> ref.getAddress().equalsIgnoreCase("0x00000000ffd07550")));
		assertTrue(((ReferencingElement)e).getReferences().stream()
				.anyMatch(ref -> ref.getAddress().equalsIgnoreCase("0x00000000ffd07ba0")));
		assertEquals(3, ((ReferencingElement)e).getReferences().size());

		e = information.getByAddress("0x00000000ffd07550");
		assertEquals("Object", e.getType());
		assertFalse(e.isHeapRoot());
		assertTrue(((ReferencingElement)e).getReferences().stream()
				.anyMatch(ref -> ref.getAddress().equalsIgnoreCase("0x00000008008b0a08")));
		assertTrue(((ReferencingElement)e).getReferences().stream()
				.anyMatch(ref -> ref.getAddress().equalsIgnoreCase("0x00000000ffd07568")));
		assertEquals(2, ((ReferencingElement)e).getReferences().size());

		e = information.getByAddress("0x00000000ffd07568");
		assertEquals("Object", e.getType());
		assertFalse(e.isHeapRoot());
		assertTrue(((ReferencingElement)e).getReferences().stream()
				.anyMatch(ref -> ref.getAddress().equalsIgnoreCase("0x00000008007eee30")));
		assertTrue(((ReferencingElement)e).getReferences().stream()
				.anyMatch(ref -> ref.getAddress().equalsIgnoreCase("0x00000000ffe5d0a0")));
		assertEquals(2, ((ReferencingElement)e).getReferences().size());

		e = information.getByAddress("0x00000000ffe5d0a0");
		assertEquals("Object", e.getType());
		assertFalse(e.isHeapRoot());
		assertTrue(((ReferencingElement)e).getReferences().stream()
				.anyMatch(ref -> ref.getAddress().equalsIgnoreCase("0x000000080081b748")));
		assertEquals(1, ((ReferencingElement)e).getReferences().size());

		e = information.getByAddress("0x00000000ffdf4f38");
		assertEquals("Object", e.getType());
		assertFalse(e.isHeapRoot());
		assertTrue(((ReferencingElement)e).getReferences().stream()
				.anyMatch(ref -> ref.getAddress().equalsIgnoreCase("0x00000008007f5a48")));
		assertTrue(((ReferencingElement)e).getReferences().stream()
				.anyMatch(ref -> ref.getAddress().equalsIgnoreCase("0x00000008007f4290")));
		assertTrue(((ReferencingElement)e).getReferences().stream()
				.anyMatch(ref -> ref.getAddress().equalsIgnoreCase("0x00000008008b0a08")));
		assertEquals(3, ((ReferencingElement)e).getReferences().size());

	}

}