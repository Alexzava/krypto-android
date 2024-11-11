package com.alexzava.krypto;

public class Constants {
    public static final String APP_HOST = "krypto";
    public static final String REPO_URL = "https://github.com/alexzava/krypto-android";

    public static final int CHUNK_SIZE = 64 * 1024 * 1024;
    public static final String SIGNATURE_SYMMETRIC = "zDKO6XYXioc";
    public static final String SIGNATURE_ASYMMETRIC = "hTWKbfoikeg";

    public static final String LINK_APP = "app://krypto/";
    public static final String LINK_HAT_SH = "https://hat.sh/?tab=decryption&publicKey=";
    public static final String HAT_SH_HOST = "hat.sh";

    public static final int PASSWORD_LEN_MIN = 8;
    public static final int PASSWORD_DEFAULT_LEN = 20;
    public static final String PASSWORD_GENERATOR_ALLOWED_CHARACTERS = "abcdefghijklmnopqrstuvwxyz0123456789_#!@*%$^&";

    public static final String QR_CODE_PUBLIC_KEY_PARAM = "publicKey";

    public static final String NOTIFICATION_PROGRESS_CHANNEL_ID = "PROGRESS_NOTIFICATION";
    public static final String NOTIFICATION_PROGRESS_CHANNEL_NAME = "Progress";
    public static final String PROGRESS_BROADCAST_NAME = "PROGRESS_BROADCAST";

    public static final String MODE_PASSWORD = "MODE_PASSWORD";
    public static final String MODE_PUBLIC_KEY = "MODE_PUBLIC_KEY";

    public static final String ACTION_ENCRYPT = "ACTION_ENCRYPT";
    public static final String ACTION_DECRYPT = "ACTION_DECRYPT";

    public static final String ARG_SERVICE_URI = "uri";
    public static final String ARG_SERVICE_ACTION = "action";
    public static final String ARG_SERVICE_KEY_HEX = "keyHex";
    public static final String ARG_SERVICE_SALT_HEX = "keySalt";
    public static final String ARG_SERVICE_KEY_MODE = "keyMode";

    public static final String BROADCAST_RECEIVER_PROGRESS = "BROADCAST_RECEIVER_PROGRESS";
    public static final String BROADCAST_RECEIVER_IS_DONE = "BROADCAST_RECEIVER_IS_DONE";
    public static final String BROADCAST_RECEIVER_DECRYPTION_ERROR = "BROADCAST_RECEIVER_DECRYPTION_ERROR";
}
