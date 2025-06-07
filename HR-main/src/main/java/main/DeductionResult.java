package main;

import java.math.BigDecimal;

public class DeductionResult {
    BigDecimal nationalPensionEmployee;
    BigDecimal healthInsuranceEmployee;
    BigDecimal longTermCareInsuranceEmployee;
    BigDecimal employmentInsuranceEmployee;
    BigDecimal incomeTax;
    BigDecimal localIncomeTax;
    BigDecimal totalEmployeeDeduction;

    BigDecimal nationalPensionEmployer;
    BigDecimal healthInsuranceEmployer;
    BigDecimal longTermCareInsuranceEmployer;
    BigDecimal employmentInsuranceEmployer;
    BigDecimal industrialAccidentInsuranceEmployer;
    BigDecimal totalEmployerDeduction;

    BigDecimal netPay;
}