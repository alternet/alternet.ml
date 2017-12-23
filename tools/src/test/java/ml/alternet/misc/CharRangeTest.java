package ml.alternet.misc;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.Collectors;

import org.testng.annotations.Test;

import ml.alternet.misc.CharRange.BoundRange;

@Test
public class CharRangeTest {

    public void notChar_ShouldBe_reversedTo2Ranges() {
        CharRange c = CharRange.isNot('a');

        List<BoundRange> charRanges = c.asIntervals().collect(Collectors.toList());
        assertThat(charRanges).containsExactly(
            CharRange.range(Character.MIN_CODE_POINT, 'a' - 1),
            CharRange.range('a' + 1, Character.MAX_CODE_POINT));
    }

    public void singleChar_ShouldBe_theSameAsARangeThatStartAndEndWithTheSameChar() {
        CharRange c = CharRange.is('a');
        CharRange r = CharRange.range('a', 'a');
        List<BoundRange> charRanges = c.asIntervals().collect(Collectors.toList());
        assertThat(charRanges).isEqualTo(
                r.asIntervals().collect(Collectors.toList()));
        assertThat(c).isEqualTo(r);
        assertThat(c.toString()).isEqualTo(r.toString());
    }

    public void rangeWhereEndIsBeforeStart_ShouldBe_empty() {
        CharRange.BoundRange r = CharRange.range('b', 'a');
        assertThat(r.isEmpty()).isTrue();
        assertThat(r.toString()).isEqualTo("''");
    }

    public void charInterval_ShouldBe_Itself() {
        CharRange c = CharRange.is('a');
        assertThat(c.contains('a')).isTrue();
        List<BoundRange> charRanges = c.asIntervals().collect(Collectors.toList());
        assertThat(charRanges).containsExactly(
                CharRange.is('a'));
    }

}
