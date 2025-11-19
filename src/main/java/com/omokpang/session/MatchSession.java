package com.omokpang.session;

/**
 * 역할: 매칭 결과(게임 모드, 참가자 닉네임 목록, 내 닉네임)를
 *       화면 간에 공유하기 위한 간단한 전역 저장소.
 */
public class MatchSession {

    private static String mode;          // 예: "1v1"
    private static String[] players;     // 예: ["채채채", "채빵"]
    private static String myNickname;    // 현재 클라이언트 유저 닉네임

    public static String getMode() {
        return mode;
    }

    public static void setMode(String mode) {
        MatchSession.mode = mode;
    }

    public static String[] getPlayers() {
        return players;
    }

    public static void setPlayers(String[] players) {
        MatchSession.players = players;
    }

    public static String getMyNickname() {
        return myNickname;
    }

    public static void setMyNickname(String myNickname) {
        MatchSession.myNickname = myNickname;
    }

    /** 필요하면 이전 매칭 정보 지울 때 사용 */
    public static void clear() {
        mode = null;
        players = null;
        myNickname = null;
    }
}
