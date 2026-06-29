import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

/**
 * AI 玩家逻辑类
 * 本类专注于实现人机对弈的计算内核。
 * 主要运用了局部最优的贪心算法 (Greedy Algorithm) 配合自定义的启发式位置评估函数 (Heuristic Evaluation)。
 */
public class AIPlayer {
    
    /**
     * 简单模式 AI：纯随机落子策略
     * 遍历整个棋盘收集所有空位坐标，随后利用 Math.random() 在合法空位中随机抽取其一。
     * 该模式主要供新手练习测试使用。
     * 
     * @param model 游戏当前的数据模型
     * @return 决定的落子坐标
     */
    public Point getEasyMove(GameModel model) {
        int[][] board = model.getBoard();
        int size = GameModel.BOARD_SIZE;
        List<Point> emptyPoints = new ArrayList<>();
        
        // 遍历提取全盘空位
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                if (board[i][j] == 0) emptyPoints.add(new Point(i, j));
            }
        }
        // 防止出现无可落子导致异常的极端情况
        if (emptyPoints.isEmpty()) return null;
        
        // 返回随机索引处的坐标
        return emptyPoints.get((int)(Math.random() * emptyPoints.size()));
    }

    /**
     * 困难模式 AI：贪心算法核心引擎
     * 算法思想：不对未来步数进行深度博弈树搜索（如极小极大算法），而是将算力集中于对当前局面的单步静态评估。
     * 对于棋盘上每一个空位，分别计算它对白方（AI自身）的进攻价值和对黑方（玩家）的防守价值。
     * 将两项价值加和，求出全局战略价值最高的最优解。
     * 
     * @param model 游戏当前的数据模型
     * @return 全局最高收益的落子坐标
     */
    public Point getHardMove(GameModel model) {
        int maxScore = -1;
        Point bestPoint = null;
        int size = GameModel.BOARD_SIZE;
        int[][] board = model.getBoard();

        // 嵌套循环遍历 15x15 棋盘
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                if (board[i][j] == 0) { 
                    // scoreAttack: 如果AI在此落子，能促成己方连珠的评分收益
                    int scoreAttack = evaluatePosition(board, size, i, j, 2); 
                    // scoreDefend: 如果玩家在此落子，能促成敌方连珠的评分威胁（防守收益）
                    int scoreDefend = evaluatePosition(board, size, i, j, 1); 
                    // 综合该点的攻防总价值
                    int score = scoreAttack + scoreDefend;
                    
                    // 贪心策略：只记录并保留得分最高的位置
                    if (score > maxScore) {
                        maxScore = score;
                        bestPoint = new Point(i, j);
                    }
                }
            }
        }
        // 若棋盘已满或出现未预料的情况，回退至随机落子处理
        return bestPoint != null ? bestPoint : getEasyMove(model);
    }

    /**
     * 启发式打分函数（权重评估系统）
     * 原理：模拟在指定坐标(x,y)落下指定颜色(color)的棋子后，向 4 个方向探测所能构成的棋型。
     * 重点不仅在于计算"连续同色棋子的数量 (count)"，更在于识别"该连线的两端是否被堵死 (block)"。
     * 
     * @param board 棋盘二维数组
     * @param size 棋盘尺寸
     * @param x 评估点横坐标
     * @param y 评估点纵坐标
     * @param color 评估的阵营颜色（1黑，2白）
     * @return 该点的阵型加权得分
     */
    private int evaluatePosition(int[][] board, int size, int x, int y, int color) {
        int totalScore = 0;
        // 四个探测轴：横轴、纵轴、两条对角线
        int[][] directions = {{1, 0}, {0, 1}, {1, 1}, {1, -1}};
        
        for (int[] d : directions) {
            int count = 1;  // 当前轴上连成一线的同色棋子总数
            int block = 0;  // 当前轴上被堵死的端点数（取值为 0, 1 或 2）
            
            // 正向四步探测
            for (int step = 1; step <= 4; step++) {
                int nx = x + d[0] * step;
                int ny = y + d[1] * step;
                // 若坐标越界，视为该端被物理边界堵死
                if (nx < 0 || nx >= size || ny < 0 || ny >= size) { block++; break; }
                
                if (board[nx][ny] == color) { 
                    count++; // 同色棋子，延长连线
                } else if (board[nx][ny] == 0) { 
                    break;   // 遇到空位，说明线未被堵死，停止延伸探测
                } else { 
                    block++; // 遇到敌方棋子，说明该端被堵死
                    break; 
                }                 
            }
            
            // 反向四步探测（逻辑同正向）
            for (int step = 1; step <= 4; step++) {
                int nx = x - d[0] * step;
                int ny = y - d[1] * step;
                if (nx < 0 || nx >= size || ny < 0 || ny >= size) { block++; break; }
                
                if (board[nx][ny] == color) { 
                    count++; 
                } else if (board[nx][ny] == 0) { 
                    break; 
                } else { 
                    block++; 
                    break; 
                }
            }
            
            // 极其核心的权重赋值表：通过赋予极大差值的权重，引导 AI 做出理性决策
            if (count >= 5) totalScore += 100000;                  // 连五：达成或破坏必胜条件，优先级最高
            else if (count == 4 && block == 0) totalScore += 10000;// 活四：两端皆空的四连，等同于必胜
            else if (count == 4 && block == 1) totalScore += 1000; // 冲四：一端被堵的四连，必须防守
            else if (count == 3 && block == 0) totalScore += 1000; // 活三：有极大发展潜力的阵型
            else if (count == 3 && block == 1) totalScore += 100;  // 眠三：发展受限的三连
            else if (count == 2 && block == 0) totalScore += 100;  // 活二：初级发展阵型
            else if (count == 2 && block == 1) totalScore += 10;   // 眠二：价值较低
            else totalScore += 1;                                  // 单子边缘收益
        }
        return totalScore;
    }
}
