package org.exoplatform.commands;

import java.util.concurrent.ExecutionException;

public class AbstractCommand implements Command, CommandProgress{
    @Override
    public int getProgress() {
        return 0;
    }

    @Override
    public boolean isDone() {
        return false;
    }

    @Override
    public long getStartTime() {
        return 0;
    }

    @Override
    public long getFinishTime() {
        return 0;
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public boolean hasChanges() {
        return false;
    }

    @Override
    public void await() throws ExecutionException, InterruptedException {

    }

    @Override
    public long getComplete() {
        return 0;
    }

    @Override
    public long getAvailable() {
        return 0;
    }

    @Override
    public int getAttempts() {
        return 0;
    }

    public void exec() {
    }
}
