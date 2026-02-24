package org.rockend.person_service_.entity;

import jakarta.persistence.*;
import org.hibernate.envers.RevisionEntity;
import org.hibernate.envers.RevisionNumber;
import org.hibernate.envers.RevisionTimestamp;

//Базовая сущность для аудита с помощью Hibernate Envers

@Entity
// @RevisionEntity помечает сущность, которая хранит метаданные о ревизиях (версиях) данных,
// такие как время изменения и пользователь, совершивший его
@RevisionEntity
@Table(name = "revinfo", schema = "person_history")
public class BaseEnversUtilEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    //Аннотация @RevisionNumber (обычно из пакета org.hibernate.envers или Spring Data Envers)
    // используется для маркировки поля в сущности аудита, в котором Hibernate Envers
    // автоматически сохраняет порядковый номер ревизии (версии) при изменении данных
    @RevisionNumber
    @Column(name = "rev")
    private long rev;

    //Аннотация @RevisionTimestamp используется в Hibernate Envers
    // для автоматической фиксации времени (timestamp) создания ревизии при аудите изменений сущностей
    @RevisionTimestamp
    @Column(name = "revtmstmp")
    private long revtmstmp;


}