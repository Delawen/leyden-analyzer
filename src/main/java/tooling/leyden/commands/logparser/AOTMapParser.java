package tooling.leyden.commands.logparser;

import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStyle;
import tooling.leyden.aotcache.BasicObject;
import tooling.leyden.aotcache.ClassObject;
import tooling.leyden.aotcache.ConstantPoolObject;
import tooling.leyden.aotcache.Element;
import tooling.leyden.aotcache.MethodObject;
import tooling.leyden.aotcache.PlaceHolderElement;
import tooling.leyden.aotcache.ReferencingElement;
import tooling.leyden.commands.LoadFileCommand;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class is capable of parsing the AOT Cache map file.
 */
public class AOTMapParser extends Parser {

	private final String regexpAddress = "(?<address>0[xX][0-9a-fA-F]+)";

	// 0x0000000800868d58: @@ Class             520 java.lang.constant.ClassDesc
// 0x00000008049a8410: @@ Misc data 1985520 bytes
// 0x00000000fff69c68: @@ Object (0xfff69c68) [B length: 45
// 0x00000000fff63458: @@ Object (0xfff63458) java.lang.String$CaseInsensitiveComparator
// 0x00000000ffe94558: @@ Object (0xffe94558) java.lang.String "sun.util.locale.BaseLocale"
// 0x00000000ffef4720: @@ Object (0xffef4720) java.lang.Class Lsun/util/locale/BaseLocale$1;
// 0x0000000801cd0518: @@ MethodTrainingData 96
	private final Pattern assetHeader =
			Pattern.compile(regexpAddress + ": @@ (?<type>\\w+)(?: data)?\\s+"
					+ "(?<miniaddress>\\(0[xX][0-9a-fA-F]+\\))?"
					+ "(?<size>\\d+)?\\s*(?<identifier>.*)");

	//  - klass: 'java/lang/Integer'[] 0x000000080081be80
	private final Pattern klass = Pattern.compile(" - klass: '(?<class>[\\w+\\/?]+)' " + regexpAddress);

	//  - klass: {type array byte} 0x00000008007f08c0
	private final Pattern klassArray = Pattern.compile(" - klass: \\{.*\\} " + regexpAddress);

	// -   0: 0x00000000ffe5c700 (0xffe5c700) java.lang.Integer
	// -   1: 0x00000000ffe5c710 (0xffe5c710) java.lang.Integer
	// -   2: 0x00000000ffe5c720 (0xffe5c720) java.lang.Integer
	// -   3: 0x00000000ffe5c730 (0xffe5c730) java.lang.Integer
	// -   4: 0x00000000ffe5c740 (0xffe5c740) java.lang.Integer
	// -   5: 0x00000000ffe5c750 (0xffe5c750) java.lang.Integer
	// -   6: 0x00000000ffe5c760 (0xffe5c760) java.lang.Integer
	private final Pattern array = Pattern.compile(" -\\s*\\d+: " + regexpAddress + " .+");

	// [heap               0x00000000ffd00000 - 0x00000000fff30528   2295080 bytes]
	//0x00000000ffd00000: @@ Object (0xffd00000) [Ljava.lang.Object; length: 5391
	// - klass: 'java/lang/Object'[] 0x00000008007eee30
	// root[   0]: 0x00000000ffd05450 (0xffd05450) [Ljava.lang.Integer; length: 256
	// root[   1]: 0x00000000ffd05860 (0xffd05860) [Ljava.lang.Long; length: 256
	// root[   2]: 0x00000000ffd05c70 (0xffd05c70) [Ljava.lang.Byte; length: 256
	// root[   3]: 0x00000000ffd06080 (0xffd06080) [Ljava.lang.Short; length: 256
	// root[   4]: 0x00000000ffd06490 (0xffd06490) [Ljava.lang.Character; length: 128
	// root[1972]: 0x00000000ffdfd898 (0xffdfd898) java.lang.Class [Ljava/lang/annotation/Annotation;
	// root[1973]: 0x00000000ffdfd910 (0xffdfd910) java.lang.Class [[Ljava/lang/annotation/Annotation;
	// root[1974]: 0x00000000ffdf5c78 (0xffdf5c78) java.lang.Class Ljava/lang/reflect/GenericDeclaration;
	// root[2192]: 0x00000000ffdf1368 (0xffdf1368) java.lang.Class Ljava/lang/invoke/BoundMethodHandle; (aot-inited)
	// root[2193]: 0x00000000ffdef7d8 (0xffdef7d8) java.lang.Class Ljava/lang/invoke/LambdaForm$Kind; (aot-inited)
	private final Pattern heapRoot = Pattern.compile(" root\\[\\s*\\d+\\]: " + regexpAddress + " \\(0.*\\) (.+)");


	// - resolved_references: 0x00000000ffd5dd18 (0xffd5dd18) [Ljava.lang.Object; length: 2
	private final Pattern resolvedReferences = Pattern.compile(
			" - resolved_references: " + regexpAddress + "\\s+\\((?<miniaddress>0[xX][0-9a-fA-F]+)?\\)(?:.*)");

	// - protected transient 'modCount' 'I' @12  1 (0x00000001)
	// - private final 'sequence' 'Ljava/util/List;' @16 0x00000000ffd07550 (0xffd07550) java.util.ArrayList
	// - private transient 'name' 'Ljava/lang/String;' @52 null
	// - private static final 'PRIMITIVE_ARRAY_TYPES' '[[Ljava/lang/Object;' @120 null
	// - injected 'klass' 'J' @16  34368850440 (0x00000008008b0a08)
	private final Pattern fieldClass = Pattern.compile(
			" - (?<modifiers>[public|protected|private|static|final|transient|volatile|synthetic|injected|\\s]+)'" +
					"(?<variable>.+)'\\s+'\\[*[\\[|L](?<classname>[^;']+);?'\\s+(?<index>@\\d*+)\\s+\\(?" +
					regexpAddress + "?\\)?\\s*\\(?(?<miniaddress>0[xX][0-9a-fA-F]+)?\\)?(?:.*)");
	private final Pattern fieldPrimitive = Pattern.compile(
			" - (?<modifiers>[public|protected|private|static|final|transient|volatile|synthetic|injected|\\s]+)'" +
					"(?<variable>.+)'\\s+'(?<classname>[^;']+)'\\s+(?<index>@\\d*+)\\s*(?<value>[^ ]+)\\s*\\(?" +
					regexpAddress + "?\\)?\\s*\\(?(?<miniaddress>0[xX][0-9a-fA-F]+)?\\)?(?:.*)");

	// When parsing extra information, we need to keep track of
	// what element we are processing
	private Element current = null;

	public AOTMapParser(LoadFileCommand loadFile) {
		super(loadFile);
	}

	@Override
	String getSource() {
		return "AOT Map";
	}

	@Override
	public void postProcessing() {
		//Due to ordering on the map file, we may have some unresolved placeholder elements
		information.getAll().stream()
				.filter(e -> e instanceof ReferencingElement)
				.map(ReferencingElement.class::cast)
				.forEach(e -> e.resolvePlaceholders());
	}


	@Override
	public void accept(String content) {
		Matcher m = assetHeader.matcher(content);
		if (m.matches()) {
			processAssetHeader(
					m.group("address"), m.group("type"), m.group("size"), m.group("identifier"),
					m.group("miniaddress"));
			return;
		}

		processOopsLog(content);
	}

	private void processOopsLog(String content) {
		if (processKlassLine(content)) {
			return;
		}

		if (processField(content)) {
			return;
		}

		if (processArray(content)) {
			return;
		}

		if (processResolvedReferences(content)) {
			return;
		}

		processHeapRoot(content);
	}

	private boolean processArray(String content) {
		Matcher m = array.matcher(content);
		if (m.matches()) {
			information.setHeapRoot((ReferencingElement) current);
			((ReferencingElement) current).addReference(new PlaceHolderElement(m.group("address")));
			return true;
		}
		return false;
	}

	private boolean processHeapRoot(String content) {
		Matcher m = heapRoot.matcher(content);
		if (m.matches()) {
			information.addHeapRoot(m.group("address"));
			if (!current.isHeapRoot()) {
				current.setHeapRoot(true);
				information.setHeapRoot((ReferencingElement) current);
				((ReferencingElement) current).addReference(new PlaceHolderElement(m.group("address")));
			}
			return true;
		}
		return false;
	}


	private boolean processResolvedReferences(String content) {
		Matcher m = resolvedReferences.matcher(content);
		if (m.matches()) {
			if (m.group("address") != null) {
				((ReferencingElement) current).addReference(new PlaceHolderElement(m.group("address")));
			}
			return true;
		}
		return false;
	}

	private boolean processField(String content) {
		Matcher m = fieldClass.matcher(content);
		if (m.matches()) {
			if (m.group("address") != null) {
				((ReferencingElement) current).addReference(new PlaceHolderElement(m.group("address")));
			} else {
				var classObj = information.getElements(m.group("classname").replaceAll("/", "."), null, null, true, true, "Class").findAny();
				classObj.ifPresent(element -> ((ReferencingElement) current).addReference(element));
			}
			return true;
		}
		m = fieldPrimitive.matcher(content);
		if (m.matches()) {
			if (m.group("address") != null) {
				((ReferencingElement) current).addReference(new PlaceHolderElement(m.group("address")));
			}
			return true;
		}
		return false;
	}

	private boolean processKlassLine(String content) {
		Matcher m = klass.matcher(content);
		if (m.matches()) {
			final var address = m.group("address");
			Element e = information.getByAddress(address);
			if (e == null) {
				((ReferencingElement) current).addReference(new PlaceHolderElement(address));
			} else if (!e.getKey().equalsIgnoreCase(m.group("class").replaceAll("/", "."))
					|| !e.getType().equalsIgnoreCase("Class")) {
				(new AttributedString("ERROR: Was expecting class " + m.group(1)
						+ " at address " + address + " but found " + e,
						AttributedStyle.DEFAULT.foreground(AttributedStyle.RED).bold()))
						.println(loadFile.getParent().getTerminal());
			} else {
				((ReferencingElement) current).addReference(e);
			}
			return true;
		} else {
			m = klassArray.matcher(content);
			if (m.matches()) {
				Element e = information.getByAddress(m.group("address"));
				if (e == null) {
					((ReferencingElement) current).addReference(new PlaceHolderElement(m.group("address")));
				} else {
					((ReferencingElement) current).addReference(e);
				}
				return true;
			}
		}
		return false;
	}

	private void processAssetHeader(String address, String type, String size_s, String identifier, String miniaddress) {
		try {
			Integer size = -1;
			if (size_s != null) {
				size = Integer.valueOf(size_s);
			}

			Element element;

			if (type.equalsIgnoreCase("Class")) {
				// Metadata Klass
//					0x0000000800868d58: @@ Class             520 java.lang.constant.ClassDesc
//					0x0000000800869078: @@ Class             512 [Ljava.lang.constant.ClassDesc;
				element = processClass(identifier, getSource());
			} else if (type.equalsIgnoreCase("Method")) {
				//Metadata Method
//					0x0000000800831250: @@ Method            88 void java.lang.management.MemoryUsage.<init>(javax.management.openmbean.CompositeData)
// 					0x000000080082ac80: @@ Method            88 char example.Class.example(long)
//				 	0x0000000800773ea0: @@ Method            88 boolean java.lang.Object.equals(java.lang.Object)
				element = processMethod(identifier, getSource());
			} else if (type.equalsIgnoreCase("ConstMethod")) {
//					 0x0000000804990600: @@ ConstMethod       88 void jdk.internal.access.SharedSecrets.setJavaNetHttpCookieAccess(jdk.internal.access.JavaNetHttpCookieAccess)
				element = processConstMethod(identifier);
			} else if (type.equalsIgnoreCase("Symbol")) {
//					0x0000000801e3c000: @@ Symbol            40 [Ljdk/internal/vm/FillerElement;
//					0x0000000801e3c028: @@ Symbol            32 jdk/internal/event/Event
//					0x0000000801e3c048: @@ Symbol            24 jdk/jfr/Event
//					0x0000000801e3c060: @@ Symbol            8 [Z
				element = processSymbol(identifier);
			} else if (type.equalsIgnoreCase("MethodCounters")
//					0x0000000801e4c280: @@ MethodCounters    64
//					0x0000000801e4c280:   0000000800001800 0000000000000002 0000000801e4c228 0000000801e4c280   ................(...............
//					0x0000000801e4c2a0:   0000000000000000 000000fe00000000 00000000000007fe 0000000000000000   ................................
					|| type.equalsIgnoreCase("MethodData")) {
//					0x0000000801f54000: @@ MethodData        496 java.lang.Class sun.invoke.util.Wrapper.wrapperType(java.lang.Class)
//					0x0000000801f5fb68: @@ MethodData        296 void java.lang.Long.<init>(long)
//					0x0000000801f6b328: @@ MethodData        328 int jdk.internal.util.ArraysSupport.hashCode(int, short[], int, int)
//					0x0000000801f6f848: @@ MethodData        584 void java.util.ImmutableCollections$Set12.<init>(java.lang.Object, java.lang.Object)
				element = processMethodDataAndCounter(identifier, address, type);
			} else if (type.equalsIgnoreCase("ConstantPoolCache")) {
//					0x0000000800ec7408: @@ ConstantPoolCache 64 javax.naming.spi.ObjectFactory
				element = processConstantPoolCache(identifier, address);
				type = "ConstantPool";
			} else if (type.equalsIgnoreCase("ConstantPool")) {
				element = processConstantPool(identifier);
			} else if (type.equalsIgnoreCase("KlassTrainingData")) {
//					0x0000000801bc7e40: @@ KlassTrainingData 40 java.util.logging.LogManager
//					0x0000000801bc8968: @@ KlassTrainingData 40 java.lang.invoke.MethodHandleImpl$IntrinsicMethodHandle
//					0x0000000801bcb1a8: @@ KlassTrainingData 40 java.lang.classfile.AttributeMapper$AttributeStability
				element = processKlassTrainingData(new ReferencingElement(), identifier, address);
			} else if (type.equalsIgnoreCase("MethodTrainingData")) {
//					0x0000000801bc7e40: @@ KlassTrainingData 40 java.util.logging.LogManager
//					0x0000000801bc8968: @@ KlassTrainingData 40 java.lang.invoke.MethodHandleImpl$IntrinsicMethodHandle
//					0x0000000801bcb1a8: @@ KlassTrainingData 40 java.lang.classfile.AttributeMapper$AttributeStability
				element = processMethodTrainingData(new ReferencingElement(), identifier, address);
			} else if (type.equalsIgnoreCase("CompileTrainingData")) {
//					0x0000000801cd54b8: @@ CompileTrainingData 80 4 void java.lang.ref.Reference.reachabilityFence(java.lang.Object)
//					0x0000000801cd5508: @@ CompileTrainingData 80 3 void java.lang.ref.Reference.reachabilityFence(java.lang.Object)
//					0x0000000801cd5558: @@ CompileTrainingData 80 3 java.lang.AbstractStringBuilder java.lang.AbstractStringBuilder.append(java.lang.String)
				element = processCompileTrainingData(new ReferencingElement(), identifier, address);
			} else if (type.startsWith("TypeArray")
//					0x0000000800001d80: @@ TypeArrayU1       600
//					0x000000080074cc50: @@ TypeArrayOther    800
					|| type.equalsIgnoreCase("AdapterFingerPrint")
//					0x000000080074cc20: @@ AdapterFingerPrint 8
//					0x000000080074cc20:   bbbeaaaa00000001                                                      ........
//					0x000000080074cc28: @@ AdapterFingerPrint 16
//					0x000000080074cc28:   bbbeaaaa00000002 000000000000aaaa
					|| type.equalsIgnoreCase("AdapterHandlerEntry")
//					0x00000008008431b0: @@ AdapterHandlerEntry 48
//					0x00000008008431b0:   0000000800019868 00007f276e002e60 00007f276e002ee5 00007f276e002ec4   h.......`..n'......n'......n'...
//					0x00000008008431d0:   00007f276e002f20 0000000000000001
					|| type.equals("RecordComponent")
//					 0x00000008029329e8: @@ RecordComponent   24
					|| type.equalsIgnoreCase("Annotations")
//					0x0000000802bf50f0: @@ Annotations       32
//					0x0000000802bf50f0:   0000000802b719a0 0000000000000000 0000000000000000 0000000000000000   ................................
			) {
				element = new BasicObject(identifier.isBlank() ? address : identifier);
			} else if (type.equalsIgnoreCase("Misc")) {
//					0x00000008049a8410: @@ Misc data 1985520 bytes
//					0x00000008049a8410:   0000000000000005 0000000801e563d0 0000000801e56600 0000000801e56420   .........c.......f...... d......
//					0x00000008049a8430:   0000000801e543a8 0000000801e548a8 0000000000000005 0000000801e58dc0   .C.......H................
				type = "Misc-data";
				element = new BasicObject(address);
			} else if (type.equalsIgnoreCase("Object")) {
				//Instances of classes:
//				0x00000000fff69c68: @@ Object (0xfff69c68) [B length: 45
//				0x00000000fff63458: @@ Object (0xfff63458) java.lang.String$CaseInsensitiveComparator
				//2 Special cases: they are a literal representation (String and Class instances)
				// and therefore they can be named in the code of some class
				// and be linked into the heap via a constant pool cache.
//				0x00000000ffe94558: @@ Object (0xffe94558) java.lang.String "sun.util.locale.BaseLocale"
				//java.lang.Class instances (they have been pre-created by <clinit> method:
//				0x00000000ffef4720: @@ Object (0xffef4720) java.lang.Class Lsun/util/locale/BaseLocale$1;
				element = processObject(identifier, miniaddress);
			} else {
				loadFile.getParent().getOut().println("Unidentified: " + type + " at address " + address);
				element = new BasicObject(address);
			}
			element.setAddress(address);
			element.setType(type);
			element.setSize(size);
			this.information.addAOTCacheElement(element, getSource());
			current = element;

		} catch (Exception e) {
			loadFile.getParent().getOut().println(
					"ERROR at " + address + ": " + e.getClass() + " " +
							e.getStackTrace()[0].getFileName() + e.getStackTrace()[0].getLineNumber()
							+ " " + e.getMessage());
		}
	}

	private Element processObject(String identifier, String miniaddress) {
		ReferencingElement element;
		var id = miniaddress + " " + identifier;

		// It shouldn't already exist unless we load the map file twice which is a mistake,
		// so let's assume this is new
		element = new ReferencingElement(id, "Object");

		String[] contentParts = identifier.split("\\s+");

		//Link to corresponding assets:
		final var className = contentParts[0];
		if (className.equalsIgnoreCase("java.lang.String") && className.length() < identifier.length()) {
			//Add the java.lang.String class itself...
			this.information.getElements("java.lang.String", null, null, true, true,
					"Class").forEach(element::addReference);
		}
		if (className.equalsIgnoreCase("java.lang.Class") && className.length() < identifier.length()) {
			final var targetClass = contentParts[1];
			this.information.getElements(targetClass, null, null, true, true,
					"Symbol").forEach(element::addReference);
			this.information.getElements(targetClass.replaceAll("/", "."), null, null, true, true,
					"Class").forEach(element::addReference);
		} else {
			this.information.getElements(className, null, null, true, true,
							"Class")
					.forEach(element::addReference);

			if (className.startsWith("L") && className.endsWith(";")) {
				this.information.getElements(className.substring(1, className.length() - 1), null,
								null, true, true, "Class")
						.forEach(element::addReference);
			}
		}

		return element;
	}

	private Element processSymbol(String identifier) {
		Element element;

		var existingSymbol =
				this.information.getElements(identifier, null, null, true, true,
						"Symbol").findAny();
		element = existingSymbol.orElseGet(() -> new ReferencingElement(identifier, "Symbol"));

		//Try to associate it to the corresponding class:
		var classObject =
				this.information.getElements(identifier.replaceAll("/", "."), null, null, true, true,
						"Class").findAny();
		if (classObject.isPresent()) {
			((ClassObject) classObject.get()).addSymbol((ReferencingElement) element);
		} else if (identifier.startsWith("L") && identifier.endsWith(";")) {
			classObject =
					this.information.getElements(identifier.replaceAll("/", ".").substring(1, identifier.length() - 1),
							null,
							null, true,
							true,
							"Class").findAny();
			classObject.ifPresent(value -> ((ClassObject) value).addSymbol((ReferencingElement) element));
		}
		// Do not link to anything else, we will do that with the logs
		// logs are factual, not guessing as we can do at this point
		return element;
	}

	private Element processMethodDataAndCounter(String identifier, String address, String type) {
		ReferencingElement result = new ReferencingElement();
		if (!identifier.isBlank()) {
			result.setName(identifier);
			MethodObject method;
			var methods = this.information.getElements(identifier, null, null, true, false, "Method").toList();
			if (!methods.isEmpty()) {
				method = (MethodObject) methods.getFirst();
			} else {
				method = new MethodObject(identifier, "Referenced at '" + address + "'", false, this.information);
			}
			result.addReference(method);

			if (type.equalsIgnoreCase("MethodData")) {
				method.setMethodData(result);
			} else {
				method.setMethodCounters(result);
			}
		} else {
			result.setName(address);
		}
		return result;
	}

	private Element processConstantPoolCache(String identifier, String address) {
		//Look for an existing constant pool object
		//Usually we get the ConstantPoolCache before the ConstantPool
		//So this should not find anything
		ConstantPoolObject e = null;
		var cp = this.information.getElements(identifier, null, null, true, true, "ConstantPool").findAny();
		if (cp.isPresent()) {
			e = (ConstantPoolObject) cp.get();
		}

		//If not found, create it
		if (e == null) {
			e = (ConstantPoolObject) processConstantPool(identifier);
			this.information.addExternalElement(e, "Referenced by a ConstantPoolCache.");
		}

		//And assign the ConstantPoolCache address to the element
		e.setConstantPoolCacheAddress(address);
		return e;
	}

	private Element processConstantPool(String identifier) {
		var element = this.information.getElements(identifier, null, null, true, true, "ConstantPool").findAny();
		ConstantPoolObject cp;
		cp = element.map(value -> (ConstantPoolObject) value).orElseGet(() -> new ConstantPoolObject(identifier));

		if (cp.getPoolHolder() == null) {
			//Try to associate it to the corresponding class:
			element = this.information.getElements(identifier, null, null, true, true, "Class").findAny();
			element.ifPresent(value -> cp.setPoolHolder((ClassObject) value));
		}

		return cp;
	}

	private Element processKlassTrainingData(ReferencingElement e, String identifier, String address) {
		//0x0000000801bb4950: @@ KlassTrainingData 40 java.nio.file.Files$AcceptAllFilter
		//0x0000000801bbd700: @@ KlassTrainingData 40 sun.util.calendar.ZoneInfo

		//Looking for the Class
		if (!identifier.isBlank()) {
			var classs = this.information.getElements(identifier, null, null, true, true, "Class").findAny();
			if (classs.isEmpty()) {
				ClassObject classObject = new ClassObject(identifier);
				classObject.setKlassTrainingData(e);
				e.addReference(classObject);
				this.information.addExternalElement(classObject, "Referenced from a KlassTrainingData.");
			} else {
				e.addReference(classs.get());
				((ClassObject) classs.get()).setKlassTrainingData(e);
			}

			e.setName(identifier);
		} else {
			e.setName(address);
		}

		return e;
	}

	private Element processMethodTrainingData(ReferencingElement e, String identifier, String address) {
		//0x0000000801c4d7a8: @@ MethodTrainingData 96 void java.util.concurrent.atomic.AtomicLong.lazySet(long)

		//Looking for the Method
		if (!identifier.isBlank()) {
			var methods = this.information.getElements(identifier, null, null, true, true, "Method").findAny();
			MethodObject method;
			if (methods.isEmpty()) {
				String source = "Referenced from a MethodTrainingData: " + address;
				method = new MethodObject(identifier, source, true, this.information);
				this.information.addExternalElement(method, source);
			} else {
				method = (MethodObject) methods.get();
			}
			e.addReference(method);
			method.setMethodTrainingData(e);
			e.setName(identifier);
		} else {
			e.setName(address);
		}

		return e;
	}


	private Element processCompileTrainingData(ReferencingElement e, String content, String address) {
		// 0x0000000801a41200: @@ CompileTrainingData 80 3 org.apache.logging.log4j.spi.LoggerContext org.apache.logging.log4j.LogManager.getContext(boolean)

		content = content.trim();

		//Looking for the Method
		if (!content.isBlank()) {
			Integer level = Integer.valueOf(content.substring(0, 1));
			String identifier = content.substring(2);
			var methods = this.information.getElements(identifier, null, null, true, true, "Method").findAny();
			MethodObject method;
			if (methods.isEmpty()) {
				String source = "Referenced from a CompiledTrainingData: " + address;
				method = new MethodObject(identifier, source, true, this.information);
				this.information.addExternalElement(method, source);
			} else {
				method = (MethodObject) methods.get();
			}
			e.addReference(method);
			method.addCompileTrainingData(level, e);
			e.setName(content);
		} else {
			e.setName(address);
		}

		return e;
	}

	private Element processClass(String identifier, String thisSource) {
		// 0x000000080082d490: @@ Class             760 java.lang.StackFrameInfo
		// It could have been already loaded

		var classs = this.information.getElements(identifier, null, null, true, true, "Class").findAny();
		if (classs.isPresent()) {
			classs.get().getSources().add(thisSource);
			return classs.get();
		}
		ClassObject classObject = new ClassObject(identifier);
		//If there are Symbols with this exact class name (dotted or slashed), link them:
		var symbol = this.information.getElements(identifier.replaceAll("\\.", "/"), null, null, true, true, "Symbol").findAny();
		symbol.ifPresent(element -> classObject.addSymbol((ReferencingElement) element));
		symbol = this.information.getElements(identifier, null, null, true, true, "Symbol").findAny();
		symbol.ifPresent(element -> classObject.addSymbol((ReferencingElement) element));

		if (identifier.contains("$$")) {
			//Lambda class, link to main outer class
			String parent = identifier.substring(0, identifier.indexOf("$$"));
			final var parentClass = this.information.getElements(parent, null, null, true, false, "Class").findAny();
			if (parentClass.isEmpty()) {
				ClassObject parentObject = new ClassObject(parent);
				information.addExternalElement(parentObject, "Referenced from a Class element in " + thisSource);
			} else {
				classObject.addReference(parentClass.get());
			}
		}

		return classObject;
	}

	private Element processMethod(String identifier, String thisSource) {
		// It could have been already loaded
		var e = this.information.getElements(identifier, null, null, true, false, "Method").findAny();
		if (e.isPresent()) {
			e.get().addSource(thisSource);
			return e.get();
		}
		return new MethodObject(identifier, thisSource, false, this.information);
	}

	private Element processConstMethod(String identifier) {
		// It could have been already loaded
		var e = this.information.getElements(identifier, null, null, true, false, "ConstMethod").findAny();
		if (e.isPresent()) {
			return e.get();
		}

		BasicObject constMethod = new BasicObject(identifier);

		//Which Method do we belong to?
		var mop = this.information.getElements(identifier, null, null, true, false, "Method").findAny();
		if (mop.isPresent()) {
			((MethodObject) mop.get()).setConstMethod(constMethod);
		} else {
			MethodObject method = new MethodObject(identifier, "Referenced by ConstMethod in AOT Cache", true, this.information);
			method.setConstMethod(constMethod);
			this.information.addExternalElement(method, "Referenced by ConstMethod in AOT Cache");
		}

		return constMethod;
	}
}
