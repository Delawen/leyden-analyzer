package tooling.leyden.commands;

import sun.misc.Signal;

import java.util.concurrent.atomic.AtomicBoolean;

public abstract class BaseCommand  implements Runnable {

    private static AtomicBoolean running = new AtomicBoolean(true);

    public final void run() {
        setupHandle();
        execution();
    }

    protected void setupHandle() {
        Signal.handle(new Signal("INT"), signal -> {
            running.set(false);
            System.out.println(">>>> Interrupted by Ctrl+C <<<<");
        });
        running.set(true);
    }

    public abstract void execution();

    protected boolean isRunning() {
        return running.get();
    }
}
