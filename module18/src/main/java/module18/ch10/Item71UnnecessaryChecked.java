package module18.ch10;

import java.util.List;
import java.util.Optional;

/**
 * Item 71 - Avoid unnecessary use of checked exceptions.
 *
 * <p>A checked exception the caller cannot recover from just adds try/catch noise, and checked exceptions do not
 * compose with lambdas/streams. Alternatives: return an {@link Optional} (no detail needed), a result type with the
 * reason, or split into a state-testing method plus an unchecked action. Here a lookup that "throws
 * AccountNotFoundException" becomes an Optional, which streams use directly.
 */
public final class Item71UnnecessaryChecked {
    private Item71UnnecessaryChecked() {
    }

    public static final class AccountNotFoundException extends Exception {
        @java.io.Serial
        private static final long serialVersionUID = 1L; // Item 86: always explicit

        public AccountNotFoundException(String id) {
            super("account not found: " + id);
        }
    }

    private static final List<String> ACCOUNTS = List.of("ACC-1", "ACC-2");

    /** BAD: forces try/catch on every caller; unusable in a stream without wrapping. */
    public static String findOrThrow(String id) throws AccountNotFoundException {
        if (!ACCOUNTS.contains(id)) throw new AccountNotFoundException(id);
        return id;
    }

    /** GOOD */
    public static Optional<String> find(String id) {
        return ACCOUNTS.contains(id) ? Optional.of(id) : Optional.empty();
    }

    public static List<String> existing(List<String> ids) {
        return ids.stream().map(Item71UnnecessaryChecked::find).flatMap(Optional::stream).toList();
    }
}
