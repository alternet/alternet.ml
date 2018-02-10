package ml.alternet.test.security.auth;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;

import org.assertj.core.api.Assertions;
import org.testng.annotations.Test;

import ml.alternet.security.binary.SafeBuffer;

public class SafeBufferTest {

    @Test
    public void SafeEncoding_ShouldWork_LikeStandardEncoding() {
        String data = "abcdefghijklmnopqrstuvwxyz";
        byte[] expected = data.getBytes(StandardCharsets.UTF_8);
        ByteBuffer bb = SafeBuffer.encode(CharBuffer.wrap(data.toCharArray()), StandardCharsets.UTF_8);
        byte[] bytes = new byte[bb.limit()];
        bb.get(bytes);
        Assertions.assertThat(bytes).isEqualTo(expected);
    }

    @Test
    public void ByteArrayOfPartialBuffer_ShouldNotBe_TheOneExtracted() {
        String data = "abcdefghijklmnopqrstuvwxyz";
        int start = 4;
        int len = 12;
        byte[] bytes = data.getBytes(StandardCharsets.UTF_8);
        byte[] expected = new byte[len];
        System.arraycopy(bytes, start, expected, 0, len);
        ByteBuffer bb = ByteBuffer.wrap(bytes, start, len);
        byte[] actual = SafeBuffer.getData(bb);
        Assertions.assertThat(actual.length).isEqualTo(len);
        Assertions.assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void ByteArrayOfFullBuffer_ShouldBe_TheOneExtracted() {
        String data = "abcdefghijklmnopqrstuvwxyz";
        byte[] expected = data.getBytes(StandardCharsets.UTF_8);
        int start = 0;
        int len = expected.length;
        ByteBuffer bb = ByteBuffer.wrap(expected, start, len);
        byte[] actual = SafeBuffer.getData(bb);
        Assertions.assertThat(actual.length).isEqualTo(len);
        Assertions.assertThat(actual).isSameAs(expected);
    }

    @Test
    public void Bytes_ShouldBe_AppendedToBuffer() {
        String data1 = "abcdefghijklmnopqrstuvwxyz";
        String data2 = "0123456789";
        int start = 4;
        int len = 12;
        byte[] bytes1 = data1.getBytes(StandardCharsets.UTF_8);
        byte[] bytes2 = data2.getBytes(StandardCharsets.UTF_8);

        byte[] expected = new byte[len + bytes2.length];
        System.arraycopy(bytes1, start, expected, 0, len);
        System.arraycopy(bytes2, 0, expected, len, bytes2.length);

        // do same operation with our method
        ByteBuffer bb = ByteBuffer.wrap(bytes1, start, len);
        SafeBuffer.append(bb, bytes2);
        byte[] actual = SafeBuffer.getData(bb);

        // check whether we get the same
        Assertions.assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void TooMuchBytes_ShouldBe_AppendedToBuffer() {
        String data1 = "abcdefghijklmnopqrstuvwxyz";
        String data2 = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        int start = 4;
        int len = 12;
        byte[] bytes1 = data1.getBytes(StandardCharsets.UTF_8);
        byte[] bytes2 = data2.getBytes(StandardCharsets.UTF_8);

        byte[] expected = new byte[len + bytes2.length];
        System.arraycopy(bytes1, start, expected, 0, len);
        System.arraycopy(bytes2, 0, expected, len, bytes2.length);

        // do same operation with our method
        ByteBuffer bb = ByteBuffer.wrap(bytes1, start, len);
        bb = SafeBuffer.append(bb, bytes2);
        byte[] actual = SafeBuffer.getData(bb);

        // check whether we get the same
        Assertions.assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void BufferUnderflow_ShouldBe_extracted() {
        String data1 = "abcd";
        byte[] bytes1 = data1.getBytes(StandardCharsets.UTF_8);

        byte[] expected = bytes1;
        // do same operation with our method
        ByteBuffer bb = ByteBuffer.allocate(bytes1.length * 2);
        bb.flip();
        bb = SafeBuffer.append(bb, bytes1);
        byte[] actual = SafeBuffer.getData(bb);

        // check whether we get the same
        Assertions.assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void TooMuchChars_ShouldBe_AppendedToBuffer() {
        char[] data1 = "abcdefghijklmnopqrstuvwxyz".toCharArray();
        char[] data2 = "ABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
        int start = 4;
        int len = 12;

        char[] expected = new char[len + data2.length];
        System.arraycopy(data1, start, expected, 0, len);
        System.arraycopy(data2, 0, expected, len, data2.length);

        // do same operation with our method
        CharBuffer bb = CharBuffer.wrap(data1, start, len);
        bb = SafeBuffer.append(bb, data2);
        char[] actual = SafeBuffer.getData(bb);

        // check whether we get the same
        Assertions.assertThat(actual).isEqualTo(expected);
    }


}
