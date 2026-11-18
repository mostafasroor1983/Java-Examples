package module18.ch06;

import java.util.ArrayList;
import java.util.List;

/**
 * Item 41 - Use marker interfaces to define types.
 *
 * <p>A marker interface defines a TYPE, so misuse is caught at compile time: {@code storeInVault(PciScoped)} cannot
 * be called with an object that is not PCI-scoped. A marker annotation cannot do that (it is only checked at runtime,
 * if at all), but annotations fit better for non-type elements (methods, fields) and for frameworks that already
 * process annotations. Rule: if you would write a method that accepts only marked objects, use an interface.
 */
public final class Item41MarkerInterfaces {
    private Item41MarkerInterfaces() {
    }

    /** Marker: data that may only be stored in the PCI-compliant vault. */
    public interface PciScoped { }

    public record CardData(String panToken, String expiry) implements PciScoped { }

    public record Address(String line1, String country) { }

    public static final class Vault {
        private final List<PciScoped> stored = new ArrayList<>();

        public void storeInVault(PciScoped data) { // vault.storeInVault(new Address(...)) does not compile
            stored.add(data);
        }

        public int size() {
            return stored.size();
        }
    }
}
