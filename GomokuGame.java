import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 顶层主程序类 (MVC - Controller / Main UI)
 * 继承自 JFrame，是整个客户端软件的顶层容器和启动入口。
 * 承担了 Controller 控制器的职责，负责协调 Model (游戏数据)、View (棋盘画板)、AI 算法模块以及底层的数据持久化组件，
 * 是驱动整个生命周期运转的核心调度枢纽。
 */
public class GomokuGame extends JFrame implements BoardPanel.BoardClickListener {
    // 聚合系统三大核心子模块引用
    private GameModel model;      // 指向数据逻辑中枢
    private AIPlayer aiPlayer;    // 指向启发式算法引擎
    private BoardPanel boardPanel;// 指向负责视图渲染的画布
    
    // 软件的业务状态参数设定
    private int gameMode = 0; // 交互模式枚举 - 0: 传统双人模式, 1: 初级人机交互, 2: 高级人机交互
    private int blackWins = 0; // 黑方历史胜局总计
    private int whiteWins = 0; // 白方历史胜局总计
    
    // Swing 控件成员声明
    private JTextArea logArea;             // 滚动日志视窗区域
    private JLabel statusLabel;            // 底部运行状态指示器
    private JLabel blackScoreLabel;        // 计分板黑方比分展示器
    private JLabel whiteScoreLabel;        // 计分板白方比分展示器
    private JLabel currentTurnLabel;       // 顶部回合指示器

    public GomokuGame() {
        // 1. 在构造函数初期完成底层业务组件的实例化工作
        model = new GameModel();
        aiPlayer = new AIPlayer();
        
        // 2. 初始化持久化状态：调用系统级 I/O 读取往期战绩记录，恢复比分数据
        int[] scores = RecordManager.loadScores();
        blackWins = scores[0];
        whiteWins = scores[1];
        
        // 3. 构建操作系统级别的图形窗口句柄及其属性
        initWindow();
        
        // 4. 构建顶层布局管理器 BorderLayout
        JPanel container = new JPanel(new BorderLayout(10, 10));
        container.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // 实例化 View 视窗，通过 this 指针实现控制层的回调绑定
        boardPanel = new BoardPanel(model, this); 
        
        // 拼装挂载于容器东侧 (EAST) 的复杂交互控制面板
        JPanel sidePanel = createSidePanel();
        
        // 实例化底部的状态条组件
        statusLabel = new JLabel(" 欢迎来到五子棋对战系统 | 系统初始化就绪 ");
        statusLabel.setBorder(BorderFactory.createEtchedBorder());
        
        // 按方位要求，将三大核心 UI 块组合进顶层容器中
        container.add(boardPanel, BorderLayout.CENTER);
        container.add(sidePanel, BorderLayout.EAST);
        container.add(statusLabel, BorderLayout.SOUTH);
        
        add(container); 
    }

    /**
     * 封装底层窗口 UI 的常规初始化设定参数
     */
    private void initWindow() {
        setTitle("Java 高级五子棋博弈系统");
        // 动态计算最佳窗口分辨率，防止缩放导致的 UI 异常
        int width = GameModel.BOARD_SIZE * BoardPanel.CELL_SIZE + BoardPanel.MARGIN * 2 + 300;
        int height = GameModel.BOARD_SIZE * BoardPanel.CELL_SIZE + BoardPanel.MARGIN * 2 + 100;
        setSize(width, height);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); 
        setLocationRelativeTo(null); 
        setResizable(false); 
    }

    /**
     * 构建并返回右侧整合的控制面板 (Control Panel)
     * 布局剖析：利用 BoxLayout 的纵向堆叠特性 (Y_AXIS) ，将不同功能的子 JPanel 区块垂直封装，
     * 以达成良好的界面交互层级与模块化解耦。
     */
    private JPanel createSidePanel() {
        JPanel side = new JPanel();
        side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS)); 
        side.setPreferredSize(new Dimension(250, 0)); 

        // -- 子模块 I: 交互模式下拉选择器 --
        JPanel modePanel = new JPanel(new FlowLayout());
        modePanel.setBorder(BorderFactory.createTitledBorder("博弈模式配置"));
        JComboBox<String> modeBox = new JComboBox<>(new String[]{"PVP 双人对弈", "PVE 人机博弈 (初级)", "PVE 人机博弈 (高级)"});
        modeBox.addActionListener(e -> {
            gameMode = modeBox.getSelectedIndex(); 
            resetGame(); // 切换模式时自动重置内存矩阵
        });
        modePanel.add(modeBox);

        // -- 子模块 II: 战况比分面板 (集成持久化存储) --
        JPanel scorePanel = new JPanel(new GridLayout(2, 2, 5, 5));
        scorePanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "生涯战绩", TitledBorder.LEFT, TitledBorder.TOP));
        
        scorePanel.add(new JLabel("黑方(执先)胜局:"));
        blackScoreLabel = new JLabel(String.valueOf(blackWins));
        scorePanel.add(blackScoreLabel);
        
        scorePanel.add(new JLabel("白方(执后)胜局:"));
        whiteScoreLabel = new JLabel(String.valueOf(whiteWins));
        scorePanel.add(whiteScoreLabel);

        // -- 子模块 III: 动态回合状态灯 --
        JPanel infoPanel = new JPanel(new GridLayout(2, 1));
        infoPanel.setBorder(BorderFactory.createTitledBorder("博弈状态"));
        currentTurnLabel = new JLabel("指令等待：黑方回合", JLabel.CENTER);
        currentTurnLabel.setFont(new Font("微软雅黑", Font.BOLD, 18));
        currentTurnLabel.setForeground(Color.BLACK);
        infoPanel.add(currentTurnLabel);

        // -- 子模块 IV: 事件日志滚动流媒体视窗 --
        logArea = new JTextArea();
        logArea.setEditable(false); // 封锁用户底层键入权限
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane scrollPane = new JScrollPane(logArea); 
        scrollPane.setBorder(BorderFactory.createTitledBorder("系统运行日志"));

        // -- 子模块 V: 控制台按钮指令区 --
        JPanel btnPanel = new JPanel(new FlowLayout());
        JButton restartBtn = new JButton("重启系统");
        JButton undoBtn = new JButton("执行回退");
        JButton clearScoreBtn = new JButton("格式化比分");

        // 基于 Lambda 匿名函数映射点击事件至具体的底层控制器方法
        restartBtn.addActionListener(e -> resetGame());
        undoBtn.addActionListener(e -> undoMove());
        clearScoreBtn.addActionListener(e -> clearScores());

        btnPanel.add(restartBtn);
        btnPanel.add(undoBtn);
        btnPanel.add(clearScoreBtn);

        // 将实例化的五大子模块顺序塞入纵向布局流容器，并利用 VerticalStrut 撑起美学间距
        side.add(modePanel);
        side.add(Box.createVerticalStrut(10));
        side.add(scorePanel);
        side.add(Box.createVerticalStrut(10));
        side.add(infoPanel);
        side.add(Box.createVerticalStrut(10));
        side.add(scrollPane);
        side.add(Box.createVerticalStrut(10));
        side.add(btnPanel);

        return side;
    }

    /**
     * 重载自定义的回调事件，充当全局控制神经。
     * 当收到 BoardPanel 捕捉并映射好的鼠标逻辑坐标事件时，触发后续的核心业务链。
     */
    @Override
    public void onBoardClicked(int i, int j) {
        if (model.isGameOver()) {
            addLog("系统级拦截：博弈已分出胜负，请重启对局模块。");
            return;
        }
        // 并发拦截：当处于 PVE 人机模式，且当前非玩家操作权限阶段时，抛弃无意义的点击脉冲
        if (gameMode > 0 && !model.isBlackTurn()) return; 

        // 将合法坐标推送至模型层进行存储
        if (model.addMove(i, j)) {
            String colorStr = model.isBlackTurn() ? "白方棋子" : "黑方棋子"; 
            addLog(String.format("[执行流水号 %02d] 玩家签入 %s: (%d, %d)", model.getMoveCount(), colorStr, i, j));
            
            // 提交数据后立刻拉起底层扫描引擎，校验是否达成终止态（胜负已分）
            if (model.checkWin(i, j)) {
                boardPanel.repaint(); // 强制提交一帧重绘指令，确保终端棋子得以物理展示
                handleWin(); // 切入结算分支
            } else {
                model.toggleTurn(); // 指令下发，交出写控制权
                updateTurnUI();
                boardPanel.repaint();
                
                // 如果在 PVE 模式下，随即调度 AI 后台运算接口
                if (gameMode > 0 && !model.isGameOver()) {
                    // 使用 javax.swing.Timer 进行时间节流操作
                    // 模拟运算阻滞效果 (0.4秒)，优化拟人性并缓解 UI 线程的连续压力
                    javax.swing.Timer timer = new javax.swing.Timer(400, e -> aiMove());
                    timer.setRepeats(false); 
                    timer.start();
                }
            }
        }
    }

    /**
     * AI 运算调度函数。
     */
    private void aiMove() {
        if (model.isGameOver()) return;
        Point bestMove = null;
        
        // 获取配置环境状态并按需调用对应的 AI 内核级别
        if (gameMode == 1) bestMove = aiPlayer.getEasyMove(model);
        else if (gameMode == 2) bestMove = aiPlayer.getHardMove(model);
        
        if (bestMove != null) {
            // 将 AI 推演出的最佳落子点推送到 Model 进行最终存盘
            if (model.addMove(bestMove.x, bestMove.y)) {
                addLog(String.format("[执行流水号 %02d] AI(机器侧)计算并落子: (%d, %d)", model.getMoveCount(), bestMove.x, bestMove.y));
                if (model.checkWin(bestMove.x, bestMove.y)) {
                    boardPanel.repaint();
                    handleWin();
                } else {
                    model.toggleTurn();
                    updateTurnUI();
                    boardPanel.repaint();
                }
            }
        }
    }

    /**
     * 最终结算状态机的核心流转处理函数
     */
    private void handleWin() {
        // 全局状态机锁定
        model.setGameOver(true);
        String winner = model.isBlackTurn() ? "黑方(玩家端)" : "白方(机器端)";
        if (model.isBlackTurn()) blackWins++; else whiteWins++;
        
        // 强更界面元素数据
        blackScoreLabel.setText(String.valueOf(blackWins));
        whiteScoreLabel.setText(String.valueOf(whiteWins));
        
        // 【关键步骤】系统调用 I/O 进行同步持久化，保证游戏状态即便面临异常断电也能完成记录
        RecordManager.saveScores(blackWins, whiteWins);
        
        addLog("系统通告 >>> 胜负已分！" + winner + " 斩获最终胜利！");
        statusLabel.setText(" 博弈终止 - " + winner + "判定为赢家 ");
        
        // 弹出最高优先级的阻塞式提示窗
        JOptionPane.showMessageDialog(this, "博弈终止，" + winner + " 获得了压倒性胜利！", "系统对局结算", JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * 控制台日志追加挂载函数，内置本地时间戳组装机制
     */
    private void addLog(String msg) {
        String time = new SimpleDateFormat("HH:mm:ss").format(new Date());
        logArea.append("[" + time + "] " + msg + "\n");
        // 控制游标，强制使得底层滚动条焦点对齐最新产生的行区
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    /**
     * 辅助 UI 组件颜色渲染状态刷新
     */
    private void updateTurnUI() {
        if (model.isBlackTurn()) {
            currentTurnLabel.setText("指令等待：黑方回合");
            currentTurnLabel.setForeground(Color.BLACK);
            statusLabel.setText(" 当前状态：处于玩家输入监听...");
        } else {
            currentTurnLabel.setText("指令等待：白方回合");
            currentTurnLabel.setForeground(new Color(100, 100, 100)); 
            statusLabel.setText(" 当前状态：处于机器推演状态...");
        }
    }

    private void resetGame() {
        model.reset();
        logArea.setText(""); 
        addLog("系统日志 >>> 数据缓冲清空，系统完成硬重启");
        updateTurnUI();
        boardPanel.repaint();
    }

    /**
     * 单步回退系统 (回溯控制机制)
     */
    private void undoMove() {
        if (model.isGameOver()) return;
        if (model.getHistory().isEmpty()) {
            JOptionPane.showMessageDialog(this, "系统异常拦截：堆栈内不存在任何合法落子记录指令！");
            return;
        }
        
        // 复杂补偿运算：在 PVE 下发生回退时，如果仅退一步将导致将控制权重新交回 AI 而导致无限覆写，
        // 因而需要采用跨步回溯机制，直接退回 2 个状态点到达上一周期的最初发起端。
        int stepsToUndo = (gameMode > 0 && model.isBlackTurn() && model.getHistory().size() >= 2) ? 2 : 1;
        for (int i = 0; i < stepsToUndo; i++) {
            model.undoMove();
        }
        addLog("指令回调 >>> 基于系统堆栈完成了 " + stepsToUndo + " 条落子事件撤回。");
        updateTurnUI();
        boardPanel.repaint();
    }

    private void clearScores() {
        blackWins = 0;
        whiteWins = 0;
        blackScoreLabel.setText("0");
        whiteScoreLabel.setText("0");
        // 下达抹除指令，将双0持久化至物理层面覆盖原始纪录
        RecordManager.saveScores(0, 0); 
        addLog("安全提示 >>> 全局物理级历史记录已完成深层格式化。");
    }

    /**
     * Client Application Entry Point
     * 程序的最终初始化入口点，规范化采用 Event Dispatch Thread (EDT) 防止因死锁诱发的 UI 无响应
     */
    public static void main(String[] args) {
        try {
            // 将 Java 的金属感 L&F(LookAndFeel) 配置覆写为底层宿主系统的原生窗体风格，带来现代化拟合观感
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        // 将顶层对象的创建压入安全队列
        SwingUtilities.invokeLater(() -> {
            GomokuGame game = new GomokuGame();
            game.setVisible(true); // 使能句柄渲染可见
        });
    }
}
