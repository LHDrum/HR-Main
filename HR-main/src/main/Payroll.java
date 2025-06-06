package main;

public class Payroll {
    private int id;
    private int employeeId;
    private Employee employee; // 조인된 데이터용

    // <<< 기능 추가: 이 급여 데이터가 귀속되는 연도와 월 >>>
    private Integer paymentYear;
    private Integer paymentMonth;

    private int monthlyBasicSalary;
    private int bonus; // 상여금
    private int fixedOvertimeAllowance; // 고정연장수당 (계약분의 실 지급액)
    private int additionalOvertimePremium; // 추가 연장 수당 (실제 발생분)
    private int otherAllowance;         // 기타수당
    private int mealAllowance;
    private int vehicleMaintenanceFee;  // 차량유지비
    private int researchDevelopmentExpense;
    private int childcareAllowance;     // 육아수당
    private int unpaidDays;
    private int unauthorizedAbsenceDays; // 무단결근일수
    private double weeklyHolidayAllowance;


    // 생성자 1: 새 계약 급여 정보 생성용 (EmployeeManagementPage에서 사용)
    // paymentYear와 paymentMonth는 null로 설정됩니다 (특정 월 귀속 아님).
    public Payroll(Employee employee, int monthlyBasicSalary, int bonus,
                   int fixedOvertimeAllowance, int otherAllowance, int mealAllowance,
                   int vehicleMaintenanceFee, int researchDevelopmentExpense, int childcareAllowance) {
        this.employee = employee;
        if (employee != null) {
            this.employeeId = employee.getId();
        }
        this.monthlyBasicSalary = monthlyBasicSalary;
        this.bonus = bonus;
        this.fixedOvertimeAllowance = fixedOvertimeAllowance;
        this.otherAllowance = otherAllowance;
        this.mealAllowance = mealAllowance;
        this.vehicleMaintenanceFee = vehicleMaintenanceFee;
        this.researchDevelopmentExpense = researchDevelopmentExpense;
        this.childcareAllowance = childcareAllowance;
        this.additionalOvertimePremium = 0; // 계약 시 추가 연장 수당은 0
        this.paymentYear = null;            // 계약 정보는 특정 연/월에 귀속되지 않음
        this.paymentMonth = null;           // 계약 정보는 특정 연/월에 귀속되지 않음
        this.unpaidDays = 0;
        this.unauthorizedAbsenceDays = 0;
        this.weeklyHolidayAllowance = 0.0;
    }

    // 생성자 2: DB에서 월별 확정 급여 또는 계약 급여 정보를 불러올 때 사용 (PayrollManager에서 사용)
    // 이 생성자는 총 16개의 인자를 받습니다.
    public Payroll(int id, int employeeId, Integer paymentYear, Integer paymentMonth,
                   int monthlyBasicSalary, int bonus,
                   int fixedOvertimeAllowance, int otherAllowance, int mealAllowance,
                   int vehicleMaintenanceFee, int researchDevelopmentExpense, int childcareAllowance,
                   int additionalOvertimePremium,
                   int unpaidDays, int unauthorizedAbsenceDays, double weeklyHolidayAllowance) {
        this.id = id;
        this.employeeId = employeeId;
        this.paymentYear = paymentYear;
        this.paymentMonth = paymentMonth;
        this.monthlyBasicSalary = monthlyBasicSalary;
        this.bonus = bonus;
        this.fixedOvertimeAllowance = fixedOvertimeAllowance;
        this.otherAllowance = otherAllowance;
        this.mealAllowance = mealAllowance;
        this.vehicleMaintenanceFee = vehicleMaintenanceFee;
        this.researchDevelopmentExpense = researchDevelopmentExpense;
        this.childcareAllowance = childcareAllowance;
        this.additionalOvertimePremium = additionalOvertimePremium;
        this.unpaidDays = unpaidDays;
        this.unauthorizedAbsenceDays = unauthorizedAbsenceDays;
        this.weeklyHolidayAllowance = weeklyHolidayAllowance;
    }

    // Getters
    public int getId() { return id; }
    public int getEmployeeId() { return employeeId; }
    public Employee getEmployee() { return employee; }
    public Integer getPaymentYear() { return paymentYear; }
    public Integer getPaymentMonth() { return paymentMonth; }
    public int getMonthlyBasicSalary() { return monthlyBasicSalary; }
    public int getBonus() { return bonus; }
    public int getFixedOvertimeAllowance() { return fixedOvertimeAllowance; }
    public int getAdditionalOvertimePremium() { return additionalOvertimePremium; }
    public int getOtherAllowance() { return otherAllowance; }
    public int getMealAllowance() { return mealAllowance; }
    public int getVehicleMaintenanceFee() { return vehicleMaintenanceFee; }
    public int getResearchDevelopmentExpense() { return researchDevelopmentExpense; }
    public int getChildcareAllowance() { return childcareAllowance; }
    public int getUnpaidDays() { return unpaidDays; }
    public int getUnauthorizedAbsenceDays() { return unauthorizedAbsenceDays; }
    public double getWeeklyHolidayAllowance() { return weeklyHolidayAllowance; }

    // Setters
    public void setId(int id) { this.id = id; }
    public void setEmployeeId(int employeeId) { this.employeeId = employeeId; }
    public void setEmployee(Employee employee) { this.employee = employee; }
    public void setPaymentYear(Integer paymentYear) { this.paymentYear = paymentYear; }
    public void setPaymentMonth(Integer paymentMonth) { this.paymentMonth = paymentMonth; }
    public void setMonthlyBasicSalary(int monthlyBasicSalary) { this.monthlyBasicSalary = monthlyBasicSalary; }
    public void setBonus(int bonus) { this.bonus = bonus; }
    public void setFixedOvertimeAllowance(int fixedOvertimeAllowance) { this.fixedOvertimeAllowance = fixedOvertimeAllowance; }
    public void setAdditionalOvertimePremium(int additionalOvertimePremium) { this.additionalOvertimePremium = additionalOvertimePremium; }
    public void setOtherAllowance(int otherAllowance) { this.otherAllowance = otherAllowance; }
    public void setMealAllowance(int mealAllowance) { this.mealAllowance = mealAllowance; }
    public void setVehicleMaintenanceFee(int vehicleMaintenanceFee) { this.vehicleMaintenanceFee = vehicleMaintenanceFee; }
    public void setResearchDevelopmentExpense(int researchDevelopmentExpense) { this.researchDevelopmentExpense = researchDevelopmentExpense; }
    public void setChildcareAllowance(int childcareAllowance) { this.childcareAllowance = childcareAllowance; }
    public void setUnpaidDays(int unpaidDays) { this.unpaidDays = unpaidDays; }
    public void setUnauthorizedAbsenceDays(int unauthorizedAbsenceDays) { this.unauthorizedAbsenceDays = unauthorizedAbsenceDays; }
    public void setWeeklyHolidayAllowance(double weeklyHolidayAllowance) { this.weeklyHolidayAllowance = weeklyHolidayAllowance; }

    // 이해동 바보
    public int getTotalSalaryForDisplay() {
        return monthlyBasicSalary + bonus +
                fixedOvertimeAllowance + otherAllowance + mealAllowance +
                vehicleMaintenanceFee + researchDevelopmentExpense + childcareAllowance +
                additionalOvertimePremium;
    }
}