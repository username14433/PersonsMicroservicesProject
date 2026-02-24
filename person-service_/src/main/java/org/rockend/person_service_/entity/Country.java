package org.rockend.person_service_.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.envers.Audited;
import org.hibernate.envers.RelationTargetAuditMode;

import javax.validation.constraints.Size;
import java.time.Instant;

//Сущность Country


//Атрибут targetAuditMode в аннотации @Audited (библиотека Hibernate Envers) определяет,
// нужно ли создавать аудит-версии для связанных сущностей
//В данном случае мы не будем создавать аудит-версии для сущностей связанных с Country
@Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
@NotNull
@Getter
@Setter
@Entity
@Table(name = "countries", schema = "person")
public class Country {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @ColumnDefault("true")
    @Column(name = "active", nullable = false)
    private Boolean active;

    @NotNull
    @ColumnDefault("true")
    @Column(name = "created", nullable = false)
    private Instant created;

    @NotNull
    @ColumnDefault("true")
    @Column(name = "updated", nullable = false)
    private Instant updated;

    @Size(max = 128) //Дополнительная проверка размера поля на уровне Java-объекта, до того как данные попадут в БД
    @Column(name = "name", nullable = false, length = 128)
    private String name;

    @Size(max = 3)
    @Column(name = "code", nullable = false, length = 3)
    private String code;

}
