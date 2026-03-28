package model;

import java.io.Serializable;

public class Task implements Serializable {
    private static final long serialVersionUID = 1L;
    
    public enum Status {
        PENDING, ACTIVE, COMPLETED, FAILED
    }

    private String taskId;
    private String clientToken;
    private String description;
    private Status status;
    private int lamportTimestamp;
    private String assignedWorkerId; // NOVO CAMPO

    public Task(String taskId, String clientToken, String description, int lamportTimestamp) {
        this.taskId = taskId;
        this.clientToken = clientToken;
        this.description = description;
        this.status = Status.PENDING;
        this.lamportTimestamp = lamportTimestamp;
    }

    // Getters e Setters
    public String getTaskId() { return taskId; }
    public String getClientToken() { return clientToken; }
    public String getDescription() { return description; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public int getLamportTimestamp() { return lamportTimestamp; }
    public void setLamportTimestamp(int lamportTimestamp) { this.lamportTimestamp = lamportTimestamp; }
    public String getAssignedWorkerId() { return assignedWorkerId; }
    public void setAssignedWorkerId(String assignedWorkerId) { this.assignedWorkerId = assignedWorkerId; }

    @Override
    public String toString() {
        return "Task{" + "id='" + taskId + '\'' + ", status=" + status + ", time=" + lamportTimestamp + '}';
    }
}