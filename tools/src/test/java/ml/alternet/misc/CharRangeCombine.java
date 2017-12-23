package ml.alternet.misc;

import static ml.alternet.misc.CharRange.*;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.Collectors;

import org.testng.annotations.Test;

import ml.alternet.misc.CharRange;
import ml.alternet.misc.CharRange.BoundRange;
import ml.alternet.misc.CharRange$.Ranges;

@Test
public class CharRangeCombine {

    public void anyCharsExceptAChar_Shoud_haveTheSameIntervalsThanNotAChar() {
        CharRange a = CharRange.is('a');
        CharRange anyExceptA = ANY.except(a);
        CharRange notA = CharRange.isNot('a');
        assertThat(anyExceptA.asIntervals().findFirst().get()).isEqualTo(notA.asIntervals().findFirst().get());
        assertThat(notA).usingComparator(CHAR_RANGE_COMPARATOR).isEqualTo(anyExceptA);
    }

    public void doubleNegateForChar_Shoud_giveTheSame() {
        CharRange a = CharRange.is('a');
        CharRange anyExceptA = ANY.except(a);
        CharRange aAgain = ANY.except(anyExceptA);
        assertThat(a).isEqualTo(aAgain);
    }

    public void doubleNegateForNotChar_Shoud_giveTheSame() {
        CharRange a = CharRange.isNot('a');
        CharRange anyExceptA = ANY.except(a);
        CharRange aAgain = ANY.except(anyExceptA);
        assertThat(CHAR_RANGE_COMPARATOR.compare(a, aAgain)).isEqualTo(0);
        assertThat(a).usingComparator(CHAR_RANGE_COMPARATOR).isEqualTo(aAgain);
    }

    public void rangesWithExclusions_Shoud_beCut() {
        CharRange range = CharRange.range('A', 'Z')
                .union(CharRange.isOneOf("9287315"))
                .union(CharRange.is('a'))
                .union(CharRange.is('h'))
                .union(CharRange.range('m', 'x'))
                .except(CharRange.is('M'))
                .except(CharRange.range('w', 'z'))
                .except(CharRange.range('f', 'p'))
                .except(CharRange.is('0'));

        assertThat(range.getClass()).isAssignableFrom(Ranges.class);

        List<BoundRange> charRanges = range.asIntervals().collect(Collectors.toList());
        assertThat(charRanges).containsExactly(
            CharRange.range('1', '3'),
            CharRange.is('5'),
            CharRange.range('7', '9'),
            CharRange.range('A', 'L'),
            CharRange.range('N', 'Z'),
            CharRange.is('a'),
            CharRange.range('q', 'v')
        );
    }

    public void rangesWithExclusionsAtOnce_Shoud_beCut() {
        CharRange range = CharRange.range('A', 'Z')
                .union(CharRange.isOneOf("9287315"), CharRange.is('a'), CharRange.is('h'), CharRange.range('m', 'x'))
                .except(CharRange.is('M'), CharRange.range('w', 'z'), CharRange.range('f', 'p'), CharRange.is('0'));

        assertThat(range.getClass()).isAssignableFrom(Ranges.class);

        List<BoundRange> charRanges = range.asIntervals().collect(Collectors.toList());
        assertThat(charRanges).containsExactly(
            CharRange.range('1', '3'),
            CharRange.is('5'),
            CharRange.range('7', '9'),
            CharRange.range('A', 'L'),
            CharRange.range('N', 'Z'),
            CharRange.is('a'),
            CharRange.range('q', 'v')
        );
    }

}
