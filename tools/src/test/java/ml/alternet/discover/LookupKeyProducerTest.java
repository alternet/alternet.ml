package ml.alternet.discover;

import javax.inject.Inject;

import org.assertj.core.api.Assertions;
import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.environment.se.WeldContainer;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import ml.alternet.discover.Injection;

@LookupKey(forClass = LookupKeyProducerTest.H.class, implClass = LookupKeyProducerTest.H2.class)
@Injection.Producer(forClass = LookupKeyProducerTest.H.class)
public class LookupKeyProducerTest {

    Weld weld;
    WeldContainer container;

    @BeforeTest
    public void startCDI() {
        weld = new Weld();
        container = weld.initialize();
    }

    @AfterTest
    public void stopCDI() {
        weld.shutdown();
    }

    // ====================================

    public static class A {
        B b;

        @Inject
        public A(@Injection.LookupKey(variant = "var2") B b) {
            this.b = b;
        }
    }

    public static interface B {
    }

    @LookupKey(forClass = B.class, variant = "var1")
    @Injection.Producer(forClass = B.class, variant = "var1")
    public static class B1 implements B {
    }

    @LookupKey(forClass = B.class, variant = "var2")
    @Injection.Producer(forClass = B.class, variant = "var2")
    public static class B2 implements B {
    }

    @Test
    public void injectedInstanceWithVariant_ShouldBe_producedWithVariant() {
        A a = container.instance().select(A.class).get();
        // a.b injected by the generated producer
        Assertions.assertThat(a.b).isInstanceOf(B2.class);
    }

    @Test
    public void discoveryService_Should_createInstanceWithVariant() throws InstantiationException,
            IllegalAccessException, ClassNotFoundException {
        B b1 = DiscoveryService.lookupSingleton(B.class.getName() + "/var1");
        Assertions.assertThat(b1).isInstanceOf(B1.class);
        B b2 = DiscoveryService.lookupSingleton(B.class.getName() + "/var2");
        Assertions.assertThat(b2).isInstanceOf(B2.class);
    }

    // ====================================

    public static class C {
        D d;

        @Inject
        public C(@Injection.LookupKey D d) {
            this.d = d;
        }
    }

    public static interface D {
    }

    @LookupKey(forClass = D.class, variant = "var1")
    public static class D1 implements D {
    }

    @LookupKey(forClass = D.class, variant = "var2")
    @Injection.Producer(forClass = D.class, lookupVariant = "var2")
    public static class D2 implements D {
    }

    @Test
    public void injectedInstanceWithoutVariant_ShouldBe_producedWithVariant() {
        C c = container.instance().select(C.class).get();
        // c.d injected by the generated producer
        Assertions.assertThat(c.d).isInstanceOf(D2.class);
    }

    // ====================================

    public static class E {
        F f;

        @Inject
        public E(@Injection.LookupKey F f) {
            this.f = f;
        }
    }

    public static interface F {
    }

    @LookupKey(forClass = F.class)
    @Injection.Producer(forClass = F.class)
    public static class F1 implements F {
    }

    public static class F2 implements F {
    }

    @Test
    public void injectedInstanceWithoutVariant_ShouldBe_producedWithoutVariant() {
        E e = container.instance().select(E.class).get();
        // e.f injected by the generated producer
        Assertions.assertThat(e.f).isInstanceOf(F1.class);
    }

    @Test
    public void discoveryService_Should_createInstanceWithoutVariant() throws InstantiationException,
            IllegalAccessException, ClassNotFoundException {
        F f = DiscoveryService.lookupSingleton(F.class);
        Assertions.assertThat(f).isInstanceOf(F1.class);
    }

    // ====================================

    public static class G {
        H h;

        @Inject
        public G(@Injection.LookupKey H h) {
            this.h = h;
        }
    }

    public static interface H {
    }

    public static class H1 implements H {
    }

    public static class H2 implements H {
    }

    @Test
    public void injectedInstance_ShouldBe_produced() {
        G g = container.instance().select(G.class).get();
        // g.h injected by the generated producer
        Assertions.assertThat(g.h).isInstanceOf(H2.class);
    }

    @Test
    public void discoveryService_Should_findKeyDefinedOnADifferentClass() throws InstantiationException,
            IllegalAccessException, ClassNotFoundException {
        H h = DiscoveryService.lookupSingleton(H.class);
        Assertions.assertThat(h).isInstanceOf(H2.class);
    }

    // ====================================

    public static class I {
        J j;

        @Inject
        public I(@Injection.LookupKey(variant = "var2") J j) {
            this.j = j;
        }
    }

    public static class K {
        J j;

        @Inject
        public K(@Injection.LookupKey(variant = "var1") J j) {
            this.j = j;
        }
    }

    @Injection.Producer(forClass = J.class, variant = "var1")
    @Injection.Producer(forClass = J.class, variant = "var2")
    @LookupKey(forClass = J.class, implClass=J1.class, variant = "var1")
    @LookupKey(forClass = J.class, implClass=J2.class, variant = "var2")
    public static interface J {
    }

    public static class J1 implements J {
    }

    public static class J2 implements J {
    }

    @Test
    public void multipleAnnotation_Should_generateEntries() throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        I i = container.instance().select(I.class).get();
        // i.j injected by the generated producer
        Assertions.assertThat(i.j).isInstanceOf(J2.class);

        K k = container.instance().select(K.class).get();
        // k.j injected by the generated producer
        Assertions.assertThat(k.j).isInstanceOf(J1.class);
        
        J j1 = DiscoveryService.lookupSingleton(J.class.getCanonicalName() + "/var1");
        Assertions.assertThat(j1).isInstanceOf(J1.class);
        J j2 = DiscoveryService.lookupSingleton(J.class.getCanonicalName() + "/var2");
        Assertions.assertThat(j2).isInstanceOf(J2.class);
    }

}
