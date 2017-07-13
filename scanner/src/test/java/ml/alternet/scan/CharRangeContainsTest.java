package ml.alternet.scan;

import static org.assertj.core.api.Assertions.assertThat;

import org.testng.annotations.Test;

import ml.alternet.scan.CharRange.Char;
import ml.alternet.scan.CharRange.Chars;
import ml.alternet.scan.CharRange.Range;

@Test
public class CharRangeContainsTest {

    public void char_Should_containOnlyItsChar() {
        Char c = new Char(true, 'a');
        assertThat(c.contains('a')).isTrue();
        assertThat(c.contains('A')).isFalse();
    }

    public void rangeOfSingleChar_Should_containOnlyItsChar() {
        Range r = new Range('a', 'a');
        assertThat(r.contains('a')).isTrue();
        assertThat(r.contains('A')).isFalse();
    }

    public void range_Should_containOnlyItsChars() {
        Range r = new Range('a', 'z');
        assertThat(r.contains('a')).isTrue();
        assertThat(r.contains('b')).isTrue();
        assertThat(r.contains('z')).isTrue();
        assertThat(r.contains('A')).isFalse();
    }

    public void charsOfSingleChar_Should_containOnlyItsChar() {
        Chars c = new Chars(true, "a");
        assertThat(c.contains('a')).isTrue();
        assertThat(c.contains('A')).isFalse();
    }

    public void chars_Should_containOnlyTheirChars() {
        Chars c = new Chars(true, "abcz");
        assertThat(c.contains('a')).isTrue();
        assertThat(c.contains('b')).isTrue();
        assertThat(c.contains('z')).isTrue();
        assertThat(c.contains('A')).isFalse();
    }

    public void notChar_Should_containOnlyOtherChars() {
        Char c = new Char(false, 'a');
        assertThat(c.contains('A')).isTrue();
        assertThat(c.contains('a')).isFalse();
    }

    public void notCharsOfSingleChar_Should_containOtherChars() {
        Chars c = new Chars(false, "a");
        assertThat(c.contains('A')).isTrue();
        assertThat(c.contains('a')).isFalse();
    }

    public void notChars_Should_containOnlyOtherChars() {
        Chars c = new Chars(false, "abcz");
        assertThat(c.contains('a')).isFalse();
        assertThat(c.contains('b')).isFalse();
        assertThat(c.contains('z')).isFalse();
        assertThat(c.contains('A')).isTrue();
    }

}
