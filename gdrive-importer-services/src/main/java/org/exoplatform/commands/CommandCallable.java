package org.exoplatform.commands;

import java.util.concurrent.Callable;

public class CommandCallable implements Callable<Command>{

    private final AbstractCommand command;

    public CommandCallable(AbstractCommand command) {
        this.command = command;
    }

    @Override
    public Command call() throws Exception {
        command.exec();
        return command;
    }
}
