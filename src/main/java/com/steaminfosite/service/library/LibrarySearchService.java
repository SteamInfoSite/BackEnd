package com.steaminfosite.service.library;

import com.steaminfosite.dto.response.library.LibraryResponse;

public interface LibrarySearchService {
    LibraryResponse getMyLibrary(String steamId);
}