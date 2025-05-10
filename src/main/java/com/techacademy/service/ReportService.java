package com.techacademy.service;

import com.techacademy.entity.Employee;
import com.techacademy.entity.Employee.Role;
import com.techacademy.entity.Report;
import com.techacademy.repository.EmployeeRepository;
import com.techacademy.repository.ReportRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class ReportService {

    private final ReportRepository reportRepository;
    private final EmployeeRepository employeeRepository;


    @Autowired
    public ReportService(ReportRepository reportRepository, EmployeeRepository employeeRepository) {
        this.reportRepository = reportRepository;
        this.employeeRepository = employeeRepository;
    }

 // **従業員情報をもとに日報を取得**
    public List<Report> findByEmployee(Employee employee) {
        return reportRepository.findByEmployee(employee);
    }


    public Employee findEmployeeByCode(String employeeCode) {
        return employeeRepository.findByCode(employeeCode); // 修正: EmployeeRepository を利用
    }


    public List<Report> findByEmployeeCode(String employeeCode) {
        return reportRepository.findByEmployeeCode(employeeCode);
    }


    public void save(Report report) {
        reportRepository.save(report); // JPA の save メソッドを呼び出す
    }


    // 日報一覧取得
    public List<Report> findAll() {
        return reportRepository.findAll();
    }


    // 日報詳細取得
    public Optional<Report> findById(Long id) {
        return reportRepository.findById(id);
    }

    // 日報新規登録
    public Report createReport(Report report) {
        return reportRepository.save(report);
    }

    public boolean existsByEmployeeAndDateWithoutExclusion(String employeeCode, LocalDate reportDate) {
        return reportRepository.existsByEmployee_CodeAndReportDate(employeeCode, reportDate);
    }


    // 既存データ確認
    public boolean existsByEmployeeAndDateExcludeCurrent(String employeeCode, LocalDate reportDate, Long currentId) {
        return reportRepository.existsByEmployee_CodeAndReportDateExcludeCurrent(employeeCode, reportDate, currentId);
    }

    // **日報更新**
    public Report updateReport(Long id, Report updatedReport) {
        return reportRepository.findById(id)
                .map(report -> {
                    if (existsByEmployeeAndDateExcludeCurrent(updatedReport.getEmployee().getCode(), updatedReport.getReportDate(), id)) {
                        throw new RuntimeException("既に登録されている日付です");
                    }
                    report.setTitle(updatedReport.getTitle());
                    report.setContent(updatedReport.getContent());
                    report.setReportDate(updatedReport.getReportDate());
                    return reportRepository.save(report);
                })
                .orElseThrow(() -> new RuntimeException("Report not found"));
    }


    // 日報論理削除
    public void delete(Long id) {
        reportRepository.findById(id).ifPresent(report -> {
            report.setDeleteFlg(true); // **論理削除を適用**
            report.setUpdatedAt(LocalDateTime.now());
            reportRepository.save(report);
        });
    }

    // ** ユーザー権限に応じた日報取得メソッド**
    public List<Report> findReportsByUserRole(String employeeCode) {
        Employee employee = findEmployeeByCode(employeeCode);

        // ** Enum の比較を `==` で行う**
        if (employee.getRole() == Role.ADMIN) {
            return reportRepository.findAll(); // **管理者は全データ取得**
        }

        return reportRepository.findByEmployeeCode(employeeCode); // **一般ユーザーは自身のデータのみ**
    }


}