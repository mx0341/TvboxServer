import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.LinkedHashMap;
import java.util.Map;

public class Config {
	public static int PORT = 8888;
	public static String WEB_ROOT = "/public";
	public static final String ConfigPath = "config.ini";
	public static Map<String,Object> config = new LinkedHashMap<String,Object>();

	static{
		try {
			InputStream inputStream = Main.class.getResourceAsStream(ConfigPath);
			if (inputStream == null) {
				Log.error("读取配置文件 " + ConfigPath + " 出错");
			} else {
				BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
				String line;
				while ((line = br.readLine()) != null) {
					String[] kv = line.split("=");
					config.put(kv[0], kv[1]);
				}
				PORT = config.containsKey("PORT") ?Integer.parseInt(config.get("PORT").toString()): PORT;
				WEB_ROOT = config.containsKey("WEB_ROOT") ?config.get("WEB_ROOT").toString(): WEB_ROOT;
				if (config.containsKey("LogLevel")) Log.setLogLevel(Integer.parseInt(config.get("LogLevel").toString()));
				Log.info("配置文件读取完成");
				String v0 = String.format("===== %s 配置 =====", ConfigPath);
				Log.info(v0);
				for (Map.Entry<String,Object> entry : config.entrySet()) {
					Log.info(String.format("%s = %s", entry.getKey(), String.valueOf(entry.getValue())));
				}
				Log.info("=".repeat(v0.getBytes("gbk").length));
			}
		} catch (Exception e) {
			Log.error("读取配置文件 " + ConfigPath + " 出错");
			Log.error(e.toString());
		}
	}
}
