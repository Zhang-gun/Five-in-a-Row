import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

/**
 * 棋盘视图类 (MVC - View)
 * 继承自 JPanel，是图形界面的核心绘制区域。
 * 遵循 MVC 设计模式的低耦合原则，本类完全剥离了胜负判断等底层游戏逻辑，
 * 仅负责两项核心任务：1. 通过 Graphics2D 绘制网格与棋子渲染； 2. 捕获鼠标事件并将物理坐标映射为逻辑坐标。
 */
public class BoardPanel extends JPanel {
    // UI 常量设定
    public static final int CELL_SIZE = 40;       // 网格中单一正方形格子的边长（像素）
    public static final int MARGIN = 30;          // 棋盘最外围的边框留白，防止最外围棋子渲染超出边界
    private static final Color WOOD_COLOR = new Color(230, 180, 120); // 实例化一个自定义的柔和木材质色调

    // 引入依赖
    private GameModel model; // 持有模型的引用，采用只读方式获取当前棋盘状态矩阵
    private BoardClickListener listener; // 声明回调监听器引用

    /**
     * 定义回调接口。
     * 当鼠标成功触发点击且算出坐标后，通过此接口向外部控制层 (Controller) 传递坐标。
     * 这种设计解耦了 BoardPanel 与主窗口的硬关联。
     */
    public interface BoardClickListener {
        void onBoardClicked(int i, int j);
    }

    public BoardPanel(GameModel model, BoardClickListener listener) {
        this.model = model;
        this.listener = listener;
        
        // 动态计算整个组件的物理像素尺寸：格子数 * 格边长 + 左右(上下)留白总和
        int panelSize = GameModel.BOARD_SIZE * CELL_SIZE + MARGIN * 2;
        setPreferredSize(new Dimension(panelSize, panelSize));
        setBackground(WOOD_COLOR); 

        // 注册匿名内部类的鼠标监听适配器，专用于捕获玩家落子行为
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                // 【核心算法】物理坐标 (X,Y) 到 逻辑矩阵索引 (i,j) 的降维映射映射
                // 算式解析：先剔除边框留白的偏移量，再加上半个格子的长度进行舍入补偿，
                // 使得鼠标在靠近网格交叉点时，具有一种“磁吸”对齐的效果。
                int i = (e.getX() - MARGIN + CELL_SIZE / 2) / CELL_SIZE;
                int j = (e.getY() - MARGIN + CELL_SIZE / 2) / CELL_SIZE;
                
                // 将计算过滤完毕的合法矩阵坐标发送给监听它的主控制器
                if (listener != null) {
                    listener.onBoardClicked(i, j);
                }
            }
        });
    }

    /**
     * Java Swing 重绘生命周期的核心回调方法。
     * 每次界面刷新（如拖动窗口、调用 repaint()）时，JVM 均会调用此方法渲染当前帧。
     */
    @Override
    protected void paintComponent(Graphics g) {
        // 调用超类方法，清除当前缓冲帧残留的上一帧像素污渍
        super.paintComponent(g); 
        // 将基础画笔强转为高级 Graphics2D 对象，以调用更丰富的渲染 API
        Graphics2D g2d = (Graphics2D) g; 
        
        // 【UI优化】开启抗锯齿渲染管线，消除绘制圆形与倾斜直线时产生的锯齿感，提升视觉清晰度
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int size = GameModel.BOARD_SIZE;
        g2d.setColor(Color.BLACK); 
        
        // 1. 绘制底层纵横交错的网格线
        for (int i = 0; i < size; i++) {
            // 利用简单的线性函数定位每一条横线和竖线的始终点物理坐标
            g2d.drawLine(MARGIN, MARGIN + i * CELL_SIZE, MARGIN + (size - 1) * CELL_SIZE, MARGIN + i * CELL_SIZE);
            g2d.drawLine(MARGIN + i * CELL_SIZE, MARGIN, MARGIN + i * CELL_SIZE, MARGIN + (size - 1) * CELL_SIZE);
        }

        // 2. 绘制星位（专业棋盘中用于定位的实心小圆点，一般有天元及四角星位）
        int[] stars = {3, 7, 11};
        for (int x : stars) {
            for (int y : stars) {
                g2d.fillOval(MARGIN + x * CELL_SIZE - 4, MARGIN + y * CELL_SIZE - 4, 8, 8);
            }
        }

        // 3. 遍历模型层的二维矩阵状态数组，并渲染所有已落子的棋子
        int[][] board = model.getBoard();
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                if (board[i][j] != 0) {
                    drawStone(g2d, i, j, board[i][j]);
                }
            }
        }
        
        // 4. 辅助视效：绘制红色方框追踪显示全局最后一手的具体位置，降低玩家的记忆负担
        List<Point> history = model.getHistory();
        if (!history.isEmpty()) {
            Point p = history.get(history.size() - 1);
            g2d.setColor(Color.RED);
            g2d.drawRect(MARGIN + p.x * CELL_SIZE - 2, MARGIN + p.y * CELL_SIZE - 2, 4, 4);
        }
    }

    /**
     * 封装的单个棋子绘制方法，采用坐标反向映射及径向渐变算法进行拟物化渲染。
     * 
     * @param g2d Graphics2D 渲染引擎
     * @param i 逻辑矩阵横坐标
     * @param j 逻辑矩阵纵坐标
     * @param color 棋子阵营枚举类型（1代表黑子，2代表白子）
     */
    private void drawStone(Graphics2D g2d, int i, int j, int color) {
        // 反向映射：由逻辑坐标算出目标图形包围盒的左上角绝对像素坐标
        int xPos = MARGIN + i * CELL_SIZE - CELL_SIZE / 2 + 2;
        int yPos = MARGIN + j * CELL_SIZE - CELL_SIZE / 2 + 2;
        // 稍作尺寸收缩，使相邻棋子之间保留微小的呼吸空间
        int s = CELL_SIZE - 4; 

        // 【图形进阶】抛弃单调的纯色填充，采用 RadialGradientPaint 构建光源渐变以形成 3D 球体的错觉
        RadialGradientPaint rgp;
        if (color == 1) { 
            // 渲染黑子：通过构建从左上角偏离中心点向外辐射的渐变矩阵，将颜色由高光亮灰过渡至深黑
            rgp = new RadialGradientPaint(new Point(xPos + s/3, yPos + s/3), s, 
                    new float[]{0f, 1f}, new Color[]{Color.LIGHT_GRAY, Color.BLACK});
        } else { 
            // 渲染白子：构建自纯白向外辐射渐变至边缘灰底的光影
            rgp = new RadialGradientPaint(new Point(xPos + s/3, yPos + s/3), s, 
                    new float[]{0f, 1f}, new Color[]{Color.WHITE, Color.GRAY});
        }
        
        // 注入渐变着色器并执行填充指令
        g2d.setPaint(rgp); 
        g2d.fillOval(xPos, yPos, s, s); 
        
        // 追加一层细微的暗灰色描边，作为漫反射边缘以增强整体材质感
        g2d.setColor(Color.DARK_GRAY);
        g2d.setStroke(new BasicStroke(1));
        g2d.drawOval(xPos, yPos, s, s);
    }
}
