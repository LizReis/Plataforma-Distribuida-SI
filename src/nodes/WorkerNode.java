package nodes;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import model.Message;
import model.Task;
import model.Message.Type;
import model.Task.Status;
import services.LamportClock;

public class WorkerNode {
    private String workerId;
    private int workerPort;
    private static final String ORCHESTRATOR_HOST = "localhost";
    private static final int ORCHESTRATOR_PORT = 5000;
    private LamportClock clock;

    public WorkerNode(int port) {
        this.workerId = "WORKER-" + UUID.randomUUID().toString().substring(0, 6); // ID mais curto para facilitar a leitura
        this.workerPort = port;
        this.clock = new LamportClock();
    }

    public void start() {
        System.out.println("Iniciando " + workerId + " na porta local " + workerPort);
        registerWithOrchestrator();
        startHeartbeat();
        listenForTasks();
    }

    private void registerWithOrchestrator() {
        try (Socket socket = new Socket(ORCHESTRATOR_HOST, ORCHESTRATOR_PORT);
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

            clock.tick();
            String payload = workerId + ":" + workerPort;
            // ADICIONADO: clock.getTime()
            Message regReq = new Message(Message.Type.WORKER_REGISTER, payload, null, null, clock.getTime());
            out.writeObject(regReq);

            Message response = (Message) in.readObject();
            clock.update(response.getTimestamp());
            System.out.println("Registro no Orquestrador: " + response.getPayload());

        } catch (Exception e) {
            System.err.println("Erro ao registrar: " + e.getMessage());
        }
    }

    private void startHeartbeat() {
        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
            try (Socket socket = new Socket(ORCHESTRATOR_HOST, ORCHESTRATOR_PORT);
                 ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                 ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

                clock.tick();
                // ADICIONADO: clock.getTime()
                Message hbReq = new Message(Message.Type.HEARTBEAT, workerId, null, null, clock.getTime());
                out.writeObject(hbReq);
                Message response = (Message) in.readObject();
                clock.update(response.getTimestamp());

            } catch (Exception e) {
                System.err.println("Erro ao enviar heartbeat. O Orquestrador pode estar em Failover.");
                registerWithOrchestrator();
            }
        }, 5, 5, TimeUnit.SECONDS);
    }

    private void listenForTasks() {
        try (ServerSocket serverSocket = new ServerSocket(workerPort)) {
            System.out.println(workerId + " aguardando tarefas na porta " + workerPort + "...");
            while (true) {
                Socket orchSocket = serverSocket.accept();
                new Thread(() -> processAssignedTask(orchSocket)).start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void processAssignedTask(Socket orchSocket) {
        try (ObjectOutputStream out = new ObjectOutputStream(orchSocket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(orchSocket.getInputStream())) {
             
             Message message = (Message) in.readObject();
             clock.update(message.getTimestamp());
             if (message.getType() == Message.Type.ASSIGN_TASK) {
                 Task task = message.getTask();
                 System.out.println("[" + workerId + "] Iniciando processamento: " + task.getDescription());
                 
                 // Simula processamento pesado (ex: 4 segundos)
                 Thread.sleep(4000); 
                 
                 System.out.println("[" + workerId + "] Finalizado: " + task.getDescription());
                 task.setStatus(Task.Status.COMPLETED);
                 
                 // Reporta conclusão ao Orquestrador
                 reportTaskCompletion(task);
             }
        } catch (Exception e) {
             System.err.println("Erro ao processar tarefa: " + e.getMessage());
        }
    }

    private void reportTaskCompletion(Task task) {
        try (Socket socket = new Socket(ORCHESTRATOR_HOST, ORCHESTRATOR_PORT);
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

            clock.tick();
            Message msg = new Message(Message.Type.TASK_COMPLETED, null, null, task, clock.getTime());
            out.writeObject(msg);

            Message response = (Message) in.readObject();
            clock.update(response.getTimestamp());

        } catch (Exception e) {
            System.err.println("Erro ao reportar conclusão da tarefa: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        // Exemplo: Worker iniciando na porta 5001. Você pode rodar várias instâncias mudando a porta.
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 5001;
        new WorkerNode(port).start();
    }
}