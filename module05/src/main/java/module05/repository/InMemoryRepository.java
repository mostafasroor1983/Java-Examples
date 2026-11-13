package module05.repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/** One implementation serves every entity type. */
public class InMemoryRepository<T extends Entity<ID>, ID> implements Repository<T, ID> {
    private final ConcurrentHashMap<ID, T> store = new ConcurrentHashMap<>();

    @Override
    public T save(T entity) {
        Objects.requireNonNull(entity, "entity");
        Objects.requireNonNull(entity.id(), "entity.id");
        store.put(entity.id(), entity);
        return entity;
    }

    @Override
    public Optional<T> findById(ID id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<T> findAll() {
        return List.copyOf(store.values());
    }

    @Override
    public List<T> findWhere(Predicate<? super T> predicate) {
        List<T> out = new ArrayList<>();
        for (T t : store.values()) if (predicate.test(t)) out.add(t);
        return out;
    }

    @Override
    public boolean deleteById(ID id) {
        return store.remove(id) != null;
    }

    @Override
    public long count() {
        return store.size();
    }
}
