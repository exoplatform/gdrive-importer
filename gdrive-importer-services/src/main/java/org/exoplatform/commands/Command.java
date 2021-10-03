package org.exoplatform.commands;

import javax.jcr.RepositoryException;

public interface Command {

    final int COMPLETE = 100;

    public int getProgress();

    public boolean isDone();

    public long getStartTime();

    long getFinishTime();

    String getName() throws RepositoryException;
}
