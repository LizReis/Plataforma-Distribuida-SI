import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.UUID;

public class ClientNode {
    private static final String HOST = "localhost";
    private static final int PORT = 5000;
    private LamportClock clock;
    private String authToken = null;

    public ClientNode() {
        this.clock = new LamportClock();
    }

    public void authenticate(String username, String password) {
        try (Socket socket = new Socket(HOST, PORT);
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

            clock.tick();
            // ADICIONADO: clock.getTime() no final
            Message authReq = new Message(Message.Type.AUTH_REQUEST, username + ":" + password, null, null, clock.getTime());
            out.writeObject(authReq);

            Message response = (Message) in.readObject();
            if (response.getType() == Message.Type.AUTH_SUCCESS) {
                this.authToken = response.getToken();
                System.out.println("Login com sucesso! Token: " + authToken);
            } else {
                System.out.println("Erro no login: " + response.getPayload());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void submitTask(String description) {
        if (authToken == null) {
            System.out.println("Você precisa estar logado para submeter tarefas.");
            return;
        }

        try (Socket socket = new Socket(HOST, PORT);
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

            clock.tick();
            String taskId = UUID.randomUUID().toString();
            Task newTask = new Task(taskId, authToken, description, clock.getTime());

            // ADICIONADO: clock.getTime() no final
            Message submitReq = new Message(Message.Type.SUBMIT_TASK, null, authToken, newTask, clock.getTime());
            out.writeObject(submitReq);

            Message response = (Message) in.readObject();
            System.out.println("Resposta do Orquestrador: " + response.getPayload());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    

    public static void main(String[] args) {
        ClientNode client = new ClientNode();
        
        System.out.println("Tentando enviar tarefa sem login...");
        client.submitTask("Processar imagem");

        System.out.println("\nTentando fazer login...");
        client.authenticate("alexa", "senha123");

        System.out.println("\nEnviando tarefa autenticada...");
        client.submitTask("Renderizar modelo 3D");
    }
}