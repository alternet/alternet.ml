package ml.alternet.security.binary;

/**
 * Little Endian subject to code inlining.
 *
 * @author Philippe Poulard
 */
public final class LittleEndian {

    private LittleEndian() { }

    public static int readInt(final byte[] b, int off) {
        int v0
            = (b [ off++ ] & 0xFF );
        v0 |= (b [ off++ ] & 0xFF ) <<  8;
        v0 |= (b [ off++ ] & 0xFF ) << 16;
        v0 |= (b [ off   ]        ) << 24;
        return v0;
    }

    /** Little endian - byte[] to long */
    public static long readLong(final byte[] b, int off) {
        long v0
            = ((long)b [ off++ ] & 0xFF );
        v0 |= ((long)b [ off++ ] & 0xFF ) <<  8;
        v0 |= ((long)b [ off++ ] & 0xFF ) << 16;
        v0 |= ((long)b [ off++ ] & 0xFF ) << 24;
        v0 |= ((long)b [ off++ ] & 0xFF ) << 32;
        v0 |= ((long)b [ off++ ] & 0xFF ) << 40;
        v0 |= ((long)b [ off++ ] & 0xFF ) << 48;
        v0 |= ((long)b [ off   ]        ) << 56;
        return v0;
    }

    /** */
    /** Little endian - long to byte[] */
    public static final void writeLong(long v, final byte[] b, final int off) {
        b [ off ]     = (byte) v; v >>>= 8;
        b [ off + 1 ] = (byte) v; v >>>= 8;
        b [ off + 2 ] = (byte) v; v >>>= 8;
        b [ off + 3 ] = (byte) v; v >>>= 8;
        b [ off + 4 ] = (byte) v; v >>>= 8;
        b [ off + 5 ] = (byte) v; v >>>= 8;
        b [ off + 6 ] = (byte) v; v >>>= 8;
        b [ off + 7 ] = (byte) v;
    }

    /** Little endian - int to byte[] */
    public static void writeInt(int v, final byte[] b, final int off) {
        b [ off ]     = (byte) v; v >>>= 8;
        b [ off + 1 ] = (byte) v; v >>>= 8;
        b [ off + 2 ] = (byte) v; v >>>= 8;
        b [ off + 3 ] = (byte) v;
    }

    public static long bytesToLong(byte[] b) {
        return readLong(b, 0);
    }

    public static byte[] intToBytes(int i) {
        byte[] result = new byte[4];
        writeInt(i, result, 0);
        return result;
    }

    public static byte[] longToBytes(long l) {
        byte[] result = new byte[8];
        writeLong(l, result, 0);
        return result;
    }

}
