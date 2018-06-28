package responses;

public class ResponseMessage {

    private int status;

    private String message;

    public ResponseMessage() {
        super();
    }

    public ResponseMessage(int status) {
        this.status = status;
    }

    public ResponseMessage(int status, String message) {
        this.status = status;
        this.message = message;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

}
