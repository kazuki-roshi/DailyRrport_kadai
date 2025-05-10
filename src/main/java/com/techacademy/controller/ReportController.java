package com.techacademy.controller;

import com.techacademy.constants.ErrorKinds;
import com.techacademy.constants.ErrorMessage;
import com.techacademy.entity.Report;
import com.techacademy.service.ReportService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/reports")
public class ReportController {

    private final ReportService reportService;

    @Autowired
    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    // 日報一覧画面への遷移（権限別の表示制御を適用）**
    @GetMapping
    public String showReportList(Model model, Principal principal) {
        String employeeCode = principal.getName();
        List<Report> reportsList = reportService.findReportsByUserRole(employeeCode); // **権限に応じたデータ取得**

        model.addAttribute("reportsList", reportsList);
        model.addAttribute("listSize", reportsList.size()); // **件数を表示**

        return "reports/list"; // **一覧画面へ遷移**
    }

    // 日報新規登録画面への遷移
    @GetMapping(value = "/add")
    public String create(Model model, Principal principal) {
        List<Report> reportsList = reportService.findByEmployeeCode(principal.getName());
        String userName = reportsList.isEmpty() ? "未設定" : reportsList.get(0).getEmployee().getName(); // 修正: Reports から氏名取得



        model.addAttribute("report", new Report()); // 新規オブジェクトを追加
        model.addAttribute("userName", userName); // 画面に渡す

        return "reports/new";
    }

    //日報新規登録処理
    @PostMapping("/add")
    public String add(@Validated @ModelAttribute Report report, BindingResult result, Model model, Principal principal) {
        // **日付が入力されているか確認**
        boolean isReportDateEmpty = result.hasFieldErrors("reportDate");

        // **既存の日付チェック**
        boolean hasDuplicateDateError = reportService.existsByEmployeeAndDateWithoutExclusion(principal.getName(), report.getReportDate());

        // **エラーメッセージ適用**
        if (hasDuplicateDateError) {
            result.rejectValue("reportDate", "error.reportDate", "既に登録されている日付です");
        } else if (isReportDateEmpty) {
            result.rejectValue("reportDate", "error.reportDate", ErrorMessage.getErrorValue(ErrorKinds.BLANK_ERROR));
        }

        // **どの項目でもエラーが発生した場合、入力画面に戻る**
        if (result.hasErrors()) {
            model.addAttribute("report", report);

            // **タイトルのバリデーションエラー**
            if (result.hasFieldErrors("title")) {
                model.addAttribute("titleError", ErrorMessage.getErrorValue(ErrorKinds.BLANK_ERROR));
            }

            // **内容のバリデーションエラー**
            if (result.hasFieldErrors("content")) {
                model.addAttribute("contentError", ErrorMessage.getErrorValue(ErrorKinds.BLANK_ERROR));
            }

            // **日付のエラーメッセージ適用**
            if (result.hasFieldErrors("reportDate")) {
                model.addAttribute("reportDateError", result.getFieldError("reportDate").getDefaultMessage());
            }

            // **ログイン中のユーザー名を維持**
            List<Report> reportsList = reportService.findByEmployeeCode(principal.getName());
            String userName = reportsList.isEmpty() ? "未設定" : reportsList.get(0).getEmployee().getName();
            model.addAttribute("userName", userName);

            return "reports/new"; // **エラーがある場合は入力画面に戻る**
        }

        // **エラーがない場合のみ登録を実行**
        report.setEmployee(reportService.findEmployeeByCode(principal.getName()));
        report.setCreatedAt(LocalDateTime.now());
        report.setUpdatedAt(LocalDateTime.now());
        report.setDeleteFlg(false);
        reportService.save(report);

        return "redirect:/reports";
    }


    // 日報詳細画面への遷移
    @GetMapping("/{id}/")
    public String showReportDetail(@PathVariable("id") Long id, Model model) {
        Report report = reportService.findById(id).orElseThrow(() -> new RuntimeException("Report not found"));
        model.addAttribute("report", report);
        return "reports/detail"; // 詳細画面のテンプレート (reports/detail.html) に遷移
    }

    // 日報更新画面への遷移
    @GetMapping("/{id}/update")
    public String edit(@PathVariable Long id, Model model) {
        Optional<Report> optionalReport = reportService.findById(id);

        if (optionalReport.isEmpty()) {
            return "redirect:/reports"; // **該当データがない場合は一覧画面へ**
        }

        Report report = optionalReport.get(); // **DBから取得**

        model.addAttribute("report", report); // **画面に渡す**
        model.addAttribute("reportDateStr", report.getReportDate() != null ? report.getReportDate().toString() : ""); // `yyyy-MM-dd` 形式で渡す
        return "reports/update";
    }



    // 更新処理
    @PostMapping("/{id}/update")
    public String update(@Validated @ModelAttribute Report report, BindingResult result, Model model, @PathVariable("id") Long id, Principal principal) {
        Report existingReport = reportService.findById(id).orElse(null);

        if (existingReport == null) {
            return "redirect:/reports"; // **存在しない場合は一覧画面へ**
        }

        // ** 既存の日報の従業員情報を維持する **
        report.setEmployee(existingReport.getEmployee());


     // **既存の日付チェックを「画面表示中の従業員」で検索**
        boolean hasDuplicateDateError = reportService.existsByEmployeeAndDateExcludeCurrent(
                existingReport.getEmployee().getCode(), // 🔹 変更: もとの従業員コードで検索
                report.getReportDate(),
                id
        );

        // ** 日付のバリデーションエラーを追加**
        if (result.hasFieldErrors("reportDate")) {
            model.addAttribute("reportDateError", result.getFieldError("reportDate").getDefaultMessage());
        }


        if (hasDuplicateDateError) {
            model.addAttribute("reportDateError", "既に登録されている日付です");
        }

        // ** 入力エラーのチェック**
        if (result.hasErrors() || hasDuplicateDateError) { // 日付のエラーを含める
            model.addAttribute("report", report);

            if (result.hasFieldErrors("title")) {
                model.addAttribute("titleError", result.getFieldError("title").getDefaultMessage());
            }
            if (result.hasFieldErrors("content")) {
                model.addAttribute("contentError", result.getFieldError("content").getDefaultMessage());
            }

            return "reports/update"; // **エラー時は更新画面へ戻る**
        }

        // ** 更新処理**
        existingReport.setReportDate(report.getReportDate());
        existingReport.setTitle(report.getTitle());
        existingReport.setContent(report.getContent());
        existingReport.setUpdatedAt(LocalDateTime.now());

        reportService.save(existingReport);
        return "redirect:/reports";
    }



    // 削除処理
    @PostMapping("/{id}/delete")
    public String delete(@PathVariable("id") Long id) {
        reportService.delete(id);
        return "redirect:/reports"; // **論理削除後に一覧画面へ**
    }


}