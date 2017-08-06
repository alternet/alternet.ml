package ml.alternet.util;

import ml.alternet.misc.Thrower;

/**
 * Generate the byte code of a class that implements an
 * interface, but without methods implementation.
 *
 * This allow to get an instance of an interface.
 *
 * @author Philippe Poulard
 */
@Util
public abstract class ByteCodeFactory {

    /**
     * Create a new instance of an interface :
     *
     * <pre>Foo foo = ByteCodeUtil.newInstance(Foo.class);</pre>
     *
     * is similar to :
     *
     * <pre>Foo foo = new Foo() {};</pre>
     *
     * except that you are not compelled to supply all the
     * implementations of the interface.
     *
     * @param interFace The interface to which we want an instance.
     *      If it is not an interface, it should fail.
     * @param <T> The return type, same as the interface.
     * @return An instance of this interface,
     *      without methods implementations.
     *      Its class name has the same name than the
     *      interface but ends with a "$"
     *
     * @throws InstantiationException When the bytecode failed.
     * @throws IllegalAccessException When the bytecode failed.
     * @throws ClassNotFoundException When the bytecode failed.
     */
    @SuppressWarnings("unchecked")
    public <T> T newInstance(Class<T> interFace) throws InstantiationException, IllegalAccessException,
            ClassNotFoundException
    {
        return (T) getClass(interFace).newInstance();
    }

    /**
     * Get the singleton instance of an interface :
     *
     * <pre>Foo foo = ByteCodeUtil.getInstance(Foo.class);</pre>
     *
     * is similar to :
     *
     * <pre>public class FooImpl implements Foo {
     *    public static final Foo SINGLETON = new FooImpl();
     *}
     *Foo foo = FooImpl.SINGLETON;</pre>
     *
     * except that you are not compelled to supply all the
     * implementations of the interface.
     *
     * @param interFace The interface to which we want an instance.
     *      If it is not an interface, it should fail ;
     *      that interface MUST NOT DEFINED the field
     *      "SINGLETON".
     * @param <T> The interface type.
     * @return An instance of this interface,
     *      without methods implementations.
     *      Its class name has the same name than the
     *      interface but ends with a "$"
     *
     * @throws InstantiationException When the bytecode failed.
     * @throws IllegalAccessException When the bytecode failed.
     * @throws ClassNotFoundException When the bytecode failed.
     * @throws IllegalArgumentException When the bytecode failed.
     * @throws NoSuchFieldException When the bytecode failed.
     * @throws SecurityException When the bytecode failed.
     */
    @SuppressWarnings("unchecked")
    public <T> T getInstance(Class<T> interFace) throws InstantiationException, IllegalAccessException,
            ClassNotFoundException, IllegalArgumentException, NoSuchFieldException, SecurityException
    {
        return (T) getClass(interFace).getField(singletonName(interFace.getName())).get(null);
    }

    /**
     * Create the concrete class of an interface :
     *
     * <pre>Foo foo = ByteCodeUtil.newInstance(Foo.class);</pre>
     *
     * You are not compelled to supply all the
     * implementations of the interface.
     *
     * @param interFace The interface to which we want
     *      to generate a concrete implementation.
     *      If it is not an interface, it should fail.
     * @return A concrete class of this interface,
     *      without methods implementations.
     *      Its name has the same name than the
     *      interface but ends with a "$"
     *
     * @throws IllegalAccessException When the bytecode failed.
     * @throws ClassNotFoundException When the bytecode failed.
     */
    public Class<?> getClass(Class<?> interFace) throws IllegalAccessException, ClassNotFoundException {
        String interfaceName = interFace.getName();
        String implName = interfaceName + "$";
        return Class.forName(implName, true, classLoader);
    }

    /**
     * By default the parent class is Object
     *
     * @return "<code>java/lang/Object</code>"
     */
    public String getParentClassName() {
        return "java/lang/Object";
    }

    /**
     * Return the name of the singleton for the generated class.
     *
     * @param interfaceName The name of the interface,
     *      allow the singleton name to be derived from it.
     * @return "SINGLETON"
     */
    public String singletonName(String interfaceName) {
        return "SINGLETON";
//        StringBuffer singletonName = new StringBuffer();
//        String nqName;
//        int lastSlash = interfaceName.lastIndexOf('/');
//        if (lastSlash++ == -1) {
//            nqName = interfaceName;
//        } else {
//            nqName = interfaceName.substring(lastSlash);
//        }
//        nqName.chars().forEach(c -> {
//            if (Character.isUpperCase(c) && singletonName.length() > 0) {
//                    singletonName.append("_").append(Character.toChars(c));
//                } else {
//                    singletonName.append(Character.toChars(Character.toUpperCase(c)));
//                }
//            });
//        return singletonName.toString();
    }

    /**
     * Use this classloader if you want to create a class that implements
     * an interface :
     * <pre>Foo foo = Class.forName(Foo.class.getName() + "$", true, ByteCode.CLASSLOADER);</pre>
     */
    final ClassLoader classLoader = new ClassLoader() {

        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            String implName = name.replace('.', '/');
            if (getParentClassName().equals(implName)) { // prevent java.lang.ClassCircularityError
                return ByteCodeFactory.class.getClassLoader().loadClass(name);
            }
            if (name.endsWith("$")) {
                String interfaceName = implName.substring(0, implName.length() - 1);
                String interfaceType = 'L' + interfaceName + ';';
                byte[] b = getByteCode(implName, interfaceName, interfaceType);
                Class<?> c = defineClass(name, b, 0, b.length);
                return c;
            } else {
                return ByteCodeFactory.class.getClassLoader().loadClass(name);
            }
        }

    };

    /**
     * Create the byte code of a class that implements an interface.
     *
     * NOTE : the implementation of this method is generated at
     * compile time.
     *
     * @param className The name of the class to generate.
     * @param interfaceName The name of the interface, must exist as a Class.
     * @param interfaceType The interface type as in bytecode signature.
     *
     * @return The byte code of the class.
     *
     * @see ClassLoader
     */
    public byte[] getByteCode(String className, String interfaceName, String interfaceType) {
        return new byte[0];
    }

    /**
     * Get the byte code factory instance.
     *
     * @param className The concrete class name of the byte code factory.
     *
     * @return A new instance.
     */
    public static ByteCodeFactory getInstance(String className) {
        try {
            return (ByteCodeFactory) Class.forName(className).newInstance();
        } catch (Exception e) {
            return Thrower.doThrow(e);
        }
    }

    //             MyClassImpl   MyParentClass     MyClass        LMyClass;      MY_CLASS
        //        String className, String parentClassName, String interfaceName,
        //                                  String interfaceType, String singletonName);

        //    {
//        // I don't want to use a big library for so few code generation
//        // but I used one to generate the following byte code creation
//        try {
//            ByteArrayOutputStream baos = new ByteArrayOutputStream(1024);
//            DataOutput bytecode = new DataOutputStream(baos);
//            bytecode.writeInt(0xcafebabe);  // magicNumber
//            bytecode.writeInt(52);          // version
//            bytecode.writeShort(12);        // CONSTANT POOL size
//            bytecode.writeByte(1);          // 1 text
//            bytecode.writeUTF(className);
//            bytecode.writeByte(7);          // 2 class ref
//            bytecode.writeShort(1);
//            bytecode.writeByte(1);          // 3 text
//            bytecode.writeUTF("java/lang/Object");
//            bytecode.writeByte(7);          // 4 class ref
//            bytecode.writeShort(3);
//            bytecode.writeByte(1);          // 5 text
//            bytecode.writeUTF(interfaceName);
//            bytecode.writeByte(7);          // 6 class ref
//            bytecode.writeShort(5);
//            bytecode.writeByte(1);          // 7 text
//            bytecode.writeUTF("<init>");
//            bytecode.writeByte(1);          // 8 text
//            bytecode.writeUTF("()V");
//            bytecode.writeByte(1);          // 9 text
//            bytecode.writeUTF("Code");
//            bytecode.writeByte(12);         // 10 name and type
//            bytecode.writeShort(7);
//            bytecode.writeShort(8);
//            bytecode.writeByte(10);         // 11 method ref
//            bytecode.writeShort(4);
//            bytecode.writeShort(10);
//            bytecode.writeShort(33);        // access flag = PUBLIC SUPER
//            bytecode.writeShort(2);         // this Class
//            bytecode.writeShort(4);         // super Class
//            bytecode.writeShort(1);         // interfaces size = 1
//            bytecode.writeShort(6);
//            bytecode.writeShort(0);         // fields size = 0
//            bytecode.writeShort(1);         // methods size = 1
//            bytecode.writeShort(1);         // access flag = PUBLIC = 1
//            bytecode.writeShort(7);         // name
//            bytecode.writeShort(8);         // type
//            bytecode.writeShort(1);
//            bytecode.writeShort(9);         // "Code"
//            bytecode.writeInt(17);
//            bytecode.writeShort(1);
//            bytecode.writeShort(1);
//            bytecode.writeInt(5);
//            bytecode.writeByte(42);  // 0x2a    ALOAD_0
//            bytecode.writeByte(183); // 0xb7    INVOKESPECIAL java/lang/Object <init> ()V
//            bytecode.writeShort(11);
//            bytecode.writeByte(177); // 0xb1    RETURN
//            bytecode.writeShort(0);
//            bytecode.writeShort(0);
//            bytecode.writeShort(0);
//            return baos.toByteArray();
//        } catch (IOException e) {
//            return Thrower.doThrow(e);
//        }
//    }

}
