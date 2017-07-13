package ml.alternet.scan;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.testng.annotations.Test;

import ml.alternet.scan.CharRange.BoundRange;
import ml.alternet.scan.CharRange.Char;
import ml.alternet.scan.CharRange.Chars;
import ml.alternet.scan.CharRange.Range;
import ml.alternet.scan.CharRange.Ranges;

@Test
public class CharRangesTest {

    public void unionOfOrderedChar_Should_giveOrderedRanges() {
        CharRange c = new Range('A', 'Z').union(new Char(true, 'a'));

        assertThat(c.getClass()).isAssignableFrom(Ranges.class);

        List<BoundRange> charRanges = c.asIntervals().collect(Collectors.toList());
        assertThat(charRanges).containsExactly(
            new Range('A', 'Z'),
            new Char(true, 'a')
        );
    }

    public void unionOfUnorderedChar_Should_giveOrderedRanges() {
        // check order too
        CharRange c = new Range('A', 'Z').union(new Chars(true, "9287315")).union(new Char(true, 'a'));

        assertThat(c.getClass()).isAssignableFrom(Ranges.class);

        List<BoundRange> charRanges = c.asIntervals().collect(Collectors.toList());
        assertThat(charRanges).containsExactly(
            new Range('1', '3'),
            new Char(true, '5'),
            new Range('7', '9'),
            new Range('A', 'Z'),
            new Char(true, 'a'));
    }

    public void alphaChar_ShouldBe_orderedAfterNumericRange() {
        BoundRange a = new Char(true, 'a');
        BoundRange oneToThree = new Range('1', '3');
        assertThat(a).isGreaterThan(oneToThree);
    }

    public void alphaRange_ShouldBe_orderedAfterNumericChar() {
        BoundRange one = new Char(true, '1');
        BoundRange aTof = new Range('a', 'f');
        assertThat(aTof).isGreaterThan(one);
    }

    public void variousRanges_ShouldBe_sortedByStartChar() {
        List<BoundRange> list = Arrays.asList(
            new Char(true, 'a'),
            new Char(true, '5'),
            new Range('1', '3'),
            new Range('A', 'Z'),
            new Range('7', '9')
        );
        list = list.stream().sorted().collect(Collectors.toList());
        assertThat(list).containsExactly(
            new Range('1', '3'),
            new Char(true, '5'),
            new Range('7', '9'),
            new Range('A', 'Z'),
            new Char(true, 'a')
        );
    }

}
