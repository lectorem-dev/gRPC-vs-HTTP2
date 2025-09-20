package ru.ya.libs;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FleasAnswerDto {
    private long result;     // ответ задачи
    private long durationNs; // время решения в миллисекундах
}