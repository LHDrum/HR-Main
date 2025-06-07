package main;

import java.math.BigDecimal;

public class CalculationResult {

    private BigDecimal finalAdjustedBasicPay;
    private BigDecimal finalAdjustedFixedOvertimeAllowance;
    private BigDecimal finalAdjustedBonus;
    private BigDecimal finalAdjustedOtherAllowance;
    private BigDecimal finalAdjustedMealAllowance;
    private BigDecimal finalAdjustedVehicleMaintenanceFee;
    private BigDecimal finalAdjustedResearchDevelopmentExpense;
    private BigDecimal finalAdjustedChildcareAllowance;
    private BigDecimal finalAdjustedAdditionalOvertimePremium;

    // double에서 BigDecimal로 변경
    private BigDecimal hireProrationRatio;
    private BigDecimal attendanceBasedPaymentRatio;

    private BigDecimal totalShortfallMonetaryDeduction;
    private BigDecimal weeklyAbsencePenalty;
    private BigDecimal overtimePremium;
    private BigDecimal nightPremium;
    private BigDecimal holidayPremium;
    private long paidPremiumMinutes;
    private long totalShortfallMinutes;

    // Getters and Setters
    public BigDecimal getFinalAdjustedBasicPay() { return finalAdjustedBasicPay; }
    public void setFinalAdjustedBasicPay(BigDecimal finalAdjustedBasicPay) { this.finalAdjustedBasicPay = finalAdjustedBasicPay; }
    public BigDecimal getFinalAdjustedFixedOvertimeAllowance() { return finalAdjustedFixedOvertimeAllowance; }
    public void setFinalAdjustedFixedOvertimeAllowance(BigDecimal finalAdjustedFixedOvertimeAllowance) { this.finalAdjustedFixedOvertimeAllowance = finalAdjustedFixedOvertimeAllowance; }
    public BigDecimal getFinalAdjustedBonus() { return finalAdjustedBonus; }
    public void setFinalAdjustedBonus(BigDecimal finalAdjustedBonus) { this.finalAdjustedBonus = finalAdjustedBonus; }
    public BigDecimal getFinalAdjustedOtherAllowance() { return finalAdjustedOtherAllowance; }
    public void setFinalAdjustedOtherAllowance(BigDecimal finalAdjustedOtherAllowance) { this.finalAdjustedOtherAllowance = finalAdjustedOtherAllowance; }
    public BigDecimal getFinalAdjustedMealAllowance() { return finalAdjustedMealAllowance; }
    public void setFinalAdjustedMealAllowance(BigDecimal finalAdjustedMealAllowance) { this.finalAdjustedMealAllowance = finalAdjustedMealAllowance; }
    public BigDecimal getFinalAdjustedVehicleMaintenanceFee() { return finalAdjustedVehicleMaintenanceFee; }
    public void setFinalAdjustedVehicleMaintenanceFee(BigDecimal finalAdjustedVehicleMaintenanceFee) { this.finalAdjustedVehicleMaintenanceFee = finalAdjustedVehicleMaintenanceFee; }
    public BigDecimal getFinalAdjustedResearchDevelopmentExpense() { return finalAdjustedResearchDevelopmentExpense; }
    public void setFinalAdjustedResearchDevelopmentExpense(BigDecimal finalAdjustedResearchDevelopmentExpense) { this.finalAdjustedResearchDevelopmentExpense = finalAdjustedResearchDevelopmentExpense; }
    public BigDecimal getFinalAdjustedChildcareAllowance() { return finalAdjustedChildcareAllowance; }
    public void setFinalAdjustedChildcareAllowance(BigDecimal finalAdjustedChildcareAllowance) { this.finalAdjustedChildcareAllowance = finalAdjustedChildcareAllowance; }
    public BigDecimal getFinalAdjustedAdditionalOvertimePremium() { return finalAdjustedAdditionalOvertimePremium; }
    public void setFinalAdjustedAdditionalOvertimePremium(BigDecimal finalAdjustedAdditionalOvertimePremium) { this.finalAdjustedAdditionalOvertimePremium = finalAdjustedAdditionalOvertimePremium; }

    // Getter/Setter 타입을 BigDecimal으로 변경
    public BigDecimal getHireProrationRatio() { return hireProrationRatio; }
    public void setHireProrationRatio(BigDecimal hireProrationRatio) { this.hireProrationRatio = hireProrationRatio; }
    public BigDecimal getAttendanceBasedPaymentRatio() { return attendanceBasedPaymentRatio; }
    public void setAttendanceBasedPaymentRatio(BigDecimal attendanceBasedPaymentRatio) { this.attendanceBasedPaymentRatio = attendanceBasedPaymentRatio; }

    public BigDecimal getTotalShortfallMonetaryDeduction() { return totalShortfallMonetaryDeduction; }
    public void setTotalShortfallMonetaryDeduction(BigDecimal totalShortfallMonetaryDeduction) { this.totalShortfallMonetaryDeduction = totalShortfallMonetaryDeduction; }
    public BigDecimal getWeeklyAbsencePenalty() { return weeklyAbsencePenalty; }
    public void setWeeklyAbsencePenalty(BigDecimal weeklyAbsencePenalty) { this.weeklyAbsencePenalty = weeklyAbsencePenalty; }
    public BigDecimal getOvertimePremium() { return overtimePremium; }
    public void setOvertimePremium(BigDecimal overtimePremium) { this.overtimePremium = overtimePremium; }
    public BigDecimal getNightPremium() { return nightPremium; }
    public void setNightPremium(BigDecimal nightPremium) { this.nightPremium = nightPremium; }
    public BigDecimal getHolidayPremium() { return holidayPremium; }
    public void setHolidayPremium(BigDecimal holidayPremium) { this.holidayPremium = holidayPremium; }
    public long getPaidPremiumMinutes() { return paidPremiumMinutes; }
    public void setPaidPremiumMinutes(long paidPremiumMinutes) { this.paidPremiumMinutes = paidPremiumMinutes; }
    public long getTotalShortfallMinutes() { return totalShortfallMinutes; }
    public void setTotalShortfallMinutes(long totalShortfallMinutes) { this.totalShortfallMinutes = totalShortfallMinutes; }
}