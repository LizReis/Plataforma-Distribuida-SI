import java.util.concurrent.atomic.AtomicInteger;

public class LamportClock {
    private AtomicInteger counter;

    public LamportClock() {
        this.counter = new AtomicInteger(0);
    }

    // Incrementa o relógio em eventos locais ou de envio
    public int tick() {
        return counter.incrementAndGet();
    }

    // Atualiza o relógio ao receber uma mensagem de outro nó
    public void update(int receivedTimestamp) {
        int current;
        int next;
        do {
            current = counter.get();
            next = Math.max(current, receivedTimestamp) + 1;
        } while (!counter.compareAndSet(current, next));
    }

    public int getTime() {
        return counter.get();
    }
}