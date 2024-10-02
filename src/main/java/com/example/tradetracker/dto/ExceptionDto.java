package com.example.tradetracker.dto;

import com.example.tradetracker.response.ErrorCode;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

public record ExceptionDto (
        @NotNull Integer code,
        @NotNull String message
) {

    public static ExceptionDto of(ErrorCode errorCode) {
        return new ExceptionDto(
                errorCode.getCode(),
                errorCode.getMessage()
        );
    }
}
