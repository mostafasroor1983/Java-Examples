package module02.mutablekey;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MutableKeyTest {

    @Test
    void mutatingKeyAfterInsertionOrphansEntry() {
        Map<MutablePaymentKey, BigDecimal> map = new HashMap<>();
        MutablePaymentKey key = new MutablePaymentKey("m1", "o1");
        map.put(key, BigDecimal.TEN);

        //Mutating after insertion orphans the entry.
        key.setOrderId("o2"); // hash changes, entry stays in old bucket

        assertNull(map.get(key), "same reference, not found");

        /*
        * على الرغم من أن العنصر موجود فعلياً في الصندوق (Bucket) الخاص لهاش الكود القديم ("m1", "o1"), إلا أن البحث باستخدام new MutablePaymentKey("m1", "o1") يعيد null للأسباب التالية:
مطابقة الهاش: مفتاح البحث الجديد ينتج نفس الهاش القديم ويذهب للصندوق الصحيح.
فشل دالة equals(): عندما يقوم HashMap بفحص العناصر داخل الصندوق، فهو يقارن مفتاح البحث مع المفتاح المخزن فعلياً داخل الخريطة (node.key).
بما أن المرجع الأصلي تم تعديله (key.setOrderId("o2"))، فإن المفتاح المخزن أصبح يحمل القيمة "o2". لذلك، المقارنة equals() بين المفتاح المخزن ("m1", "o2") ومفتاح البحث ("m1", "o1") تُرجع false، مما يجعل البحث يفشل ويعيد null.
* */
        assertNull(map.get(new MutablePaymentKey("m1", "o1")), "original value, not found either");

        /*
        * عندما تبحث باستخدام new MutablePaymentKey("m1", "o2"), يقوم HashMap بحساب الهاش (Hash code) بناءً على القيم الجديدة ("m1", "o2").
هذا يوجه عملية البحث إلى صندوق (Bucket) مختلف تماماً عن الصندوق الذي وُضع فيه العنصر أصلاً (والذي يتبع الهاش القديم "m1", "o1"). وبما أن الصندوق الجديد فارغ، فإن get() تعيد null مباشرة دون أن تصل إلى العنصر التائه في الصندوق القديم.
* */
        assertNull(map.get(new MutablePaymentKey("m1", "o2")));
        assertEquals(1, map.size(), "entry is still there, just unreachable");
        assertFalse(map.containsKey(key));
    }

    @Test
    void recordKeyIsSafe() {
        Map<PaymentKey, BigDecimal> map = new HashMap<>();
        map.put(new PaymentKey("m1", "o1"), BigDecimal.TEN);
        assertEquals(BigDecimal.TEN, map.get(new PaymentKey("m1", "o1")));
        assertThrows(NullPointerException.class, () -> new PaymentKey(null, "o"));
    }
}
