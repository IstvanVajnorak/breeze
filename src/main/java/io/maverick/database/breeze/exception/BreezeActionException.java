package io.maverick.database.breeze.exception;

/**
 * Created by istvanvajnorak on 2020. 05. 26..
 *
 * A generic exception that ships an error code
 */
public class BreezeActionException extends RuntimeException{

    final ErrorContext context;

    public BreezeActionException(ErrorCode errorCode, String message){
        super(message);
        this.context =  new ErrorContext(errorCode,message);
    }

    /**
     * The erro code the exception was raised for
     * @return
     */
    public ErrorCode getErrorCode(){
        return context.getCause();
    }

    @Override
    public String toString() {
        return getContext().getCause()+": "+super.getMessage();
    }

    public ErrorContext getContext(){
        return context;
    }

    /**
     * A simple way to deliver only the relevant portions to our controllers
     */
    private final class ErrorContext{
        private final ErrorCode cause;
        private final String message;
        private final String errorCode;

        private ErrorContext(ErrorCode errorCode,String message) {
            this.cause = errorCode;
            this.errorCode = errorCode.getCode();
            this.message = message;
        }

        public String getMessage() {
            return message;
        }

        public String getErrorCode() {
            return errorCode;
        }

        public ErrorCode getCause() {
            return cause;
        }
    }
}
