import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

public class ABC {
	public final static String Url = GetUrl();
	public final static String TypeUrl = Url + ""; //type page
	public final static String SearchUrl = Url + "";//page wd
	public final static String VodUrl = Url + "";//id
	private static String main = "";//主页缓存

	public static String GetUrl() {
		return "url";
	}

	/*获取主页*/
	public static String GetHome() {  
		return GetPage(Url);
	}
	/*api调用主入口*/
	public static void api(Map<String, String> args, OutputStream out) throws IOException {
		String Output = "";
		if (args.size() == 0) {
			Output = GetHome();
			main = Output;
		} else {
			String ac = args.containsKey("ac") ?args.get("ac"): null;
			String[] ids = args.containsKey("ids") ?args.get("ids").split("%2C"): null;
			String t = args.containsKey("t") ?args.get("t"): null;
			String pg = args.containsKey("pg") ?args.get("pg"): null;
			String wd = args.containsKey("wd") ?args.get("wd"): null;
			String f = args.containsKey("f") ?args.get("f"): null;
			if (ids != null) {
				//Output = main;
				if (ids.length >= 20) {
					Output = main;
				} else {
					Output = String.format(Tvbox.output, GetVod(ids).toString(), "[]");
				}
			}
			if (t != null) { //分类
				String url = String.format(TypeUrl, t, pg);
				Output = GetPage(url);
			} else if (wd != null) { //搜索
				Output = String.format(Tvbox.output, GetSearch(wd), "[]");
			}
		}
		Main.sendResponse(out, 200, "text/plain", Output);
	}
	/*获取分类*/
	public static List<String> GetTypes(String html) {
		return Tvbox.GetTypes(html, "", 1, 2);
	}
	/*获取每个点播*/
	public static List<String> GetVod(String html) {
		return Tvbox.GetVod(html, "", 1, 2, 3, Tvbox.DecodeMethod.Done);
	}
	/*根据id获取点播*/
	public static List<String> GetVod(String[] ids) {
		List<String> result = new ArrayList<String>();
		for (String id : ids) {
			String url = String.format(VodUrl, id);
			String html = GetWeb(url);
			String regex = "<title>(.*?)</title>";
			Pattern pattern = Pattern.compile(regex);
			Matcher matcher = pattern.matcher(html);
			while (matcher.find()) {
				result.add(String.format(Tvbox.vod, id, matcher.group(1), "", url));
				break;
			}
		}
		return result;
	}
	/*获取页面数据*/
	public static String GetPage(String url) {
		String result = Tvbox.output;
		String html = GetWeb(url);
		if (html.isEmpty() || html.length() == 0) {
			return null;
		}
		String Types = GetTypes(html).toString();
		String Vods = GetVod(html).toString();
		result = String.format(Tvbox.output, Vods, Types);
		return result;
	}
	/*搜索*/
	public static List<String> GetSearch(String wd) {
		List<String> result = new ArrayList<String>();
		//System.out.println(String.format(SearchUrl,1,wd));
		String html = GetWeb(String.format(SearchUrl, 1, wd));
		String regex = "<span class=\"num\">(.*?)</span>";
		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(html);
		int limit = 20;
		while (matcher.find()) {
			try {
				int max = Integer.parseInt(matcher.group(1).split("/")[1]);
				for (int pg = 1;pg <= max && pg <= limit;pg++) {
					String shtml = GetWeb(String.format(SearchUrl, pg, wd));
					result.addAll(GetVod(shtml));
				}
			} catch (Exception e) {
				break;
			}
			break;
		}
		return result;
	}
	/*爬虫*/
	public static String GetWeb(String urlString) {
        StringBuilder content = new StringBuilder();
        HttpURLConnection conn = null;
        BufferedReader reader = null;
        try {
            // 创建 URL 对象并建立连接
            URL url = new URL(urlString);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(10000); // 5秒连接超时
            conn.setReadTimeout(10000);   // 5秒读取超时
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");
            // 检查返回状态码
            int status = conn.getResponseCode();
            if (status != HttpURLConnection.HTTP_OK) {
                throw new RuntimeException("请求失败，状态码: " + status);
            }
            // 获取原始输入流
            InputStream inputStream = conn.getInputStream();
            // 获取压缩类型，并按逆序解压（符合 RFC 7230）
            String contentEncoding = conn.getContentEncoding();
            if (contentEncoding != null) {
                String[] encodings = contentEncoding.split(",");
                for (int i = encodings.length - 1; i >= 0; i--) {
                    String enc = encodings[i].trim();
                    if (enc.equalsIgnoreCase("gzip")) {
                        inputStream = new GZIPInputStream(inputStream);
                    } else if (enc.equalsIgnoreCase("deflate")) {
                        inputStream = new InflaterInputStream(inputStream, new Inflater(true), 8192);
                    } else {
                        // 忽略未知的压缩类型
                        throw new IOException("不支持的编码方式: " + enc);
                    }
                }
            }
            // 按 UTF-8 编码读取解压后的内容
            reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        } catch (Exception e) {
        	Log.error(e.toString());
        } finally {
            // 确保资源释放
            if (conn != null) {
                conn.disconnect();
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    Log.error(e.toString());
                }
            }
        }
        return content.toString();
    }

	public static String GetWebJs(String urlString) {
        System.out.println(urlString);
		String html = GetWeb(urlString);
		if (html.length() != 0) {
			String regex = "<!DOCTYPE html><script>document\\.write\\(decodeURIComponent\\(\"(.*?)\"\\)\\)</script>";
			Pattern pattern = Pattern.compile(regex);
			Matcher matcher = pattern.matcher(html);
			while (matcher.find()) {
				try {
					html = HtmlDecoder.htmlDecode(URLDecoder.decode(matcher.group(1), "UTF-8"));
					//System.out.println(html);
				} catch (Exception e) {
					Log.error(e.toString());
				}
				break;
			}
		}
        return html;
    }
}
