package org.exoplatform.commands;

import java.util.concurrent.ExecutionException;

public interface Command {

    final int COMPLETE = 100;


    public int getProgress();

    public boolean isDone();

    public long getStartTime();

    long getFinishTime();

    String getName();

    boolean hasChanges();

    void await() throws ExecutionException, InterruptedException;

}
