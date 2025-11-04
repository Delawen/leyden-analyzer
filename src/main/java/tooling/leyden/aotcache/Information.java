package tooling.leyden.aotcache;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

public class Information {

	//This represents the AOT Cache
	private Map<Key, Element> elements = new ConcurrentHashMap<>();

	//This represents elements that were loaded in the app
	//from a different source, not the AOT Cache.
	//Useful to detect if there are elements that should have been cached.
	private Map<Key, Element> elementsNotInTheCache = new ConcurrentHashMap<>();

	//List of warnings and incidents that may be useful to check
	private List<Warning> warnings = new ArrayList<>();
	//Auto-generated warnings by `warning check` command
	private List<Warning> autoWarnings = new ArrayList<>();

	//Store information extracted and inferred
	private Configuration configuration = new Configuration();
	private Configuration statistics = new Configuration();

	//To pre-calculate auto-completion
	private List<String> identifiers = new ArrayList<>();
	//To search by address
	private Map<String, Element> elementsByAddress = new ConcurrentHashMap<>();
	//To find Heap Roots
	private Set<String> heapRootAddresses = Collections.synchronizedSet(new HashSet<>());
	private ReferencingElement heapRoot = null;

	//Singletonish
	private static Information myself;
	public static Information getMyself() {
		return myself;
	}


	public Information() {
		myself = this;
	}

	public void addAOTCacheElement(Element e, String source) {
		e.addSource(source);
		final var key = new Key(e.getKey(), e.getType());
		elements.put(key, e);

		// Due to weird ordering in logfiles, sometimes a method gets
		// referenced before the class it belongs to gets referenced.
		// So we have to make sure elements are not repeated both in
		// this.elements and this.elementsNotInTheCache
		if (elementsNotInTheCache.containsKey(key)) {
			elementsNotInTheCache.remove(key);
		}

		if (e.getAddress() != null) {
			elementsByAddress.putIfAbsent(e.getAddress(), e);
			if (heapRootAddresses.contains(e.getAddress())) {
				e.setHeapRoot(true);
				heapRootAddresses.remove(e.getAddress());
				if (heapRoot != null) {
					heapRoot.addReference(e);
				}
			}
		}

		// Pre-calculate auto-completions
		if (e.getType().equalsIgnoreCase("Class") && !identifiers.contains(e.getKey())) {
			identifiers.add(e.getKey());
		}
	}

	public void addExternalElement(Element e, String source) {
		elementsNotInTheCache.put(new Key(e.getKey(), e.getType()), e);
		if (e.getAddress() != null) {
			elementsByAddress.putIfAbsent(e.getAddress(), e);
		}
		e.addSource(source);
	}

	public Map<Key, Element> getExternalElements() {
		return this.elementsNotInTheCache;
	}

	public void addHeapRoot(String address) {
		this.heapRootAddresses.add(address);
	}

	public void setHeapRoot(ReferencingElement e) {
		this.heapRoot = e;
	}

	public void addWarning(Element element, String reason, WarningType warningType) {
		this.warnings.add(new Warning(element, reason, warningType));
	}

	public void clear() {
		elements.clear();
		elementsNotInTheCache.clear();
		warnings.clear();
		autoWarnings.clear();
		statistics.clear();
		configuration.clear();
		identifiers.clear();
		elementsByAddress.clear();
		heapRootAddresses.clear();
		heapRoot = null;
	}
	public boolean cacheContains(Element e) {
		return getElements(e.getKey(), null, null, true, false, e.getType()).count() > 0;
	}

	public Element getByAddress(String address) {
		return elementsByAddress.getOrDefault(address, null);
	}

	public Stream<Element> getElements(String key, String[] packageName, String[] excludePackageName,
									 Boolean includeArrays, Boolean includeExternalElements, String... type) {

		if (key != null && !key.isBlank() && type != null && type.length > 0) {
			//This is trivial, don't search through all elements
			var result = new ArrayList<Element>();
			for (String t : type) {
				Element e = elements.get(new Key(key, t));
				if (e != null) {
					result.add(e);
				} else if (includeExternalElements) {
					e = elementsNotInTheCache.get(new Key(key, t));
					if (e != null) {
						result.add(e);
					}
				}
			}
			return result.parallelStream();
		}

		var tmp = new HashSet<Map.Entry<Key, Element>>();
		tmp.addAll(elements.entrySet());
		if (includeExternalElements) {
			tmp.addAll(elementsNotInTheCache.entrySet());
		}
		var result = tmp.parallelStream();

		if (key != null && !key.isBlank()) {
			result = result.filter(keyElementEntry -> keyElementEntry.getKey().identifier().equalsIgnoreCase(key));
		}

		return filterByParams(packageName, excludePackageName, includeArrays, type,
				result.map(keyElementEntry -> keyElementEntry.getValue()));
	}

	public static Stream<Element> filterByParams(String[] packageName,
																 String[] excludePackageName,
																 Boolean addArrays,
																 String[] type,
																 Stream<Element> result) {
		if (packageName != null && packageName.length > 0) {
			result = result.filter(e -> {
				if (e instanceof ClassObject classObject) {
					return Arrays.stream(packageName).anyMatch(p -> classObject.getPackageName().startsWith(p));
				}
				if (e instanceof MethodObject methodObject) {
					if (methodObject.getClassObject() != null) {
						return Arrays.stream(packageName).anyMatch(p ->
								methodObject.getClassObject().getPackageName().startsWith(p));
					}
					return Arrays.stream(packageName).anyMatch(p -> methodObject.getName().startsWith(p));
				}
				if (e.getType().equals("Object")
						|| e.getType().startsWith("ConstantPool")) {

					return Arrays.stream(packageName)
							.anyMatch(p -> e.getKey().startsWith(p));
				}
				return false;
			});
		}

		if (excludePackageName != null && excludePackageName.length > 0) {
			result = result.filter(e -> {
				if (e instanceof ClassObject classObject) {
					return Arrays.stream(excludePackageName).noneMatch(p -> classObject.getPackageName().startsWith(p));
				}
				if (e instanceof MethodObject methodObject) {
					if (methodObject.getClassObject() != null) {
						return Arrays.stream(excludePackageName).noneMatch(p ->
								methodObject.getClassObject().getPackageName().startsWith(p));
					}
					return Arrays.stream(excludePackageName).noneMatch(p -> methodObject.getName().startsWith(p));
				}
				if (e.getType().equals("Object") || e.getType().startsWith("ConstantPool")) {

					return Arrays.stream(excludePackageName).noneMatch(p -> e.getKey().startsWith(p));
				}
				return false;
			});
		}

		if (type != null && type.length > 0) {
			result = result.filter(e -> Arrays.stream(type).anyMatch(t -> t.equalsIgnoreCase(e.getType()))
			);
		}

		if (!addArrays) {
			result = result.filter(e -> {
				if (e instanceof ClassObject classObject) {
					return !classObject.isArray();
				}
				return true;
			});
		}
		return result;
	}

	public List<Warning> getWarnings() {
		return warnings;
	}

	public List<Warning> getAutoWarnings() {
		return autoWarnings;
	}

	public Collection<Element> getAll() {
		return elements.values();
	}

	public Configuration getConfiguration() {
		return configuration;
	}

	public Configuration getStatistics() {
		return statistics;
	}

	public List<String> getAllTypes() {
		return this.elements.keySet()
				.parallelStream().map(key -> key.type).distinct().toList();
	}

	public List<String> getAllPackages() {
		return this.elements.entrySet()
				.parallelStream()
				.filter((entry) -> entry.getValue() instanceof ClassObject)
				.map(entry -> ((ClassObject) entry.getValue()).getPackageName())
				.distinct()
				.toList();
	}

	public List<String> getIdentifiers() {
		return List.copyOf(identifiers);
	}

	public record Key(String identifier, String type) {
	}
}
