package ml.alternet.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.IntStream;

/**
 * Utilities related to the Java language.
 *
 * @author Philippe Poulard
 */
@Util
public final class JavaUtil {

    private JavaUtil() { }

    private static Set<String> KW = new HashSet<>(Arrays.asList(
            "abstract", "continue", "for",        "new",       "switch",
            "assert",   "default",  "goto",       "package",   "synchronized",
            "boolean",  "do",       "if",         "private",   "this",
            "break",    "double",   "implements", "protected", "throw",
            "byte",     "else",     "import",     "public",    "throws",
            "case",     "enum",     "instanceof", "return",    "transient",
            "catch",    "extends",  "int",        "short",     "try",
            "char",     "final",    "interface",  "static",    "void",
            "class",    "finally",  "long",       "strictfp",  "volatile",
            "const",    "float",    "native",     "super",     "while"));

    /**
     * Return the set of Java keywords.
     *
     * @return The set of Java keywords.
     */
    public static Set<String> keywords() {
        return Collections.unmodifiableSet(KW);
    }

    /**
     * Indicates whether a word is a Java keyword or not.
     *
     * @param s The word to check.
     *
     * @return <code>true</code> if the word is a Java keyword,
     *      <code>false</code> otherwise.
     */
    public static boolean isKeyword(String s) {
        return KW.contains(s);
    }

    /**
     * Remove the characters from a string that are
     * not legal for a Java name.
     * NOTE : this method doesn't check whether the
     * result is a Java keyword or not.
     *
     * @param javaName The candidate Java name.
     * @return The cleaned Java name
     *
     * @throws IllegalArgumentException If the candidate Java name
     *      contains only illegal characters.
     *
     * @see Character#isJavaIdentifierStart(int)
     * @see Character#isJavaIdentifierPart(int)
     */
    public static String asJavaName(String javaName) {
        int[] n = { 0 }; // expect to be one but increase
                         // at each wrong first character found
        int first = javaName.codePoints().filter(cp -> {
            n[0]++;
            return Character.isJavaIdentifierStart(cp);
        }).findFirst().orElseThrow(() ->
            new IllegalArgumentException(javaName + " is not a Java name."));
        IntStream parts = javaName.codePoints().skip(n[0]).filter(
            c -> Character.isJavaIdentifierPart(c));
        int[] codepoints = IntStream.concat(
            IntStream.of(first),
            parts
        ).toArray();
        return new String(codepoints, 0, codepoints.length);
    }

}
