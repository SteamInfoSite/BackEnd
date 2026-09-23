package com.steaminfosite.repository;

import com.steaminfosite.entity.type.SiteUserRole;
import com.steaminfosite.entity.user.SiteUserEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class SiteUserRepositoryTest {
    @Autowired
    private SiteUserRepository siteUserRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    @DisplayName("Steam64 ID로 사용자를 조회")
    void findBySteamId() {
        SiteUserEntity user = new SiteUserEntity("76561198000000000");
        siteUserRepository.saveAndFlush(user);
        entityManager.clear();

        Optional<SiteUserEntity> foundUser = siteUserRepository.findBySteamId("76561198000000000");

        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getSteamId()).isEqualTo("76561198000000000");
        assertThat(foundUser.get().getRole()).isEqualTo(SiteUserRole.USER);
    }
}