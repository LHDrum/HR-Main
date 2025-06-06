package main;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.TableModelEvent;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.*;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.ss.usermodel.BorderStyle;


public class EmployeeManagementPage extends JPanel {
    private PayrollManager payrollManager;
    private Runnable returnToSummaryPage;
    private PayrollApp payrollApp;


    private JComboBox<String> departmentComboBox;
    private JTextField nameField, residentRegNoField, phoneField, addressField, hireDateField, salaryChangeDateField;
    private JTextField annualSalaryField, hourlyWageField, monthlyBasicSalaryField, fixedOvertimeAllowanceField;
    private JTextField bonusField, otherAllowanceField, mealAllowanceField, vehicleMaintenanceFeeField;
    private JTextField researchDevelopmentExpenseField, childcareAllowanceField, totalMonthlyPayField;
    private JTextField workLocationField, siteLocationField;

    private JTextField searchField;
    private JButton searchButton, addButton, updateButton, deleteButton, clearButton, backButton;
    private JButton addRowButton;
    private JButton exportTableButton;
    private JButton createContractButton;


    private JTable employeeTable;
    private DefaultTableModel tableModel;
    private TableRowSorter<TableModel> sorter;
    private int selectedEmployeeDbId = -1;
    private List<Employee> displayedEmployeesInTableOrder;
    private Set<Integer> modifiedRowModelIndices;

    private final DecimalFormat numberFormatter = new DecimalFormat("#,###");
    private boolean isUpdatingFromForm = false;
    private boolean isUpdatingFromTable = false;
    private boolean isProgrammaticChange = false;

    private final float FONT_SCALE_FACTOR = 1.2f;
    private java.awt.Font enlargedFont;
    private java.awt.Font enlargedFontBold;
    private java.awt.Font totalsRowFont;


    // 급여 계산 시 사용될 상수 정의
    private static final double HOURS_FOR_BASIC_PAY_CALC = 209.0;
    private static final double TOTAL_WORK_HOURS_DIVISOR = 224.0;
    private static final double FIXED_OVERTIME_HOURS_PARAM = 10.0;


    private static final int COL_NO = 0; private static final int COL_HIRE_DATE = 1; private static final int COL_NAME = 2;
    private static final int COL_DEPARTMENT = 3; private static final int COL_RESIDENT_REG_NO = 4; private static final int COL_PHONE = 5;
    private static final int COL_ADDRESS = 6; private static final int COL_ANNUAL_SALARY = 7; private static final int COL_MONTHLY_BASIC = 8;
    private static final int COL_FIXED_OVERTIME = 9; private static final int COL_ADDITIONAL_OVERTIME = 10; private static final int COL_BONUS = 11;
    private static final int COL_OTHER_ALLOWANCE = 12; private static final int COL_MEAL_ALLOWANCE = 13; private static final int COL_VEHICLE_FEE = 14;
    private static final int COL_RESEARCH_EXPENSE = 15; private static final int COL_CHILDCARE_ALLOWANCE = 16; private static final int COL_TOTAL_MONTHLY_PAY = 17;

    // 합계 행에 포함될 컬럼 인덱스들
    private final int[] sumColumnIndices = {
            COL_ANNUAL_SALARY, COL_MONTHLY_BASIC, COL_FIXED_OVERTIME, COL_ADDITIONAL_OVERTIME,
            COL_BONUS, COL_OTHER_ALLOWANCE, COL_MEAL_ALLOWANCE, COL_VEHICLE_FEE,
            COL_RESEARCH_EXPENSE, COL_CHILDCARE_ALLOWANCE, COL_TOTAL_MONTHLY_PAY
    };


    private int scale(int value) {
        return (int) (value * FONT_SCALE_FACTOR);
    }

    public EmployeeManagementPage(PayrollManager payrollManager, PayrollApp payrollApp, Runnable returnToSummaryPage) {
        this.payrollManager = payrollManager;
        this.payrollApp = payrollApp;
        this.returnToSummaryPage = returnToSummaryPage;

        java.awt.Font baseFont = new JLabel().getFont();
        float newBaseSize = baseFont.getSize() * FONT_SCALE_FACTOR;
        enlargedFont = baseFont.deriveFont(newBaseSize);
        enlargedFontBold = baseFont.deriveFont(Font.BOLD, newBaseSize);
        totalsRowFont = enlargedFontBold; // 합계 행 폰트

        this.displayedEmployeesInTableOrder = new ArrayList<>();
        this.modifiedRowModelIndices = new HashSet<>();
        setLayout(new BorderLayout(scale(10), scale(10)));
        initComponents();
        loadEmployeeTable(""); // 내부에서 addOrUpdateTotalsRow() 호출
        addFormListeners();
        addTableListeners();
    }

    private String formatNumber(long number) { return numberFormatter.format(number); }
    private String formatNumber(double number) { return numberFormatter.format(Math.round(number)); }
    private long parseFormattedNumber(String formattedNumber) throws NumberFormatException {
        if (formattedNumber == null || formattedNumber.trim().isEmpty() || formattedNumber.equals("-") || formattedNumber.equals("오류")) return 0;
        try {
            return numberFormatter.parse(formattedNumber.trim()).longValue();
        } catch (ParseException e) {
            try {
                return Long.parseLong(formattedNumber.trim().replace(",", ""));
            } catch (NumberFormatException nfe) {
                System.err.println("Error parsing number: " + formattedNumber + " - " + nfe.getMessage());
                throw nfe;
            }
        }
    }

    private void applyDateFormatting(JTextField dateField) {
        if (isProgrammaticChange) return;

        final String originalText = dateField.getText();
        final int originalCaretPos = dateField.getCaretPosition();

        String digits = originalText.replaceAll("[^0-9]", "");
        String formattedText = digits;

        if (digits.length() >= 5) {
            formattedText = digits.substring(0, 4) + "-" + digits.substring(4);
        }
        if (digits.length() >= 7) {
            String monthPart = digits.substring(4, Math.min(6, digits.length()));
            formattedText = digits.substring(0, 4) + "-" + monthPart + "-" + digits.substring(4 + monthPart.length());
        }

        if (formattedText.length() > 10) {
            formattedText = formattedText.substring(0, 10);
        }

        if (!originalText.equals(formattedText)) {
            isProgrammaticChange = true;
            final String finalTextToSet = formattedText;

            SwingUtilities.invokeLater(() -> {
                dateField.setText(finalTextToSet);
                int newCaretPos = originalCaretPos;
                int hyphensInOriginal = (int) originalText.chars().filter(c -> c == '-').count();
                int hyphensInFormatted = (int) finalTextToSet.chars().filter(c -> c == '-').count();
                int diffHyphens = hyphensInFormatted - hyphensInOriginal;

                if (diffHyphens > 0) {
                    int originalDigitsBeforeCaret = 0;
                    for (int i = 0; i < originalCaretPos; i++) {
                        if (Character.isDigit(originalText.charAt(i))) {
                            originalDigitsBeforeCaret++;
                        }
                    }
                    int currentDigitsCounted = 0;
                    int tempNewPos = 0;
                    for (int i = 0; i < finalTextToSet.length(); i++) {
                        tempNewPos = i + 1;
                        if (Character.isDigit(finalTextToSet.charAt(i))) {
                            currentDigitsCounted++;
                        }
                        if (currentDigitsCounted == originalDigitsBeforeCaret) {
                            if (tempNewPos < finalTextToSet.length() && finalTextToSet.charAt(tempNewPos-1) != '-' &&
                                    finalTextToSet.charAt(tempNewPos) == '-' &&
                                    (originalText.length() < finalTextToSet.length() ||
                                            (originalText.length() >=tempNewPos && originalText.charAt(tempNewPos) != '-'))) {
                                newCaretPos = tempNewPos +1;
                                break;
                            } else {
                                newCaretPos = tempNewPos;
                                break;
                            }
                        }
                    }
                    if (currentDigitsCounted < originalDigitsBeforeCaret) {
                        newCaretPos = finalTextToSet.length();
                    }
                } else if (diffHyphens < 0) {
                    newCaretPos = Math.min(originalCaretPos, finalTextToSet.length());
                } else {
                    newCaretPos = originalCaretPos;
                }
                dateField.setCaretPosition(Math.min(newCaretPos, finalTextToSet.length()));
                isProgrammaticChange = false;
            });
        }
    }


    private void applyResidentRegNoFormatting(JTextField field) {
        if (isProgrammaticChange) return;
        final String originalText = field.getText();
        final int originalCaretPos = field.getCaretPosition();

        String text = originalText.replaceAll("[^0-9]", "");
        String formattedText = text;

        if (text.length() >= 7) {
            formattedText = text.substring(0, 6) + "-" + text.substring(6);
        }
        if (formattedText.length() > 14) {
            formattedText = formattedText.substring(0, 14);
        }

        if (!originalText.equals(formattedText)) {
            isProgrammaticChange = true;
            final String finalTextToSet = formattedText;
            SwingUtilities.invokeLater(() -> {
                field.setText(finalTextToSet);
                int newCaretPos = originalCaretPos;
                if (finalTextToSet.length() > originalText.length() && originalText.length() == 6 && originalCaretPos == 6 && finalTextToSet.charAt(6) == '-') {
                    newCaretPos = 7;
                } else if (finalTextToSet.length() < originalText.length() && originalCaretPos > finalTextToSet.length()){
                    newCaretPos = finalTextToSet.length();
                } else if (finalTextToSet.length() > originalText.length()){
                    if (originalCaretPos >=6 && finalTextToSet.indexOf('-') == 6) newCaretPos++;
                }
                field.setCaretPosition(Math.min(newCaretPos, finalTextToSet.length()));
                isProgrammaticChange = false;
            });
        }
    }

    private void applyPhoneFormatting(JTextField field) {
        if (isProgrammaticChange) return;
        final String originalTextInField = field.getText();
        final int originalCaretPos = field.getCaretPosition();

        String text = originalTextInField.replaceAll("[^0-9]", "");
        String formattedText = text;

        if (text.length() >= 2) {
            if (text.startsWith("02")) {
                if (text.length() <= 2) formattedText = text;
                else if (text.length() <= 6) formattedText = text.substring(0, 2) + "-" + text.substring(2);
                else if (text.length() <= 10) formattedText = text.substring(0, 2) + "-" + text.substring(2, Math.min(6, text.length())) + "-" + text.substring(Math.min(6, text.length()));
                else formattedText = text.substring(0, 2) + "-" + text.substring(2, 6) + "-" + text.substring(6, 10);
            } else if (text.startsWith("010")) {
                if (text.length() <= 3) formattedText = text;
                else if (text.length() <= 7) formattedText = text.substring(0, 3) + "-" + text.substring(3);
                else if (text.length() <= 11) formattedText = text.substring(0, 3) + "-" + text.substring(3, Math.min(7, text.length())) + "-" + text.substring(Math.min(7, text.length()));
                else formattedText = text.substring(0, 3) + "-" + text.substring(3, 7) + "-" + text.substring(7, 11);
            } else {
                if (text.length() <= 3) { formattedText = text; }
                else if (text.length() <= 7) { formattedText = text.substring(0,3) + "-" + text.substring(3); }
                else if (text.length() <=11) { formattedText = text.substring(0,3) + "-" + text.substring(3, Math.min(7, text.length())) + "-" + text.substring(Math.min(7, text.length()));}
                else { formattedText = text.substring(0,3) + "-" + text.substring(3, 7) + "-" + text.substring(7, 11); }
            }
        }
        if (formattedText.length() > 13) {
            formattedText = formattedText.substring(0, 13);
        }

        if (!originalTextInField.equals(formattedText)) {
            isProgrammaticChange = true;
            final String fTextToSet = formattedText;
            SwingUtilities.invokeLater(() -> {
                field.setText(fTextToSet);
                int newCaretPos = originalCaretPos;
                int hyphensInOriginal = (int) originalTextInField.chars().filter(ch -> ch == '-').count();
                int hyphensInFormatted = (int) fTextToSet.chars().filter(ch -> ch == '-').count();
                int diffHyphens = hyphensInFormatted - hyphensInOriginal;

                if (diffHyphens != 0) {
                    int digitsBeforeCaretOriginal = 0;
                    for (int i = 0; i < originalCaretPos; i++) {
                        if (Character.isDigit(originalTextInField.charAt(i))) {
                            digitsBeforeCaretOriginal++;
                        }
                    }
                    int currentDigits = 0;
                    newCaretPos = fTextToSet.length();
                    for (int i = 0; i < fTextToSet.length(); i++) {
                        if (Character.isDigit(fTextToSet.charAt(i))) {
                            currentDigits++;
                        }
                        if (currentDigits == digitsBeforeCaretOriginal) {
                            if (i + 1 < fTextToSet.length() && fTextToSet.charAt(i + 1) == '-' && diffHyphens > 0 && originalCaretPos == i + 1 - diffHyphens) {
                                newCaretPos = i + 2;
                            } else {
                                newCaretPos = i + 1;
                            }
                            break;
                        }
                    }
                }
                field.setCaretPosition(Math.max(0, Math.min(newCaretPos, fTextToSet.length())));
                isProgrammaticChange = false;
            });
        }
    }

    private void formatNumericFieldOnFocusLost(JTextField field) {
        if (isProgrammaticChange || isUpdatingFromTable) return;
        try {
            String text = field.getText().replace(",", "");
            if (!text.isEmpty()) {
                long value = Long.parseLong(text);
                isProgrammaticChange = true;
                field.setText(formatNumber(value));
                isProgrammaticChange = false;
            } else {
                isProgrammaticChange = true;
                field.setText(formatNumber(0));
                isProgrammaticChange = false;
            }
        } catch (NumberFormatException ex) {
            isProgrammaticChange = true;
            field.setText(formatNumber(0));
            isProgrammaticChange = false;
        }
    }

    private void initComponents() {
        JPanel topFormPanel = new JPanel(new GridLayout(1, 2, scale(20), 0));
        topFormPanel.setBorder(BorderFactory.createEmptyBorder(scale(5),scale(5),scale(5),scale(5)));

        JPanel employeeInfoPanel = new JPanel(new GridBagLayout());
        TitledBorder empInfoBorder = BorderFactory.createTitledBorder("직원 기본 정보");
        empInfoBorder.setTitleFont(enlargedFontBold);
        employeeInfoPanel.setBorder(empInfoBorder);

        GridBagConstraints gbcEmp = new GridBagConstraints();
        gbcEmp.insets = new Insets(scale(4), scale(5), scale(4), scale(5));
        gbcEmp.fill = GridBagConstraints.HORIZONTAL;
        gbcEmp.weightx = 1.0;

        JLabel searchLabel = new JLabel("직원 검색");
        searchLabel.setFont(enlargedFont);
        gbcEmp.anchor = GridBagConstraints.WEST; gbcEmp.fill = GridBagConstraints.NONE; gbcEmp.weightx = 0.0;
        gbcEmp.gridx = 0; gbcEmp.gridy = 0; employeeInfoPanel.add(searchLabel, gbcEmp);

        searchField = new JTextField(scale(15));
        searchField.setFont(enlargedFont);
        gbcEmp.anchor = GridBagConstraints.WEST; gbcEmp.fill = GridBagConstraints.HORIZONTAL; gbcEmp.weightx = 1.0;
        gbcEmp.gridx = 1; gbcEmp.gridy = 0; gbcEmp.gridwidth = 2; employeeInfoPanel.add(searchField, gbcEmp);

        searchButton = new JButton("검색");
        searchButton.setFont(enlargedFont);
        gbcEmp.fill = GridBagConstraints.NONE; gbcEmp.weightx = 0.0; gbcEmp.anchor = GridBagConstraints.LINE_START;
        gbcEmp.gridx = 3; gbcEmp.gridy = 0; gbcEmp.gridwidth = 1; employeeInfoPanel.add(searchButton, gbcEmp);

        JLabel hireDateLabel = new JLabel("입사일:");
        hireDateLabel.setFont(enlargedFont);
        gbcEmp.anchor = GridBagConstraints.WEST; gbcEmp.fill = GridBagConstraints.NONE; gbcEmp.weightx = 0.0;
        gbcEmp.gridx = 0; gbcEmp.gridy = 1; employeeInfoPanel.add(hireDateLabel, gbcEmp);

        hireDateField = new JTextField(scale(10));
        hireDateField.setFont(enlargedFont);
        gbcEmp.anchor = GridBagConstraints.WEST; gbcEmp.fill = GridBagConstraints.HORIZONTAL; gbcEmp.weightx = 1.0;
        gbcEmp.gridx = 1; gbcEmp.gridy = 1; employeeInfoPanel.add(hireDateField, gbcEmp);

        JLabel salaryChangeDateLabel = new JLabel("급여변동일:");
        salaryChangeDateLabel.setFont(enlargedFont);
        gbcEmp.anchor = GridBagConstraints.WEST; gbcEmp.fill = GridBagConstraints.NONE; gbcEmp.weightx = 0.0;
        gbcEmp.gridx = 2; gbcEmp.gridy = 1; employeeInfoPanel.add(salaryChangeDateLabel, gbcEmp);

        salaryChangeDateField = new JTextField(scale(10));
        salaryChangeDateField.setFont(enlargedFont);
        gbcEmp.anchor = GridBagConstraints.WEST; gbcEmp.fill = GridBagConstraints.HORIZONTAL; gbcEmp.weightx = 1.0;
        gbcEmp.gridx = 3; gbcEmp.gridy = 1; employeeInfoPanel.add(salaryChangeDateField, gbcEmp);

        JLabel departmentLabel = new JLabel("부서:");
        departmentLabel.setFont(enlargedFont);
        gbcEmp.anchor = GridBagConstraints.WEST; gbcEmp.fill = GridBagConstraints.NONE; gbcEmp.weightx = 0.0;
        gbcEmp.gridx = 0; gbcEmp.gridy = 2; employeeInfoPanel.add(departmentLabel, gbcEmp);

        String[] departments = {"", "임원", "UI/UX", "프론트", "백엔드", "경영지원"};
        departmentComboBox = new JComboBox<>(departments);
        departmentComboBox.setFont(enlargedFont);
        gbcEmp.anchor = GridBagConstraints.WEST; gbcEmp.fill = GridBagConstraints.HORIZONTAL; gbcEmp.weightx = 1.0;
        gbcEmp.gridx = 1; gbcEmp.gridy = 2; employeeInfoPanel.add(departmentComboBox, gbcEmp);

        JLabel nameLabel = new JLabel("이름:");
        nameLabel.setFont(enlargedFont);
        gbcEmp.anchor = GridBagConstraints.WEST; gbcEmp.fill = GridBagConstraints.NONE; gbcEmp.weightx = 0.0;
        gbcEmp.gridx = 2; gbcEmp.gridy = 2; employeeInfoPanel.add(nameLabel, gbcEmp);

        nameField = new JTextField(scale(10));
        nameField.setFont(enlargedFont);
        gbcEmp.anchor = GridBagConstraints.WEST; gbcEmp.fill = GridBagConstraints.HORIZONTAL; gbcEmp.weightx = 1.0;
        gbcEmp.gridx = 3; gbcEmp.gridy = 2; employeeInfoPanel.add(nameField, gbcEmp);

        JLabel residentRegNoLabel = new JLabel("주민번호:");
        residentRegNoLabel.setFont(enlargedFont);
        gbcEmp.anchor = GridBagConstraints.WEST; gbcEmp.fill = GridBagConstraints.NONE; gbcEmp.weightx = 0.0;
        gbcEmp.gridx = 0; gbcEmp.gridy = 3; employeeInfoPanel.add(residentRegNoLabel, gbcEmp);

        residentRegNoField = new JTextField(scale(10));
        residentRegNoField.setFont(enlargedFont);
        gbcEmp.anchor = GridBagConstraints.WEST; gbcEmp.fill = GridBagConstraints.HORIZONTAL; gbcEmp.weightx = 1.0;
        gbcEmp.gridx = 1; gbcEmp.gridy = 3; employeeInfoPanel.add(residentRegNoField, gbcEmp);

        JLabel phoneLabel = new JLabel("전화번호:");
        phoneLabel.setFont(enlargedFont);
        gbcEmp.anchor = GridBagConstraints.WEST; gbcEmp.fill = GridBagConstraints.NONE; gbcEmp.weightx = 0.0;
        gbcEmp.gridx = 2; gbcEmp.gridy = 3; employeeInfoPanel.add(phoneLabel, gbcEmp);

        phoneField = new JTextField(scale(10));
        phoneField.setFont(enlargedFont);
        gbcEmp.anchor = GridBagConstraints.WEST; gbcEmp.fill = GridBagConstraints.HORIZONTAL; gbcEmp.weightx = 1.0;
        gbcEmp.gridx = 3; gbcEmp.gridy = 3; employeeInfoPanel.add(phoneField, gbcEmp);

        JLabel addressLabel = new JLabel("주소:");
        addressLabel.setFont(enlargedFont);
        gbcEmp.anchor = GridBagConstraints.WEST; gbcEmp.fill = GridBagConstraints.NONE; gbcEmp.weightx = 0.0;
        gbcEmp.gridx = 0; gbcEmp.gridy = 4; employeeInfoPanel.add(addressLabel, gbcEmp);

        addressField = new JTextField(scale(25));
        addressField.setFont(enlargedFont);
        gbcEmp.anchor = GridBagConstraints.WEST; gbcEmp.fill = GridBagConstraints.HORIZONTAL; gbcEmp.weightx = 1.0;
        gbcEmp.gridx = 1; gbcEmp.gridy = 4; gbcEmp.gridwidth = 3; employeeInfoPanel.add(addressField, gbcEmp);
        gbcEmp.gridwidth = 1;

        JLabel workLocationLabel = new JLabel("근무지:");
        workLocationLabel.setFont(enlargedFont);
        gbcEmp.anchor = GridBagConstraints.WEST; gbcEmp.fill = GridBagConstraints.NONE; gbcEmp.weightx = 0.0;
        gbcEmp.gridx = 0; gbcEmp.gridy = 5; employeeInfoPanel.add(workLocationLabel, gbcEmp);

        workLocationField = new JTextField(scale(25));
        workLocationField.setFont(enlargedFont);
        gbcEmp.anchor = GridBagConstraints.WEST; gbcEmp.fill = GridBagConstraints.HORIZONTAL; gbcEmp.weightx = 1.0;
        gbcEmp.gridx = 1; gbcEmp.gridy = 5; gbcEmp.gridwidth = 3; employeeInfoPanel.add(workLocationField, gbcEmp);
        gbcEmp.gridwidth = 1;

        JLabel siteLocationLabel = new JLabel("소재지:");
        siteLocationLabel.setFont(enlargedFont);
        gbcEmp.anchor = GridBagConstraints.WEST; gbcEmp.fill = GridBagConstraints.NONE; gbcEmp.weightx = 0.0;
        gbcEmp.gridx = 0; gbcEmp.gridy = 6; employeeInfoPanel.add(siteLocationLabel, gbcEmp);

        siteLocationField = new JTextField(scale(25));
        siteLocationField.setFont(enlargedFont);
        gbcEmp.anchor = GridBagConstraints.WEST; gbcEmp.fill = GridBagConstraints.HORIZONTAL; gbcEmp.weightx = 1.0;
        gbcEmp.gridx = 1; gbcEmp.gridy = 6; gbcEmp.gridwidth = 3; employeeInfoPanel.add(siteLocationField, gbcEmp);
        gbcEmp.gridwidth = 1;

        gbcEmp.gridy = 7; gbcEmp.weighty = 1.0; gbcEmp.fill = GridBagConstraints.BOTH;
        employeeInfoPanel.add(new JLabel(), gbcEmp);

        topFormPanel.add(employeeInfoPanel);

        JPanel salaryInfoPanel = new JPanel(new GridBagLayout());
        TitledBorder salaryInfoBorder = BorderFactory.createTitledBorder("급여 상세 정보");
        salaryInfoBorder.setTitleFont(enlargedFontBold);
        salaryInfoPanel.setBorder(salaryInfoBorder);

        GridBagConstraints gbcSal = new GridBagConstraints();
        gbcSal.insets = new Insets(scale(4), scale(5), scale(4), scale(5));
        gbcSal.fill = GridBagConstraints.HORIZONTAL;
        gbcSal.weightx = 1.0;

        JLabel annualSalaryLabel = new JLabel("연봉:");
        annualSalaryLabel.setFont(enlargedFont);
        gbcSal.anchor = GridBagConstraints.WEST; gbcSal.fill = GridBagConstraints.NONE; gbcSal.weightx = 0.0;
        gbcSal.gridx = 0; gbcSal.gridy = 0; salaryInfoPanel.add(annualSalaryLabel, gbcSal);

        annualSalaryField = new JTextField(scale(10));
        annualSalaryField.setFont(enlargedFont);
        gbcSal.anchor = GridBagConstraints.WEST; gbcSal.fill = GridBagConstraints.HORIZONTAL; gbcSal.weightx = 1.0;
        gbcSal.gridx = 1; gbcSal.gridy = 0; salaryInfoPanel.add(annualSalaryField, gbcSal);

        JLabel hourlyWageLabel = new JLabel("시급(월급/224h):");
        hourlyWageLabel.setFont(enlargedFont);
        gbcSal.anchor = GridBagConstraints.WEST; gbcSal.fill = GridBagConstraints.NONE; gbcSal.weightx = 0.0;
        gbcSal.gridx = 2; gbcSal.gridy = 0; salaryInfoPanel.add(hourlyWageLabel, gbcSal);

        hourlyWageField = new JTextField(scale(8));
        hourlyWageField.setFont(enlargedFont);
        hourlyWageField.setEditable(false);
        hourlyWageField.setForeground(Color.BLUE);
        gbcSal.anchor = GridBagConstraints.WEST; gbcSal.fill = GridBagConstraints.HORIZONTAL; gbcSal.weightx = 1.0;
        gbcSal.gridx = 3; gbcSal.gridy = 0; salaryInfoPanel.add(hourlyWageField, gbcSal);

        JLabel monthlyBasicSalaryLabel = new JLabel("기본급(재산정):");
        monthlyBasicSalaryLabel.setFont(enlargedFont);
        gbcSal.anchor = GridBagConstraints.WEST; gbcSal.fill = GridBagConstraints.NONE; gbcSal.weightx = 0.0;
        gbcSal.gridx = 0; gbcSal.gridy = 1; salaryInfoPanel.add(monthlyBasicSalaryLabel, gbcSal);

        monthlyBasicSalaryField = new JTextField(scale(10));
        monthlyBasicSalaryField.setFont(enlargedFont);
        monthlyBasicSalaryField.setEditable(false);
        gbcSal.anchor = GridBagConstraints.WEST; gbcSal.fill = GridBagConstraints.HORIZONTAL; gbcSal.weightx = 1.0;
        gbcSal.gridx = 1; gbcSal.gridy = 1; salaryInfoPanel.add(monthlyBasicSalaryField, gbcSal);

        JLabel fixedOvertimeAllowanceLabel = new JLabel("고정연장수당(재산정):");
        fixedOvertimeAllowanceLabel.setFont(enlargedFont);
        gbcSal.anchor = GridBagConstraints.WEST; gbcSal.fill = GridBagConstraints.NONE; gbcSal.weightx = 0.0;
        gbcSal.gridx = 2; gbcSal.gridy = 1; salaryInfoPanel.add(fixedOvertimeAllowanceLabel, gbcSal);

        fixedOvertimeAllowanceField = new JTextField(scale(8));
        fixedOvertimeAllowanceField.setFont(enlargedFont);
        fixedOvertimeAllowanceField.setEditable(false);
        gbcSal.anchor = GridBagConstraints.WEST; gbcSal.fill = GridBagConstraints.HORIZONTAL; gbcSal.weightx = 1.0;
        gbcSal.gridx = 3; gbcSal.gridy = 1; salaryInfoPanel.add(fixedOvertimeAllowanceField, gbcSal);

        JLabel bonusLabel = new JLabel("상여금:");
        bonusLabel.setFont(enlargedFont);
        gbcSal.anchor = GridBagConstraints.WEST; gbcSal.fill = GridBagConstraints.NONE; gbcSal.weightx = 0.0;
        gbcSal.gridx = 0; gbcSal.gridy = 2; salaryInfoPanel.add(bonusLabel, gbcSal);

        bonusField = new JTextField(scale(10));
        bonusField.setFont(enlargedFont);
        gbcSal.anchor = GridBagConstraints.WEST; gbcSal.fill = GridBagConstraints.HORIZONTAL; gbcSal.weightx = 1.0;
        gbcSal.gridx = 1; gbcSal.gridy = 2; salaryInfoPanel.add(bonusField, gbcSal);

        JLabel otherAllowanceLabel = new JLabel("기타수당:");
        otherAllowanceLabel.setFont(enlargedFont);
        gbcSal.anchor = GridBagConstraints.WEST; gbcSal.fill = GridBagConstraints.NONE; gbcSal.weightx = 0.0;
        gbcSal.gridx = 2; gbcSal.gridy = 2; salaryInfoPanel.add(otherAllowanceLabel, gbcSal);

        otherAllowanceField = new JTextField(scale(8));
        otherAllowanceField.setFont(enlargedFont);
        gbcSal.anchor = GridBagConstraints.WEST; gbcSal.fill = GridBagConstraints.HORIZONTAL; gbcSal.weightx = 1.0;
        gbcSal.gridx = 3; gbcSal.gridy = 2; salaryInfoPanel.add(otherAllowanceField, gbcSal);

        JLabel mealAllowanceLabel = new JLabel("식대:");
        mealAllowanceLabel.setFont(enlargedFont);
        gbcSal.anchor = GridBagConstraints.WEST; gbcSal.fill = GridBagConstraints.NONE; gbcSal.weightx = 0.0;
        gbcSal.gridx = 0; gbcSal.gridy = 3; salaryInfoPanel.add(mealAllowanceLabel, gbcSal);

        mealAllowanceField = new JTextField(scale(10));
        mealAllowanceField.setFont(enlargedFont);
        gbcSal.anchor = GridBagConstraints.WEST; gbcSal.fill = GridBagConstraints.HORIZONTAL; gbcSal.weightx = 1.0;
        gbcSal.gridx = 1; gbcSal.gridy = 3; salaryInfoPanel.add(mealAllowanceField, gbcSal);

        JLabel vehicleMaintenanceFeeLabel = new JLabel("차량유지비:");
        vehicleMaintenanceFeeLabel.setFont(enlargedFont);
        gbcSal.anchor = GridBagConstraints.WEST; gbcSal.fill = GridBagConstraints.NONE; gbcSal.weightx = 0.0;
        gbcSal.gridx = 2; gbcSal.gridy = 3; salaryInfoPanel.add(vehicleMaintenanceFeeLabel, gbcSal);

        vehicleMaintenanceFeeField = new JTextField(scale(8));
        vehicleMaintenanceFeeField.setFont(enlargedFont);
        gbcSal.anchor = GridBagConstraints.WEST; gbcSal.fill = GridBagConstraints.HORIZONTAL; gbcSal.weightx = 1.0;
        gbcSal.gridx = 3; gbcSal.gridy = 3; salaryInfoPanel.add(vehicleMaintenanceFeeField, gbcSal);

        JLabel researchDevelopmentExpenseLabel = new JLabel("연구개발비:");
        researchDevelopmentExpenseLabel.setFont(enlargedFont);
        gbcSal.anchor = GridBagConstraints.WEST; gbcSal.fill = GridBagConstraints.NONE; gbcSal.weightx = 0.0;
        gbcSal.gridx = 0; gbcSal.gridy = 4; salaryInfoPanel.add(researchDevelopmentExpenseLabel, gbcSal);

        researchDevelopmentExpenseField = new JTextField(scale(10));
        researchDevelopmentExpenseField.setFont(enlargedFont);
        gbcSal.anchor = GridBagConstraints.WEST; gbcSal.fill = GridBagConstraints.HORIZONTAL; gbcSal.weightx = 1.0;
        gbcSal.gridx = 1; gbcSal.gridy = 4; salaryInfoPanel.add(researchDevelopmentExpenseField, gbcSal);

        JLabel childcareAllowanceLabel = new JLabel("육아수당:");
        childcareAllowanceLabel.setFont(enlargedFont);
        gbcSal.anchor = GridBagConstraints.WEST; gbcSal.fill = GridBagConstraints.NONE; gbcSal.weightx = 0.0;
        gbcSal.gridx = 2; gbcSal.gridy = 4; salaryInfoPanel.add(childcareAllowanceLabel, gbcSal);

        childcareAllowanceField = new JTextField(scale(8));
        childcareAllowanceField.setFont(enlargedFont);
        gbcSal.anchor = GridBagConstraints.WEST; gbcSal.fill = GridBagConstraints.HORIZONTAL; gbcSal.weightx = 1.0;
        gbcSal.gridx = 3; gbcSal.gridy = 4; salaryInfoPanel.add(childcareAllowanceField, gbcSal);

        JLabel totalMonthlyPayLabel = new JLabel("총 월 급여:");
        totalMonthlyPayLabel.setFont(enlargedFontBold);
        gbcSal.gridx = 0; gbcSal.gridy = 5; gbcSal.gridwidth = 1;
        gbcSal.anchor = GridBagConstraints.WEST; gbcSal.fill = GridBagConstraints.NONE; gbcSal.weightx = 0.0;
        salaryInfoPanel.add(totalMonthlyPayLabel, gbcSal);

        totalMonthlyPayField = new JTextField(scale(10));
        totalMonthlyPayField.setFont(enlargedFontBold);
        totalMonthlyPayField.setForeground(Color.BLUE);
        totalMonthlyPayField.setEditable(false);
        gbcSal.gridx = 1; gbcSal.gridy = 5;
        gbcSal.anchor = GridBagConstraints.WEST; gbcSal.fill = GridBagConstraints.HORIZONTAL; gbcSal.weightx = 1.0;
        salaryInfoPanel.add(totalMonthlyPayField, gbcSal);

        gbcSal.gridx = 2; gbcSal.gridy = 5; salaryInfoPanel.add(new JLabel(" "), gbcSal);
        gbcSal.gridx = 3; gbcSal.gridy = 5; salaryInfoPanel.add(new JLabel(" "), gbcSal);


        gbcSal.gridy = 6; gbcSal.weighty = 1.0; gbcSal.fill = GridBagConstraints.BOTH;
        salaryInfoPanel.add(new JLabel(), gbcSal);

        topFormPanel.add(salaryInfoPanel);
        add(topFormPanel, BorderLayout.NORTH);

        JPanel bottomButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, scale(10), scale(10)));
        createContractButton = new JButton("근로계약서 작성");
        createContractButton.setFont(enlargedFont);
        addRowButton = new JButton("+ 행 추가");
        addRowButton.setFont(enlargedFont);
        exportTableButton = new JButton("표 엑셀 저장");
        exportTableButton.setFont(enlargedFont);
        addButton = new JButton("직원 등록");
        addButton.setFont(enlargedFont);
        updateButton = new JButton("수정사항 저장");
        updateButton.setFont(enlargedFont);
        deleteButton = new JButton("직원 삭제");
        deleteButton.setFont(enlargedFont);
        clearButton = new JButton("입력 초기화");
        clearButton.setFont(enlargedFont);
        backButton = new JButton("뒤로 가기");
        backButton.setFont(enlargedFont);

        bottomButtonPanel.add(createContractButton);
        bottomButtonPanel.add(addRowButton);
        bottomButtonPanel.add(exportTableButton);
        bottomButtonPanel.add(addButton);
        bottomButtonPanel.add(updateButton);
        bottomButtonPanel.add(deleteButton);
        bottomButtonPanel.add(clearButton);
        bottomButtonPanel.add(backButton);
        add(bottomButtonPanel, BorderLayout.SOUTH);


        String[] tableColumnNames = { "No.", "입사일", "이름", "부서", "주민번호", "전화번호", "주소", "연봉", "기본급", "고정연장수당", "추가 연장 수당", "상여금", "기타수당", "식대", "차량유지비", "연구개발비", "육아수당", "총 월 급여" };
        tableModel = new DefaultTableModel(tableColumnNames, 0) {
            @Override public boolean isCellEditable(int row, int column) {
                if (getRowCount() > 0 && getValueAt(row, COL_NO) != null && getValueAt(row, COL_NO).equals("합계")) {
                    return false;
                }
                switch (column) {
                    case COL_NO:
                    case COL_MONTHLY_BASIC:
                    case COL_FIXED_OVERTIME:
                    case COL_ADDITIONAL_OVERTIME:
                    case COL_TOTAL_MONTHLY_PAY:
                        return false;
                    default:
                        return true;
                }
            }
        };

        employeeTable = new JTable(tableModel) {
            @Override
            public boolean editCellAt(int row, int column, java.util.EventObject e) {
                boolean result = super.editCellAt(row, column, e);
                if (result) {
                    final Component editor = getEditorComponent();
                    if (editor != null && editor instanceof JTextComponent) {
                        if (e instanceof KeyEvent) {
                            KeyEvent ke = (KeyEvent) e;
                            ((JTextComponent) editor).setText(String.valueOf(ke.getKeyChar()));
                        } else {
                            SwingUtilities.invokeLater(((JTextComponent) editor)::selectAll);
                        }
                        ((JTextComponent) editor).requestFocusInWindow();
                    }
                }
                return result;
            }
        };
        new TableClipboardAdapter(employeeTable);
        employeeTable.setSurrendersFocusOnKeystroke(true);

        employeeTable.setFont(enlargedFont);
        employeeTable.getTableHeader().setFont(enlargedFontBold);
        employeeTable.setDefaultRenderer(Object.class, new TotalsAwareNumberRenderer(enlargedFont, totalsRowFont));


        FontMetrics fm = employeeTable.getFontMetrics(enlargedFont);
        int fontHeight = fm.getHeight();
        int rowPadding = scale(6);
        employeeTable.setRowHeight(fontHeight + rowPadding);

        employeeTable.setShowGrid(true);
        employeeTable.setGridColor(new Color(220,220,220));
        employeeTable.setIntercellSpacing(new Dimension(1,1));

        employeeTable.setFillsViewportHeight(true);
        sorter = new TableRowSorter<>(tableModel); employeeTable.setRowSorter(sorter);
        TableColumnModel tcm = employeeTable.getColumnModel();

        tcm.getColumn(COL_NO).setPreferredWidth(scale(40));
        tcm.getColumn(COL_HIRE_DATE).setPreferredWidth(scale(90));
        tcm.getColumn(COL_NAME).setPreferredWidth(scale(80));
        tcm.getColumn(COL_DEPARTMENT).setPreferredWidth(scale(80));
        tcm.getColumn(COL_RESIDENT_REG_NO).setPreferredWidth(scale(120));
        tcm.getColumn(COL_PHONE).setPreferredWidth(scale(110));
        tcm.getColumn(COL_ADDRESS).setPreferredWidth(scale(200));

        for (int i = COL_ANNUAL_SALARY; i < tableColumnNames.length ; i++) {
            try {
                if (i == COL_ANNUAL_SALARY) tcm.getColumn(i).setPreferredWidth(scale(90));
                else if (i == COL_MONTHLY_BASIC || i == COL_FIXED_OVERTIME || i == COL_ADDITIONAL_OVERTIME) tcm.getColumn(i).setPreferredWidth(scale(100));
                else if (i == COL_TOTAL_MONTHLY_PAY) tcm.getColumn(i).setPreferredWidth(scale(110));
                else tcm.getColumn(i).setPreferredWidth(scale(90));
            } catch (ArrayIndexOutOfBoundsException e){
                System.err.println("테이블 컬럼 (" + i + ") 설정 오류 (존재하지 않는 컬럼 인덱스): " + e.getMessage());
            }
        }

        JScrollPane scrollPane = new JScrollPane(employeeTable); add(scrollPane, BorderLayout.CENTER);

        searchButton.addActionListener(e -> performSearch());
        addButton.addActionListener(e -> addNewEmployeeFromTableRowOrForm());
        updateButton.addActionListener(e -> saveModifiedTableRows());
        deleteButton.addActionListener(e -> deleteEmployee());
        clearButton.addActionListener(e -> clearFieldsAndSelection());
        backButton.addActionListener(e -> {
            if (returnToSummaryPage != null) returnToSummaryPage.run();
        });
        addRowButton.addActionListener(e -> addNewEmptyRowToTable());
        exportTableButton.addActionListener(e -> exportEmployeeTableToExcel());
        createContractButton.addActionListener(e -> openContractPage());
    }


    private void performSearch() {
        String searchQuery = searchField.getText().trim();
        loadEmployeeTable(searchQuery);
    }

    private void addTableListeners() {
        employeeTable.getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting() && !isUpdatingFromForm) {
                int selectedViewRow = employeeTable.getSelectedRow();
                if (selectedViewRow >= 0) {
                    int modelRow = employeeTable.convertRowIndexToModel(selectedViewRow);
                    if (modelRow < tableModel.getRowCount() &&
                            tableModel.getValueAt(modelRow, COL_NO) != null &&
                            !tableModel.getValueAt(modelRow, COL_NO).equals("합계")) {
                        isUpdatingFromTable = true;
                        updateFormFieldsFromSelectedTableRow(modelRow);
                        isUpdatingFromTable = false;
                    } else if (tableModel.getValueAt(modelRow, COL_NO) != null &&
                            tableModel.getValueAt(modelRow, COL_NO).equals("합계")) {
                        // 합계 행 클릭 시 폼 비우기
                        clearFields();
                    }
                }
            }
        });

        tableModel.addTableModelListener(e -> {
            if (e.getType() == TableModelEvent.UPDATE && !isUpdatingFromForm && !isProgrammaticChange) {
                int modelRow = e.getFirstRow();
                int column = e.getColumn();

                if (column == TableModelEvent.ALL_COLUMNS || modelRow < 0 || modelRow >= tableModel.getRowCount() ||
                        (tableModel.getValueAt(modelRow, COL_NO) != null && tableModel.getValueAt(modelRow, COL_NO).equals("합계"))) {
                    return;
                }

                int selectedViewRow = employeeTable.getSelectedRow();
                if (selectedViewRow != -1) {
                    int selectedModelRow = employeeTable.convertRowIndexToModel(selectedViewRow);
                    if (modelRow == selectedModelRow) {
                        isUpdatingFromTable = true;
                        updateFormFieldsFromSelectedTableRow(modelRow);
                        if (column == COL_ANNUAL_SALARY ||
                                (column >= COL_BONUS && column <= COL_CHILDCARE_ALLOWANCE)) {
                            recalculateSalariesForTableRow(modelRow, true);
                        }
                        isUpdatingFromTable = false;
                    } else {
                        if (column == COL_ANNUAL_SALARY ||
                                (column >= COL_BONUS && column <= COL_CHILDCARE_ALLOWANCE)) {
                            recalculateSalariesForTableRow(modelRow, false);
                        }
                    }
                } else {
                    if (column == COL_ANNUAL_SALARY ||
                            (column >= COL_BONUS && column <= COL_CHILDCARE_ALLOWANCE)) {
                        recalculateSalariesForTableRow(modelRow, false);
                    }
                }
                modifiedRowModelIndices.add(modelRow);
                addOrUpdateTotalsRow();
            }
        });
    }

    private void openContractPage() {
        int selectedViewRow = employeeTable.getSelectedRow();
        if (selectedViewRow == -1) {
            JOptionPane.showMessageDialog(this, "근로계약서를 작성할 직원을 테이블에서 선택해주세요.", "알림", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int modelRow = employeeTable.convertRowIndexToModel(selectedViewRow);

        if (tableModel.getValueAt(modelRow, COL_NO).equals("합계")){
            JOptionPane.showMessageDialog(this, "합계 행에 대해서는 근로계약서를 작성할 수 없습니다.", "알림", JOptionPane.INFORMATION_MESSAGE);
            return;
        }


        if (modelRow < 0 || modelRow >= displayedEmployeesInTableOrder.size()) {
            Object noValue = tableModel.getValueAt(modelRow, COL_NO);
            if ("*".equals(noValue != null ? noValue.toString() : "")) {
                JOptionPane.showMessageDialog(this, "새로 추가 중인 직원입니다. 먼저 '직원 등록' 또는 '수정사항 저장'을 완료해주세요.", "알림", JOptionPane.WARNING_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, "유효한 직원 데이터가 아닙니다. 목록을 새로고침하거나 직원을 다시 선택해주세요.", "알림", JOptionPane.WARNING_MESSAGE);
            }
            return;
        }

        Employee selectedEmployee = displayedEmployeesInTableOrder.get(modelRow);
        if (selectedEmployee.getId() <= 0) {
            JOptionPane.showMessageDialog(this, "아직 DB에 저장되지 않은 직원입니다. 먼저 '직원 등록' 또는 '수정사항 저장'을 완료해주세요.", "알림", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Optional<Payroll> contractualPayrollOpt = payrollManager.getContractualPayroll(selectedEmployee.getId());

        if (!contractualPayrollOpt.isPresent()) {
            try {
                long annualSalary = selectedEmployee.getAnnualSalary();
                long bonus = parseFormattedNumber(tableModel.getValueAt(modelRow, COL_BONUS).toString());
                long otherAllowance = parseFormattedNumber(tableModel.getValueAt(modelRow, COL_OTHER_ALLOWANCE).toString());
                long mealAllowance = parseFormattedNumber(tableModel.getValueAt(modelRow, COL_MEAL_ALLOWANCE).toString());
                long vehicleFee = parseFormattedNumber(tableModel.getValueAt(modelRow, COL_VEHICLE_FEE).toString());
                long researchExpense = parseFormattedNumber(tableModel.getValueAt(modelRow, COL_RESEARCH_EXPENSE).toString());
                long childcareAllowance = parseFormattedNumber(tableModel.getValueAt(modelRow, COL_CHILDCARE_ALLOWANCE).toString());

                CalculatedSalaryItems items = calculateNewContractualSalaryItems(annualSalary, bonus, otherAllowance, mealAllowance, vehicleFee, researchExpense, childcareAllowance);
                Payroll tempPayroll = new Payroll(selectedEmployee, (int)items.monthlyBasicSalary, (int)bonus, (int)items.fixedOvertimeAllowance, (int)otherAllowance, (int)mealAllowance, (int)vehicleFee, (int)researchExpense, (int)childcareAllowance);
                if (payrollApp != null) {
                    payrollApp.showEmploymentContractPage(selectedEmployee, tempPayroll);
                }

            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, selectedEmployee.getName() + " 님의 계약 급여 정보 구성 중 오류가 발생했습니다.", "오류", JOptionPane.WARNING_MESSAGE);
                return;
            }
        } else {
            if (payrollApp != null) {
                payrollApp.showEmploymentContractPage(selectedEmployee, contractualPayrollOpt.get());
            }
        }
    }

    private void addFormListeners() {
        DocumentListener autoFormatAndUpdateListener = new DocumentListener() {
            private void handleEvent(DocumentEvent e) {
                if (isProgrammaticChange || isUpdatingFromTable) return;

                Object sourceDocProperty = e.getDocument().getProperty("owner");
                if (!(sourceDocProperty instanceof JTextField)) return;

                final JTextField ownerField = (JTextField) sourceDocProperty;

                if (ownerField == hireDateField || ownerField == salaryChangeDateField) {
                    SwingUtilities.invokeLater(() -> applyDateFormatting(ownerField));
                } else if (ownerField == residentRegNoField) {
                    SwingUtilities.invokeLater(() -> applyResidentRegNoFormatting(ownerField));
                } else if (ownerField == phoneField) {
                    SwingUtilities.invokeLater(() -> applyPhoneFormatting(ownerField));
                }

                if (ownerField == annualSalaryField || ownerField == bonusField || ownerField == otherAllowanceField ||
                        ownerField == mealAllowanceField || ownerField == vehicleMaintenanceFeeField ||
                        ownerField == researchDevelopmentExpenseField || ownerField == childcareAllowanceField) {
                    SwingUtilities.invokeLater(() -> {
                        recalculateAndDisplaySalariesFromForm();
                        updateSelectedTableRowFromForm();
                    });
                } else {
                    SwingUtilities.invokeLater(this::triggerTableUpdateFromForm);
                }
            }

            private void triggerTableUpdateFromForm() {
                updateSelectedTableRowFromForm();
            }

            public void changedUpdate(DocumentEvent e) { handleEvent(e); }
            public void removeUpdate(DocumentEvent e) { handleEvent(e); }
            public void insertUpdate(DocumentEvent e) { handleEvent(e); }
        };

        hireDateField.getDocument().putProperty("owner", hireDateField);
        hireDateField.getDocument().addDocumentListener(autoFormatAndUpdateListener);
        salaryChangeDateField.getDocument().putProperty("owner", salaryChangeDateField);
        salaryChangeDateField.getDocument().addDocumentListener(autoFormatAndUpdateListener);
        nameField.getDocument().putProperty("owner", nameField);
        nameField.getDocument().addDocumentListener(autoFormatAndUpdateListener);
        residentRegNoField.getDocument().putProperty("owner", residentRegNoField);
        residentRegNoField.getDocument().addDocumentListener(autoFormatAndUpdateListener);
        phoneField.getDocument().putProperty("owner", phoneField);
        phoneField.getDocument().addDocumentListener(autoFormatAndUpdateListener);
        addressField.getDocument().putProperty("owner", addressField);
        addressField.getDocument().addDocumentListener(autoFormatAndUpdateListener);
        workLocationField.getDocument().putProperty("owner", workLocationField);
        workLocationField.getDocument().addDocumentListener(autoFormatAndUpdateListener);
        siteLocationField.getDocument().putProperty("owner", siteLocationField);
        siteLocationField.getDocument().addDocumentListener(autoFormatAndUpdateListener);

        annualSalaryField.getDocument().putProperty("owner", annualSalaryField);
        annualSalaryField.getDocument().addDocumentListener(autoFormatAndUpdateListener);
        bonusField.getDocument().putProperty("owner", bonusField);
        bonusField.getDocument().addDocumentListener(autoFormatAndUpdateListener);
        otherAllowanceField.getDocument().putProperty("owner", otherAllowanceField);
        otherAllowanceField.getDocument().addDocumentListener(autoFormatAndUpdateListener);
        mealAllowanceField.getDocument().putProperty("owner", mealAllowanceField);
        mealAllowanceField.getDocument().addDocumentListener(autoFormatAndUpdateListener);
        vehicleMaintenanceFeeField.getDocument().putProperty("owner", vehicleMaintenanceFeeField);
        vehicleMaintenanceFeeField.getDocument().addDocumentListener(autoFormatAndUpdateListener);
        researchDevelopmentExpenseField.getDocument().putProperty("owner", researchDevelopmentExpenseField);
        researchDevelopmentExpenseField.getDocument().addDocumentListener(autoFormatAndUpdateListener);
        childcareAllowanceField.getDocument().putProperty("owner", childcareAllowanceField);
        childcareAllowanceField.getDocument().addDocumentListener(autoFormatAndUpdateListener);


        departmentComboBox.addActionListener(e -> {
            if (!isUpdatingFromTable && !isProgrammaticChange) {
                updateSelectedTableRowFromForm();
            }
        });

        FocusAdapter formFocusLostAdapter = new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                if (isProgrammaticChange || isUpdatingFromTable) return;
                Object sourceComponent = e.getSource();
                if (!(sourceComponent instanceof JTextField)) return;

                final JTextField sourceField = (JTextField) sourceComponent;

                if (sourceField == hireDateField || sourceField == salaryChangeDateField) {
                    applyDateFormatting(sourceField);
                } else if (sourceField == residentRegNoField) {
                    applyResidentRegNoFormatting(sourceField);
                } else if (sourceField == phoneField) {
                    applyPhoneFormatting(sourceField);
                } else if (sourceField == annualSalaryField || sourceField == bonusField || sourceField == otherAllowanceField ||
                        sourceField == mealAllowanceField || sourceField == vehicleMaintenanceFeeField ||
                        sourceField == researchDevelopmentExpenseField || sourceField == childcareAllowanceField) {
                    formatNumericFieldOnFocusLost(sourceField);

                }
                if (! (sourceField == annualSalaryField || sourceField == bonusField || sourceField == otherAllowanceField ||
                        sourceField == mealAllowanceField || sourceField == vehicleMaintenanceFeeField ||
                        sourceField == researchDevelopmentExpenseField || sourceField == childcareAllowanceField) ) {
                    updateSelectedTableRowFromForm();
                }
            }
        };

        hireDateField.addFocusListener(formFocusLostAdapter);
        salaryChangeDateField.addFocusListener(formFocusLostAdapter);
        nameField.addFocusListener(formFocusLostAdapter);
        residentRegNoField.addFocusListener(formFocusLostAdapter);
        phoneField.addFocusListener(formFocusLostAdapter);
        addressField.addFocusListener(formFocusLostAdapter);
        workLocationField.addFocusListener(formFocusLostAdapter);
        siteLocationField.addFocusListener(formFocusLostAdapter);

        annualSalaryField.addFocusListener(formFocusLostAdapter);
        bonusField.addFocusListener(formFocusLostAdapter);
        otherAllowanceField.addFocusListener(formFocusLostAdapter);
        mealAllowanceField.addFocusListener(formFocusLostAdapter);
        vehicleMaintenanceFeeField.addFocusListener(formFocusLostAdapter);
        researchDevelopmentExpenseField.addFocusListener(formFocusLostAdapter);
        childcareAllowanceField.addFocusListener(formFocusLostAdapter);
    }

    public void loadEmployeeTable(String searchQuery) {
        isProgrammaticChange = true;
        removeTotalsRow();
        tableModel.setRowCount(0);
        displayedEmployeesInTableOrder.clear();
        modifiedRowModelIndices.clear();

        List<Employee> allEmployeesFromDB = payrollManager.getAllEmployees();
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        allEmployeesFromDB.sort(Comparator.comparing(
                emp -> {
                    try {
                        if (emp.getHireDate() != null && !emp.getHireDate().isEmpty()) {
                            return LocalDate.parse(emp.getHireDate(), dateFormatter);
                        }
                    } catch (DateTimeParseException e) { }
                    return LocalDate.MAX;
                },
                Comparator.nullsLast(Comparator.naturalOrder())
        ));


        List<Employee> filteredEmployees;
        if (searchQuery != null && !searchQuery.trim().isEmpty()) {
            String lowerCaseQuery = searchQuery.trim().toLowerCase();
            filteredEmployees = allEmployeesFromDB.stream()
                    .filter(emp -> emp.getName().toLowerCase().contains(lowerCaseQuery) ||
                            (emp.getResidentRegistrationNumber() != null && emp.getResidentRegistrationNumber().contains(lowerCaseQuery)))
                    .collect(Collectors.toList());
        } else {
            filteredEmployees = new ArrayList<>(allEmployeesFromDB);
        }

        displayedEmployeesInTableOrder.addAll(filteredEmployees);
        AtomicInteger sequenceNumber = new AtomicInteger(1);

        displayedEmployeesInTableOrder.forEach(emp -> {
            Optional<Payroll> contractualPayrollOpt = payrollManager.getContractualPayroll(emp.getId());
            Object[] rowData;

            long annualSalary = emp.getAnnualSalary();
            long bonus = 0, otherAllowance = 0, mealAllowance = 0, vehicleFee = 0, researchExpense = 0, childcareAllowance = 0;
            long additionalOTPremium = 0;

            if (contractualPayrollOpt.isPresent()) {
                Payroll storedPayroll = contractualPayrollOpt.get();
                bonus = storedPayroll.getBonus();
                otherAllowance = storedPayroll.getOtherAllowance();
                mealAllowance = storedPayroll.getMealAllowance();
                vehicleFee = storedPayroll.getVehicleMaintenanceFee();
                researchExpense = storedPayroll.getResearchDevelopmentExpense();
                childcareAllowance = storedPayroll.getChildcareAllowance();
                additionalOTPremium = storedPayroll.getAdditionalOvertimePremium();
            } else {
                Map<String, String> settings = payrollManager.loadSettings();
                mealAllowance = Long.parseLong(settings.getOrDefault("defaultMealAllowance", "200000"));
            }

            CalculatedSalaryItems calculatedItems = calculateNewContractualSalaryItems(
                    annualSalary, bonus, otherAllowance, mealAllowance, vehicleFee, researchExpense, childcareAllowance
            );

            rowData = new Object[]{
                    sequenceNumber.getAndIncrement(), emp.getHireDate(), emp.getName(),
                    emp.getDepartment(), emp.getResidentRegistrationNumber(), emp.getPhoneNumber(),
                    emp.getAddress(),
                    formatNumber(annualSalary),
                    formatNumber(calculatedItems.monthlyBasicSalary),
                    formatNumber(calculatedItems.fixedOvertimeAllowance),
                    formatNumber(additionalOTPremium),
                    formatNumber(bonus),
                    formatNumber(otherAllowance),
                    formatNumber(mealAllowance),
                    formatNumber(vehicleFee),
                    formatNumber(researchExpense),
                    formatNumber(childcareAllowance),
                    formatNumber(calculatedItems.totalMonthlyPay)
            };
            tableModel.addRow(rowData);
        });

        addOrUpdateTotalsRow();

        if (sorter == null && tableModel != null) {
            sorter = new TableRowSorter<>(tableModel);
            employeeTable.setRowSorter(sorter);
        } else if (sorter != null) {
            sorter.setModel(tableModel);
        }


        isProgrammaticChange = false;
        if (employeeTable.getRowCount() > 1) {
            employeeTable.setRowSelectionInterval(0,0);
        } else if (employeeTable.getRowCount() == 1 && !tableModel.getValueAt(0, COL_NO).equals("합계")) {
            employeeTable.setRowSelectionInterval(0,0);
        } else {
            clearFields();
        }
    }

    private void removeTotalsRow() {
        if (tableModel.getRowCount() > 0) {
            int lastRowIndex = tableModel.getRowCount() - 1;
            if (tableModel.getValueAt(lastRowIndex, COL_NO) != null &&
                    tableModel.getValueAt(lastRowIndex, COL_NO).equals("합계")) {
                tableModel.removeRow(lastRowIndex);
            }
        }
    }

    private void addOrUpdateTotalsRow() {
        removeTotalsRow();

        if (tableModel.getRowCount() == 0) return;

        long[] columnTotals = new long[tableModel.getColumnCount()];
        for (int col : sumColumnIndices) {
            columnTotals[col] = 0;
        }

        for (int i = 0; i < tableModel.getRowCount(); i++) {
            for (int colIndex : sumColumnIndices) {
                try {
                    Object value = tableModel.getValueAt(i, colIndex);
                    if (value != null && !value.toString().isEmpty()) {
                        columnTotals[colIndex] += parseFormattedNumber(value.toString());
                    }
                } catch (NumberFormatException e) {
                    // Ignore parsing errors for sum
                }
            }
        }

        Object[] totalsRowData = new Object[tableModel.getColumnCount()];
        totalsRowData[COL_NO] = "합계";
        for (int i = 1; i < tableModel.getColumnCount(); i++) {
            boolean summed = false;
            for (int sumColIdx : sumColumnIndices) {
                if (i == sumColIdx) {
                    totalsRowData[i] = formatNumber(columnTotals[i]);
                    summed = true;
                    break;
                }
            }
            if (!summed) {
                totalsRowData[i] = "";
            }
        }
        tableModel.addRow(totalsRowData);
    }


    private void updateFormFieldsFromSelectedTableRow(int modelRow) {
        if (modelRow < 0 || modelRow >= tableModel.getRowCount() || isUpdatingFromForm ||
                (tableModel.getValueAt(modelRow, COL_NO) != null && tableModel.getValueAt(modelRow, COL_NO).equals("합계")) ) {
            return;
        }
        isProgrammaticChange = true;

        hireDateField.setText(tableModel.getValueAt(modelRow, COL_HIRE_DATE) != null ? tableModel.getValueAt(modelRow, COL_HIRE_DATE).toString() : "");
        nameField.setText(tableModel.getValueAt(modelRow, COL_NAME) != null ? tableModel.getValueAt(modelRow, COL_NAME).toString() : "");
        departmentComboBox.setSelectedItem(tableModel.getValueAt(modelRow, COL_DEPARTMENT) != null ? tableModel.getValueAt(modelRow, COL_DEPARTMENT).toString() : "");
        residentRegNoField.setText(tableModel.getValueAt(modelRow, COL_RESIDENT_REG_NO) != null ? tableModel.getValueAt(modelRow, COL_RESIDENT_REG_NO).toString() : "");
        phoneField.setText(tableModel.getValueAt(modelRow, COL_PHONE) != null ? tableModel.getValueAt(modelRow, COL_PHONE).toString() : "");
        addressField.setText(tableModel.getValueAt(modelRow, COL_ADDRESS) != null ? tableModel.getValueAt(modelRow, COL_ADDRESS).toString() : "");

        if (modelRow < displayedEmployeesInTableOrder.size()) {
            Employee emp = displayedEmployeesInTableOrder.get(modelRow);
            workLocationField.setText(emp.getWorkLocation() == null ? "" : emp.getWorkLocation());
            siteLocationField.setText(emp.getSiteLocation() == null ? "" : emp.getSiteLocation());
            salaryChangeDateField.setText(emp.getSalaryChangeDate() == null ? "" : emp.getSalaryChangeDate());
            selectedEmployeeDbId = emp.getId();
        } else {
            workLocationField.setText("");
            siteLocationField.setText("");
            salaryChangeDateField.setText("");
            selectedEmployeeDbId = 0;
        }


        try {
            annualSalaryField.setText(tableModel.getValueAt(modelRow, COL_ANNUAL_SALARY).toString());
            bonusField.setText(tableModel.getValueAt(modelRow, COL_BONUS).toString());
            otherAllowanceField.setText(tableModel.getValueAt(modelRow, COL_OTHER_ALLOWANCE).toString());
            mealAllowanceField.setText(tableModel.getValueAt(modelRow, COL_MEAL_ALLOWANCE).toString());
            vehicleMaintenanceFeeField.setText(tableModel.getValueAt(modelRow, COL_VEHICLE_FEE).toString());
            researchDevelopmentExpenseField.setText(tableModel.getValueAt(modelRow, COL_RESEARCH_EXPENSE).toString());
            childcareAllowanceField.setText(tableModel.getValueAt(modelRow, COL_CHILDCARE_ALLOWANCE).toString());
        } catch (Exception ex) {
            System.err.println("테이블 -> 폼 숫자 변환 오류 (수당류): " + ex.getMessage() + " 행: " + modelRow);
            annualSalaryField.setText(formatNumber(0));
            bonusField.setText(formatNumber(0));
            otherAllowanceField.setText(formatNumber(0));
            mealAllowanceField.setText(formatNumber(0));
            vehicleMaintenanceFeeField.setText(formatNumber(0));
            researchDevelopmentExpenseField.setText(formatNumber(0));
            childcareAllowanceField.setText(formatNumber(0));
        }
        recalculateAndDisplaySalariesFromForm();
        isProgrammaticChange = false;
    }

    private void updateSelectedTableRowFromForm() {
        if (isUpdatingFromTable || isProgrammaticChange) return;

        int selectedViewRow = employeeTable.getSelectedRow();
        if (selectedViewRow == -1) return;
        int modelRow = employeeTable.convertRowIndexToModel(selectedViewRow);

        if (modelRow < 0 || modelRow >= tableModel.getRowCount() ||
                (tableModel.getValueAt(modelRow, COL_NO) != null && tableModel.getValueAt(modelRow, COL_NO).equals("합계")) ){
            return;
        }


        isUpdatingFromForm = true;

        tableModel.setValueAt(hireDateField.getText(), modelRow, COL_HIRE_DATE);
        tableModel.setValueAt(nameField.getText(), modelRow, COL_NAME);
        tableModel.setValueAt(departmentComboBox.getSelectedItem().toString(), modelRow, COL_DEPARTMENT);
        tableModel.setValueAt(residentRegNoField.getText(), modelRow, COL_RESIDENT_REG_NO);
        tableModel.setValueAt(phoneField.getText(), modelRow, COL_PHONE);
        tableModel.setValueAt(addressField.getText(), modelRow, COL_ADDRESS);

        try {
            tableModel.setValueAt(formatNumber(parseFormattedNumber(annualSalaryField.getText())), modelRow, COL_ANNUAL_SALARY);
            tableModel.setValueAt(formatNumber(parseFormattedNumber(bonusField.getText())), modelRow, COL_BONUS);
            tableModel.setValueAt(formatNumber(parseFormattedNumber(otherAllowanceField.getText())), modelRow, COL_OTHER_ALLOWANCE);
            tableModel.setValueAt(formatNumber(parseFormattedNumber(mealAllowanceField.getText())), modelRow, COL_MEAL_ALLOWANCE);
            tableModel.setValueAt(formatNumber(parseFormattedNumber(vehicleMaintenanceFeeField.getText())), modelRow, COL_VEHICLE_FEE);
            tableModel.setValueAt(formatNumber(parseFormattedNumber(researchDevelopmentExpenseField.getText())), modelRow, COL_RESEARCH_EXPENSE);
            tableModel.setValueAt(formatNumber(parseFormattedNumber(childcareAllowanceField.getText())), modelRow, COL_CHILDCARE_ALLOWANCE);

            recalculateSalariesForTableRow(modelRow, false);

        } catch (NumberFormatException ex) {
            System.err.println("폼 -> 테이블 숫자 변환 오류: " + ex.getMessage());
        }
        modifiedRowModelIndices.add(modelRow);
        addOrUpdateTotalsRow();
        isUpdatingFromForm = false;
    }


    private void recalculateAndDisplaySalariesFromForm() {
        if (isUpdatingFromTable || isProgrammaticChange) return;
        try {
            isProgrammaticChange = true;

            long annualSalary = parseFormattedNumber(annualSalaryField.getText());
            long bonus = parseFormattedNumber(bonusField.getText());
            long other = parseFormattedNumber(otherAllowanceField.getText());
            long meal = parseFormattedNumber(mealAllowanceField.getText());
            long vehicle = parseFormattedNumber(vehicleMaintenanceFeeField.getText());
            long research = parseFormattedNumber(researchDevelopmentExpenseField.getText());
            long childcare = parseFormattedNumber(childcareAllowanceField.getText());

            CalculatedSalaryItems calculated = calculateNewContractualSalaryItems(
                    annualSalary, bonus, other, meal, vehicle, research, childcare
            );

            monthlyBasicSalaryField.setText(formatNumber(calculated.monthlyBasicSalary));
            fixedOvertimeAllowanceField.setText(formatNumber(calculated.fixedOvertimeAllowance));
            totalMonthlyPayField.setText(formatNumber(calculated.totalMonthlyPay));

            if (TOTAL_WORK_HOURS_DIVISOR > 0 && calculated.totalMonthlyPay > 0) {
                hourlyWageField.setText(formatNumber(calculated.totalMonthlyPay / TOTAL_WORK_HOURS_DIVISOR));
            } else {
                hourlyWageField.setText(formatNumber(0));
            }

        } catch (NumberFormatException ex) {
            hourlyWageField.setText("계산오류");
            monthlyBasicSalaryField.setText("계산오류");
            fixedOvertimeAllowanceField.setText("계산오류");
            totalMonthlyPayField.setText("계산오류");
            System.err.println("폼 급여 재계산 중 숫자 형식 오류: " + ex.getMessage());
        } finally {
            isProgrammaticChange = false;
        }
    }

    private void recalculateSalariesForTableRow(int modelRow, boolean updateFormIfSelected) {
        if (modelRow < 0 || modelRow >= tableModel.getRowCount() || isUpdatingFromForm || isProgrammaticChange ||
                (tableModel.getValueAt(modelRow, COL_NO) != null && tableModel.getValueAt(modelRow, COL_NO).equals("합계")) ) {
            return;
        }
        isProgrammaticChange = true;
        try {
            long annualSalary = parseFormattedNumber(tableModel.getValueAt(modelRow, COL_ANNUAL_SALARY).toString());
            long bonus = parseFormattedNumber(tableModel.getValueAt(modelRow, COL_BONUS).toString());
            long other = parseFormattedNumber(tableModel.getValueAt(modelRow, COL_OTHER_ALLOWANCE).toString());
            long meal = parseFormattedNumber(tableModel.getValueAt(modelRow, COL_MEAL_ALLOWANCE).toString());
            long vehicle = parseFormattedNumber(tableModel.getValueAt(modelRow, COL_VEHICLE_FEE).toString());
            long research = parseFormattedNumber(tableModel.getValueAt(modelRow, COL_RESEARCH_EXPENSE).toString());
            long childcare = parseFormattedNumber(tableModel.getValueAt(modelRow, COL_CHILDCARE_ALLOWANCE).toString());

            CalculatedSalaryItems calculated = calculateNewContractualSalaryItems(
                    annualSalary, bonus, other, meal, vehicle, research, childcare
            );

            tableModel.setValueAt(formatNumber(calculated.monthlyBasicSalary), modelRow, COL_MONTHLY_BASIC);
            tableModel.setValueAt(formatNumber(calculated.fixedOvertimeAllowance), modelRow, COL_FIXED_OVERTIME);
            tableModel.setValueAt(formatNumber(calculated.totalMonthlyPay), modelRow, COL_TOTAL_MONTHLY_PAY);

            if (updateFormIfSelected) {
                int selectedViewRow = employeeTable.getSelectedRow();
                if (selectedViewRow != -1 && employeeTable.convertRowIndexToModel(selectedViewRow) == modelRow) {
                    isUpdatingFromTable = true;
                    monthlyBasicSalaryField.setText(formatNumber(calculated.monthlyBasicSalary));
                    fixedOvertimeAllowanceField.setText(formatNumber(calculated.fixedOvertimeAllowance));
                    totalMonthlyPayField.setText(formatNumber(calculated.totalMonthlyPay));
                    if (TOTAL_WORK_HOURS_DIVISOR > 0 && calculated.totalMonthlyPay > 0) {
                        hourlyWageField.setText(formatNumber(calculated.totalMonthlyPay / TOTAL_WORK_HOURS_DIVISOR));
                    } else {
                        hourlyWageField.setText(formatNumber(0));
                    }
                    isUpdatingFromTable = false;
                }
            }

        } catch (NumberFormatException ex) {
            System.err.println("테이블 행 급여 재계산 오류 (행 " + modelRow + "): " + ex.getMessage());
            tableModel.setValueAt("오류", modelRow, COL_MONTHLY_BASIC);
            tableModel.setValueAt("오류", modelRow, COL_FIXED_OVERTIME);
            tableModel.setValueAt("오류", modelRow, COL_TOTAL_MONTHLY_PAY);
        } finally {
            isProgrammaticChange = false;
        }
    }

    private static class CalculatedSalaryItems {
        long monthlyBasicSalary;
        long fixedOvertimeAllowance;
        long totalMonthlyPay;

        CalculatedSalaryItems(long basic, long fixedOT, long total) {
            this.monthlyBasicSalary = basic;
            this.fixedOvertimeAllowance = fixedOT;
            this.totalMonthlyPay = total;
        }
    }

    private CalculatedSalaryItems calculateNewContractualSalaryItems(
            long annualSalary, long bonus, long otherAllowance, long mealAllowance,
            long vehicleMaintenanceFee, long researchDevelopmentExpense, long childcareAllowance) {

        if (annualSalary <= 0 && (bonus + otherAllowance + mealAllowance + vehicleMaintenanceFee + researchDevelopmentExpense + childcareAllowance <=0 )) {
            return new CalculatedSalaryItems(0, 0, 0);
        }

        double totalMonthlyPayFromAnnual = (double) annualSalary / 12.0;
        double hourlyRate = (TOTAL_WORK_HOURS_DIVISOR > 0) ? (totalMonthlyPayFromAnnual / TOTAL_WORK_HOURS_DIVISOR) : 0;

        double basicPortionFromRate = hourlyRate * HOURS_FOR_BASIC_PAY_CALC;

        long finalMonthlyBasicSalary = Math.round(basicPortionFromRate - mealAllowance - childcareAllowance - vehicleMaintenanceFee - researchDevelopmentExpense);
        finalMonthlyBasicSalary = Math.max(0, finalMonthlyBasicSalary);

        long finalFixedOvertimeAllowance = Math.round(hourlyRate * (FIXED_OVERTIME_HOURS_PARAM * 1.5));
        finalFixedOvertimeAllowance = Math.max(0, finalFixedOvertimeAllowance);

        long finalTotalMonthlyPay = finalMonthlyBasicSalary + finalFixedOvertimeAllowance +
                bonus + otherAllowance + mealAllowance +
                vehicleMaintenanceFee + researchDevelopmentExpense + childcareAllowance;

        return new CalculatedSalaryItems(finalMonthlyBasicSalary, finalFixedOvertimeAllowance, finalTotalMonthlyPay);
    }


    private void addNewEmptyRowToTable() {
        isProgrammaticChange = true;
        removeTotalsRow();

        // --- 설정에서 기본값 로드 ---
        Map<String, String> settings = payrollManager.loadSettings();
        long defaultAnnualSalary = 0;
        long defaultBonus = 0;
        long defaultOther = 0;
        long defaultMeal = Long.parseLong(settings.getOrDefault("defaultMealAllowance", "200000"));
        long defaultVehicle = Long.parseLong(settings.getOrDefault("defaultVehicleFee", "0"));
        long defaultResearch = Long.parseLong(settings.getOrDefault("defaultRdExpense", "0"));
        long defaultChildcare = Long.parseLong(settings.getOrDefault("defaultChildcareAllowance", "0"));
        // --- 설정 로드 끝 ---

        Object[] newRowData = new Object[tableModel.getColumnCount()];
        newRowData[COL_NO] = "*";
        newRowData[COL_HIRE_DATE] = DateTimeFormatter.ofPattern("yyyy-MM-dd").format(LocalDate.now());

        CalculatedSalaryItems calculatedDefaults = calculateNewContractualSalaryItems(
                defaultAnnualSalary, defaultBonus, defaultOther, defaultMeal, defaultVehicle, defaultResearch, defaultChildcare
        );

        newRowData[COL_ANNUAL_SALARY] = formatNumber(defaultAnnualSalary);
        newRowData[COL_MONTHLY_BASIC] = formatNumber(calculatedDefaults.monthlyBasicSalary);
        newRowData[COL_FIXED_OVERTIME] = formatNumber(calculatedDefaults.fixedOvertimeAllowance);
        newRowData[COL_ADDITIONAL_OVERTIME] = formatNumber(0);
        newRowData[COL_BONUS] = formatNumber(defaultBonus);
        newRowData[COL_OTHER_ALLOWANCE] = formatNumber(defaultOther);
        newRowData[COL_MEAL_ALLOWANCE] = formatNumber(defaultMeal);
        newRowData[COL_VEHICLE_FEE] = formatNumber(defaultVehicle);
        newRowData[COL_RESEARCH_EXPENSE] = formatNumber(defaultResearch);
        newRowData[COL_CHILDCARE_ALLOWANCE] = formatNumber(defaultChildcare);
        newRowData[COL_TOTAL_MONTHLY_PAY] = formatNumber(calculatedDefaults.totalMonthlyPay);

        newRowData[COL_NAME] = ""; newRowData[COL_DEPARTMENT] = ""; newRowData[COL_RESIDENT_REG_NO] = "";
        newRowData[COL_PHONE] = ""; newRowData[COL_ADDRESS] = "";

        tableModel.addRow(newRowData);
        addOrUpdateTotalsRow();

        int newRowViewIndex = employeeTable.convertRowIndexToView(tableModel.getRowCount() - 2);
        if (newRowViewIndex != -1) {
            employeeTable.setRowSelectionInterval(newRowViewIndex, newRowViewIndex);
            employeeTable.scrollRectToVisible(employeeTable.getCellRect(newRowViewIndex, 0, true));
        }
        isProgrammaticChange = false;

        updateFormFieldsFromSelectedTableRow(tableModel.getRowCount() - 2);
    }

    private void saveModifiedTableRows() {
        if (modifiedRowModelIndices.isEmpty()) {
            JOptionPane.showMessageDialog(this, "테이블에서 수정된 내용이 없습니다.", "알림", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int successCount = 0;
        int failCount = 0;
        Set<Integer> indicesToProcess = new HashSet<>(modifiedRowModelIndices);

        for (int modelRow : indicesToProcess) {
            if (modelRow >= displayedEmployeesInTableOrder.size() ||
                    displayedEmployeesInTableOrder.get(modelRow).getId() <=0 ||
                    (tableModel.getValueAt(modelRow, COL_NO) != null && tableModel.getValueAt(modelRow, COL_NO).equals("합계")) ||
                    (tableModel.getValueAt(modelRow, COL_NO) != null && tableModel.getValueAt(modelRow, COL_NO).equals("*")) ) {
                continue;
            }

            Employee originalEmployee = displayedEmployeesInTableOrder.get(modelRow);
            int employeeDbId = originalEmployee.getId();

            try {
                String name = tableModel.getValueAt(modelRow, COL_NAME).toString();
                String department = tableModel.getValueAt(modelRow, COL_DEPARTMENT).toString();
                String phone = tableModel.getValueAt(modelRow, COL_PHONE).toString();
                String address = tableModel.getValueAt(modelRow, COL_ADDRESS).toString();
                long annualSalary = parseFormattedNumber(tableModel.getValueAt(modelRow, COL_ANNUAL_SALARY).toString());
                String hireDate = tableModel.getValueAt(modelRow, COL_HIRE_DATE).toString();
                String residentRegNo = tableModel.getValueAt(modelRow, COL_RESIDENT_REG_NO).toString();

                String salaryChangeDateStr = originalEmployee.getSalaryChangeDate();
                String workLocation = originalEmployee.getWorkLocation();
                String siteLocation = originalEmployee.getSiteLocation();

                int selectedViewRow = employeeTable.getSelectedRow();
                if(selectedViewRow != -1 && employeeTable.convertRowIndexToModel(selectedViewRow) == modelRow) {
                    salaryChangeDateStr = salaryChangeDateField.getText();
                    workLocation = workLocationField.getText();
                    siteLocation = siteLocationField.getText();
                }

                if (name.isEmpty() || department.isEmpty() || residentRegNo.isEmpty()) {
                    failCount++; JOptionPane.showMessageDialog(this, (modelRow + 1) + "번째 행: 이름, 부서, 주민번호는 필수입니다.", "입력 오류", JOptionPane.ERROR_MESSAGE); continue;
                }
                if (!residentRegNo.matches("\\d{6}-\\d{7}")) {
                    failCount++; JOptionPane.showMessageDialog(this, (modelRow + 1) + "번째 행: 주민등록번호 형식이 올바르지 않습니다.", "입력 오류", JOptionPane.ERROR_MESSAGE); continue;
                }
                if (!isValidDateFormat(hireDate)) {
                    failCount++; JOptionPane.showMessageDialog(this, (modelRow+1) + "번째 행: 입사일 형식이 올바르지 않습니다 (YYYY-MM-DD).", "입력 오류", JOptionPane.ERROR_MESSAGE); continue;
                }
                if (salaryChangeDateStr != null && !salaryChangeDateStr.trim().isEmpty() && !isValidDateFormat(salaryChangeDateStr)) {
                    failCount++; JOptionPane.showMessageDialog(this, (modelRow+1) + "번째 행: 급여변동일 형식이 올바르지 않습니다 (YYYY-MM-DD).", "입력 오류", JOptionPane.ERROR_MESSAGE); continue;
                }

                Optional<Employee> existingEmpByRRN = payrollManager.getEmployeeByResidentRegNo(residentRegNo);
                if (existingEmpByRRN.isPresent() && existingEmpByRRN.get().getId() != employeeDbId) {
                    failCount++; JOptionPane.showMessageDialog(this, (modelRow + 1) + "번째 행: 해당 주민등록번호는 이미 다른 직원에게 사용 중입니다.", "입력 오류", JOptionPane.ERROR_MESSAGE); continue;
                }

                long bonus = parseFormattedNumber(tableModel.getValueAt(modelRow, COL_BONUS).toString());
                long otherAllowance = parseFormattedNumber(tableModel.getValueAt(modelRow, COL_OTHER_ALLOWANCE).toString());
                long mealAllowance = parseFormattedNumber(tableModel.getValueAt(modelRow, COL_MEAL_ALLOWANCE).toString());
                long vehicleFee = parseFormattedNumber(tableModel.getValueAt(modelRow, COL_VEHICLE_FEE).toString());
                long researchExpense = parseFormattedNumber(tableModel.getValueAt(modelRow, COL_RESEARCH_EXPENSE).toString());
                long childcareAllowance = parseFormattedNumber(tableModel.getValueAt(modelRow, COL_CHILDCARE_ALLOWANCE).toString());

                CalculatedSalaryItems calculatedPayrollItems = calculateNewContractualSalaryItems(
                        annualSalary, bonus, otherAllowance, mealAllowance, vehicleFee, researchExpense, childcareAllowance
                );

                Employee employeeToUpdate = new Employee(employeeDbId, name, residentRegNo, phone, (int)annualSalary, address, hireDate, salaryChangeDateStr == null || salaryChangeDateStr.trim().isEmpty() ? null : salaryChangeDateStr, department, workLocation, siteLocation);

                boolean empSuccess = payrollManager.updateEmployeeDetails(employeeToUpdate);
                boolean payrollSuccess = payrollManager.updatePayroll(
                        employeeToUpdate,
                        (int)calculatedPayrollItems.monthlyBasicSalary,
                        (int)bonus,
                        (int)calculatedPayrollItems.fixedOvertimeAllowance,
                        (int)otherAllowance,
                        (int)mealAllowance,
                        (int)vehicleFee,
                        (int)researchExpense,
                        (int)childcareAllowance
                );

                if (empSuccess || payrollSuccess) { successCount++;
                } else { failCount++; }
            } catch (Exception ex) {
                failCount++; JOptionPane.showMessageDialog(this, (modelRow + 1) + "번째 행 수정 중 오류: " + ex.getMessage(), "오류", JOptionPane.ERROR_MESSAGE); ex.printStackTrace();
            }
        }

        String message = "";
        if (successCount > 0) message += successCount + "명의 직원 정보가 성공적으로 수정되었습니다.";
        if (failCount > 0) message += (message.isEmpty() ? "" : "\n") + failCount + "건의 수정에 실패했습니다.";
        if (message.isEmpty()) message = "수정할 기존 직원이 없거나, 새 행은 '직원 등록'으로 처리됩니다.";

        JOptionPane.showMessageDialog(this, message, "수정 결과", JOptionPane.INFORMATION_MESSAGE);
        modifiedRowModelIndices.clear();
        loadEmployeeTable(searchField.getText().trim());
    }


    private void addNewEmployeeFromTableRowOrForm() {
        int processedCount = 0;
        int successCount = 0;
        int failCount = 0;
        List<Integer> rowsToRemoveAfterProcessing = new ArrayList<>();

        for (int modelRow = 0; modelRow < tableModel.getRowCount(); modelRow++) {
            if (tableModel.getValueAt(modelRow, COL_NO) != null &&
                    "*".equals(tableModel.getValueAt(modelRow, COL_NO).toString())) {

                processedCount++;
                try {
                    String name = tableModel.getValueAt(modelRow, COL_NAME) != null ? tableModel.getValueAt(modelRow, COL_NAME).toString() : "";
                    String department = tableModel.getValueAt(modelRow, COL_DEPARTMENT) != null ? tableModel.getValueAt(modelRow, COL_DEPARTMENT).toString() : "";
                    String resNo = tableModel.getValueAt(modelRow, COL_RESIDENT_REG_NO) != null ? tableModel.getValueAt(modelRow, COL_RESIDENT_REG_NO).toString() : "";
                    String phone = tableModel.getValueAt(modelRow, COL_PHONE) != null ? tableModel.getValueAt(modelRow, COL_PHONE).toString() : "";
                    String address = tableModel.getValueAt(modelRow, COL_ADDRESS) != null ? tableModel.getValueAt(modelRow, COL_ADDRESS).toString() : "";
                    String hireDate = tableModel.getValueAt(modelRow, COL_HIRE_DATE) != null ? tableModel.getValueAt(modelRow, COL_HIRE_DATE).toString() : "";

                    String salaryChangeDateStr = salaryChangeDateField.getText();
                    String workLocation = workLocationField.getText();
                    String siteLocation = siteLocationField.getText();

                    long annualSalary = parseFormattedNumber(tableModel.getValueAt(modelRow, COL_ANNUAL_SALARY).toString());
                    long bonus = parseFormattedNumber(tableModel.getValueAt(modelRow, COL_BONUS).toString());
                    long other = parseFormattedNumber(tableModel.getValueAt(modelRow, COL_OTHER_ALLOWANCE).toString());
                    long meal = parseFormattedNumber(tableModel.getValueAt(modelRow, COL_MEAL_ALLOWANCE).toString());
                    long vehicle = parseFormattedNumber(tableModel.getValueAt(modelRow, COL_VEHICLE_FEE).toString());
                    long research = parseFormattedNumber(tableModel.getValueAt(modelRow, COL_RESEARCH_EXPENSE).toString());
                    long childcare = parseFormattedNumber(tableModel.getValueAt(modelRow, COL_CHILDCARE_ALLOWANCE).toString());

                    if (name.isEmpty() || department == null || department.isEmpty() || resNo.isEmpty() || annualSalary <= 0 || hireDate.isEmpty()) {
                        JOptionPane.showMessageDialog(this, "행 " + (modelRow + 1) + ": 이름, 부서, 주민번호, 입사일, 연봉(0 초과)은 필수입니다.", "입력 오류", JOptionPane.ERROR_MESSAGE);
                        failCount++; continue;
                    }
                    if (!resNo.matches("\\d{6}-\\d{7}")) {
                        JOptionPane.showMessageDialog(this, "행 " + (modelRow + 1) + ": 주민등록번호 형식이 올바르지 않습니다 (xxxxxx-xxxxxxx).");
                        failCount++; continue;
                    }
                    if (payrollManager.getEmployeeByResidentRegNo(resNo).isPresent()) {
                        JOptionPane.showMessageDialog(this, "행 " + (modelRow + 1) + ": 이미 등록된 주민등록번호입니다.");
                        failCount++; continue;
                    }
                    if (!isValidDateFormat(hireDate)) {
                        JOptionPane.showMessageDialog(this, "행 " + (modelRow + 1) + ": 입사일 형식이 올바르지 않습니다 (YYYY-MM-DD).");
                        failCount++; continue;
                    }
                    if (salaryChangeDateStr != null && !salaryChangeDateStr.trim().isEmpty() && !isValidDateFormat(salaryChangeDateStr)) {
                        JOptionPane.showMessageDialog(this, "행 " + (modelRow + 1) + ": 급여변동일 형식이 올바르지 않습니다 (YYYY-MM-DD).");
                        failCount++; continue;
                    }


                    CalculatedSalaryItems calculatedPayrollItems = calculateNewContractualSalaryItems(
                            annualSalary, bonus, other, meal, vehicle, research, childcare
                    );
                    Employee newEmp = new Employee(name, resNo, phone, (int)annualSalary, address, hireDate, salaryChangeDateStr == null || salaryChangeDateStr.trim().isEmpty() ? null : salaryChangeDateStr, department, workLocation, siteLocation);

                    if (payrollManager.addEmployee(newEmp)) {
                        Optional<Employee> addedEmpOpt = payrollManager.getEmployeeByResidentRegNo(resNo);
                        if (addedEmpOpt.isPresent()) {
                            newEmp.setId(addedEmpOpt.get().getId());
                            payrollManager.updatePayroll(newEmp,
                                    (int)calculatedPayrollItems.monthlyBasicSalary, (int)bonus,
                                    (int)calculatedPayrollItems.fixedOvertimeAllowance, (int)other, (int)meal,
                                    (int)vehicle, (int)research, (int)childcare);
                            successCount++;
                            rowsToRemoveAfterProcessing.add(modelRow);
                        } else {
                            failCount++; payrollManager.deleteEmployeeById(newEmp.getId());
                        }
                    } else {
                        failCount++;
                    }
                } catch (Exception ex) {
                    failCount++;
                    JOptionPane.showMessageDialog(this, "행 " + (modelRow + 1) + " 처리 중 오류: " + ex.getMessage(), "오류", JOptionPane.ERROR_MESSAGE);
                    ex.printStackTrace();
                }
            }
        }

        if (processedCount == 0 && (selectedEmployeeDbId <= 0 && !nameField.getText().trim().isEmpty())) {
            try {
                String name = nameField.getText();
                String department = (String) departmentComboBox.getSelectedItem();
                String resNo = residentRegNoField.getText();
                String phone = phoneField.getText();
                String address = addressField.getText();
                String hireDate = hireDateField.getText();
                String salaryChangeDateStr = salaryChangeDateField.getText();
                String workLocation = workLocationField.getText();
                String siteLocation = siteLocationField.getText();

                long annualSalary = parseFormattedNumber(annualSalaryField.getText());
                long bonus = parseFormattedNumber(bonusField.getText());
                long other = parseFormattedNumber(otherAllowanceField.getText());
                long meal = parseFormattedNumber(mealAllowanceField.getText());
                long vehicle = parseFormattedNumber(vehicleMaintenanceFeeField.getText());
                long research = parseFormattedNumber(researchDevelopmentExpenseField.getText());
                long childcare = parseFormattedNumber(childcareAllowanceField.getText());

                if (name.isEmpty() || department == null || department.isEmpty() || resNo.isEmpty() || annualSalary <= 0 || hireDate.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "폼 입력: 이름, 부서, 주민번호, 입사일, 연봉(0 초과)은 필수입니다.", "입력 오류", JOptionPane.ERROR_MESSAGE);
                } else if (!resNo.matches("\\d{6}-\\d{7}")) {
                    JOptionPane.showMessageDialog(this, "폼 입력: 주민등록번호 형식이 올바르지 않습니다.");
                } else if (payrollManager.getEmployeeByResidentRegNo(resNo).isPresent()) {
                    JOptionPane.showMessageDialog(this, "폼 입력: 이미 등록된 주민등록번호입니다.");
                } else if (!isValidDateFormat(hireDate)) {
                    JOptionPane.showMessageDialog(this, "폼 입력: 입사일 형식이 올바르지 않습니다 (YYYY-MM-DD).");
                } else if (salaryChangeDateStr != null && !salaryChangeDateStr.trim().isEmpty() && !isValidDateFormat(salaryChangeDateStr)) {
                    JOptionPane.showMessageDialog(this, "폼 입력: 급여변동일 형식이 올바르지 않습니다 (YYYY-MM-DD).");
                } else {
                    CalculatedSalaryItems calculatedPayrollItems = calculateNewContractualSalaryItems(
                            annualSalary, bonus, other, meal, vehicle, research, childcare);
                    Employee newEmp = new Employee(name, resNo, phone, (int)annualSalary, address, hireDate, salaryChangeDateStr == null || salaryChangeDateStr.trim().isEmpty() ? null : salaryChangeDateStr, department, workLocation, siteLocation);

                    if (payrollManager.addEmployee(newEmp)) {
                        Optional<Employee> addedEmpOpt = payrollManager.getEmployeeByResidentRegNo(resNo);
                        if (addedEmpOpt.isPresent()) {
                            newEmp.setId(addedEmpOpt.get().getId());
                            payrollManager.updatePayroll(newEmp,
                                    (int)calculatedPayrollItems.monthlyBasicSalary, (int)bonus,
                                    (int)calculatedPayrollItems.fixedOvertimeAllowance, (int)other, (int)meal,
                                    (int)vehicle, (int)research, (int)childcare);
                            successCount++;
                        } else {
                            failCount++; payrollManager.deleteEmployeeById(newEmp.getId());
                        }
                    } else {
                        failCount++;
                    }
                }
            } catch (Exception ex) {
                failCount++;
                JOptionPane.showMessageDialog(this, "폼 데이터 처리 중 오류: " + ex.getMessage(), "오류", JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
            }
        }


        String message = "";
        if (successCount > 0) message += successCount + "명의 신규 직원이 등록되었습니다.";
        if (failCount > 0) message += (message.isEmpty() ? "" : "\n") + failCount + "건의 등록에 실패했습니다.";
        if (successCount == 0 && failCount == 0 && processedCount == 0 && (selectedEmployeeDbId > 0 || nameField.getText().trim().isEmpty() )) {
            message = "등록할 신규 직원 정보가 없습니다. 테이블에 새 행을 추가하고 내용을 입력하거나, 폼에서 새 직원 정보를 입력해주세요.";
        }


        if (!message.isEmpty()) {
            JOptionPane.showMessageDialog(this, message, "일괄 등록 결과", JOptionPane.INFORMATION_MESSAGE);
        }

        loadEmployeeTable(searchField.getText().trim());
        clearFieldsAndSelection();
    }


    private void deleteEmployee() {
        int currentIdToDelete = selectedEmployeeDbId;
        int selectedViewRow = employeeTable.getSelectedRow();

        if (selectedViewRow != -1) {
            int modelRow = employeeTable.convertRowIndexToModel(selectedViewRow);
            if (modelRow < tableModel.getRowCount() &&
                    tableModel.getValueAt(modelRow, COL_NO) != null &&
                    tableModel.getValueAt(modelRow, COL_NO).equals("합계")) {
                JOptionPane.showMessageDialog(this, "합계 행은 삭제할 수 없습니다.", "알림", JOptionPane.WARNING_MESSAGE);
                return;
            }

            if (modelRow < displayedEmployeesInTableOrder.size() &&
                    displayedEmployeesInTableOrder.get(modelRow) != null &&
                    displayedEmployeesInTableOrder.get(modelRow).getId() > 0) {
                currentIdToDelete = displayedEmployeesInTableOrder.get(modelRow).getId();
            } else if (modelRow < tableModel.getRowCount() &&
                    tableModel.getValueAt(modelRow, COL_NO) != null &&
                    "*".equals(tableModel.getValueAt(modelRow, COL_NO).toString())) {
                isProgrammaticChange = true;
                tableModel.removeRow(modelRow);
                addOrUpdateTotalsRow();
                isProgrammaticChange = false;
                modifiedRowModelIndices.remove(modelRow);
                JOptionPane.showMessageDialog(this, "테이블에서 새 입력 행이 삭제되었습니다.");
                clearFieldsAndSelection();
                return;
            }
        }

        if (currentIdToDelete <= 0 ) {
            JOptionPane.showMessageDialog(this, "삭제할 직원을 선택해주세요 (DB에 저장된 직원).");
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this, "정말로 이 직원을 삭제하시겠습니까? 관련 급여 데이터도 삭제됩니다.", "직원 삭제 확인", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            if (payrollManager.deleteEmployeeById(currentIdToDelete)) {
                JOptionPane.showMessageDialog(this, "직원이 성공적으로 삭제되었습니다.");
                loadEmployeeTable("");
                clearFieldsAndSelection();
            } else {
                JOptionPane.showMessageDialog(this, "직원 삭제 실패.");
            }
        }
    }

    private void clearFieldsAndSelection() {
        clearFields();
        modifiedRowModelIndices.clear();
        employeeTable.clearSelection();
    }

    public void clearFields() {
        isProgrammaticChange = true;
        selectedEmployeeDbId = -1;

        departmentComboBox.setSelectedIndex(0);
        nameField.setText(""); residentRegNoField.setText(""); phoneField.setText("");
        addressField.setText(""); hireDateField.setText(""); salaryChangeDateField.setText("");
        workLocationField.setText(""); siteLocationField.setText("");

        annualSalaryField.setText("");

        Map<String, String> settings = payrollManager.loadSettings();
        long defaultMeal = Long.parseLong(settings.getOrDefault("defaultMealAllowance", "200000"));
        long defaultChildcare = Long.parseLong(settings.getOrDefault("defaultChildcareAllowance", "0"));
        long defaultVehicle = Long.parseLong(settings.getOrDefault("defaultVehicleFee", "0"));
        long defaultResearch = Long.parseLong(settings.getOrDefault("defaultRdExpense", "0"));

        bonusField.setText(formatNumber(0));
        otherAllowanceField.setText(formatNumber(0));
        mealAllowanceField.setText(formatNumber(defaultMeal));
        vehicleMaintenanceFeeField.setText(formatNumber(defaultVehicle));
        researchDevelopmentExpenseField.setText(formatNumber(defaultResearch));
        childcareAllowanceField.setText(formatNumber(defaultChildcare));

        CalculatedSalaryItems clearedCalc = calculateNewContractualSalaryItems(0, 0, 0, defaultMeal, defaultVehicle, defaultResearch, defaultChildcare);
        monthlyBasicSalaryField.setText(formatNumber(clearedCalc.monthlyBasicSalary));
        fixedOvertimeAllowanceField.setText(formatNumber(clearedCalc.fixedOvertimeAllowance));
        totalMonthlyPayField.setText(formatNumber(clearedCalc.totalMonthlyPay));
        if (TOTAL_WORK_HOURS_DIVISOR > 0 && clearedCalc.totalMonthlyPay > 0) {
            hourlyWageField.setText(formatNumber(clearedCalc.totalMonthlyPay / TOTAL_WORK_HOURS_DIVISOR));
        } else {
            hourlyWageField.setText(formatNumber(0));
        }

        searchField.setText("");
        isProgrammaticChange = false;
    }

    private boolean isValidDateFormat(String date) {
        if (date == null || date.trim().isEmpty()) return true;
        try {
            LocalDate.parse(date, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    private void exportEmployeeTableToExcel() {
        if (tableModel.getRowCount() == 0 || (tableModel.getRowCount() == 1 && tableModel.getValueAt(0, COL_NO).equals("합계"))) {
            JOptionPane.showMessageDialog(this, "내보낼 데이터가 없습니다.", "알림", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("직원 정보 엑셀로 저장");
        fileChooser.setSelectedFile(new File("직원급여정보목록.xlsx"));
        fileChooser.setFileFilter(new FileNameExtensionFilter("Excel 통합 문서 (*.xlsx)", "xlsx"));

        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();
            if (!fileToSave.getName().toLowerCase().endsWith(".xlsx")) {
                fileToSave = new File(fileToSave.getParentFile(), fileToSave.getName() + ".xlsx");
            }

            if (fileToSave.exists()) {
                int response = JOptionPane.showConfirmDialog(this,
                        "이미 같은 이름의 파일이 존재합니다. 덮어쓰시겠습니까?",
                        "덮어쓰기 확인",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE);
                if (response == JOptionPane.NO_OPTION) {
                    return;
                }
            }

            try (XSSFWorkbook workbook = new XSSFWorkbook()) {
                Sheet sheet = workbook.createSheet("직원목록");

                CellStyle headerStyle = workbook.createCellStyle();
                org.apache.poi.ss.usermodel.Font poiHeaderFont = workbook.createFont();
                poiHeaderFont.setBold(true);
                poiHeaderFont.setFontHeightInPoints((short) 11);
                headerStyle.setFont(poiHeaderFont);
                headerStyle.setAlignment(HorizontalAlignment.CENTER);
                headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
                headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                headerStyle.setBorderTop(BorderStyle.THIN);
                headerStyle.setBorderBottom(BorderStyle.THIN);
                headerStyle.setBorderLeft(BorderStyle.THIN);
                headerStyle.setBorderRight(BorderStyle.THIN);

                CellStyle dataStyle = workbook.createCellStyle();
                dataStyle.setBorderTop(BorderStyle.DOTTED);dataStyle.setBorderBottom(BorderStyle.DOTTED);
                dataStyle.setBorderLeft(BorderStyle.DOTTED);dataStyle.setBorderRight(BorderStyle.DOTTED);
                dataStyle.setVerticalAlignment(VerticalAlignment.CENTER);

                CellStyle numberStyle = workbook.createCellStyle();
                numberStyle.cloneStyleFrom(dataStyle);
                DataFormat excelFormat = workbook.createDataFormat();
                numberStyle.setDataFormat(excelFormat.getFormat("#,##0"));
                numberStyle.setAlignment(HorizontalAlignment.RIGHT);

                CellStyle centerDataStyle = workbook.createCellStyle();
                centerDataStyle.cloneStyleFrom(dataStyle);
                centerDataStyle.setAlignment(HorizontalAlignment.CENTER);

                CellStyle totalsLabelStyle = workbook.createCellStyle();
                totalsLabelStyle.cloneStyleFrom(headerStyle);
                totalsLabelStyle.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
                totalsLabelStyle.setAlignment(HorizontalAlignment.CENTER);

                CellStyle totalsNumberStyle = workbook.createCellStyle();
                totalsNumberStyle.cloneStyleFrom(numberStyle);
                totalsNumberStyle.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
                totalsNumberStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                org.apache.poi.ss.usermodel.Font poiTotalsNumberFont = workbook.createFont();
                poiTotalsNumberFont.setBold(true);
                totalsNumberStyle.setFont(poiTotalsNumberFont);


                Row excelHeaderRow = sheet.createRow(0);
                for (int col = 0; col < tableModel.getColumnCount(); col++) {
                    Cell cell = excelHeaderRow.createCell(col);
                    cell.setCellValue(tableModel.getColumnName(col));
                    cell.setCellStyle(headerStyle);
                }

                int excelRowIdx = 0;
                for (int row = 0; row < tableModel.getRowCount(); row++) {
                    boolean isTotalsRow = tableModel.getValueAt(row, COL_NO) != null && tableModel.getValueAt(row, COL_NO).equals("합계");

                    Row dataRow = sheet.createRow(excelRowIdx + 1);
                    for (int col = 0; col < tableModel.getColumnCount(); col++) {
                        Object cellValue = tableModel.getValueAt(row, col);
                        Cell cell = dataRow.createCell(col);

                        if (isTotalsRow) {
                            if (col == COL_NO) {
                                cell.setCellValue(cellValue.toString());
                                cell.setCellStyle(totalsLabelStyle);
                            } else {
                                boolean isSumCol = false;
                                for(int sumIdx : sumColumnIndices){
                                    if(col == sumIdx){
                                        isSumCol = true;
                                        break;
                                    }
                                }
                                if(isSumCol && cellValue != null && !cellValue.toString().isEmpty()){
                                    try {
                                        cell.setCellValue(Double.parseDouble(cellValue.toString().replace(",", "")));
                                        cell.setCellStyle(totalsNumberStyle);
                                    } catch (NumberFormatException e) {
                                        cell.setCellValue(cellValue.toString());
                                        cell.setCellStyle(totalsLabelStyle);
                                    }
                                } else {
                                    cell.setCellValue("");
                                    cell.setCellStyle(totalsLabelStyle);
                                }
                            }
                        } else {
                            if (cellValue != null) {
                                if (col >= COL_ANNUAL_SALARY && col <= COL_TOTAL_MONTHLY_PAY ) {
                                    try {
                                        cell.setCellValue(Double.parseDouble(cellValue.toString().replace(",", "")));
                                        cell.setCellStyle(numberStyle);
                                    } catch (NumberFormatException e) {
                                        cell.setCellValue(cellValue.toString());
                                        if ("-".equals(cellValue.toString()) || "오류".equals(cellValue.toString())) {
                                            cell.setCellStyle(centerDataStyle);
                                        } else {
                                            cell.setCellStyle(dataStyle);
                                        }
                                    }
                                } else {
                                    cell.setCellValue(cellValue.toString());
                                    if(col == COL_NO || col == COL_HIRE_DATE || cellValue.toString().equals("-") || cellValue.toString().equals("*")) {
                                        cell.setCellStyle(centerDataStyle);
                                    } else {
                                        cell.setCellStyle(dataStyle);
                                    }
                                }
                            } else {
                                cell.setCellStyle(dataStyle);
                            }
                        }
                    }
                    excelRowIdx++;
                }

                for (int col = 0; col < tableModel.getColumnCount(); col++) {
                    sheet.autoSizeColumn(col);
                }

                try (FileOutputStream fileOut = new FileOutputStream(fileToSave)) {
                    workbook.write(fileOut);
                }
                JOptionPane.showMessageDialog(this, "엑셀 파일이 성공적으로 저장되었습니다:\n" + fileToSave.getAbsolutePath(), "저장 완료", JOptionPane.INFORMATION_MESSAGE);

            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "엑셀 파일 저장/생성 중 오류: " + ex.getMessage(), "오류", JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
            }
        }
    }

    static class TotalsAwareNumberRenderer extends DefaultTableCellRenderer {
        private Font normalFont;
        private Font totalsFont;

        public TotalsAwareNumberRenderer(Font normalFont, Font totalsFont) {
            this.normalFont = normalFont;
            this.totalsFont = totalsFont;
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            boolean isTotalsRow = false;
            if (table.getModel().getValueAt(row, COL_NO) != null &&
                    table.getModel().getValueAt(row, COL_NO).equals("합계")) {
                isTotalsRow = true;
            }

            if (isTotalsRow) {
                c.setFont(totalsFont);
                c.setBackground(new Color(230, 230, 230));
                if(isSelected) c.setBackground(table.getSelectionBackground().darker());
            } else {
                c.setFont(normalFont);
                c.setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
            }
            c.setForeground(isSelected ? table.getSelectionForeground() : table.getForeground());


            boolean isNumericColumn = false;
            for(int sumIdx : ((EmployeeManagementPage) SwingUtilities.getAncestorOfClass(EmployeeManagementPage.class, table)).sumColumnIndices){
                if(column == sumIdx){
                    isNumericColumn = true;
                    break;
                }
            }

            if (isNumericColumn) {
                if (value instanceof Number) {
                    NumberFormat nf = NumberFormat.getNumberInstance(Locale.KOREA);
                    setText(nf.format(value));
                } else if (value instanceof String) {
                    try {
                        if (!"-".equals(value.toString()) && !value.toString().trim().isEmpty() && !"오류".equals(value.toString())) {
                            String cleanedValue = ((String) value).replace(",", "");
                            long numValue = Long.parseLong(cleanedValue);
                            NumberFormat nf = NumberFormat.getNumberInstance(Locale.KOREA);
                            setText(nf.format(numValue));
                        } else {
                            setText((String) value);
                        }
                    } catch (NumberFormatException e) {
                        setText((String) value);
                    }
                } else if (value == null) {
                    setText("");
                } else {
                    setText(value.toString());
                }
                setHorizontalAlignment(JLabel.RIGHT);
            } else if (column == COL_NO || column == COL_HIRE_DATE) {
                setHorizontalAlignment(JLabel.CENTER);
                setText(value != null ? value.toString() : "");
            } else {
                setHorizontalAlignment(JLabel.LEFT);
                setText(value != null ? value.toString() : "");
            }
            return c;
        }
    }
}