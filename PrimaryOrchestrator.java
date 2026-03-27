import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class PrimaryOrchestrator {
    private static final int PORT = 5000;
    private static final String MULTICAST_IP = "230.0.0.0";
    private static final int MULTICAST_PORT = 5005;
    
    private LamportClock clock;
    private ExecutorService threadPool;
    private AuthService authService;
    private ConcurrentHashMap<String, WorkerInfo> activeWorkers;
    private ConcurrentHashMap<String, Task> globalTasks;
    private TaskDistributor taskDistributor;

    // Construtor padrão
    public PrimaryOrchestrator() {
        this(null);
    }

    // Construtor de Recuperação (usado pelo Backup no Failover)
    public PrimaryOrchestrator(ConcurrentHashMap<String, Task> recoveredTasks) {
        this.clock = new LamportClock();
        this.threadPool = Executors.newCachedThreadPool();
        this.authService = new AuthService();
        this.activeWorkers = new ConcurrentHashMap<>();
        this.globalTasks = recoveredTasks != null ? recoveredTasks : new ConcurrentHashMap<>();
        this.taskDistributor = new TaskDistributor(activeWorkers, globalTasks);
    }

    public void start() {
        System.out.println("Orquestrador Principal na porta " + PORT + " | Tarefas recuperadas: " + globalTasks.size());
        startHealthCheck(); 
        startStateSynchronization(); // NOVO: Inicia o envio de dados via Multicast

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                threadPool.execute(new ConnectionHandler(clientSocket, clock, authService, activeWorkers, taskDistributor));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void startHealthCheck() {
        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
            long now = System.currentTimeMillis();
            activeWorkers.entrySet().removeIf(entry -> {
                boolean isDead = (now - entry.getValue().getLastHeartbeat()) > 15000; 
                if (isDead) {
                    System.out.println("ALERTA: Worker " + entry.getKey() + " falhou!");
                    
                    // NOVO: Redistribuição de tarefas não concluídas
                    for (Task t : globalTasks.values()) {
                        if (t.getStatus() == Task.Status.ACTIVE && entry.getKey().equals(t.getAssignedWorkerId())) {
                            System.out.println("-> Redistribuindo tarefa interrompida: " + t.getDescription());
                            t.setStatus(Task.Status.PENDING);
                            t.setAssignedWorkerId(null);
                            taskDistributor.dispatchTask(t);
                        }
                    }
                }
                return isDead;
            });
        }, 10, 5, TimeUnit.SECONDS); 
    }

    // NOVO: Sincronização de Estado via UDP Multicast
    private void startStateSynchronization() {
        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
            try (DatagramSocket socket = new DatagramSocket()) {
                InetAddress group = InetAddress.getByName(MULTICAST_IP);
                
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(baos);
                oos.writeObject(globalTasks); // Serializa o estado global das tarefas
                byte[] data = baos.toByteArray();
                
                DatagramPacket packet = new DatagramPacket(data, data.length, group, MULTICAST_PORT);
                socket.send(packet);
            } catch (Exception e) {
                // Erros de rede suprimidos para não poluir o terminal
            }
        }, 0, 3, TimeUnit.SECONDS); // Envia o estado a cada 3 segundos
    }

    public static void main(String[] args) {
        new PrimaryOrchestrator().start();
    }
}