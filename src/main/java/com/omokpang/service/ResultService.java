package com.omokpang.service;

import com.omokpang.repository.ResultRepository;

/** ResultService
 * 역할: 게임 종료 시 players 결과 배열을 해석해 DB에 정산을 반영하는 서비스.
 * 핵심기능: 순위·포인트 정보를 기반으로 ResultRepository.updateUserResult 호출.
 */
public class ResultService {

    private static final ResultService INSTANCE = new ResultService();
    public static ResultService getInstance() { return INSTANCE; }

    private final ResultRepository resultRepository = new ResultRepository();

    private ResultService() {}

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