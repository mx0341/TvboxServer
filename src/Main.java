import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.net.URLDecoder;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

class Main {
    public static final int PORT = 8888;
    public static final String WEB_ROOT = "/public";
	public static String ip = "127.0.0.1";
	public static String MainUrl = "http://" + ip + ":" + PORT;
	public static final String[] apis = new String[]{"abc"};
	public static final String ReplaceString = "※";
	//private static final Map<String,byte[]> cache = new LinkedHashMap<String,byte[]>();
	private static String ajson = "";
	private static boolean abc = true;

    private static final int THREAD_POOL_SIZE = 100;
    private static final ExecutorService executorService = 
    Executors.newFixedThreadPool(THREAD_POOL_SIZE);

    static ServerSocket serverSocket = null;

	public static byte[] HJ;

	static {
		try {
			ip = getLocalHostLANAddress().getHostAddress();
			if (ip.contains(".")) {
				MainUrl = "http://" + ip + ":" + PORT;
			}
			List<String> l = new ArrayList<String>();
			Map<String,Object> site = new LinkedHashMap<String,Object>();
			Map<String,Object> sites = new LinkedHashMap<String,Object>();
			int i = 0;
			for (String api : apis) {
				site.put("\"key\"", "\"" + api + "\"");
				site.put("\"name\"", "\"" + String.format("%02d", i) + "\"");
				site.put("\"type\"", 1);
				site.put("\"api\"", "\"" + ReplaceString + "/" + api + "\"");
				site.put("\"searchable\"", 1);
				site.put("\"quickSearch\"", 1);
				site.put("\"filterable\"", 1);
				l.add(site.toString());
				site.clear();
				i++;
			}
			l.add("{\"key\": \"vod\",\"name\": \"test\",\"type\": 1,\"api\": \"简单api事例链接\",\"searchable\": 1,\"quickSearch\": 1,\"filterable\": 1}");
			sites.put("\"sites\"", l);
			ajson = sites.toString();
		} catch (UnknownHostException e) {
			Log.error(e.toString());
		}
	}

    public static void main(String[] args) {
        try {
            serverSocket = new ServerSocket(PORT);
            Log.info("服务器已启动，监听端口：" + PORT + "，访问链接：" + GetMainUrl());

            Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
                                                         @Override
                                                         public void run() {
                                                             shutdown(serverSocket);
                                                         }
                                                     }
                                                 ));

            // 主线程循环接收客户端连接
            while (!serverSocket.isClosed()) {
                try {
                    Socket client = serverSocket.accept();
                    Log.info("客户端已连接: " + client.getRemoteSocketAddress());

                    // 提交到线程池处理
                    executorService.submit(new ClientHandler(client));
                } catch (IOException e) {
                    if (!serverSocket.isClosed()) {
                        Log.error("接收客户端连接时发生错误: " + e.getMessage());
                    }
                    // 如果 serverSocket 被关闭，accept() 会抛异常，正常退出
                }
            }
        } catch (IOException e) {
            Log.error("无法启动服务器: " + e.getMessage());
        } finally {
            // 关闭线程池和 serverSocket
            shutdown(serverSocket);
        }
    }

    private static class ClientHandler implements Runnable {
        private final Socket clientSocket;

        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
        }

        @Override
        public void run() {
            handleClient(clientSocket);
        }
    }

    private static void shutdown(ServerSocket serverSocket) {
        Log.info("正在关闭服务器...");

        // 关闭 ServerSocket
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                Log.error("关闭 ServerSocket 失败: " + e.getMessage());
            }
        }

        // 关闭线程池
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }

        Log.info("服务器已关闭。");
    }

    private static void handleClient(Socket client) {
        try {
			BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
			OutputStream out = client.getOutputStream();
            String requestLine = in.readLine();
			Log.info(requestLine);
            if (requestLine == null || !requestLine.startsWith("GET")) {
                sendResponse(out, 405, "Method Not Allowed", "仅支持 GET 请求");
                return;
            }

            // 解析路径和参数
            String[] parts = requestLine.split(" ");
            String fullPath = parts[1];
            String path = fullPath.split("\\?")[0];
            Map<String, String> queryParams = parseQueryParams(fullPath);

            // 特殊参数处理示例：?name=xxx 返回 Hello, xxx
            if (path.equals("/hello") && queryParams.containsKey("name")) {
                String name = queryParams.get("name");
                sendResponse(out, 200, "text/plain", "Hello, " + name + "!");
                return;
            }
			Map<String, BiConsumer<Map<String, String>, OutputStream>> lightApis = new HashMap<>();
			
			//用lambda表达式会更简洁一点
			lightApis.put("/abc", new BiConsumer<Map<String, String>, OutputStream>() {
					@Override
					public void accept(Map<String, String> params, OutputStream out) {
						try {
							ABC.api(params, out);
						} catch (IOException e) {
							Log.error(e.toString());
						}
					}
				});
				
			lightApis.put("/pd", new BiConsumer<Map<String, String>, OutputStream>() {
					@Override
					public void accept(Map<String, String> params, OutputStream out) {
						try {
							PngDecode.api(params, out);
						} catch (IOException e) {
							Log.error(e.toString());
						}
					}
				});
			if (path.equals("/a.json")) {
				sendResponse(out, 200, "text/plain", ajson.replaceAll(ReplaceString,GetMainUrl()));
				return;
			}
			BiConsumer handler = lightApis.get(path);
			if (handler != null) {
				if (!abc) {
					light.api(queryParams, out);
				} else {
					handler.accept(queryParams, out);
				}
				return;
			}
			InputStream inputStream = Main.class.getResourceAsStream(WEB_ROOT + path);
			if (inputStream == null) {
				sendResponse(out, 404, "text/plain", "404 文件未找到");
				return;
			}
			byte[] fileData = inputStream.readAllBytes();
			sendFileResponse(out, 200, detectMimeType(WEB_ROOT + path), fileData);
			return;

        } catch (IOException e) {
            Log.error(e.toString());
        }
    }

    private static Map<String, String> parseQueryParams(String fullPath) throws UnsupportedEncodingException {
        Map<String, String> params = new HashMap<>();
        if (fullPath.contains("?")) {
            String queryString = fullPath.split("\\?", 2)[1];
            for (String pair : queryString.split("&")) {
                String[] kv = pair.split("=", 2);
                if (kv.length == 2) {
					params.put(kv[0], kv[1]);
                    //params.put(URLDecoder.decode(kv[0], "UTF-8"), URLDecoder.decode(kv[1], "UTF-8"));
                }
            }
        }
        return params;
    }

    public static void sendResponse(OutputStream out, int statusCode, String contentType, String content) throws IOException {
		//System.out.println(content);
        String header = "HTTP/1.1 " + statusCode + " OK\r\n" +
			"Content-Type: " + contentType + "; charset=UTF-8\r\n" +
			"Content-Length: " + content.getBytes("UTF-8").length + "\r\n" +
			"Connection: close\r\n\r\n";
        out.write(header.getBytes("UTF-8"));
        out.write(content.getBytes("UTF-8"));
        out.flush();
    }

    public static void sendFileResponse(OutputStream out, int statusCode, String contentType, byte[] content) throws IOException {
        String header = "HTTP/1.1 " + statusCode + " OK\r\n" +
			"Content-Type: " + (contentType != null ? contentType : "application/octet-stream") + "\r\n" +
			"Content-Length: " + content.length + "\r\n" +
			"Connection: close\r\n\r\n";
        out.write(header.getBytes("UTF-8"));
        out.write(content);
        out.flush();
    }

	public static String d(String e) throws UnsupportedEncodingException {
		return new String(Base64.getDecoder().decode(e), "UTF-8");
	}
	
	public static String GetMainUrl() throws UnknownHostException {
		String _ip = "127.0.0.1";
		String _MainUrl = "http://" + _ip + ":" + PORT;
		_ip = getLocalHostLANAddress().getHostAddress();
		if (_ip.contains(".")) {
			_MainUrl = "http://" + _ip + ":" + PORT;
		}
		return _MainUrl;
	}

	public static String detectMimeType(String resourceName) {
        // 2. 通过内容检测MIME类型
        InputStream inputStream = Main.class.getResourceAsStream(resourceName);
        if (inputStream == null) {
            return "application/octet-stream";
        }

        try {
			BufferedInputStream bufferedStream = new BufferedInputStream(inputStream);
            bufferedStream.mark(28); // 标记位置
            byte[] header = new byte[28];
            int bytesRead = bufferedStream.read(header, 0, 28);
            if (bytesRead == -1) {
                return "application/octet-stream";
            }

            String mimeType = getMimeTypeFromMagicNumber(header);
            bufferedStream.reset(); // 回退流位置
            return mimeType;
        } catch (IOException e) {
            Log.error(e.toString());
            return "application/octet-stream";
        }
    }

    private static String getMimeTypeFromMagicNumber(byte[] header) {
        if (startsWith(header, new byte[] {0x25, 0x50, 0x44, 0x46, 0x2D})) {
            return "application/pdf";
        }
        if (startsWith(header, new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF})) {
            return "application/jpeg";
        }
        if (startsWith(header, new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A})) {
            return "image/png";
        }
        if (startsWith(header, new byte[] {0x47, 0x49, 0x46, 0x38})) {
            return "image/gif";
        }
        if (startsWith(header, new byte[] {0x50, 0x4B, 0x03, 0x04})) {
            return "application/zip";
        }
        if (startsWith(header, "<?xml".getBytes()) || startsWith(header, "<?XML".getBytes())) {
            return "application/xml";
        }

        return "application/octet-stream";
    }

    private static boolean startsWith(byte[] header, byte[] magicNumber) {
        if (header.length < magicNumber.length) {
            return false;
        }
        for (int i = 0; i < magicNumber.length; i++) {
            if (header[i] != magicNumber[i]) {
                return false;
            }
        }
        return true;
    }

	public static InetAddress getLocalHostLANAddress() throws UnknownHostException {
        try {
            InetAddress candidateAddress = null;
            // 遍历所有的网络接口
            for (Enumeration ifaces = NetworkInterface.getNetworkInterfaces(); ifaces.hasMoreElements();) {
                NetworkInterface iface = (NetworkInterface) ifaces.nextElement();
                // 在所有的接口下再遍历IP
                for (Enumeration inetAddrs = iface.getInetAddresses(); inetAddrs.hasMoreElements();) {
                    InetAddress inetAddr = (InetAddress) inetAddrs.nextElement();
                    if (!inetAddr.isLoopbackAddress()) {// 排除loopback类型地址
                        if (inetAddr.isSiteLocalAddress()) {
                            // 如果是site-local地址，就是它了
                            return inetAddr;
                        } else if (candidateAddress == null) {
                            // site-local类型的地址未被发现，先记录候选地址
                            candidateAddress = inetAddr;
                        }
                    }
                }
            }
            if (candidateAddress != null) {
                return candidateAddress;
            }
            // 如果没有发现 non-loopback地址.只能用最次选的方案
            InetAddress jdkSuppliedAddress = InetAddress.getLocalHost();
            if (jdkSuppliedAddress == null) {
                throw new UnknownHostException("The JDK InetAddress.getLocalHost() method unexpectedly returned null.");
            }
            return jdkSuppliedAddress;
        } catch (Exception e) {
            UnknownHostException unknownHostException = new UnknownHostException(
				"Failed to determine LAN address: " + e);
            unknownHostException.initCause(e);
            throw unknownHostException;
        }
    }
}
