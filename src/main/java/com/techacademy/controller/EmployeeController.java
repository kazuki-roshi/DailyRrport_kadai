package com.techacademy.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;

import com.techacademy.constants.ErrorKinds;
import com.techacademy.constants.ErrorMessage;

import com.techacademy.entity.Employee;
import com.techacademy.service.EmployeeService;
import com.techacademy.service.UserDetail;

@Controller
@RequestMapping("employees")
public class EmployeeController {

    private final EmployeeService employeeService;

    @Autowired
    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    // 従業員一覧画面
    @GetMapping
    public String list(Model model) {

        model.addAttribute("listSize", employeeService.findAll().size());
        model.addAttribute("employeeList", employeeService.findAll());

        return "employees/list";
    }

    // 従業員詳細画面
    @GetMapping(value = "/{code}/")
    public String detail(@PathVariable("code") String code, Model model) {

        model.addAttribute("employee", employeeService.findByCode(code));
        return "employees/detail";
    }

    // 従業員更新画面の表示
    @GetMapping(value = "/{code}/edit")
    public String editEmployee(@PathVariable String code, Model model) {
        Employee employee = employeeService.findByCode(code); // DBから従業員情報を取得
        model.addAttribute("employee", employee); // 更新画面に従業員情報を渡す
        return "employees/update"; // Thymeleafテンプレートを表示
    }
    // 従業員更新処理
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{code}/update")
    public String updateEmployee(@Validated @ModelAttribute Employee employee, BindingResult result, Model model) {
        // **氏名のエラーチェックは変更せず、元の状態のまま**
        if (result.hasErrors()) {
            model.addAttribute("employee", employee);
            return "employees/update";
        }

        String password = employee.getPassword();

        // **パスワードが空白の場合はチェックせずにそのまま更新**
        if (password == null || password.isEmpty()) {
            employeeService.updateEmployee(employee);
            return "redirect:/employees";
        }

        // **エラーメッセージを管理**
        String passwordErrorMessage = "";

        if (!password.matches("^[a-zA-Z0-9]+$")) {
            passwordErrorMessage += ErrorMessage.getErrorValue(ErrorKinds.HALFSIZE_ERROR) + " ";
        }

        if (password.length() < 8 || password.length() > 16) {
            passwordErrorMessage += ErrorMessage.getErrorValue(ErrorKinds.RANGECHECK_ERROR) + " ";
        }

        // **エラーがある場合、パスワードの値をモデルに追加**
        if (!passwordErrorMessage.isEmpty()) {
            model.addAttribute("passwordError", passwordErrorMessage.trim());
            model.addAttribute("employee", employee);
            model.addAttribute("passwordInput", password); // ここでパスワードをモデルにセット
            return "employees/update";
        }

     // 現在のログインユーザーの権限を取得
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = auth.getAuthorities().stream()
            .anyMatch(authority -> authority.getAuthority().equals("ADMIN"));

        // **一般権限なら 403 Forbidden を返す**
        if (!isAdmin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "権限がありません");
        }
        // **管理者が一般権限へ変更した場合、セッションを破棄**




        employeeService.updateEmployee(employee);
        return "redirect:/employees";
    }


    // 従業員新規登録画面
    @GetMapping(value = "/add")
    public String create(@ModelAttribute Employee employee) {

        return "employees/new";
    }

    // 従業員新規登録処理
    @PostMapping(value = "/add")
    public String add(@Validated Employee employee, BindingResult res, Model model) {

        // パスワード空白チェック
        /*
         * エンティティ側の入力チェックでも実装は行えるが、更新の方でパスワードが空白でもチェックエラーを出さずに
         * 更新出来る仕様となっているため上記を考慮した場合に別でエラーメッセージを出す方法が簡単だと判断
         */
        if ("".equals(employee.getPassword())) {
            // パスワードが空白だった場合
            model.addAttribute(ErrorMessage.getErrorName(ErrorKinds.BLANK_ERROR),
                    ErrorMessage.getErrorValue(ErrorKinds.BLANK_ERROR));

            return create(employee);

        }

        // 入力チェック
        if (res.hasErrors()) {
            return create(employee);
        }

        // 論理削除を行った従業員番号を指定すると例外となるためtry~catchで対応
        // (findByIdでは削除フラグがTRUEのデータが取得出来ないため)
        try {
            ErrorKinds result = employeeService.save(employee);

            if (ErrorMessage.contains(result)) {
                model.addAttribute(ErrorMessage.getErrorName(result), ErrorMessage.getErrorValue(result));
                return create(employee);
            }

        } catch (DataIntegrityViolationException e) {
            model.addAttribute(ErrorMessage.getErrorName(ErrorKinds.DUPLICATE_EXCEPTION_ERROR),
                    ErrorMessage.getErrorValue(ErrorKinds.DUPLICATE_EXCEPTION_ERROR));
            return create(employee);
        }

        return "redirect:/employees";
    }

    // 従業員削除処理
    @PostMapping(value = "/{code}/delete")
    public String delete(@PathVariable("code") String code, @AuthenticationPrincipal UserDetail userDetail, Model model) {

        ErrorKinds result = employeeService.delete(code, userDetail);

        if (ErrorMessage.contains(result)) {
            model.addAttribute(ErrorMessage.getErrorName(result), ErrorMessage.getErrorValue(result));
            model.addAttribute("employee", employeeService.findByCode(code));
            return detail(code, model);
        }

        return "redirect:/employees";
    }

}
