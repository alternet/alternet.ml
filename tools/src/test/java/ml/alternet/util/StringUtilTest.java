package ml.alternet.util;

import ml.alternet.util.StringUtil;

import org.assertj.core.api.Assertions;
import org.testng.annotations.Test;

public class StringUtilTest {

    @Test
    public void removeDiacritics_Should_RemoveUpercaseDiacritics() {
        Assertions.assertThat(StringUtil.removeDiacritics("ÀÁÂÃÄÅÇÈÉÊËÌÍÎÏÑÒÓÔÕÖÙÚÛÜÝ")).isEqualTo("AAAAAACEEEEIIIINOOOOOUUUUY");
    }

    @Test
    public void removeDiacritics_Should_RemoveLowercaseDiacritics() {
        Assertions.assertThat(StringUtil.removeDiacritics("àáâãäåçèéêëìíîïñòóôõöùúûüýÿ")).isEqualTo("aaaaaaceeeeiiiinooooouuuuyy");
    }

}
