package com.techacademy.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.hibernate.annotations.SQLRestriction;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Entity
@Data
@Table(name = "reports")
@SQLRestriction("delete_flg = 0") // 0 のとき表示
public class Report {

    // ID (自動採番)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 日付
    @Column(name = "report_date", nullable = false)
    @NotNull
    private LocalDate reportDate;

    // タイトル
    @Column(name = "title", nullable = false, length = 100)
    @NotEmpty
    @Size(max = 100, message = "100文字以下で入力してください") // 100文字以下のバリデーションを追加
    private String title;

    // 内容
    @Column(name = "content", columnDefinition = "LONGTEXT")
    @NotEmpty
    @Size(max = 600, message = "600文字以下で入力してください") // 600文字以下のバリデーションを追加
    private String content;

    // 社員情報 (ManyToOne)
    @ManyToOne
    @JoinColumn(name = "employee_code", referencedColumnName = "code", nullable = false)
    private Employee employee;

    // 削除フラグ (0: 有効, 1: 削除)
    @Column(columnDefinition = "TINYINT", nullable = false)
    private boolean deleteFlg;

    // 登録日時
    @Column(nullable = false)
    private LocalDateTime createdAt;

    // 更新日時
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}