package com.back.handsUp.repository.fcm;

import com.back.handsUp.domain.fcmToken.FcmToken;
import com.back.handsUp.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FcmTokenRepository extends JpaRepository<FcmToken, Long> {
    Optional<FcmToken> findFcmTokenByUser(User user);
}
