package main;

public class Payroll {
    private int id;
    private int employeeId;
    private Employee employee; // 조인된 데이터용

    private Integer paymentYear;
    private Integer paymentMonth;

    // 급여 항목
    private int monthlyBasicSalary;
    private int bonus;
    private int fixedOvertimeAllowance;
    private int additionalOvertimePremium;
    private int otherAllowance;
    private int mealAllowance;
    private int vehicleMaintenanceFee;
    private int researchDevelopmentExpense;
    private int childcareAllowance;

    // 근태 관련
    private int unpaidDays;
    private int unauthorizedAbsenceDays;

    // --- 신규 추가: 공제 항목 ---
    // 근로자 부담
    private double nationalPensionEmployee;          // 국민연금
    private double healthInsuranceEmployee;          // 건강보험
    private double longTermCareInsuranceEmployee;    // 장기요양보험
    private double employmentInsuranceEmployee;      // 고용보험
    private double incomeTax;                        // 소득세
    private double localIncomeTax;                   // 지방소득세
    private double totalEmployeeDeduction;           // 근로자 공제 총액

    // 사업주 부담
    private double nationalPensionEmployer;          // 국민연금
    private double healthInsuranceEmployer;          // 건강보험
    private double longTermCareInsuranceEmployer;    // 장기요양보험
    private double employmentInsuranceEmployer;      // 고용보험
    private double industrialAccidentInsuranceEmployer; // 산재보험

    // 최종 지급액
    private double netPay;                           // 실지급액

    // 빈 생성자
    public Payroll() {}

    // 생성자 1: 새 계약 급여 정보 생성용 (EmployeeManagementPage에서 사용)
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
    }

    // 생성자 2: DB에서 모든 데이터를 포함하여 불러올 때 사용 (확장됨)
    public Payroll(int id, int employeeId, Integer paymentYear, Integer paymentMonth, int monthlyBasicSalary, int bonus, int fixedOvertimeAllowance, int additionalOvertimePremium, int otherAllowance, int mealAllowance, int vehicleMaintenanceFee, int researchDevelopmentExpense, int childcareAllowance, int unpaidDays, int unauthorizedAbsenceDays, double nationalPensionEmployee, double healthInsuranceEmployee, double longTermCareInsuranceEmployee, double employmentInsuranceEmployee, double incomeTax, double localIncomeTax, double totalEmployeeDeduction, double nationalPensionEmployer, double healthInsuranceEmployer, double longTermCareInsuranceEmployer, double employmentInsuranceEmployer, double industrialAccidentInsuranceEmployer, double netPay) {
        this.id = id;
        this.employeeId = employeeId;
        this.paymentYear = paymentYear;
        this.paymentMonth = paymentMonth;
        this.monthlyBasicSalary = monthlyBasicSalary;
        this.bonus = bonus;
        this.fixedOvertimeAllowance = fixedOvertimeAllowance;
        this.additionalOvertimePremium = additionalOvertimePremium;
        this.otherAllowance = otherAllowance;
        this.mealAllowance = mealAllowance;
        this.vehicleMaintenanceFee = vehicleMaintenanceFee;
        this.researchDevelopmentExpense = researchDevelopmentExpense;
        this.childcareAllowance = childcareAllowance;
        this.unpaidDays = unpaidDays;
        this.unauthorizedAbsenceDays = unauthorizedAbsenceDays;
        this.nationalPensionEmployee = nationalPensionEmployee;
        this.healthInsuranceEmployee = healthInsuranceEmployee;
        this.longTermCareInsuranceEmployee = longTermCareInsuranceEmployee;
        this.employmentInsuranceEmployee = employmentInsuranceEmployee;
        this.incomeTax = incomeTax;
        this.localIncomeTax = localIncomeTax;
        this.totalEmployeeDeduction = totalEmployeeDeduction;
        this.nationalPensionEmployer = nationalPensionEmployer;
        this.healthInsuranceEmployer = healthInsuranceEmployer;
        this.longTermCareInsuranceEmployer = longTermCareInsuranceEmployer;
        this.employmentInsuranceEmployer = employmentInsuranceEmployer;
        this.industrialAccidentInsuranceEmployer = industrialAccidentInsuranceEmployer;
        this.netPay = netPay;
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
    public double getNationalPensionEmployee() { return nationalPensionEmployee; }
    public double getHealthInsuranceEmployee() { return healthInsuranceEmployee; }
    public double getLongTermCareInsuranceEmployee() { return longTermCareInsuranceEmployee; }
    public double getEmploymentInsuranceEmployee() { return employmentInsuranceEmployee; }
    public double getIncomeTax() { return incomeTax; }
    public double getLocalIncomeTax() { return localIncomeTax; }
    public double getTotalEmployeeDeduction() { return totalEmployeeDeduction; }
    public double getNationalPensionEmployer() { return nationalPensionEmployer; }
    public double getHealthInsuranceEmployer() { return healthInsuranceEmployer; }
    public double getLongTermCareInsuranceEmployer() { return longTermCareInsuranceEmployer; }
    public double getEmploymentInsuranceEmployer() { return employmentInsuranceEmployer; }
    public double getIndustrialAccidentInsuranceEmployer() { return industrialAccidentInsuranceEmployer; }
    public double getNetPay() { return netPay; }


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
    public void setNationalPensionEmployee(double nationalPensionEmployee) { this.nationalPensionEmployee = nationalPensionEmployee; }
    public void setHealthInsuranceEmployee(double healthInsuranceEmployee) { this.healthInsuranceEmployee = healthInsuranceEmployee; }
    public void setLongTermCareInsuranceEmployee(double longTermCareInsuranceEmployee) { this.longTermCareInsuranceEmployee = longTermCareInsuranceEmployee; }
    public void setEmploymentInsuranceEmployee(double employmentInsuranceEmployee) { this.employmentInsuranceEmployee = employmentInsuranceEmployee; }
    public void setIncomeTax(double incomeTax) { this.incomeTax = incomeTax; }
    public void setLocalIncomeTax(double localIncomeTax) { this.localIncomeTax = localIncomeTax; }
    public void setTotalEmployeeDeduction(double totalEmployeeDeduction) { this.totalEmployeeDeduction = totalEmployeeDeduction; }
    public void setNationalPensionEmployer(double nationalPensionEmployer) { this.nationalPensionEmployer = nationalPensionEmployer; }
    public void setHealthInsuranceEmployer(double healthInsuranceEmployer) { this.healthInsuranceEmployer = healthInsuranceEmployer; }
    public void setLongTermCareInsuranceEmployer(double longTermCareInsuranceEmployer) { this.longTermCareInsuranceEmployer = longTermCareInsuranceEmployer; }
    public void setEmploymentInsuranceEmployer(double employmentInsuranceEmployer) { this.employmentInsuranceEmployer = employmentInsuranceEmployer; }
    public void setIndustrialAccidentInsuranceEmployer(double industrialAccidentInsuranceEmployer) { this.industrialAccidentInsuranceEmployer = industrialAccidentInsuranceEmployer; }
    public void setNetPay(double netPay) { this.netPay = netPay; }


    // 세전 총 급여액을 반환하는 헬퍼 메서드
    public int getGrossPay() {
        return monthlyBasicSalary + bonus +
                fixedOvertimeAllowance + otherAllowance + mealAllowance +
                vehicleMaintenanceFee + researchDevelopmentExpense + childcareAllowance +
                additionalOvertimePremium;
    }
}