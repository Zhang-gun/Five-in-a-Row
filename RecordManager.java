import java.io.*;
import java.util.Properties;

/**
 * 历史数据存储管理器
 * 我们没有用复杂的数据库，因为作为课设来说太臃肿了。
 * 而是采用了 Java 自带的 Properties 类，它专门用来读写键值对格式的配置文件。
 */
public class RecordManager {

    // 保存比分的文件名，会生成在项目根目录
    private static final String RECORD_FILE_NAME = "score_record.properties";

    /**
     * 读取本地保存的比分
     */
    public static int[] loadScores() {
        int[] scores = new int[2];
        scores[0] = 0; // 黑方
        scores[1] = 0; // 白方
        
        File file = new File(RECORD_FILE_NAME);
        
        // 容错处理：如果文件不存在（第一次玩），就直接返回 0，千万别硬读，不然会报错。
        if (!file.exists()) {
            return scores;
        }
        
        InputStream inStream = null;
        try {
            inStream = new FileInputStream(file);
            
            // Properties 类就像一个特殊的 Map，
            // 它的好处是可以直接调用 load() 方法，把文件里通过等号连起来的配置解析进去。
            Properties properties = new Properties();
            properties.load(inStream);
            
            // 用 key 获取 value，第二个参数 "0" 的意思是，如果找不到这个 key，就默认返回 "0"
            String blackStr = properties.getProperty("blackWins", "0");
            String whiteStr = properties.getProperty("whiteWins", "0");
            
            // 字符串转成 int 数字
            scores[0] = Integer.parseInt(blackStr);
            scores[1] = Integer.parseInt(whiteStr);
            
        } catch (FileNotFoundException e) {
            System.out.println("找不到配置文件");
            e.printStackTrace();
        } catch (IOException e) {
            System.out.println("读取配置文件时出错");
            e.printStackTrace();
        } catch (NumberFormatException e) {
            // 防御性编程：万一玩家无聊，自己打开文件把数字改成了字母 "abc"，
            // 转换数字的时候就会报错，我们捕捉到这个错误就不管它，返回默认的 0 分。
            System.out.println("文件里的数字被改坏了");
            e.printStackTrace();
        } finally {
            // 【知识点】一定要在 finally 里面关闭流！
            // 不然的话，无论代码正常跑完还是报错，这个文件都会被一直锁住，
            // 别的程序想访问都访问不了，这叫内存泄漏。
            if (inStream != null) {
                try {
                    inStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        
        return scores;
    }

    /**
     * 把新的比分保存进文件里
     */
    public static void saveScores(int blackWins, int whiteWins) {
        OutputStream outStream = null;
        
        try {
            // FileOutputStream 会自动帮我们创建一个文件，如果是覆盖写入就不加 true
            outStream = new FileOutputStream(RECORD_FILE_NAME);
            Properties properties = new Properties();
            
            // Properties 存的必须是 String，所以要把数字转成字符串
            properties.setProperty("blackWins", String.valueOf(blackWins));
            properties.setProperty("whiteWins", String.valueOf(whiteWins));
            
            // store() 是专门往外写文件的接口，第二个参数可以顺手在文件第一行加句英文注释
            properties.store(outStream, "This file saves the Gomoku scores.");
            
        } catch (FileNotFoundException e) {
            System.out.println("无法创建写入文件");
            e.printStackTrace();
        } catch (IOException e) {
            System.out.println("保存配置文件时出错");
            e.printStackTrace();
        } finally {
            // 同样，最后一定要关掉流
            if (outStream != null) {
                try {
                    outStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
