package com.auction.model.user;

public class User extends Entity {
    // MÃ HÓA VÀ GIẢI MÃ PASSWORD
    
    public static String encode(String password) {
        StringBuilder encoded = new StringBuilder();
        for (char c : password.toCharArray()) {
            encoded.append((char) (c + 5));
        }
        return encoded.toString();
    }

    public static String decode(String encodedPassword) {
        StringBuilder decoded = new StringBuilder();
        for (char c : encodedPassword.toCharArray()) {
            decoded.append((char) (c - 5));
        }
        return decoded.toString();
    }

    // KHAI BÁO THUỘC TÍNH

    private static int idCounter = 0;
    protected final String userName;
    protected String fullName;
    protected String email;
    protected String password;
    protected String numberPhone;
    protected final String citizenId;

    
    // KHAI BÁO CONSTRUCTOR
    public User(String userName, String password, String fullName, String email, String numberPhone, String citizenId) {
        super("USR" + (++idCounter));
        this.userName = userName;
        this.fullName = fullName;
        this.email = email;
        this.password = encode(password);
        this.numberPhone = numberPhone;
        this.citizenId = citizenId;
    }

    // LẤY VÀ UPDATE THUỘC TÍNH

    public String getUserName() { return userName; }

    public String getFullName() { return fullName; }
    public void updateFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void updateEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void updatePassword(String password) { this.password = encode(password); }

    public String getNumberPhone() { return numberPhone; }
    public void updateNumberPhone(String numberPhone) { this.numberPhone = numberPhone; }

    public String getCitizenId() { return citizenId; }

}
