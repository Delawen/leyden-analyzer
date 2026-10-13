package tooling.leyden.aotcache;

import io.quarkus.arc.Unremovable;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Optional;

@ApplicationScoped
@Unremovable
public class StatisticRepository implements PanacheRepository<Statistic> {

    public Optional<Statistic> findByName(String name) {
        return find("key", name).firstResultOptional();
    }

    public void createStatistic(String key, Integer defaultValue) {
        if (findByName(key).isEmpty()) {
            Statistic s = new Statistic(key, defaultValue, Information.getMyself().getPlaygroundObject());
            persist(s);
            flush();
        }
    }

    public void incrementStatistic(String key) {
        getEntityManager()
                .createNativeQuery(
                        "MERGE INTO statistic (stat_name, playground, stat_value) " +
                                "KEY (stat_name, playground) " +
                                "VALUES (:key, :playground, COALESCE(" +
                                "(SELECT stat_value FROM statistic " +
                                "WHERE stat_name = :key AND playground = :playground), 0) + 1)")
                .setParameter("key", key)
                .setParameter("playground", Information.getMyself().getPlaygroundObject().getDbId())
                .executeUpdate();
    }

    public void deleteAll(Playground playground) {
        getEntityManager()
                .createNativeQuery("DELETE FROM Statistic WHERE playground = :playground")
                .setParameter("playground", playground.getDbId())
                .executeUpdate();
        flush();
    }
}