package ml.alternet.scan;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.Collectors;

import org.testng.annotations.Test;

import ml.alternet.scan.CharRange.BoundRange;
import ml.alternet.scan.CharRange.Char;
import ml.alternet.scan.CharRange.Chars;
import ml.alternet.scan.CharRange.Range;

@Test
public class CharRangeIntervalTest {

    public void intervalOfChar_ShouldBe_itself() {
        Char c = new Char(true, 'a');
        List<BoundRange> interval = c.asIntervals().collect(Collectors.toList());
        assertThat(interval).containsExactly(c);
        interval.get(0).getClass().isAssignableFrom(Char.class);
    }

    public void intervalOfRangeOfSingleChar_ShouldBe_aChar() {
        Range r = new Range('a', 'a');
        List<BoundRange> interval = r.asIntervals().collect(Collectors.toList());
        Char c = new Char(true, 'a');
        assertThat(interval).containsExactly(c);
        interval.get(0).getClass().isAssignableFrom(Char.class);
    }

    public void intervalOfRange_ShouldBe_itself() {
        Range r = new Range('a', 'z');
        List<BoundRange> interval = r.asIntervals().collect(Collectors.toList());
        assertThat(interval).containsExactly(r);
        interval.get(0).getClass().isAssignableFrom(Range.class);
    }

    public void intervalOfCharsOfSingleChar_ShouldBe_aChar() {
        Chars c = new Chars(true, "a");
        List<BoundRange> interval = c.asIntervals().collect(Collectors.toList());
        Char expected = new Char(true, 'a');
        assertThat(interval).containsExactly(expected);
        interval.get(0).getClass().isAssignableFrom(Char.class);
    }

    public void intervalOfTwoConsecutiveChars_ShouldBe_aRange() {
        Chars c = new Chars(true, "ab");
        List<BoundRange> interval = c.asIntervals().collect(Collectors.toList());
        Range expected = new Range('a', 'b');
        assertThat(interval).containsExactly(expected);
        interval.get(0).getClass().isAssignableFrom(Range.class);
    }

    public void intervalOfConsecutiveChars_ShouldBe_aRange() {
        Chars c = new Chars(true, "abcde");
        List<BoundRange> interval = c.asIntervals().collect(Collectors.toList());
        Range expected = new Range('a', 'e');
        assertThat(interval).containsExactly(expected);
        interval.get(0).getClass().isAssignableFrom(Range.class);
    }

    public void intervalOfCharsWithConsecutiveChars_Should_containRangesAndChars() {
        Chars c = new Chars(true, "abcz");
        List<BoundRange> intervals = c.asIntervals().collect(Collectors.toList());
        assertThat(intervals).containsExactly(new Range('a', 'c'), new Char(true, 'z'));
    }

    public void intervalOfConsecutiveCharsWithChars_Should_containRangesAndChars() {
        Chars c = new Chars(true, "axyz");
        List<BoundRange> intervals = c.asIntervals().collect(Collectors.toList());
        assertThat(intervals).containsExactly(new Char(true, 'a'), new Range('x', 'z') );
    }

    public void intervalsOfConsecutiveChars_Should_containRanges() {
        Chars c = new Chars(true, "abcxyz");
        List<BoundRange> intervals = c.asIntervals().collect(Collectors.toList());
        assertThat(intervals).containsExactly(new Range('a', 'c'), new Range('x', 'z'));
    }

    public void intervalsOfChars_Should_containChars() {
        Chars c = new Chars(true, "az");
        List<BoundRange> intervals = c.asIntervals().collect(Collectors.toList());
        assertThat(intervals).containsExactly(new Char(true, 'a'), new Char(true, 'z'));
    }

}
