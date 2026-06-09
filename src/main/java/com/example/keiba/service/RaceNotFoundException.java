package com.example.keiba.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * 指定されたレースが存在しない場合の例外（HTTP 404 にマッピング）。
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class RaceNotFoundException extends RuntimeException {

    public RaceNotFoundException(Long raceId) {
        super("Race not found: id=" + raceId);
    }
}
