package ml.alternet.test.security.web.server;

import javax.xml.bind.annotation.XmlRootElement;

import ml.alternet.security.Password;

// the data sent back in the response to the client
@XmlRootElement
public class Data {

    public Data() {}

    public Data(String user, String pwd) {
        this.user = user;
        this.pwd = pwd;
    }

    public Data(Password pwd, String rawValue) {
        this.pwd = rawValue;
        this.clearValue = pwd.getClearCopy().get();
    }

    public String user;
    public String pwd;
    public char[] clearValue;

}