package ml.alternet.util;

import java.math.BigInteger;

import org.assertj.core.api.Assertions;
import org.testng.annotations.Test;

public class NumberUtilTest {

    BigInteger bigLong = BigInteger.valueOf(Long.MAX_VALUE);
    BigInteger twiceALong = bigLong.add(bigLong);
    String twiceALongAsString = twiceALong.toString();

    @Test
    public void parseNumber_Should_ParseBigInt() {
        Number n = NumberUtil.parseNumber(twiceALongAsString, true, BigInteger.class);
        Assertions.assertThat(n).isInstanceOf(BigInteger.class);
        Assertions.assertThat(n).isEqualTo(twiceALong);
    }

    @Test
    public void parseNumber_Should_ParseNonFloatingPointBigIntToBigInt() {
        Number n = NumberUtil.parseNumber(twiceALongAsString, false, BigInteger.class);
        Assertions.assertThat(n).isInstanceOf(BigInteger.class);
        Assertions.assertThat(n).isEqualTo(twiceALong);
    }

    @Test
    public void parseNumber_Should_ParseBigIntToFloat() {
        Number n = NumberUtil.parseNumber(twiceALongAsString);
        Assertions.assertThat(n).isInstanceOf(Float.class);
        Assertions.assertThat(n).isEqualTo(Float.parseFloat(twiceALongAsString));
    }

    @Test
    public void parseNumber_Should_ParseMaxFloatToFloat() {
        Number n = NumberUtil.parseNumber(toDecimal("" + Float.MAX_VALUE));
        Assertions.assertThat(n).isInstanceOf(Float.class);
        Assertions.assertThat(n).isEqualTo(Float.MAX_VALUE);
    }

    @Test
    public void parseNumber_Should_ParseIntBiggerThanMaxFloatToDouble() {
        Number n = NumberUtil.parseNumber(toDecimal("" + Float.MAX_VALUE) + "0"); // Float.MAX_VALUE
                                                                                  // *
                                                                                  // 10
                                                                                  // is
                                                                                  // a
                                                                                  // double
        Assertions.assertThat(n).isInstanceOf(Double.class);
        Assertions.assertThat(n).isEqualTo(Double.parseDouble(toDecimal("" + Float.MAX_VALUE) + "0"));
    }

    @Test
    public void parseNumber_Should_ParseIntBiggerThanMaxDoubleToBigInt() {
        String biggerThanADouble = toDecimal("" + Double.MAX_VALUE) + "0";
        Number n = NumberUtil.parseNumber(biggerThanADouble); // Double.MAX_VALUE
                                                              // * 10 is a big
                                                              // int
        Assertions.assertThat(n).isInstanceOf(BigInteger.class);
        Assertions.assertThat(n).isEqualTo(new BigInteger(biggerThanADouble));
    }

    @Test
    public void toDecimal_Should_ConvertMaxFloat() {
        String maxFloatAsString = "340282350000000000000000000000000000000"; // Float.MAX_VALUE
        Assertions.assertThat(toDecimal("" + Float.MAX_VALUE)).isEqualTo(maxFloatAsString);
    }

    String toDecimal(String number) {
        String[] parts = number.split("E");
        int exp = Integer.parseInt(parts[1]);
        parts = parts[0].split("\\.");
        StringBuffer s = new StringBuffer(exp + 20);
        s.append(parts[0]);
        for (int i = 0; i < parts[1].length(); i++) {
            s.append(parts[1].charAt(i));
            exp--;
        }
        while (exp-- > 0) {
            s.append('0');
        }
        return s.toString();
    }

}
