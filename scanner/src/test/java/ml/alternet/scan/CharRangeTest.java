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
public class CharRangeTest {

    public void char_Should_containOnlyItsChar() {
        Char c = new Char(true, 'a');
        assertThat(c.contains('a')).isTrue();
        assertThat(c.contains('b')).isFalse();
    }

    public void rangeOfSingleChar_Should_containOnlyItsChar() {
        Range r = new Range('a', 'a');
        assertThat(r.contains('a')).isTrue();
        assertThat(r.contains('b')).isFalse();
    }

    public void range_Should_containOnlyItsChars() {
        Range r = new Range('a', 'z');
        assertThat(r.contains('a')).isTrue();
        assertThat(r.contains('b')).isTrue();
        assertThat(r.contains('z')).isTrue();
        assertThat(r.contains('A')).isFalse();
    }

    public void charsOfSingleChar_Should_containOnlyItsChar() {
        Chars r = new Chars(true, "a");
        assertThat(r.contains('a')).isTrue();
        assertThat(r.contains('b')).isFalse();
    }

    public void chars_Should_containOnlyItsChars() {
        Chars r = new Chars(true, "abcz");
        assertThat(r.contains('a')).isTrue();
        assertThat(r.contains('b')).isTrue();
        assertThat(r.contains('z')).isTrue();
        assertThat(r.contains('A')).isFalse();
    }

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
