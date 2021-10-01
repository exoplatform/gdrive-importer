package org.exoplatform.gdrive;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpResponseException;
import com.google.api.client.util.DateTime;
import com.google.api.services.drive.model.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.common.usermodel.HyperlinkType;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Hyperlink;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xslf.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFHyperlink;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFHyperlinkRun;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRelation;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.model.ClonedGFile;
import org.exoplatform.services.cms.clouddrives.CloudDriveException;
import org.exoplatform.services.cms.clouddrives.CloudUser;
import org.exoplatform.services.cms.clouddrives.NotFoundException;
import org.exoplatform.services.cms.clouddrives.jcr.NodeFinder;
import org.exoplatform.services.cms.documents.DocumentService;
import org.exoplatform.services.cms.documents.VersionHistoryUtils;
import org.exoplatform.services.cms.drives.DriveData;
import org.exoplatform.services.cms.drives.ManageDriveService;
import org.exoplatform.services.cms.impl.Utils;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.core.NodetypeConstant;
import org.exoplatform.storage.ClonedGFileStorage;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.*;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.*;

public class GDriveCloneService {

    private static final Log LOG = ExoLogger.getLogger(GDriveCloneService.class);
    /**
     * The constant DRIVE_VIEWS.
     */
    private static final String      DRIVE_VIEWS                    = "List, Icons, Admin";

    /**
     * The constant DRIVE_ICON.
     */
    private static final String      DRIVE_ICON                     = "";

    /**
     * The constant DRIVE_VIEW_REFERENCES.
     */
    private static final boolean     DRIVE_VIEW_REFERENCES          = false;

    /**
     * The constant DRIVE_VIEW_NON_DOCUMENT.
     */
    private static final boolean     DRIVE_VIEW_NON_DOCUMENT        = false;

    /**
     * The constant DRIVE_VIEW_SIDE_BAR.
     */
    private static final boolean     DRIVE_VIEW_SIDE_BAR            = false;

    /**
     * The constant DRIVE_SHOW_HIDDEN_NODE.
     */
    private static final boolean     DRIVE_SHOW_HIDDEN_NODE         = false;

    /**
     * The constant DRIVE_ALLOW_CREATE_FOLDER.
     */
    private static final String      DRIVE_ALLOW_CREATE_FOLDER      = "nt:folder,nt:unstructured";

    /**
     * The constant DRIVE_ALLOW_NODE_TYPES_ON_TREE.
     */
    private static final String      DRIVE_ALLOW_NODE_TYPES_ON_TREE = "*";

    private static final String      EXO_DATETIME                   = "exo:datetime";
    private static final String      EXO_MODIFY                     = "exo:modify";
    public static final String      DUMMY_DATA                      = "".intern();

    private static final String     G_DOCS_MIME_TYPE                = "application/vnd.google-apps.document";
    private static final String     G_SHEETS_MIME_TYPE              = "application/vnd.google-apps.spreadsheet";
    private static final String     G_PRESENTATIONS_MIME_TYPE       = "application/vnd.google-apps.presentation";

    private static final String     DOCX_MIMETYPE                   = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
    private static final String     XLSX_MIMETYPE                   = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private static final String     PPTX_MIMETYPE                   = "application/vnd.openxmlformats-officedocument.presentationml.presentation";

    private ManageDriveService manageDriveService;
    private ClonedGFileStorage clonedGFileStorage;
    private GoogleDriveAPI api;
    private DocumentService documentService;

    private int clonedFileNumber;

    public GDriveCloneService(ManageDriveService manageDriveService, ClonedGFileStorage clonedGFileStorage,
                              DocumentService documentService) {
        this.manageDriveService = manageDriveService;
        this.clonedGFileStorage = clonedGFileStorage;
        this.documentService = documentService;
    }

    public DriveData cloneCloudDrive(GoogleUser user, Node driveNode, String folderOrFileId, String groupId) throws Exception {
        this.api = user.api();
        String workspace = driveNode.getSession().getWorkspace().getName();
        initDrive(user, driveNode);
        try {
            manageDriveService.addDrive(user.createDriveTitle(), workspace, "", driveNode.getPath(),
                    DRIVE_VIEWS, DRIVE_ICON, DRIVE_VIEW_REFERENCES, DRIVE_VIEW_NON_DOCUMENT, DRIVE_VIEW_SIDE_BAR, DRIVE_SHOW_HIDDEN_NODE,
                    DRIVE_ALLOW_CREATE_FOLDER, DRIVE_ALLOW_NODE_TYPES_ON_TREE);
        } catch (Exception e) {
            LOG.error("error while creating cloned drive", e);
        }
        fetchFiles(user, driveNode, folderOrFileId, groupId);
        Long startTime = System.currentTimeMillis();
        LOG.info("Start process Links of cloned files");
        processLinks(driveNode);
        long endTime = System.currentTimeMillis();
        Long period = ((endTime - startTime) / 1000) / 60;
        LOG.info("End process Links of cloned files in {} minutes", period);
        return manageDriveService.getDriveByName(user.createDriveTitle());
    }

    private void processLinks(Node driveNode) throws RepositoryException, IOException {
        NodeIterator iterator = driveNode.getNodes();
        while(iterator.hasNext()) {
            Node current = iterator.nextNode();
            if(current.isNodeType(NodetypeConstant.NT_FILE)) {
                Node content = current.getNode(NodetypeConstant.JCR_CONTENT);
                InputStream inputStream = content.getProperty(NodetypeConstant.JCR_DATA).getStream();
                String mimeType = content.getProperty(NodetypeConstant.JCR_MIME_TYPE).getValue().getString();
                if (mimeType.equals(DOCX_MIMETYPE)) {
                    XWPFDocument document = new XWPFDocument(inputStream);
                    List<XWPFParagraph> paragraphs = document.getParagraphs();
                    for (XWPFParagraph para : paragraphs) {
                        final Object[] runs = para.getRuns().toArray();
                        int countLink = -1;
                        for (int i = 0; i < runs.length; i++) {
                            if (runs[i] instanceof XWPFHyperlinkRun) {
                                countLink++;
                                String oldUrl = ((XWPFHyperlinkRun)runs[i]).getHyperlink(document).getURL();
                                String fileId = getFileIdFromLink(oldUrl);
                                String newUrl = null;
                                if(fileId != null) {
                                    newUrl = clonedGFileStorage.getExoLinkByGFileId(fileId);
                                }
                                CTHyperlink oldLink = para.getCTP().getHyperlinkArray(countLink);
                                List<CTText> ctTextList = oldLink.getRList().get(0).getTList();
                                String text = ctTextList.get(0).getStringValue();
                                String linkId;
                                if(newUrl != null) {
                                    linkId = para
                                            .getDocument()
                                            .getPackagePart()
                                            .addExternalRelationship(newUrl,
                                                    XWPFRelation.HYPERLINK.getRelation()).getId();

                                    CTHyperlink ctHyperlink = para.getCTP().getHyperlinkArray(countLink);
                                    ctHyperlink.setId(linkId);
                                    CTText ctText = CTText.Factory.newInstance();
                                    ctText.setStringValue(text);
                                    CTR ctr = CTR.Factory.newInstance();
                                    CTRPr ctrPr = ctr.addNewRPr();
                                    CTColor color = CTColor.Factory.newInstance();
                                    color.setVal("0000FF");
                                    ctrPr.setColor(color);
                                    ctrPr.addNewU().setVal(STUnderline.SINGLE);
                                    ctr.setTArray(new CTText[]{ctText});
                                    ctHyperlink.setRArray(new CTR[]{ctr});
                                }
                            }
                        }
                    }
                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                    document.write(outputStream);
                    InputStream newInputStream = new ByteArrayInputStream(outputStream.toByteArray());
                    content.setProperty(NodetypeConstant.JCR_DATA, newInputStream);
                    document.close();
                } else if (mimeType.equals(XLSX_MIMETYPE)) {
                    XSSFWorkbook xssfWorkbook = new XSSFWorkbook(inputStream);
                    CreationHelper helper = xssfWorkbook.getCreationHelper();
                    int numSheets = xssfWorkbook.getNumberOfSheets();
                    for (int i = 0; i < numSheets; i++) {
                        XSSFSheet sheet = xssfWorkbook.getSheetAt(i);
                        Iterator<Row> rowIterator = sheet.rowIterator();
                        while (rowIterator.hasNext()) {
                            Row currentRow = rowIterator.next();
                            Iterator<Cell> cellIterator = currentRow.cellIterator();
                            while (cellIterator.hasNext()) {
                                Cell currentCell = cellIterator.next();
                                Hyperlink oldHyperlink = currentCell.getHyperlink();
                                if (oldHyperlink != null) {
                                    String oldUrl = currentCell.getHyperlink().getAddress();
                                    String fileId = getFileIdFromLink(oldUrl);
                                    String newUrl = null;
                                    if (fileId != null) {
                                        newUrl = clonedGFileStorage.getExoLinkByGFileId(fileId);
                                    }
                                    if (newUrl != null) {
                                        currentCell.removeHyperlink();
                                        XSSFHyperlink link = (XSSFHyperlink) helper.createHyperlink(HyperlinkType.URL);
                                        link.setAddress(newUrl);
                                        link.setLabel(oldHyperlink.getLabel());
                                        currentCell.setHyperlink(link);
                                    }
                                }
                            }
                        }
                    }
                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                    xssfWorkbook.write(outputStream);
                    InputStream newInputStream = new ByteArrayInputStream(outputStream.toByteArray());
                    content.setProperty(NodetypeConstant.JCR_DATA, newInputStream);
                    xssfWorkbook.close();
                } else if (mimeType.equals(PPTX_MIMETYPE)) {
                    XMLSlideShow slideShow = new XMLSlideShow(inputStream);
                    Iterator<XSLFSlide> slideIterator = slideShow.getSlides().iterator();
                    while (slideIterator.hasNext()) {
                        XSLFSlide currentSlide = slideIterator.next();
                        Iterator<XSLFShape> shapeIterator = currentSlide.iterator();
                        while (shapeIterator.hasNext()) {
                            XSLFShape currentShape = shapeIterator.next();
                            if(currentShape instanceof XSLFTextShape) {
                                Iterator<XSLFTextParagraph> paragraphIterator = ((XSLFTextShape) currentShape).iterator();
                                while(paragraphIterator.hasNext()) {
                                    XSLFTextParagraph paragraph = paragraphIterator.next();
                                    Iterator<XSLFTextRun> textRunIterator = paragraph.iterator();
                                    while (textRunIterator.hasNext()) {
                                        XSLFTextRun currentText = textRunIterator.next();
                                        XSLFHyperlink hyperlink = currentText.getHyperlink();
                                        if(hyperlink != null) {
                                            String oldUrl = hyperlink.getAddress();
                                            String fileId = getFileIdFromLink(oldUrl);
                                            String newUrl = null;
                                            if (fileId != null) {
                                                newUrl = clonedGFileStorage.getExoLinkByGFileId(fileId);
                                            }
                                            if (newUrl != null) {
                                                XSLFHyperlink link = currentText.createHyperlink();
                                                link.setLabel(hyperlink.getLabel());
                                                link.setAddress(newUrl);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                    slideShow.write(outputStream);
                    InputStream newInputStream = new ByteArrayInputStream(outputStream.toByteArray());
                    content.setProperty(NodetypeConstant.JCR_DATA, newInputStream);
                    slideShow.close();
                }
                    current.save();
            }
        }
    }

    private void initDrive(CloudUser user, Node driveNode) throws RepositoryException, CloudDriveException {
        if (!driveNode.hasProperty(NodetypeConstant.EXO_TITLE)) {
            driveNode.setProperty(NodetypeConstant.EXO_TITLE, user.createDriveTitle());
            driveNode.setProperty(NodetypeConstant.EXO_NAME, Utils.cleanName(user.createDriveTitle()));
        }
    }

    private void fetchFiles(GoogleUser user, Node driveNode, String folderOrFileId, String groupId) throws Exception {
        About about = api.about();
        String id = about.getRootFolderId();
        Long startTime = System.currentTimeMillis();
        LOG.info("Start GDrive cloning files ...");
        setClonedFileNumber(0);
        if (StringUtils.isNotBlank(folderOrFileId)) {
            fetchParents(folderOrFileId, driveNode, groupId);
        } else {
            fetchChildren(id, driveNode, groupId);
        }
        long endTime = System.currentTimeMillis();
        Long period = ((endTime - startTime) / 1000) / 60;
        LOG.info("End cloning GDrive files : {} files were successfully cloned in {} minutes", getClonedFileNumber(), period);
    }

    private void fetchParents(String id, Node localNode, String groupId) throws Exception {
        List<File> parents = new ArrayList<>();
        File gf = this.api.file(id);
        List<File> files = getParentsHierarchy(id, parents);
        Node createdNode;
        if (files.size() != 0) {
            Collections.reverse(files);
        }
        for (File file : files) {
            createdNode = createFolder(file, localNode, groupId);
            localNode = createdNode;
            saveChanges(localNode);
        }
        if (api.isFolder(gf) && localNode != null) {
            createdNode = createFolder(gf, localNode, groupId);
            saveChanges(localNode);
            fetchChildren(gf.getId(), createdNode, groupId);
        } else {
            createFile(gf, localNode, groupId);
            saveChanges(localNode);
        }
    }

    private void saveChanges(Node node) throws RepositoryException {
        node.getSession().refresh(true);
        node.getSession().save();
    }

    private List<File> getParentsHierarchy(String id, List files) throws IOException, GoogleDriveException, NotFoundException {
        Iterator iterator = ParentsIterator(id);
        while (iterator.hasNext()) {
            ParentReference parent = (ParentReference) iterator.next();
            File file = this.api.file(parent.getId());
            if (!parent.getIsRoot()) {
                files.add(file);
                getParentsHierarchy(file.getId(), files);
            }
        }
        return files;
    }

    private Node createFolder(File file, Node driveNode, String groupId) throws Exception {
        DateTime createDate = file.getCreatedDate();
        if (createDate == null) {
            throw new GoogleDriveException("File " + file.getTitle() + " doesn't have Created Date.");
        }
        Calendar created = api.parseDate(createDate.toStringRfc3339());
        DateTime modifiedDate = file.getModifiedDate();
        if (modifiedDate == null) {
            throw new GoogleDriveException("File " + file.getTitle() + " doesn't have Modified Date.");
        }
        Calendar modified = api.parseDate(modifiedDate.toStringRfc3339());
        Node folderNode = openNode(file.getTitle(), driveNode, "nt:folder", file.getMimeType(), modified, file.getFileExtension());
        folderNode = makeFolderNode(folderNode, file.getTitle(), file.getMimeType(), file.getOwnerNames().get(0),
                file.getLastModifyingUserName(), created, modified);

        LOG.info("File with name {} was successfully cloned!", file.getTitle());
        clonedFileNumber++;
        return folderNode;
    }

    private Node createFile(File file, Node driveNode, String groupId) throws Exception {
        DateTime createDate = file.getCreatedDate();
        if (createDate == null) {
            throw new GoogleDriveException("File " + file.getTitle() + " doesn't have Created Date.");
        }
        Calendar created = api.parseDate(createDate.toStringRfc3339());
        DateTime modifiedDate = file.getModifiedDate();
        if (modifiedDate == null) {
            throw new GoogleDriveException("File " + file.getTitle() + " doesn't have Modified Date.");
        }
        Calendar modified = api.parseDate(modifiedDate.toStringRfc3339());
        Long size = fileSize(file);
        String link = file.getAlternateLink();
        Node fileNode = null;
        if (sizeToMegaBytes(size) <= 190) {
            InputStream inputStream = getGFileInputStream(file.getId(), file.getMimeType(), size, link);
            fileNode = openNode(file.getTitle(), driveNode, "nt:file", file.getMimeType(), modified, file.getFileExtension());
            fileNode = makeFileNode(fileNode, file.getTitle(), file.getMimeType(), file.getOwnerNames().get(0),
                    file.getLastModifyingUserName(), created, modified, inputStream, file.getFileExtension());
            addClonedFile(fileNode, groupId, file.getId(), file.getAlternateLink(), modified.getTime());
            LOG.info("File with name {} was successfully cloned!", file.getTitle());
            clonedFileNumber++;
        }
        return fileNode;
    }

    private void fetchChildren(String id, Node localNode, String groupId) throws Exception {
        GoogleDriveAPI.ChildIterator children = api.children(id);
        while (children.hasNext() && !Thread.currentThread().isInterrupted()) {
            ChildReference child = children.next();
            File gf = api.file(child.getId());
            if (!gf.getLabels().getTrashed()) {
                boolean isFolder = api.isFolder(gf);
                if (isFolder) {
                    Node createdNode = createFolder(gf, localNode, groupId);
                    fetchChildren(gf.getId(), createdNode, groupId);
                } else {
                    createFile(gf, localNode, groupId);
                }
            }
            setClonedFileNumber(clonedFileNumber);
            saveChanges(localNode);
        }
    }

    private void addClonedFile(Node fileNode, String groupId, String fileId, String fileGDriveLink, Date lastModifiedInGDrive) throws Exception {
        String exoLink = documentService.getDocumentUrlInSpaceDocuments(fileNode, groupId);
        ClonedGFile clonedGFile = new ClonedGFile(null, fileId, fileNode.getUUID(), fileGDriveLink,
                exoLink, lastModifiedInGDrive, Calendar.getInstance().getTime());
        ClonedGFile exist = clonedGFileStorage.getClonedFileByGLink(fileId);
        if (exist == null) {
            clonedGFileStorage.addClonedFile(clonedGFile);
        } else {
            clonedGFile.setId(exist.getId());
            clonedGFileStorage.update(clonedGFile);
        }
    }
    private long fileSize(File gf) {
        Long size = gf.getFileSize();
        if (size == null) {
            size = gf.getQuotaBytesUsed();
            if (size == null) {
                size = -1l;
            }
        }
        return size;
    }

    private double sizeToMegaBytes(Long size) {
        return size / Math.pow(1024,2);
    }

    private String getFileIdFromLink(String link) {
        if (link.contains("/document/d/")) {
            return link.split("/document/d/")[1].split("/")[0];
        } else if (link.contains("/spreadsheets/d/")) {
            return link.split("/spreadsheets/d/")[1].split("/")[0];
        } else if (link.contains("/presentation/d/")) {
            return link.split("/presentation/d/")[1].split("/")[0];
        } else if (link.contains("/document/u/1/d/")) {
            return link.split("/document/u/1/d/")[1].split("/")[0];
        } else if (link.contains("/spreadsheets/u/1/d/")) {
            return link.split("/spreadsheets/u/1/d/")[1].split("/")[0];
        } else if (link.contains("/presentation/u/1/d/")) {
            return link.split("/presentation/u/1/d/")[1].split("/")[0];
        }
        return null;
    }
    private InputStream getGFileInputStream(String fileId, String mimeType, Long size, String link) throws NotFoundException, GoogleDriveException {
        try {
            return this.api.drive.files().get(fileId).executeMediaAsInputStream();
        } catch (GoogleJsonResponseException e) {
            if (e.getStatusCode() == 404) {
                LOG.error("Cloud file not found: " + fileId, e);
            } else {
                // try to export the file when it's not a file with binary content (not downloadable)
                if (sizeToMegaBytes(size) <= 10) {
                    try {
                        return this.api.drive.files().export(fileId, getMimeTypeToExport(mimeType)).executeMedia().getContent();
                    } catch (IOException ioException) {
                        LOG.error("Error getting file from Files service: " + e.getMessage(), e);
                    }
                } else {
                    try {
                        if (getExportLink(link) != null) {
                            return this.api.drive.getRequestFactory().
                                    buildGetRequest(new GenericUrl(getExportLink(link))).execute().getContent();
                        }
                    } catch (IOException exception) {
                        LOG.debug("Could not get an input stream from an external url", e);
                    }
                }
            }
        } catch (IOException e) {
            LOG.error("Error requesting file from Files service: " + e.getMessage(), e);
        }
        return null;
    }

    private String getExportLink(String link) {
        if (link.contains("/document/d/")) {
            return link.substring(0, link.lastIndexOf("/") + 1).concat("export?format=docx");
        } else if (link.contains("/spreadsheets/d/")) {
            return link.substring(0, link.lastIndexOf("/") + 1).concat("export?format=xlsx");
        } else if (link.contains("/presentation/d/")) {
            return link.substring(0, link.lastIndexOf("/") + 1).concat("export/pptx");
        }
        return null;
    }

    private String getMimeTypeToExport(String mimeType) {
        if (mimeType.equals(G_DOCS_MIME_TYPE)) {
            return DOCX_MIMETYPE;
        } else if (mimeType.equals(G_SHEETS_MIME_TYPE)) {
            return XLSX_MIMETYPE;
        } else if (mimeType.equals(G_PRESENTATIONS_MIME_TYPE)) {
            return PPTX_MIMETYPE;
        }
        return mimeType;
    }

    private String getGFileTitleWithExtension(String title, String mimeType, String extension) {
        if (mimeType.equals(G_DOCS_MIME_TYPE)) {
            return title.concat(".docx");
        } else if (mimeType.equals(G_SHEETS_MIME_TYPE)) {
            return title.concat(".xlsx");
        } else if (mimeType.equals(G_PRESENTATIONS_MIME_TYPE)) {
            return title.concat(".pptx");
        }

        if (title.endsWith(" ") && extension != null) {
            title = title.concat("." + extension);
        } else {
            title = title.replaceAll("\\s+$", "");
        }
        return title;
    }

    private boolean isFileInFolder(String folderId, String fileId) throws IOException {
        if(folderId == null) {
            return false;
        }
        try {
            this.api.drive.parents().get(fileId, folderId).execute();
        }catch (HttpResponseException e) {
            if (e.getStatusCode() == 404) {
                return false;
            } else {
                LOG.error("An error occurred: " + e);
            }
        } catch (IOException e) {
            LOG.error("An error occurred: " + e);
        }
        return true;
    }

    private boolean isParent(String fileId, String folderId) throws CloudDriveException, IOException {
        boolean found = false;
        if (this.isFileInFolder(folderId, fileId))
            return true;
        GoogleDriveAPI.ChildIterator children = this.api.children(folderId);
        while (children.hasNext()) {
            ChildReference child = children.next();
            if (this.api.isFolder(this.api.file(child.getId()))) {
                if (this.isFileInFolder(child.getId(), fileId)) {
                    found = true;
                    break;
                } else {
                    found = isParent(fileId, child.getId());
                }
            }
        }
        return found;
    }

    private Iterator<ParentReference> ParentsIterator (String fileId) throws IOException {
        ParentList parents =  this.api.drive.parents().list(fileId).execute();
        Iterator<ParentReference> iterator = parents.getItems().iterator();
        return iterator;
    }
    private void addMetadata(Node node, String title, String creator, String lastModifier, Calendar created) throws RepositoryException {
        if(node.canAddMixin("dc:elementSet")) {
            node.addMixin("dc:elementSet");
            String array[] = {creator};
            node.setProperty("dc:creator", array);
            array = new String[] {lastModifier};
            node.setProperty("dc:contributor", array);
            array = new String[] {title};
            node.setProperty("dc:title", array);
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
            array = new String[] {dateFormat.format(created.getTime())};
            node.setProperty("dc:date", array);
        }
    }
    private Node makeFileNode(Node node, String title, String mimeType, String owner, String lastModifier,
                              Calendar created, Calendar modified, InputStream fileContent, String extension) throws RepositoryException {
        if (node.isNew() && !node.hasNode("jcr:content")) {
            node.addMixin("mix:versionable");
            initNodeCommonProperties(node, title, mimeType, owner, lastModifier, created, modified, extension);
            Node content = node.addNode("jcr:content", "nt:resource");
            if (fileContent != null) {
                content.setProperty("jcr:data", fileContent);
                addMetadata(content, title, owner, lastModifier, created);
            } else {
                content.setProperty("jcr:data", DUMMY_DATA);
            }
        }
        Node content = node.getNode("jcr:content");

        content.setProperty("jcr:mimeType", getMimeTypeToExport(mimeType));
        content.setProperty("jcr:lastModified", modified);

        if (!node.isNew()) {
            if (fileContent != null) {
                content.setProperty("jcr:data", fileContent);
            } else {
                content.setProperty("jcr:data", DUMMY_DATA);
            }
            node.save();
        }
        return node;
    }

    private Node makeFolderNode(Node node, String title, String mimeType, String owner, String lastModifier,
                                Calendar created, Calendar modified) throws RepositoryException {
        if (node.canAddMixin("exo:privilegeable")) {
            node.addMixin("exo:privilegeable");
        }
        initNodeCommonProperties(node, title, mimeType, owner, lastModifier, created, modified, null);
        return node;
    }

    private void initNodeCommonProperties(Node node, String title, String mimeType, String owner, String lastModifier,
                                          Calendar created, Calendar modified, String extension) throws RepositoryException {

        String name = title;
        if (node.isNodeType(NodetypeConstant.NT_FILE)) {
            name = getGFileTitleWithExtension(title, mimeType, extension);
        }
        node.setProperty("exo:title", name);
        if (node.hasProperty("exo:name")) {
            node.setProperty("exo:name", Utils.cleanName(name));
        }
        if (node.isNodeType(EXO_DATETIME)) {
            if (created != null) {
                node.setProperty("exo:dateCreated", created);
            }
            if (modified != null) {
                node.setProperty("exo:dateModified", modified);
            }
        }
        if (node.isNodeType(EXO_MODIFY)) {
            if (modified != null && lastModifier != null) {
                node.setProperty("exo:lastModifiedDate", modified);
                node.setProperty("exo:lastModifier", lastModifier);
            }
        }
    }


    private Node openNode(String fileTitle, Node parent, String nodeType, String mimetype, Calendar lastModified, String extension)  {
        NodeFinder nodeFinder = CommonsUtils.getService(NodeFinder.class);
        Node node = null;
        String baseName = Utils.cleanName(fileTitle);
        String name = baseName;
        String internalName = null;
        boolean titleTried = false;
        do {
            if (internalName == null) {
                internalName = name;
                // try NodeFinder name (storage specific, e.g. ECMS)
                String finderName = nodeFinder.cleanName(fileTitle);
                if (finderName.length() > 1) {
                    name = finderName;
                    continue;
                }
            }
            if (!titleTried) {
                titleTried = true;
                try {
                    if (parent.hasNode(fileTitle)) {
                        name = fileTitle;
                        continue;
                    }
                } catch (Throwable te) {
                }
            }
            String cname = getGFileTitleWithExtension(internalName,mimetype, extension);
            try {
                if (!parent.hasNode(cname)) {
                    node = parent.addNode(cname, nodeType);
                } else {
                    node = parent.getNode(cname);
                    if (node.isNodeType(NodetypeConstant.NT_FILE)) {
                        Date d1 = node.getProperty("exo:lastModifiedDate").getValue().getDate().getTime();
                        Date d2 = lastModified.getTime();
                        if (d1.compareTo(d2) < 0) {
                            VersionHistoryUtils.createVersion(node);
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            break;
        } while (true);

        return node;
    }

    public int getClonedFileNumber() {
        return clonedFileNumber;
    }

    public void setClonedFileNumber(int clonedFileNumber) {
        this.clonedFileNumber = clonedFileNumber;
    }
}
