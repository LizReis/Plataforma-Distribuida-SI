package services;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import model.Message;
import model.Task;
import model.WorkerInfo;
import model.Message.Type;
import model.Task.Status;

public class TaskDistributor {
    private ConcurrentHashMap<String, WorkerInfo> activeWorkers;
    private ConcurrentHashMap<String, Task> globalTasks; // Mantém o estado global das tarefas
    private int currentIndex = 0; // Ponteiro do Round Robin

    public TaskDistributor(ConcurrentHashMap<String, WorkerInfo> activeWorkers, ConcurrentHashMap<String, Task> globalTasks) {
        this.activeWorkers = activeWorkers;
        this.globalTasks = globalTasks;
    }

    public synchronized void dispatchTask(Task task) {
        if (activeWorkers.isEmpty()) {
            System.out.println("ALERTA: Nenhum worker disponível para a tarefa " + task.getTaskId());
            // Em um sistema real, a tarefa ficaria numa fila aguardando. Aqui, deixaremos como PENDING.
            return;
        }

        // Lógica do Round Robin: pega a lista atualizada de workers e seleciona o próximo
        List<WorkerInfo> workers = new ArrayList<>(activeWorkers.values());
        if (currentIndex >= workers.size()) {
            currentIndex = 0; // Reinicia o ciclo
        }
        WorkerInfo chosenWorker = workers.get(currentIndex);
        currentIndex++;

        // NOVA LINHA: Associa a tarefa ao worker
        task.setAssignedWorkerId(chosenWorker.getWorkerId());

        task.setStatus(Task.Status.ACTIVE);
        globalTasks.put(task.getTaskId(), task);
        
        System.out.println("Distribuindo tarefa '" + task.getDescription() + "' para " + chosenWorker.getWorkerId() + " (Via Round Robin)");

        // Envia a tarefa para o worker selecionado em uma nova Thread
        new Thread(() -> sendTaskToWorker(chosenWorker, task)).start();
    }

    private void sendTaskToWorker(WorkerInfo worker, Task task) {
        try (Socket socket = new Socket(worker.getHost(), worker.getPort());
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {
             
             // ADICIONADO: 0 no final para satisfazer o construtor da Message
             Message msg = new Message(Message.Type.ASSIGN_TASK, null, null, task, 0);
             out.writeObject(msg);
             
        } catch (Exception e) {
             System.err.println("Falha ao enviar tarefa para " + worker.getWorkerId() + ". Redistribuição ocorrerá no tratamento de falhas.");
        }
    }
}