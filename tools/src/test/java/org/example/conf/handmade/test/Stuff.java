package org.example.conf.handmade.test;

public class Stuff {

    String s;
    int l;

    public Stuff(String s) {
        this.s = s;
        this.l = s.length() * 2;
    }

    @Override
    public String toString() {
        return "[" + l + "]" + s;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Stuff) {
            Stuff s = (Stuff) obj;
            return this.s.equals(s.s) && this.l == s.l;
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return this.l * 31 + s.hashCode();
    }

}
