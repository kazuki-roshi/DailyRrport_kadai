package com.techacademy.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.techacademy.constants.ErrorKinds;
import com.techacademy.entity.Employee;
import com.techacademy.entity.Report;
import com.techacademy.repository.EmployeeRepository;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;
    private final ReportService reportService;



    @Autowired
    public EmployeeService(EmployeeRepository employeeRepository, PasswordEncoder passwordEncoder, ReportService reportService) {
        this.employeeRepository = employeeRepository;
        this.passwordEncoder = passwordEncoder;
        this.reportService = reportService;


    }

    // 従業員保存
    @Transactional
    public ErrorKinds save(Employee employee) {

        // パスワードチェック
        ErrorKinds result = employeePasswordCheck(employee);
        if (ErrorKinds.CHECK_OK != result) {
            return result;
        }

        // 従業員番号重複チェック
        if (findByCode(employee.getCode()) != null) {
            return ErrorKinds.DUPLICATE_ERROR;
        }

        employee.setDeleteFlg(false);

        LocalDateTime now = LocalDateTime.now();
        employee.setCreatedAt(now);
        employee.setUpdatedAt(now);

        employeeRepository.save(employee);
        return ErrorKinds.SUCCESS;
    }

    // 従業員削除
    @Transactional
    public ErrorKinds delete(String code, UserDetail userDetail) {

        // 自分を削除しようとした場合はエラーメッセージを表示
        if (code.equals(userDetail.getEmployee().getCode())) {
            return ErrorKinds.LOGINCHECK_ERROR;
        }
        Employee employee = findByCode(code);
        LocalDateTime now = LocalDateTime.now();
        employee.setUpdatedAt(now);
        employee.setDeleteFlg(true);

        // **🟢 削除対象の従業員に紐づく日報を取得**
        List<Report> reportList = reportService.findByEmployee(employee);

        // **🟢 日報情報を論理削除**
        for (Report report : reportList) {
            reportService.delete(report.getId()); // **論理削除を適用**
        }

        return ErrorKinds.SUCCESS;
    }

    // 従業員一覧表示処理
    public List<Employee> findAll() {
        return employeeRepository.findAll();
    }

    // 1件を検索
    public Employee findByCode(String code) {
        // findByIdで検索
        Optional<Employee> option = employeeRepository.findById(code);
        // 取得できなかった場合はnullを返す
        Employee employee = option.orElse(null);
        return employee;
    }

    // 従業員パスワードチェック
    private ErrorKinds employeePasswordCheck(Employee employee) {

        // 従業員パスワードの半角英数字チェック処理
        if (isHalfSizeCheckError(employee)) {

            return ErrorKinds.HALFSIZE_ERROR;
        }

        // 従業員パスワードの8文字～16文字チェック処理
        if (isOutOfRangePassword(employee)) {

            return ErrorKinds.RANGECHECK_ERROR;
        }

        employee.setPassword(passwordEncoder.encode(employee.getPassword()));

        return ErrorKinds.CHECK_OK;
    }

    // 従業員パスワードの半角英数字チェック処理
    private boolean isHalfSizeCheckError(Employee employee) {

        // 半角英数字チェック
        Pattern pattern = Pattern.compile("^[A-Za-z0-9]+$");
        Matcher matcher = pattern.matcher(employee.getPassword());
        return !matcher.matches();
    }

    // 従業員パスワードの8文字～16文字チェック処理
    public boolean isOutOfRangePassword(Employee employee) {

        // 桁数チェック
        int passwordLength = employee.getPassword().length();
        return passwordLength < 8 || 16 < passwordLength;
    }


    // 従業員情報の更新
    public ErrorKinds updateEmployee(Employee employee) {
        // 既存の従業員データを取得
        Employee existingEmployee = employeeRepository.findById(employee.getCode()).orElseThrow();

        // パスワードが空欄でない場合のみバリデーションチェックを適用
        if (employee.getPassword() != null && !employee.getPassword().trim().isEmpty()) {
            ErrorKinds passwordValidation = employeePasswordCheck(employee);
            if (passwordValidation != ErrorKinds.CHECK_OK) {
                return passwordValidation;
            }
            employee.setPassword(passwordEncoder.encode(employee.getPassword())); // パスワードを暗号化
        } else {
            // パスワードが空欄なら既存の値を維持
            employee.setPassword(existingEmployee.getPassword());
        }

        // `created_at` の値を維持
        employee.setCreatedAt(existingEmployee.getCreatedAt());

        // `updated_at` を現在時刻に更新
        employee.setUpdatedAt(LocalDateTime.now());

        // データを更新
        employeeRepository.save(employee);

        return ErrorKinds.SUCCESS; // 成功時の戻り値を追加
    }

}
