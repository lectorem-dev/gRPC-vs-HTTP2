package ru.ya.libs;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FleasAnswerDto {
    private long result;     // ответ задачи
    private long durationMs; // время решения в миллисекундах
}