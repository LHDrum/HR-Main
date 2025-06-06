package main;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import com.formdev.flatlaf.FlatIntelliJLaf;


public class PayrollApp extends JFrame {
    private CardLayout cardLayout;
    private JPanel cardPanel;

    private SummaryPage summaryPage;
    private AttendancePage attendancePage;
    private EmployeeManagementPage employeeManagementPage;
    private EmploymentContractPage employmentContractPage;
    private SettingsPage settingsPage;
    private AnnualLeavePage annualLeavePage;
    private DeductionsPage deductionsPage; // DeductionsPage 멤버 변수

    private PayrollManager payrollManager;
    private Employee lastSelectedEmployeeForContract;
    private Payroll lastContractualPayrollForContract;

    // --- 디자인 및 기능 관련 상수 및 변수 ---
    private static final Color SIDEBAR_BACKGROUND = new Color(90, 125, 175);
    private static final Color SIDEBAR_TEXT_COLOR = Color.WHITE;
    private static final Color SIDEBAR_BUTTON_HOVER_BACKGROUND = new Color(110, 145, 195);
    private static final Color SIDEBAR_BUTTON_SELECTED_BACKGROUND = new Color(0, 123, 255);
    private static final Color SIDEBAR_BUTTON_SELECTED_BORDER_COLOR = Color.WHITE;
    private static final Color SIDEBAR_RIGHT_BORDER_COLOR = new Color(70, 100, 150);
    private static final Color CONTENT_BACKGROUND = Color.WHITE;

    // 폰트 정의
    private static final Font GLOBAL_FONT = new Font("맑은 고딕", Font.PLAIN, 13);
    private static final Font GLOBAL_BOLD_FONT = new Font("맑은 고딕", Font.BOLD, 13);
    private static final Font SIDEBAR_TITLE_FONT = new Font("맑은 고딕", Font.BOLD, 20);
    private static final Font SIDEBAR_BUTTON_FONT = new Font("맑은 고딕", Font.BOLD, 15);
    private static final Font SIDEBAR_BUTTON_SELECTED_FONT = new Font("맑은 고딕", Font.BOLD, SIDEBAR_BUTTON_FONT.getSize() + 1);
    private static final Font TITLE_FONT = new Font("맑은 고딕", Font.BOLD, 18);

    private SidebarButton selectedSidebarButton = null;
    private final int SELECTED_BUTTON_BORDER_THICKNESS = 4;
    private final int SIDEBAR_WIDTH = 150;
    private final int TEXT_AFTER_BORDER_PADDING = 5;

    private JLabel sidebarTitleLabel;
    private List<SidebarButton> navigationButtons = new ArrayList<>();


    public PayrollApp() {
        setTitle("Kit-Works 급여 관리 시스템");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        setUIFont(new javax.swing.plaf.FontUIResource(GLOBAL_FONT));
        payrollManager = new PayrollManager();

        JPanel sidebarPanel = createSidebarPanel();
        add(sidebarPanel, BorderLayout.WEST);

        cardLayout = new CardLayout();
        cardPanel = new JPanel(cardLayout);
        cardPanel.setBackground(CONTENT_BACKGROUND);

        // 각 페이지 객체 생성
        summaryPage = new SummaryPage(payrollManager, this);
        attendancePage = new AttendancePage(cardPanel, cardLayout, payrollManager, summaryPage);
        employeeManagementPage = new EmployeeManagementPage(payrollManager, this, () -> showCard("Summary"));
        deductionsPage = new DeductionsPage(payrollManager); // 신규 페이지 생성
        annualLeavePage = new AnnualLeavePage(payrollManager);
        employmentContractPage = new EmploymentContractPage(this);
        settingsPage = new SettingsPage(payrollManager);

        // CardLayout에 페이지 추가
        cardPanel.add(summaryPage, "Summary");
        cardPanel.add(attendancePage, "Attendance");
        cardPanel.add(employeeManagementPage, "EmployeeManagement");
        cardPanel.add(deductionsPage, "Deductions"); // 신규 페이지 연결
        cardPanel.add(annualLeavePage, "AnnualLeave");
        cardPanel.add(employmentContractPage, "EmploymentContract");
        cardPanel.add(settingsPage, "Settings");

        add(cardPanel, BorderLayout.CENTER);

        setMinimumSize(new Dimension(1200, 800));
        setPreferredSize(new Dimension(1280, 850));
        pack();
        setLocationRelativeTo(null);
        showCard("Summary");
    }

    private JPanel createSidebarPanel() {
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(SIDEBAR_BACKGROUND);
        sidebar.setPreferredSize(new Dimension(SIDEBAR_WIDTH, 0));
        sidebar.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, SIDEBAR_RIGHT_BORDER_COLOR));

        sidebarTitleLabel = new JLabel("Kit-Works");
        sidebarTitleLabel.setFont(SIDEBAR_TITLE_FONT);
        sidebarTitleLabel.setForeground(SIDEBAR_TEXT_COLOR);
        sidebarTitleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        sidebarTitleLabel.setBorder(new EmptyBorder(25, TEXT_AFTER_BORDER_PADDING + SELECTED_BUTTON_BORDER_THICKNESS, 25, 5));

        JPanel titleContainer = new JPanel(new BorderLayout());
        titleContainer.setBackground(SIDEBAR_BACKGROUND);
        titleContainer.add(sidebarTitleLabel, BorderLayout.WEST);
        titleContainer.setAlignmentX(Component.LEFT_ALIGNMENT);
        titleContainer.setMaximumSize(new Dimension(Integer.MAX_VALUE, sidebarTitleLabel.getPreferredSize().height + 50));
        sidebar.add(titleContainer);

        navigationButtons.clear();

        // --- 사이드바 메뉴 순서 및 번호 재조정 ---
        SidebarButton summaryButton = new SidebarButton("1. 급여 내역");
        summaryButton.addActionListener(e -> { setSelectedButton(summaryButton); showCard("Summary"); });
        navigationButtons.add(summaryButton);
        sidebar.add(summaryButton);

        SidebarButton attendanceButton = new SidebarButton("2. 근태/급여 계산");
        attendanceButton.addActionListener(e -> { setSelectedButton(attendanceButton); showCard("Attendance"); });
        navigationButtons.add(attendanceButton);
        sidebar.add(attendanceButton);

        SidebarButton employeeManagementButton = new SidebarButton("3. 직원 정보");
        employeeManagementButton.addActionListener(e -> { setSelectedButton(employeeManagementButton); showCard("EmployeeManagement"); });
        navigationButtons.add(employeeManagementButton);
        sidebar.add(employeeManagementButton);

        SidebarButton deductionsButton = new SidebarButton("4. 4대보험/세금");
        deductionsButton.addActionListener(e -> {
            setSelectedButton(deductionsButton);
            showCard("Deductions");
        });
        navigationButtons.add(deductionsButton);
        sidebar.add(deductionsButton);

        SidebarButton annualLeaveButton = new SidebarButton("5. 연차 관리");
        annualLeaveButton.addActionListener(e -> {
            setSelectedButton(annualLeaveButton);
            showCard("AnnualLeave");
        });
        navigationButtons.add(annualLeaveButton);
        sidebar.add(annualLeaveButton);

        SidebarButton contractButton = new SidebarButton("6. 근로계약서");
        contractButton.addActionListener(e -> {
            setSelectedButton(contractButton);
            if (lastSelectedEmployeeForContract != null && lastContractualPayrollForContract != null) {
                showEmploymentContractPage(lastSelectedEmployeeForContract, lastContractualPayrollForContract);
            } else {
                int option = JOptionPane.showConfirmDialog(this,
                        "표시할 직원 정보가 없습니다.\n3페이지에서 직원을 선택하고 '근로계약서 작성' 버튼을 이용하시거나,\n이대로 빈 계약서 양식을 여시겠습니까?",
                        "직원 선택 필요", JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE);
                if (option == JOptionPane.YES_OPTION) {
                    employmentContractPage.displayPageForEmployee(null, null);
                    cardLayout.show(cardPanel, "EmploymentContract");
                }
            }
        });
        navigationButtons.add(contractButton);
        sidebar.add(contractButton);

        SidebarButton settingsButton = new SidebarButton("설정");
        settingsButton.addActionListener(e -> {
            setSelectedButton(settingsButton);
            showCard("Settings");
        });
        navigationButtons.add(settingsButton);
        sidebar.add(settingsButton);

        sidebar.add(Box.createVerticalGlue());

        if (!navigationButtons.isEmpty()) {
            setSelectedButton(navigationButtons.get(0));
        }
        return sidebar;
    }

    private void setSelectedButton(SidebarButton button) {
        if (selectedSidebarButton != null) {
            selectedSidebarButton.setSelectedState(false);
        }
        selectedSidebarButton = button;
        if (selectedSidebarButton != null) {
            selectedSidebarButton.setSelectedState(true);
        }
    }

    class SidebarButton extends JButton {
        private boolean isSelected = false;

        public SidebarButton(String text) {
            super(text);
            setFont(SIDEBAR_BUTTON_FONT);
            setForeground(SIDEBAR_TEXT_COLOR);
            setBackground(SIDEBAR_BACKGROUND);
            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setOpaque(true);
            setHorizontalAlignment(SwingConstants.LEFT);

            setBorder(new EmptyBorder(12, SELECTED_BUTTON_BORDER_THICKNESS + TEXT_AFTER_BORDER_PADDING, 12, 5));
            setAlignmentX(Component.LEFT_ALIGNMENT);
            setMaximumSize(new Dimension(Integer.MAX_VALUE, getFontMetrics(SIDEBAR_BUTTON_SELECTED_FONT).getHeight() + 24 + 5));

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    if (!isSelected && isEnabled()) {
                        setBackground(SIDEBAR_BUTTON_HOVER_BACKGROUND);
                    }
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    if (!isSelected) {
                        setBackground(SIDEBAR_BACKGROUND);
                    }
                }
            });
        }

        public void setSelectedState(boolean selected) {
            this.isSelected = selected;
            if (selected) {
                setBackground(SIDEBAR_BUTTON_SELECTED_BACKGROUND);
                setFont(SIDEBAR_BUTTON_SELECTED_FONT);
            } else {
                setBackground(SIDEBAR_BACKGROUND);
                setFont(SIDEBAR_BUTTON_FONT);
            }
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            if (isSelected) {
                g2.setColor(SIDEBAR_BUTTON_SELECTED_BACKGROUND);
            } else if (getModel().isRollover() && isEnabled()) {
                g2.setColor(SIDEBAR_BUTTON_HOVER_BACKGROUND);
            } else {
                g2.setColor(getBackground());
            }
            g2.fillRect(0, 0, getWidth(), getHeight());

            if (isSelected) {
                g2.setColor(SIDEBAR_BUTTON_SELECTED_BORDER_COLOR);
                g2.fillRect(0, 0, SELECTED_BUTTON_BORDER_THICKNESS, getHeight());
            }

            g2.dispose();

            super.paintComponent(g);
        }
    }

    public void showCard(String cardName) {
        cardLayout.show(cardPanel, cardName);
        if (cardName.equals("Summary")) {
            if (summaryPage != null) summaryPage.refreshTableData();
        } else if (cardName.equals("Attendance")) {
            if (attendancePage != null) attendancePage.refreshEmployeeComboBox();
        } else if (cardName.equals("EmployeeManagement")) {
            if (employeeManagementPage != null) {
                employeeManagementPage.loadEmployeeTable("");
            }
        } else if (cardName.equals("Deductions")) {
            if (deductionsPage != null) {
                deductionsPage.refreshEmployeeComboBox();
            }
        } else if (cardName.equals("AnnualLeave")) {
            if (annualLeavePage != null) {
                annualLeavePage.refreshEmployeeComboBox();
            }
        } else if (cardName.equals("Settings")) {
            // No specific action needed on show
        }
    }

    public void showEmploymentContractPage(Employee employee, Payroll contractualPayroll) {
        this.lastSelectedEmployeeForContract = employee;
        this.lastContractualPayrollForContract = contractualPayroll;

        if (employmentContractPage != null) {
            employmentContractPage.displayPageForEmployee(employee, contractualPayroll);
            cardLayout.show(cardPanel, "EmploymentContract");
        } else {
            JOptionPane.showMessageDialog(this, "계약서 페이지를 로드할 수 없습니다.", "오류", JOptionPane.ERROR_MESSAGE);
        }
    }

    public PayrollManager getPayrollManager() {
        return this.payrollManager;
    }

    public static void setUIFont(javax.swing.plaf.FontUIResource f) {
        java.util.Enumeration<Object> keys = UIManager.getDefaults().keys();
        while (keys.hasMoreElements()) {
            Object key = keys.nextElement();
            Object value = UIManager.get(key);
            if (value instanceof javax.swing.plaf.FontUIResource) {
                UIManager.put(key, f);
            }
        }
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(new FlatIntelliJLaf());
            UIManager.put("Button.arc", 8);
            UIManager.put("Component.arc", 8);
            UIManager.put("ProgressBar.arc", 8);
            UIManager.put("TextComponent.arc", 5);
        } catch (Exception e) {
            System.err.println("Failed to initialize LaF: " + e.getMessage());
            e.printStackTrace();
        }

        SwingUtilities.invokeLater(() -> {
            PayrollApp app = new PayrollApp();
            app.setVisible(true);
        });
    }
}