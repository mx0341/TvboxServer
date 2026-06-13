import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;

//流式Base64 tmd Java8 :(
public class Base64DecodingInputStream extends InputStream {
    private final InputStream base64InputStream;
    private final byte[] buffer;        // 解码后缓存
    private int pos = 0, limit = 0;     // 当前缓冲区位置和长度
    private final byte[] chunk;         // 读取 Base64 数据的块
    private boolean eof = false;

    // Base64 块大小是 4 的倍数，缓冲区设为 4096（4 的倍数）
    public Base64DecodingInputStream(InputStream base64InputStream, int bufferSize) {
        this.base64InputStream = base64InputStream;
        this.buffer = new byte[bufferSize];     // 解码后输出缓冲
        this.chunk = new byte[bufferSize];      // 读取 Base64 输入缓冲
    }

    public Base64DecodingInputStream(InputStream base64InputStream) {
        this(base64InputStream, 4096);
    }

    @Override
    public int read() throws IOException {
        if (pos >= limit) {
            if (!fill()) {
                return -1; // EOF
            }
        }
        return buffer[pos++] & 0xFF;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (b == null) throw new NullPointerException();
        if (off < 0 || len < 0 || len > b.length - off) {
            throw new IndexOutOfBoundsException();
        }
        if (len == 0) return 0;

        if (pos >= limit && !fill()) {
            return -1;
        }

        int available = limit - pos;
        int count = Math.min(available, len);
        System.arraycopy(buffer, pos, b, off, count);
        pos += count;
        return count;
    }

    // 填充解码缓冲区
    private boolean fill() throws IOException {
        pos = 0;
        limit = 0;

        // 读取 Base64 数据块（跳过空白字符）
        int offset = 0;
        while (offset < chunk.length && !eof) {
            int b = base64InputStream.read();
            if (b == -1) {
                eof = true;
                break;
            }
            // 只保留 Base64 有效字符
            if (isBase64Char(b)) {
                chunk[offset++] = (byte) b;
            }
        }

        if (offset == 0 && eof) {
            return false;
        }

        // 补齐为 4 的倍数
        int padLen = offset % 4;
        if (padLen != 0) {
            int newLen = offset + (4 - padLen);
            if (newLen <= chunk.length) {
                for (int i = offset; i < newLen; i++) {
                    chunk[i] = '=';
                }
                offset = newLen;
            } else {
                // 如果缓冲区不够，截断到 4 的倍数
                offset = offset - padLen; // 截断到最近的 4 的倍数
                if (offset == 0) return false;
            }
        }
		
        byte[] input = new byte[offset];
        System.arraycopy(chunk, 0, input, 0, offset);

        // 解码
        try {
            byte[] decoded = Base64.getDecoder().decode(input);
            System.arraycopy(decoded, 0, buffer, 0, decoded.length);
            limit = decoded.length;
        } catch (IllegalArgumentException e) {
            throw new IOException("Invalid Base64 encoding", e);
        }

        return limit > 0;
    }

    private boolean isBase64Char(int b) {
        return (b >= 'A' && b <= 'Z') ||
            (b >= 'a' && b <= 'z') ||
            (b >= '0' && b <= '9') ||
            b == '+' || b == '/' || b == '=';
    }

    @Override
    public void close() throws IOException {
        base64InputStream.close();
    }
}


