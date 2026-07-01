import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

/**
 * 游戏数据模型 (Model层)
 * 作用：专门用来存数据和写判断输赢的逻辑。
 * 我们把界面（画图）和数据（算数）分开写，这样即使以后想把五子棋做到网页上，
 * 这部分代码也完全不用改。这就叫面向对象里的“低耦合”。
 */
public class GameModel {
    
    // 棋盘大小，标准五子棋为15x15
    // 用 public static final 定义成常量，这样其他类就可以直接通过 GameModel.BOARD_SIZE 来用，
    // 不用到处写死 "15" 这个数字。如果以后想改成 19x19 围棋盘，只改这里就行了。
    public static final int BOARD_SIZE = 15;
    
    // 二维数组存放棋盘状态
    // 注意：这里 board[x][y] 其实 x 对应的是列(横坐标)，y 对应的是行(纵坐标)。
    // 0 代表空位，1 代表黑棋（玩家），2 代表白棋（电脑）。
    private int[][] board;
    
    // 当前回合判断：true为黑方，false为白方
    private boolean isBlackTurn;
    
    // 游戏是否结束的标志，如果结束了就不让再下棋了
    private boolean gameOver;
    
    // 历史落子记录
    // 用 List 泛型集合来保存 Point 对象（Point里面存了 x 和 y）。
    // 这个相当于一个栈（Stack），后下的棋在最后面，这样悔棋的时候拿最后面的就行了。
    private List<Point> history;
    
    // 记录目前一共下了多少步棋，如果下满 225 步就说明平局了
    private int moveCount;

    /**
     * 构造函数
     * 每次 new 这个类的对象时，就会自动调用 reset() 方法。
     */
    public GameModel() {
        reset();
    }

    /**
     * 重置游戏所有数据
     * 无论是新开一局，还是点击“重新开始”按钮，都是调用这里。
     */
    public void reset() {
        // 重新 new 一个 15x15 的数组，Java 会自动把所有元素设为 0
        this.board = new int[BOARD_SIZE][BOARD_SIZE];
        this.history = new ArrayList<>();
        this.isBlackTurn = true; // 黑棋先走
        this.gameOver = false;
        this.moveCount = 0;
    }

    // ==========================================
    // 下面是一堆 Getter 和 Setter 方法
    // 因为上面的变量都是 private（私有的），外面访问不到，
    // 所以必须提供这些 public 方法给外面的类调用，这体现了面向对象的“封装”思想。
    // ==========================================

    public boolean isGameOver() {
        return this.gameOver;
    }

    public void setGameOver(boolean over) {
        this.gameOver = over;
    }

    public boolean isBlackTurn() {
        return this.isBlackTurn;
    }

    public int getMoveCount() {
        return this.moveCount;
    }

    public int[][] getBoard() {
        return this.board;
    }

    public List<Point> getHistory() {
        return this.history;
    }

    /**
     * 判断当前棋盘是否已经下满（平局判断）
     */
    public boolean isBoardFull() {
        int maxCapacity = BOARD_SIZE * BOARD_SIZE;
        // 如果步数大于等于 225，说明全填满了
        if (this.moveCount >= maxCapacity) {
            return true;
        }
        return false;
    }

    /**
     * 在指定坐标尝试落子
     * 
     * @param x 传进来的棋盘横向索引
     * @param y 传进来的棋盘纵向索引
     * @return 如果落子成功返回 true，否则返回 false
     */
    public boolean addMove(int x, int y) {
        // 必须做越界检查！如果不拦截负数或者大于14的值，
        // Java 就会报 ArrayIndexOutOfBoundsException，整个程序就崩溃闪退了。
        if (x < 0 || x >= BOARD_SIZE) {
            return false;
        }
        if (y < 0 || y >= BOARD_SIZE) {
            return false;
        }
        
        // 判断该位置是否已经有棋子了，如果不为0说明被占了
        if (this.board[x][y] != 0) {
            return false;
        }

        // 根据当前回合，决定填入1（黑）还是2（白）
        if (this.isBlackTurn) {
            this.board[x][y] = 1;
        } else {
            this.board[x][y] = 2;
        }

        // 将这一步存进 history，留着悔棋用
        Point currentPoint = new Point(x, y);
        this.history.add(currentPoint);
        this.moveCount++; // 步数+1
        
        return true; // 落子成功
    }

    /**
     * 执行悔棋操作
     */
    public void undoMove() {
        // 安全检查：如果记录是空的，说明还没下过棋，直接 return 防止报错
        if (this.history.isEmpty()) {
            return;
        }
        
        // history.size() - 1 就是最后一步棋的索引
        int lastIndex = this.history.size() - 1;
        
        // 用 remove 方法把最后一步弹出来
        Point lastMove = this.history.remove(lastIndex);
        
        // 关键步骤：不仅要从集合里删掉，还要把棋盘数组里这个坐标的值恢复成 0！
        this.board[lastMove.x][lastMove.y] = 0;
        
        this.moveCount--; // 步数减回去
        
        // 把控制权交还给悔棋的一方
        this.isBlackTurn = !this.isBlackTurn;
    }

    /**
     * 切换回合
     */
    public void toggleTurn() {
        this.isBlackTurn = !this.isBlackTurn; // true 变 false，false 变 true
    }

    /**
     * 核心胜负判断：看看刚刚下的这颗棋子有没有促成五连珠。
     * 思路：不需要全盘扫描，只要以刚刚落子的点为中心，
     * 向左向右、向上向下、左上右下、左下右上 四条线数一下连子数就行了。
     */
    public boolean checkWin(int x, int y) {
        // 先看看刚刚下的是黑的还是白的
        int targetColor = this.board[x][y];
        
        // 1. 检查横向（水平方向的连子数，要加上自己这颗，所以最后 +1）
        int countHorizontal = countContinuousStones(x, y, -1, 0, targetColor) 
                            + countContinuousStones(x, y, 1, 0, targetColor) + 1;
        if (countHorizontal >= 5) {
            return true; // 如果大于等于5，直接宣布赢了
        }
        
        // 2. 检查纵向（垂直方向）
        int countVertical = countContinuousStones(x, y, 0, -1, targetColor) 
                          + countContinuousStones(x, y, 0, 1, targetColor) + 1;
        if (countVertical >= 5) {
            return true;
        }
        
        // 3. 检查主对角线（左上 到 右下）
        int countMainDiagonal = countContinuousStones(x, y, -1, -1, targetColor) 
                              + countContinuousStones(x, y, 1, 1, targetColor) + 1;
        if (countMainDiagonal >= 5) {
            return true;
        }
        
        // 4. 检查副对角线（右上 到 左下）
        int countSubDiagonal = countContinuousStones(x, y, 1, -1, targetColor) 
                             + countContinuousStones(x, y, -1, 1, targetColor) + 1;
        if (countSubDiagonal >= 5) {
            return true;
        }
        
        // 四个方向都没有5颗连在一起的，那就没赢
        return false;
    }

    /**
     * 辅助方法：顺着某个方向去数，有几颗连续的同色棋子。
     * 这个方法被上面的 checkWin 调用了 8 次，极大减少了代码冗余。
     * 
     * @param dx 控制横向走步，如果是 -1 就代表往左找，1 就代表往右找
     * @param dy 控制纵向走步，如果是 -1 就代表往上找，1 就代表往下找
     */
    private int countContinuousStones(int startX, int startY, int dx, int dy, int color) {
        int count = 0;
        
        // 顺着给定的步长走一步，作为起点
        int currentX = startX + dx;
        int currentY = startY + dy;
        
        // 五子棋最多只要连着找 4 步就够了（因为加上自己就是5颗）
        for (int step = 0; step < 4; step++) {
            // 一旦走到棋盘外面，就直接跳出循环
            if (currentX < 0 || currentX >= BOARD_SIZE) {
                break;
            }
            if (currentY < 0 || currentY >= BOARD_SIZE) {
                break;
            }
            
            // 如果碰到的棋子刚好是我们想要找的颜色
            if (this.board[currentX][currentY] == color) {
                count++; // 记录找到一颗
                // 继续顺着这个方向往下走
                currentX += dx;
                currentY += dy;
            } else {
                // 如果遇到空位或者不同颜色的棋子（被断开了），马上停止找
                break;
            }
        }
        
        return count;
    }
}
