import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

/**
 * 游戏数据与逻辑模型类 (MVC - Model)
 * 本类负责维护棋盘的核心数据结构、落子历史记录，以及处理胜负校验逻辑。
 * 设计思想：将游戏逻辑与界面绘制完全分离，保证了代码的高内聚与低耦合，符合面向对象设计原则。
 */
public class GameModel {
    // 棋盘尺寸常量，标准的五子棋盘为 15x15 的交叉点网格
    public static final int BOARD_SIZE = 15; 
    
    // 核心数据结构：二维数组表示棋盘状态。0代表空位，1代表黑子，2代表白子
    private int[][] board; 
    
    // 状态标识：当前是否轮到黑棋（玩家）落子
    private boolean isBlackTurn; 
    
    // 状态标识：记录当前游戏是否已经分出胜负
    private boolean gameOver; 
    
    // 存储历史落子坐标的集合，使用 List 充当栈(Stack)的作用，主要用于实现悔棋功能
    private List<Point> history; 
    
    // 记录当前对局的总落子手数，用于在界面日志中展示
    private int moveCount; 

    public GameModel() {
        reset(); 
    }

    /**
     * 重置游戏状态的初始化方法。
     * 每次重新开始对局时调用，清空二维数组与历史记录，并将先手交还给黑棋。
     */
    public void reset() {
        board = new int[BOARD_SIZE][BOARD_SIZE];
        history = new ArrayList<>();
        isBlackTurn = true; 
        gameOver = false;
        moveCount = 0;
    }

    // --- 以下为封装属性的 Getter 和 Setter 方法，保证数据的封装性 ---
    public boolean isGameOver() { return gameOver; }
    public void setGameOver(boolean over) { this.gameOver = over; }
    public boolean isBlackTurn() { return isBlackTurn; }
    public int getMoveCount() { return moveCount; }
    public int[][] getBoard() { return board; }
    public List<Point> getHistory() { return history; }

    /**
     * 执行落子操作。
     * @param x 棋盘横坐标
     * @param y 棋盘纵坐标
     * @return 如果落子成功返回 true，如果坐标越界或该位置已有棋子则返回 false
     */
    public boolean addMove(int x, int y) {
        // 安全性检查：防止数组越界异常，并确保只能在空位落子
        if (x < 0 || x >= BOARD_SIZE || y < 0 || y >= BOARD_SIZE || board[x][y] != 0) {
            return false;
        }
        // 根据当前轮次判断填入 1(黑) 还是 2(白)
        board[x][y] = isBlackTurn ? 1 : 2; 
        
        // 将此次落子行为压入历史集合，为可能的悔棋操作做准备
        history.add(new Point(x, y)); 
        moveCount++;
        return true; 
    }

    /**
     * 悔棋逻辑处理。
     * 核心原理：弹出 history 集合中最后一个记录的坐标点，并将其在二维数组中恢复为 0(空位)，
     * 同时递减落子步数并切换回上一方的回合。
     */
    public void undoMove() {
        if (!history.isEmpty()) {
            Point last = history.remove(history.size() - 1);
            board[last.x][last.y] = 0;
            moveCount--;
            isBlackTurn = !isBlackTurn; 
        }
    }

    /**
     * 回合切换控制
     */
    public void toggleTurn() {
        isBlackTurn = !isBlackTurn;
    }

    /**
     * 胜负判定核心算法 (线性双向扫描)
     * 性能优化说明：该算法避免了每次落子后全盘遍历扫描 O(N^2) 的巨大开销。
     * 它选择以当前最新落子的坐标为中心，分别向水平、垂直、主对角线、副对角线 4 个方向进行探测，
     * 时间复杂度仅为 O(1)。
     * 
     * @param x 当前落子的横坐标
     * @param y 当前落子的纵坐标
     * @return 是否产生五连珠获胜
     */
    public boolean checkWin(int x, int y) {
        int color = board[x][y]; 
        
        // 定义四个方向的步长向量：{水平方向}, {垂直方向}, {主对角线 \}, {副对角线 /}
        int[][] directions = {{1, 0}, {0, 1}, {1, 1}, {1, -1}};
        
        for (int[] d : directions) {
            // 初始化连子数为 1（包含中心刚刚落下的这颗棋子自身）
            int count = 1; 
            
            // 沿当前方向的正半轴延伸探测（最多探测 4 步）
            for (int step = 1; step < 5; step++) {
                int nx = x + d[0] * step;
                int ny = y + d[1] * step;
                // 若坐标未越界且颜色相同，则连子数+1；否则说明该线已断开，立即跳出内循环
                if (nx >= 0 && nx < BOARD_SIZE && ny >= 0 && ny < BOARD_SIZE && board[nx][ny] == color) count++;
                else break;
            }
            
            // 沿当前方向的负半轴延伸探测（逻辑同上）
            for (int step = 1; step < 5; step++) {
                int nx = x - d[0] * step;
                int ny = y - d[1] * step;
                if (nx >= 0 && nx < BOARD_SIZE && ny >= 0 && ny < BOARD_SIZE && board[nx][ny] == color) count++;
                else break;
            }
            
            // 只要有任何一条轴向上的同色连子数达到或超过 5 颗，即可判定当前玩家获胜
            if (count >= 5) return true;
        }
        return false;
    }
}
