package main;

// 계산된 모든 공제 항목을 담는 데이터 클래스
public class DeductionResult {
    // 근로자 부담분
    double nationalPensionEmployee;
    double healthInsuranceEmployee;
    double longTermCareInsuranceEmployee;
    double employmentInsuranceEmployee;
    double incomeTax;
    double localIncomeTax;
    double totalEmployeeDeduction;

    // 사업주 부담분
    double nationalPensionEmployer;
    double healthInsuranceEmployer;
    double longTermCareInsuranceEmployer;
    double employmentInsuranceEmployer;
    double industrialAccidentInsuranceEmployer;
    double totalEmployerDeduction;

    // 실지급액
    double netPay;
}