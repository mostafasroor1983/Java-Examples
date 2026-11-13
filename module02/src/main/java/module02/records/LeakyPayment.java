package module02.records;

import java.util.List;

/** BAD: record whose List component is stored as-is; callers can mutate it after construction. */
public record LeakyPayment(String id, List<String> tags) {
}
