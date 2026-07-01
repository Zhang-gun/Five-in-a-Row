import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 游戏主窗口类 (MVC架构里的 Controller 控制器)
 * 这个类是最大的管家，它负责把各个模块（界面、模型、AI）拼装起来，
 * 并处理所有的鼠标点击和按钮事件。
 */
public class GomokuGame extends JFrame implements BoardPanel.BoardClickListener {
    
    // 聚合三个核心组件
    private GameModel model;      
    private AIPlayer aiPlayer;    
    private BoardPanel boardPanel;
    
    // 游戏模式：0是双人对战，1是简单人机，2是困难人机
    private int gameMode = 0; 
    
    // 记录赢了多少局
    private int blackWins = 0; 
    private int whiteWins = 0; 
    
    // 我们声明了一堆需要随状态改变的界面控件
    private JTextArea logArea;             
    private JLabel statusLabel;            
    private JLabel blackScoreLabel;        
    private JLabel whiteScoreLabel;        
    private JLabel currentTurnLabel;       

    public GomokuGame() {
        // 第一步：new 出核心模块
        this.model = new GameModel();
        this.aiPlayer = new AIPlayer();
        
        // 调用 C 同学写的文件存储类，读取上次存下来的比分
        int[] scores = RecordManager.loadScores();
        this.blackWins = scores[0];
        this.whiteWins = scores[1];
        
        // 设置窗口的大小、居中等属性
        setupWindowProperties();
        
        // 加载顶部的文件和帮助菜单
        setupTopMenu(); 
        
        // 重点知识：为什么要用 BorderLayout（边界布局）？
        // 它能把窗口划分成东西南北中 5 个区域。这样即使拉伸窗口，
        // 棋盘和控制面板的位置也不会串位。
        JPanel mainContainer = new JPanel();
        mainContainer.setLayout(new BorderLayout(10, 10)); // 10代表组件之间的空隙
        mainContainer.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // 把自己 (this) 当作监听器传给画板
        this.boardPanel = new BoardPanel(this.model, this); 
        
        // 组装右边那一长串控制面板
        JPanel rightSidePanel = createRightControlPanel();
        
        // 底部的状态提示
        this.statusLabel = new JLabel(" 欢迎来到五子棋游戏！");
        this.statusLabel.setBorder(BorderFactory.createEtchedBorder());
        
        // 往主容器里塞东西：中间放画板，东边放右侧面板，南边放状态栏
        mainContainer.add(this.boardPanel, BorderLayout.CENTER);
        mainContainer.add(rightSidePanel, BorderLayout.EAST);
        mainContainer.add(this.statusLabel, BorderLayout.SOUTH);
        
        this.add(mainContainer); 
    }

    private void setupWindowProperties() {
        this.setTitle("Java 课程设计 - 面向对象五子棋");
        int w = GameModel.BOARD_SIZE * BoardPanel.CELL_SIZE + BoardPanel.MARGIN * 2 + 300;
        int h = GameModel.BOARD_SIZE * BoardPanel.CELL_SIZE + BoardPanel.MARGIN * 2 + 80;
        this.setSize(w, h);
        
        // 记住这个属性：EXIT_ON_CLOSE，点叉叉的时候会把控制台的运行程序也关掉。
        // 如果不加这句，窗口没了但是后台程序还在偷偷跑。
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); 
        this.setLocationRelativeTo(null); 
        this.setResizable(false); 
    }

    private void setupTopMenu() {
        JMenuBar menuBar = new JMenuBar();
        
        JMenu gameMenu = new JMenu("游戏 (G)");
        JMenuItem restartMenu = new JMenuItem("重新开始");
        JMenuItem exitMenu = new JMenuItem("退出游戏");
        
        // 这里用到了 Java 8 的 Lambda 表达式 (e -> xxx)，
        // 写法比传统的 new ActionListener() 简洁多了。
        restartMenu.addActionListener(e -> restartGame());
        exitMenu.addActionListener(e -> System.exit(0)); // System.exit(0) 直接杀进程退出
        
        gameMenu.add(restartMenu);
        gameMenu.addSeparator(); // 加一条分割线好看点
        gameMenu.add(exitMenu);
        
        JMenu helpMenu = new JMenu("帮助 (H)");
        JMenuItem ruleMenu = new JMenuItem("游戏规则");
        
        ruleMenu.addActionListener(e -> {
            // 弹出一个专门的帮助窗口
            HelpDialog dialog = new HelpDialog(this);
            dialog.setVisible(true);
        });
        
        helpMenu.add(ruleMenu);
        
        menuBar.add(gameMenu);
        menuBar.add(helpMenu);
        this.setJMenuBar(menuBar);
    }

    /**
     * 拼接右侧那一长条的控制界面
     */
    private JPanel createRightControlPanel() {
        JPanel rightPanel = new JPanel();
        
        // 这里用了 BoxLayout的 Y_AXIS（垂直布局）。
        // FlowLayout 是一排排过去的，而 BoxLayout Y_AXIS 保证所有组件从上到下像叠罗汉一样。
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS)); 
        rightPanel.setPreferredSize(new Dimension(250, 0)); 

        // -- 模式选择 --
        JPanel modeBoxPanel = new JPanel(new FlowLayout());
        modeBoxPanel.setBorder(BorderFactory.createTitledBorder("选择模式"));
        String[] modes = {"双人对战", "简单人机", "困难人机"};
        JComboBox<String> modeComboBox = new JComboBox<>(modes); // 下拉框组件
        modeComboBox.addActionListener(e -> {
            // 获取下拉框选中的索引（0或1或2），赋值给变量
            this.gameMode = modeComboBox.getSelectedIndex(); 
            restartGame(); 
        });
        modeBoxPanel.add(modeComboBox);

        // -- 历史得分 --
        // GridLayout(2, 2) 意思是两行两列的网格
        JPanel scorePanel = new JPanel(new GridLayout(2, 2, 5, 5));
        scorePanel.setBorder(BorderFactory.createTitledBorder("历史战绩"));
        
        scorePanel.add(new JLabel("黑方胜利:"));
        this.blackScoreLabel = new JLabel(String.valueOf(this.blackWins));
        scorePanel.add(this.blackScoreLabel);
        
        scorePanel.add(new JLabel("白方胜利:"));
        this.whiteScoreLabel = new JLabel(String.valueOf(this.whiteWins));
        scorePanel.add(this.whiteScoreLabel);

        // -- 当前轮次 --
        JPanel turnPanel = new JPanel(new GridLayout(2, 1));
        turnPanel.setBorder(BorderFactory.createTitledBorder("当前状态"));
        this.currentTurnLabel = new JLabel("轮到黑棋落子", JLabel.CENTER);
        this.currentTurnLabel.setFont(new Font("宋体", Font.BOLD, 18));
        turnPanel.add(this.currentTurnLabel);

        // -- 滚动日志区 --
        this.logArea = new JTextArea();
        this.logArea.setEditable(false); // 不让玩家自己用键盘乱敲日志
        // JScrollPane 会自动给文本框加上滚轮
        JScrollPane scrollPane = new JScrollPane(this.logArea); 
        scrollPane.setBorder(BorderFactory.createTitledBorder("下棋记录"));

        // -- 按钮区 --
        JPanel buttonsPanel = new JPanel(new FlowLayout());
        JButton btnRestart = new JButton("重开");
        JButton btnUndo = new JButton("悔棋");

        btnRestart.addActionListener(e -> restartGame());
        btnUndo.addActionListener(e -> handleUndo());

        buttonsPanel.add(btnRestart);
        buttonsPanel.add(btnUndo);

        // 把这些面板全加到 rightPanel 里面，中间用 Box.createVerticalStrut(10) 加了 10 像素的间隔
        rightPanel.add(modeBoxPanel);
        rightPanel.add(Box.createVerticalStrut(10));
        rightPanel.add(scorePanel);
        rightPanel.add(Box.createVerticalStrut(10));
        rightPanel.add(turnPanel);
        rightPanel.add(Box.createVerticalStrut(10));
        rightPanel.add(scrollPane);
        rightPanel.add(Box.createVerticalStrut(10));
        rightPanel.add(buttonsPanel);

        return rightPanel;
    }

    /**
     * 当接收到画板传递过来的鼠标点击坐标时，走这里
     */
    @Override
    public void onBoardClicked(int x, int y) {
        if (this.model.isGameOver()) return; // 游戏结束不给下
        if (this.gameMode > 0 && !this.model.isBlackTurn()) return; // 人机模式下，如果没轮到你，屏蔽点击

        // addMove 如果成功，说明坐标没越界，且该位置没被下过
        if (this.model.addMove(x, y)) {
            String colorStr = this.model.isBlackTurn() ? "白方" : "黑方"; 
            printLog("玩家 " + colorStr + " 下在了: (" + x + ", " + y + ")");
            
            // 下完后查一下赢没赢
            if (this.model.checkWin(x, y)) {
                this.boardPanel.repaint(); // 刷新一遍画板把棋子显示出来
                processWin(); 
            } else {
                this.model.toggleTurn(); // 没赢就换下一个人的回合
                updateTurnLabel();
                this.boardPanel.repaint();
                
                // PVE模式下轮到电脑走
                if (this.gameMode > 0 && !this.model.isGameOver()) {
                    // 重要说明：为什么要用 Timer 延迟一下？
                    // 因为如果玩家一落子，电脑瞬间就落子，界面会看起来卡顿了一下。
                    // 加上 400 毫秒的延迟，就像是电脑在思考一样，也能让界面有时间先把玩家的棋子画出来。
                    Timer timer = new Timer(400, e -> letComputerPlay());
                    timer.setRepeats(false); // 只执行一次
                    timer.start();
                }
            }
        }
    }

    /**
     * 调用 AI 算下一步棋
     */
    private void letComputerPlay() {
        if (this.model.isGameOver()) return;
        
        Point computerMove = null;
        if (this.gameMode == 1) {
            computerMove = this.aiPlayer.getEasyMove(this.model);
        } else if (this.gameMode == 2) {
            computerMove = this.aiPlayer.getHardMove(this.model);
        }
        
        if (computerMove != null) {
            if (this.model.addMove(computerMove.x, computerMove.y)) {
                printLog("电脑白棋下在了: (" + computerMove.x + ", " + computerMove.y + ")");
                
                if (this.model.checkWin(computerMove.x, computerMove.y)) {
                    this.boardPanel.repaint();
                    processWin();
                } else {
                    this.model.toggleTurn();
                    updateTurnLabel();
                    this.boardPanel.repaint();
                }
            }
        }
    }

    /**
     * 有人获胜的处理逻辑
     */
    private void processWin() {
        this.model.setGameOver(true);
        String winnerText = "";
        
        if (this.model.isBlackTurn()) {
            winnerText = "黑方胜利！";
            this.blackWins++;
            this.blackScoreLabel.setText(String.valueOf(this.blackWins));
        } else {
            winnerText = "白方胜利！";
            this.whiteWins++;
            this.whiteScoreLabel.setText(String.valueOf(this.whiteWins));
        }
        
        printLog("对局结束，" + winnerText);
        this.statusLabel.setText(" " + winnerText);
        
        // 赢了之后，调用 C 同学的方法，把比分保存到本地硬盘里
        RecordManager.saveScores(this.blackWins, this.whiteWins);
        BoardExporter.exportBoardToTxt(this.model.getBoard(), this.model.getMoveCount(), winnerText);
        
        // 弹出一个信息对话框
        JOptionPane.showMessageDialog(this, winnerText, "游戏结束", JOptionPane.INFORMATION_MESSAGE);
    }

    private void printLog(String message) {
        String time = new SimpleDateFormat("HH:mm:ss").format(new Date());
        this.logArea.append("[" + time + "] " + message + "\n");
        // 把光标移动到文本区最后，这样日志滚动条就会一直保持在最底部
        this.logArea.setCaretPosition(this.logArea.getDocument().getLength());
    }

    private void updateTurnLabel() {
        if (this.model.isBlackTurn()) {
            this.currentTurnLabel.setText("轮到黑棋落子");
            this.currentTurnLabel.setForeground(Color.BLACK);
            this.statusLabel.setText(" 等待玩家操作...");
        } else {
            this.currentTurnLabel.setText("轮到白棋落子");
            this.currentTurnLabel.setForeground(Color.RED);
            this.statusLabel.setText(" 电脑思考中...");
        }
    }

    private void restartGame() {
        this.model.reset();
        this.logArea.setText(""); 
        printLog("点击了重开游戏按钮");
        updateTurnLabel();
        this.boardPanel.repaint();
    }

    private void handleUndo() {
        if (this.model.isGameOver()) return;
        if (this.model.getHistory().isEmpty()) {
            JOptionPane.showMessageDialog(this, "棋盘上还没下子，无法悔棋！");
            return;
        }
        
        int stepsToUndo = 1;
        // 如果是在跟电脑玩，要一次性撤销 2 步（电脑的一步和玩家的一步）。
        // 否则你撤销一步，回合又跑到电脑那边，电脑又会瞬间下了一步把你堵死。
        if (this.gameMode > 0 && this.model.isBlackTurn()) {
            if (this.model.getHistory().size() >= 2) {
                stepsToUndo = 2;
            }
        }
        
        for (int i = 0; i < stepsToUndo; i++) {
            this.model.undoMove();
        }
        
        printLog("执行了悔棋操作，退了 " + stepsToUndo + " 步");
        updateTurnLabel();
        this.boardPanel.repaint(); // 必须重画棋盘，不然撤销的棋子还在界面上
    }

    public static void main(String[] args) {
        try {
            // 让 Java 窗口不要长得像十年前的界面，使用操作系统的自带 UI 主题
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        // invokeLater 是为了多线程安全，Java Swing 强制要求所有更新界面的操作都在事件分发线程里执行
        SwingUtilities.invokeLater(() -> {
            GomokuGame window = new GomokuGame();
            window.setVisible(true); 
        });
    }
}
