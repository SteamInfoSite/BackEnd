package com.steaminfosite.dto.response;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Getter
@Setter
public class MoviesDto {
    private String thumbnail;
    private String video;   //hls_h264
}
