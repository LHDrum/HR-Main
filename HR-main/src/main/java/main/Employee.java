package main;

public class Employee {
    private int id;
    private String name;
    private String residentRegistrationNumber;
    private String phoneNumber;
    private int annualSalary;
    private String address;
    private String hireDate;
    private String salaryChangeDate;
    private String department;
    private String workLocation; // 근무지 필드 추가
    private String siteLocation; // 소재지 필드 추가

    // 새 직원 생성용 (ID는 DB에서 자동 생성)
    public Employee(String name, String residentRegistrationNumber, String phoneNumber,
                    int annualSalary, String address, String hireDate, String salaryChangeDate, String department,
                    String workLocation, String siteLocation) { // 근무지, 소재지 추가
        this.name = name;
        this.residentRegistrationNumber = residentRegistrationNumber;
        this.phoneNumber = phoneNumber;
        this.annualSalary = annualSalary;
        this.address = address;
        this.hireDate = hireDate;
        this.salaryChangeDate = salaryChangeDate;
        this.department = department;
        this.workLocation = workLocation; // 초기화
        this.siteLocation = siteLocation; // 초기화
    }

    // DB에서 불러올 때 사용 (ID 포함)
    public Employee(int id, String name, String residentRegistrationNumber, String phoneNumber,
                    int annualSalary, String address, String hireDate, String salaryChangeDate, String department,
                    String workLocation, String siteLocation) { // 근무지, 소재지 추가
        this.id = id;
        this.name = name;
        this.residentRegistrationNumber = residentRegistrationNumber;
        this.phoneNumber = phoneNumber;
        this.annualSalary = annualSalary;
        this.address = address;
        this.hireDate = hireDate;
        this.salaryChangeDate = salaryChangeDate;
        this.department = department;
        this.workLocation = workLocation; // 초기화
        this.siteLocation = siteLocation; // 초기화
    }

    // Getters
    public int getId() { return id; }
    public String getName() { return name; }
    public String getResidentRegistrationNumber() { return residentRegistrationNumber; }
    public String getPhoneNumber() { return phoneNumber; }
    public int getAnnualSalary() { return annualSalary; }
    public String getAddress() { return address; }
    public String getHireDate() { return hireDate; }
    public String getSalaryChangeDate() { return salaryChangeDate; }
    public String getDepartment() { return department; }
    public String getWorkLocation() { return workLocation; } // 근무지 Getter
    public String getSiteLocation() { return siteLocation; } // 소재지 Getter

    // Setters
    public void setId(int id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setResidentRegistrationNumber(String residentRegistrationNumber) { this.residentRegistrationNumber = residentRegistrationNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    public void setAnnualSalary(int annualSalary) { this.annualSalary = annualSalary; }
    public void setAddress(String address) { this.address = address; }
    public void setHireDate(String hireDate) { this.hireDate = hireDate; }
    public void setSalaryChangeDate(String salaryChangeDate) { this.salaryChangeDate = salaryChangeDate; }
    public void setDepartment(String department) { this.department = department; }
    public void setWorkLocation(String workLocation) { this.workLocation = workLocation; } // 근무지 Setter
    public void setSiteLocation(String siteLocation) { this.siteLocation = siteLocation; } // 소재지 Setter
}