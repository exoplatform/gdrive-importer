package org.exoplatform.actions;

import org.exoplatform.actions.form.UICloudLinkForm;
import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.ecm.webui.component.explorer.control.filter.*;
import org.exoplatform.ecm.webui.component.explorer.control.listener.UIActionBarActionListener;
import org.exoplatform.web.application.JavascriptManager;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIPopupContainer;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.ext.filter.UIExtensionFilter;
import org.exoplatform.webui.ext.filter.UIExtensionFilters;
import org.exoplatform.webui.ext.manager.UIAbstractManager;
import org.exoplatform.webui.ext.manager.UIAbstractManagerComponent;

import javax.jcr.Node;
import java.util.Arrays;
import java.util.List;


@ComponentConfig(
        events = { @EventConfig(listeners = CloneGDriveActionComponent.CloneGDriveActionListener.class) })
public class CloneGDriveActionComponent extends UIAbstractManagerComponent {

    protected static final String PROVIDER_ID = "cgdrive";

    private static final List<UIExtensionFilter> FILTERS =
            Arrays.asList(new IsNotNtFileFilter(),
                    new CanAddNodeFilter(),
                    new IsNotCategoryFilter(),
                    new IsNotLockedFilter(),
                    new IsCheckedOutFilter(),
                    new IsNotTrashHomeNodeFilter(),
                    new IsNotInTrashFilter(),
                    new IsNotEditingDocumentFilter(),
                    new HasAllowedFolderTypeFilter());

    @UIExtensionFilters
    public List<UIExtensionFilter> getFilters() {
        return FILTERS;
    }

    @Override
    public Class<? extends UIAbstractManager> getUIAbstractManagerClass() {
        return null;
    }

    public static class CloneGDriveActionListener extends UIActionBarActionListener<CloneGDriveActionComponent> {
        @Override
        protected void processEvent(Event<CloneGDriveActionComponent> event) throws Exception {
            UIJCRExplorer uiJCRExplorer = event.getSource().getAncestorOfType(UIJCRExplorer.class);
            Node rootNode = uiJCRExplorer.getRootNode();
            String workspace = rootNode.getSession().getWorkspace().getName();
            JavascriptManager jsManager = event.getRequestContext().getJavascriptManager();
            jsManager.require("SHARED/ClonedDrive", "cpgdrive").
                    addScripts("cpgdrive.init('" + workspace + "','" + rootNode.getPath() + "');\n");
            UIPopupContainer uiPopupContainer = uiJCRExplorer.getChild(UIPopupContainer.class);
            UICloudLinkForm uiCloudLinkForm = uiPopupContainer.createUIComponent(UICloudLinkForm.class, null, null);
            uiPopupContainer.activate(uiCloudLinkForm, 450, 300, false);
            event.getRequestContext().addUIComponentToUpdateByAjax(uiPopupContainer);
        }

    }
}