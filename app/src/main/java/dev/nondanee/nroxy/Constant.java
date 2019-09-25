package dev.nondanee.nroxy;

public class Constant {
    public static final String ACTION_START = ".action.start";
    public static final String ACTION_STOP = ".action.stop";

    public static String[] PERMISSIONS_STORAGE = {"android.permission.READ_EXTERNAL_STORAGE"};
    public static final int CODE_REQUEST_STORAGE = 100;
    public static final int CODE_PREPARE_VPN = 101;
    public static final int CODE_CHOOSE_FILE = 102;

    public static final String CHANNEL_DEFAULT_ID = "default";
    public static final String CHANNEL_DEFAULT_NAME = "Default channel";

    public static final String DEBUG_TAG = "nondanee";
    public static final String HELP_LINK = "https://github.com/nondanee/Nroxy/issues";
}