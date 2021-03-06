package org.bouncycastle.util.test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.Provider;
import java.security.SecureRandom;
import org.bouncycastle.util.Pack;
import org.bouncycastle.util.encoders.Hex;

public class FixedSecureRandom extends SecureRandom {
    private static java.math.BigInteger ANDROID = new java.math.BigInteger("1111111105060708ffffffff01020304", 16);
    private static java.math.BigInteger CLASSPATH = new java.math.BigInteger("3020104ffffffff05060708111111", 16);
    private static java.math.BigInteger REGULAR = new java.math.BigInteger("01020304ffffffff0506070811111111", 16);
    private static final boolean isAndroidStyle;
    private static final boolean isClasspathStyle;
    private static final boolean isRegularStyle;
    private byte[] _data;
    private int _index;

    private static class DummyProvider extends Provider {
        DummyProvider() {
            super("BCFIPS_FIXED_RNG", 1.0d, "BCFIPS Fixed Secure Random Provider");
        }
    }

    private static class RandomChecker extends SecureRandom {
        byte[] data = Hex.decode("01020304ffffffff0506070811111111");
        int index = 0;

        RandomChecker() {
            super(null, new DummyProvider());
        }

        public void nextBytes(byte[] bArr) {
            System.arraycopy(this.data, this.index, bArr, 0, bArr.length);
            this.index += bArr.length;
        }
    }

    public static class Source {
        byte[] data;

        Source(byte[] bArr) {
            this.data = bArr;
        }
    }

    public static class BigInteger extends Source {
        public BigInteger(int i, String str) {
            super(FixedSecureRandom.expandToBitLength(i, Hex.decode(str)));
        }

        public BigInteger(int i, byte[] bArr) {
            super(FixedSecureRandom.expandToBitLength(i, bArr));
        }

        public BigInteger(String str) {
            this(Hex.decode(str));
        }

        public BigInteger(byte[] bArr) {
            super(bArr);
        }
    }

    public static class Data extends Source {
        public Data(byte[] bArr) {
            super(bArr);
        }
    }

    static {
        java.math.BigInteger bigInteger = new java.math.BigInteger(128, new RandomChecker());
        java.math.BigInteger bigInteger2 = new java.math.BigInteger(120, new RandomChecker());
        isAndroidStyle = bigInteger.equals(ANDROID);
        isRegularStyle = bigInteger.equals(REGULAR);
        isClasspathStyle = bigInteger2.equals(CLASSPATH);
    }

    public FixedSecureRandom(byte[] bArr) {
        this(new Source[]{new Data(bArr)});
    }

    public FixedSecureRandom(Source[] sourceArr) {
        super(null, new DummyProvider());
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        int i = 0;
        int length;
        if (isRegularStyle) {
            if (isClasspathStyle) {
                while (i != sourceArr.length) {
                    try {
                        if (sourceArr[i] instanceof BigInteger) {
                            byte[] bArr = sourceArr[i].data;
                            int length2 = bArr.length - (bArr.length % 4);
                            for (length = (bArr.length - length2) - 1; length >= 0; length--) {
                                byteArrayOutputStream.write(bArr[length]);
                            }
                            for (length = bArr.length - length2; length < bArr.length; length += 4) {
                                byteArrayOutputStream.write(bArr, length, 4);
                            }
                        } else {
                            byteArrayOutputStream.write(sourceArr[i].data);
                        }
                        i++;
                    } catch (IOException e) {
                        throw new IllegalArgumentException("can't save value source.");
                    }
                }
            }
            while (i != sourceArr.length) {
                try {
                    byteArrayOutputStream.write(sourceArr[i].data);
                    i++;
                } catch (IOException e2) {
                    throw new IllegalArgumentException("can't save value source.");
                }
            }
        } else if (isAndroidStyle) {
            int i2 = 0;
            while (i2 != sourceArr.length) {
                try {
                    if (sourceArr[i2] instanceof BigInteger) {
                        byte[] bArr2 = sourceArr[i2].data;
                        length = bArr2.length - (bArr2.length % 4);
                        int i3 = 0;
                        while (i3 < length) {
                            i3 += 4;
                            byteArrayOutputStream.write(bArr2, bArr2.length - i3, 4);
                        }
                        if (bArr2.length - length != 0) {
                            for (i3 = 0; i3 != 4 - (bArr2.length - length); i3++) {
                                byteArrayOutputStream.write(0);
                            }
                        }
                        for (i3 = 0; i3 != bArr2.length - length; i3++) {
                            byteArrayOutputStream.write(bArr2[length + i3]);
                        }
                    } else {
                        byteArrayOutputStream.write(sourceArr[i2].data);
                    }
                    i2++;
                } catch (IOException e3) {
                    throw new IllegalArgumentException("can't save value source.");
                }
            }
        } else {
            throw new IllegalStateException("Unrecognized BigInteger implementation");
        }
        this._data = byteArrayOutputStream.toByteArray();
    }

    public FixedSecureRandom(byte[][] bArr) {
        this(buildDataArray(bArr));
    }

    private static Data[] buildDataArray(byte[][] bArr) {
        Data[] dataArr = new Data[bArr.length];
        for (int i = 0; i != bArr.length; i++) {
            dataArr[i] = new Data(bArr[i]);
        }
        return dataArr;
    }

    private static byte[] expandToBitLength(int i, byte[] bArr) {
        int i2 = (i + 7) / 8;
        if (i2 > bArr.length) {
            byte[] bArr2 = new byte[i2];
            System.arraycopy(bArr, 0, bArr2, bArr2.length - bArr.length, bArr.length);
            if (isAndroidStyle) {
                i %= 8;
                if (i != 0) {
                    Pack.intToBigEndian(Pack.bigEndianToInt(bArr2, 0) << (8 - i), bArr2, 0);
                }
            }
            return bArr2;
        }
        if (isAndroidStyle && i < bArr.length * 8) {
            i %= 8;
            if (i != 0) {
                Pack.intToBigEndian(Pack.bigEndianToInt(bArr, 0) << (8 - i), bArr, 0);
            }
        }
        return bArr;
    }

    private int nextValue() {
        byte[] bArr = this._data;
        int i = this._index;
        this._index = i + 1;
        return bArr[i] & 255;
    }

    public byte[] generateSeed(int i) {
        byte[] bArr = new byte[i];
        nextBytes(bArr);
        return bArr;
    }

    public boolean isExhausted() {
        return this._index == this._data.length;
    }

    public void nextBytes(byte[] bArr) {
        System.arraycopy(this._data, this._index, bArr, 0, bArr.length);
        this._index += bArr.length;
    }

    public int nextInt() {
        return ((((nextValue() << 24) | 0) | (nextValue() << 16)) | (nextValue() << 8)) | nextValue();
    }

    public long nextLong() {
        return ((((((((((long) nextValue()) << 56) | 0) | (((long) nextValue()) << 48)) | (((long) nextValue()) << 40)) | (((long) nextValue()) << 32)) | (((long) nextValue()) << 24)) | (((long) nextValue()) << 16)) | (((long) nextValue()) << 8)) | ((long) nextValue());
    }
}
