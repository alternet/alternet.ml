package org.example.conf.generated.test;

import com.fakesql.jdbc.Driver;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URI;
import java.util.List;
import java.util.Properties;
import javax.annotation.Generated;
import ml.alternet.properties.Binder;

/**
 * Configuration structure for "synchro.properties"
 * 
 * DO NOT EDIT : this class has been generated !
 */
@Generated(value="ml.alternet.properties.Generator", date="2017-12-29")
public class Synchro {

    public Plugin plugin;

    public static class Plugin {

        public List list;

        public static class List {

            public Default default_;

            public static class Default {

                public java.lang.String $;
                public Class<?> class_;
            }

        }

        public File file;

        public static class File {

            public java.io.File default_;
        }

        public String string;

        public static class String {

            public java.lang.String default_;
            public java.lang.String value;
        }

    }

    public Db db;

    public static class Db {

        public Class<Driver> driver;
        public URI url;
        public Account account;

        public static class Account {

            public String login;
            public char[] password;
        }

    }

    public Synchro_ synchro;

    public static class Synchro_ {

        public Some some;

        public static class Some {

            public Service service;

            public static class Service {

                public URI url;
                public String login;
                public char[] pwd;
                public Category category;

                public static class Category {

                    public List<Filter> filter;
                    public static enum Filter {

                        CAT1, CATB, OTHER

                    }
                }

            }

        }

        public Ldap ldap;

        public static class Ldap {

            public URI url;
            public Search search;

            public static class Search {

                public String name;
                public FilterExp filterExp;

                public static class FilterExp {

                    public String $;
                    public String uid;
                }

            }

            public Unsafe unsafe;

            public static class Unsafe {

                public PasswordCheck passwordCheck;

                public static class PasswordCheck {

                    public boolean disable = true;
                }

                public UserNotFound userNotFound;

                public static class UserNotFound {

                    public boolean bypass;
                }

            }

            public Attribute attribute;

            public static class Attribute {

                public String uid;
            }

            public Filter filter;

            public static class Filter {

                public String mail;
                public String nom;
                public String prenom;
            }

        }

    }

    public Jndi jndi;

    public static class Jndi {

        public URI ds;
    }

    public Ext ext;

    public static class Ext {

        public URI url;
    }

    
    public static Synchro unmarshall(InputStream properties) throws IOException {
        return Binder.unmarshall(
            properties,
            Synchro.class
        );
    }
    
    public Synchro update(InputStream properties) throws IOException {
        return Binder.unmarshall(
            properties,
            this
        );
    }
    
    public static Synchro unmarshall(Reader properties) throws IOException {
        return Binder.unmarshall(
            properties,
            Synchro.class
        );
    }
    
    public Synchro update(Reader properties) throws IOException {
        return Binder.unmarshall(
            properties,
            this
        );
    }
    
    public static Synchro unmarshall(Properties properties) throws IOException {
        return Binder.unmarshall(
            properties,
            Synchro.class
        );
    }
    
    public Synchro update(Properties properties) throws IOException {
        return Binder.unmarshall(
            properties,
            this
        );
    }
}

