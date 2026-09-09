package tooling.leyden.aotcache;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import io.quarkus.arc.Arc;

import io.quarkus.arc.Unremovable;
import jakarta.inject.Singleton;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import tooling.leyden.commands.CommonParameters;

@Singleton
@Unremovable
public class Information {

    private ElementRepository elementRepository;
    private ConfigurationRepository configurationRepository;
    private StatisticRepository statisticRepository;

    //List of warnings and incidents that may be useful to check
    private final List<Warning> warnings = Collections.synchronizedList(new ArrayList<>());
    private final List<Warning> autoWarnings = Collections.synchronizedList(new ArrayList<>());

    //To find Heap Roots
    private final Set<String> heapRootAddresses = Collections.synchronizedSet(new HashSet<>());
    private ReferencingElement heapRoot = null;

    // To replace placeholders
    private final Map<String, PlaceHolderElement> placeholders = new HashMap();

    private final ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor();

    public static Information getMyself() {
        var container = Arc.container();
        if (container == null) return null;
        var instance = container.instance(Information.class);
        return instance.isAvailable() ? instance.get() : null;
    }

    public Information() {
    }

    private ElementRepository elementRepository() {
        if (elementRepository == null) {
            elementRepository = lookupRepository(ElementRepository.class);
        }
        return elementRepository;
    }

    public Element updateElement(Element e) {
        if (e == null){
            return e;
        }
        if (e.getDbId() == null) {
            elementRepository().getEntityManager().persist(e);
        } else {
            e = elementRepository().getEntityManager().merge(e);
        }

        return e;
    }

    private ConfigurationRepository configurationRepository() {
        if (configurationRepository == null) {
            configurationRepository = lookupRepository(ConfigurationRepository.class);
        }
        return configurationRepository;
    }

    private StatisticRepository statisticRepository() {
        if (statisticRepository == null) {
            statisticRepository = lookupRepository(StatisticRepository.class);
        }
        return statisticRepository;
    }

    private static <T> T lookupRepository(Class<T> type) {
        var container = Arc.container();
        if (container == null) return null;
        var instance = container.instance(type);
        return instance.isAvailable() ? instance.get() : null;
    }

    public List<Configuration> getConfiguration() {
        return configurationRepository().findAll().list();
    }
    public Configuration getConfiguration(String key, String defaultValue) {
        return configurationRepository().findByName(key).orElseGet(() -> new Configuration(key, defaultValue));
    }

    public void update(Configuration config) {
        configurationRepository().getEntityManager().merge(config);
    }

    public void update(Statistic stat) {
        configurationRepository().getEntityManager().merge(stat);
        statisticRepository().getEntityManager().flush();
    }

    public void incrementStatistic(String key) {
        statisticRepository().incrementStatistic(key);
    }

    public synchronized Statistic getStatistic(String key, Integer defaultValue) {
        statisticRepository().createStatistic(key);
        return statisticRepository().findByName(key).get();
    }

    public List<Statistic> getStatistic() {
        return statisticRepository().findAll().list();
    }

    public void addAOTCacheElement(Element e, String source) {
        e.addSource(source);
        e.setInCache(true);

        // Due to ordering in logfiles, sometimes an asset gets
        // referenced before the asset itself gets defined.
        if (heapRootAddresses.contains(e.getAddress())) {
            e.setHeapRoot(true);
            heapRootAddresses.remove(e.getAddress());
            if (heapRoot != null) {
                heapRoot.addReference(e);
            }
        }

        updateElement(e);
    }

    public void addHeapRoot(String address) {
        this.heapRootAddresses.add(address);
    }

    public boolean isHeapRootSet() {
        return this.heapRoot != null;
    }

    public void setHeapRoot(ReferencingElement e) {
        this.heapRoot = e;
    }

    public void addWarning(Element element, String reason, WarningType warningType) {
        this.warnings.add(new Warning(element, reason, warningType));
    }

    public void clear() {

        io.quarkus.narayana.jta.QuarkusTransaction.joiningExisting().run(() -> {
            elementRepository().deleteAll();
            configurationRepository().deleteAll();
            statisticRepository().deleteAll();
            flush();

            warnings.clear();
            autoWarnings.clear();
            heapRootAddresses.clear();
            heapRoot = null;
        });
    }

    public boolean cacheContains(Element e) {
        CommonParameters parameters = new CommonParameters();
        parameters.setName(e.getKey());
        parameters.setTypes(new String[]{e.getType()});
        parameters.setUse(CommonParameters.ElementsToUse.cached);
        return getElements(parameters).findAny().isPresent();
    }

    public Element getByAddress(String address) {
        return elementRepository().findByAddress(address).orElse(null);
    }

    public Element getByDbId(Long dbId) {
        return elementRepository().findByDbId(dbId).orElse(null);
    }

    public Stream<Element> getElements(String key, String[] packageName, String[] excludePackageName,
                                       Boolean includeExternalElements, String... type) {
        CommonParameters parameters = new CommonParameters();
        parameters.setName(key);
        parameters.setPackageName(packageName);
        parameters.setExcludePackageName(excludePackageName);
        parameters.setTypes(type);
        parameters.setUse(includeExternalElements
                ? CommonParameters.ElementsToUse.both
                : CommonParameters.ElementsToUse.cached);

        return getElements(parameters);
    }

    public Future<Stream<Element>> getFutureElements(CommonParameters parameters) {
        return executorService.submit(() -> {
            AtomicReference<Stream<Element>> res = new AtomicReference<>();
            io.quarkus.narayana.jta.QuarkusTransaction.requiringNew().run(() -> res.set(getElements(parameters)));
            return res.get();
        });
    }

    public Stream<Element> getElements(CommonParameters parameters) {
        return elementRepository().findByParameters(parameters).stream();
    }

    public static Stream<Element> filterByParams(String[] packageName,
                                                 String[] excludePackageName,
                                                 String[] types,
                                                 Stream<Element> result) {
        CommonParameters parameters = new CommonParameters();
        parameters.setPackageName(packageName);
        parameters.setExcludePackageName(excludePackageName);
        parameters.setTypes(types);

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

        if (parameters.getShowAOTInited() != null) {
            result = result.filter(e -> {
                if (e instanceof InstanceObject io) {
                    return io.isAOTinited() == parameters.getShowAOTInited();
                } else {
                    return true;
                }
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
            result = result.filter(e -> {
                if (e instanceof ClassObject classObject) {
                    return !classObject.getName().contains("$$Lambda");
                } else {
                    return true;
                }
            });
        }

        if (!parameters.getInnerClasses()) {
            result = result.filter(e -> {
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
        return elementRepository().findAllCached();
    }

    public List<String> getAllTypes() {
        AtomicReference<List<String>> res = new AtomicReference<>();
        io.quarkus.narayana.jta.QuarkusTransaction.joiningExisting().run(() -> res.set(elementRepository().findAllTypes()));
        return res.get();
    }

    public List<String> getAllPackages() {
        AtomicReference<List<String>> res = new AtomicReference<>();
        io.quarkus.narayana.jta.QuarkusTransaction.joiningExisting().run(() -> res.set(elementRepository().findAllPackages()));
        return res.get();
    }

    public Collection<String> getIdentifiers() {

        AtomicReference<List<String>> res = new AtomicReference<>();
        io.quarkus.narayana.jta.QuarkusTransaction.joiningExisting().run(() -> res.set(elementRepository().findAllClassIdentifiers()));
        return res.get();
    }

    public List<String> getAddressess() {
        return elementRepository().findHeapRootAddresses();
    }

    public Collection<Element> getWhoReferencesMe(Element element) {
        return elementRepository().findWhoReferencesMe(element);
    }

    public long getCount() {
        return elementRepository().count();
    }

    public long getPackagesCount() {
        return elementRepository().count("SELECT DISTINCT e.packageName FROM Element e");
    }

    public long getTypesCount() {
        CriteriaBuilder cb = elementRepository().getEntityManager()
                .getCriteriaBuilder();
        CriteriaQuery<Long> query = cb.createQuery(Long.class);
        Root<Element> root = query.from(Element.class);
        query.select(cb.countDistinct(root.get("type")));
        TypedQuery<Long> typedQuery = elementRepository().getEntityManager().createQuery(query);
        return typedQuery.getSingleResult();
    }

    public Map<String, Long> getDetailedCount() {
        var res = new HashMap<String, Long>();
        var list = elementRepository().getEntityManager()
                .createQuery("SELECT e.type, Count(*) as count FROM Element e GROUP BY type")
                .getResultList();
        for (Object o : list) {
            var data = (Object[]) o;
            res.put((String) data[0], (Long) data[1]);
        }
        return res;
    }

    //Use it only on tests if needed
    public void flush() {
        io.quarkus.narayana.jta.QuarkusTransaction.joiningExisting()
                .run(() ->elementRepository().getEntityManager().flush());
    }

    public Element refresh(Element re) {
        return elementRepository().findById(re.getDbId());
    }

    public void addRelationship(ReferencingElement ref, Element e) {
        if (ref == null || e == null || ref.equals(e)) {
            return;
        }
        if (ref.getDbId() == null) {
            ref = (ReferencingElement) updateElement(ref);
        }
        if (e.getDbId() == null) {
            e = updateElement(e);
        }
        elementRepository().addRelationship(ref.getDbId(), e.getDbId());
    }
    public void addSource(Element e, String source) {
        if (source == null || e == null) {
            return;
        }
        if (e.getDbId() == null) {
            e = updateElement(e);
        }
        elementRepository().addSource(e.getDbId(), source);
    }

    public void replacePlaceHolders() {
        elementRepository().replacePlaceHolders();
    }

    public PlaceHolderElement getPlaceholder(String address) {
        return this.placeholders.entrySet().stream()
                .filter(e -> e.getKey().equalsIgnoreCase(address))
                .map(e -> e.getValue())
                .findAny().orElse(null);
    }

    public void replacePlaceHolder(PlaceHolderElement placeholder, Element asset) {
        elementRepository().replacePlaceHolder(placeholder, asset);
    }

    public void addPlaceholder(PlaceHolderElement e) {
        this.placeholders.put(e.getAddress(), e);
    }

    public void removePlaceholder(String address) {
        this.placeholders.remove(address);
    }
}
