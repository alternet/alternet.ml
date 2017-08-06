package ml.alternet.discover.gen;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic.Kind;
import javax.tools.FileObject;
import javax.tools.StandardLocation;

import ml.alternet.discover.Injection;
import ml.alternet.discover.Injection.LookupKey;
import ml.alternet.discover.Injection.Producer;
import ml.alternet.discover.Injection.Producers;
import ml.alternet.util.AnnotationProcessorUtil;

/**
 * Generate a producer that inject classes.
 *
 * @see LookupKey
 * @see javax.inject.Inject
 *
 * @author Philippe Poulard
 */
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class LookupKeyProducerGenerator extends AbstractProcessor {

    static Set<String> TYPES = new HashSet<>();

    static {
        TYPES.add(Producer.class.getCanonicalName());
        TYPES.add(Producers.class.getCanonicalName());
    }

    Set<Mapping> mappings = new HashSet<>();

    static class Mapping {

        String producerPkg;
        String producerClassName;
        String shortClassName;
        String className;
        String variant;
        String lookupKey;

        Mapping(String producerPkg, String producerClassName, String shortClassName, String className,
                String variant, String lookupKey)
        {
            this.producerPkg = producerPkg;             // org.acme.foo
            this.producerClassName = producerClassName; // Bar$Baz_Producer_var
            this.shortClassName = shortClassName;       // Bar$Baz
            this.className = className;                 // org.acme.foo.Bar.Baz
            this.variant = variant;                     // var
            this.lookupKey = lookupKey;                 // org.acme.foo.Bar.Baz/var
        }

        @Override
        public boolean equals(Object obj) {
            return className.equals(((Mapping) obj).className) && variant.equals(((Mapping) obj).variant);
        }

        @Override
        public int hashCode() {
            return className.hashCode() ^ variant.hashCode();
        }

    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return TYPES;
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (roundEnv.processingOver()) {
            generateFiles();
        } else {
            processAnnotations(annotations, roundEnv);
        }
        Messager messager = processingEnv.getMessager();
        messager.printMessage(Kind.NOTE, LookupKeyProducerGenerator.class.getCanonicalName() + " : end");
        return true;
    }

    /**
     * Process the annotations.
     *
     * @param annotations The annotation types requested to be processed
     * @param roundEnv Environment for information about the current and prior round
     */
    public void processAnnotations(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        Elements eltUtils = processingEnv.getElementUtils();

        TypeElement elementLookup = eltUtils.getTypeElement(Producers.class.getCanonicalName());
        Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(elementLookup);
        for (Element element : elements) {
            Producers lks = element.getAnnotation(Producers.class);
            if (lks != null) {
                for (Producer lk : lks.value()) {
                    processAnnotation(element, lk);
                }
            }
        }
        elementLookup = eltUtils.getTypeElement(Producer.class.getCanonicalName());
        elements = roundEnv.getElementsAnnotatedWith(elementLookup);
        for (Element element : elements) {
            Producer lk = element.getAnnotation(Producer.class);
            if (lk != null) {
                processAnnotation(element, lk);
            }
        }
    }

    /**
     * Process a single annotation.
     *
     * @param element The element that hold the annotation.
     * @param producer The annotation.
     */
    public void processAnnotation(Element element, Injection.Producer producer) {
        String forClass = null;
        try { // getting a Class cause an exception
            producer.forClass();
        } catch (MirroredTypeException mte) {
            TypeMirror tm = mte.getTypeMirror();
            forClass = AnnotationProcessorUtil.getClassName(tm);
        }
        if (forClass.equals(Void.class.getName()) && element.getKind().isClass() || element.getKind().isInterface()) {
            forClass = AnnotationProcessorUtil.getClassName((TypeElement) element);
        }
        String producerPkg = forClass.substring(0, forClass.lastIndexOf('.'));
        String shortClassName = forClass.substring(forClass.lastIndexOf('.') + 1);
        String variant = producer.variant();
        String producerClassName = shortClassName + "_Producer_" + variant;
        forClass = forClass.replace('$', '.');
        String lookupKey;
        if (producer.lookupVariant().length() == 0) {
            if (variant.length() == 0) {
                lookupKey = forClass;
            } else {
                lookupKey = forClass + '/' + variant;
            }
        } else if (producer.lookupVariant().trim().length() == 0) {
            lookupKey = forClass;
        } else {
            lookupKey = forClass + '/' + producer.lookupVariant();
        }
        String className = forClass;
        this.mappings.add(new Mapping(producerPkg, producerClassName, shortClassName, className, variant, lookupKey));
    }

    /**
     * Generate the files.
     */
    public void generateFiles() {
        Filer filer = processingEnv.getFiler();
        Messager messager = processingEnv.getMessager();
        for (Mapping mapping : this.mappings) {
            messager.printMessage(Kind.NOTE, "GENERATE PRODUCER " + mapping.producerPkg + "."
                    + mapping.producerClassName);
            try {
                FileObject file = filer.createResource(StandardLocation.SOURCE_OUTPUT, mapping.producerPkg,
                        mapping.producerClassName + ".java");
                messager.printMessage(Kind.NOTE, file.toUri().toString());
                PrintWriter writer = new PrintWriter(file.openWriter());
                BufferedReader reader = new BufferedReader(new InputStreamReader(
                        LookupKeyProducerGenerator.class.getResourceAsStream("Producer.java.template"), "UTF8"));
                reader.lines().map(s -> s.replaceAll("\\{producerPkg\\}", escapeDollar(mapping.producerPkg)))
                        .map(s -> s.replaceAll("\\{producerClassName\\}", escapeDollar(mapping.producerClassName)))
                        .map(s -> s.replaceAll("\\{shortClassName\\}", escapeDollar(mapping.shortClassName)))
                        .map(s -> s.replaceAll("\\{className\\}", escapeDollar(mapping.className)))
                        .map(s -> s.replaceAll("\\{variant\\}", escapeDollar(mapping.variant)))
                        .map(s -> s.replaceAll("\\{lookupKey\\}", escapeDollar(mapping.lookupKey)))
                        .forEach(writer::println);
                writer.close();
            } catch (IOException ioe) {
                messager.printMessage(Kind.ERROR, ioe.getMessage());
            }
        }
    }

    // escape $ sign :
    // LookupKeyProducerTest$B2_Producer_var2 -> LookupKeyProducerTest\$B2_Producer_var2
    // because the result string is used as replacement regexp, and the $ sign
    // stand for a backreference
    String escapeDollar(String string) {
        return string.replaceAll("\\$", "\\\\\\$");
    }

}
