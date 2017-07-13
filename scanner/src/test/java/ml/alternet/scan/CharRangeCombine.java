package ml.alternet.scan;

import static org.assertj.core.api.Assertions.assertThat;

import org.testng.annotations.Test;

import ml.alternet.scan.CharRange.Char;
import static ml.alternet.scan.CharRange.*;

@Test
public class CharRangeCombine {

    public void anyCharsExceptAChar_Shoud_haveTheSameIntervalsThanNotAChar() {
        Char a = CharRange.is('a');
        CharRange anyExceptA = ANY.except(a);
        Char notA = CharRange.isNot('a');
        assertThat(anyExceptA.asIntervals().findFirst().get()).isEqualTo(notA.asIntervals().findFirst().get());
    }

    public void doubleNegateForChar_Shoud_giveTheSame() {
        Char a = CharRange.is('a');
        CharRange anyExceptA = ANY.except(a);
        CharRange aAgain = ANY.except(anyExceptA);
        assertThat(a).isEqualTo(aAgain);
    }

    public void doubleNegateForNotChar_Shoud_giveTheSame() {
        Char a = CharRange.isNot('a');
        CharRange anyExceptA = ANY.except(a);
        CharRange aAgain = ANY.except(anyExceptA);
        assertThat(CHAR_RANGE_COMPARATOR.compare(a, aAgain)).isEqualTo(0);
    }

}
