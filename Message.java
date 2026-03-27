import java.io.Serializable;

public class Message implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum Type {
        AUTH_REQUEST, AUTH_SUCCESS, AUTH_FAIL, SUBMIT_TASK, TASK_SUCCESS, ERROR,
        WORKER_REGISTER, HEARTBEAT, ASSIGN_TASK, TASK_COMPLETED, TASK_FAILED // Novos tipos
    }

    private Type type;
    private String payload; 
    private String token;   
    private Task task;      

    public Message(Type type, String payload, String token, Task task) {
        this.type = type;
        this.payload = payload;
        this.token = token;
        this.task = task;
    }

    public Type getType() { return type; }
    public String getPayload() { return payload; }
    public String getToken() { return token; }
    public Task getTask() { return task; }
}