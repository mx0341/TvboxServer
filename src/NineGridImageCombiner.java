import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;

public class NineGridImageCombiner {
	public static byte[] HJ(InputStream[] is) {
		byte[] resultb = new byte[]{};
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		// 读取输入图片
		List<BufferedImage> images = new ArrayList<>();
		for (int i = 0; i < is.length && i < 9; i++) {
			try {
				BufferedImage img = ImageIO.read(is[i]);
				images.add(img);
			} catch (IOException e) {
				Log.error("无法读取图片");
			}
		}

		if (images.isEmpty()) {
			Log.error("没有有效的图片文件。");
			return resultb;
		}

		// 拼接九宫格
		BufferedImage result = createNineGridImage(images);
		if (result != null) {
			// 保存结果图像
			try {
				ImageIO.write(result, "png", outputStream);
				resultb = outputStream.toByteArray();
				Log.info("合集拼接成功");
			} catch (IOException e) {
				Log.error("出错: " + e.getMessage());
			}
		}
		return resultb;
	}

	/**
	 * 拼接九宫格图像
	 *
	 * @param images 原始输入图片列表
	 * @return 拼接后的图像
	 */
	public static BufferedImage createNineGridImage(List<BufferedImage> images) {
		// 基准大小为第一张图片
		BufferedImage first = images.get(0);
		int width = first.getWidth();
		int height = first.getHeight();

		// 制作九宫格图片列表（自动循环）
		List<BufferedImage> gridImages = new ArrayList<>();
		for (int i = 0; i < 9; i++) {
			int idx = i % images.size();
			BufferedImage scaled = resize(images.get(idx), width, height);
			gridImages.add(scaled);
		}

		// 创建拼接后的大图
		int columns = 3;
		int rows = 3;
		BufferedImage combined = new BufferedImage(columns * width, rows * height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = combined.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

		// 绘制到对应坐标
		for (int i = 0; i < gridImages.size(); i++) {
			Log.debug("拼接第 " + (i + 1) + " 张图片");
			int x = (i % columns) * width;
			int y = (i / columns) * height;
			g.drawImage(gridImages.get(i), x, y, null);
		}

		g.dispose();
		return combined;
	}

	/**
	 * 图像缩放工具方法
	 *
	 * @param original 原始图像
	 * @param width    目标宽度
	 * @param height   目标高度
	 * @return 缩放后的图像
	 */
	public static BufferedImage resize(BufferedImage original, int width, int height) {
		BufferedImage resized = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = resized.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		g.drawImage(original, 0, 0, width, height, null);
		g.dispose();
		return resized;
	}
}

