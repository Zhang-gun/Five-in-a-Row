import java.io.*;
import java.util.Properties;

/**
 * 数据持久化管理类 (RecordManager)
 * 本类负责对接操作系统底层文件系统，实现关键数据的本地留存与读取。
 * 采用了 Java 核心库中的 Properties 类，将比分信息以键值对 (Key-Value) 的形式存入 properties 配置文件。
 */
public class RecordManager {
    // 静态常量定义存储文件的相对路径及名称
    private static final String RECORD_FILE = "score_record.properties";

    /**
     * 系统启动时调用此方法，尝试从本地文件系统中加载历史对战比分。
     * 
     * @return 长度为2的整型数组。索引0为黑方胜局数，索引1为白方胜局数。
     */
    public static int[] loadScores() {
        int[] scores = new int[]{0, 0}; // 赋予初始默认比分 0:0
        File file = new File(RECORD_FILE);
        
        // 校验文件有效性，若用户为首次运行且未产生过数据，则直接跳过读取步骤
        if (file.exists()) {
            // 使用 Java 7+ 引入的 try-with-resources 语法结构包裹 FileInputStream 流
            // 其优势在于代码块执行完毕或抛出异常时，JVM 会自动确保底层流被 close()，有效防止系统文件句柄泄漏
            try (InputStream in = new FileInputStream(file)) {
                Properties props = new Properties();
                // 解析输入流中的字符流文本，转化为内存中的键值对集合
                props.load(in); 
                
                // 提取对应的键值，如果该键不存在，则返回默认字符串 "0"，随后强制转换为 Integer 类型
                scores[0] = Integer.parseInt(props.getProperty("blackWins", "0"));
                scores[1] = Integer.parseInt(props.getProperty("whiteWins", "0"));
            } catch (Exception e) {
                // 捕获可能出现的 IOException 并在控制台打印堆栈，防止由于单一文件错误导致主程序崩溃
                e.printStackTrace();
            }
        }
        return scores;
    }

    /**
     * 当任意一方获胜或用户主动点击“清空比分”时调用此方法。
     * 负责将内存中最新的胜局数据序列化写入本地文件，覆盖原有数据。
     * 
     * @param blackWins 黑方累积胜局数
     * @param whiteWins 白方累积胜局数
     */
    public static void saveScores(int blackWins, int whiteWins) {
        // 同样采用 try-with-resources 安全地开启文件输出流
        try (OutputStream out = new FileOutputStream(RECORD_FILE)) {
            Properties props = new Properties();
            
            // 将基本数据类型转换为 String 并推入 Properties 集合
            props.setProperty("blackWins", String.valueOf(blackWins));
            props.setProperty("whiteWins", String.valueOf(whiteWins));
            
            // 调用 store 方法，系统会执行真正的磁盘 I/O 写入操作
            // 第二个参数为插入到文件头部的英文注释说明，增加配置文件的可读性
            props.store(out, "Gomoku Game Scores Record");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
