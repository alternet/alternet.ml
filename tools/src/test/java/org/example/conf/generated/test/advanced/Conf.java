package org.example.conf.generated.test.advanced;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URI;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import javax.annotation.Generated;
import ml.alternet.properties.$;
import ml.alternet.properties.Binder;
import ml.alternet.properties.Binder.Adapter;
import org.example.conf.generated.test.advanced.Status;
import org.example.conf.handmade.test.Geo;

/**
 * Configuration structure for "conf.properties"
 *
 * DO NOT EDIT : this class has been generated !
 */
@Generated(value="ml.alternet.properties.Generator", date="2017-12-29")
public class Conf {

    public Service service;

    public static class Service {

        public String $;
        public URI url;
        public UUID uuid;
        public Status status;
    }

    public Gui gui;

    public static class Gui {

        public Window window;

        public Color color;

        public static class Color {

            public java.awt.Color background;
            public java.awt.Color foreground;
            public List<java.awt.Color> pie;
        }

        public Font fonts;

        public static class Font {

            public List<Integer> size;
        }

    }

    public Database db;

    public Files files;

    public static class Files {

        public File help;
    }

    public Plugin plugin;

    public static class Plugin {

        public Status status;
        public List<Status> multipleStatus;
    }

    public $ map = new $() {};
    public User user;

    public static class User {

        public String name;
        public File home;
        public File dir;
    }


    public static Conf unmarshall(InputStream properties) throws IOException {
        return Binder.unmarshall(
            properties,
            Conf.class,
            Adapter.map(Color.class, Color::decode),
            Adapter.map(UUID.class, UUID::fromString),
            Adapter.map("map.*", Geo::parse)
        );
    }

    public Conf update(InputStream properties) throws IOException {
        return Binder.unmarshall(
            properties,
            this,
            Adapter.map(Color.class, Color::decode),
            Adapter.map(UUID.class, UUID::fromString),
            Adapter.map("map.*", Geo::parse)
        );
    }

    public static Conf unmarshall(Reader properties) throws IOException {
        return Binder.unmarshall(
            properties,
            Conf.class,
            Adapter.map(Color.class, Color::decode),
            Adapter.map(UUID.class, UUID::fromString),
            Adapter.map("map.*", Geo::parse)
        );
    }

    public Conf update(Reader properties) throws IOException {
        return Binder.unmarshall(
            properties,
            this,
            Adapter.map(Color.class, Color::decode),
            Adapter.map(UUID.class, UUID::fromString),
            Adapter.map("map.*", Geo::parse)
        );
    }

    public static Conf unmarshall(Properties properties) throws IOException {
        return Binder.unmarshall(
            properties,
            Conf.class,
            Adapter.map(Color.class, Color::decode),
            Adapter.map(UUID.class, UUID::fromString),
            Adapter.map("map.*", Geo::parse)
        );
    }

    public Conf update(Properties properties) throws IOException {
        return Binder.unmarshall(
            properties,
            this,
            Adapter.map(Color.class, Color::decode),
            Adapter.map(UUID.class, UUID::fromString),
            Adapter.map("map.*", Geo::parse)
        );
    }
}

