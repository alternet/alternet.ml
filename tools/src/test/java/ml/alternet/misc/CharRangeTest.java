package ml.alternet.misc;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.Collectors;

import org.testng.annotations.Test;

import ml.alternet.misc.CharRange.BoundRange;
import ml.alternet.misc.CharRange.Char;
import ml.alternet.misc.CharRange.Range;

@Test
public class CharRangeTest {

    public void notChar_ShouldBe_reversedTo2Ranges() {
        Char c = new Char(false, 'a');

        List<BoundRange> charRanges = c.asIntervals().collect(Collectors.toList());
        assertThat(charRanges).containsExactly(
            new Range(Character.MIN_CODE_POINT, 'a' - 1),
            new Range('a' + 1, Character.MAX_CODE_POINT));
    }

    public void singleChar_ShouldBe_theSameAsARangeThatStartAndEndWithTheSameChar() {
        Char c = new Char(true, 'a');
        Range r = new Range('a', 'a');
        List<BoundRange> charRanges = c.asIntervals().collect(Collectors.toList());
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
        List<BoundRange> charRanges = c.asIntervals().collect(Collectors.toList());
        assertThat(charRanges).containsExactly(
            new Char(true, 'a'));
    }

}
