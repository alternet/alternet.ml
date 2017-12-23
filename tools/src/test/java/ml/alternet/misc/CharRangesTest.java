package ml.alternet.misc;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.testng.annotations.Test;

import ml.alternet.misc.CharRange;
import ml.alternet.misc.CharRange.BoundRange;

@Test
public class CharRangesTest {

    public void unionOfOrderedChar_Should_giveOrderedRanges() {
        CharRange c = CharRange.range('A', 'Z').union(CharRange.is('a'));

        assertThat(c.getClass()).isAssignableFrom(ml.alternet.misc.CharRange$.Ranges.class);

        List<BoundRange> charRanges = c.asIntervals().collect(Collectors.toList());
        assertThat(charRanges).containsExactly(
            CharRange.range('A', 'Z'),
            CharRange.is('a')
        );
    }

    public void unionOfUnorderedChar_Should_giveOrderedRanges() {
        // check order too
        CharRange c = CharRange.range('A', 'Z').union(CharRange.isOneOf("9287315")).union(CharRange.is('a'));

        assertThat(c.getClass()).isAssignableFrom(ml.alternet.misc.CharRange$.Ranges.class);

        List<BoundRange> charRanges = c.asIntervals().collect(Collectors.toList());
        assertThat(charRanges).containsExactly(
            CharRange.range('1', '3'),
            CharRange.is('5'),
            CharRange.range('7', '9'),
            CharRange.range('A', 'Z'),
            CharRange.is('a'));
    }

    public void alphaChar_ShouldBe_orderedAfterNumericRange() {
        BoundRange a = CharRange.is('a');
        BoundRange oneToThree = CharRange.range('1', '3');
        assertThat(a).isGreaterThan(oneToThree);
    }

    public void alphaRange_ShouldBe_orderedAfterNumericChar() {
        BoundRange one = CharRange.is('1');
        BoundRange aTof = CharRange.range('a', 'f');
        assertThat(aTof).isGreaterThan(one);
    }

    public void variousRanges_ShouldBe_sortedByStartChar() {
        List<BoundRange> list = Arrays.asList(
            CharRange.is('a'),
            CharRange.is('5'),
            CharRange.range('1', '3'),
            CharRange.range('A', 'Z'),
            CharRange.range('7', '9')
        );
        list = list.stream().sorted().collect(Collectors.toList());
        assertThat(list).containsExactly(
            CharRange.range('1', '3'),
            CharRange.is('5'),
            CharRange.range('7', '9'),
            CharRange.range('A', 'Z'),
            CharRange.is('a')
        );
    }

}
