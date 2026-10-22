package tooling.leyden.aotcache;

import java.util.*;
import java.util.regex.Pattern;

import io.quarkus.arc.Unremovable;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.hibernate.query.criteria.JpaExpression;
import org.jspecify.annotations.NonNull;
import tooling.leyden.commands.CommonParameters;
import tooling.leyden.commands.autocomplete.WhichRun;

@ApplicationScoped
@Unremovable
public class ElementRepository implements PanacheRepository<Element> {

    /** Find an element by its memory address. */
    public Optional<Element> findByAddress(String address, Playground playground) {
        return find("address = :address AND type <> :type AND playground = :playground",
                Map.of("address", address, "type", "Placeholder",
                        "playground", playground))
                .firstResultOptional();
    }

    public Optional<PlaceHolderElement> getPlaceholder(String address, Playground playground) {
        return find("address = :address AND type = :type AND playground = :playground",
                Map.of("address", address, "type", "Placeholder",
                        "playground", playground))
                .firstResultOptional();
    }

    /** Find an element by its database primary key. */
    public Optional<Element> findByDbId(Long dbId) {
        return findByIdOptional(dbId);
    }

    /** All elements in the AOT cache. */
    public List<Element> findAllCached(Playground playground) {
        CommonParameters parameters = new CommonParameters();
        parameters.setUse(CommonParameters.ElementsToUse.cached);
        return findByParameters(parameters, playground);
    }

    /** All distinct types among cached elements. */
    public List<String> findAllTypes() {
        CriteriaBuilder builder = getEntityManager().getCriteriaBuilder();
        CriteriaQuery<String> query = builder.createQuery(String.class);
        Root<Element> elementRoot = query.from(Element.class);
        query = query.select(elementRoot.get("type")).distinct(true);
        return getSession().createQuery(query).getResultList();
    }

    /** All distinct package names among cached ClassObjects. */
    public List<String> findAllPackages() {
        CriteriaBuilder builder = getEntityManager().getCriteriaBuilder();
        CriteriaQuery<String> query = builder.createQuery(String.class);
        Root<Element> elementRoot = query.from(Element.class);
        query = query.where(builder.equal(elementRoot.get("playground"),  Information.getMyself().getPlaygroundObject()));
        query = query.select(elementRoot.get("type")).distinct(true);
        return getSession().createQuery(query).getResultList();
    }

    /** All keys of cached Class elements (used for auto-completion). */
    public List<String> findAllClassIdentifiers() {
        CriteriaBuilder builder = getEntityManager().getCriteriaBuilder();
        CriteriaQuery<String> query = builder.createQuery(String.class);
        Root<Element> elementRoot = query.from(Element.class);
        query = query.where(builder.equal(elementRoot.get("playground"),  Information.getMyself().getPlaygroundObject()));
        query = query.where(builder.equal(elementRoot.get("type"),  "Class"));
        query = query.select(elementRoot.get("name")).distinct(true);
        return getSession().createQuery(query).getResultList();
    }

    public Collection<Element> findWhoReferencesMe(Element element) {
        var res = new ArrayList<Element>();

        // Query 1: ReferencingElement rows whose references collection contains this element
        CriteriaBuilder cb = getEntityManager().getCriteriaBuilder();
        CriteriaQuery<ReferencingElement> q1 = cb.createQuery(ReferencingElement.class);
        Root<ReferencingElement> r1 = q1.from(ReferencingElement.class);
        Join<ReferencingElement, Element> ref = r1.join("references");
        q1.select(r1).where(
                cb.equal(ref.get("dbId"), element.getDbId()),
                cb.notEqual(r1.get("dbId"), element.getDbId()));
        res.addAll(getSession().createQuery(q1).getResultList());

        // Query 2: ConstantPoolObject rows whose poolHolderKey matches this element's key
        CriteriaQuery<ConstantPoolObject> q2 = cb.createQuery(ConstantPoolObject.class);
        Root<ConstantPoolObject> r2 = q2.from(ConstantPoolObject.class);
        q2.select(r2).where(
                cb.equal(r2.get("poolHolderKey"), element.getKey()),
                cb.equal(r2.get("type"), "Class"),
                cb.notEqual(r2.get("dbId"), element.getDbId()));
        res.addAll(getSession().createQuery(q2).getResultList());

        return res;
    }

    /** All addresses of registered heap roots (elements flagged as heap root). */
    public List<String> findHeapRootAddresses() {
        CriteriaBuilder builder = getEntityManager().getCriteriaBuilder();
        CriteriaQuery<String> query = builder.createQuery(String.class);
        Root<Element> elementRoot = query.from(Element.class);
        query.select(elementRoot.<String>get("address")).where(
                builder.isTrue(elementRoot.<Boolean>get("isHeapRoot")),
                builder.isNotNull(elementRoot.get("address")),
                builder.equal(elementRoot.get("playground"), Information.getMyself().getPlaygroundObject()));
        return getEntityManager().createQuery(query).getResultList();
    }

    public List<Element> findByParameters(CommonParameters parameters, Playground playground) {
        HibernateCriteriaBuilder cb = getSession().getCriteriaBuilder();
        CriteriaQuery<Element> query = cb.createQuery(Element.class);
        Root<Element> e = query.from(Element.class);
        List<Predicate> where = new ArrayList<>();
        where.add(cb.equal(e.get("playground"), playground));

        if (parameters.getName() != null && !parameters.getName().isBlank()) {
            where.add(cb.equal(e.get("key"), parameters.getName()));
        }

        if (parameters.getTypes() != null && parameters.getTypes().length > 0) {
            where.add(e.get("type").in((Object[]) parameters.getTypes()));
        }

        if (parameters.getUse() != null && parameters.getUse() != CommonParameters.ElementsToUse.both) {
            where.add(cb.equal(e.get("inCache"), parameters.getUse() == CommonParameters.ElementsToUse.cached));
        }

        if (parameters.getAddress() != null) {
            where.add(cb.equal(e.get("address"), parameters.getAddress()));
        }

        var packageName = parameters.getPackageName();
        if (packageName != null && packageName.length > 0) {
            List<Predicate> matches = new ArrayList<>();
            for (String pn : packageName) {
                matches.add(matchesPackage(cb, e, pn));
            }
            where.add(cb.or(matches.toArray(new Predicate[0])));
        }

        var excludePackageName = parameters.getExcludePackageName();
        if (excludePackageName != null && excludePackageName.length > 0) {
            List<Predicate> matches = new ArrayList<>();
            for (String pn : excludePackageName) {
                matches.add(matchesPackage(cb, e, pn));
            }
            where.add(cb.not(cb.or(matches.toArray(new Predicate[0]))));
        }

        if (parameters.getShowAOTInited() != null) {
            where.add(cb.equal(e.type(), InstanceObject.class));
            where.add(cb.equal(cb.treat(e, InstanceObject.class).get("isAOTinited"), parameters.getShowAOTInited()));
        }

        if (parameters.getLambdas() != null && !parameters.getLambdas()) {
            where.add(cb.not(cb.and(
                    cb.equal(e.type(), ClassObject.class),
                    cb.like(cb.treat(e, ClassObject.class).get("name"), "%$$Lambda%"))));
        }

        if (parameters.getInnerClasses() != null && !parameters.getInnerClasses()) {
            where.add(cb.not(cb.and(
                    cb.equal(e.type(), ClassObject.class),
                    cb.like(cb.treat(e, ClassObject.class).get("name"), "%$%"))));
        }

        if (parameters.getLoaded() != null && parameters.getLoaded() != WhichRun.all) {
            switch (parameters.getLoaded()) {
                case training -> where.add(cb.equal(e.get("loaded"), Element.WhichRun.Training));
                case production -> where.add(cb.equal(e.get("loaded"), Element.WhichRun.Production));
                case none -> where.add(cb.equal(e.get("loaded"), Element.WhichRun.None));
                case both -> where.add(cb.equal(e.get("loaded"), Element.WhichRun.Both));
            }
        }

        if (parameters.getNameLike() != null && !parameters.getNameLike().isBlank()) {
            where.add(cb.likeRegexp(e.get("key"), parameters.getNameLike()));
        }

        if (parameters.getInstanceOf() != null) {
            where.add(cb.equal(cb.treat(e, InstanceObject.class).get("instanceOf").get("key"), parameters.getInstanceOf()));
        }

        if (parameters.getReferencing() != null) {
            Join<Element, Element> ref = cb.treat(e, ReferencingElement.class).join("references");
            where.add(cb.equal(ref.get("key"), parameters.getReferencing()));
        }

        if (parameters.getTrained() != null) {
            // classObject: getKlassTrainingData() != null
            // methodObject: !compileTrainingDataIds.isEmpty()
            // nmethod: true
            boolean trained = parameters.getTrained();
            Predicate classTrained = trained
                    ? cb.isNotNull(cb.treat(e, ClassObject.class).get("klassTrainingData"))
                    : cb.isNull(cb.treat(e, ClassObject.class).get("klassTrainingData"));
            JpaExpression<Map<Integer, Long>> compileIds = (JpaExpression<Map<Integer, Long>>) cb.treat(e, MethodObject.class).<Map<Integer, Long>>get("compileTrainingDataIds");
            Predicate methodTrained = trained ? cb.isMapNotEmpty(compileIds) : cb.isMapEmpty(compileIds);
            where.add(cb.or(
                    cb.equal(e.type(), NMethodObject.class),
                    cb.and(cb.equal(e.type(), ClassObject.class), classTrained),
                    cb.and(cb.equal(e.type(), MethodObject.class), methodTrained)));
        }

        query.select(e).where(where.toArray(new Predicate[0]));
        var res = getSession().createQuery(query).getResultList();
        res.sort(Comparator.comparing(Element::getKey).thenComparing(Element::getType));
        return res;
    }

    private Predicate matchesPackage(HibernateCriteriaBuilder cb, Root<Element> e, String pn) {
        return cb.or(
                cb.and(cb.equal(e.type(), ClassObject.class), cb.like(cb.treat(e, ClassObject.class).get("packageName"), pn)),
                cb.and(cb.equal(e.type(), MethodObject.class), cb.like(cb.treat(e, MethodObject.class).get("name"), pn)),
                cb.and(cb.equal(e.type(), InstanceObject.class), cb.like(e.get("key"), pn)));
    }

    public void addRelationship(Long ref, Long e) {
        getEntityManager()
                .createNativeQuery("MERGE INTO element_references (source_id, target_id) VALUES (:refId, :eId)")
                .setParameter("refId", ref)
                .setParameter("eId", e)
                .executeUpdate();
    }

    public void addSource(Long dbId, String source) {
        getEntityManager()
                .createNativeQuery("MERGE INTO element_sources (dbId, source) VALUES (:eId, :source)")
                .setParameter("source", source)
                .setParameter("eId", dbId)
                .executeUpdate();
    }

    public void replacePlaceHolders(Playground playground) {
        // Get the rows we need to update
        //The full query with everything at the same time just takes minutes and times out
       var placeholders = getEntityManager()
                .createNativeQuery(
                        "SELECT DISTINCT placeholder.dbId as placeholderID "
                                + " FROM Elements placeholder "
                                + " WHERE placeholder.dtype = 'PlaceHolderElement'"
                                + " AND placeholder.playground_id = :playground")
                .setParameter("playground", playground.getDbId())
                .setTimeout(600000)
                .getResultList();

        for (Object row : placeholders) {
            io.quarkus.narayana.jta.QuarkusTransaction.requiringNew().run(() -> {
                var placeholder = Information.getMyself().getByDbId((Long) row);
                var assetId = Information.getMyself().getByAddress(placeholder.getAddress(), playground);
                if (assetId != null) {
                    getEntityManager()
                            .createNativeQuery(
                                    "UPDATE element_references AS ref SET target_id = :target_id "
                                            + " WHERE ref.target_id = :placeholder_id "
                                            + " AND NOT EXISTS "
                                            + " (SELECT 1 FROM element_references er "
                                            + " WHERE er.source_id = ref.source_id "
                                            + " AND er.target_id = :target_id) ")
                            .setParameter("placeholder_id", row)
                            .setParameter("target_id", assetId.getDbId())
                            .executeUpdate();
                }
            });
        }

        // Remove element references with placeholders
        // there may be some references dangling that
        // were not merged because duplication
        getEntityManager()
                .createNativeQuery(
                        "DELETE FROM element_references "
                                + "WHERE target_id = ANY"
                                + "(SELECT e.dbId FROM Elements e WHERE e.dtype = 'PlaceHolderElement' "
                                + " AND playground_id = :playground )")
                .setParameter("playground", playground.getDbId())
                .executeUpdate();

        // Remove all placeholders elements
        // (which shouldn't have any relationship at this point)
        getEntityManager()
                .createNativeQuery("DELETE FROM Elements WHERE dtype = 'PlaceHolderElement'"
                        + " AND playground_id = :playground")
                .setParameter("playground", playground.getDbId())
                .executeUpdate();
    }

    public void deleteAll(Playground playground) {
        getEntityManager()
                .createNativeQuery("DELETE FROM Elements "
                        + " WHERE playground_id = :playground")
                .setParameter("playground", playground.getDbId())
                .executeUpdate();
    }

    public void replacePlaceHolder(PlaceHolderElement placeholder, Element asset) {
        getEntityManager()
                .createNativeQuery("MERGE INTO element_references (source_id, target_id) "
                        + "SELECT source_id, :asset_id FROM element_references WHERE target_id = :placeholder_id")
                .setParameter("asset_id", asset.getDbId())
                .setParameter("placeholder_id", placeholder.getDbId())
                .executeUpdate();


        getEntityManager()
                .createNativeQuery("MERGE INTO element_references (source_id, target_id) "
                        + "SELECT :asset_id, target_id FROM element_references WHERE source_id = :placeholder_id")
                .setParameter("asset_id", asset.getDbId())
                .setParameter("placeholder_id", placeholder.getDbId())
                .executeUpdate();


        getEntityManager()
                .createNativeQuery(
                        "DELETE FROM element_references "
                                + "WHERE target_id = :placeholder_id" +
                                " OR source_id = :placeholder_id")
                .setParameter("placeholder_id", placeholder.getDbId())
                .executeUpdate();
        deleteById(placeholder.getDbId());
    }

    public @NonNull Map<Integer, Long> getCompiledLevels(Playground playground) {
        CriteriaBuilder cb = getEntityManager().getCriteriaBuilder();
        CriteriaQuery<Tuple> query = cb.createTupleQuery();
        Root<NMethodObject> root = query.from(NMethodObject.class);
        var levelPath = root.<Integer>get("compilationLevel");
        query.multiselect(levelPath, cb.count(root))
                .where(cb.equal(root.get("playground"), playground))
                .groupBy(levelPath);

        var levels = new HashMap<Integer, Long>();
        for (Tuple row : getEntityManager().createQuery(query).getResultList()) {
            levels.put(row.get(0, Integer.class), row.get(1, Long.class));
        }
        return levels;
    }

    public Collection<MethodObject> getMethods(ClassObject classObject) {
        CriteriaBuilder builder = getEntityManager().getCriteriaBuilder();
        CriteriaQuery<MethodObject> query = builder.createQuery(MethodObject.class);
        Root<MethodObject> elementRoot = query.from(MethodObject.class);
        query = query.where(builder.equal(elementRoot.get("classObject"),  classObject));
        return getSession().createQuery(query).getResultList();
    }
}
