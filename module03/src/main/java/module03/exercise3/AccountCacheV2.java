package module03.exercise3;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * v2: bounded LRU using LinkedHashMap in access-order mode + removeEldestEntry.
 * The idiomatic "LRU in 10 lines" interviewers expect.
 */
public class AccountCacheV2 {
    private final int capacity;
    private final LinkedHashMap<String, AccountCache.Account> cache;

    public AccountCacheV2(int capacity) {
        if (capacity <= 0) throw new IllegalArgumentException("capacity must be positive");
        this.capacity = capacity;
        this.cache = new LinkedHashMap<>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, AccountCache.Account> eldest) {
                return size() > AccountCacheV2.this.capacity;
            }
        };
    }

    public void put(AccountCache.Account account) {
        cache.put(account.id(), account);
    }

    public Optional<AccountCache.Account> get(String id) {
        return Optional.ofNullable(cache.get(id)); // get() moves the entry to the tail (most recent)
    }

    public int size() {
        return cache.size();
    }
}
