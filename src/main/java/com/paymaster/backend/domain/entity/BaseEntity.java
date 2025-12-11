package com.paymaster.backend.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * کلاس پایه برای تمام Entityها (موجودیت‌های دیتابیس).
 * شامل فیلدهای مشترک: id، زمان ایجاد (createdAt)، و زمان آخرین به‌روزرسانی (updatedAt).
 */
@MappedSuperclass
@Getter
@Setter
public abstract class BaseEntity {

    /**
     * شناسه منحصر به فرد (Primary Key) موجودیت.
     * با استفاده از استراتژی IDENTITY به صورت خودکار تولید می‌شود.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * زمان ایجاد موجودیت.
     * این فیلد در زمان ایجاد مقداردهی می‌شود و پس از آن غیرقابل تغییر است.
     */
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /**
     * زمان آخرین به‌روزرسانی موجودیت.
     * این فیلد در هر عملیات به‌روزرسانی (Update) جدید، به‌روزرسانی می‌شود.
     */
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}