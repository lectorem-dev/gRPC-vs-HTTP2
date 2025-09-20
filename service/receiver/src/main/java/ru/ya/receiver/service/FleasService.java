package ru.ya.receiver.service;

import org.springframework.stereotype.Service;
import ru.ya.libs.FleaPositionDto;
import ru.ya.libs.FleasAnswerDto;
import ru.ya.libs.FleasProblemDto;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Queue;

@Service
public class FleasService {

    public FleasAnswerDto calculateMinimalPathSum(FleasProblemDto fleasProblemDto) {
        long start = System.currentTimeMillis();

        int N = fleasProblemDto.getN(); // размеры доски
        int M = fleasProblemDto.getM();
        int S = fleasProblemDto.getFeederRow(); // номер строки кормушки, 0-индексация
        int T = fleasProblemDto.getFeederCol(); // номер столбца кормушки, 0-индексация
        int Q = fleasProblemDto.getFleasCount(); // количество блох на доске

        // Шахматная доска
        int[][] chessboard = new int[N][M];
        for (int[] row : chessboard) Arrays.fill(row, -1);

        // Q строк по два числа — координаты каждой блохи
        int[][] fleas = new int[fleasProblemDto.getFleasCount()][2];

        for (int i = 0; i < fleasProblemDto.getFleasCount(); i++) {
            FleaPositionDto flea = fleasProblemDto.getFleas().get(i);
            fleas[i][0] = flea.getRow(); // строка (x)
            fleas[i][1] = flea.getCol(); // колонка (y)
        }

        // BFS от кормушки
        Queue<int[]> queue = new ArrayDeque<>();
        queue.add(new int[]{S, T});
        chessboard[S][T] = 0;

        // Блохи по сути кони идущие к водопою
        int[][] moves = {{1, 2}, {2, 1}, {2, -1}, {1, -2}, {-1, -2}, {-2, -1}, {-2, 1}, {-1, 2}};

        while (!queue.isEmpty()) {
            int[] cur = queue.poll();
            int x = cur[0], y = cur[1];

            for (int[] m : moves) {
                int nx = x + m[0];
                int ny = y + m[1];

                if (nx >= 0 && nx < N && ny >= 0 && ny < M && chessboard[nx][ny] == -1) {
                    chessboard[nx][ny] = chessboard[x][y] + 1;
                    queue.add(new int[]{nx, ny});
                }
            }
        }

        long result = 0;
        boolean possible = true;

        for (int i = 0; i < Q; i++) {
            int fx = fleas[i][0];
            int fy = fleas[i][1];

            if (chessboard[fx][fy] == -1) {
                possible = false;
                break;
            } else {
                result += chessboard[fx][fy];
            }
        }

        return FleasAnswerDto.builder()
                .result(possible ? result : -1)
                .durationMs(System.currentTimeMillis() - start)
                .build();
    }
}