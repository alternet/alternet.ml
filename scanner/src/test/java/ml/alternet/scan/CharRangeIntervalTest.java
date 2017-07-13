package ml.alternet.scan;

import static org.assertj.core.api.Assertions.*;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.testng.annotations.Test;

import ml.alternet.scan.CharRange.BoundCharRange;
import ml.alternet.scan.CharRange.Char;
import ml.alternet.scan.CharRange.Chars;
import ml.alternet.scan.CharRange.Range;

@Test
public class CharRangeIntervalTest {

    // interval TESTS

    public void intervalOfChar_ShouldBe_itself() {
        Char c = new Char(true, 'a');
        List<BoundCharRange> interval = c.asIntervals().collect(Collectors.toList());
        assertThat(interval).containsExactly(c);
        interval.get(0).getClass().isAssignableFrom(Char.class);
    }

    public void intervalOfRangeOfSingleChar_ShouldBe_aChar() {
        Range r = new Range('a', 'a');
        List<BoundCharRange> interval = r.asIntervals().collect(Collectors.toList());
        Char c = new Char(true, 'a');
        assertThat(interval).containsExactly(c);
        interval.get(0).getClass().isAssignableFrom(Char.class);
    }

    public void intervalOfRange_ShouldBe_itself() {
        Range r = new Range('a', 'z');
        List<BoundCharRange> interval = r.asIntervals().collect(Collectors.toList());
        assertThat(interval).containsExactly(r);
        interval.get(0).getClass().isAssignableFrom(Range.class);
    }

    public void intervalOfCharsOfSingleChar_ShouldBe_aChar() {
        Chars c = new Chars(true, "a");
        List<BoundCharRange> interval = c.asIntervals().collect(Collectors.toList());
        Char expected = new Char(true, 'a');
        assertThat(interval).containsExactly(expected);
        interval.get(0).getClass().isAssignableFrom(Char.class);
    }

    public void intervalOfTwoConsecutiveChars_ShouldBe_aRange() {
        Chars c = new Chars(true, "ab");
        List<BoundCharRange> interval = c.asIntervals().collect(Collectors.toList());
        Range expected = new Range('a', 'b');
        assertThat(interval).containsExactly(expected);
        interval.get(0).getClass().isAssignableFrom(Range.class);
    }

    public void intervalOfConsecutiveChars_ShouldBe_aRange() {
        Chars c = new Chars(true, "abcde");
        List<BoundCharRange> interval = c.asIntervals().collect(Collectors.toList());
        Range expected = new Range('a', 'e');
        assertThat(interval).containsExactly(expected);
        interval.get(0).getClass().isAssignableFrom(Range.class);
    }

    public void intervalOfCharsWithConsecutiveChars_Should_containRangesAndChars() {
        Chars c = new Chars(true, "abcz");
        List<BoundCharRange> intervals = c.asIntervals().collect(Collectors.toList());
        assertThat(intervals).containsExactly(new Range('a', 'c'), new Char(true, 'z'));
    }

    public void intervalOfConsecutiveCharsWithChars_Should_containRangesAndChars() {
        Chars c = new Chars(true, "axyz");
        List<BoundCharRange> intervals = c.asIntervals().collect(Collectors.toList());
        assertThat(intervals).containsExactly(new Char(true, 'a'), new Range('x', 'z') );
    }

    public void intervalsOfConsecutiveChars_Should_containRanges() {
        Chars c = new Chars(true, "abcxyz");
        List<BoundCharRange> intervals = c.asIntervals().collect(Collectors.toList());
        assertThat(intervals).containsExactly(new Range('a', 'c'), new Range('x', 'z'));
    }

    public void intervalsOfChars_Should_containChars() {
        Chars c = new Chars(true, "az");
        List<BoundCharRange> intervals = c.asIntervals().collect(Collectors.toList());
        assertThat(intervals).containsExactly(new Char(true, 'a'), new Char(true, 'z'));
    }





    // reverse TESTS

    public void notChar_ShouldBe_reversedTo2Ranges() {
        Char c = new Char(false, 'a');

        List<BoundCharRange> charRanges = c.asIntervals().collect(Collectors.toList());
        assertThat(charRanges).containsExactly(
            new Range(Character.MIN_CODE_POINT, 'a' - 1),
            new Range('a' + 1, Character.MAX_CODE_POINT));
    }

    public void singleChar_ShouldBe_theSameAsARangeThatStartAndEndWithTheSameChar() {
        Char c = new Char(true, 'a');
        Range r = new Range('a', 'a');
        List<BoundCharRange> charRanges = c.asIntervals().collect(Collectors.toList());
        assertThat(charRanges).isEqualTo(
                r.asIntervals().collect(Collectors.toList()));
        assertThat(c).isEqualTo(r);
        assertThat(c.toString()).isEqualTo(r.toString());
    }

    public void rangeWhereEndIsBeforeStart_ShouldBe_empty() {
        Range r = new Range('b', 'a');
        assertThat(r.isEmpty()).isTrue();
        assertThat(r.toString()).isEqualTo("''");
    }

    public void charInterval_ShouldBe_Itself() {
        Char c = new Char(true, 'a');
        assertThat(c.contains('a')).isTrue();
        List<BoundCharRange> charRanges = c.asIntervals().collect(Collectors.toList());
        assertThat(charRanges).containsExactly(
            new Char(true, 'a'));
    }

    public void singleCharStream_ShouldBe_reversedTo2Ranges() {
        IntStream lowercases = IntStream.of( 'a' );
        List<BoundCharRange> charRanges = CharRange.reverse(lowercases).collect(Collectors.toList());
        assertThat(charRanges).containsExactly(
            new Range(Character.MIN_CODE_POINT, 'a' - 1),
            new Range('a' + 1, Character.MAX_CODE_POINT));
    }

    public void twoChars_ShouldBe_reversedTo3Ranges() {
        IntStream lowercases = IntStream.of( 'a', 'z' );
        List<BoundCharRange> charRanges = CharRange.reverse(lowercases).collect(Collectors.toList());
        assertThat(charRanges).containsExactly(
            new Range(Character.MIN_CODE_POINT, 'a' - 1),
            new Range('a' + 1, 'z' -1),
            new Range('z' + 1, Character.MAX_CODE_POINT));
    }

    public void twoConsecutiveChars_ShouldBe_reversedTo2Ranges() {
        IntStream lowercases = IntStream.of( 'a', 'b' );
        List<BoundCharRange> charRanges = CharRange.reverse(lowercases).collect(Collectors.toList());
        assertThat(charRanges).containsExactly(
            new Range(Character.MIN_CODE_POINT, 'a' - 1),
            new Range('b' + 1, Character.MAX_CODE_POINT));
    }

    public void severalConsecutiveChars_ShouldBe_reversedTo2Ranges() {
        IntStream lowercases = IntStream.of( 'a', 'b', 'c', 'd', 'e' );
        List<BoundCharRange> charRanges = CharRange.reverse(lowercases).collect(Collectors.toList());
        assertThat(charRanges).containsExactly(
            new Range(Character.MIN_CODE_POINT, 'a' - 1),
            new Range('e' + 1, Character.MAX_CODE_POINT));
    }

    public void mixOfChars_ShouldBe_reversed() {
        IntStream lowercases = IntStream.of( 'a', 'b', 'c', 'j', 'o', 'p', 'x', 'y', 'z' );
        List<BoundCharRange> charRanges = CharRange.reverse(lowercases).collect(Collectors.toList());
        assertThat(charRanges).containsExactly(
            new Range(Character.MIN_CODE_POINT, 'a' - 1),
            new Range('c' + 1, 'j' - 1),
            new Range('j' + 1, 'o' - 1),
            new Range('p' + 1, 'x' - 1),
            new Range('z' + 1, Character.MAX_CODE_POINT)
        );
    }

    public void minChar_ShouldBe_reversedToASingleRange() {
        IntStream lowercases = IntStream.of( Character.MIN_CODE_POINT );
        List<BoundCharRange> charRanges = CharRange.reverse(lowercases).collect(Collectors.toList());
        assertThat(charRanges).containsExactly(
            new Range(Character.MIN_CODE_POINT + 1, Character.MAX_CODE_POINT)
        );
    }

    public void maxChar_ShouldBe_reversedToASingleRange() {
        IntStream lowercases = IntStream.of( Character.MAX_CODE_POINT );
        List<BoundCharRange> charRanges = CharRange.reverse(lowercases).collect(Collectors.toList());
        assertThat(charRanges).containsExactly(
            new Range(Character.MIN_CODE_POINT, Character.MAX_CODE_POINT - 1)
        );
    }

    public void minCharWithAnotherChar_ShouldBe_reversedTo2Ranges() {
        IntStream lowercases = IntStream.of( Character.MIN_CODE_POINT, 'a' );
        List<BoundCharRange> charRanges = CharRange.reverse(lowercases).collect(Collectors.toList());
        assertThat(charRanges).containsExactly(
            new Range(Character.MIN_CODE_POINT + 1, 'a' - 1),
            new Range('a' + 1, Character.MAX_CODE_POINT)
        );
    }

    public void twoFirstMinCharsWithAnotherCharAndWithMaxChar_ShouldBe_reversedTo2Ranges() {
        IntStream lowercases = IntStream.of( Character.MIN_CODE_POINT, '\u0001', 'a', Character.MAX_CODE_POINT );
        List<BoundCharRange> charRanges = CharRange.reverse(lowercases).collect(Collectors.toList());
        assertThat(charRanges).containsExactly(
            new Range( '\u0001' + 1, 'a' - 1),
            new Range('a' + 1, Character.MAX_CODE_POINT - 1)
        );
    }

    public void emptyChars_ShouldBe_reversedToAllChars() {
        IntStream lowercases = IntStream.of( );
        List<BoundCharRange> charRanges = CharRange.reverse(lowercases).collect(Collectors.toList());
        assertThat(charRanges).containsExactly(
            new Range(Character.MIN_CODE_POINT, Character.MAX_CODE_POINT)
        );
    }

}
