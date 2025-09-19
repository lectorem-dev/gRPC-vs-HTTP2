package ru.ya.receiver.service;

import org.springframework.stereotype.Service;
import ru.ya.libs.FleaPositionDto;
import ru.ya.libs.FleasProblemDto;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Queue;

@Service
public class FleasService {

    private static final int[][] MOVES = {
            {1,2},
            {2,1},
            {2,-1},
            {1,-2},
            {-1,-2},
            {-2,-1},
            {-2,1},
            {-1,2}
    };

    public long calculateMinimalPathSum(FleasProblemDto problem) {
        int n = problem.getN();
        int m = problem.getM();
        int feederRow = problem.getFeederRow();
        int feederCol = problem.getFeederCol();

        int[][] board = new int[n][m];
        for (int[] row : board) Arrays.fill(row, -1);

        Queue<int[]> queue = new ArrayDeque<>();
        queue.add(new int[]{feederRow, feederCol});
        board[feederRow][feederCol] = 0;

        while (!queue.isEmpty()) {
            int[] cur = queue.poll();
            int x = cur[0], y = cur[1];
            for (int[] move : MOVES) {
                int nx = x + move[0];
                int ny = y + move[1];
                if (nx >= 0 && nx < n && ny >= 0 && ny < m && board[nx][ny] == -1) {
                    board[nx][ny] = board[x][y] + 1;
                    queue.add(new int[]{nx, ny});
                }
            }
        }

        long result = 0;
        for (FleaPositionDto flea : problem.getFleas()) {
            int fx = flea.getRow();
            int fy = flea.getCol();
            if (board[fx][fy] == -1) return -1; // блоха не может добраться
            result += board[fx][fy];
        }

        return result;
    }
}