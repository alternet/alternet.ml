package ml.alternet.security.web;

import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;

import ml.alternet.facet.Unwrappable;
import ml.alternet.security.EmptyPassword;
import ml.alternet.security.Password;
import ml.alternet.security.PasswordState;

/**
 * Represent a non empty sequence of passwords.
 *
 * <p>If no passwords are in this sequence, the current
 * item is the empty password.</p>
 *
 * <p>NOTE : All the values are available at once in a
 * <tt>PasswordParam</tt>. Actually, <tt>PasswordParam</tt>
 * is iterable on all the passwords bound to the same name
 * (if any) and already refer the first one in the sequence
 * (if any, otherwise it refers to the empty password) ;
 * therefore, it is irrelevant to specifically ask for a
 * <tt>List&lt;PasswordParam&gt;</tt>, a <tt>Set&lt;PasswordParam&gt;</tt>
 * or a <tt>SortedSet&lt;PasswordParam&gt;</tt>.</p>
 *
 * @author Philippe Poulard
 */
public class PasswordParam implements Password, Iterator<PasswordParam>, Unwrappable<Password> {

    private Iterator<Password> seq; // the sequence
    private Password that; // the current pwd

    /**
     * Create a sequence of passwords.
     *
     * @param sequence An iterator on the sequence.
     */
    public PasswordParam(Iterator<Password> sequence) {
        this.seq = sequence;
        if (sequence.hasNext()) {
            this.that = sequence.next();
        } else {
            this.that = EmptyPassword.SINGLETON;
        }
    }

    /**
     * Convenient constructor for a sequence of a single password.
     *
     * @param password The single password of the sequence.
     */
    public PasswordParam(Password password) {
        that = password;
        seq = Collections.emptyIterator();
    }

    /**
     * Convenient constructor for a sequence representing the
     * empty password.
     */
    public PasswordParam() {
        this(EmptyPassword.SINGLETON);
    }

    @Override
    public void destroy() {
        that.destroy();
    }

    @Override
    public PasswordState state() {
        return that.state();
    }

    @Override
    public Clear getClearCopy() throws IllegalStateException {
        return that.getClearCopy();
    }

    @Override
    public boolean hasNext() {
        return seq.hasNext();
    }

    @Override
    public PasswordParam next() {
        if (this.seq.hasNext()) {
            this.that = this.seq.next();
        } else if (this.that == EmptyPassword.SINGLETON) {
            throw new NoSuchElementException();
        } else {
            this.that = EmptyPassword.SINGLETON;
        }
        return this;
    }

    @Override
    public String toString() {
        return that.toString();
    }

    @Override
    public Password unwrap() {
        return that;
    }

}
