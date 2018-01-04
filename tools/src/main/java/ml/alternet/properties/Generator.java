package ml.alternet.properties;

import static ml.alternet.properties.NamesUtil.asClassName;
import static ml.alternet.properties.NamesUtil.asPropName;

import java.io.CharArrayWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.annotation.Generated;

import ml.alternet.misc.Thrower;
import ml.alternet.misc.Type;
import ml.alternet.misc.Type.Kind;
import ml.alternet.util.NumberUtil;

/**
 * Create Java source files from property schemas (template of property files).
 *
 * @author Philippe Poulard
 */
public class Generator {

    static final Logger LOG = Logger.getLogger(Generator.class.getCanonicalName());

    // INPUT : each properties template is used to generate a java source class
    Stream<PropertiesTemplate> propertiesTemplates;

    // OUTPUT : given a class name, create a writer for the generated java source code
    Function<Type, OutputStream> outputFactory;

    /* ===================================================
     *      This part is about the configuration,
     *      that is to say setting the templates to
     *      process and the target output
     * ===================================================
     */

    static class PropertiesTemplate {

        InputStream input; // the properties template
        Type className; // the target class name

        PropertiesTemplate(InputStream is) {
            this.input = is;
            this.className = Type.of(null, ""); // the target class name will be set later
        }

        PropertiesTemplate(Path path, Type name) {
            LOG.info("Reading properties file " + path.toAbsolutePath());
            try {
                this.input = new FileInputStream(path.toFile());
                this.className = name;
            } catch (FileNotFoundException e) {
                Thrower.doThrow(e);
            }
        }

    }

    /**
     * Set the properties templates to process. Each template will
     * cause the generation of a Java source class.
     *
     * @param propertiesTemplates The stream of templates to process.
     * @return <code>this</code>, for chaining.
     */
    public Generator setPropertiesTemplates(Stream<InputStream> propertiesTemplates) {
        this.propertiesTemplates = propertiesTemplates.map(is -> new PropertiesTemplate(is));
        return this;
    }

    /**
     * Set the properties template to process.
     *
     * @param propertyTemplateFile The template file.
     * @param className The qualified name of the Java target, can be <code>null</code>
     *      if the first property definition is a qualified name definition (please refer to the
     *      documentation).
     * @return <code>this</code>, for chaining.
     */
    public Generator setPropertiesTemplateFile(File propertyTemplateFile, String className) {
        setPropertiesTemplateFile(propertyTemplateFile.toPath(), className);
        return this;
    }

    /**
     * Set the properties template to process.
     *
     * @param propertyTemplatePath The template path.
     * @param className The qualified name of the Java target, can be <code>null</code>
     *      if the first property is a qualified name definition (please refer to the
     *      documentation).
     * @return <code>this</code>, for chaining.
     */
    public Generator setPropertiesTemplateFile(Path propertyTemplatePath, String className) {
        Type name = className == null ? Type.of(null, "") : Type.of(className);
        this.propertiesTemplates = Stream.of(
            new PropertiesTemplate(propertyTemplatePath.toAbsolutePath(), name)
        );
        return this;
    }

    /**
     * Set the properties templates to process. Each template will cause the
     * generation of a Java source class.
     *
     * @param propertiesTemplatesDirectory The directory contains the properties
     *      files to process (including subdirectories) ; the subpaths are used
     *      for the package name of the Java target, unless it is override by the
     *      first property definition (please refer to the documentation).
     * @return <code>this</code>, for chaining.
     */
    public Generator setPropertiesTemplatesDirectory(File propertiesTemplatesDirectory) {
        setPropertiesTemplatesDirectory(propertiesTemplatesDirectory.toPath());
        return this;
    }

    /**
     * Set the properties templates to process. Each template will cause the
     * generation of a Java source class.
     *
     * @param propertiesTemplatesDirectory The directory contains the properties
     *      files to process (including subdirectories) ; the subpaths are used
     *      for the package name of the Java target, unless it is override by the
     *      first property definition (please refer to the documentation).
     * @return <code>this</code>, for chaining.
     */
    public Generator setPropertiesTemplatesDirectory(Path propertiesTemplatesDirectory) {
        Path dir = propertiesTemplatesDirectory.toAbsolutePath();
        try {
            this.propertiesTemplates = Files.find(
                dir,
                30,
                (p, f) -> f.isRegularFile() && p.getFileName().toString().endsWith(".properties"),
                FileVisitOption.FOLLOW_LINKS
            ).map(file -> {
                Path name = dir.relativize(file);
                String pkg = name.getParent() == null ? null : name.getParent().toString().replaceAll("/", ".");
                String fileName = name.getFileName().toString();
                String cl = fileName.substring(0, fileName.length() - ".properties".length());
                cl = asClassName(cl);
                return new PropertiesTemplate(file, Type.of(pkg, cl));
            });
        } catch (IOException e) {
            Thrower.doThrow(e);
        }
        return this;
    }

    /**
     * The directory to write the target Java files.
     *
     * @param outputDirectory The output directory.
     * @return <code>this</code>, for chaining.
     */
    public Generator setOutputDirectory(File outputDirectory) {
        setOutputDirectory(outputDirectory.toPath());
        return this;
    }

    /**
     * The directory to write the target Java files.
     *
     * @param outputDirectory The path to the output directory.
     * @return <code>this</code>, for chaining.
     */
    public Generator setOutputDirectory(Path outputDirectory) {
        setOutputFactory(className -> {
            Type type = Type.of(className);
            String pkg = type.getPackageName();
            Path baseDirectory = outputDirectory;
            if (pkg != null) {
                baseDirectory = outputDirectory.resolve(pkg.replaceAll("\\.", "/"));
            }
            try {
                Files.createDirectories(baseDirectory);
                File javaFile = baseDirectory.resolve(type.getSimpleName() + ".java").toFile();
                LOG.info("Writing to " + javaFile);
                return new FileOutputStream(javaFile);
            } catch (IOException e) {
                return Thrower.doThrow(e);
            }
        });
        return this;
    }

    /**
     * Given a Java class name, create a write stream for the Java source code.
     *
     * @param outputFactory The output factory.
     * @return <code>this</code>, for chaining.
     */
    public Generator setOutputFactory(Function<Type, OutputStream> outputFactory) {
        this.outputFactory = outputFactory;
        return this;
    }

    /* ===================================================
     *      This part is about the generation
     *      that is to say collect the definitions
     *      and then generate the code
     * ===================================================
     */

    /**
     * Launch the generation of the Java source classes.
     */
    public void generate() {
        assert this.propertiesTemplates != null;
        assert this.outputFactory != null;
        this.propertiesTemplates.forEach(p -> generate(p));
    }

    // process a single property schema
    void generate(PropertiesTemplate schema) {
        try {
            PropertiesCollector collector = new PropertiesCollector(new MainClassDef(schema.className));
            collector.load(schema.input);  // collect the definitions ; see #put(key, value)
            // generate the main class and dependencies
            collector.mainClass.generateOuter(null); // null : let resolve the writer
        } catch (IOException e) {
            Thrower.doThrow(e);
        }
    }

    @SuppressWarnings("serial")
    static class PropertiesCollector extends Properties {

        MainClassDef mainClass; // the root tree

        PropertiesCollector(MainClassDef mainClass) {
            this.mainClass = mainClass;
        }

        @Override
        public synchronized Object put(Object key, Object value) {
            String strVal = (String) value;
            if (".".equals(key)) { // process "directives"
                // e.g.   . = #org.example.conf.TheConf
                String strType = strVal.substring(1).trim();
                if (strVal.startsWith("!")) {
                    // e.g.    . = ! java.util.List
                    //          => import
                    if (strType.length() == 0) {
                        throw new IllegalArgumentException("Empty import \". = !\", a class name was expected.");
                    }
                    Type iType = Type.parseTypeDefinition(strType);
                    strType = iType.toString();
                    if (iType instanceof GenericArrayType || iType instanceof ParameterizedType) {
                        throw new IllegalArgumentException("Can't import the composed type \""
                            + strType + "\", a simple type was expected.");
                    }
                    this.mainClass.imports.add(strType);
                    LOG.finest("Defining Import " + strType);
                } else if (strVal.startsWith("@")) {
                    // e.g.    . = @
                    //         . = @ Adapter.map("subConf.file", File::new)
                    //         . = @ Adapter.mapList("subConf.subConf.listOfStuff", Stuff::parseStuff)
                    this.mainClass.addAdapter(strType);
                    LOG.finest("Defining Adapter " + strType);
                } else if (strVal.startsWith("#")) {
                    if (strType.length() == 0) {
                        throw new IllegalArgumentException("Type definition incomplete \". = #\", a class name was expected.");
                    }
                    Type type = Type.parseTypeDefinition(strType); // org.example.conf.TheConf
                    if (type instanceof GenericArrayType || type instanceof ParameterizedType) {
                        throw new IllegalArgumentException("Can't define the composed type \""
                            + strType + "\", a simple type was expected.");
                    }
                    // the target class name is set if unknown, or override
                    this.mainClass.setType(type);
                    this.mainClass.name = type.getSimpleName();
                    LOG.finest("Defining main type " + type);
                } else {
                    throw new IllegalArgumentException("Unknown directive \""
                        + strVal + "\", valid directives are starting with # ! or @");
                }

            } else { // process "property definitions"
                ClassDef current = this.mainClass; // start with the root
                String strKey = (String) key;
                // breakdown the key e.g. "a.b.c.d"   in parts e.g.   a -> b -> c -> d
                //                     or "a.b.c.d."  in              a -> b -> c -> d
                String[] keys = strKey.split("\\.");
                for (int i = 0 ; i < keys.length ; i++) {
                    String namePart = keys[i];
                    if ("*".equals(namePart)) { // e.g.   map.*.geo=48.864716, 2.349014
                        current.isMap  = true; // tell the current node to accept unknown keys
                        this.mainClass.imports.add($.class.getCanonicalName());
                        LOG.finest("Defining map property " + strKey);
                        break; // can't process next name parts
                    }
                    boolean leaf = i == keys.length - 1;
                    boolean forGroup = leaf && strKey.endsWith(".");
                    // type for the group, e.g.   gui.window. = #org.example.conf.Window
                    ValueDef def = current.getOrCreateDef(namePart, leaf, forGroup);
                    if (leaf) { // leaf
                        def.setType(keys[i], strVal);
                    } else { // subtree
                        current = (ClassDef) def;
                    }
                    LOG.finest("Defining property " + strKey + " : " + def.type);
                }
            }
            return value;
        }

    }

    // base class for property definitions
    abstract class Def {

        String name; // name of the property

        Def() { }

        Def(String name) {
            this.name = name;
        }

        abstract void generate(Writindent w);

        abstract Set<String> getImports();

    }

    class ValueDef extends Def {

        ClassDef parent; // null for MainClassDef only
        Type type;
        boolean isExistingType = false;
        boolean isTrue = false; // default value for booleans
        boolean isMap = false; // contains unknown names, e.g. "map.*.geo=48.864716, 2.349014"

        ValueDef() { }

        ValueDef(String name) {
            super(name);
        }

        ClassDef replaceWithClassDef() {
            ClassDef cd = new ClassDef(this.name);
            return replaceWith(cd);
        }

        ClassDef replaceWith(ClassDef cd) {
            cd.parent = this.parent;
            cd.isMap = this.isMap;
            cd.isTrue = this.isTrue;
            cd.isExistingType = this.isExistingType;
            cd.parent.defs = cd.parent.defs.stream()
                .map(def -> def.name.equals(name) ? cd : def)
                .collect(Collectors.toList());
            return cd;
        }

        @Override
        Set<String> getImports() {
            return this.parent.getImports();
        }

        void setType(Type type) {
            this.type = type;
        }

        void setType(String name, String value) {
            if (value.startsWith("\"")) {
                setType(Type.of(String.class));
                this.isExistingType = true;
                int i = value.indexOf('"', 1);
                if (i > 0) {
                    String after = value.substring(i + 1).trim();
                    if (after.startsWith(",")) {
                        setType(Type.of(List.class).withTypeParameters(this.type));
                    }
                }
                return;
            }

            boolean list = false;

            if (value.contains("|") && ! value.startsWith("$")) {
                // enum
                boolean ownClass = value.startsWith("#");
                Type type;
                if (ownClass) {
                    // e.g                #Status Inactive | Active | Pending
                    //    or  #org.example.Status Inactive | Active | Pending
                    value = value.substring(1).trim();
                    Type.Parsed typeParsed = new Type.Parsed(value);
                    type = typeParsed.get();
                    value = typeParsed.toString().trim();
                } else {
                    // e.g    Inactive | Active | Pending
                    type = Type.of(asClassName(name));
                }
                setType(type);
                if (value.contains(",")) {
                    // list of enum
                    // e.g    Inactive | Active | Pending, Active
                    list = true;
                    value = value.split(",")[0];
                }
                // NOTE : unlike ClassDef that holds both the field and the class definition
                //    EnumDef just hold the enum definition
                EnumDef ed = new EnumDef(this.name, this.type, value.split("\\s*\\|\\s*"));
                ed.parent = this.parent;
                this.parent.defs.add(ed);
            } else if (value.startsWith("#") || value.startsWith("$")) {
                this.isExistingType = value.startsWith("$");
                // class  e.g.  $java.awt.Point             => existing type
                //              #org.example.data.Status    => type to create
                value = value.substring(1).trim();
                Type.Parsed typeParsed = new Type.Parsed(value);
                Type type = typeParsed.get();
                if (! this.isExistingType  && (type instanceof GenericArrayType || type instanceof ParameterizedType)) {
                    throw new IllegalArgumentException("Can't define the composed type \""
                                +  type + "\", a simple type was expected.");
                }
                setType(type);
                value = typeParsed.toString().trim();
            } else if ("true".equals(value) || "false".equals(value)) {
                // boolean
                setType(Type.of("boolean"));
                this.isExistingType = true;
                this.isTrue = "true".equals(value); // default value
            } else if (value.startsWith("file:/")) {
                // file
                setType(Type.of(File.class));
                this.isExistingType = true;
            } else {
                boolean wasJava = false;
                if (value.startsWith("java:")) {
                    String javaValue = value.substring("java:".length());
                    if (! javaValue.startsWith("/")) {
                        wasJava = true;
                        Type type;
                        if (javaValue.startsWith("?")) {
                            type = Type.of("?");
                            value = javaValue.substring(1).trim();
                        } else {
                            Type.Parsed parsedType = new Type.Parsed(javaValue);
                            type = parsedType.get();
                            // e.g.    java:com.mysql.jdbc.Driver
                            // but not java:/comp/env/jdbc/someDB
                            value = parsedType.toString().trim();
                        }
                        setType(Type.of(Class.class).withTypeParameters(type));
                        this.isExistingType = true;
                    }
                }
                if (!wasJava) {
                    if (isURI(value)) {
                        // URI
                        setType(Type.of(URI.class));
                        this.isExistingType = true;
                    } else if (value.matches("\\*+")) {
                        // password
                        try {
                            String pwdCl = "ml.alternet.security.Password";
                            Class.forName(pwdCl);
                            setType(Type.of(pwdCl));
                        } catch (ClassNotFoundException e) {
                            setType(Type.of(char.class).asArrayType());
                        }
                        this.isExistingType = true;
                    } else {
                        try {
                            LocalDateTime.parse(value);
                            setType(Type.of(LocalDateTime.class));
                            this.isExistingType = true;
                        } catch (DateTimeException dte) {
                            // not a date time
                            try {
                                LocalDate.parse(value);
                                setType(Type.of(LocalDate.class));
                                this.isExistingType = true;
                            } catch (DateTimeException de) {
                                // not a date
                                try {
                                    LocalTime.parse(value);
                                    setType(Type.of(LocalTime.class));
                                    this.isExistingType = true;
                                } catch (DateTimeException te) {
                                    // not a time
                                    try {
                                        Number n = NumberUtil.parseNumber(value);
                                        Class<?> c = n.getClass();
                                        setType(Type.of(c).unbox());
                                        this.isExistingType = true;
                                    } catch (NumberFormatException e) {
                                        // not a number
                                        setType(Type.of(String.class)); // fallback
                                        this.isExistingType = true;
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (value.contains(",")) {
                list = true;
            }
            if (list) {
                setType(Type.of(List.class).withTypeParameters(this.type));
            }
        }

        // rename if an ancestor type has the same name
        String checkAncestors(String cl) {
            while (hasSameAncestorName(cl)) {
                char c = cl.charAt(cl.length() - 1);
                if (c == '_' || c == '9') {
                    c = '0';
                } else if (c >= '0' && c < '9') {
                    cl = cl.substring(0, cl.length() - 1);
                    c++;
                } else {
                    c = '_';
                }
                cl = cl + c;
            }
            return cl;
        }

        // indicates whether an ancestor type has the same name
        boolean hasSameAncestorName(String cl) {
            if (this.parent == null) {
                return false;
            } else {
                if (this.parent.type.getSimpleName().equals(cl)) {
                    return true;
                } else {
                    return this.parent.hasSameAncestorName(cl);
                }
            }
        }

        String defaultIfBoolean() {
            return "boolean".equals(this.type.getSimpleName()) && this.isTrue ? " = true" : "";
        }

        void generateOuter(Writindent w) throws IOException { }

        @Override
        void generate(Writindent w) {
            String propName = asPropName(this.name);
            if (this.isMap) {
                // map.*.geo=48.864716, 2.349014     => map with any prop field
                // e.g.    public $ map = new $() {};
                w.writeln("public $ #0 = new $() {};", propName);
            } else if (this instanceof ClassDef) {
                ClassDef cd = (ClassDef) this;
                String className = this.type.toString(t -> omitPackage(t));
                // subconf.prop=...    => Subconf class with "prop" field
                if (cd.type.getPackageName() == null && ! this.isExistingType) {
                    className = checkAncestors(className);
                    cd.type = Type.of(null, className);
                }
                w.writeln("public #0 #1;", className, propName);
                w.writeln();
                if (! this.isExistingType) {
                    try {
                        generateOuter(w);
                    } catch (IOException e) {
                        Thrower.doThrow(e);
                    }
                }
            } else {
                String typeName = this.type.toString(t -> omitPackage(t));
                // e.g.    public boolean b1 = true;
                w.writeln("public #0 #1#2;", typeName, propName, defaultIfBoolean());
            }
        }

        boolean omitPackage(Type type) {
            if (hasSimpleNameInScope(type, this)) {
                return false;
            }
            if (type.getKind() == Kind.OTHER) {
                getImports().add(type.getQualifiedName());
            }
            return true;
        }

        boolean hasSimpleNameInScope(Type type, ValueDef def) {
            if (this.parent == null) {
                return false;
            } else {
                // type is in scope...
                String cl = type.getSimpleName();
                // ...if an ancestor has the same simple name
                if (this.parent.type == null && ! this.parent.isExistingType
                    && ! this.parent.isMap && asClassName(this.parent.name).equals(cl))
                {
                    return true;
                }
                // ...or if a sibling has the same simple name
                if (this.parent.defs.stream().filter(d -> {
                    if (d == def // don't compare itself
                        || ! (d instanceof ClassDef) || d.isExistingType || d.isMap )
                    {          // for new class only
                        return false;
                    }
                    if (d.type == null) {
                        return asClassName(d.name).equals(cl);
                    } else {
                        return d.type.getSimpleName().equals(cl)
                            && ((d.type.getPackageName() == null && type.getPackageName() != null)
                              || ! d.type.getPackageName().equals(type.getPackageName()));
                    }
                }).findFirst().isPresent())
                {
                    return true;
                }
                return this.parent.hasSimpleNameInScope(type, def);
            }
        }

        @Override
        public String toString() {
            return "" + this.type + ' ' + this.name;
        }
    }

    class ClassDef extends ValueDef {

        Set<String> imports = new TreeSet<>(); // e.g. "java.io.IOException"
        List<ValueDef> defs = new LinkedList<>();

        ClassDef(Type clName) {
            super(clName.getSimpleName());
            this.type = clName;
            this.imports.add(Generated.class.getCanonicalName());
        }

        ClassDef(String name) {
            super(name);
            this.type = Type.of(asClassName(name));
            this.imports.add(Generated.class.getCanonicalName());
        }

        @Override
        Set<String> getImports() {
            return this.imports;
        }

        ValueDef getOrCreateDef(String name, boolean leaf, boolean forGroup) {
            Optional<ValueDef> def = defs.stream()
                .filter(d -> d.name.equals(name))
                .findFirst();
            ValueDef valDef;
            if (def.isPresent()) {
                valDef = def.get();
                    if (leaf && ! forGroup && valDef instanceof ClassDef) {
                        ClassDef cd = (ClassDef) valDef;
                        ValueDef dollar = new ValueDef("$");
                        dollar.parent = cd;
                        cd.defs.add(0, dollar);
                        return dollar;
                    }
                    if (! (valDef instanceof ClassDef)
                            && ((leaf && forGroup) || ! leaf))
                    {
                        ClassDef cd = valDef.replaceWithClassDef();
                        cd.isExistingType = false;
                        valDef.name = "$";
                        valDef.parent = cd;
                        cd.defs.add(valDef);
                        return cd;
                    }
                return valDef;
            } else if (! leaf || forGroup) {
                valDef = new ClassDef(name);
            } else {
                valDef = new ValueDef(name);
            }
            valDef.parent = this;
            this.defs.add(valDef);
            return valDef;
        }

        String statik() {
            return this.type.getPackageName() == null ? "static " : "";
        }

        void generateInner(Writindent w) {
            String impl = this.isMap ? " implements $" : "";
            String className = this.type.getSimpleName();
            if (! this.isExistingType && this.type.getPackageName() != null) {
                className = checkAncestors(className);
            }
            // e.g.    public class SubConf implements $ {
            w.writeln("public #0class #1#2 {", statik(), className, impl);
            w.writeln();
            w.up();
            this.defs.stream().forEach(d -> d.generate(w));
            generateFooter(w);
            w.down();
            w.writeln("}");
            w.writeln();
        }

        void generateFooter(Writindent w) { } // footer before "}" <- class end

        @Override
        void generateOuter(Writindent w) throws IOException {
            boolean ownClass = this.type.getPackageName() != null || w == null;
            // write the content of the class in a buffer
            // this allow to collect required imports
            CharArrayWriter buf = new CharArrayWriter(8128);
            {
                Writindent contentWriter = new Writindent(buf);
                if (this.type.getPackageName() == null && w != null) {
                    contentWriter.level = w.level;
                }
                generateInner(contentWriter);
                contentWriter.close();
            }

            if (ownClass) {
                LOG.info("Generating class " + this.type);
                // write imports, @Generated, etc
                w = new Writindent(
                    new OutputStreamWriter(
                        Generator.this.outputFactory.apply(this.type),
                        StandardCharsets.UTF_8
                    )
                );
                generateHeader(w, this.type.getPackageName());
            } else {
                parent.getImports().addAll(getImports());
            }

            // place to insert the content
            w.w.write(buf.toCharArray());
            if (ownClass) {
                w.close();
            }
        }

        void generateHeader(Writindent w, String pkg) {
            if (pkg != null) {
                w.writeln("package #0;", pkg);
                w.writeln();
            }
            this.imports.forEach(i -> w.writeln("import #0;", i));
            if (this.imports.size() > 0) {
                w.writeln();
            }
            w.writeln("/**");
            generateTitleComment(w);
            w.writeln(" * DO NOT EDIT : this class has been generated !");
            w.writeln(" */");
            w.writeln("@Generated(value=\"#0\", date=\"#1\")", Generator.class.getName(), LocalDate.now().toString());
        }

        void generateTitleComment(Writindent w) { }

    }

    // a Java enum
    // NOTE : unlike ClassDef that holds both the field and the class definition
    //              EnumDef just hold the enum definition ; the field is managed apart
    class EnumDef extends ClassDef {

        String[] values;

        EnumDef(String propName, Type propType, String[] values) {
            super(propType);
            this.name = propName;
            this.values = values;
        }

        @Override
        public void generateInner(Writindent w) {
            w.writeln("public #0enum #1 {\n", statik(), this.type.getSimpleName());
            w.up();
            w.writeln(String.join(", ", Arrays.asList(this.values)));
            w.down();
            w.writeln();
            w.writeln("}");
        }

        @Override
        void generate(Writindent w) {
            try {
                generateOuter(w);
            } catch (IOException e) {
                Thrower.doThrow(e);
            }
        }

        @Override
        void generateTitleComment(Writindent w) { }

    }

    class MainClassDef extends ClassDef {

        List<String> adapters; // Java code for getting an instance of adapter
                               // e.g. "Adapter.map(Document.class, Utils::parseXml)"

        MainClassDef(Type clName) {
            super(clName);
            this.isExistingType = false;
        }

        void addAdapter(String adapter) {
            if (this.adapters == null) {
                this.adapters = new LinkedList<>();
            }
            if (adapter.length() > 0) {
                this.adapters.add(adapter);
            }
        }

        @Override
        void generateTitleComment(Writindent w) {
            w.writeln(" * Configuration structure for \"#0.properties\"", asPropName(name));
            w.writeln(" * ");
        }

        @Override
        void generateFooter(Writindent w) {
            if (this.adapters != null) {
                this.imports.addAll(
                    Stream.of(InputStream.class, Reader.class, IOException.class,
                                Properties.class, Binder.class)
                        .map(c -> c.getCanonicalName())
                        .collect(Collectors.toList()
                    )
                );
                if (! this.adapters.isEmpty()) {
                    this.imports.add(Binder.Adapter.class.getCanonicalName());
                }
                Stream.of("InputStream", "Reader", "Properties")
                    .forEach(input -> {
                        IntStream.range(0, 2).forEach(n -> { // repeat twice
                            w.writeln("");
                            String statik = n == 0 ? "static " : "";
                            String method = n == 0 ? "unmarshall" : "update";
                            w.writeln("public #0#1 #2(#3 properties) throws IOException {",
                                    statik, this.type.getSimpleName(), method, input);
                            w.up();
                            w.writeln("return Binder.unmarshall(");
                            w.up();
                            w.writeln("properties,");
                            if (n == 0) {
                                w.write("#0#1.class", w.indent(), this.type.getSimpleName());
                            } else {
                                w.write("#0this", w.indent());
                            }
                            if (! this.adapters.isEmpty()) {
                                this.adapters.stream().forEach(a -> {
                                    w.write(",\n");
                                    w.write("#0#1", w.indent(), a);
                                });
                            }
                            w.write("\n");
                            w.down();
                            w.writeln(");");
                            w.down();
                            w.writeln("}");
                        });
                    }
                );
            }
        }
    }

    static boolean isURI(String value) {
        try {
            return new URI(value).getScheme() != null;
        } catch (URISyntaxException e) {
            return false;
        }
    }

    static class Writindent {

        Writindent(Writer w) {
            this.w = w;
        }

        Writer w;

        int level = 0;

        public void writeln() {
            try {
                w.write("\n");
            } catch (IOException e) {
                Thrower .doThrow(e);
            }
        }

        String indent() {
            return IntStream.range(0, level * 4).mapToObj(i -> " ").collect(Collectors.joining());
        }

        void writeln(String line, String... vars) {
            try {
                w.write(indent());
                write(line, vars);
                w.write("\n");
            } catch (IOException e) {
                Thrower .doThrow(e);
            }
        }

        void write(String line, String... vars) {
            for (int i = 0 ; i < vars.length ; i++) {
                line = line.replaceAll("#" + i, Matcher.quoteReplacement(vars[i]));
            }
            try {
                w.write(line);
            } catch (IOException e) {
                Thrower .doThrow(e);
            }
        }

        void up() {
            level++;
        }

        void down() {
            level--;
        }

        void close() {
            try {
                w.flush();
                w.close();
            } catch (IOException e) {
                Thrower.doThrow(e);
            }
        }

    }

}
