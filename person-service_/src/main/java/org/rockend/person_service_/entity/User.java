package org.rockend.person_service_.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.Audited;
import org.hibernate.envers.RelationTargetAuditMode;

import javax.validation.constraints.Size;

@Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
@Getter
@Setter
@Entity
@Table(name = "users", schema = "person")
public class User extends BaseEntity {

    @Size(max = 1024)
    @Column(name = "email", nullable = false, unique = true, length = 1024)
    private String email;

    @Size(max = 64)
    @Column(name = "first_name", nullable = false, length = 64)
    private String firstName;

    @Size(max = 64)
    @Column(name = "last_name", nullable = false, length = 64)
    private String lastName;

    //cascade: Если сохраняется новый пользователь, обновляется или удаляется уже существующий,
    // то то же самое происходит с зависимым автоматически
    @OneToOne(optional = false, cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REMOVE})
    @JoinColumn(name = "address_id", nullable = false)
    private Address address;
}
