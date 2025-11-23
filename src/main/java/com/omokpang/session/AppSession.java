package com.omokpang.session;

import com.omokpang.domain.user.User;

/**
 * AppSession
 * 역할: 로그인 상태를 전역적으로 보관하는 세션 스토리지.
 * 핵심기능:
 *  - 현재 로그인 여부 저장
 *  - 로그인한 User 객체 보관
 *  - 로그아웃 시 세션 초기화
 */
public class AppSession {

    private static boolean loggedIn = false;
    private static User currentUser;

    public static boolean isLoggedIn() {
        return loggedIn;
    }

    // 로그인 여부만 바꾸고 싶을 때 필요하면 사용
    public static void setLoggedIn(boolean value) {
        loggedIn = value;
    }

    // 현재 유저 세팅 + 로그인 플래그 같이 설정
    public static void setCurrentUser(User user) {
        currentUser = user;
        loggedIn = (user != null);
    }

    // 현재 유저 조회
    public static User getCurrentUser() {
        return currentUser;
    }

    public static void logout() {
        loggedIn = false;
        currentUser = null;
    }
}
