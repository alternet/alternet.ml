package org.example.conf.handmade.test;

import java.awt.Point;
import java.io.File;
import java.net.URL;
import java.util.List;

import org.w3c.dom.Document;

import ml.alternet.properties.$;

public class MyConf {

    public boolean b1;
    public Boolean b2;

    public short s1;
    public Short s2;

    public int i1;
    public Integer i2;

    public MyNestedConf subConf;

    public Status start;
    public Status unknown;

    public static class MyNestedConf implements $ {

        public String string;
        public String someOtherString;
        public NestedConfWithGlobal subConf;
        public File tmpFile;
        public Document unsetValue;

        public static class NestedConfWithGlobal { // no field => FreeKeys

            public String $;
            public String string;
            public String anotherString;
            public List<Stuff> listOfStuff;
            public Status end;

        }

        public class NestedConfMember {
            public Stuff foo;
            public double bar;
        }

        public class Ext extends NestedConfMember {
            public Document xml; // use an adapter for this type
            public List<Point> points;
        }

        public static class NestedConfWithFreeKeys implements $ {

            public boolean knowKey;
            public String anotherKnownKey;
            // use unkownKey is possible
        }

    }

    public URL website;

    public List<String> listOfStrings;

    public Stuff someStuff;

    static class Ext extends MyNestedConf.NestedConfWithFreeKeys {

    }

    public $ map = new $() {};

}
