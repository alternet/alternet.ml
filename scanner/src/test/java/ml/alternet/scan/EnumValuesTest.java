package ml.alternet.scan;

import java.io.IOException;

import org.assertj.core.api.Assertions;
import org.testng.annotations.Test;

@Test
public class EnumValuesTest {

    enum Function {
        sin, cos, exp, ln, sqrt;
    }

    enum Argon2 {
        argon2d, argon2i, argon2id;
    }

    enum Single {
        justOne;
    }

    public void enumValue_Should_beScanned() throws IOException {
        EnumValues<Function> f = EnumValues.from(Function.class);
        System.out.println("Function = " + f);
        Function fun =  f.nextValue(Scanner.of("cos")).get();

        Assertions.assertThat(fun).isSameAs(Function.cos);
    }

    public void singleRootedEnumValue_Should_beScanned() throws IOException {
        EnumValues<Argon2> a = EnumValues.from(Argon2.class);
        System.out.println("Argon2 = " + a);
        Argon2 arg =  a.nextValue(Scanner.of("argon2d")).get();

        Assertions.assertThat(arg).isSameAs(Argon2.argon2d);
    }

    public void singleEnumValue_Should_beScanned() throws IOException {
        EnumValues<Single> s = EnumValues.from(Single.class);
        System.out.println("Single = " + s);
        Single sin =  s.nextValue(Scanner.of("justOne")).get();

        Assertions.assertThat(sin).isSameAs(Single.justOne);
    }

}
