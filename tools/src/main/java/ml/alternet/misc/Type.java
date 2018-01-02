package ml.alternet.misc;

import java.lang.reflect.GenericArrayType;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Represent a Java type (a class name that doesn't necessarily exist),
 * with special handling for primitives.
 *
 * @author Philippe Poulard
 */
public interface Type extends java.lang.reflect.Type {

    /**
     * Kind of Java types.
     *
     * @author Philippe Poulard
     */
    enum Kind {
        /** A primitive type, e.g "<code>int</code>" or "<code>float</code>". */
        PRIMITIVE,
        /** A type without package is in the default package. */
        DEFAULT_PACKAGE,
        /** A type in the "<code>java.lang</code>" package, e.g "<code>java.lang.String</code>". */
        JAVA_LANG_PACKAGE,
        /** A type that has a package not in the "<code>java.lang</code>" package. */
        OTHER;
    }

    /**
     * Create a type from the qualified name of a class,
     * that is to say the package name (if any) followed by the simple
     * name of the class.
     *
     * @param qualifiedName The qualified name of a class.
     *
     * @return The actual type.
     */
    static Type of(String qualifiedName) {
        Type$ type = new Type$(qualifiedName);
        return type.checkNative();
    }

    /**
     * Create a type from a package and a simple name.
     *
     * @param pkg The package name, or <code>null</code>.
     * @param cl The simple name of the class
     *
     * @return The actual type.
     */
    static Type of(String pkg, String cl) {
        Type$ type = new Type$(pkg, cl);
        return type.checkNative();
    }

    /**
     * Create a type from a class.
     *
     * @param clazz The class.
     *
     * @return The actual type.
     */
    static Type of(Class<?> clazz) {
        if (clazz.isArray()) {
            Type$ t = (Type$) of(clazz.getComponentType()).asArrayType();
            t.clazz = Optional.of(clazz);
            return t;
        } else {
            Type$ type = new Type$(clazz);
            return type.checkNative();
        }
    }

    /**
     * Create a type from a type.
     *
     * @param type The type.
     *
     * @return The actual type.
     */
    static Type of(java.lang.reflect.Type type) {
        if (type instanceof Type) {
            return (Type) type;
        } else if (type instanceof java.lang.reflect.ParameterizedType) {
            java.lang.reflect.ParameterizedType pType = (java.lang.reflect.ParameterizedType) type;
            Type t = of(pType.getRawType());
            Type[] param = (Type[]) Stream.of(pType.getActualTypeArguments())
                .map(p -> Type.of(p))
                .toArray();
            return t.withTypeParameters(param);
        } else {
            return of(type.getTypeName());
        }
    }

    /**
     * Return the kind of this type.
     *
     * @return The kind of this type.
     */
    Kind getKind();

    /**
     * Return the underlying class if this type corresponds
     * to a class.
     *
     * @return The underlying class if it exists.
     */
    default Optional<Class<?>> forName() {
        try {
            return Optional.of(getDeclaredClass().orElseGet(() -> {
                try {
                    return Class.forName(getQualifiedName());
                } catch (ClassNotFoundException e) {
                    return null; // => NullPointerException
                }
            }));
        } catch (NullPointerException e) {
            return Optional.empty();
        }
    }

    /**
     * Return the wrapped class if this type was created from
     * a class.
     *
     * @return The underlying class.
     */
    Optional<Class<?>> getDeclaredClass();

    /**
     * Return a string representation of this type and its
     * parameterized types if any.
     *
     * @param omitPackage Indicates for each type whether to
     *      return the simple name or the fully qualified name
     *      of the type ; typical use case is to omit the "java.lang"
     *      package or packages that are imported.
     * @return The string representation of this type.
     */
    default String toString(Predicate<Type> omitPackage) {
        return getKind() == Kind.DEFAULT_PACKAGE
            || getKind() == Kind.PRIMITIVE
            || omitPackage.test(this)
        ? getSimpleName() : getQualifiedName();
    }

    /**
     * Return the counterpart boxed type of this type.
     *
     * @return The boxed type if this type is a primitive type,
     *      this otherwise.
     */
    Type box();

    /**
     * Return the counterpart unboxed type of this type.
     *
     * @return The unboxed type if this type is a boxed type,
     *      this otherwise.
     */
    Type unbox();

    /**
     * Get the qualified name of this Java class name.
     *
     * @return The package name (if any) followed by the simple
     *          name of the class.
     */
    default String getQualifiedName() {
        if (getKind() == Kind.DEFAULT_PACKAGE || getKind() == Kind.PRIMITIVE) {
            return getSimpleName();
        } else {
            return getPackageName() + '.' + getSimpleName();
        }
    }

    /**
     * Return the simple name of this Java class name.
     *
     * @return This simple name.
     */
    String getSimpleName();

    /**
     * Return the package name of this Java class name.
     *
     * @return This package name, or <code>null</code>.
     */
    String getPackageName();

    /**
     * Turn this type e.g a "<code>java.util.List</code>"
     * to a parameterized type, e.g
     * "<code>java.util.List&lt;java.lang.Integer&gt;</code>".
     *
     * @param type The type parameters ; primitives are boxed
     *      automatically, e.g an "<code>int</code>" will be boxed to
     *      "<code>java.lang.Integer</code>".
     * @return The new type with its parameters.
     *
     * @see java.lang.reflect.ParameterizedType
     */
    default Type withTypeParameters(Type... type) {
        class ParameterizedType extends Type$ implements java.lang.reflect.ParameterizedType {

            public ParameterizedType() {
                super(Type.this.getPackageName(), Type.this.getSimpleName());
            }

            List<Type> wrapped = Stream.of(type).map(t -> t.box()).collect(Collectors.toList());

            @Override
            public java.lang.reflect.Type[] getActualTypeArguments() {
                return this.wrapped.toArray(new Type[wrapped.size()]);
            }

            @Override
            public java.lang.reflect.Type getRawType() {
                return this;
            }

            @Override
            public java.lang.reflect.Type getOwnerType() {
                return null;
            }

            @Override
            public String toString() {
                return super.toString() + "<" + wrapped.stream()
                    .map(Type::toString)
                    .collect(Collectors.joining(",")) + ">";
            }
            @Override
            public String toString(Predicate<Type> omitPackage) {
                return super.toString(omitPackage) + "<" + wrapped.stream()
                    .map(t -> t.toString(omitPackage))
                    .collect(Collectors.joining(",")) + ">";
            }
        }
        return new ParameterizedType();
    }

    /**
     * Turn this type e.g a "<code>java.time.LocalDate</code>"
     * to an array type, e.g "<code>java.time.LocalDate[]</code>".
     *
     * @return The new type, as an array type.
     *
     * @see java.lang.reflect.GenericArrayType
     */
    default Type asArrayType() {
        class GenericArrayType extends Type$ implements java.lang.reflect.GenericArrayType {

            public GenericArrayType() {
                super(Type.this.getPackageName(), Type.this.getSimpleName());
            }

            @Override
            public java.lang.reflect.Type getGenericComponentType() {
                return Type.this;
            }

            @Override
            public String toString() {
                return super.toString() + "[]";
            }
            @Override
            public String toString(Predicate<Type> omitPackage) {
                return super.toString(omitPackage) + "[]";
            }
        }
        GenericArrayType gat = new GenericArrayType();
        gat.box = () -> this.box().asArrayType();
        gat.unbox = () -> this.unbox().asArrayType();
        return gat;
    }

    /**
     * Parse a value to the underlying type (scalar type, enum value, or array of scalar type or enum).
     *
     * @param value The value to transform. If the result type is an array,
     *      the values are separated by a comma, and whitespaces around are
     *      ignored.
     *
     * @return The value in the expected type.
     *
     * @param <T> The type of the expected type.
     *
     * @throws NumberFormatException For number types, if the string does not contain a parsable byte.
     * @throws IllegalArgumentException For enum types with wrong values.
     * @throws UnsupportedOperationException If the type is neither a scalar type nor an enum type.
     */
    @SuppressWarnings({ "unchecked", "rawtypes", "unlikely-arg-type" })
    default <T> T parse(String value) {
        Optional<Class<?>> c = forName();
        if (c.isPresent()) {
            if (c.get().isEnum()) {
                return (T) Enum.valueOf((Class<Enum>) c.get(), value);
            } else if (this instanceof GenericArrayType) {
                Type t = (Type) ((GenericArrayType) this).getGenericComponentType();
                if (t.equals(char.class)) {
                    // special handling
                    return (T) value.toCharArray();
                } else {
                    return (T) Stream.of(value.split("\\s*,\\s*"))
                        .map(t::parse)
                        .toArray();
                }
            }
        }
        throw new UnsupportedOperationException();
    }

}