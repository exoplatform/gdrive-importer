package org.exoplatform.rest;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.ecm.connector.clouddrives.DriveServiceLocator;
import org.exoplatform.gdrive.GDriveCloneService;
import org.exoplatform.gdrive.GoogleUser;
import org.exoplatform.rest.utils.*;
import org.exoplatform.services.cms.clouddrives.*;
import org.exoplatform.services.cms.clouddrives.jcr.JCRLocalCloudDrive;
import org.exoplatform.services.cms.clouddrives.jcr.NodeFinder;
import org.exoplatform.services.cms.drives.DriveData;
import org.exoplatform.services.cms.drives.ManageDriveService;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.rest.resource.ResourceContainer;
import org.exoplatform.services.security.ConversationState;

import javax.annotation.security.RolesAllowed;
import javax.jcr.*;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Path("/copygdrive/clone")
@Tag(name = "/copgdrive/connect", description = "Manages copy gdrive extension connection")
@Produces(MediaType.APPLICATION_JSON)
public class CopyGDriveRestConnect implements ResourceContainer {

    /** The Constant LOG. */
    protected static final Log LOG                    = ExoLogger.getLogger(CopyGDriveRestConnect.class);


    /** The Constant INIT_COOKIE. */
    public static final String    INIT_COOKIE            = "cpgdrive-cloud-drive-init-id";
    /** The Constant CONNECT_COOKIE. */
    public static final String    CONNECT_COOKIE         = "cpgdrive-cloud-drive-connect-id";
    /**
     * Init cookie expire time in seconds.
     */
    public static final int       INIT_COOKIE_EXPIRE     = 300;                                      // 5min

    /** The authenticated. */
    protected final Map<UUID, GoogleUser> authenticated = new ConcurrentHashMap<UUID, GoogleUser>();

    /** The timeline. */
    protected final Map<UUID, Long>             timeline      = new ConcurrentHashMap<UUID, Long>();

    /** The initiated. */
    protected final Map<UUID, ConnectInit>      initiated     = new ConcurrentHashMap<UUID, ConnectInit>();
    /**
     * Connections in progress.
     */
    protected final Map<String, CloneProcess> active        = new ConcurrentHashMap<String, CloneProcess>();

    /**
     * Connect cookie expire time in seconds.
     */
    public static final int CONNECT_COOKIE_EXPIRE = 90;                                       // 1.5min

    /**
     * The Constant INIT_COOKIE_PATH.
     */
    public static final String INIT_COOKIE_PATH = "/portal/rest/copygdrive/clone";

    /**
     * The locator.
     */
    protected final DriveServiceLocator locator;

    /** The cloud drives. */
    protected final CloudDriveService cloudDrives;

    /** The session providers. */
    protected final SessionProviderService sessionProvider;

    /** The jcr service. */
    protected final RepositoryService repositoryService;

    protected final GDriveCloneService gDriveCloneService;

    protected final ManageDriveService manageDriveService;

    /** The finder. */
    protected final NodeFinder nodeFinder;

    public CopyGDriveRestConnect(DriveServiceLocator locator, CloudDriveService cloudDrives, SessionProviderService sessionProvider, RepositoryService repositoryService, GDriveCloneService gDriveCloneService, ManageDriveService manageDriveService, NodeFinder finder) {
        this.locator = locator;
        this.cloudDrives = cloudDrives;
        this.sessionProvider = sessionProvider;
        this.repositoryService = repositoryService;
        this.gDriveCloneService = gDriveCloneService;
        this.manageDriveService = manageDriveService;
        this.nodeFinder = finder;
    }

    @GET
    @Path("/{providerid}/")
    @Produces(MediaType.TEXT_HTML)
    public Response authenticateUser(@Context UriInfo uriInfo,
                             @PathParam("providerid") String providerId,
                             @QueryParam("code") String code,
                             @QueryParam("state") String state,
                             @QueryParam("error") String error,
                             @QueryParam("error_description") String errorDescription,
                             @QueryParam("hostName") String hostName,
                             @CookieParam("JSESSIONID") Cookie jsessionsId,
                             @CookieParam("JSESSIONIDSSO") Cookie jsessionsIdSSO,
                             @CookieParam(INIT_COOKIE) Cookie initId) {

        CloneResponse resp = new CloneResponse();
        URI requestURI = uriInfo.getRequestUri();
        StringBuilder serverHostBuilder = new StringBuilder();
        serverHostBuilder.append(requestURI.getScheme());
        serverHostBuilder.append("://");
        if (hostName != null && hostName.length() > 0) {
            serverHostBuilder.append(hostName);
        } else {
            serverHostBuilder.append(requestURI.getHost());
            int serverPort = requestURI.getPort();
            if (serverPort >= 0 && serverPort != 80 && serverPort != 443) {
                serverHostBuilder.append(':');
                serverHostBuilder.append(serverPort);
            }
        }
        String serverURL = serverHostBuilder.toString();

        // TODO implement CSRF handing in state parameter

        String requestHost = uriInfo.getRequestUri().getHost();
        if (state != null) {
            // state contains repoName set by the provider
            if (locator.isRedirect(requestHost)) {
                // need redirect to actual service URL
                resp.location(locator.getServiceLink(state, uriInfo.getRequestUri().toString()));
                return resp.status(Response.Status.MOVED_PERMANENTLY).build(); // redirect
            }
        }
        String baseHost = locator.getServiceHost(requestHost);
        if (initId != null) {
            try {
                UUID iid = UUID.fromString(initId.getValue());
                ConnectInit connect = initiated.remove(iid);
                timeline.remove(iid);

                if (connect != null) {
                    CloudProvider provider = connect.getProvider();
                    if (provider.getId().equals(providerId)) {
                        // TODO handle auth errors by provider code
                        if (error == null) {
                            // it's the same as initiated request
                            if (code != null) {
                                try {
                                    Map<String, String> params = new HashMap<String, String>();
                                    params.put(CloudDriveConnector.OAUTH2_CODE, code);
                                    params.put(CloudDriveConnector.OAUTH2_STATE, state);
                                    params.put(CloudDriveConnector.OAUTH2_SERVER_URL, serverURL);
                                    CloudUser user = cloudDrives.authenticate(provider, params);

                                    UUID connectId = generateId(user.getEmail() + code);
                                    authenticated.put(connectId, (GoogleUser) user);
                                    timeline.put(connectId, System.currentTimeMillis() + (CONNECT_COOKIE_EXPIRE * 1000) + 5000);

                                    // This cookie will be set on host of initial request (i.e. on
                                    // host of calling app)
                                    resp.cookie(CONNECT_COOKIE,
                                            connectId.toString(),
                                            "/",
                                            connect.getHost(),
                                            "Cloud Drive connect ID",
                                            CONNECT_COOKIE_EXPIRE,
                                            false);

                                    // reset it by expire time = 0
                                    resp.cookie(INIT_COOKIE, iid.toString(), INIT_COOKIE_PATH, baseHost, "Cloud Drive init ID", 0, false);

                                    resp.entity("<!doctype html><html><head><script type='text/javascript'> window.close();</script></head><body><div id='messageString'>Connecting to "
                                            + user.getServiceName() + "</div></body></html>");

                                    return resp.ok().build();
                                } catch (CloudDriveException e) {
                                    LOG.warn("Error authenticating user to access " + provider.getName(), e);
                                    return resp.authError("Authentication error on " + provider.getName(),
                                                    connect.getHost(),
                                                    provider.getName(),
                                                    iid.toString(),
                                                    baseHost)
                                            // TODO UNAUTHORIZED ?
                                            .status(Response.Status.BAD_REQUEST)
                                            .build();
                                }
                            } else {
                                LOG.warn("Code required for " + provider.getName());
                                return resp.authError("Code required for " + provider.getName(),
                                                connect.getHost(),
                                                provider.getName(),
                                                iid.toString(),
                                                baseHost)
                                        .status(Response.Status.BAD_REQUEST)
                                        .build();
                            }
                        } else {
                            // we have an error from provider
                            LOG.warn(provider.getName() + " error: " + error + ". error_description: " + errorDescription);
                            StringBuilder errorMsg = new StringBuilder();
                            errorMsg.append(provider.getErrorMessage(error, errorDescription));
                            return resp.authError(errorMsg.toString(), connect.getHost(), provider.getName(), iid.toString(), baseHost)
                                    .status(Response.Status.BAD_REQUEST)
                                    .build();
                        }
                    } else {
                        LOG.error("Authentication was not initiated for " + providerId + " but request to " + provider.getId()
                                + " recorded with id " + initId);
                        return resp.authError("Authentication not initiated to " + provider.getName(),
                                        connect.getHost(),
                                        provider.getName(),
                                        iid.toString(),
                                        baseHost)
                                .status(Response.Status.INTERNAL_SERVER_ERROR)
                                .build();
                    }
                } else {
                    LOG.warn("Authentication not initiated for " + providerId + " and id " + initId);
                    return resp.authError("Authentication request expired. Try again later.", baseHost, null, iid.toString(), baseHost)
                            .status(Response.Status.BAD_REQUEST)
                            .build();
                }
            } catch (Throwable e) {
                LOG.error("Error initializing drive provider by id " + providerId, e);
                return resp.authError("Error initializing drive provider", baseHost).status(Response.Status.INTERNAL_SERVER_ERROR).build();
            }
        } else {
            LOG.warn("Authentication id not set for provider id " + providerId + " and key " + code);
            return resp.authError("Authentication not initiated or expired. Try again later.", baseHost)
                    .status(Response.Status.BAD_REQUEST)
                    .build();
        }
    }

    @GET
    @Path("/init/{providerid}/")
    @RolesAllowed("users")
    public Response userInit(@Context UriInfo uriInfo,
                             @PathParam("providerid") String providerId,
                             @CookieParam("JSESSIONID") Cookie jsessionsId,
                             @CookieParam("JSESSIONIDSSO") Cookie jsessionsIdSSO) {

        CloneResponse resp = new CloneResponse();
        try {
            CloudProvider provider = cloudDrives.getProvider(providerId);
            ConversationState convo = ConversationState.getCurrent();
            if (convo != null) {
                String localUser = convo.getIdentity().getUserId();
                String host = locator.getServiceHost(uriInfo.getRequestUri().getHost());

                UUID initId = generateId(localUser);
                initiated.put(initId, new ConnectInit(localUser, provider, host));
                timeline.put(initId, System.currentTimeMillis() + (INIT_COOKIE_EXPIRE * 1000) + 5000);

                resp.cookie(INIT_COOKIE, initId.toString(), INIT_COOKIE_PATH, host, "Cloud Drive init ID", INIT_COOKIE_EXPIRE, false);
                return resp.entity(provider).ok().build();
            } else {
                LOG.warn("ConversationState not set to initialize connect to " + provider.getName());
                return resp.error("User not authenticated to connect " + provider.getName()).status(Response.Status.UNAUTHORIZED).build();
            }
        } catch (ProviderNotAvailableException e) {
            LOG.warn("Provider not found for id '" + providerId + "'", e);
            return resp.error("Provider not found.").status(Response.Status.BAD_REQUEST).build();
        } catch (Throwable e) {
            LOG.error("Error initializing user request for drive provider " + providerId, e);
            return resp.error("Error initializing user request.").status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @POST
    @RolesAllowed("users")
    public Response cloneStart(@Context UriInfo uriInfo,
                               @FormParam("workspace") String workspace,
                               @FormParam("path") String path,
                               @FormParam("folderOrFileId") String folderOrFileId,
                               @FormParam("groupId") String groupId,
                               @CookieParam("JSESSIONID") Cookie jsessionsId,
                               @CookieParam("JSESSIONIDSSO") Cookie jsessionsIdSSO,
                               @CookieParam(CONNECT_COOKIE) Cookie connectId) {

        CloneResponse resp = new CloneResponse();
        String host = locator.getServiceHost(uriInfo.getRequestUri().getHost());
        if (connectId == null) {
            LOG.warn("Connect ID not set");
            resp.error("Connection not initiated properly.").status(Response.Status.BAD_REQUEST);
            return resp.build();
        }
        UUID cid = UUID.fromString(connectId.getValue());
        GoogleUser user = authenticated.remove(cid);
        timeline.remove(cid);
        Node driveNode = null;
        Session userSession = null;
        if (user == null) {
            LOG.warn("User not authenticated for connectId " + connectId);
            resp.connectError("User not authenticated or a clone process is already in progress.", cid.toString(), host).status(Response.Status.BAD_REQUEST);
            return resp.build();
        }
        if (workspace == null) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Null workspace.").build();
        }
        if (path == null) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Null path.").build();
        }
        try {
            ConversationState state = ConversationState.getCurrent();
            if (state == null) {
                LOG.error("Error connect drive for user " + user.getEmail() + ". User identity not set: ConversationState.getCurrent() is null");
                return resp.connectError("User identity not set.", cid.toString(), host).status(Response.Status.INTERNAL_SERVER_ERROR)
                        .build();
            }
            SessionProvider sessionProvider = this.sessionProvider.getSessionProvider(null);
            userSession = sessionProvider.getSession(workspace, repositoryService.getCurrentRepository());
            Item item = nodeFinder.findItem(userSession, path);
            if(!item.isNode()) {
                LOG.warn("Item " + workspace + ":" + path + " not a node.");
                resp.connectError("Not a node.", cid.toString(), host).status(Response.Status.PRECONDITION_FAILED);
                return resp.build();
            }
            Node targetNode = (Node) item;
            String name;
            DriveData drive = manageDriveService.getDriveByName(user.createDriveTitle());
            if (drive != null && targetNode.hasNode(JCRLocalCloudDrive.cleanName(user.createDriveTitle()))) {
                // drive already exists - it's re-connect to update access
                // keys
                driveNode = (Node) userSession.getItem(drive.getHomePath());
                targetNode = driveNode.getParent();
                name = driveNode.getName();
            } else {
                name = JCRLocalCloudDrive.cleanName(user.createDriveTitle());
            }
            String processId = processId(workspace, targetNode.getPath(), name);
            resp.cookie(CONNECT_COOKIE, cid.toString(), "/", host, "Cloud Drive connect ID", 0, false);
            CloneProcess cloneProcess = active.get(processId);
            if (cloneProcess != null && cloneProcess.getProcess().getCommandState() == 0) {
                active.remove(processId);
            }
            if (cloneProcess == null || cloneProcess != null && cloneProcess.getProcess().getCommandState() == 0) {
                if (driveNode == null) {
                    try {
                        driveNode = targetNode.addNode(name);
                    }catch (PathNotFoundException exception) {
                        try {
                            driveNode = targetNode.addNode(name, JCRLocalCloudDrive.NT_FOLDER);
                            targetNode.save();
                        } catch (RepositoryException e) {
                            rollback(targetNode, null);
                            LOG.error("Error creating node for the drive of user " + user.getEmail() + ". Cannot create node under "
                                    + path, e);
                            return resp.connectError("Error creating node for the drive: storage error.", cid.toString(), host)
                                    .status(Response.Status.INTERNAL_SERVER_ERROR)
                                    .build();
                        }
                    }
                }
                resp.serviceUrl(uriInfo.getRequestUriBuilder()
                        .queryParam("workspace", workspace)
                        .queryParam("path", driveNode.getPath())
                        .build(new Object[0])
                        .toASCIIString());

                try {
                    CloneCommand command = new CloneCommand();
                    cloneProcess = new CloneProcess(user, driveNode, folderOrFileId, groupId, command);
                    ClonedDrive clonedDrive = new ClonedDrive();
                    active.put(processId, cloneProcess);
                    DriveData driveData = cloneProcess.getDrive();
                    clonedDrive.setName(name);
                    if(driveData != null) {
                        resp.status(Response.Status.CREATED);
                        clonedDrive.setCloned(true);
                        clonedDrive.setDriveData(driveData);
                        clonedDrive.setLinksProcessed(cloneProcess.getProcess().linksProcessed());
                        resp.drive(clonedDrive);
                        active.remove(processId);

                    } else {
                        clonedDrive.setWorkspace(workspace);
                        clonedDrive.setCloned(false);
                        resp.status(Response.Status.ACCEPTED);
                        clonedDrive.setLinksProcessed(cloneProcess.getProcess().linksProcessed());
                        resp.progress(cloneProcess.getProgress());
                        resp.drive(clonedDrive);
                    }
                } catch (Exception e) {
                    LOG.error("Cloning process terminated accidentally, you may not find all you cloned files", e);
                    active.remove(processId);
                    resp.error("error!");
                }
            } else {
                LOG.warn("Clone already posted and currently in progress.");
                resp.error("Clone already posted and currently in progress!");
                resp.status(Response.Status.CONFLICT);
                LOG.info(cloneProcess.getProcess().getAvailable());
            }

        } catch (RepositoryException | CloudDriveException e) {
            LOG.error("error", e);
            resp.error("error!");
        } catch (Exception e) {
            LOG.error("error", e);
            resp.error("error!");
        } finally {
            if (userSession != null) {
                userSession.logout();
            }
        }

        return resp.build();
    }

    @GET
    @RolesAllowed("users")
    public Response checkCloneState(@Context UriInfo uriInfo,
                                 @QueryParam("workspace") String workspace,
                                 @QueryParam("path") String path) {
        CloneResponse response = new CloneResponse();
        response.serviceUrl(uriInfo.getRequestUri().toASCIIString());
        String processId = processId(workspace, path);
        try {
            CloneProcess cloneProcess = active.get(processId);
            if(cloneProcess != null) {
                response.progress(cloneProcess.getProgress());
                try {
                cloneProcess.getLock().lock();
                if(cloneProcess.getError() != null) {
                    response.error(cloneProcess.getError()).status(Response.Status.INTERNAL_SERVER_ERROR);
                } else {
                    ClonedDrive clonedDrive = new ClonedDrive();
                    if(cloneProcess.getProcess().isDone()){
                        clonedDrive.setCloned(true);
                        clonedDrive.setDriveData(cloneProcess.getDrive());
                        clonedDrive.setName(cloneProcess.getDrive().getName());
                        clonedDrive.setWorkspace(cloneProcess.getDrive().getWorkspace());
                        clonedDrive.setLinksProcessed(cloneProcess.getProcess().linksProcessed());
                        response.drive(clonedDrive);
                        response.status(Response.Status.CREATED);
                    } else {
                        clonedDrive.setCloned(false);
                        clonedDrive.setLinksProcessed(cloneProcess.getProcess().linksProcessed());
                        clonedDrive.setName(cloneProcess.getProcess().getName());
                        clonedDrive.setWorkspace(workspace);
                        response.drive(clonedDrive);
                        response.status(Response.Status.ACCEPTED);
                    }
                }
                }catch (RepositoryException e) {
                    LOG.error("error occurred", e);
                    response.error("error occurred");
                } finally {
                  cloneProcess.getLock().unlock();
                }
            }
        } catch (Exception e) {
            LOG.error("error occurred", e);
            response.error("error occurred");
        }
        return response.build();
    }

    private UUID generateId(String name) {
        StringBuilder s = new StringBuilder();
        s.append(name);
        s.append(System.currentTimeMillis());
        s.append(new Random().nextLong());
        return UUID.nameUUIDFromBytes(s.toString().getBytes());
    }

    private String processId(String workspace, String parentPath, String driveName) {
        return workspace + ":" + parentPath + "/" + driveName;
    }

    private String processId(String workspace, String nodePath) {
        return workspace + ":" + nodePath;
    }

    private boolean rollback(Node targetNode, Node driveNode) {
        try {
            if (targetNode != null) {
                if (driveNode != null) {
                    driveNode.remove();
                    targetNode.save();
                }

                targetNode.refresh(false);
                return true;
            }
        } catch (Throwable e) {
            LOG.warn("Error rolling back the user node: " + e.getMessage(), e);
        }
        return false;
    }
}
