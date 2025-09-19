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

    // ход коня
    private static final int[][] MOVES = {
            {1, 2}, {2, 1}, {2, -1}, {1, -2},
            {-1, -2}, {-2, -1}, {-2, 1}, {-1, 2}
    };

    /**
     * Решает задачу для одного FleasProblemDto и возвращает ответ + время (ms).
     * Входные координаты ожидаются в 1-based (как в условии). Метод внутри переводит в 0-based.
     */
    public FleasAnswerDto calculateMinimalPathSum(FleasProblemDto problem) {
        long startNano = System.nanoTime();

        // базовые проверки
        if (problem == null) {
            return buildAnswer(-1, System.nanoTime() - startNano);
        }
        int n = problem.getN();
        int m = problem.getM();

        if (n <= 0 || m <= 0) {
            return buildAnswer(-1, System.nanoTime() - startNano);
        }

        // переводим кормушку из 1-based в 0-based (если вход в 0-based — убери -1)
        int feederRow = problem.getFeederRow() - 1;
        int feederCol = problem.getFeederCol() - 1;

        if (!inBounds(feederRow, feederCol, n, m)) {
            return buildAnswer(-1, System.nanoTime() - startNano);
        }

        // готовим поле
        int[][] board = new int[n][m];
        for (int[] row : board) Arrays.fill(row, -1);

        Queue<int[]> queue = new ArrayDeque<>();
        queue.add(new int[]{feederRow, feederCol});
        board[feederRow][feederCol] = 0;

        // BFS от кормушки
        while (!queue.isEmpty()) {
            int[] cur = queue.poll();
            int x = cur[0], y = cur[1];
            for (int[] move : MOVES) {
                int nx = x + move[0];
                int ny = y + move[1];
                if (inBounds(nx, ny, n, m) && board[nx][ny] == -1) {
                    board[nx][ny] = board[x][y] + 1;
                    queue.add(new int[]{nx, ny});
                }
            }
        }

        long sum = 0;
        if (problem.getFleas() != null) {
            for (FleaPositionDto flea : problem.getFleas()) {
                if (flea == null) {
                    return buildAnswer(-1, System.nanoTime() - startNano);
                }
                // перевод позиции блохи в 0-based
                int fx = flea.getRow() - 1;
                int fy = flea.getCol() - 1;
                if (!inBounds(fx, fy, n, m)) {
                    // некорректная координата — считаем как невозможность достичь
                    return buildAnswer(-1, System.nanoTime() - startNano);
                }
                if (board[fx][fy] == -1) {
                    // блоха не может добраться
                    return buildAnswer(-1, System.nanoTime() - startNano);
                }
                sum += board[fx][fy];
            }
        }

        long durationMs = (System.nanoTime() - startNano) / 1_000_000;
        return FleasAnswerDto.builder()
                .result(sum)
                .durationMs(durationMs)
                .build();
    }

    private boolean inBounds(int x, int y, int n, int m) {
        return x >= 0 && x < n && y >= 0 && y < m;
    }

    private FleasAnswerDto buildAnswer(long result, long elapsedNano) {
        long durationMs = elapsedNano / 1_000_000;
        return FleasAnswerDto.builder()
                .result(result)
                .durationMs(durationMs)
                .build();
    }
}