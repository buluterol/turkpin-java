package io.github.buluterol.turkpin;

/** API error code other than "000", or an HTTP / XML failure (code is then empty). */
public class TurkpinException extends RuntimeException {
    private final String code;

    public TurkpinException(String message, String code) {
        super(message);
        this.code = code;
    }

    public TurkpinException(String message, Throwable cause) {
        super(message, cause);
        this.code = "";
    }

    /** API error code such as "012"; empty for HTTP / parse errors. */
    public String getCode() {
        return code;
    }
}
