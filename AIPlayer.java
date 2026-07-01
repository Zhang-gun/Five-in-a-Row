import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

/**
 * 电脑 AI 逻辑类
 * 包含简单、中等、困难三种难度。
 * 其实核心逻辑就是给棋盘上的每个空位打分，分数越高，说明下在那里的价值越大。
 */
public class AIPlayer {

    /**
     * 简单难度：瞎下模式
     * 做法非常简单粗暴：把棋盘遍历一遍，看到空位就装进 List 里，
     * 然后用 Math.random() 随便抽一个。
     */
    public Point getEasyMove(GameModel model) {
        int[][] board = model.getBoard();
        List<Point> availablePoints = new ArrayList<>();
        
        // 双层 for 循环遍历整个二维数组
        for (int i = 0; i < GameModel.BOARD_SIZE; i++) {
            for (int j = 0; j < GameModel.BOARD_SIZE; j++) {
                // 只有等于 0（没下过棋的地方）才能加进去
                if (board[i][j] == 0) {
                    availablePoints.add(new Point(i, j));
                }
            }
        }
        
        if (availablePoints.isEmpty()) {
            return null; // 防错处理，棋盘满了就返回空
        }
        
        // Math.random() 返回 0.0 到 1.0 的小数，乘上总长度后强转为 int，就是随机索引
        int randomIndex = (int) (Math.random() * availablePoints.size());
        return availablePoints.get(randomIndex);
    }

    /**
     * 中等难度：只攻不守模式
     * 逻辑：在这个模式下，电脑眼里只有自己怎么连五子，根本不在乎玩家怎么下。
     */
    public Point getMediumMove(GameModel model) {
        int bestScore = -1; // 记录找到的最高分，初始给个 -1
        Point bestMove = null;
        int[][] board = model.getBoard();

        for (int i = 0; i < GameModel.BOARD_SIZE; i++) {
            for (int j = 0; j < GameModel.BOARD_SIZE; j++) {
                if (board[i][j] == 0) { 
                    // 这里传 2 进去，意思是：“如果把白子下在这里，能得多少分？”
                    int attackScore = evaluatePoint(board, i, j, 2); 
                    
                    // 如果这个点的得分比之前的最高分还要高，就更新最高分和最佳坐标
                    if (attackScore > bestScore) {
                        bestScore = attackScore;
                        bestMove = new Point(i, j);
                    }
                }
            }
        }
        
        // 如果实在找不到好地方，就调随机模式随便走一步
        if (bestMove == null) {
            return getEasyMove(model);
        }
        return bestMove;
    }

    /**
     * 困难难度：攻守兼备模式 (也就是俗称的贪心算法)
     * 逻辑：不仅要算自己（白棋）下这里的得分，还要假想如果玩家（黑棋）下这里，会有多大威胁。
     * 把这两者的分加起来，得出的就是这步棋的“综合战略价值”。
     */
    public Point getHardMove(GameModel model) {
        int maxTotalScore = -1;
        Point bestPoint = null;
        int[][] board = model.getBoard();

        for (int i = 0; i < GameModel.BOARD_SIZE; i++) {
            for (int j = 0; j < GameModel.BOARD_SIZE; j++) {
                if (board[i][j] == 0) { 
                    // 1. 进攻分：算算自己连珠的好处
                    int myScore = evaluatePoint(board, i, j, 2); 
                    
                    // 2. 防守分：换位思考，算算玩家下在这里的好处
                    // 如果玩家下这里是个活四，那这个分数就会极高，逼着电脑必须下这里去堵他！
                    int enemyScore = evaluatePoint(board, i, j, 1); 
                    
                    // 两者相加
                    int totalScore = myScore + enemyScore;
                    
                    if (totalScore > maxTotalScore) {
                        maxTotalScore = totalScore;
                        bestPoint = new Point(i, j);
                    }
                }
            }
        }
        
        if (bestPoint == null) {
            return getEasyMove(model);
        }
        return bestPoint;
    }

    /**
     * 给某个点打分的方法
     * 把横、竖、左斜、右斜四个方向的分数加起来，就是总分。
     */
    private int evaluatePoint(int[][] board, int x, int y, int color) {
        int finalScore = 0;
        
        // 调用下面的 evaluateSingleLine 方法，dx 和 dy 组合代表不同的方向
        finalScore += evaluateSingleLine(board, x, y, 1, 0, color);  // dx=1,dy=0 代表横向
        finalScore += evaluateSingleLine(board, x, y, 0, 1, color);  // dx=0,dy=1 代表纵向
        finalScore += evaluateSingleLine(board, x, y, 1, 1, color);  // dx=1,dy=1 代表主对角
        finalScore += evaluateSingleLine(board, x, y, 1, -1, color); // 代表副对角
        
        return finalScore;
    }

    /**
     * 对单条直线打分
     * 这个算法的核心思想是：找连着几个子，以及这串棋子的两头有没有被“堵死”。
     */
    private int evaluateSingleLine(int[][] board, int x, int y, int dx, int dy, int color) {
        int stoneCount = 1; // 记录连子的数量（自己本身算1颗）
        
        // blockedEnds 用来记录两头被堵的情况。
        // 如果是 0，说明两头都有空位，是“活”棋；
        // 如果是 1，说明一头被堵，叫“冲”棋或者“眠”棋；
        // 如果是 2，说明两头都被死死堵住，这就是废棋了。
        int blockedEnds = 0; 
        
        // ---------------- 往前探 ----------------
        int currX = x + dx;
        int currY = y + dy;
        for (int step = 0; step < 4; step++) {
            // 如果撞到了棋盘边缘，那肯定算这头被堵死了
            if (currX < 0 || currX >= GameModel.BOARD_SIZE || currY < 0 || currY >= GameModel.BOARD_SIZE) {
                blockedEnds++; 
                break;
            }
            if (board[currX][currY] == color) {
                stoneCount++; // 颜色一样，连子数 +1
            } else if (board[currX][currY] == 0) {
                break; // 碰到空位了，说明这头是活的，直接停下来
            } else {
                blockedEnds++; // 碰到敌人棋子了，这头被堵死了
                break;
            }
            currX += dx;
            currY += dy;
        }
        
        // ---------------- 往后探 ----------------
        currX = x - dx;
        currY = y - dy;
        for (int step = 0; step < 4; step++) {
            if (currX < 0 || currX >= GameModel.BOARD_SIZE || currY < 0 || currY >= GameModel.BOARD_SIZE) {
                blockedEnds++;
                break;
            }
            if (board[currX][currY] == color) {
                stoneCount++;
            } else if (board[currX][currY] == 0) {
                break;
            } else {
                blockedEnds++;
                break;
            }
            currX -= dx;
            currY -= dy;
        }
        
        // ---------------- 根据连子数量和被堵情况进行人工打分 ----------------
        // 这里的数字是我们手动调的权重。
        if (stoneCount >= 5) {
            return 100000; // 连五，最高优先级，必杀
        } else if (stoneCount == 4) {
            if (blockedEnds == 0) return 10000; // 活四：两头空，下一回合必连五
            if (blockedEnds == 1) return 1000;  // 冲四：虽然一头被堵，但只要不管它也能连五，所以分也很高
        } else if (stoneCount == 3) {
            if (blockedEnds == 0) return 1000;  // 活三：威胁很大，下一回合就变活四了
            if (blockedEnds == 1) return 100;   // 眠三：一头被堵了，威胁相对小一些
        } else if (stoneCount == 2) {
            if (blockedEnds == 0) return 100;   // 活二
            if (blockedEnds == 1) return 10;    // 眠二：分数给得很低
        }
        
        // 啥阵型都不是的话，给个保底分 1 分
        return 1;
    }
}
