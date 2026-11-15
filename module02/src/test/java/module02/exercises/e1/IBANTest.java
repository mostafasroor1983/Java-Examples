package module02.exercises.e1;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class IBANTest {

    String invalidValue = "AE19876554214092111";
    String invalidValue2 = "AF1987655421400092111";

    String value1 = "AE1987655421400092111";
    String value2 = " AE19 8 7655421400092111 ";
    String value3 = " aE19 8 7655421400092111 ";


    @Test
    void validateIBAN() {
        Assertions.assertEquals(new IBAN(value1), new IBAN(value2));
        Assertions.assertEquals(new IBAN(value1), new IBAN(value3));
        Assertions.assertEquals(new IBAN(value3), new IBAN(value2));
    }

    @Test
    void invalidateIBAN() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> new IBAN(invalidValue));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new IBAN(invalidValue2));
        Assertions.assertThrows(NullPointerException.class, () -> new IBAN(null));
    }
}
