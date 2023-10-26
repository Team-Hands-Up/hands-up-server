package com.back.handsUp.repository.user;

import com.back.handsUp.domain.user.School;
import com.back.handsUp.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUserIdx(Long userIdx);

    Optional<User> findByEmailAndStatus(String email, String status);
    Optional<User> findByNickname(String nickname);

    @Query("select u from User u where u.nickname = ?1 and u.schoolIdx = ?2 and u.status = ?3")
    List<User> findByNicknameAndSchoolIdx(String nickname, School school, String status);
}
