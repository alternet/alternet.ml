package ml.alternet.scan;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.Collectors;

import org.testng.annotations.Test;

import ml.alternet.scan.CharRange.BoundRange;
import ml.alternet.scan.CharRange.Char;
import ml.alternet.scan.CharRange.Chars;
import ml.alternet.scan.CharRange.Range;
import ml.alternet.scan.CharRange.Ranges;

import static ml.alternet.scan.CharRange.*;

@Test
public class CharRangeCombine {

    public void anyCharsExceptAChar_Shoud_haveTheSameIntervalsThanNotAChar() {
        Char a = CharRange.is('a');
        CharRange anyExceptA = ANY.except(a);
        Char notA = CharRange.isNot('a');
        assertThat(anyExceptA.asIntervals().findFirst().get()).isEqualTo(notA.asIntervals().findFirst().get());
        assertThat(notA).usingComparator(CHAR_RANGE_COMPARATOR).isEqualTo(anyExceptA);
    }

    public void doubleNegateForChar_Shoud_giveTheSame() {
        Char a = CharRange.is('a');
        CharRange anyExceptA = ANY.except(a);
        CharRange aAgain = ANY.except(anyExceptA);
        assertThat(a).isEqualTo(aAgain);
    }

    public void doubleNegateForNotChar_Shoud_giveTheSame() {
        Char a = CharRange.isNot('a');
        CharRange anyExceptA = ANY.except(a);
        CharRange aAgain = ANY.except(anyExceptA);
        assertThat(CHAR_RANGE_COMPARATOR.compare(a, aAgain)).isEqualTo(0);
        assertThat(a).usingComparator(CHAR_RANGE_COMPARATOR).isEqualTo(aAgain);
    }

    public void rangesWithExclusions_Shoud_beCut() {
        CharRange range = new Range('A', 'Z')
                .union(new Chars(true, "9287315"))
                .union(new Char(true, 'a'))
                .union(new Char(true, 'h'))
                .union(new Range('m', 'x'))
                .except(new Char(true, 'M'))
                .except(new Range('w', 'z'))
                .except(new Range('f', 'p'))
                .except(new Char(true, '0'));

        assertThat(range.getClass()).isAssignableFrom(Ranges.class);

        List<BoundRange> charRanges = range.asIntervals().collect(Collectors.toList());
        assertThat(charRanges).containsExactly(
            new Range('1', '3'),
            new Char(true, '5'),
            new Range('7', '9'),
            new Range('A', 'L'),
            new Range('N', 'Z'),
            new Char(true, 'a'),
            new Range('q', 'v')
        );
    }

    public void rangesWithExclusionsAtOnce_Shoud_beCut() {
        CharRange range = new Range('A', 'Z')
                .union(new Chars(true, "9287315"), new Char(true, 'a'), new Char(true, 'h'), new Range('m', 'x'))
                .except(new Char(true, 'M'), new Range('w', 'z'), new Range('f', 'p'), new Char(true, '0'));

        assertThat(range.getClass()).isAssignableFrom(Ranges.class);

        List<BoundRange> charRanges = range.asIntervals().collect(Collectors.toList());
        assertThat(charRanges).containsExactly(
            new Range('1', '3'),
            new Char(true, '5'),
            new Range('7', '9'),
            new Range('A', 'L'),
            new Range('N', 'Z'),
            new Char(true, 'a'),
            new Range('q', 'v')
        );
    }

}
