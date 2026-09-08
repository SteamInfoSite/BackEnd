package com.steaminfosite.dto.applist;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class SteamAppList {
    private List<SteamAppDto> apps;

    @JsonProperty("have_more_results")
    private Boolean haveMoreResults;    //그다음 페이지가 있을때 true 없으면 아예 안뜸

    @JsonProperty("last_appid")
    private Long lastAppId; //마찬가지로 뒤에 더있으면 해당 페이지의 마지막 appid들어감
}
