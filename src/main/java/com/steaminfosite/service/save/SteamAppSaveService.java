package com.steaminfosite.service.save;

import com.steaminfosite.dto.applist.SteamAppDto;

import java.util.List;

public interface SteamAppSaveService {
    int saveOrUpdateSteamApps(List<SteamAppDto> apps);
}
