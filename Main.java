import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

/**
 * 灰度阈值分割和图像处理完整实现
 * 包含所有必要的功能和演示
 */
public class Main {
    
    private final BufferedImage image;
    private final int[] histogram;
    
    public Main(String imagePath) throws IOException {
        this.image = ImageIO.read(new File(imagePath));
        this.histogram = calculateHistogram();
    }
    
    /**
     * 计算灰度直方图
     */
    private int[] calculateHistogram() {
        int[] hist = new int[256];
        
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int rgb = image.getRGB(x, y);
                int gray = (rgb >> 16) & 0xFF;
                hist[gray]++;
            }
        }
        return hist;
    }
    
    /**
     * 打印直方图信息
     */
    public void printHistogram() {
        System.out.println("=== 灰度直方图 ===");
        int count = 0;
        for (int i = 0; i < 256; i++) {
            if (histogram[i] > 0) {
                System.out.printf("灰度值 %3d: %d 像素%n", i, histogram[i]);
                count++;
                if (count > 20) {
                    System.out.println("...(其他数据已省略)");
                    break;
                }
            }
        }
    }
    
    /**
     * Otsu自适应阈值法
     */
    public int otsuThreshold() {
        int totalPixels = image.getWidth() * image.getHeight();
        
        int sumIntensity = 0;
        for (int i = 0; i < 256; i++) {
            sumIntensity += i * histogram[i];
        }
        
        double maxVariance = 0;
        int optimalThreshold = 0;
        
        int weightBackground = 0;
        int sumBackground = 0;
        
        for (int t = 0; t < 256; t++) {
            weightBackground += histogram[t];
            sumBackground += t * histogram[t];
            
            if (weightBackground == 0) continue;
            
            int weightForeground = totalPixels - weightBackground;
            if (weightForeground == 0) break;
            
            double meanBackground = (double) sumBackground / weightBackground;
            double meanForeground = (double) (sumIntensity - sumBackground) / weightForeground;
            
            double variance = (double) weightBackground * weightForeground * 
                             Math.pow(meanBackground - meanForeground, 2);
            
            if (variance > maxVariance) {
                maxVariance = variance;
                optimalThreshold = t;
            }
        }
        
        System.out.printf("✓ Otsu最优阈值: %d, 方差: %.2f%n", optimalThreshold, maxVariance);
        return optimalThreshold;
    }
    
    /**
     * 单阈值二值化
     */
    public BufferedImage thresholdImage(int threshold) {
        BufferedImage binary = new BufferedImage(
            image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
        
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int rgb = image.getRGB(x, y);
                int gray = (rgb >> 16) & 0xFF;
                
                int newColor = (gray >= threshold) ? 0xFFFFFF : 0x000000;
                binary.setRGB(x, y, newColor);
            }
        }
        return binary;
    }
    
    /**
     * 多重阈值分割
     */
    public BufferedImage multiThresholdImage(int[] thresholds) {
        BufferedImage multiLevel = new BufferedImage(
            image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
        
        int numLevels = thresholds.length + 1;
        int[] levelValues = new int[numLevels];
        for (int i = 0; i < numLevels; i++) {
            levelValues[i] = (255 * i) / (numLevels - 1);
        }
        
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int rgb = image.getRGB(x, y);
                int gray = (rgb >> 16) & 0xFF;
                
                int level = 0;
                for (int t : thresholds) {
                    if (gray >= t) level++;
                    else break;
                }
                
                int newGray = levelValues[level];
                int newColor = (newGray << 16) | (newGray << 8) | newGray;
                multiLevel.setRGB(x, y, newColor);
            }
        }
        return multiLevel;
    }
    
    /**
     * 动差法阈值
     */
    public int momentBasedThreshold() {
        double m0 = 0, m1 = 0;
        
        for (int i = 0; i < 256; i++) {
            m0 += histogram[i];
            m1 += i * histogram[i];
        }
        
        double mean = m1 / m0;
        int threshold = (int) mean;
        double minValue = Double.MAX_VALUE;
        
        for (int t = 0; t < 256; t++) {
            if (histogram[t] < minValue) {
                threshold = t;
                minValue = histogram[t];
            }
        }
        
        System.out.printf("✓ 动差法阈值: %d, 平均灰度: %.2f%n", threshold, mean);
        return threshold;
    }
    
    /**
     * 提取前景（去背）
     */
    public BufferedImage extractForeground(int threshold) {
        BufferedImage foreground = new BufferedImage(
            image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
        
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int rgb = image.getRGB(x, y);
                int gray = (rgb >> 16) & 0xFF;
                
                if (gray >= threshold) {
                    foreground.setRGB(x, y, rgb | 0xFF000000);
                } else {
                    foreground.setRGB(x, y, 0);
                }
            }
        }
        return foreground;
    }
    
    /**
     * 保存图像
     */
    public void saveImage(BufferedImage img, String outputPath) throws IOException {
        new File(new File(outputPath).getParent()).mkdirs();
        String format = outputPath.endsWith(".png") ? "png" : "jpg";
        ImageIO.write(img, format, new File(outputPath));
        System.out.println("  ✓ 已保存: " + outputPath);
    }
    
    public static void main(String[] args) {
        System.out.println("\n========== 灰度阈值分割 - 完整演示 ==========\n");
        
        String inputPath = "input.jpg";
        String outputDir = "output/";
        
        try {
            Main processor = new Main(inputPath);
            
            System.out.println("【步骤1】分析直方图");
            processor.printHistogram();
            
            System.out.println("\n【步骤2】计算最优阈值");
            int otsuT = processor.otsuThreshold();
            processor.momentBasedThreshold();
            
            System.out.println("\n【步骤3】执行图像处理");
            
            System.out.println("\n1. 单阈值二值化");
            BufferedImage binary = processor.thresholdImage(otsuT);
            processor.saveImage(binary, outputDir + "01_binary_otsu.jpg");
            
            System.out.println("\n2. 多重阈值分割（3层）");
            int[] thresholds = {otsuT/2, otsuT, otsuT + 64};
            BufferedImage multiImage = processor.multiThresholdImage(thresholds);
            processor.saveImage(multiImage, outputDir + "02_multi_threshold.jpg");
            
            System.out.println("\n3. 前景提取（去背效果）");
            BufferedImage foreground = processor.extractForeground(otsuT);
            processor.saveImage(foreground, outputDir + "03_foreground.png");
            
            System.out.println("\n========== ✓ 所有处理完成! ==========\n");
            
        } catch (IOException e) {
            System.err.println("❌ 错误: 找不到 input.jpg 文件");
            System.out.println("\n请将图像文件放在程序同目录，命名为 input.jpg\n");
            System.out.println("支持的格式: jpg, png, bmp, gif 等\n");
        }
    }
}
