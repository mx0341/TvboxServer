import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class light {
	private static String main = "";//主页缓存
	public static String MainUrl = Main.MainUrl;
	public static Map<String,String> Vods;
	public static Map<String,String> Types;
	private static String HJurl;
	private static final String ReplaceString = Main.ReplaceString;

	static{ //初始化列表
		try {
			Log.info("初始化 Light 列表");
			Map<String,String> v1 = new LinkedHashMap<String,String>();
			Vods = new LinkedHashMap<String,String>();
			Types = new LinkedHashMap<String,String>();
			List<InputStream> lis = new ArrayList<InputStream>();
			InputStream inputStream = Main.class.getResourceAsStream(Main.WEB_ROOT + "/videos/light.txt");
			if (inputStream == null) {
				Log.error("Light列表无法加载");
			}
			BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
			String line;
			String hj = "";
			String v2 = "";
			while ((line = br.readLine()) != null) {
				if (line.startsWith("#"))continue;//id,name,imgurl,videourl 开头#为注释
				String[] v0 = line.split(",");
				if (v1.containsKey(v0[0]) || Types.containsKey(v0[0])) {
					Log.warn("出现 id 重复 " + v0[1] + " 的相关内容不会加载");
				}
				if (v0[0].equals("hj")) {
					HJurl = v0[2];
					v2 = String.format(Tvbox.vod, v0[0], v0[1], (v0[2].startsWith("/") ?ReplaceString: "") + v0[2], "#");
					continue;
				}
				v1.put(v0[0], String.format(Tvbox.vod, v0[0], v0[1], (v0[2].startsWith("/") ?ReplaceString: "") + v0[2], Repeat((v0[3].startsWith("/") ?ReplaceString: "") + v0[3], 20)));
				Types.put(v0[0], String.format(Tvbox.type, v0[0], v0[0], v0[1]));
				hj += String.format("%s$%s#", v0[1], (v0[3].startsWith("/") ?ReplaceString: "") + v0[3]);
				lis.add(Main.class.getResourceAsStream(Main.WEB_ROOT + v0[2]));
				Log.info("载入 id : " + v0[0] + " " + v0[1]);
			}
			Vods.put("hj", v2.replace("#", hj.substring(0, hj.length() - 1)));
			Vods.putAll(v1);
			Log.info("Light 列表初始化完成");
			Log.info("拼接 合集9宫格");
			Main.HJ = NineGridImageCombiner.HJ(lis.toArray(new InputStream[lis.size()]));
			if (Main.HJ.length == 0) {
				Log.error("合集9宫格拼接失败");
			}
		} catch (IOException e) {
			Log.error("Light列表无法加载");
			Log.error(e.toString());
		}
	}

	public static String GetHome() {  
		return GetPage();
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
				Output = GetPage(new String[]{t});
			} else if (wd != null) { //搜索
				Output = String.format(Tvbox.output, GetSearch(wd), "[]");
			}
		}
		if (Output.lastIndexOf(ReplaceString) != -1){
			Output = Output.replaceAll(ReplaceString,Main.GetMainUrl());
		}
		Main.sendResponse(out, 200, "text/plain", Output);
	}
	/*获取分类*/
	public static List<String> GetTypes() {
		List<String> result = new ArrayList<String>();
		for (Map.Entry<String,String> entry : Types.entrySet()) {
			result.add(entry.getValue());
		}
		return result;
	}
	/*获取每个点播*/
	public static List<String> GetVod() {
		return GetVod(Vods.keySet().toArray(new String[Vods.keySet().size()]));
	}
	/*根据id获取点播*/
	public static List<String> GetVod(String[] ids) {
		List<String> result = new ArrayList<String>();
		for (String id : ids) {
			if (Vods.containsKey(id)) {
				result.add(Vods.get(id));
			}
		}
		return result;
	}
	/*获取页面数据*/
	public static String GetPage() {
		return GetPage(Vods.keySet().toArray(new String[Vods.keySet().size()]));
	}
	public static String GetPage(String[] ids) {
		String result = Tvbox.output;
		String Types = GetTypes().toString();
		String Vods = GetVod(ids).toString();
		result = String.format(Tvbox.output, Vods, Types);
		return result;
	}
	/*搜索*/
	public static List<String> GetSearch(String wd) throws UnsupportedEncodingException {
		List<String> result = new ArrayList<String>();
		for (String vod : GetVod()) {
			if (vod.split(",")[1].split(":")[1].contains(URLDecoder.decode(wd, "UTF-8"))) {
				result.add(vod);
			}
		}
		return result;
	}
	public static String Repeat(String url, int count) {
		String play = "";
		for (int i = 1;i <= count;i++) {
			play += String.format("%s$%s#", i + "", url);
		}
		return play.substring(0, play.length() - 1);
	}
}
