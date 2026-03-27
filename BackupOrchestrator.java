import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.concurrent.ConcurrentHashMap;

public class BackupOrchestrator {
    private static final String MULTICAST_IP = "230.0.0.0";
    private static final int MULTICAST_PORT = 5005;
    private static final int TIMEOUT_MS = 10000; // 10 segundos sem receber pacotes = Failover
    
    private ConcurrentHashMap<String, Task> replicatedTasks = new ConcurrentHashMap<>();
    private long lastSyncTime = System.currentTimeMillis();
    private boolean isPrimaryNow = false;

    public void start() {
        System.out.println("Orquestrador Secundário (Backup) iniciado. Escutando Multicast...");
        
        // Inicia a thread que recebe os pacotes UDP
        new Thread(this::listenToPrimary).start();
        
        // Monitora ativamente se o Principal morreu
        monitorPrimaryHealth();
    }

    @SuppressWarnings("deprecation")
    private void listenToPrimary() {
        try {
            MulticastSocket socket = new MulticastSocket(MULTICAST_PORT);
            InetAddress group = InetAddress.getByName(MULTICAST_IP);
            socket.joinGroup(group);

            byte[] buffer = new byte[65535]; 
            
            while (!isPrimaryNow) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet); // Bloqueia até receber o pacote do Principal
                
                lastSyncTime = System.currentTimeMillis();
                
                ByteArrayInputStream bais = new ByteArrayInputStream(packet.getData(), 0, packet.getLength());
                ObjectInputStream ois = new ObjectInputStream(bais);
                
                @SuppressWarnings("unchecked")
                ConcurrentHashMap<String, Task> state = (ConcurrentHashMap<String, Task>) ois.readObject();
                replicatedTasks = state;
            }
            socket.leaveGroup(group);
            socket.close();
        } catch (Exception e) {
            if (!isPrimaryNow) e.printStackTrace();
        }
    }

    private void monitorPrimaryHealth() {
        while (!isPrimaryNow) {
            try {
                Thread.sleep(2000); // Checa a cada 2 segundos
                if (System.currentTimeMillis() - lastSyncTime > TIMEOUT_MS) {
                    System.out.println("\n[FAILOVER] Orquestrador Principal parou de responder!");
                    System.out.println("[FAILOVER] Assumindo o papel principal...");
                    isPrimaryNow = true;
                    takeoverAsPrimary();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void takeoverAsPrimary() {
        // Inicia um novo Orquestrador Principal, injetando o estado das tarefas que foram salvas via Multicast
        PrimaryOrchestrator newPrimary = new PrimaryOrchestrator(replicatedTasks);
        newPrimary.start();
    }

    public static void main(String[] args) {
        new BackupOrchestrator().start();
    }
}