package ru.ya.libs;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FleaPositionDto {
    private int row; // 0-индексация
    private int col; // 0-индексация
}