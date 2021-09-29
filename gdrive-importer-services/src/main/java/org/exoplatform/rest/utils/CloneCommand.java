package org.exoplatform.rest.utils;

import org.exoplatform.commands.AbstractCommand;
import org.exoplatform.commands.Command;
import org.exoplatform.commands.CommandCallable;
import org.exoplatform.gdrive.GDriveCloneService;
import org.exoplatform.services.cms.clouddrives.CloudDriveException;
import org.exoplatform.services.cms.clouddrives.ThreadExecutor;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;

public class CloneCommand extends AbstractCommand {

    private static final Log LOG = ExoLogger.getLogger(GDriveCloneService.class);


    private Map<String, Set<String>> cloned  = new HashMap<String, Set<String>>();
    protected final ThreadExecutor workerExecutor      = ThreadExecutor.getInstance();
    protected Future<Command>                async;


    protected void process() {}

    @Override
    public void exec() {
        process();
    }

    public Future<Command> start() throws CloudDriveException {
        return async = workerExecutor.submit(new CommandCallable(this));
    }
}
