package com.techacademy.repository;

import com.techacademy.entity.Employee;
import com.techacademy.entity.Report;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {

    List<Report> findByEmployee(Employee employee);

    // 修正: 従業員コードで検索
    List<Report> findByEmployeeCode(String employeeCode);

    // 修正: ログイン中の従業員のコードと日付を条件に、登録済みの日報があるか確認
    boolean existsByEmployee_CodeAndReportDate(String employeeCode, LocalDate reportDate);

    @Query("SELECT COUNT(r) > 0 FROM Report r WHERE r.employee.code = :employeeCode AND r.reportDate = :reportDate AND r.id <> :currentId")
    boolean existsByEmployee_CodeAndReportDateExcludeCurrent(@Param("employeeCode") String employeeCode, @Param("reportDate") LocalDate reportDate, @Param("currentId") Long currentId);

}
