package ml.alternet.misc;

import static org.assertj.core.api.Assertions.assertThat;

import org.testng.annotations.Test;

@Test
public class CharRangeContainsTest {

    public void char_Should_containOnlyItsChar() {
        CharRange c = CharRange.is('a');
        assertThat(c.contains('a')).isTrue();
        assertThat(c.contains('A')).isFalse();
    }

    public void rangeOfSingleChar_Should_containOnlyItsChar() {
        CharRange r = CharRange.range('a', 'a');
        assertThat(r.contains('a')).isTrue();
        assertThat(r.contains('A')).isFalse();
    }

    public void range_Should_containOnlyItsChars() {
        CharRange r = CharRange.range('a', 'z');
        assertThat(r.contains('a')).isTrue();
        assertThat(r.contains('b')).isTrue();
        assertThat(r.contains('z')).isTrue();
        assertThat(r.contains('A')).isFalse();
    }

    public void charsOfSingleChar_Should_containOnlyItsChar() {
        CharRange c = CharRange.isOneOf("a");
        assertThat(c.contains('a')).isTrue();
        assertThat(c.contains('A')).isFalse();
    }

    public void chars_Should_containOnlyTheirChars() {
        CharRange c = CharRange.isOneOf("abcz");
        assertThat(c.contains('a')).isTrue();
        assertThat(c.contains('b')).isTrue();
        assertThat(c.contains('z')).isTrue();
        assertThat(c.contains('A')).isFalse();
    }

    public void notChar_Should_containOnlyOtherChars() {
        CharRange c = CharRange.isNot('a');
        assertThat(c.contains('A')).isTrue();
        assertThat(c.contains('a')).isFalse();
    }

    public void notCharsOfSingleChar_Should_containOtherChars() {
        CharRange c = CharRange.isNotOneOf("a");
        assertThat(c.contains('A')).isTrue();
        assertThat(c.contains('a')).isFalse();
    }

    public void notChars_Should_containOnlyOtherChars() {
        CharRange c = CharRange.isNotOneOf("abcz");
        assertThat(c.contains('a')).isFalse();
        assertThat(c.contains('b')).isFalse();
        assertThat(c.contains('z')).isFalse();
        assertThat(c.contains('A')).isTrue();
    }

}
