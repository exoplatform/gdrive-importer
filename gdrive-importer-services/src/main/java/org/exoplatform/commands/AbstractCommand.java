package org.exoplatform.commands;

import javax.jcr.RepositoryException;
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
    public String getName() throws RepositoryException {
        return null;
    }

    @Override
    public boolean linksProcessed() {
        return false;
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

    public void exec() throws Exception {}
    public void reset () {}
}
