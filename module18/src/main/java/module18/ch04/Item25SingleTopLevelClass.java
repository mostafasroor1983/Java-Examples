package module18.ch04;

/**
 * Item 25 - Limit source files to a single top-level class.
 *
 * <p>Several top-level classes in one source file make the program's meaning depend on the ORDER in which files are
 * passed to the compiler: two files defining the same top-level names compile or fail differently. Keep one top-level
 * type per file; group related types as static nested types (as below) when they belong together.
 *
 * <p>Note: {@code explanations/src/main/java/explanations/sealed/PaymentOutcome.java} in this repository used to declare
 * {@code Success}, {@code Failure} and {@code Pending} as extra top-level records, and {@code javac -Xlint} reported
 * "auxiliary class ... should not be accessed from outside its own source file" 37 times. They now live in their own
 * files ({@code Success.java}, {@code Failure.java}, {@code Pending.java}) and the warning is gone.
 */
public final class Item25SingleTopLevelClass {
    private Item25SingleTopLevelClass() {
    }

    public sealed interface SettlementStatus permits Settled, Rejected { }

    public record Settled(String reference) implements SettlementStatus { }

    public record Rejected(String reference, String reason) implements SettlementStatus { }
}
