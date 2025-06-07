package main;

import java.math.BigDecimal;
import java.time.LocalDate;

public class Employee {
    private int id;
    private String name;
    private String residentRegistrationNumber;
    private String phoneNumber;
    private BigDecimal annualSalary;
    private String address;
    private LocalDate hireDate;
    private LocalDate salaryChangeDate;
    private String department;
    private String workLocation;
    private String siteLocation;

    public Employee(String name, String residentRegistrationNumber, String phoneNumber,
                    BigDecimal annualSalary, String address, LocalDate hireDate, LocalDate salaryChangeDate, String department,
                    String workLocation, String siteLocation) {
        this.name = name;
        this.residentRegistrationNumber = residentRegistrationNumber;
        this.phoneNumber = phoneNumber;
        this.annualSalary = annualSalary;
        this.address = address;
        this.hireDate = hireDate;
        this.salaryChangeDate = salaryChangeDate;
        this.department = department;
        this.workLocation = workLocation;
        this.siteLocation = siteLocation;
    }

    public Employee(int id, String name, String residentRegistrationNumber, String phoneNumber,
                    BigDecimal annualSalary, String address, LocalDate hireDate, LocalDate salaryChangeDate, String department,
                    String workLocation, String siteLocation) {
        this.id = id;
        this.name = name;
        this.residentRegistrationNumber = residentRegistrationNumber;
        this.phoneNumber = phoneNumber;
        this.annualSalary = annualSalary;
        this.address = address;
        this.hireDate = hireDate;
        this.salaryChangeDate = salaryChangeDate;
        this.department = department;
        this.workLocation = workLocation;
        this.siteLocation = siteLocation;
    }

    // Getters
    public int getId() { return id; }
    public String getName() { return name; }
    public String getResidentRegistrationNumber() { return residentRegistrationNumber; }
    public String getPhoneNumber() { return phoneNumber; }
    public BigDecimal getAnnualSalary() { return annualSalary; }
    public String getAddress() { return address; }
    public LocalDate getHireDate() { return hireDate; }
    public LocalDate getSalaryChangeDate() { return salaryChangeDate; }
    public String getDepartment() { return department; }
    public String getWorkLocation() { return workLocation; }
    public String getSiteLocation() { return siteLocation; }

    // Setters
    public void setId(int id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setResidentRegistrationNumber(String residentRegistrationNumber) { this.residentRegistrationNumber = residentRegistrationNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    public void setAnnualSalary(BigDecimal annualSalary) { this.annualSalary = annualSalary; }
    public void setAddress(String address) { this.address = address; }
    public void setHireDate(LocalDate hireDate) { this.hireDate = hireDate; }
    public void setSalaryChangeDate(LocalDate salaryChangeDate) { this.salaryChangeDate = salaryChangeDate; }
    public void setDepartment(String department) { this.department = department; }
    public void setWorkLocation(String workLocation) { this.workLocation = workLocation; }
    public void setSiteLocation(String siteLocation) { this.siteLocation = siteLocation; }
}