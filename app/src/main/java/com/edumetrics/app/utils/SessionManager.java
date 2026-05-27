package com.edumetrics.app.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {
    private static final String PREF_NAME = "EduMetricsSession";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USER_NAME = "user_name";
    private static final String KEY_USER_EMAIL = "user_email";
    private static final String KEY_USER_ROLE = "user_role";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";
    private static final String KEY_CURRENT_CLASS_ID = "current_class_id";

    private final SharedPreferences prefs;
    private final SharedPreferences.Editor editor;

    public SessionManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = prefs.edit();
    }

    public void createSession(int userId, String name, String email, String role) {
        editor.putInt(KEY_USER_ID, userId);
        editor.putString(KEY_USER_NAME, name);
        editor.putString(KEY_USER_EMAIL, email);
        editor.putString(KEY_USER_ROLE, role);
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.apply();
    }

    public boolean isLoggedIn() {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    public int getUserId() {
        return prefs.getInt(KEY_USER_ID, -1);
    }

    public String getUserName() {
        return prefs.getString(KEY_USER_NAME, "");
    }

    public String getUserEmail() {
        return prefs.getString(KEY_USER_EMAIL, "");
    }

    public String getUserRole() {
        return prefs.getString(KEY_USER_ROLE, "");
    }

    public boolean isStudent() {
        return "student".equals(getUserRole());
    }

    public boolean isFaculty() {
        return "faculty".equals(getUserRole());
    }

    public void setCurrentClassId(int classId) {
        editor.putInt(KEY_CURRENT_CLASS_ID, classId);
        editor.apply();
    }

    public int getCurrentClassId() {
        return prefs.getInt(KEY_CURRENT_CLASS_ID, -1);
    }

    public void logout() {
        editor.clear();
        editor.apply();
    }
}
