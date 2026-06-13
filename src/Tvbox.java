import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Tvbox {
	public final static String output = "{\"code\":1,\"msg\":\"数据列表\",\"page\":1,\"pagecount\":2112,\"limit\":\"20\",\"total\":42226,\"list\":%s,\"class\":%s}";
	public final static String vod = "{\"vod_id\":%s,\"vod_name\":\"%s\",\"vod_pic\":\"%s\",\"vod_play_url\":\"%s\",\"vod_play_from\":\"m3u8\"}";
	public final static String type = "{\"type_id\":%s,\"type_pid\":%s,\"type_name\":\"%s\"}";

	public static List<String> GetTypes(String html, String regex, int... args) {
		List<String> result = new ArrayList<String>();
		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(html);
		while (matcher.find()) {
			result.add(String.format(Tvbox.type, matcher.group(args[0]), "0", matcher.group(args[1])).trim()); //id pid name
        }
		return result;
	}

    public static List<String> GetVod(String html, String regex, int id, int name, int pic, DecodeMethod dm) {
        List<String> result = new ArrayList<String>();
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(html);
        while (matcher.find()) {
            result.add(String.format(Tvbox.vod, matcher.group(id), matcher.group(name), decodePic(matcher.group(pic), dm), ""));//id name pic playurl
        }
        return result;
    }

    public static String decodePic(String url, DecodeMethod dm) {
		if (dm == null || dm == DecodeMethod.Done) {
			return url;
		}
		return "http://127.0.0.1:8888/pd?png=" + url + "&m=" + dm.toString();
    }

    public static enum DecodeMethod {
        Done,
        Lazed,
        Base64
    }
}
