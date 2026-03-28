package nodes;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.UUID;

import model.Message;
import model.Task;
import model.Message.Type;
import services.LamportClock;

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
            Message authReq = new Message(Message.Type.AUTH_REQUEST, username + ":" + password, null, null, clock.getTime());
            out.writeObject(authReq);

            Message response = (Message) in.readObject();
            clock.update(response.getTimestamp()); // CORREÇÃO: Atualiza o relógio com a resposta do servidor

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

    public String submitTask(String description) { // Alterado para retornar o taskId
        if (authToken == null) {
            System.out.println("Você precisa estar logado para submeter tarefas.");
            return null;
        }

        try (Socket socket = new Socket(HOST, PORT);
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

            clock.tick();
            String taskId = UUID.randomUUID().toString();
            Task newTask = new Task(taskId, authToken, description, clock.getTime());

            Message submitReq = new Message(Message.Type.SUBMIT_TASK, null, authToken, newTask, clock.getTime());
            out.writeObject(submitReq);

            Message response = (Message) in.readObject();
            clock.update(response.getTimestamp()); // CORREÇÃO: Atualiza o relógio

            System.out.println("Resposta do Orquestrador: " + response.getPayload() + " | ID da Tarefa: " + taskId);
            return taskId;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // CORREÇÃO: Método adicionado para cumprir o requisito de consulta do PDF
    public void checkTaskStatus(String taskId) {
        if (authToken == null || taskId == null) return;

        try (Socket socket = new Socket(HOST, PORT);
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

            clock.tick();
            Message req = new Message(Message.Type.CHECK_STATUS, taskId, authToken, null, clock.getTime());
            out.writeObject(req);

            Message response = (Message) in.readObject();
            clock.update(response.getTimestamp()); // CORREÇÃO: Atualiza o relógio
            
            System.out.println("Status da Tarefa [" + taskId + "]: " + response.getPayload());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws InterruptedException {
        ClientNode client = new ClientNode();
        
        System.out.println("Tentando enviar tarefa sem login...");
        client.submitTask("Processar imagem");

        System.out.println("\nTentando fazer login...");
        client.authenticate("alexa", "senha123");

        System.out.println("\nEnviando tarefa autenticada...");
        String taskId = client.submitTask("Renderizar modelo 3D");

        // Simula o cliente aguardando e consultando o status da tarefa
        System.out.println("\nConsultando status da tarefa em tempo real...");
        Thread.sleep(1000);
        client.checkTaskStatus(taskId);
        
        Thread.sleep(5000); // Aguarda o worker terminar (leva 4s)
        client.checkTaskStatus(taskId);
    }
}