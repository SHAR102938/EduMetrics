package com.edumetrics.app.database.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "users")
public class User {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String name;
    public String email;
    public String passwordHash;
    public String role; // "student" or "faculty"
    public String profileImage;
    public long createdAt;

    public User() {
        this.createdAt = System.currentTimeMillis();
    }

    public User(String name, String email, String passwordHash, String role) {
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.createdAt = System.currentTimeMillis();
    }
}
