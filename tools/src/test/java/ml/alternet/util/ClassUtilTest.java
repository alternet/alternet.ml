package ml.alternet.util;

import java.io.Serializable;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.assertj.core.api.Assertions;
import org.testng.annotations.Test;

import ml.alternet.util.ClassUtil;

public class ClassUtilTest {

    static Class<?>[] hashMapHierarchy = new Class[] { Map.class, Cloneable.class,
        Serializable.class, HashMap.class, AbstractMap.class };

    @Test
    public void classes_ShouldBe_partOfAHierarchy() {
        List<Class<?>> classes = ClassUtil.getClasses(HashMap.class)
//            .peek( c-> { System.out.println("USING " + c.getSimpleName()); })
            .collect(Collectors.toList());

        classes.forEach(c -> Assertions.assertThat(c).isIn((Object[]) hashMapHierarchy));
        Arrays.stream(hashMapHierarchy).forEach(c -> Assertions.assertThat(c).isIn(classes));
    }

    interface I1 extends Runnable {}

    interface I2 extends Serializable {}

    interface I3 extends Cloneable {}

    interface I4 extends I1, I2, I3, Appendable {}

    @SuppressWarnings("serial")
    abstract class C extends Number implements I4 { }

    static Class<?>[] myClasses = new Class[] { C.class, I1.class, I2.class, I3.class, I4.class,
        Runnable.class, Serializable.class, Cloneable.class, Appendable.class, Number.class };

    @Test
    public void interfaces_ShouldBe_partOfAHierarchy() {
        List<Class<?>> classes = ClassUtil.getClasses(C.class)
//            .peek( c-> { System.out.println("USING " + c.getSimpleName()); })
            .collect(Collectors.toList());

        classes.forEach(c -> Assertions.assertThat(c).isIn((Object[]) myClasses));
        Arrays.stream(myClasses).forEach(c -> Assertions.assertThat(c).isIn(classes));
    }

}
