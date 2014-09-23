package ml.alternet.discover.gen;

import java.io.IOException;
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

import ml.alternet.discover.DiscoveryService;
import ml.alternet.discover.LookupKey;
import ml.alternet.discover.LookupKeys;
import ml.alternet.util.AnnotationProcessorUtil;

/**
 * Generate META-INF xservice files.
 * 
 * For each lookup key, the file :
 * 
 * <pre>
 * META-INF/xservices/[forClass](/[variant])
 * </pre>
 * 
 * is generated with the content :
 * 
 * <pre>
 * (# default)
 * [implClass]
 * </pre>
 * 
 * @see LookupKey
 * @see DiscoveryService
 * 
 * @author Philippe Poulard
 */
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class LookupKeyProcessor extends AbstractProcessor {

    static Set<String> TYPES = new HashSet<String>();

    static {
        TYPES.add(LookupKey.class.getCanonicalName());
        TYPES.add(LookupKeys.class.getCanonicalName());
    }

    Set<Mapping> mappings = new HashSet<Mapping>();

    static class Mapping {

        String xservice;
        String implClass;
        boolean byDefault;

        public Mapping(String xservice, String implClass, boolean byDefault) {
            this.xservice = xservice;
            this.implClass = implClass;
            this.byDefault = byDefault;
        }

        @Override
        public boolean equals(Object obj) {
            return xservice.equals(((Mapping) obj).xservice);
        }

        @Override
        public int hashCode() {
            return xservice.hashCode();
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
        messager.printMessage(Kind.NOTE, LookupKeyProcessor.class.getCanonicalName() + " : end");
        return true;
    }

    public void processAnnotations(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        Elements eltUtils = processingEnv.getElementUtils();

        TypeElement elementLookup = eltUtils.getTypeElement(LookupKeys.class.getCanonicalName());
        Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(elementLookup);
        for (Element element : elements) {
            LookupKeys lks = element.getAnnotation(LookupKeys.class);
            if (lks != null) {
                for (LookupKey lk : lks.value()) {
                    processLookupKey(element, lk);
                }
            }
        }
        elementLookup = eltUtils.getTypeElement(LookupKey.class.getCanonicalName());
        elements = roundEnv.getElementsAnnotatedWith(elementLookup);
        for (Element element : elements) {
            LookupKey lk = element.getAnnotation(LookupKey.class);
            if (lk != null) {
                processLookupKey(element, lk);
            }
        }
    }

    public void processLookupKey(Element element, LookupKey lk) {
        String forClass = null;
        try { // getting a Class cause an exception
            lk.forClass();
        } catch (MirroredTypeException mte) {
            TypeMirror tm = mte.getTypeMirror();
            forClass = AnnotationProcessorUtil.getClassName(tm);
        }
        if (forClass.equals(Void.class.getName()) && (element.getKind().isClass() || element.getKind().isInterface())) {
            forClass = AnnotationProcessorUtil.getClassName((TypeElement) element);
        }
        String implClass = null;
        try { // getting a Class cause an exception
            lk.implClass();
        } catch (MirroredTypeException mte) {
            TypeMirror tm = mte.getTypeMirror();
            implClass = AnnotationProcessorUtil.getClassName(tm);
        }
        if (implClass.equals(Void.class.getName()) && element.getKind().isClass()) {
            implClass = AnnotationProcessorUtil.getClassName(((TypeElement) element));
        }
        forClass = forClass.replace('$', '.');
        String xservice;
        if (lk.variant().length() > 0) {
            xservice = forClass + "/" + lk.variant();
        } else {
            xservice = forClass;
        }
        this.mappings.add(new Mapping(xservice, implClass, lk.byDefault()));
    }

    public void generateFiles() {
        for (Mapping mapping : this.mappings) {
            Filer filer = processingEnv.getFiler();
            Messager messager = processingEnv.getMessager();
            messager.printMessage(Kind.NOTE, "LookupKey : " + mapping.xservice + " -> " + mapping.implClass
                    + (mapping.byDefault ? " (default)" : ""));
            try {
                FileObject file = filer.createResource(StandardLocation.CLASS_OUTPUT, "", "META-INF/xservices/"
                        + mapping.xservice);
                messager.printMessage(Kind.NOTE, file.toUri().toString());
                PrintWriter writer = new PrintWriter(file.openWriter());
                if (mapping.byDefault) {
                    writer.println("# default");
                }
                writer.println(mapping.implClass);
                writer.close();
            } catch (IOException ioe) {
                messager.printMessage(Kind.ERROR, ioe.getMessage());
            }
        }
    }

}
