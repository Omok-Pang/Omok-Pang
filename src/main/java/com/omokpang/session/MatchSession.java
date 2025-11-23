package com.omokpang.session;

import com.omokpang.domain.card.Card;

import java.util.List;

/**
 * MatchSession
 * 역할: 매칭~게임 시작까지 모든 화면에서 공유되는 전역 매칭 세션.
 * 핵심기능:
 *  - 게임 모드(1v1/2v2/FFA) 보관
 *  - 매칭된 플레이어 목록/팀 정보/아바타 정보 저장
 *  - 내 닉네임, 선택한 카드 2장 저장
 *  - 로비 복귀 시 clear()로 세션 전체 초기화
 */
public class MatchSession {

    /** 게임 모드 (예: "1v1", "2v2" 등) */
    private static String mode;
    private static String requestedMode;

    /** 매칭된 플레이어 닉네임 목록 (예: ["채채채", "채빵"]) */
    private static String[] players;

    /** 현재 이 클라이언트의 유저 닉네임 */
    private static String myNickname;

    /** 각 플레이어에게 배정된 아바타 이미지 경로 (예: "/images/user/user1.png" 등) */
    private static String[] playerAvatars;

    /** 카드 선택 화면에서 이 유저가 고른 카드 2장 */
    private static List<Card> mySelectedCards;

    private static int[] playerTeam;  // 각 플레이어가 어느 팀인지 (0 또는 1)

    // ===================== 기본 정보 (모드 / 플레이어 / 내 닉네임) =====================

    public static String getMode() {
        return mode;
    }

    public static void setMode(String mode) {
        MatchSession.mode = mode;
    }

    public static void setRequestedMode(String m) {
        requestedMode = m;
    }

    public static String getRequestedMode() {
        return requestedMode;
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

    public static void setPlayerTeam(int[] team) {
        playerTeam = team;
    }

    public static int[] getPlayerTeam() {
        return playerTeam;
    }

    /**
     * 현재 클라이언트의 닉네임을 저장한다.
     * - MatchingController에서 매칭 큐에 진입할 때 한 번 세팅해준다.
     */
    public static void setMyNickname(String myNickname) {
        MatchSession.myNickname = myNickname;
    }

    // ===================== 아바타 관련 =====================

    /** 각 플레이어에게 배정된 아바타 이미지 경로 세팅 */
    public static void setPlayerAvatars(String[] avatars) {
        playerAvatars = avatars;
    }

    /** 아바타 이미지 경로 조회 */
    public static String[] getPlayerAvatars() {
        return playerAvatars;
    }

    // ===================== 카드 선택 관련 =====================

    public static List<Card> getMySelectedCards() {
        return mySelectedCards;
    }

    public static void setMySelectedCards(List<Card> cards) {
        mySelectedCards = cards;
    }

    // ===================== 세션 초기화 =====================

    /**
     * 필요하면 이전 매칭 정보를 지울 때 사용.
     * (로비로 완전히 빠질 때 등)
     */
    public static void clear() {
        mode = null;
        players = null;
        myNickname = null;
        playerAvatars = null;
        mySelectedCards = null;
    }
}