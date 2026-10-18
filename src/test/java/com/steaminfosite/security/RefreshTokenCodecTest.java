package com.steaminfosite.security;

import org.junit.jupiter.api.Test;

import java.util.Base64;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RefreshTokenCodecTest {
    private final RefreshTokenCodec codec = new RefreshTokenCodec();

    @Test
    void generatesThirtyTwoRandomBytes() {
        String token = codec.generate();

        assertThat(token).matches("[A-Za-z0-9_-]{43}");
        assertThat(Base64.getUrlDecoder().decode(token)).hasSize(32);
    }

    @Test
    void generatedTokensDoNotRepeatInSample() {
        Set<String> tokens = IntStream.range(0, 100)
                .mapToObj(number -> codec.generate())
                .collect(Collectors.toSet());

        assertThat(tokens).hasSize(100);
    }

    @Test
    void hashIsStableAndDoesNotExposeRawToken() {
        String first = codec.generate();
        String second = codec.generate();

        String firstHash = codec.hash(first);

        assertThat(firstHash).matches("[0-9a-f]{64}");
        assertThat(firstHash).isEqualTo(codec.hash(first));
        assertThat(firstHash).isNotEqualTo(first);
        assertThat(firstHash).isNotEqualTo(codec.hash(second));
    }

    @Test
    void rejectsBlankToken() {
        assertThatThrownBy(() -> codec.hash(" "))
                .isInstanceOf(IllegalArgumentException.class);
    }
}