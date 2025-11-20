package tooling.leyden.aotcache;

import jakarta.enterprise.context.Dependent;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.transaction.Transactional;
import tooling.leyden.commands.CommonParameters;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

@Dependent
public class Information {

	@PersistenceContext
	EntityManager em;

	//Store information extracted and inferred
	private Configuration configuration = new Configuration();
	private Configuration statistics = new Configuration();

	//To find Heap Roots
	private Set<String> heapRootAddresses = Collections.synchronizedSet(new HashSet<>());
	private ReferencingElement heapRoot = null;

	public Information() {

	}

	@Transactional
	public Element addAOTCacheElement(Element e, String source) {
		e.addSource(source);
		e.setIsCached(true);

		if (heapRootAddresses.contains(e.getAddress())) {
			e.setHeapRoot(true);
			heapRootAddresses.remove(e.getAddress());
			if (heapRoot != null) {
				heapRoot.addReference(e);
			}
		}

		return em.merge(e);
	}

	@Transactional
	public Element addExternalElement(Element e) {
		e.setIsCached(false);
		return em.merge(e);
	}

	@Transactional
	public Element update(Element e) {
		return em.merge(e);
	}

	public void addHeapRoot(String address) {
		this.heapRootAddresses.add(address);
	}

	public void setHeapRoot(ReferencingElement e) {
		this.heapRoot = e;
	}

	@Transactional
	public void addWarning(Element element, String reason, WarningType warningType) {
		em.persist(new Warning(element, reason, warningType));
	}

	public void clear() {
		statistics.clear();
		configuration.clear();
		heapRootAddresses.clear();
		heapRoot = null;
		//TODO clean database
	}


	@Transactional
	public Element getByAddress(String address) {
		return em.createQuery("SELECT e FROM Element e WHERE e.address LIKE :address", Element.class)
				.setParameter("address", address)
				.setMaxResults(1)
				.getSingleResultOrNull();
	}

	@Transactional
	public Optional<Element> getElement(String key, String[] packageName, String[] excludePackageName,
										CommonParameters.ElementsToUse source, String... type) {
		return getElements(key, packageName, excludePackageName, source, type).findAny();
	}

	@Transactional(Transactional.TxType.MANDATORY)
	public Stream<Element> getElements(String key, String[] packageName, String[] excludePackageName,
									   CommonParameters.ElementsToUse source, String... type) {
		var cb = em.getCriteriaBuilder();
		var cr = cb.createQuery(Element.class);

		Root<Element> root = cr.from(Element.class);

		List<Predicate> predicates = new ArrayList<>();

		if (key != null && !key.isBlank()) {
			predicates.add(cb.like(root.get("identifier"), key));
		}

		if (type != null && type.length > 0) {
			predicates.add(root.get("type").in(type));
		}

		if (source.equals(CommonParameters.ElementsToUse.cached)) {
			predicates.add(cb.equal(root.get("isCached"), true));
		} else if (source.equals(CommonParameters.ElementsToUse.notCached)) {
			predicates.add(cb.equal(root.get("isCached"), false));
		}

		if (packageName != null && packageName.length > 0) {
			predicates.add(root.get("packageName").in(packageName));
		}

		if (excludePackageName != null && excludePackageName.length > 0) {
			predicates.add(cb.not(root.get("packageName").in(excludePackageName)));
		}

		if (excludePackageName != null && excludePackageName.length > 0) {
			predicates.add(cb.not(root.get("packageName").in(excludePackageName)));
		}

		cr = cr.where(predicates);

		cr = cr.select(root);

		var query = em.createQuery(cr);
		return query.getResultStream();
	}

	@Transactional
	public List<Warning> getWarnings() {
		var cb = em.getCriteriaBuilder();
		var cr = cb.createQuery(Warning.class);

		Root<Warning> root = cr.from(Warning.class);
		cr = cr.where(cb.equal(root.get("auto"), false));
		cr = cr.select(root);

		return em.createQuery(cr).getResultList();
	}

	@Transactional
	public List<Warning> getAutoWarnings() {
		var cb = em.getCriteriaBuilder();
		var cr = cb.createQuery(Warning.class);

		Root<Warning> root = cr.from(Warning.class);
		cr = cr.where(cb.equal(root.get("auto"), true));
		cr = cr.select(root);

		return em.createQuery(cr).getResultList();
	}

	@Transactional
	public Collection<Element> getAll() {
		var cr = em.getCriteriaBuilder().createQuery(Element.class);
		cr = cr.select(cr.from(Element.class));
		return em.createQuery(cr).getResultList();
	}

	@Transactional
	public Collection<Element> getExternalElements() {
		var cb = em.getCriteriaBuilder();
		var cr = cb.createQuery(Element.class);

		Root<Element> root = cr.from(Element.class);
		cr = cr.where(cb.equal(root.get("isCached"), false));
		cr = cr.select(root);

		var query = em.createQuery(cr);
		return query.getResultList();
	}

	@Transactional
	public Configuration getConfiguration() {
		return configuration;
	}

	@Transactional
	public Configuration getStatistics() {
		return statistics;
	}

	@Transactional
	public List<String> getAllTypes() {
		var cb = em.getCriteriaBuilder();
		var cr = cb.createQuery(String.class);

		Root<Element> root = cr.from(Element.class);
		cr = cr.select(root.get("type")).distinct(true);

		return em.createQuery(cr).getResultList();
	}

	@Transactional
	public List<String> getAllPackages() {
		var cb = em.getCriteriaBuilder();
		var cr = cb.createQuery(String.class);

		Root<Element> root = cr.from(Element.class);
		cr = cr.select(root.get("packageName")).distinct(true);

		return em.createQuery(cr).getResultList();
	}

	@Transactional
	public List<String> getIdentifiers() {
		var cb = em.getCriteriaBuilder();
		var cr = cb.createQuery(String.class);

		Root<Element> root = cr.from(Element.class);
		cr = cr.select(root.get("identifier")).distinct(true);

		return em.createQuery(cr).getResultList();
	}
}
