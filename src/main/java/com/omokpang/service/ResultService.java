/** ResultService
 * 역할: 게임 종료 정산/기록.
 *  - players 배열을 받아서 DB에 wins / losses / points 반영
 */
package com.omokpang.service;

import com.omokpang.repository.ResultRepository;

public class ResultService {

    private static final ResultService INSTANCE = new ResultService();
    public static ResultService getInstance() { return INSTANCE; }

    private final ResultRepository resultRepository = new ResultRepository();

    private ResultService() {}

    /**
     * @param players [ [순위, 닉네임, 이번 판 포인트(80/40), 아바타경로], ... ]
     */
    public void applyGameResult(String[][] players) {
        if (players == null) return;

        for (String[] p : players) {
            if (p == null || p.length < 3) continue;

            int rank       = Integer.parseInt(p[0]); // "1" or "2"
            String nickname = p[1];
            int pointDelta = Integer.parseInt(p[2]); // 80 or 40

            boolean isWinner = (rank == 1);
            resultRepository.updateUserResult(nickname, isWinner, pointDelta);
        }
    }
}