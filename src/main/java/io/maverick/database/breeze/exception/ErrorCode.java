package io.maverick.database.breeze.exception;

public enum ErrorCode {

    TRANSACTION_ALREADY_EXISTS("BREEZE-001"),
    UNKNOWN_TRANSACTION("BREEZE-002"),
    UNCOMMITABLE_TRANSACTION("BREEZE-003");

    private final String code;

    /**
     * Default constructor taking an error code as a parameter
     * @param s
     */
    ErrorCode(String s) {
        this.code = s;
    }

    /**
     * To be able to grab the code of an error code
     * @return
     */
    public String getCode() {
        return code;
    }
}
