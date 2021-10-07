package org.exoplatform.rest.utils;

import org.exoplatform.commands.AbstractCommand;
import org.exoplatform.commands.Command;
import org.exoplatform.commands.CommandCallable;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.gdrive.GDriveCloneService;
import org.exoplatform.gdrive.GoogleUser;
import org.exoplatform.services.cms.drives.DriveData;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.core.NodetypeConstant;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class CloneCommand extends AbstractCommand {

    private static final Log LOG = ExoLogger.getLogger(GDriveCloneService.class);


    private Map<String, Set<String>> cloned  = new HashMap<String, Set<String>>();
    protected Future<Command>                async;

    private DriveData driveData = null;

    private GoogleUser user;
    private Node driveNode;
    private String folderOrFileId;
    private String groupId;
    private String driveNodeUUID;
    private String workspace;

    private boolean finished = false;
    private int commandState = 0;

    private static final ExecutorService executorService = Executors.newSingleThreadExecutor();

    private final GDriveCloneService gDriveCloneService;

    public CloneCommand() {
        this.gDriveCloneService = CommonsUtils.getService(GDriveCloneService.class);
    }

    protected void process() {
        commandState = 1;
        driveData = gDriveCloneService.cloneCloudDrive(user, workspace, driveNodeUUID, folderOrFileId, groupId);
    }

    @Override
    public boolean linksProcessed () {
        return gDriveCloneService.isLinksProcessed();
    }

    @Override
    public void exec(){
        reset();
        process();
        finished = true;
        commandState = 0;
    }
    @Override
    public void reset() {
        finished = false;
        gDriveCloneService.resetAvailable();
        gDriveCloneService.setClonedFileNumber(0);
        gDriveCloneService.setLinksProcessed(false);
    }

    @Override
    public int getProgress() {
        return  Math.round((getComplete() * 100f) / getAvailable());
    }

    @Override
    public long getComplete() {
        return gDriveCloneService.getClonedFileNumber();
    }

    @Override
    public long getAvailable() {
        return gDriveCloneService.getAvailable();
    }

    @Override
    public boolean isDone() {
        return commandState == 0 && finished == true && driveData != null && linksProcessed();
    }

    public Future<Command> start(GoogleUser user, Node driveNode, String folderOrFileId, String groupId) throws RepositoryException {
        this.user = user;
        if (driveNode.canAddMixin(NodetypeConstant.MIX_REFERENCEABLE)) {
            driveNode.addMixin(NodetypeConstant.MIX_REFERENCEABLE);
        }
        ExoContainer container = ExoContainerContext.getCurrentContainer();
        driveNode.getSession().save();
        this.driveNodeUUID = driveNode.getUUID();
        this.driveNode = driveNode;
        this.workspace = driveNode.getSession().getWorkspace().getName();
        this.folderOrFileId = folderOrFileId;
        this.groupId = groupId;
        return async = executorService.submit(new CommandCallable(this, container));
    }

    @Override
    public String getName() throws RepositoryException {
        return  driveNode.getName();
    }

    public DriveData getDrive() {
        return this.driveData;
    }

    public int getCommandState() {
        return commandState;
    }
}
