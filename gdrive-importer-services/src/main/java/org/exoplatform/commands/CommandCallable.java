package org.exoplatform.commands;

import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.component.RequestLifeCycle;

import java.util.concurrent.Callable;

public class CommandCallable implements Callable<Command>{

    private final AbstractCommand command;
    private final ExoContainer container;

    public CommandCallable(AbstractCommand command, ExoContainer container) {
        this.command = command;
        this.container = container;
    }

    @Override
    public Command call() throws Exception {
        ExoContainerContext.setCurrentContainer(container);
        RequestLifeCycle.begin(container);
        try {
            command.exec();
        } finally {
            RequestLifeCycle.end();
        }
        return command;
    }
}
