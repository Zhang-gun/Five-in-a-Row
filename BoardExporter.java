import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * 棋谱导出工具类
 * 用最朴实无华的双重 for 循环，把棋盘的二维数组输出到文本文档里。
 * 这个类展示了我们不仅会用 Properties，也会用原生的 FileWriter 读写文本流。
 */
public class BoardExporter {

    // 导出的文件名
    private static final String EXPORT_FILE_NAME = "board_log.txt";

    /**
     * 执行导出棋谱操作
     */
    public static void exportBoardToTxt(int[][] board, int moveCount, String winner) {
        
        File file = new File(EXPORT_FILE_NAME);
        FileWriter writer = null;
        
        try {
            // 初始化写入工具，直接绑定那个 File
            writer = new FileWriter(file);
            
            // 写一些看起来很高大上的表头
            writer.write("=====================================\n");
            writer.write("        五子棋局面对局快照导出        \n");
            writer.write("=====================================\n");
            writer.write("当前总步数: " + moveCount + " 步\n");
            writer.write("当前胜利状态: " + winner + "\n");
            writer.write("=====================================\n\n");
            
            // 1. 先写第一行的列坐标（A, B, C... O）
            writer.write("   "); // 前面空出两个格，给后面的行号留位置
            for (int i = 0; i < 15; i++) {
                char letter = (char) ('A' + i);
                writer.write(letter + " "); // 拼上一个空格，看着不挤
            }
            writer.write("\n"); // 列号写完一定要换行
            
            // 2. 双重循环扫面整个二维数组
            // 外层循环控制行（Y轴）
            for (int row = 0; row < 15; row++) {
                
                // 每一行的最前面要写上行号（1到15）
                int rowNumber = row + 1;
                // 如果是个位数，为了对齐，我们多写一个空格
                if (rowNumber < 10) {
                    writer.write(rowNumber + "  ");
                } else {
                    writer.write(rowNumber + " ");
                }
                
                // 内层循环控制列（X轴）
                for (int col = 0; col < 15; col++) {
                    int stoneValue = board[row][col];
                    
                    // 用 if-else 判断这个格子到底有什么
                    if (stoneValue == 0) {
                        // 空的地方画个 + 充当交叉线
                        writer.write("+ ");
                    } else if (stoneValue == 1) {
                        // 黑子画 X
                        writer.write("X ");
                    } else if (stoneValue == 2) {
                        // 白子画 O
                        writer.write("O ");
                    } else {
                        writer.write("? ");
                    }
                }
                
                // 【重点】内层的一行格子全画完了，必须要写一个 \n 换行！
                // 不然全糊成一排了。
                writer.write("\n");
            }
            
            writer.write("\n=====================================\n");
            writer.write("导出完成！\n");
            
        } catch (IOException e) {
            System.out.println("导出棋谱时发生了写入错误！");
            e.printStackTrace(); // 在控制台打出红色报错信息，方便调试
        } finally {
            // 一定要在这把流关掉
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
