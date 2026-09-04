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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import tooling.leyden.commands.CommonParameters;

public class Information {

    //This represents the AOT Cache
    private final Map<Key, Element> elements = new ConcurrentHashMap<>();

    //This represents elements that were loaded in the app
    //from a different source, not the AOT Cache.
    //Useful to detect if there are elements that should have been cached.
    private final Map<Key, Element> elementsNotInTheCache = new ConcurrentHashMap<>();

    //List of warnings and incidents that may be useful to check
    private final List<Warning> warnings = Collections.synchronizedList(new ArrayList<>());
    //Auto-generated warnings by `warning check` command
    private final List<Warning> autoWarnings = Collections.synchronizedList(new ArrayList<>());

    //Store information extracted and inferred
    private final Configuration configuration = new Configuration();
    private final Configuration statistics = new Configuration();

    //To pre-calculate auto-completion
    private final List<String> identifiers = new ArrayList<>();
    //To search by address
    private final Map<String, Element> elementsByAddress = new ConcurrentHashMap<>();
    //To find Heap Roots
    private final Set<String> heapRootAddresses = Collections.synchronizedSet(new HashSet<>());
    private ReferencingElement heapRoot = null;

    private final ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor();

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
        elementsNotInTheCache.remove(key);

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

    public void addExternalElement(Element e) {
        elementsNotInTheCache.put(new Key(e.getKey(), e.getType()), e);
        if (e.getAddress() != null) {
            elementsByAddress.putIfAbsent(e.getAddress(), e);
        }
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
        CommonParameters parameters = new CommonParameters();
        parameters.setName(e.getKey());
        parameters.setTypes(new String[] { e.getType() });
        parameters.setUse(CommonParameters.ElementsToUse.cached);
        return getElements(parameters).findAny().isPresent();
    }

    public Element getByAddress(String address) {
        return elementsByAddress.getOrDefault(address, null);
    }

    public Stream<Element> getElements(String key, String[] packageName, String[] excludePackageName,
            Boolean includeArrays, Boolean includeExternalElements, String... type) {

        CommonParameters parameters = new CommonParameters();
        parameters.setName(key);
        parameters.setPackageName(packageName);
        parameters.setExcludePackageName(excludePackageName);
        parameters.setUseArrays(includeArrays);
        parameters.setTypes(type);
        parameters
                .setUse(includeExternalElements ? CommonParameters.ElementsToUse.both : CommonParameters.ElementsToUse.cached);

        return getElements(parameters);
    }

    public Future<Stream<Element>> getFutureElements(CommonParameters parameters) {
        return executorService.submit(() -> getElements(parameters));
    }

    public Stream<Element> getElements(CommonParameters parameters) {
        String key = parameters.getName();
        String[] type = parameters.getTypes();

        if (key != null && !key.isBlank() && type != null && type.length > 0) {
            //This is trivial, don't search through all elements
            var result = new ArrayList<Element>();
            for (String t : type) {
                var k = new Key(key, t);
                if (parameters.getUse() != CommonParameters.ElementsToUse.notCached) {
                    var element = elements.get(k);
                    if (element != null) {
                        result.add(element);
                    }
                }
                if (parameters.getUse() != CommonParameters.ElementsToUse.cached) {
                    var element = elementsNotInTheCache.get(k);
                    if (element != null) {
                        result.add(element);
                    }
                }
            }
            return result.parallelStream();
        }

        //Another trivial set
        if (parameters.getAddress() != null) {
            return Stream.ofNullable(elementsByAddress.getOrDefault(parameters.getAddress(), null));
        }

        var tmp = new HashSet<Map.Entry<Key, Element>>();
        if (parameters.getUse() != CommonParameters.ElementsToUse.cached) {
            tmp.addAll(elementsNotInTheCache.entrySet());
        }
        if (parameters.getUse() != CommonParameters.ElementsToUse.notCached) {
            tmp.addAll(elements.entrySet());
        }
        var result = tmp.parallelStream();

        if (key != null && !key.isBlank()) {
            result = result.filter(keyElementEntry -> keyElementEntry.getKey().identifier().equalsIgnoreCase(key));
        }

        if (parameters.getNameLike() != null && !parameters.getNameLike().isBlank()) {
            Pattern pattern = Pattern.compile(parameters.getNameLike());
            result = result.filter(
                    keyElementEntry -> pattern.matcher(keyElementEntry.getKey().identifier()).matches());
        }

        return filterByParams(parameters, result.map(Map.Entry::getValue));
    }

    public static Stream<Element> filterByParams(String[] packageName,
            String[] excludePackageName,
            Boolean addArrays,
            String[] types,
            Boolean showOnlyHeapRoots,
            Stream<Element> result) {

        CommonParameters parameters = new CommonParameters();
        parameters.setPackageName(packageName);
        parameters.setExcludePackageName(excludePackageName);
        parameters.setUseArrays(addArrays);
        parameters.setTypes(types);
        parameters.setHeapRoot(showOnlyHeapRoots);

        return filterByParams(parameters, result);
    }

    public static Stream<Element> filterByParams(CommonParameters parameters, Stream<Element> result) {

        var packageName = parameters.getPackageName();
        var excludePackageName = parameters.getExcludePackageName();

        if (packageName != null && packageName.length > 0) {
            result = result.filter(e -> {
                if (e instanceof ClassObject classObject) {
                    return Arrays.stream(packageName).anyMatch(p -> classObject.getPackageName().startsWith(p));
                }
                if (e instanceof MethodObject methodObject) {
                    if (methodObject.getClassObject() != null) {
                        return Arrays.stream(packageName)
                                .anyMatch(p -> methodObject.getClassObject().getPackageName().startsWith(p));
                    }
                    return Arrays.stream(packageName).anyMatch(p -> methodObject.getName().startsWith(p));
                }
                if (e.getType().equals("Object")
                        || e.getType().startsWith("ConstantPool")) {
                    return Arrays.stream(packageName)
                            .anyMatch(p -> e.getKey().startsWith(p));
                }
                if (e.getType().endsWith("TrainingData")
                        || e.getType().equalsIgnoreCase("MethodData")
                        || e.getType().equalsIgnoreCase("MethodCounters")) {
                    return Arrays.stream(packageName)
                            .anyMatch(p -> ((ReferencingElement) e).getReferences().stream()
                                    .anyMatch(r -> {
                                        if (r instanceof ClassObject classObject) {
                                            return classObject.getPackageName().startsWith(p);
                                        } else if (r instanceof MethodObject methodObject) {
                                            return methodObject.getClassObject().getPackageName().startsWith(p);
                                        }
                                        return false;
                                    }));
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
                        return Arrays.stream(excludePackageName)
                                .noneMatch(p -> methodObject.getClassObject().getPackageName().startsWith(p));
                    }
                    return Arrays.stream(excludePackageName).noneMatch(p -> methodObject.getName().startsWith(p));
                }
                if (e.getType().equals("Object") || e.getType().startsWith("ConstantPool")) {

                    return Arrays.stream(excludePackageName).noneMatch(p -> e.getKey().startsWith(p));
                }
                if (e.getType().endsWith("TrainingData")
                        || e.getType().equalsIgnoreCase("MethodData")
                        || e.getType().equalsIgnoreCase("MethodCounters")) {
                    return Arrays.stream(excludePackageName)
                            .noneMatch(p -> ((ReferencingElement) e).getReferences().stream()
                                    .anyMatch(r -> {
                                        if (r instanceof ClassObject classObject) {
                                            return classObject.getPackageName().startsWith(p);
                                        } else if (r instanceof MethodObject methodObject) {
                                            return methodObject.getClassObject().getPackageName().startsWith(p);
                                        }
                                        return false;
                                    }));
                }
                return false;
            });
        }

        if (parameters.getTypes() != null && parameters.getTypes().length > 0) {
            result = result.filter(
                    e -> Arrays.stream(parameters.getTypes())
                            .anyMatch(t -> t.equalsIgnoreCase(e.getType())));
        }

        if (parameters.isHeapRoot() != null) {
            result = result.filter(e -> e.isHeapRoot() == parameters.isHeapRoot());
        }

        if (parameters.getShowAOTInited() != null) {
            result = result.filter(e -> {
                if (e instanceof InstanceObject io) {
                    return io.isAOTinited() == parameters.getShowAOTInited();
                } else {
                    return true;
                }
            });
        }

        if (parameters.useArrays()!= null && !parameters.useArrays()) {
            result = result.filter(e -> {
                if (e instanceof ClassObject classObject) {
                    return !classObject.isArray();
                }
                return true;
            });
        }

        if (parameters.getTrained() != null) {
            if (parameters.getTrained()) {
                result = result.filter(e -> e.isTraineable() && e.isTrained());
            } else {
                result = result.filter(e -> e.isTraineable() && !e.isTrained());
            }
        }

        if (!parameters.getLambdas()) {
            result = result
                    .filter(e -> {
                        if (e instanceof ClassObject classObject) {
                            return !classObject.getName().contains("$$Lambda");
                        } else {
                            return true;
                        }
                    });
        }

        if (!parameters.getInnerClasses()) {
            result = result
                    .filter(e -> {
                        if (e instanceof ClassObject classObject) {
                            return !classObject.getName().contains("$");
                        } else {
                            return true;
                        }
                    });
        }

        if (parameters.getReferencing() != null) {
            result = result.filter(e -> {
                if (e instanceof ReferencingElement re) {
                    return re.getReferences().stream().anyMatch(
                            ref -> ref.getKey().equalsIgnoreCase(parameters.getReferencing()));
                }
                return false;
            });
        }

        if (parameters.getInstanceOf() != null) {
            result = result.filter(e -> {
                if (e instanceof InstanceObject io) {
                    return io.getInstanceOf() != null &&
                            io.getInstanceOf().getKey().equalsIgnoreCase(parameters.getInstanceOf());
                }
                return false;
            });
        }

        switch (parameters.getLoaded()) {
            case training -> result = result.filter(e -> e.getType().equalsIgnoreCase("Class")
                    && e.wasLoaded().equals(Element.WhichRun.Training)
                    && !((ClassObject) e).isArray());
            case production -> result = result.filter(e -> e.getType().equalsIgnoreCase("Class") &&
                    e.wasLoaded().equals(Element.WhichRun.Production));
            case both -> result = result.filter(e -> e.getType().equalsIgnoreCase("Class") &&
                    e.wasLoaded().equals(Element.WhichRun.Both));
            case none -> result = result.filter(e -> e.getType().equalsIgnoreCase("Class") &&
                    e.wasLoaded().equals(Element.WhichRun.None));
            default -> {
            }
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

    public List<String> getAddressess() {
        return List.copyOf(elementsByAddress.keySet());
    }

    public record Key(String identifier, String type) {
    }
}
