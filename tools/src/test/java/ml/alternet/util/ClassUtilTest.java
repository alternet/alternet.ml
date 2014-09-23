package ml.alternet.util;

import java.io.Serializable;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import org.assertj.core.api.Assertions;
import org.testng.annotations.Test;

import ml.alternet.util.ClassUtil;

public class ClassUtilTest {

    static Class<?>[] hashMapHierarchy = new Class[] { Map.class, Cloneable.class, 
        Serializable.class, HashMap.class, AbstractMap.class };

    @Test
    public void classes_ShouldBe_partOfAHierarchy() {
        List<Class<?>> classes = ClassUtil.getClasses(HashMap.class).collect(Collectors.toList());

        classes.forEach(c -> Assertions.assertThat(c).isIn((Object[]) hashMapHierarchy));
        Arrays.stream(hashMapHierarchy).forEach(c -> Assertions.assertThat(c).isIn(classes));
    }

}
