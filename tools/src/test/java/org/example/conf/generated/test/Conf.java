package org.example.conf.generated.test;

import java.awt.Color;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URI;
import java.util.Properties;
import java.util.UUID;
import javax.annotation.Generated;
import ml.alternet.properties.Binder;
import ml.alternet.properties.Binder.Adapter;

/**
 * Configuration structure for "conf.properties"
 * 
 * DO NOT EDIT : this class has been generated !
 */
@Generated(value="ml.alternet.properties.Generator", date="2017-12-29")
public class Conf {

    public Gui gui;

    public static class Gui {

        public Window window;

        public static class Window {

            public int width;
            public short height;
        }

        public Colors colors;

        public static class Colors {

            public Color background;
            public Color foreground;
        }

    }

    public Service service;

    public static class Service {

        public URI url;
        public UUID uuid;
    }

    public Db db;

    public static class Db {

        public Class<?> driver;
        public String url;
        public Account account;

        public static class Account {

            public String login;
            public char[] password;
        }

    }

    
    public static Conf unmarshall(InputStream properties) throws IOException {
        return Binder.unmarshall(
            properties,
            Conf.class,
            Adapter.map(Color.class, Color::decode),
            Adapter.map(UUID.class, UUID::fromString)
        );
    }
    
    public Conf update(InputStream properties) throws IOException {
        return Binder.unmarshall(
            properties,
            this,
            Adapter.map(Color.class, Color::decode),
            Adapter.map(UUID.class, UUID::fromString)
        );
    }
    
    public static Conf unmarshall(Reader properties) throws IOException {
        return Binder.unmarshall(
            properties,
            Conf.class,
            Adapter.map(Color.class, Color::decode),
            Adapter.map(UUID.class, UUID::fromString)
        );
    }
    
    public Conf update(Reader properties) throws IOException {
        return Binder.unmarshall(
            properties,
            this,
            Adapter.map(Color.class, Color::decode),
            Adapter.map(UUID.class, UUID::fromString)
        );
    }
    
    public static Conf unmarshall(Properties properties) throws IOException {
        return Binder.unmarshall(
            properties,
            Conf.class,
            Adapter.map(Color.class, Color::decode),
            Adapter.map(UUID.class, UUID::fromString)
        );
    }
    
    public Conf update(Properties properties) throws IOException {
        return Binder.unmarshall(
            properties,
            this,
            Adapter.map(Color.class, Color::decode),
            Adapter.map(UUID.class, UUID::fromString)
        );
    }
}

