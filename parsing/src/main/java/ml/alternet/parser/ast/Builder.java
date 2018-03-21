package ml.alternet.parser.ast;

import java.io.IOException;
import java.io.Reader;
import java.util.Optional;

import ml.alternet.parser.handlers.DataHandler;
import ml.alternet.scan.Scanner;

/**
 * Build a value from a grammar.
 *
 * @author Philippe Poulard
 *
 * @param <Node> The type of the result value.
 */
public interface Builder<T> extends DataHandler<T> {

    /**
     * Entry point for building an expression.
     *
     * @param input An expression that follows the grammar, e.g. "sin(x)*(1+var_12)"
     * @param matchAll <code>true</code> to indicates that if
     *      the input contains more characters at the end of
     *      the parsing, an error will be reported, <code>false</code>
     *      otherwise.
     *
     * @return A value of the type expected.
     *
     * @throws IOException When an I/O error occurs.
     */
    default Optional<T> build(String input, boolean matchAll) throws IOException {
        return build(Scanner.of(input), matchAll);
    }

    /**
     * Entry point for building an expression.
     *
     * @param input An expression that follows the grammar, e.g. "sin(x)*(1+var_12)"
     * @param matchAll <code>true</code> to indicates that if
     *      the input contains more characters at the end of
     *      the parsing, an error will be reported, <code>false</code>
     *      otherwise.
     *
     * @return A value of the type expected.
     *
     * @throws IOException When an I/O error occurs.
     */
    default Optional<T> build(Reader input, boolean matchAll) throws IOException {
        return build(Scanner.of(input), matchAll);
    }

    /**
     * Entry point for building an expression.
     *
     * @param input An expression that follows the grammar, e.g. "sin(x)*(1+var_12)"
     * @param matchAll <code>true</code> to indicates that if
     *      the input contains more characters at the end of
     *      the parsing, an error will be reported, <code>false</code>
     *      otherwise.
     *
     * @return A value of the type expected.
     *
     * @throws IOException When an I/O error occurs.
     */
    Optional<T> build(Scanner input, boolean matchAll) throws IOException;

}
