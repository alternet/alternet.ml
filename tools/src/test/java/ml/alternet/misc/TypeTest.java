package ml.alternet.misc;

import static org.assertj.core.api.Assertions.assertThat;

import org.testng.annotations.Test;

@Test
public class TypeTest {

    public void primitiveFromNameOrClass_Shoud_beTheSame() {
        Type f1 = Type.of(float.class);
        Type f2 = Type.of("float");
        Type f3 = Type.of(null, "float");

        assertThat(f1).isSameAs(f2);
        assertThat(f1).isSameAs(f3);

        assertThat(f1.getPackageName()).isNull();
        assertThat(f1.getKind()).isSameAs(Type.Kind.PRIMITIVE);
        assertThat(f1.getSimpleName()).isEqualTo("float");
        assertThat(f1.getQualifiedName()).isEqualTo("float");
        assertThat(f1.toString()).isEqualTo("float");
        assertThat(f1.getDeclaredClass()).contains(float.class);

        Type b1 = f1.box();
        assertThat(b1.getKind()).isSameAs(Type.Kind.JAVA_LANG_PACKAGE);
        assertThat(b1.getPackageName()).isEqualTo("java.lang");
        assertThat(b1.getSimpleName()).isEqualTo("Float");
        assertThat(b1.getQualifiedName()).isEqualTo("java.lang.Float");
        assertThat(b1.toString()).isEqualTo("java.lang.Float");
        assertThat(b1.getDeclaredClass()).contains(Float.class);
        assertThat(b1.unbox()).isEqualTo(f1);

        Type ff1 = b1.unbox();
        assertThat(ff1).isSameAs(f1);

        Type fff1 = f1.unbox();
        assertThat(fff1).isSameAs(f1);

        assertThat(f1.<Float> parse("1")).isEqualTo(1.0f);
    }

    public void classFromJavaLangPackage_Shoud_beBoxedUnboxed() {
        Type t1 = Type.of(Appendable.class);
        Type b1 = t1.box();
        Type u1 = t1.unbox();

        assertThat(b1).isSameAs(t1);
        assertThat(u1).isSameAs(t1);

        assertThat(t1.getKind()).isSameAs(Type.Kind.JAVA_LANG_PACKAGE);
        assertThat(t1.getPackageName()).isEqualTo("java.lang");
        assertThat(t1.getSimpleName()).isEqualTo("Appendable");
        assertThat(t1.getQualifiedName()).isEqualTo("java.lang.Appendable");
        assertThat(t1.toString()).isEqualTo("java.lang.Appendable");
        assertThat(u1.getDeclaredClass()).contains(Appendable.class);
    }

    public void string_Shoud_beBoxedUnboxed() {
        Type t1 = Type.of(String.class);
        Type b1 = t1.box();
        Type u1 = t1.unbox();

        assertThat(b1).isSameAs(t1);
        assertThat(u1).isSameAs(t1);

        assertThat(t1.getKind()).isSameAs(Type.Kind.JAVA_LANG_PACKAGE);
        assertThat(t1.getPackageName()).isEqualTo("java.lang");
        assertThat(t1.getSimpleName()).isEqualTo("String");
        assertThat(t1.getQualifiedName()).isEqualTo("java.lang.String");
        assertThat(t1.toString()).isEqualTo("java.lang.String");
        assertThat(u1.getDeclaredClass()).contains(String.class);
        assertThat(t1.<String> parse("whatever")).isEqualTo("whatever");
    }

    public void primitiveClass_Shoud_beBoxedUnboxed() {
        Type t1 = Type.of(char.class);
        Type b1 = t1.box();
        Type u1 = t1.unbox();

        assertThat(b1).isNotSameAs(t1);
        assertThat(u1).isSameAs(t1);

        assertThat(t1.getKind()).isSameAs(Type.Kind.PRIMITIVE);
        assertThat(t1.getPackageName()).isNull();
        assertThat(t1.getSimpleName()).isEqualTo("char");
        assertThat(t1.getQualifiedName()).isEqualTo("char");
        assertThat(t1.toString()).isEqualTo("char");
        assertThat(t1.getDeclaredClass()).contains(char.class);

        assertThat(b1.getKind()).isSameAs(Type.Kind.JAVA_LANG_PACKAGE);
        assertThat(b1.getPackageName()).isEqualTo("java.lang");
        assertThat(b1.getSimpleName()).isEqualTo("Character");
        assertThat(b1.getQualifiedName()).isEqualTo("java.lang.Character");
        assertThat(b1.toString()).isEqualTo("java.lang.Character");
        assertThat(b1.getDeclaredClass()).contains(Character.class);

        assertThat(t1.<Character> parse("whatever")).isEqualTo('w');
    }

    public void unboxableClass_Shoud_beBoxedUnboxed() {
        Type t1 = Type.of(Integer.class);
        Type b1 = t1.box();
        Type u1 = t1.unbox();

        assertThat(b1).isSameAs(t1);
        assertThat(u1).isNotSameAs(t1);

        assertThat(t1.getKind()).isSameAs(Type.Kind.JAVA_LANG_PACKAGE);
        assertThat(t1.getPackageName()).isEqualTo("java.lang");
        assertThat(t1.getSimpleName()).isEqualTo("Integer");
        assertThat(t1.getQualifiedName()).isEqualTo("java.lang.Integer");
        assertThat(t1.toString()).isEqualTo("java.lang.Integer");
        assertThat(t1.getDeclaredClass()).contains(Integer.class);

        assertThat(u1.getKind()).isSameAs(Type.Kind.PRIMITIVE);
        assertThat(u1.getPackageName()).isNull();
        assertThat(u1.getSimpleName()).isEqualTo("int");
        assertThat(u1.getQualifiedName()).isEqualTo("int");
        assertThat(u1.toString()).isEqualTo("int");
        assertThat(u1.getDeclaredClass()).contains(int.class);

        assertThat(t1.<Integer> parse("1")).isEqualTo(1);
    }

    public void className_Shoud_beBoxedUnboxed() {
        Type t1 = Type.of("org.acme.Foo");
        Type b1 = t1.box();
        Type u1 = t1.unbox();

        assertThat(b1).isSameAs(t1);
        assertThat(u1).isSameAs(t1);

        assertThat(t1.getKind()).isSameAs(Type.Kind.OTHER);
        assertThat(t1.getPackageName()).isEqualTo("org.acme");
        assertThat(t1.getSimpleName()).isEqualTo("Foo");
        assertThat(t1.getQualifiedName()).isEqualTo("org.acme.Foo");
        assertThat(t1.toString()).isEqualTo("org.acme.Foo");
        assertThat(t1.getDeclaredClass()).isEmpty();
        assertThat(t1.forName()).isEmpty();
    }

    public void doubleUnbox_Should_returnTheSame() {
        Type t1 = Type.of(Boolean.class);
        Type u1 = t1.unbox();
        // the code has changed !!!
        Type u2 = t1.unbox();
        assertThat(u1).isSameAs(u2);
    }

    public void doubleBox_Should_returnTheSame() {
        Type t1 = Type.of(void.class);
        Type b1 = t1.box();
        // the code has changed !!!
        Type b2 = t1.box();
        assertThat(b1).isSameAs(b2);
    }

    public void classWithTypesParameter_Shoud_beBoxed() {
        Type f1 = Type.of("org.acme.Foo");
        Type p1 = f1.withTypeParameters(
            Type.of(int.class),
            Type.of("com.example.Bar"),
            Type.of("java.lang.Appendable"));

        assertThat(p1.toString(t -> t.getKind() == Type.Kind.JAVA_LANG_PACKAGE)).isEqualTo("org.acme.Foo<Integer,com.example.Bar,Appendable>");
        assertThat(p1.toString(t -> true)).isEqualTo("Foo<Integer,Bar,Appendable>");
    }

    public void parsedTypeWithTypesParameter_Shoud_beBoxed() {
        Type p1 = Type.parseTypeDefinition("org.acme.Foo<java.util.Map<?, ? super java.lang.Integer>,com.example.Bar[],java.lang.Appendable>");
        String s1 = p1.toString(t -> t.getKind() == Type.Kind.JAVA_LANG_PACKAGE);
        assertThat(s1).isEqualTo("org.acme.Foo<java.util.Map<?,? super Integer>,com.example.Bar[],Appendable>");
        String s2 = p1.toString(t -> t.getKind() == Type.Kind.JAVA_LANG_PACKAGE || "com.example".equals(t.getPackageName()));
        assertThat(s2).isEqualTo("org.acme.Foo<java.util.Map<?,? super Integer>,Bar[],Appendable>");
        String s3 = p1.toString(t -> true);
        assertThat(s3).isEqualTo("Foo<Map<?,? super Integer>,Bar[],Appendable>");
    }

}
