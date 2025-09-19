package ru.ya.libs;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class FleasProblemDto {
    private int n; // число строк доски
    private int m; // число столбцов доски
    private int feederRow; // строка кормушки, 0-индексация
    private int feederCol; // столбец кормушки, 0-индексация
    private List<FleaPositionDto> fleas; // список блох
}