package com.steaminfosite.dto.response;

import com.steaminfosite.entity.id.GameInfoEntity;
import lombok.*;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class GameInfoResponse {
    private Long appId;                 //게임 아이디
    private String gameName;            //게임 이름
    private String type;                //타입(게임, 등..)
    private Long requiredAge;           //성인게임 여부 확인 수치
    private Boolean isFree;             //무료게임 판별
    private String shortDescription;    //짧은 소개
    private String headerImage;         //헤더 이미지 링크
    private String capsuleImage;        //캡슐 이미지 링크
    private String website;             //웹사이트 링크(없을경우 null)
    private Boolean comingSoon;         //출시 할 예정인지 이미 출시 한 게임인지 구분
    private String relDate;               //출시 날짜 releaseDate
    private List<MoviesDto> movies; //게임 플래이 영상링크와 썸네일 사진 링크
    //achievements 보류
    private String developers;          //개발사
    private String publishers;          //배급사
    private String genres;              //장르 이름
    public static class Platforms{
        private Boolean windows;        //윈,맥,리눅스 지원 여부
        private Boolean mac;
        private Boolean linux;
    }

    public static GameInfoResponse from(GameInfoEntity gameInfo){
        return GameInfoResponse.builder()
                .appId(gameInfo.getAppId())
                .gameName(gameInfo.getGameName())
                .type(gameInfo.getType())
                .requiredAge(gameInfo.getRequiredAge())
                .isFree(gameInfo.getIsFree())
                .shortDescription(gameInfo.getShortDescription())
                .headerImage(gameInfo.getHeaderImage())
                .capsuleImage(gameInfo.getCapsuleImage())
                .comingSoon(gameInfo.getComingSoon())
                .relDate(gameInfo.getRelDate())
                .build();
    }
}
