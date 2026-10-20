package tooling.leyden.aotcache;

import java.util.*;
import java.util.regex.Pattern;

import io.quarkus.arc.Unremovable;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
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
        return getEntityManager()
                .createQuery("SELECT DISTINCT e.type FROM Element e", String.class)
                .getResultList();
    }

    /** All distinct package names among cached ClassObjects. */
    public List<String> findAllPackages() {
        return getEntityManager()
                .createQuery(
                        "SELECT DISTINCT c.packageName FROM ClassObject c WHERE playground = :playground",
                        String.class)
                .setParameter("playground", Information.getMyself().getPlaygroundObject())
                .getResultList();
    }

    /** All keys of cached Class elements (used for auto-completion). */
    public List<String> findAllClassIdentifiers() {
        return getEntityManager()
                .createQuery(
                        "SELECT e.name FROM Element e WHERE e.type = 'Class' AND playground = :playground",
                        String.class)
                .setParameter("playground", Information.getMyself().getPlaygroundObject())
                .getResultList();
    }

    public Collection<Element> findWhoReferencesMe(Element element) {
        var res = new ArrayList<Element>();
        var select = "SELECT e FROM ReferencingElement e JOIN e.references ref WHERE ref.dbId = :referencing AND e.dbId <> :myself";
        Map<String, Object> params = Map.of("referencing", element.getDbId(), "myself", element.getDbId());
        res.addAll(find(select, params).list());
        select = "SELECT e FROM ConstantPoolObject e WHERE e.poolHolderKey = :referencing AND e.type = 'Class' AND e.dbId <> :myself";
        res.addAll(find(select, params).list());
        return res;
    }

    /** All addresses of registered heap roots (elements flagged as heap root). */
    public List<String> findHeapRootAddresses() {
        return getEntityManager()
                .createQuery(
                        "SELECT e.address FROM Element e WHERE e.isHeapRoot = true AND e.address IS NOT NULL AND playground = :playground",
                        String.class)
                .setParameter("playground", Information.getMyself().getPlaygroundObject())
                .getResultList();
    }

    private void addWhere(StringBuilder where, String s) {
        where.append(" AND ");
        where.append(s);
    }

    public List<Element> findByParameters(CommonParameters parameters, Playground playground) {
        Map<String, Object> params = new HashMap<>();
        StringBuilder where = new StringBuilder("");
        String select = "SELECT e FROM Element e WHERE playground = :playground";
        params.put("playground", playground);

        if (parameters.getName() != null && !parameters.getName().isBlank()) {
            addWhere(where, "e.key = :key");
            params.put("key", parameters.getName());
        }

        if (parameters.getTypes() != null && parameters.getTypes().length > 0) {
            int i = 0;
            where.append(" AND (");
            for (String type : parameters.getTypes()) {
                if (i > 0) {
                    where.append(" OR ");
                }
                where.append("e.type = :type" + i);
                params.put("type" + i, type);
                i++;
            }
            where.append(")");
        }

        if (parameters.getUse() != null && parameters.getUse() != CommonParameters.ElementsToUse.both) {
            addWhere(where, "e.inCache = :inCache");
            params.put("inCache", parameters.getUse() == CommonParameters.ElementsToUse.cached);
        }

        if (parameters.getAddress() != null) {
            addWhere(where, "e.address = :address");
            params.put("address", parameters.getAddress());
        }

        var packageName = parameters.getPackageName();

        if (packageName != null && packageName.length > 0) {
            int i = 0;
            where.append(" AND (");
            for (String pn : packageName) {
                if (i > 0) {
                    where.append(" OR ");
                }
               where.append("((e.class = :classType AND e.packageName like :packageName" + i + ") "
                        + "OR (e.class = :methodType AND e.name like :packageName" + i + ")"
                        + "OR (e.class = :objectType AND e.key like :packageName" + i + ")"
                        + ")");
                params.putIfAbsent("classType", ClassObject.class);
                params.putIfAbsent("methodType", MethodObject.class);
                params.putIfAbsent("objectType", InstanceObject.class);
                params.put("packageName" + i, pn);
                i++;
            }
            where.append(")");
        }

        var excludePackageName = parameters.getExcludePackageName();
        if (excludePackageName != null && excludePackageName.length > 0) {
            int i = 0;
            where.append(" AND NOT (");
            for (String pn : excludePackageName) {
                if (i > 0) {
                    where.append(" OR ");
                }
                where.append("(e.class = :classType AND e.packageName like :excludePackageName" + i + ")"
                        + " OR (e.class = :methodType AND e.name like :excludePackageName" + i + ")"
                        + " OR (e.class = :objectType AND e.key like :excludePackageName" + i + ")"
                        + " ");
                params.putIfAbsent("classType", ClassObject.class);
                params.putIfAbsent("methodType", MethodObject.class);
                params.putIfAbsent("objectType", InstanceObject.class);
                params.put("excludePackageName" + i, pn);
                i++;
            }
            where.append(")");
        }

        if (parameters.getShowAOTInited() != null) {
            addWhere(where, "e.class = :typeInstance");
            addWhere(where, "e.isAOTinited = :isAOTinited");
            params.put("isAOTinited", parameters.getShowAOTInited());
            params.putIfAbsent("typeInstance", InstanceObject.class);
        }

        if (parameters.getLambdas() != null && !parameters.getLambdas()) {
            addWhere(where, "NOT (e.class = :classType AND e.name like '%$$Lambda%')");
            params.putIfAbsent("classType", ClassObject.class);
        }

        if (parameters.getInnerClasses() != null && !parameters.getInnerClasses())  {
            addWhere(where, "NOT(e.class = :classType AND e.name like '%$%')");
            params.putIfAbsent("classType", ClassObject.class);
        }

        if (parameters.getLoaded() != null && parameters.getLoaded() != WhichRun.all) {
            addWhere(where, "e.loaded = :loaded");
            switch (parameters.getLoaded()) {
                case training -> params.putIfAbsent("loaded", Element.WhichRun.Training);
                case production -> params.putIfAbsent("loaded", Element.WhichRun.Production);
                case none -> params.putIfAbsent("loaded", Element.WhichRun.None);
                case both -> params.putIfAbsent("loaded", Element.WhichRun.Both);
            }
        }

        if (parameters.getNameLike() != null && !parameters.getNameLike().isBlank()) {
            addWhere(where, "e.key like regexp :nameLike");
            params.putIfAbsent("nameLike", parameters.getNameLike());
        }

        if (parameters.getInstanceOf() != null) {
            addWhere(where, "e.instanceOf.key = :instanceof");
            params.putIfAbsent("instanceof", parameters.getInstanceOf());
        }

        if (parameters.getReferencing() != null) {
            select = "SELECT e FROM ReferencingElement e JOIN e.references ref WHERE e.playground = :playground";
            addWhere(where, "ref.key = :referencing ");
            params.putIfAbsent("referencing", parameters.getReferencing());
        }

        if (parameters.getTrained() != null) {
            // classObject            return this.getKlassTrainingData() != null;
            //methodObject             return !this.compileTrainingDataIds.isEmpty();
            //nmethod true
            if (parameters.getTrained()) {
                addWhere(where, "(e.class = :nmethodType "
                        + "OR (e.class = :classType AND e.klassTrainingData IS NOT NULL) "
                        + "OR (e.class = :methodType AND e.compileTrainingDataIds IS NOT EMPTY))");
            } else {
                addWhere(where, "(e.class = :nmethodType "
                        + "OR (e.class = :classType AND e.klassTrainingData IS NULL) "
                        + "OR (e.class = :methodType AND e.compileTrainingDataIds IS EMPTY))");
            }
            params.putIfAbsent("nmethodType", NMethodObject.class);
            params.putIfAbsent("methodType", MethodObject.class);
            params.putIfAbsent("classType", ClassObject.class);
        }

        var res = find(select + where, params).list();
        res.sort(Comparator.comparing(Element::getKey).thenComparing(Element::getType));
        return res;
    }

    public void addRelationship(Long ref, Long e) {
        getEntityManager().flush();
        getEntityManager()
                .createNativeQuery("MERGE INTO element_references (source_id, target_id) VALUES (:refId, :eId)")
                .setParameter("refId", ref)
                .setParameter("eId", e)
                .executeUpdate();
        getEntityManager().flush();
    }

    public void addSource(Long dbId, String source) {
        getEntityManager().flush();
        getEntityManager()
                .createNativeQuery("MERGE INTO element_sources (dbId, source) VALUES (:eId, :source)")
                .setParameter("source", source)
                .setParameter("eId", dbId)
                .executeUpdate();
        getEntityManager().flush();
    }

    public void replacePlaceHolders(Playground playground) {
        flush();
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
        flush();
    }

    public void replacePlaceHolder(PlaceHolderElement placeholder, Element asset) {
        flush();

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
        flush();
        deleteById(placeholder.getDbId());
        flush();
    }

    public @NonNull Map<Integer, Long> getCompiledLevels(Playground playground) {
        var levels = new HashMap<Integer, Long>();
        var compiled = getEntityManager()
                .createQuery("SELECT e.compilationLevel as level, COUNT(*) as count "
                        + " FROM NMethodObject e "
                        + " WHERE playground = :playground"
                        + " GROUP BY compilationLevel")
                .setParameter("playground", playground)
                .getResultList();

        for (Object element : compiled) {
            var rows = (Object[]) element;
            levels.put((Integer) rows[0], (Long) rows[1]);
        }

        return levels;
    }
}
