package java.util.zip;

import java.nio.charset.StandardCharsets;
import java.nio.file.attribute.FileTime;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class ZipEntry implements ZipConstants, Cloneable {
    public static final int DEFLATED = 8;
    static final long DOSTIME_BEFORE_1980 = 2162688;
    public static final int STORED = 0;
    public static final long UPPER_DOSTIME_BOUND = 4036608000000L;
    FileTime atime;
    String comment;
    long crc = -1;
    long csize = -1;
    FileTime ctime;
    long dataOffset;
    byte[] extra;
    int flag = 0;
    int method = -1;
    FileTime mtime;
    String name;
    long size = -1;
    long xdostime = -1;

    public ZipEntry(String name, String comment, long crc, long compressedSize, long size, int compressionMethod, int xdostime, byte[] extra, long dataOffset) {
        this.name = name;
        this.comment = comment;
        this.crc = crc;
        this.csize = compressedSize;
        this.size = size;
        this.method = compressionMethod;
        this.xdostime = (long) xdostime;
        this.dataOffset = dataOffset;
        setExtra0(extra, false);
    }

    public ZipEntry(String name) {
        Objects.requireNonNull((Object) name, "name");
        if (name.getBytes(StandardCharsets.UTF_8).length <= 65535) {
            this.name = name;
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(name);
        stringBuilder.append(" too long: ");
        stringBuilder.append(name.getBytes(StandardCharsets.UTF_8).length);
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    public ZipEntry(ZipEntry e) {
        Objects.requireNonNull((Object) e, "entry");
        this.name = e.name;
        this.xdostime = e.xdostime;
        this.mtime = e.mtime;
        this.atime = e.atime;
        this.ctime = e.ctime;
        this.crc = e.crc;
        this.size = e.size;
        this.csize = e.csize;
        this.method = e.method;
        this.flag = e.flag;
        this.extra = e.extra;
        this.comment = e.comment;
        this.dataOffset = e.dataOffset;
    }

    ZipEntry() {
    }

    public long getDataOffset() {
        return this.dataOffset;
    }

    public String getName() {
        return this.name;
    }

    public void setTime(long time) {
        this.xdostime = ZipUtils.javaToExtendedDosTime(time);
        if (this.xdostime == DOSTIME_BEFORE_1980 || time > UPPER_DOSTIME_BOUND) {
            this.mtime = FileTime.from(time, TimeUnit.MILLISECONDS);
        } else {
            this.mtime = null;
        }
    }

    public long getTime() {
        if (this.mtime != null) {
            return this.mtime.toMillis();
        }
        long j = -1;
        if (this.xdostime != -1) {
            j = ZipUtils.extendedDosToJavaTime(this.xdostime);
        }
        return j;
    }

    public ZipEntry setLastModifiedTime(FileTime time) {
        this.mtime = (FileTime) Objects.requireNonNull((Object) time, "lastModifiedTime");
        this.xdostime = ZipUtils.javaToExtendedDosTime(time.to(TimeUnit.MILLISECONDS));
        return this;
    }

    public FileTime getLastModifiedTime() {
        if (this.mtime != null) {
            return this.mtime;
        }
        if (this.xdostime == -1) {
            return null;
        }
        return FileTime.from(getTime(), TimeUnit.MILLISECONDS);
    }

    public ZipEntry setLastAccessTime(FileTime time) {
        this.atime = (FileTime) Objects.requireNonNull((Object) time, "lastAccessTime");
        return this;
    }

    public FileTime getLastAccessTime() {
        return this.atime;
    }

    public ZipEntry setCreationTime(FileTime time) {
        this.ctime = (FileTime) Objects.requireNonNull((Object) time, "creationTime");
        return this;
    }

    public FileTime getCreationTime() {
        return this.ctime;
    }

    public void setSize(long size) {
        if (size >= 0) {
            this.size = size;
            return;
        }
        throw new IllegalArgumentException("invalid entry size");
    }

    public long getSize() {
        return this.size;
    }

    public long getCompressedSize() {
        return this.csize;
    }

    public void setCompressedSize(long csize) {
        this.csize = csize;
    }

    public void setCrc(long crc) {
        if (crc < 0 || crc > 4294967295L) {
            throw new IllegalArgumentException("invalid entry crc-32");
        }
        this.crc = crc;
    }

    public long getCrc() {
        return this.crc;
    }

    public void setMethod(int method) {
        if (method == 0 || method == 8) {
            this.method = method;
            return;
        }
        throw new IllegalArgumentException("invalid compression method");
    }

    public int getMethod() {
        return this.method;
    }

    public void setExtra(byte[] extra) {
        setExtra0(extra, false);
    }

    void setExtra0(byte[] extra, boolean doZIP64) {
        if (extra != null) {
            if (extra.length <= 65535) {
                int off = 0;
                int len = extra.length;
                while (off + 4 < len) {
                    int tag = ZipUtils.get16(extra, off);
                    int sz = ZipUtils.get16(extra, off + 2);
                    off += 4;
                    if (off + sz > len) {
                        break;
                    }
                    if (tag != 1) {
                        int sz0;
                        if (tag != 10) {
                            if (tag == 21589) {
                                int flag = Byte.toUnsignedInt(extra[off]);
                                sz0 = 1;
                                if ((flag & 1) != 0 && 1 + 4 <= sz) {
                                    this.mtime = ZipUtils.unixTimeToFileTime(ZipUtils.get32(extra, off + 1));
                                    sz0 = 1 + 4;
                                }
                                if ((flag & 2) != 0 && sz0 + 4 <= sz) {
                                    this.atime = ZipUtils.unixTimeToFileTime(ZipUtils.get32(extra, off + sz0));
                                    sz0 += 4;
                                }
                                if ((flag & 4) != 0 && sz0 + 4 <= sz) {
                                    this.ctime = ZipUtils.unixTimeToFileTime(ZipUtils.get32(extra, off + sz0));
                                    sz0 += 4;
                                }
                            }
                        } else if (sz >= 32) {
                            sz0 = off + 4;
                            if (ZipUtils.get16(extra, sz0) == 1 && ZipUtils.get16(extra, sz0 + 2) == 24) {
                                this.mtime = ZipUtils.winTimeToFileTime(ZipUtils.get64(extra, sz0 + 4));
                                this.atime = ZipUtils.winTimeToFileTime(ZipUtils.get64(extra, sz0 + 12));
                                this.ctime = ZipUtils.winTimeToFileTime(ZipUtils.get64(extra, sz0 + 20));
                            }
                        }
                    } else if (doZIP64 && sz >= 16) {
                        this.size = ZipUtils.get64(extra, off);
                        this.csize = ZipUtils.get64(extra, off + 8);
                    }
                    off += sz;
                }
            } else {
                throw new IllegalArgumentException("invalid extra field length");
            }
        }
        this.extra = extra;
    }

    public byte[] getExtra() {
        return this.extra;
    }

    public void setComment(String comment) {
        if (comment == null) {
            this.comment = null;
        } else if (comment.getBytes(StandardCharsets.UTF_8).length <= 65535) {
            this.comment = comment;
        } else {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(comment);
            stringBuilder.append(" too long: ");
            stringBuilder.append(comment.getBytes(StandardCharsets.UTF_8).length);
            throw new IllegalArgumentException(stringBuilder.toString());
        }
    }

    public String getComment() {
        return this.comment;
    }

    public boolean isDirectory() {
        return this.name.endsWith("/");
    }

    public String toString() {
        return getName();
    }

    public int hashCode() {
        return this.name.hashCode();
    }

    public Object clone() {
        try {
            ZipEntry e = (ZipEntry) super.clone();
            e.extra = this.extra == null ? null : (byte[]) this.extra.clone();
            return e;
        } catch (CloneNotSupportedException e2) {
            throw new InternalError(e2);
        }
    }
}
