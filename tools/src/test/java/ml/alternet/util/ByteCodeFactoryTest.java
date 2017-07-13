package ml.alternet.util;

import java.lang.reflect.Field;

import org.assertj.core.api.Assertions;
import org.testng.annotations.Test;

import ml.alternet.util.gen.ByteCodeSpec;

public class ByteCodeFactoryTest {

    public interface SomeInterface {
        // look Ma ! No implementation given !

        int methodWithoutImplementation(String parameter);

    }

    public static abstract class SomeClass { // it doesn't implements SomeInterface
        public int answer() { return 42; }
    }

    @ByteCodeSpec
    private static ByteCodeFactory BYTECODE_FACTORY = ByteCodeFactory
        .getInstance("ml.alternet.util.ByteCodeFactory$"); // exist after code generation

    @Test
    public void instance_Should_BeGeneratedFromInterface() throws InstantiationException, IllegalAccessException, ClassNotFoundException, IllegalArgumentException, NoSuchFieldException, SecurityException {
        SomeInterface si = BYTECODE_FACTORY.newInstance(SomeInterface.class);
        Assertions.assertThat(si).isInstanceOf(SomeInterface.class);
        SomeInterface si2 = BYTECODE_FACTORY.newInstance(SomeInterface.class);
        Assertions.assertThat(si2).isNotSameAs(si);
    }

    @Test(expectedExceptions=AbstractMethodError.class)
    public void method_Should_NotBeImplemented() throws InstantiationException, IllegalAccessException, ClassNotFoundException, IllegalArgumentException, NoSuchFieldException, SecurityException {
        SomeInterface si = BYTECODE_FACTORY.newInstance(SomeInterface.class);
        Assertions.assertThat(si).isInstanceOf(SomeInterface.class);

        si.methodWithoutImplementation("foo");
    }

    @Test
    public void singleton_Should_BeGeneratedFromInterface() throws InstantiationException, IllegalAccessException, ClassNotFoundException, IllegalArgumentException, NoSuchFieldException, SecurityException {
        SomeInterface si = BYTECODE_FACTORY.getInstance(SomeInterface.class);
        Assertions.assertThat(si).isInstanceOf(SomeInterface.class);
        SomeInterface si2 = BYTECODE_FACTORY.getInstance(SomeInterface.class);
        Assertions.assertThat(si2).isSameAs(si);
    }

    @ByteCodeSpec(factoryClassName="Foo", factoryPkg="org.acme.bytecode", parentClass=SomeClass.class, singletonName="FOO_INSTANCE")
    private static ByteCodeFactory FOO_BYTECODE_FACTORY = ByteCodeFactory
        .getInstance("org.acme.bytecode.Foo"); // exist after code generation

    @Test
    public void instance_Should_BeGeneratedFromAbstractClassAndInterface() throws InstantiationException, IllegalAccessException, ClassNotFoundException, IllegalArgumentException, NoSuchFieldException, SecurityException {
        SomeInterface si = FOO_BYTECODE_FACTORY.newInstance(SomeInterface.class);
        Assertions.assertThat(si).isInstanceOf(SomeInterface.class);
        SomeInterface si2 = FOO_BYTECODE_FACTORY.newInstance(SomeInterface.class);
        Assertions.assertThat(si2).isNotSameAs(si);

        // abstract class tests
        Assertions.assertThat(si).isInstanceOf(SomeClass.class);
        SomeClass sc = (SomeClass) si;
        Assertions.assertThat(sc.answer()).isEqualTo(42);
        Field singleton = sc.getClass().getField("FOO_INSTANCE");
        Assertions.assertThat(singleton).isNotNull();
        SomeClass sc2 = (SomeClass) singleton.get(null);
        Assertions.assertThat(sc2).isNotNull();
        Assertions.assertThat(sc2).isNotSameAs(sc);
    }

    @Test
    public void singleton_Should_BeGeneratedFromAbstractClassAndInterface() throws InstantiationException, IllegalAccessException, ClassNotFoundException, IllegalArgumentException, NoSuchFieldException, SecurityException {
        SomeInterface si = FOO_BYTECODE_FACTORY.getInstance(SomeInterface.class);
        Assertions.assertThat(si).isInstanceOf(SomeInterface.class);
        SomeInterface si2 = FOO_BYTECODE_FACTORY.getInstance(SomeInterface.class);
        Assertions.assertThat(si2).isSameAs(si);

        // abstract class tests
        Assertions.assertThat(si).isInstanceOf(SomeClass.class);
        SomeClass sc = (SomeClass) si;
        Assertions.assertThat(sc.answer()).isEqualTo(42);
        Field singleton = sc.getClass().getField("FOO_INSTANCE");
        Assertions.assertThat(singleton).isNotNull();
        SomeClass sc2 = (SomeClass) singleton.get(null);
        Assertions.assertThat(sc2).isNotNull();
        Assertions.assertThat(sc2).isSameAs(sc);
    }

}
