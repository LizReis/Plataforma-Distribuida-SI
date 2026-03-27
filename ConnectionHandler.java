import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;

public class ConnectionHandler implements Runnable {
    private Socket clientSocket;
    private LamportClock clock;
    private AuthService authService;
    private ConcurrentHashMap<String, WorkerInfo> activeWorkers;
    private TaskDistributor taskDistributor; // Novo

    public ConnectionHandler(Socket clientSocket, LamportClock clock, AuthService authService, 
                             ConcurrentHashMap<String, WorkerInfo> activeWorkers, TaskDistributor taskDistributor) {
        this.clientSocket = clientSocket;
        this.clock = clock;
        this.authService = authService;
        this.activeWorkers = activeWorkers;
        this.taskDistributor = taskDistributor;
    }

    @Override
    public void run() {
        try (
            ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream())
        ) {
            Message message = (Message) in.readObject();
            clock.update(message.getTimestamp());

            if (message.getType() == Message.Type.AUTH_REQUEST) {
                String[] credentials = message.getPayload().split(":");
                String token = authService.authenticate(credentials[0], credentials[1]);

                if (token != null) {
                    out.writeObject(new Message(Message.Type.AUTH_SUCCESS, "Login efetuado", token, null));
                } else {
                    out.writeObject(new Message(Message.Type.AUTH_FAIL, "Credenciais inválidas", null, null));
                }
            } 
            else if (message.getType() == Message.Type.SUBMIT_TASK) {
                if (authService.validateToken(message.getToken())) {
                    Task receivedTask = message.getTask();
                    System.out.println("Nova tarefa submetida: " + receivedTask.getDescription());
                    
                    // Repassa para o balanceador de carga distribuir
                    taskDistributor.dispatchTask(receivedTask);
                    
                    out.writeObject(new Message(Message.Type.TASK_SUCCESS, "Tarefa em processamento", null, null));
                } else {
                    out.writeObject(new Message(Message.Type.ERROR, "Não autenticado", null, null));
                }
            }
            else if (message.getType() == Message.Type.WORKER_REGISTER) {
                String[] data = message.getPayload().split(":");
                String workerId = data[0];
                int workerPort = Integer.parseInt(data[1]);
                activeWorkers.put(workerId, new WorkerInfo(workerId, clientSocket.getInetAddress().getHostAddress(), workerPort));
                System.out.println("Worker registrado: " + workerId);
                out.writeObject(new Message(Message.Type.TASK_SUCCESS, "Registro efetuado", null, null));
            }
            else if (message.getType() == Message.Type.HEARTBEAT) {
                String workerId = message.getPayload();
                if (activeWorkers.containsKey(workerId)) {
                    activeWorkers.get(workerId).updateHeartbeat();
                }
                out.writeObject(new Message(Message.Type.TASK_SUCCESS, "ACK", null, null));
            }
            // NOVO: Recebendo a conclusão da tarefa do Worker
            else if (message.getType() == Message.Type.TASK_COMPLETED) {
                Task completedTask = message.getTask();
                System.out.println("-> ESTADO GLOBAL ATUALIZADO: Tarefa '" + completedTask.getDescription() + "' foi CONCLUÍDA!");
                // O estado já vem atualizado do worker, mas poderíamos acessar o globalTasks aqui se precisasse
                out.writeObject(new Message(Message.Type.TASK_SUCCESS, "ACK de Conclusão", null, null));
            }
            

        } catch (Exception e) {
            // Logs suprimidos para limpeza do console
        }
    }
}