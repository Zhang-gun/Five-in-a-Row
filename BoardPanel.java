import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

/**
 * 画板绘制类 (View层)
 * 作用：它就像一张画布，不管游戏规则，只负责把二维数组里的数字用图形画出来。
 * 这个类里面重写了 paintComponent，是 Java Swing 面试经常被考的核心知识。
 */
public class BoardPanel extends JPanel {
    
    // 我们把每一个小格子的宽和高定为 40 像素
    public static final int CELL_SIZE = 40;       
    
    // 棋盘边缘一定要留白，不然最外围的棋子有一半会画出屏幕外面去
    public static final int MARGIN = 40;          
    
    // 挑了一个类似木头的颜色，看起来比单纯的灰底好看
    private static final Color BOARD_COLOR = new Color(222, 184, 135); 

    private GameModel model; 
    
    // 回调接口
    private BoardClickListener listener; 

    /**
     * 这是我们自己定义的一个接口。
     * 为什么不直接在鼠标点击事件里去改数组？
     * 因为画板（View）不能干涉数据（Model），我们通过接口把坐标传出去，
     * 让外面的主窗口（Controller）去决定能不能下棋。
     */
    public interface BoardClickListener {
        void onBoardClicked(int x, int y);
    }

    public BoardPanel(GameModel gameModel, BoardClickListener clickListener) {
        this.model = gameModel;
        this.listener = clickListener;
        
        // 算出画板的物理尺寸：15个格子 * 40像素 + 两边的留白（40 * 2）
        int totalWidth = GameModel.BOARD_SIZE * CELL_SIZE + MARGIN * 2;
        int totalHeight = GameModel.BOARD_SIZE * CELL_SIZE + MARGIN * 2;
        
        // 把这个尺寸告诉上层的布局管理器
        this.setPreferredSize(new Dimension(totalWidth, totalHeight));
        this.setBackground(BOARD_COLOR); 

        // 监听鼠标左键点击
        this.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                handleMouseClick(e.getX(), e.getY());
            }
        });
    }

    /**
     * 难点：坐标映射。
     * 鼠标拿到的 e.getX() 是电脑屏幕上的像素（比如118像素），
     * 我们必须把它转换成二维数组的索引（0~14）。
     */
    private void handleMouseClick(int mouseX, int mouseY) {
        // 第一步：先减去 MARGIN（40），把边缘的偏差去掉。
        // 第二步：为什么要加上 CELL_SIZE / 2（20）？
        // 这是为了实现“四舍五入”。比如鼠标点在 18 的位置，加上 20 就是 38，除以 40 就是 0。
        // 如果点在 25 的位置，加上 20 就是 45，除以 40 就是 1。
        // 这样玩家哪怕没点准，只要离哪个交叉点近，棋子就会自动吸附到那个点上。
        int arrayX = (mouseX - MARGIN + CELL_SIZE / 2) / CELL_SIZE;
        int arrayY = (mouseY - MARGIN + CELL_SIZE / 2) / CELL_SIZE;
        
        if (this.listener != null) {
            // 把算好的数组坐标报告给外面
            this.listener.onBoardClicked(arrayX, arrayY);
        }
    }

    /**
     * 这是整个类最核心的方法。
     * 每次窗口被挡住又移开，或者我们主动调用 repaint() 时，系统就会执行这里。
     */
    @Override
    protected void paintComponent(Graphics g) {
        // 注意：这句 super 一定不能丢！它的作用是把上一帧的画面擦干净，
        // 不然你画的东西会跟之前残影重叠在一起，变成大染缸。
        super.paintComponent(g); 
        
        // 把普通的画笔 g 强转成高阶的 g2d，g2d 提供了很多高级画法（比如加粗、渐变色）
        Graphics2D g2d = (Graphics2D) g; 
        
        // 开启抗锯齿，不然画出的圆圈边缘全是像素颗粒（狗牙状）
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        drawGridLines(g2d);     // 画线
        drawAxisLabels(g2d);    // 画坐标数字
        drawStarPoints(g2d);    // 画五个小黑点
        drawAllStones(g2d);     // 画棋子
        drawLastMoveMarker(g2d);// 重点标出最后一步
    }

    private void drawGridLines(Graphics2D g2d) {
        g2d.setColor(Color.BLACK); 
        int size = GameModel.BOARD_SIZE;
        
        // 利用循环，一次画一条横线和一条竖线
        for (int i = 0; i < size; i++) {
            // 起点 y 和终点 y 是一样的，x 从 margin 一直画到棋盘右边
            int yPos = MARGIN + i * CELL_SIZE;
            g2d.drawLine(MARGIN, yPos, MARGIN + (size - 1) * CELL_SIZE, yPos);
            
            int xPos = MARGIN + i * CELL_SIZE;
            g2d.drawLine(xPos, MARGIN, xPos, MARGIN + (size - 1) * CELL_SIZE);
        }
    }

    private void drawAxisLabels(Graphics2D g2d) {
        g2d.setFont(new Font("Arial", Font.BOLD, 12));
        g2d.setColor(Color.DARK_GRAY);
        
        int size = GameModel.BOARD_SIZE;
        for (int i = 0; i < size; i++) {
            // 画英文字母 A, B, C...
            String charLabel = String.valueOf((char)('A' + i));
            int charX = MARGIN + i * CELL_SIZE - 4; // -4是为了居中
            int topCharY = MARGIN - 15; // 放在最上边留白区
            g2d.drawString(charLabel, charX, topCharY);
            
            // 画数字 1, 2, 3...
            String numLabel = String.valueOf(i + 1);
            int leftNumX = MARGIN - 25;
            int numY = MARGIN + i * CELL_SIZE + 4;
            g2d.drawString(numLabel, leftNumX, numY);
        }
    }

    private void drawStarPoints(Graphics2D g2d) {
        g2d.setColor(Color.BLACK);
        // 五子棋的标准星位在第4根线和第12根线（索引就是3和11），中间是天元（7）
        int[] stars = {3, 7, 11}; 
        
        for (int i = 0; i < stars.length; i++) {
            for (int j = 0; j < stars.length; j++) {
                int px = MARGIN + stars[i] * CELL_SIZE;
                int py = MARGIN + stars[j] * CELL_SIZE;
                // 用 fillOval 画实心圆，圆心偏移-4是为了以交叉点为中心画直径为8的圆
                g2d.fillOval(px - 4, py - 4, 8, 8);
            }
        }
    }

    private void drawAllStones(Graphics2D g2d) {
        // 去问模型要数据
        int[][] currentBoard = this.model.getBoard();
        int size = GameModel.BOARD_SIZE;
        
        // 两层 for 循环，如果是 1 就画黑子，如果是 2 就画白子
        for (int row = 0; row < size; row++) {
            for (int col = 0; col < size; col++) {
                int colorValue = currentBoard[row][col];
                if (colorValue == 1) {
                    drawSingleStone(g2d, row, col, true);
                } else if (colorValue == 2) {
                    drawSingleStone(g2d, row, col, false);
                }
            }
        }
    }

    private void drawSingleStone(Graphics2D g2d, int gridX, int gridY, boolean isBlack) {
        // 先把数组坐标再转回成像素坐标
        int startX = MARGIN + gridX * CELL_SIZE - CELL_SIZE / 2 + 2;
        int startY = MARGIN + gridY * CELL_SIZE - CELL_SIZE / 2 + 2;
        // 把棋子画得比格子稍小一点，以免连在一起太挤了
        int stoneSize = CELL_SIZE - 4; 

        // 亮点：不用纯黑色画棋子，用了 RadialGradientPaint (径向渐变色)！
        // 它会产生一种中间发白（反光），边缘发黑的光学效果，看起来像真的立体玻璃棋子。
        if (isBlack) {
            RadialGradientPaint paint = new RadialGradientPaint(
                new Point(startX + stoneSize / 3, startY + stoneSize / 3), 
                stoneSize, 
                new float[]{0.0f, 1.0f}, 
                new Color[]{Color.LIGHT_GRAY, Color.BLACK}
            );
            g2d.setPaint(paint); // 挂载渐变色画笔
        } else {
            RadialGradientPaint paint = new RadialGradientPaint(
                new Point(startX + stoneSize / 3, startY + stoneSize / 3), 
                stoneSize, 
                new float[]{0.0f, 1.0f}, 
                new Color[]{Color.WHITE, Color.GRAY}
            );
            g2d.setPaint(paint);
        }
        
        g2d.fillOval(startX, startY, stoneSize, stoneSize); 
    }

    private void drawLastMoveMarker(Graphics2D g2d) {
        List<Point> history = this.model.getHistory();
        if (history.size() > 0) {
            // 获取最后一步下的那颗棋子
            Point lastPoint = history.get(history.size() - 1);
            g2d.setColor(Color.RED);
            int boxX = MARGIN + lastPoint.x * CELL_SIZE - 3;
            int boxY = MARGIN + lastPoint.y * CELL_SIZE - 3;
            // 在上面画一个红色的小框，提示玩家这是最后走的
            g2d.drawRect(boxX, boxY, 6, 6);
        }
    }
}
