package ml.alternet.misc;

import java.util.Optional;
import java.util.function.Supplier;

import ml.alternet.util.EnumUtil;

/**
 * Internal implementation of a type.
 *
 * @author Philippe Poulard
 */
class Type$ implements Type {

    enum Native {

        _int(int.class) {
            @SuppressWarnings("unchecked")
            @Override
            Integer parse(String value) {
                return java.lang.Integer.parseInt(value);
            }
        },
        _double(double.class) {
            @SuppressWarnings("unchecked")
            @Override
            Double parse(String value) {
                return java.lang.Double.parseDouble(value);
            }
        },
        _byte(byte.class) {
            @SuppressWarnings("unchecked")
            @Override
            Byte parse(String value) {
                return java.lang.Byte.parseByte(value);
            }
        },
        _boolean(boolean.class) {
            @SuppressWarnings("unchecked")
            @Override
            Boolean parse(String value) {
                return java.lang.Boolean.parseBoolean(value);
            }
        },
        _char(char.class) {
            @SuppressWarnings("unchecked")
            @Override
            Character parse(String value) {
                return (value == null || value.length() == 0) ? null : value.charAt(0);
            }
        },
        _void(void.class) {
            @SuppressWarnings("unchecked")
            @Override
            Void parse(String value) {
                throw new UnsupportedOperationException();
            }
        },
        _short(short.class) {
            @SuppressWarnings("unchecked")
            @Override
            Short parse(String value) {
                return java.lang.Short.parseShort(value);
            }
        },
        _float(float.class) {
            @SuppressWarnings("unchecked")
            @Override
            Float parse(String value) {
                return java.lang.Float.parseFloat(value);
            }
        },
        _long(long.class) {
            @SuppressWarnings("unchecked")
            @Override
            Long parse(String value) {
                return java.lang.Long.parseLong(value);
            }
        },
        // above : primitive types

        // we have String since it is a native type
        String(java.lang.String.class) {
            @SuppressWarnings("unchecked")
            @Override
            String parse(String value) {
                return value;
            }
        },

        // below : counterpart types of primitive types
        Integer(java.lang.Integer.class) {
            @SuppressWarnings("unchecked")
            @Override
            Integer parse(String value) {
                return java.lang.Integer.parseInt(value);
            }
        },
        Double(java.lang.Double.class) {
            @SuppressWarnings("unchecked")
            @Override
            Double parse(String value) {
                return java.lang.Double.parseDouble(value);
            }
        },
        Byte(java.lang.Byte.class) {
            @SuppressWarnings("unchecked")
            @Override
            Byte parse(String value) {
                return java.lang.Byte.parseByte(value);
            }
        },
        Boolean(java.lang.Boolean.class) {
            @SuppressWarnings("unchecked")
            @Override
            Boolean parse(String value) {
                return java.lang.Boolean.parseBoolean(value);
            }
        },
        Character(java.lang.Character.class) {
            @SuppressWarnings("unchecked")
            @Override
            Character parse(String value) {
                return (value == null || value.length() == 0) ? null : value.charAt(0);
            }
        },
        Void(java.lang.Void.class) {
            @SuppressWarnings("unchecked")
            @Override
            Void parse(String value) {
                throw new UnsupportedOperationException();
            }
        },
        Short(java.lang.Short.class) {
            @SuppressWarnings("unchecked")
            @Override
            Short parse(String value) {
                return java.lang.Short.parseShort(value);
            }
        },
        Float(java.lang.Float.class) {
            @SuppressWarnings("unchecked")
            @Override
            Float parse(String value) {
                return java.lang.Float.parseFloat(value);
            }
        },
        Long(java.lang.Long.class) {
            @SuppressWarnings("unchecked")
            @Override
            Long parse(String value) {
                return java.lang.Long.parseLong(value);
            }
        };

        Native(Class<?> clazz) {
            // remove the first _   e.g. change  _int  to  int
            EnumUtil.replace(Type$.Native.class, this, s -> s.charAt(0) == '_' ? s.substring(1) : s );
            this.type = new Type$(clazz) {
                @Override
                public <T> T parse(String value) {
                    return Native.this.parse(value);
                }
            };
            if (this.type.kind == Kind.DEFAULT_PACKAGE) {
                this.type.kind = Kind.PRIMITIVE;
            }
            // [0-8]   => primitives that can be boxed
            // 9       => String box/unboxed to itself
            // [10-18] => Primitives that can be unboxed (look, there is an uppercase)
            this.type.box = () -> { // deferred setting
                Type$ t = ordinal() < 9 ? Native.values()[ordinal() + 10].type : this.type;
                this.type.box = () -> t;
                return t;
            };
            this.type.unbox = () -> { // deferred setting
                Type$ t = ordinal() < 10 ? this.type : Native.values()[ordinal() - 10].type;
                this.type.unbox = () -> t;
                return t;
            };
        }

        Type$ type;

        abstract <T> T parse(String value);

    }

    String cl;  // simple name
    String pkg; // package
    Kind kind = Kind.OTHER;
    Optional<Class<?>> clazz = Optional.empty(); // when this type was built from a class

    Supplier<Type> box = () -> this;   // by default, box to itself
    Supplier<Type> unbox = () -> this; // by default, unbox to itself

    Type$(Class<?> clazz) {
        this(clazz.getCanonicalName());
        this.clazz = Optional.of(clazz);
    }

    Type$(String qualifiedName) {
        // e.g. "org.acme.Foo"
        int dot = qualifiedName.lastIndexOf('.');
        if (dot == -1) {
            // just "Foo"
            this.cl = qualifiedName;
            this.kind = Kind.DEFAULT_PACKAGE;
        } else {
            this.cl = qualifiedName.substring(dot + 1);
            this.pkg = qualifiedName.substring(0,  dot);
            if ("java.lang".equals(this.pkg)) {
                this.kind = Kind.JAVA_LANG_PACKAGE;
            };
        }
    }

    Type$(String pkg, String cl) {
        this.pkg = pkg;
        this.cl = cl;
        if (this.pkg == null || this.pkg.length() == 0) {
            this.pkg = null;
            this.kind = Kind.DEFAULT_PACKAGE;
        } else if ("java.lang".equals(this.pkg)) {
            this.kind = Kind.JAVA_LANG_PACKAGE;
        };
    }

    // called after each creation to return primitive or Primitive, or String
    Type$ checkNative() {
        if (getKind() == Kind.DEFAULT_PACKAGE) {
            try {
                Type$ primitive = Type$.Native.valueOf(getSimpleName()).type;
                if (primitive.getKind() == Kind.PRIMITIVE) {
                    return primitive;
                }
            } catch (IllegalArgumentException e) { }
        } else if (getKind() == Kind.JAVA_LANG_PACKAGE) {
            try {
                Type$ primitive = Type$.Native.valueOf(getSimpleName()).type;
                if (primitive.getKind() == Kind.JAVA_LANG_PACKAGE) {
                    return primitive;
                }
            } catch (IllegalArgumentException e) { }
        }
        return this;
    }

    @Override
    public String getSimpleName() {
        return this.cl;
    }

    @Override
    public String getPackageName() {
        return this.pkg;
    }

    @Override
    public Kind getKind() {
        return this.kind;
    }

    @Override
    public Type box() {
        return this.box.get();
    }

    @Override
    public Type unbox() {
        return this.unbox.get();
    }

    @Override
    public Optional<Class<?>> getDeclaredClass() {
        return this.clazz;
    }

    @Override
    public String toString() {
        return getQualifiedName();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Type) {
            return getQualifiedName().equals(((Type) obj).getQualifiedName());
        } else if (obj instanceof java.lang.reflect.Type) {
            return getQualifiedName().equals(obj.toString());
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return getQualifiedName().hashCode();
    }

}
