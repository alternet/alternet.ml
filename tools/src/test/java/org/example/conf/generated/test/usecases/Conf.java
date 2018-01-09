package org.example.conf.generated.test.usecases;

import java.awt.Point;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URI;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import javax.annotation.Generated;
import javax.naming.spi.InitialContextFactory;
import ml.alternet.properties.$;
import ml.alternet.properties.Binder;
import ml.alternet.properties.Binder.Adapter;
import org.example.conf.generated.test.usecases.Conf.SubConf.SubConf_.Status;
import org.example.conf.generated.test.usecases.ExtEnum;
import org.example.conf.generated.test.usecases.status.ExtStatus;
import org.example.conf.handmade.test.Stuff;

/**
 * Configuration structure for "conf.properties"
 *
 * DO NOT EDIT : this class has been generated !
 */
@Generated(value="ml.alternet.properties.Generator", date="2018-01-07")
public class Conf {

    public Ldap ldap;

    public static class Ldap {

        public Class<InitialContextFactory> initialContextFactory;
    }

    public List<ExtEnum> listOfExtEnums;
    public String string;

    public static class String {

        public java.lang.String theString;
        public String_ string;

        public static class String_ {

            public String_0 string;

            public static class String_0 {

                public java.lang.String theString;
            }

        }

    }

    public Status restart;
    public ExtStatus theExtStatus;
    public List<java.lang.String> listOfBooleans;
    public $ map = new $() {};
    public SubConf subConf;

    public static class SubConf {

        public NestedConfMember nestedConfMember;

        public static class NestedConfMember {

            public java.lang.String foo;
            public float bar;
        }

        public java.lang.String string;
        public java.lang.String someOtherString;
        public File tmpFile;
        public File file;
        public Point points;
        public List<java.lang.String> booleans;
        public SubConf_ subConf;

        public static class SubConf_ {

            public java.lang.String $;
            public java.lang.String string;
            public List<Stuff> listOfStuff;
            public Status end;
            public static enum Status {

                Inactive, Active, Pending

            }
            public Other other;
            public static enum Other {

                Full, Empty, Medium

            }
            public ExtStatus another;
            public java.lang.String anotherString;
        }

        public Level level;

        public static class Level {

            public SubLevel subLevel;

            public static class SubLevel {

                public java.lang.String subSubLevel;
            }

        }

        public Ext ext;

        public static class Ext {

            public java.lang.String xml;
            public double bar;
            public List<Point> points;
        }

    }

    public List<java.lang.String> listOfStrings;
    public List<java.lang.String> listOfQuotedStrings;
    public List<java.lang.String> listOfQuotedString;
    public java.lang.String quotedString;
    public boolean b1 = true;
    public boolean b2;
    public byte s1;
    public byte s2;
    public Status start;
    public Point points;
    public URI website;
    public java.lang.String someStuff;
    public short i1;
    public int i2;
    public Instants instants;

    public static class Instants {

        public LocalDate newYear;
        public LocalTime wakeUp;
        public LocalDateTime bug;
    }

    public Map<java.lang.String,Number> associativeArray;
    public List<Map<java.lang.String,Number>> associativeArrays;
    public Function function;

    public static class Function {

        public java.util.function.Function<java.lang.String,Number> $;
        public List<java.util.function.Function<java.lang.String,Number>> list;
    }

    public Foo foo;

    public static class Foo {

        public Foo_ foo;

        public static class Foo_ {

            public Foo_0 foo;

            public static class Foo_0 {

                public Foo_1 foo;

                public static class Foo_1 {

                    public Foo_2 foo;

                    public static class Foo_2 {

                        public Foo_3 foo;

                        public static class Foo_3 {

                            public Foo_4 foo;

                            public static class Foo_4 {

                                public NotFoo notFoo;

                                public static class NotFoo {

                                    public Foo_5 foo;

                                    public static class Foo_5 {

                                        public Foo_6 foo;

                                        public static class Foo_6 {

                                            public NotFoo_ notFoo;

                                            public static class NotFoo_ {

                                                public Foo_7 foo;

                                                public static class Foo_7 {

                                                    public Foo_8 foo;

                                                    public static class Foo_8 {

                                                        public Foo_9 foo;

                                                        public static class Foo_9 {

                                                            public Foo_90 foo;

                                                            public static class Foo_90 {

                                                                public Foo_91 foo;

                                                                public static class Foo_91 {

                                                                    public java.lang.String foo;
                                                                }

                                                            }

                                                        }

                                                    }

                                                }

                                            }

                                        }

                                    }

                                }

                            }

                        }

                    }

                }

            }

        }

    }

    public Integer expectedNull;

    public static Conf unmarshall(InputStream properties) throws IOException {
        return Binder.unmarshall(
            properties,
            Conf.class,
            Adapter.map("subConf.file", File::new),
            Adapter.mapList("subConf.subConf.listOfStuff", Stuff::new)
        );
    }

    public Conf update(InputStream properties) throws IOException {
        return Binder.unmarshall(
            properties,
            this,
            Adapter.map("subConf.file", File::new),
            Adapter.mapList("subConf.subConf.listOfStuff", Stuff::new)
        );
    }

    public static Conf unmarshall(Reader properties) throws IOException {
        return Binder.unmarshall(
            properties,
            Conf.class,
            Adapter.map("subConf.file", File::new),
            Adapter.mapList("subConf.subConf.listOfStuff", Stuff::new)
        );
    }

    public Conf update(Reader properties) throws IOException {
        return Binder.unmarshall(
            properties,
            this,
            Adapter.map("subConf.file", File::new),
            Adapter.mapList("subConf.subConf.listOfStuff", Stuff::new)
        );
    }

    public static Conf unmarshall(Properties properties) throws IOException {
        return Binder.unmarshall(
            properties,
            Conf.class,
            Adapter.map("subConf.file", File::new),
            Adapter.mapList("subConf.subConf.listOfStuff", Stuff::new)
        );
    }

    public Conf update(Properties properties) throws IOException {
        return Binder.unmarshall(
            properties,
            this,
            Adapter.map("subConf.file", File::new),
            Adapter.mapList("subConf.subConf.listOfStuff", Stuff::new)
        );
    }
}

