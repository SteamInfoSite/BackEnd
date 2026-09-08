package com.steaminfosite.service;

public interface SteamAppListService {
    int collectSteamAppList(int maxResults, int maxPages);
    int steamAppList(int maxResults);
}
