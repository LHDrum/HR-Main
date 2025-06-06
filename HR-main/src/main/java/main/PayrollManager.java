package main;

import java.sql.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.swing.JOptionPane;
import java.io.File;

public class PayrollManager {
    // H2 데이터베이스 파일 경로 설정
    private static final String DB_APP_FOLDER_NAME = ".HR_Payroll_App_Data";
    private static final String DB_FILE_NAME = "payroll_hr_db";
    private static final String DB_FOLDER_PATH = System.getProperty("user.home") + File.separator + DB_APP_FOLDER_NAME;
    private static final String DB_URL = "jdbc:h2:file:" + DB_FOLDER_PATH + File.separator + DB_FILE_NAME + ";AUTO_SERVER=TRUE;DB_CLOSE_DELAY=-1";
    private static final String DB_USER = "sa";
    private static final String DB_PASSWORD = "";

    public PayrollManager() {
        try {
            File dbAppFolder = new File(DB_FOLDER_PATH);
            if (!dbAppFolder.exists()) {
                if (!dbAppFolder.mkdirs()) {
                    System.err.println("애플리케이션 데이터 폴더 생성 실패: " + DB_FOLDER_PATH);
                    JOptionPane.showMessageDialog(null, "오류: 애플리케이션 데이터 폴더를 생성할 수 없습니다.\n" + DB_FOLDER_PATH + "\n프로그램을 종료합니다.", "치명적 오류", JOptionPane.ERROR_MESSAGE);
                    System.exit(1);
                }
            }
            try {
                Class.forName("org.h2.Driver");
            } catch (ClassNotFoundException cnfe) {
                System.err.println("H2 JDBC 드라이버를 찾을 수 없습니다.");
                cnfe.printStackTrace();
                JOptionPane.showMessageDialog(null, "오류: H2 데이터베이스 드라이버를 찾을 수 없습니다.\n프로그램 실행에 필요한 파일이 누락되었습니다.", "드라이버 오류", JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }
            try (Connection conn = getConnection()) {
                System.out.println("H2 데이터베이스에 성공적으로 연결되었습니다.");
                setupDatabase();
            }
        } catch (SQLException e) {
            System.err.println("### 오류: H2 데이터베이스 연결 또는 초기화 실패 ###");
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "오류: 데이터베이스 초기화에 실패했습니다.\n프로그램을 정상적으로 사용할 수 없습니다.\n오류: " + e.getMessage(), "데이터베이스 오류", JOptionPane.ERROR_MESSAGE);
        }
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }

    public void setupDatabase() {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS employees (id INT PRIMARY KEY AUTO_INCREMENT, name VARCHAR(100) NOT NULL, resident_registration_number VARCHAR(20) UNIQUE NOT NULL, phone_number VARCHAR(20), annual_salary INT NOT NULL, address VARCHAR(255) DEFAULT NULL, hire_date VARCHAR(10) DEFAULT NULL, salary_change_date VARCHAR(10) DEFAULT NULL, department VARCHAR(50) DEFAULT NULL, work_location VARCHAR(255) DEFAULT NULL, site_location VARCHAR(255) DEFAULT NULL);");
            System.out.println("'employees' 테이블 준비 완료.");

            String createPayrollsTableSQL = "CREATE TABLE IF NOT EXISTS payrolls (" +
                    "id INT PRIMARY KEY AUTO_INCREMENT, employee_id INT NOT NULL, " +
                    "payment_year INT, payment_month INT, " +
                    "monthly_basic_salary INT DEFAULT 0, bonus INT DEFAULT 0, fixed_overtime_allowance INT DEFAULT 0, " +
                    "additional_overtime_premium INT DEFAULT 0, other_allowance INT DEFAULT 0, meal_allowance INT DEFAULT 0, " +
                    "vehicle_maintenance_fee INT DEFAULT 0, research_development_expense INT DEFAULT 0, childcare_allowance INT DEFAULT 0, " +
                    "unpaid_days INT DEFAULT 0, unauthorized_absence_days INT DEFAULT 0, " +
                    "national_pension_employee DOUBLE DEFAULT 0.0, health_insurance_employee DOUBLE DEFAULT 0.0, " +
                    "long_term_care_insurance_employee DOUBLE DEFAULT 0.0, employment_insurance_employee DOUBLE DEFAULT 0.0, " +
                    "income_tax DOUBLE DEFAULT 0.0, local_income_tax DOUBLE DEFAULT 0.0, " +
                    "total_employee_deduction DOUBLE DEFAULT 0.0, " +
                    "national_pension_employer DOUBLE DEFAULT 0.0, health_insurance_employer DOUBLE DEFAULT 0.0, " +
                    "long_term_care_insurance_employer DOUBLE DEFAULT 0.0, employment_insurance_employer DOUBLE DEFAULT 0.0, " +
                    "industrial_accident_insurance_employer DOUBLE DEFAULT 0.0, " +
                    "net_pay DOUBLE DEFAULT 0.0, " +
                    "FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE CASCADE, " +
                    "CONSTRAINT uk_employee_period UNIQUE (employee_id, payment_year, payment_month)" +
                    ");";
            stmt.executeUpdate(createPayrollsTableSQL);
            System.out.println("'payrolls' 테이블 준비 완료 (공제 컬럼 포함).");

            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS work_records (id INT PRIMARY KEY AUTO_INCREMENT, employee_id INT NOT NULL, work_date DATE NOT NULL, start_time TIME NULL, end_time TIME NULL, work_status VARCHAR(20) NOT NULL, FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE CASCADE, CONSTRAINT uk_employee_work_date UNIQUE (employee_id, work_date));");
            System.out.println("'work_records' 테이블 준비 완료.");
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS app_settings (setting_key VARCHAR(255) PRIMARY KEY, setting_value VARCHAR(255) NOT NULL);");
            System.out.println("'app_settings' 테이블 준비 완료.");
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS annual_leaves (id INT PRIMARY KEY AUTO_INCREMENT, employee_id INT NOT NULL, leave_year INT NOT NULL, total_generated_days DOUBLE DEFAULT 0.0, adjustment_days DOUBLE DEFAULT 0.0, notes VARCHAR(255), FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE CASCADE, CONSTRAINT uk_employee_leave_year UNIQUE (employee_id, leave_year));");
            System.out.println("'annual_leaves' 테이블 준비 완료.");
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS leave_usage_records (id INT PRIMARY KEY AUTO_INCREMENT, employee_id INT NOT NULL, leave_date DATE NOT NULL, leave_type VARCHAR(50) NOT NULL, deduct_days DOUBLE DEFAULT 1.0, FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE CASCADE, CONSTRAINT uk_employee_leave_date UNIQUE (employee_id, leave_date));");
            System.out.println("'leave_usage_records' 테이블 준비 완료.");

        } catch (SQLException e) {
            System.err.println("DB 테이블 준비 중 오류: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void saveSettings(Map<String, String> settings) {
        String sql = "MERGE INTO app_settings (setting_key, setting_value) KEY(setting_key) VALUES (?, ?)";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            for (Map.Entry<String, String> entry : settings.entrySet()) {
                pstmt.setString(1, entry.getKey());
                pstmt.setString(2, entry.getValue());
                pstmt.addBatch();
            }
            pstmt.executeBatch();
        } catch (SQLException e) {
            System.err.println("설정 저장 실패: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public Map<String, String> loadSettings() {
        Map<String, String> settings = new HashMap<>();
        String sql = "SELECT setting_key, setting_value FROM app_settings";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                settings.put(rs.getString("setting_key"), rs.getString("setting_value"));
            }
        } catch (SQLException e) {
            System.err.println("설정 로드 실패: " + e.getMessage());
            e.printStackTrace();
        }
        return settings;
    }

    public boolean addEmployee(Employee employee) {
        if (getEmployeeByResidentRegNo(employee.getResidentRegistrationNumber()).isPresent()) {
            System.err.println("주민등록번호가 중복되는 직원이 이미 존재합니다: " + employee.getResidentRegistrationNumber());
            return false;
        }
        String sql = "INSERT INTO employees (name, resident_registration_number, phone_number, annual_salary, address, hire_date, salary_change_date, department, work_location, site_location) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, employee.getName());
            pstmt.setString(2, employee.getResidentRegistrationNumber());
            pstmt.setString(3, employee.getPhoneNumber());
            pstmt.setInt(4, employee.getAnnualSalary());
            pstmt.setString(5, employee.getAddress());
            pstmt.setString(6, employee.getHireDate());
            pstmt.setString(7, employee.getSalaryChangeDate());
            pstmt.setString(8, employee.getDepartment());
            pstmt.setString(9, employee.getWorkLocation());
            pstmt.setString(10, employee.getSiteLocation());
            int affectedRows = pstmt.executeUpdate();

            if (affectedRows > 0) {
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        employee.setId(rs.getInt(1));
                    }
                }
                System.out.println(employee.getName() + " 직원 정보가 DB에 추가되었습니다.");
                return true;
            }
            return false;
        } catch (SQLException e) {
            System.err.println("직원 추가 실패: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public Optional<Employee> getEmployeeById(int id) {
        String sql = "SELECT * FROM employees WHERE id = ?";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new Employee(rs.getInt("id"), rs.getString("name"), rs.getString("resident_registration_number"), rs.getString("phone_number"), rs.getInt("annual_salary"), rs.getString("address"), rs.getString("hire_date"), rs.getString("salary_change_date"), rs.getString("department"), rs.getString("work_location"), rs.getString("site_location")));
                }
            }
        } catch (SQLException e) {
            System.err.println("직원 ID 조회 실패: " + e.getMessage());
            e.printStackTrace();
        }
        return Optional.empty();
    }

    public Optional<Employee> getEmployeeByResidentRegNo(String residentRegNo) {
        String sql = "SELECT * FROM employees WHERE resident_registration_number = ?";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, residentRegNo);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new Employee(rs.getInt("id"), rs.getString("name"), rs.getString("resident_registration_number"), rs.getString("phone_number"), rs.getInt("annual_salary"), rs.getString("address"), rs.getString("hire_date"), rs.getString("salary_change_date"), rs.getString("department"), rs.getString("work_location"), rs.getString("site_location")));
                }
            }
        } catch (SQLException e) {
            System.err.println("주민번호 직원 조회 실패: " + e.getMessage());
            e.printStackTrace();
        }
        return Optional.empty();
    }

    public Optional<Employee> getEmployeeByName(String name) {
        String sql = "SELECT * FROM employees WHERE name = ?";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, name);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new Employee(rs.getInt("id"), rs.getString("name"), rs.getString("resident_registration_number"), rs.getString("phone_number"), rs.getInt("annual_salary"), rs.getString("address"), rs.getString("hire_date"), rs.getString("salary_change_date"), rs.getString("department"), rs.getString("work_location"), rs.getString("site_location")));
                }
            }
        } catch (SQLException e) {
            System.err.println("직원 이름 '" + name + "' 조회 실패: " + e.getMessage());
        }
        return Optional.empty();
    }

    public List<Employee> getAllEmployees() {
        List<Employee> employeeList = new ArrayList<>();
        String sql = "SELECT * FROM employees";
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                employeeList.add(new Employee(rs.getInt("id"), rs.getString("name"), rs.getString("resident_registration_number"), rs.getString("phone_number"), rs.getInt("annual_salary"), rs.getString("address"), rs.getString("hire_date"), rs.getString("salary_change_date"), rs.getString("department"), rs.getString("work_location"), rs.getString("site_location")));
            }
        } catch (SQLException e) {
            System.err.println("모든 직원 조회 실패: " + e.getMessage());
            e.printStackTrace();
        }
        return employeeList;
    }

    public boolean updateEmployeeDetails(Employee employee) {
        String sql = "UPDATE employees SET name = ?, resident_registration_number = ?, phone_number = ?, annual_salary = ?, address = ?, hire_date = ?, salary_change_date = ?, department = ?, work_location = ?, site_location = ? WHERE id = ?";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, employee.getName());
            pstmt.setString(2, employee.getResidentRegistrationNumber());
            pstmt.setString(3, employee.getPhoneNumber());
            pstmt.setInt(4, employee.getAnnualSalary());
            pstmt.setString(5, employee.getAddress());
            pstmt.setString(6, employee.getHireDate());
            pstmt.setString(7, employee.getSalaryChangeDate());
            pstmt.setString(8, employee.getDepartment());
            pstmt.setString(9, employee.getWorkLocation());
            pstmt.setString(10, employee.getSiteLocation());
            pstmt.setInt(11, employee.getId());
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                System.out.println(employee.getName() + "님의 직원 정보가 DB에서 업데이트되었습니다.");
            }
            return affectedRows > 0;
        } catch (SQLException e) {
            System.err.println("직원 정보 업데이트 실패: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public boolean deleteEmployeeById(int employeeId) {
        String sqlDeleteEmployee = "DELETE FROM employees WHERE id = ?";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sqlDeleteEmployee)) {
            pstmt.setInt(1, employeeId);
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                System.out.println("직원 ID " + employeeId + "의 정보가 DB에서 삭제되었습니다.");
            }
            return affectedRows > 0;
        } catch (SQLException e) {
            System.err.println("직원 삭제 실패: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public boolean updatePayroll(Employee employee, int monthlyBasicSalary, int bonus,
                                 int fixedOvertimeAllowance, int otherAllowance, int mealAllowance,
                                 int vehicleMaintenanceFee, int researchDevelopmentExpense, int childcareAllowance) {
        String sqlSelectPayroll = "SELECT id FROM payrolls WHERE employee_id = ? AND payment_year IS NULL AND payment_month IS NULL";
        String sqlUpdatePayroll = "UPDATE payrolls SET monthly_basic_salary = ?, bonus = ?, fixed_overtime_allowance = ?, other_allowance = ?, meal_allowance = ?, vehicle_maintenance_fee = ?, research_development_expense = ?, childcare_allowance = ? WHERE employee_id = ? AND payment_year IS NULL AND payment_month IS NULL";
        String sqlInsertPayroll = "INSERT INTO payrolls (employee_id, monthly_basic_salary, bonus, fixed_overtime_allowance, other_allowance, meal_allowance, vehicle_maintenance_fee, research_development_expense, childcare_allowance, payment_year, payment_month) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, NULL, NULL)";

        try (Connection conn = getConnection()) {
            PreparedStatement pstmtSelect = conn.prepareStatement(sqlSelectPayroll);
            pstmtSelect.setInt(1, employee.getId());
            ResultSet rs = pstmtSelect.executeQuery();

            if (rs.next()) {
                try (PreparedStatement pstmtUpdate = conn.prepareStatement(sqlUpdatePayroll)) {
                    pstmtUpdate.setInt(1, monthlyBasicSalary);
                    pstmtUpdate.setInt(2, bonus);
                    pstmtUpdate.setInt(3, fixedOvertimeAllowance);
                    pstmtUpdate.setInt(4, otherAllowance);
                    pstmtUpdate.setInt(5, mealAllowance);
                    pstmtUpdate.setInt(6, vehicleMaintenanceFee);
                    pstmtUpdate.setInt(7, researchDevelopmentExpense);
                    pstmtUpdate.setInt(8, childcareAllowance);
                    pstmtUpdate.setInt(9, employee.getId());
                    pstmtUpdate.executeUpdate();
                }
            } else {
                try (PreparedStatement pstmtInsert = conn.prepareStatement(sqlInsertPayroll)) {
                    pstmtInsert.setInt(1, employee.getId());
                    pstmtInsert.setInt(2, monthlyBasicSalary);
                    pstmtInsert.setInt(3, bonus);
                    pstmtInsert.setInt(4, fixedOvertimeAllowance);
                    pstmtInsert.setInt(5, otherAllowance);
                    pstmtInsert.setInt(6, mealAllowance);
                    pstmtInsert.setInt(7, vehicleMaintenanceFee);
                    pstmtInsert.setInt(8, researchDevelopmentExpense);
                    pstmtInsert.setInt(9, childcareAllowance);
                    pstmtInsert.executeUpdate();
                }
            }
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public Optional<Payroll> getContractualPayroll(int employeeId) {
        String sql = "SELECT * FROM payrolls WHERE employee_id = ? AND payment_year IS NULL AND payment_month IS NULL";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, employeeId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Payroll payroll = createPayrollFromResultSet(rs);
                    return Optional.of(payroll);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    public List<Payroll> getPayrollsForPeriod(int year, int month) {
        List<Payroll> payrollList = new ArrayList<>();
        String sql = "SELECT p.*, e.name, e.resident_registration_number, e.phone_number, e.annual_salary, e.address, e.hire_date, e.salary_change_date, e.department, e.work_location, e.site_location " +
                "FROM payrolls p JOIN employees e ON p.employee_id = e.id " +
                "WHERE p.payment_year = ? AND p.payment_month = ?";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, year);
            pstmt.setInt(2, month);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Payroll payroll = createPayrollFromResultSet(rs);
                    Employee employee = new Employee(rs.getInt("employee_id"), rs.getString("name"), rs.getString("resident_registration_number"), rs.getString("phone_number"), rs.getInt("annual_salary"), rs.getString("address"), rs.getString("hire_date"), rs.getString("salary_change_date"), rs.getString("department"), rs.getString("work_location"), rs.getString("site_location"));
                    payroll.setEmployee(employee);
                    payrollList.add(payroll);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return payrollList;
    }

    private Payroll createPayrollFromResultSet(ResultSet rs) throws SQLException {
        return new Payroll(
                rs.getInt("id"), rs.getInt("employee_id"),
                (Integer) rs.getObject("payment_year"), (Integer) rs.getObject("payment_month"),
                rs.getInt("monthly_basic_salary"), rs.getInt("bonus"), rs.getInt("fixed_overtime_allowance"), rs.getInt("additional_overtime_premium"),
                rs.getInt("other_allowance"), rs.getInt("meal_allowance"), rs.getInt("vehicle_maintenance_fee"), rs.getInt("research_development_expense"), rs.getInt("childcare_allowance"),
                rs.getInt("unpaid_days"), rs.getInt("unauthorized_absence_days"),
                rs.getDouble("national_pension_employee"), rs.getDouble("health_insurance_employee"), rs.getDouble("long_term_care_insurance_employee"), rs.getDouble("employment_insurance_employee"),
                rs.getDouble("income_tax"), rs.getDouble("local_income_tax"), rs.getDouble("total_employee_deduction"),
                rs.getDouble("national_pension_employer"), rs.getDouble("health_insurance_employer"), rs.getDouble("long_term_care_insurance_employer"), rs.getDouble("employment_insurance_employer"),
                rs.getDouble("industrial_accident_insurance_employer"), rs.getDouble("net_pay")
        );
    }

    public void finalizeMonthlyPayAndDeductions(int employeeId, YearMonth period,
                                                double adjustedBasic, double adjustedFixedOvertime, double additionalOvertime,
                                                double adjustedBonus, double adjustedOther, double adjustedMeal,
                                                double adjustedVehicle, double adjustedResearch, double adjustedChildcare,
                                                double industrialAccidentRate, int dependents,
                                                int unpaidDays, int unauthorizedAbsenceDays) {

        double grossPay = adjustedBasic + adjustedFixedOvertime + additionalOvertime + adjustedBonus + adjustedOther +
                adjustedMeal + adjustedVehicle + adjustedResearch + adjustedChildcare;

        DeductionResult result = DeductionCalculator.calculate(grossPay, industrialAccidentRate, dependents);

        String selectSql = "SELECT id FROM payrolls WHERE employee_id = ? AND payment_year = ? AND payment_month = ?";
        String updateSql = "UPDATE payrolls SET monthly_basic_salary=?, fixed_overtime_allowance=?, additional_overtime_premium=?, bonus=?, other_allowance=?, meal_allowance=?, vehicle_maintenance_fee=?, research_development_expense=?, childcare_allowance=?, unpaid_days=?, unauthorized_absence_days=?, national_pension_employee=?, health_insurance_employee=?, long_term_care_insurance_employee=?, employment_insurance_employee=?, income_tax=?, local_income_tax=?, total_employee_deduction=?, national_pension_employer=?, health_insurance_employer=?, long_term_care_insurance_employer=?, employment_insurance_employer=?, industrial_accident_insurance_employer=?, net_pay=? WHERE id=?";
        String insertSql = "INSERT INTO payrolls (employee_id, payment_year, payment_month, monthly_basic_salary, fixed_overtime_allowance, additional_overtime_premium, bonus, other_allowance, meal_allowance, vehicle_maintenance_fee, research_development_expense, childcare_allowance, unpaid_days, unauthorized_absence_days, national_pension_employee, health_insurance_employee, long_term_care_insurance_employee, employment_insurance_employee, income_tax, local_income_tax, total_employee_deduction, national_pension_employer, health_insurance_employer, long_term_care_insurance_employer, employment_insurance_employer, industrial_accident_insurance_employer, net_pay) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

        try (Connection conn = getConnection()) {
            PreparedStatement pstmtSelect = conn.prepareStatement(selectSql);
            pstmtSelect.setInt(1, employeeId);
            pstmtSelect.setInt(2, period.getYear());
            pstmtSelect.setInt(3, period.getMonthValue());
            ResultSet rs = pstmtSelect.executeQuery();

            if (rs.next()) {
                int payrollId = rs.getInt("id");
                try (PreparedStatement pstmt = conn.prepareStatement(updateSql)) {
                    setAllPayrollParams(pstmt, result, adjustedBasic, adjustedFixedOvertime, additionalOvertime, adjustedBonus, adjustedOther, adjustedMeal, adjustedVehicle, adjustedResearch, adjustedChildcare, unpaidDays, unauthorizedAbsenceDays);
                    pstmt.setInt(25, payrollId);
                    pstmt.executeUpdate();
                }
            } else {
                try (PreparedStatement pstmt = conn.prepareStatement(insertSql)) {
                    pstmt.setInt(1, employeeId);
                    pstmt.setInt(2, period.getYear());
                    pstmt.setInt(3, period.getMonthValue());
                    setAllPayrollParams(pstmt, result, adjustedBasic, adjustedFixedOvertime, additionalOvertime, adjustedBonus, adjustedOther, adjustedMeal, adjustedVehicle, adjustedResearch, adjustedChildcare, unpaidDays, unauthorizedAbsenceDays);
                    pstmt.executeUpdate();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void setAllPayrollParams(PreparedStatement pstmt, DeductionResult result,
                                     double basic, double fixedOt, double additionalOt, double bonus, double other,
                                     double meal, double vehicle, double research, double childcare,
                                     int unpaid, int absence) throws SQLException {
        int i = pstmt.getParameterMetaData().getParameterCount() > 25 ? 4 : 1;

        pstmt.setInt(i++, (int) Math.round(basic));
        pstmt.setInt(i++, (int) Math.round(fixedOt));
        pstmt.setInt(i++, (int) Math.round(additionalOt));
        pstmt.setInt(i++, (int) Math.round(bonus));
        pstmt.setInt(i++, (int) Math.round(other));
        pstmt.setInt(i++, (int) Math.round(meal));
        pstmt.setInt(i++, (int) Math.round(vehicle));
        pstmt.setInt(i++, (int) Math.round(research));
        pstmt.setInt(i++, (int) Math.round(childcare));
        pstmt.setInt(i++, unpaid);
        pstmt.setInt(i++, absence);

        pstmt.setDouble(i++, result.nationalPensionEmployee);
        pstmt.setDouble(i++, result.healthInsuranceEmployee);
        pstmt.setDouble(i++, result.longTermCareInsuranceEmployee);
        pstmt.setDouble(i++, result.employmentInsuranceEmployee);
        pstmt.setDouble(i++, result.incomeTax);
        pstmt.setDouble(i++, result.localIncomeTax);
        pstmt.setDouble(i++, result.totalEmployeeDeduction);

        pstmt.setDouble(i++, result.nationalPensionEmployer);
        pstmt.setDouble(i++, result.healthInsuranceEmployer);
        pstmt.setDouble(i++, result.longTermCareInsuranceEmployer);
        pstmt.setDouble(i++, result.employmentInsuranceEmployer);
        pstmt.setDouble(i++, result.industrialAccidentInsuranceEmployer);

        pstmt.setDouble(i++, result.netPay);
    }

    public boolean deletePayrollForPeriod(int employeeId, int year, int month) {
        String sql = "DELETE FROM payrolls WHERE employee_id = ? AND payment_year = ? AND payment_month = ?";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, employeeId);
            pstmt.setInt(2, year);
            pstmt.setInt(3, month);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public void saveWorkRecords(int employeeId, YearMonth yearMonth, List<WorkLogEntry> records) {
        String deleteSql = "DELETE FROM work_records WHERE employee_id = ? AND YEAR(work_date) = ? AND MONTH(work_date) = ?";
        String insertSql = "INSERT INTO work_records (employee_id, work_date, start_time, end_time, work_status) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement pstmtDelete = conn.prepareStatement(deleteSql)) {
                pstmtDelete.setInt(1, employeeId);
                pstmtDelete.setInt(2, yearMonth.getYear());
                pstmtDelete.setInt(3, yearMonth.getMonthValue());
                pstmtDelete.executeUpdate();
            }
            try (PreparedStatement pstmtInsert = conn.prepareStatement(insertSql)) {
                for (WorkLogEntry record : records) {
                    pstmtInsert.setInt(1, record.getEmployeeId());
                    pstmtInsert.setDate(2, Date.valueOf(record.getWorkDate()));
                    pstmtInsert.setTime(3, record.getStartTime() == null ? null : Time.valueOf(record.getStartTime()));
                    pstmtInsert.setTime(4, record.getEndTime() == null ? null : Time.valueOf(record.getEndTime()));
                    pstmtInsert.setString(5, record.getStatus().name());
                    pstmtInsert.addBatch();
                }
                pstmtInsert.executeBatch();
            }
            conn.commit();
        } catch (SQLException e) {
            e.printStackTrace();
            // Rollback logic
        }
    }

    public List<WorkLogEntry> getWorkRecords(int employeeId, YearMonth yearMonth) {
        List<WorkLogEntry> records = new ArrayList<>();
        String sql = "SELECT * FROM work_records WHERE employee_id = ? AND YEAR(work_date) = ? AND MONTH(work_date) = ? ORDER BY work_date ASC";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, employeeId);
            pstmt.setInt(2, yearMonth.getYear());
            pstmt.setInt(3, yearMonth.getMonthValue());
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                records.add(new WorkLogEntry(rs.getInt("id"), rs.getInt("employee_id"), rs.getDate("work_date").toLocalDate(),
                        rs.getTime("start_time") == null ? null : rs.getTime("start_time").toLocalTime(),
                        rs.getTime("end_time") == null ? null : rs.getTime("end_time").toLocalTime(),
                        AttendancePage.WorkStatus.valueOf(rs.getString("work_status"))));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return records;
    }

    public void calculateAndGrantAnnualLeave(Employee employee, int forYear) {
        if (employee.getHireDate() == null || employee.getHireDate().isEmpty()) return;
        Map<String, String> settings = loadSettings();
        String basis = settings.getOrDefault("annualLeaveBasis", "FISCAL");
        LocalDate hireDate;
        try { hireDate = LocalDate.parse(employee.getHireDate()); } catch (Exception e) { return; }
        double generatedDays = 0;
        LocalDate today = LocalDate.now();
        if ("HIRE_DATE".equals(basis)) {
            long yearsOfService = ChronoUnit.YEARS.between(hireDate, today);
            if (yearsOfService < 1) {
                generatedDays = Math.min(ChronoUnit.MONTHS.between(hireDate, today), 11);
            } else {
                generatedDays = 15;
                if (yearsOfService >= 3) generatedDays += (yearsOfService - 1) / 2;
            }
        } else {
            long yearsOfService = ChronoUnit.YEARS.between(hireDate, LocalDate.of(forYear, 1, 1));
            if (hireDate.getYear() >= forYear) generatedDays = 0;
            else if (hireDate.getYear() == forYear - 1) generatedDays = ChronoUnit.MONTHS.between(hireDate.withDayOfMonth(1), LocalDate.of(forYear, 1, 1));
            else {
                generatedDays = 15;
                if (yearsOfService >= 3) generatedDays += (yearsOfService - 1) / 2;
            }
        }
        generatedDays = Math.min(generatedDays, 25);
        String sql = "MERGE INTO annual_leaves (employee_id, leave_year, total_generated_days) KEY(employee_id, leave_year) VALUES (?, ?, ?)";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, employee.getId());
            pstmt.setInt(2, forYear);
            pstmt.setDouble(3, generatedDays);
            pstmt.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public Map<String, Double> getAnnualLeaveSummary(int employeeId, int year) {
        Map<String, Double> summary = new HashMap<>();
        summary.put("generated", 0.0); summary.put("adjustment", 0.0); summary.put("used", 0.0);
        try (Connection conn = getConnection()) {
            try (PreparedStatement pstmt = conn.prepareStatement("SELECT total_generated_days, adjustment_days FROM annual_leaves WHERE employee_id = ? AND leave_year = ?")) {
                pstmt.setInt(1, employeeId);
                pstmt.setInt(2, year);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    summary.put("generated", rs.getDouble("total_generated_days"));
                    summary.put("adjustment", rs.getDouble("adjustment_days"));
                }
            }
            try (PreparedStatement pstmt = conn.prepareStatement("SELECT SUM(deduct_days) as used_days FROM leave_usage_records WHERE employee_id = ? AND YEAR(leave_date) = ?")) {
                pstmt.setInt(1, employeeId);
                pstmt.setInt(2, year);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) summary.put("used", rs.getDouble("used_days"));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return summary;
    }

    public boolean saveLeaveUsage(int employeeId, List<LocalDate> dates) {
        String sql = "MERGE INTO leave_usage_records (employee_id, leave_date, leave_type, deduct_days) KEY(employee_id, leave_date) VALUES (?, ?, 'ANNUAL', 1.0)";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            for (LocalDate date : dates) {
                pstmt.setInt(1, employeeId);
                pstmt.setDate(2, java.sql.Date.valueOf(date));
                pstmt.addBatch();
            }
            pstmt.executeBatch();
            return true;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    public List<LocalDate> getLeaveUsageRecords(int employeeId, int year) {
        List<LocalDate> usedDates = new ArrayList<>();
        String sql = "SELECT leave_date FROM leave_usage_records WHERE employee_id = ? AND YEAR(leave_date) = ? ORDER BY leave_date ASC";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, employeeId);
            pstmt.setInt(2, year);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) usedDates.add(rs.getDate("leave_date").toLocalDate());
        } catch (SQLException e) { e.printStackTrace(); }
        return usedDates;
    }

    public boolean applyLeaveAdjustment(int employeeId, int year, double adjustmentDays, String notes) {
        String sql = "UPDATE annual_leaves SET adjustment_days = adjustment_days + ?, notes = CONCAT(COALESCE(notes, ''), ?) WHERE employee_id = ? AND leave_year = ?";
        String insertSql = "INSERT INTO annual_leaves (employee_id, leave_year, total_generated_days, adjustment_days, notes) VALUES (?, ?, 0.0, ?, ?)";
        String checkSql = "SELECT id FROM annual_leaves WHERE employee_id = ? AND leave_year = ?";
        try (Connection conn = getConnection()) {
            try (PreparedStatement pstmtCheck = conn.prepareStatement(checkSql)) {
                pstmtCheck.setInt(1, employeeId);
                pstmtCheck.setInt(2, year);
                if (!pstmtCheck.executeQuery().next()) {
                    try (PreparedStatement pstmtInsert = conn.prepareStatement(insertSql)) {
                        pstmtInsert.setInt(1, employeeId);
                        pstmtInsert.setInt(2, year);
                        pstmtInsert.setDouble(3, adjustmentDays);
                        pstmtInsert.setString(4, notes + "\n");
                        pstmtInsert.executeUpdate();
                    }
                } else {
                    try (PreparedStatement pstmtUpdate = conn.prepareStatement(sql)) {
                        pstmtUpdate.setDouble(1, adjustmentDays);
                        pstmtUpdate.setString(2, notes + "\n");
                        pstmtUpdate.setInt(3, employeeId);
                        pstmtUpdate.setInt(4, year);
                        pstmtUpdate.executeUpdate();
                    }
                }
            }
            return true;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    public boolean setTotalLeaveManually(int employeeId, int year, double totalDays, String notes) {
        String sql = "MERGE INTO annual_leaves (employee_id, leave_year, total_generated_days, adjustment_days, notes) KEY(employee_id, leave_year) VALUES (?, ?, 0.0, ?, ?)";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, employeeId);
            pstmt.setInt(2, year);
            pstmt.setDouble(3, totalDays);
            pstmt.setString(4, notes);
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }
}