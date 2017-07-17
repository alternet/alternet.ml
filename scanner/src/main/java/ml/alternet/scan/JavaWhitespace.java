package ml.alternet.scan;

import java.io.IOException;
import java.util.function.Predicate;

/**
 * Read only Java whitespaces.
 *
 * @see Character#isWhitespace(char)
 *
 * @see Character#isWhitespace(char)
 *
 * @author Philippe Poulard
 */
public class JavaWhitespace implements StringConstraint, Predicate<Integer> {

    @Override
    public boolean test(Integer cp) {
        return Character.isWhitespace(cp);
    }

    @Override
    public int append(int sourceIndex, int targetLength, Scanner scanner,
            StringBuilder buf) throws IOException
    {
        int c = scanner.lookAhead();
        buf.append( c );
        return 1;
    }

    @Override
    public boolean stopCondition(int sourceIndex, int targetLength,
            Scanner scanner) throws IOException
    {
        // stop as soon as the next char is not a whitespace
        return ! test( scanner.lookAhead() );
    }

}
