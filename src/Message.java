import java.io.Serializable;

public class Message implements Serializable {
    public static final long serialVersionUID = 1L;

    private String msgHeader;
    private String msgBody;

    public Message(String msgHeader, String msgBody){
        this.msgHeader = msgHeader;
        this.msgBody = msgBody;
    }



    public String getMsgHeader() {
        return msgHeader;
    }

    public void setMsgHeader(String msgHeader) {
        this.msgHeader = msgHeader;
    }

    public String getMsgBody() {
        return msgBody;
    }

    public void setMsgBody(String msgBody) {
        this.msgBody = msgBody;
    }

    @Override
    public String toString() {
        return "Message{" +
                "msgHeader='" + msgHeader + '\'' +
                ", msgBody='" + msgBody + '\'' +
                '}';
    }
}
