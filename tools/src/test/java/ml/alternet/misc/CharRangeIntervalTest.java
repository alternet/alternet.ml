package ml.alternet.misc;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.Collectors;

import org.testng.annotations.Test;

import ml.alternet.misc.CharRange;

@Test
public class CharRangeIntervalTest {

    public void intervalOfChar_ShouldBe_itself() {
        CharRange.BoundRange c = CharRange.is('a');
        List<CharRange.BoundRange> interval = c.asIntervals().collect(Collectors.toList());
        assertThat(interval).containsExactly(c);
        interval.get(0).getClass().isAssignableFrom(ml.alternet.misc.CharRange$.Char.class);
    }

    public void intervalOfRangeOfSingleChar_ShouldBe_aChar() {
        CharRange r = CharRange.range('a', 'a');
        List<CharRange.BoundRange> interval = r.asIntervals().collect(Collectors.toList());
        CharRange.BoundRange c = CharRange.is('a');
        assertThat(interval).containsExactly(c);
        interval.get(0).getClass().isAssignableFrom(ml.alternet.misc.CharRange$.Char.class);
    }

    public void intervalOfRange_ShouldBe_itself() {
        CharRange.BoundRange r = CharRange.range('a', 'z');
        List<CharRange.BoundRange> interval = r.asIntervals().collect(Collectors.toList());
        assertThat(interval).containsExactly(r);
        interval.get(0).getClass().isAssignableFrom(ml.alternet.misc.CharRange$.Range.class);
    }

    public void intervalOfCharsOfSingleChar_ShouldBe_aChar() {
        CharRange c = CharRange.isOneOf("a");
        List<CharRange.BoundRange> interval = c.asIntervals().collect(Collectors.toList());
        CharRange.BoundRange expected = CharRange.is('a');
        assertThat(interval).containsExactly(expected);
        interval.get(0).getClass().isAssignableFrom(ml.alternet.misc.CharRange$.Char.class);
    }

    public void intervalOfTwoConsecutiveChars_ShouldBe_aRange() {
        CharRange c = CharRange.isOneOf("ab");
        List<CharRange.BoundRange> interval = c.asIntervals().collect(Collectors.toList());
        CharRange.BoundRange expected = CharRange.range('a', 'b');
        assertThat(interval).containsExactly(expected);
        interval.get(0).getClass().isAssignableFrom(ml.alternet.misc.CharRange$.Range.class);
    }

    public void intervalOfConsecutiveChars_ShouldBe_aRange() {
        CharRange c = CharRange.isOneOf("abcde");
        List<CharRange.BoundRange> interval = c.asIntervals().collect(Collectors.toList());
        CharRange.BoundRange expected = CharRange.range('a', 'e');
        assertThat(interval).containsExactly(expected);
        interval.get(0).getClass().isAssignableFrom(ml.alternet.misc.CharRange$.Range.class);
    }

    public void intervalOfCharsWithConsecutiveChars_Should_containRangesAndChars() {
        CharRange c = CharRange.isOneOf("abcz");
        List<CharRange.BoundRange> intervals = c.asIntervals().collect(Collectors.toList());
        assertThat(intervals).containsExactly(CharRange.range('a', 'c'), CharRange.is('z'));
    }

    public void intervalOfConsecutiveCharsWithChars_Should_containRangesAndChars() {
        CharRange c = CharRange.isOneOf("axyz");
        List<CharRange.BoundRange> intervals = c.asIntervals().collect(Collectors.toList());
        assertThat(intervals).containsExactly(CharRange.is('a'), CharRange.range('x', 'z') );
    }

    public void intervalsOfConsecutiveChars_Should_containRanges() {
        CharRange c = CharRange.isOneOf("abcxyz");
        List<CharRange.BoundRange> intervals = c.asIntervals().collect(Collectors.toList());
        assertThat(intervals).containsExactly(CharRange.range('a', 'c'), CharRange.range('x', 'z'));
    }

    public void intervalsOfChars_Should_containChars() {
        CharRange c = CharRange.isOneOf("az");
        List<CharRange.BoundRange> intervals = c.asIntervals().collect(Collectors.toList());
        assertThat(intervals).containsExactly(CharRange.is('a'), CharRange.is('z'));
    }

}
