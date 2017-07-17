package ml.alternet.util.gen;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.NestingKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic.Kind;
import javax.tools.FileObject;
import javax.tools.StandardLocation;

import org.isk.jvmhardcore.pjba.builder.ClassFileBuilder;
import org.isk.jvmhardcore.pjba.structure.ClassFile;

import ml.alternet.util.gen.jvm.Assembler;
import ml.alternet.util.gen.jvm.HexDumper;

/**
 * Generate an instance of the class ByteCodeFactory.
 *
 * @see ByteCodeSpec
 *
 * @author Philippe Poulard
 */
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class ByteCodeFactoryGenerator extends AbstractProcessor {

    static Set<String> TYPES = new HashSet<>();

    static {
        TYPES.add(ByteCodeSpec.class.getCanonicalName());
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return TYPES;
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (! roundEnv.processingOver()) {
            processAnnotations(annotations, roundEnv);
        }
        Messager messager = processingEnv.getMessager();
        messager.printMessage(Kind.NOTE, ByteCodeFactoryGenerator.class.getCanonicalName() + " : end");
        return true;
    }

    public void processAnnotations(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        Elements eltUtils = processingEnv.getElementUtils();

        TypeElement elementLookup = eltUtils.getTypeElement(ByteCodeSpec.class.getCanonicalName());
        Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(elementLookup);
        for (Element element : elements) {
            ByteCodeSpec bc = element.getAnnotation(ByteCodeSpec.class);
            if (bc != null) {
                processAnnotation(element, bc);
            }
        }
    }

    public void processAnnotation(Element element, ByteCodeSpec byteCode) {
        String pkg = byteCode.factoryPkg(); // "ml.alternet.util";
        String className = byteCode.factoryClassName(); // "ByteCodeFactory$";
        String parentClass = ""; // java/lang/Object
        try { // getting a Class cause an exception
            byteCode.parentClass();
        } catch (MirroredTypeException mte) {
            TypeMirror tm = mte.getTypeMirror();
            parentClass = getClassName(tm);
        }
        String parentClassName = parentClass.replace('.', '/');
        String singletonName = byteCode.singletonName(); // "SINGLETON"

        Messager messager = processingEnv.getMessager();
        messager.printMessage(Kind.NOTE, "GENERATE " + pkg + '.' + className);

        String code = getCode();

        Filer filer = processingEnv.getFiler();
        try {
            FileObject file = filer.createResource(StandardLocation.SOURCE_OUTPUT, pkg,
                    className + ".java");
            messager.printMessage(Kind.NOTE, file.toUri().toString());
            PrintWriter writer = new PrintWriter(file.openWriter());
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                ByteCodeFactoryGenerator.class.getResourceAsStream(byteCode.template()), "UTF8"));
            reader.lines()
                .map(s -> s.replaceAll("\\{factoryPkg\\}", escapeDollar(pkg)))
                .map(s -> s.replaceAll("\\{factoryClassName\\}", escapeDollar(className)))
                .map(s -> s.replaceAll("\\{code\\}", escapeDollar(code)))
                .map(s -> s.replaceAll("\\{parentClassName\\}", escapeDollar(parentClassName)))
                .map(s -> s.replaceAll("\\{singletonName\\}", escapeDollar(singletonName)))
                .forEach(writer::println);
            writer.close();
        } catch (IOException ioe) {
            messager.printMessage(Kind.ERROR, ioe.getMessage());
        }
    }

    // because the result string is used as replacement regexp, and the $ sign
    // stands for a backreference
    String escapeDollar(String string) {
        return string.replaceAll("\\$", "\\\\\\$");
    }

    /**
     * Return the qualified name of a type (those that contain a $ sign for
     * nested classes).
     *
     * @param tm
     *            Represent the class
     * @return The class name
     */
    public static String getClassName(TypeMirror tm) {
        if (tm.getKind().equals(TypeKind.DECLARED)) {
            TypeElement el = (TypeElement) ((DeclaredType) tm).asElement();
            return getClassName(el);
        } else {
            return tm.toString();
        }
    }

    /**
     * Return the qualified name of a type (those that contain a $ sign for
     * nested classes).
     *
     * @param el
     *            Represent the class
     * @return The class name
     */
    public static String getClassName(TypeElement el) {
        if (el.getNestingKind() == NestingKind.TOP_LEVEL) {
            return el.getQualifiedName().toString();
        } else {
            return getClassName((TypeElement) el.getEnclosingElement()) + "$" + el.getSimpleName();
        }
    }

    /**
     * The code generator is based on the output given by the disassembler :
     *
     * <pre>$ javap -c -v ml.alternet.util.gen.sample.SampleInterfaceImpl</pre>
     *
     * Output :
     * <pre>Compiled from "SampleInterfaceImpl.java"
     *public class ml.alternet.util.gen.sample.SampleInterfaceImpl implements ml.alternet.util.gen.sample.SampleInterface {
     *  public static final ml.alternet.util.gen.sample.SampleInterface SAMPLE_INTERFACE;
     *
     *  static {};
     *    Code:
     *       0: new           #1                  // class ml/alternet/util/gen/sample/SampleInterfaceImpl
     *       3: dup
     *       4: invokespecial #12                 // Method "&lt;init&gt;":()V
     *       7: putstatic     #15                 // Field SAMPLE_INTERFACE:Lml/alternet/util/gen/sample/SampleInterface;
     *      10: return
     *
     *  public ml.alternet.util.gen.sample.SampleInterfaceImpl();
     *    Code:
     *       0: aload_0
     *       1: invokespecial #19                 // Method java/lang/Object."&lt;init&gt;":()V
     *       4: return
     *}</pre>
     *
     * @return The code
     */
    public String getCode() {
        // the code for which we will capture the byte code generation
        // that is to say a data write sequence to include in the template

        // if we want something different, the changes have to be applied here

        // CODE for :
        // public class SampleInterfaceImpl implements SampleInterface {}

        // ClassFile classFile = new ClassFileBuilder(ClassFile.MODIFIER_PUBLIC + ClassFile.MODIFIER_SUPER, "className", "java/lang/Object")
        //         .newInterface("interfaceName")
        //         .newMethod(ClassFile.MODIFIER_PUBLIC , "<init>", "()V")
        //         .aload_0()
        //         .invokespecial("java/lang/Object", "<init>", "()V")
        //         .return_()
        //         .build();


        // CODE for :
        // public class SampleInterfaceImpl implements SampleInterface {
        //     public static final SampleInterface SAMPLE_INTERFACE = new SampleInterfaceImpl();
        // }
        ClassFileBuilder classFileBuilder = new ClassFileBuilder(ClassFile.MODIFIER_PUBLIC + ClassFile.MODIFIER_SUPER, "className", "parentClassName" )
          .newInterface("interfaceName")
          .newField(Modifier.PUBLIC + Modifier.STATIC + Modifier.FINAL, "singletonName", "interfaceType");
        classFileBuilder.newMethod(Modifier.STATIC , "<clinit>", "()V")
          .new_("className")
          .dup()
          .invokespecial("className", "<init>", "()V")
          .putstatic("className", "singletonName", "interfaceType")
          .return_();
        classFileBuilder.newMethod(ClassFile.MODIFIER_PUBLIC , "<init>", "()V")
          .aload_0()
//          .invokespecial("java/lang/Object", "<init>", "()V")
          .invokespecial("parentClassName", "<init>", "()V")
          .return_();
        ClassFile cf = classFileBuilder.build();

        // the idea is to capture the Java instructions that can generate the
        // above code

        // required by the assembler
        ByteArrayOutputStream baos = new ByteArrayOutputStream(1024);
        DataOutput bytecode = new DataOutputStream(baos);

        // the buffer that accept the instructions
        StringBuffer buf = new StringBuffer();
        // that assembler can intercept each write instruction
        Assembler a = new Assembler(cf, bytecode) {
            @Override
            public void start() {
                buf.append("\n            // === START GENERATED CODE ===\n");
            };
            @Override
            public void end() {
                buf.append("            // === END GENERATED CODE ===\n");
            };
            @Override
            public void write(String method, Object data) {
                // replace "String" by variable names
                // according to the template
                if ("\"className\"".equals(data)) {
                    data = "className";
                } else if ("\"parentClassName\"".equals(data)) {
                        data = "parentClassName";
                } else if ("\"interfaceName\"".equals(data)) {
                    data = "interfaceName";
                } else if ("\"interfaceType\"".equals(data)) {
                    data = "interfaceType";
                } else if ("\"singletonName\"".equals(data)) {
                    data = "singletonName";
                }
                buf.append("            bytecode." + method + "(" + data + ");\n");
            }
        };
        a.assemble();
        Messager messager = processingEnv.getMessager();
        messager.printMessage(Kind.NOTE, new HexDumper(cf).dump());
        return buf.toString();
    }

}
