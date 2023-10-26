package com.back.handsUp.domain;

import com.back.handsUp.baseResponse.BaseEntity;
import com.back.handsUp.domain.user.User;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.DynamicInsert;

import javax.persistence.*;

@Getter
@Setter
@Entity
@Table(name = "NOTIFICATION")
@NoArgsConstructor
@DynamicInsert
public class Notification extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "userIdx")
    private User userIdx;

    @Column
    private String title;

    @Column
    private String body;

    @Builder
    public Notification(User userIdx, String title, String body) {
        this.userIdx = userIdx;
        this.title = title;
        this.body = body;
    }
}
