import javax.swing.*;
import java.awt.*;

/**
 * 规则帮助窗口类
 * 为什么单独写一个类？
 * 因为如果你直接用 JOptionPane 弹窗，排版会很乱。
 * 继承 JDialog，我们可以把一条条规则用 JLabel 写好，整齐地排列在界面上。
 */
public class HelpDialog extends JDialog {

    public HelpDialog(JFrame parent) {
        // 设置它为模态对话框（第三个参数 true），意思是不关掉这个窗口，你就点不了后面的主窗口。
        super(parent, "五子棋规则与操作指南", true);
        
        setupDialog();
    }

    /**
     * 拼接界面的控件
     */
    private void setupDialog() {
        this.setSize(380, 280);
        this.setLocationRelativeTo(getParent()); // 居中显示在父窗口上
        this.setResizable(false); // 固定大小，不然拉长了很难看
        
        // 我们用 JPanel 当背景板
        JPanel contentPanel = new JPanel();
        // 同样用 BoxLayout 的垂直方向，这样加入的标签就会像列表一样往下排
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));
        
        // 一行一行地把 JLabel 添加进去
        JLabel titleLabel = new JLabel("=== 核心对弈规则 ===");
        titleLabel.setFont(new Font("微软雅黑", Font.BOLD, 16));
        titleLabel.setForeground(Color.BLUE);
        
        JLabel rule1 = new JLabel("1. 对局双方各执一色棋子，黑先、白后。");
        JLabel rule2 = new JLabel("2. 每次只能在一个空白交叉点落下一颗棋子。");
        JLabel rule3 = new JLabel("3. 最先在棋盘横向、竖向、或斜向上形成");
        JLabel rule4 = new JLabel("   连续的五颗同色棋子的一方为胜。");
        JLabel rule5 = new JLabel("4. 若棋盘被下满且无任何一方达到五连，则平局。");
        
        JLabel space = new JLabel(" "); // 拿一个空文本来充当换行间距
        
        JLabel tipLabel = new JLabel("操作提示：");
        tipLabel.setFont(new Font("微软雅黑", Font.BOLD, 14));
        JLabel rule6 = new JLabel("人机模式下点击【悔棋】会自动连退两步。");
        JLabel rule7 = new JLabel("右下角会自动记录您的总胜率，退出不会丢失。");

        // 统一设置字体
        Font textFont = new Font("宋体", Font.PLAIN, 14);
        rule1.setFont(textFont);
        rule2.setFont(textFont);
        rule3.setFont(textFont);
        rule4.setFont(textFont);
        rule5.setFont(textFont);
        rule6.setFont(textFont);
        rule7.setFont(textFont);

        // 装载组件
        contentPanel.add(titleLabel);
        contentPanel.add(Box.createVerticalStrut(10)); 
        contentPanel.add(rule1);
        contentPanel.add(Box.createVerticalStrut(5));
        contentPanel.add(rule2);
        contentPanel.add(Box.createVerticalStrut(5));
        contentPanel.add(rule3);
        contentPanel.add(rule4);
        contentPanel.add(Box.createVerticalStrut(5));
        contentPanel.add(rule5);
        contentPanel.add(space);
        contentPanel.add(tipLabel);
        contentPanel.add(rule6);
        contentPanel.add(rule7);

        // 底部做一个按钮面板
        JPanel buttonPanel = new JPanel();
        JButton closeButton = new JButton("我了解了");
        closeButton.addActionListener(e -> {
            // dispose() 是释放窗体内存的方法，也就是关掉窗口
            this.dispose(); 
        });
        buttonPanel.add(closeButton);

        // 整体合并起来
        this.setLayout(new BorderLayout());
        this.add(contentPanel, BorderLayout.CENTER);
        this.add(buttonPanel, BorderLayout.SOUTH);
    }
}
