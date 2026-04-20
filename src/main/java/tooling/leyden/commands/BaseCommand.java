package tooling.leyden.commands;

import java.util.concurrent.atomic.AtomicBoolean;

import sun.misc.Signal;

public abstract class BaseCommand implements Runnable {

    private static final AtomicBoolean running = new AtomicBoolean(true);

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
