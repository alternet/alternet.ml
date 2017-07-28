package ml.alternet.scan;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.testng.annotations.Test;

import ml.alternet.scan.CharRange.BoundRange;
import ml.alternet.scan.CharRange.Range;
import static ml.alternet.scan.CharRange.Chars.reverse;

@Test
public class CharRangeReverseTest {

    public void singleCharStream_ShouldBe_reversedTo2Ranges() {
        IntStream lowercases = IntStream.of( 'a' );
        List<BoundRange> charRanges = reverse(lowercases).collect(Collectors.toList());
        assertThat(charRanges).containsExactly(
            new Range(Character.MIN_CODE_POINT, 'a' - 1),
            new Range('a' + 1, Character.MAX_CODE_POINT));
    }

    public void twoChars_ShouldBe_reversedTo3Ranges() {
        IntStream lowercases = IntStream.of( 'a', 'z' );
        List<BoundRange> charRanges = reverse(lowercases).collect(Collectors.toList());
        assertThat(charRanges).containsExactly(
            new Range(Character.MIN_CODE_POINT, 'a' - 1),
            new Range('a' + 1, 'z' -1),
            new Range('z' + 1, Character.MAX_CODE_POINT));
    }

    public void twoConsecutiveChars_ShouldBe_reversedTo2Ranges() {
        IntStream lowercases = IntStream.of( 'a', 'b' );
        List<BoundRange> charRanges = reverse(lowercases).collect(Collectors.toList());
        assertThat(charRanges).containsExactly(
            new Range(Character.MIN_CODE_POINT, 'a' - 1),
            new Range('b' + 1, Character.MAX_CODE_POINT));
    }

    public void severalConsecutiveChars_ShouldBe_reversedTo2Ranges() {
        IntStream lowercases = IntStream.of( 'a', 'b', 'c', 'd', 'e' );
        List<BoundRange> charRanges = reverse(lowercases).collect(Collectors.toList());
        assertThat(charRanges).containsExactly(
            new Range(Character.MIN_CODE_POINT, 'a' - 1),
            new Range('e' + 1, Character.MAX_CODE_POINT));
    }

    public void mixOfChars_ShouldBe_reversed() {
        IntStream lowercases = IntStream.of( 'a', 'b', 'c', 'j', 'o', 'p', 'x', 'y', 'z' );
        List<BoundRange> charRanges = reverse(lowercases).collect(Collectors.toList());
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
        List<BoundRange> charRanges = reverse(lowercases).collect(Collectors.toList());
        assertThat(charRanges).containsExactly(
            new Range(Character.MIN_CODE_POINT + 1, Character.MAX_CODE_POINT)
        );
    }

    public void maxChar_ShouldBe_reversedToASingleRange() {
        IntStream lowercases = IntStream.of( Character.MAX_CODE_POINT );
        List<BoundRange> charRanges = reverse(lowercases).collect(Collectors.toList());
        assertThat(charRanges).containsExactly(
            new Range(Character.MIN_CODE_POINT, Character.MAX_CODE_POINT - 1)
        );
    }

    public void minCharWithAnotherChar_ShouldBe_reversedTo2Ranges() {
        IntStream lowercases = IntStream.of( Character.MIN_CODE_POINT, 'a' );
        List<BoundRange> charRanges = reverse(lowercases).collect(Collectors.toList());
        assertThat(charRanges).containsExactly(
            new Range(Character.MIN_CODE_POINT + 1, 'a' - 1),
            new Range('a' + 1, Character.MAX_CODE_POINT)
        );
    }

    public void twoFirstMinCharsWithAnotherCharAndWithMaxChar_ShouldBe_reversedTo2Ranges() {
        IntStream lowercases = IntStream.of( Character.MIN_CODE_POINT, '\u0001', 'a', Character.MAX_CODE_POINT );
        List<BoundRange> charRanges = reverse(lowercases).collect(Collectors.toList());
        assertThat(charRanges).containsExactly(
            new Range( '\u0001' + 1, 'a' - 1),
            new Range('a' + 1, Character.MAX_CODE_POINT - 1)
        );
    }

    public void emptyChars_ShouldBe_reversedToAllChars() {
        IntStream lowercases = IntStream.of( );
        List<BoundRange> charRanges = reverse(lowercases).collect(Collectors.toList());
        assertThat(charRanges).containsExactly(
            new Range(Character.MIN_CODE_POINT, Character.MAX_CODE_POINT)
        );
    }

}
