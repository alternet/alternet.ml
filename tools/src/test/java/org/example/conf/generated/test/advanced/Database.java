package org.example.conf.generated.test.advanced;

import com.fakesql.jdbc.Driver;
import javax.annotation.Generated;

/**
 * DO NOT EDIT : this class has been generated !
 */
@Generated(value="ml.alternet.properties.Generator", date="2017-12-29")
public class Database {

    public Class<Driver> driver;
    public String url;
    public Account account;

    public static class Account {

        public String login;
        public char[] password;
    }

}

