package com.steaminfosite.service.user;

import com.steaminfosite.entity.type.SiteUserRole;
import com.steaminfosite.entity.user.SiteUserEntity;
import com.steaminfosite.repository.SiteUserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class SiteUserServiceTest {

    @Mock
    private SiteUserRepository siteUserRepository;

    @InjectMocks
    private SiteUserService siteUserService;

    @Test
    @DisplayName("기존 Steam64 ID 사용자는 새로 저장하지 않음")
    void findExistingUser() {
        SiteUserEntity existingUser = new SiteUserEntity("76561198000000000");
        given(siteUserRepository.findBySteamId("76561198000000000"))
                .willReturn(Optional.of(existingUser));

        SiteUserEntity result = siteUserService.findOrCreate("76561198000000000");

        assertThat(result).isSameAs(existingUser);
        then(siteUserRepository).should(never()).save(any(SiteUserEntity.class));
    }

    @Test
    @DisplayName("처음 로그인한 Steam64 ID사용자를 저장")
    void createNewUser() {
        given(siteUserRepository.findBySteamId("76561198000000000"))
                .willReturn(Optional.empty());
        given(siteUserRepository.save(any(SiteUserEntity.class)))
                .willAnswer(invocation ->
                        invocation.getArgument(0)
                );

        SiteUserEntity result = siteUserService.findOrCreate("76561198000000000");

        assertThat(result.getSteamId()).isEqualTo("76561198000000000");
        assertThat(result.getRole()).isEqualTo(SiteUserRole.USER);
        then(siteUserRepository).should().save(any(SiteUserEntity.class));
    }

    @Test
    @DisplayName("Steam64 ID가 비어 있으면 사용자 조회를 거부")
    void rejectEmptySteamId() {
        String steamId = " ";

        Throwable exception = catchThrowable(
                () -> siteUserService.findOrCreate(steamId)
        );

        assertThat(exception).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Steam64 ID가 필요합니다");
        then(siteUserRepository).shouldHaveNoInteractions();
    }
}