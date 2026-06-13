import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

class Log {
	// 日志级别常量
	public static final int DEBUG = 1;
	public static final int INFO = 2;
	public static final int WARN = 3;
	public static final int ERROR = 4;

	// 当前日志级别（默认为 INFO）
	private static int logLevel = INFO;

	// 输出日志信息
	public static void debug(String message) {
		log(DEBUG, message);
	}

	public static void info(String message) {
		log(INFO, message);
	}

	public static void warn(String message) {
		log(WARN, message);
	}

	public static void error(String message) {
		log(ERROR, message);
	}

    public static void error(Exception e) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream pw = new PrintStream(baos);
        e.printStackTrace(pw);
        error(baos.toString());
    }

    public static void error(Throwable e) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream pw = new PrintStream(baos);
        e.printStackTrace(pw);
        error(baos.toString());
    }

	// 重载 error 方法，支持输出异常堆栈信息
	public static void error(String message, Throwable throwable) {
		log(ERROR, message);
		throwable.printStackTrace();
	}

	// 根据日志级别输出日志
	private static void log(int level, String message) {
		if (level >= logLevel) {
			String timestamp = LocalDateTime.now().format(
				DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
			);
			String levelStr = getLevelStr(level);
			String className = getCallerClassName();
			String logMessage = "[" + levelStr + "] [" + timestamp + "] [" + className + "] - " + message;
			System.out.println(logMessage);
		}
	}

	// 将日志级别数字转换为字符串
	private static String getLevelStr(int level) {
		switch (level) {
			case DEBUG: return "DEBUG";
			case INFO: return "INFO";
			case WARN: return "WARN";
			case ERROR: return "ERROR";
			default: return "UNKNOWN";
		}
	}

	// 设置日志级别
	public static void setLogLevel(int level) {
		logLevel = level;
	}

	// 获取调用者的类名（简单类名）
	private static String getCallerClassName() {
		StackTraceElement[] stackTrace = new Exception().getStackTrace();
		String logClassName = Log.class.getName();
		for (StackTraceElement element : stackTrace) {
			String className = element.getClassName();
			if (!className.equals(logClassName)) {
				int lastIndex = className.lastIndexOf('.');
				return lastIndex == -1 ? className : className.substring(lastIndex + 1);
			}
		}
		return "Unknown";
	}
}

