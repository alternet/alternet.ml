package org.example.conf.generated.test;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.Properties;
import javax.annotation.Generated;
import ml.alternet.properties.Binder;

/**
 * Configuration structure for "mail.properties"
 * 
 * DO NOT EDIT : this class has been generated !
 */
@Generated(value="ml.alternet.properties.Generator", date="2017-12-29")
public class Mail {

    public Mail_ mail;

    public static class Mail_ {

        public Smtp smtp;

        public static class Smtp {

            public Starttls starttls;

            public static class Starttls {

                public boolean enable;
            }

            public boolean auth;
            public String host;
            public int port;
            public Ssl ssl;

            public static class Ssl {

                public String trust;
            }

        }

    }

    public String username;
    public char[] password;
    public Name name;

    public static class Name {

        public String expediteur;
    }

    
    public static Mail unmarshall(InputStream properties) throws IOException {
        return Binder.unmarshall(
            properties,
            Mail.class
        );
    }
    
    public Mail update(InputStream properties) throws IOException {
        return Binder.unmarshall(
            properties,
            this
        );
    }
    
    public static Mail unmarshall(Reader properties) throws IOException {
        return Binder.unmarshall(
            properties,
            Mail.class
        );
    }
    
    public Mail update(Reader properties) throws IOException {
        return Binder.unmarshall(
            properties,
            this
        );
    }
    
    public static Mail unmarshall(Properties properties) throws IOException {
        return Binder.unmarshall(
            properties,
            Mail.class
        );
    }
    
    public Mail update(Properties properties) throws IOException {
        return Binder.unmarshall(
            properties,
            this
        );
    }
}

