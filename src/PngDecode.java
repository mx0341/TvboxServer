import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

public class PngDecode {
	public static byte[] api(Map<String, String> args, OutputStream out) throws IOException {
		String method = args.containsKey("dm") ?args.get("dm"): null;
		String png = args.containsKey("png") ?args.get("png"): null;
		if (png != null) {
			Tvbox.DecodeMethod dm = Tvbox.DecodeMethod.Done;
			try {
				dm = Tvbox.DecodeMethod.valueOf(method);
			} catch (Exception e) {}
			
			switch (dm) {
				case Lazed:
					return fetchContentL(png);
				case Base64:
					return fetchContentB(png);
			}
		}
		return new byte[0];
	}
	
	public static byte[] fetchContentL(String urlString) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");

        // 设置合理的 User-Agent，避免被服务器拒绝
        connection.setRequestProperty("Referer", urlString);
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");

        // 检查 HTTP 响应码
        int responseCode = connection.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            throw new IOException("Server returned HTTP status: " + responseCode);
        }

        // 读取响应字节数据
        try (InputStream is = connection.getInputStream()) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[1];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                baos.write(new byte[]{(byte)(buffer[0] ^ 136)}, 0, bytesRead);
            }
            return baos.toByteArray();
        }
    }
	
	public static byte[] fetchContentB(String urlString) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");

        // 设置合理的请求头
        connection.setRequestProperty("Referer", urlString);
        connection.setRequestProperty("User-Agent",
                                      "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");

        // 检查响应码
        int responseCode = connection.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            throw new IOException("Server returned HTTP status: " + responseCode);
        }

        // 获取响应输入流（Base64 编码的文本）
        try (InputStream base64InputStream = connection.getInputStream()) {
            // 使用我们自定义的 Base64 解码流包装
            try (Base64DecodingInputStream decodingStream = new Base64DecodingInputStream(base64InputStream)) {
                // 将解码后的数据读取为字节数组
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] buffer = new byte[8192];  // 大缓冲区，高性能
                int bytesRead;
                while ((bytesRead = decodingStream.read(buffer)) != -1) {
                    baos.write(buffer, 0, bytesRead);
                }
                return baos.toByteArray();
            }
        }
    }
}
