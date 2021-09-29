package org.exoplatform.actions.form;

import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.UIPopupComponent;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.form.UIForm;
import org.exoplatform.webui.form.UIFormStringInput;

import javax.jcr.Node;

@ComponentConfig(
        lifecycle = UIFormLifecycle.class,
        template =  "app:/template/UICloudLink.gtmpl",
        events = {
                @EventConfig(listeners = UICloudLinkForm.CloneActionListener.class),
                @EventConfig(listeners = UICloudLinkForm.OnChangeActionListener.class),
                @EventConfig(listeners = UICloudLinkForm.CancelActionListener.class, phase= Event.Phase.DECODE)
        }
)
public class UICloudLinkForm extends UIForm implements UIPopupComponent {
    public static final String FIELD_TITLE_TEXT_BOX = "titleTextBox";

    private static final Log LOG  = ExoLogger.getLogger(UICloudLinkForm.class.getName());
    private static final String DEFAULT_NAME = "untitled";

    String workspace;
    String nodePath;


    public UICloudLinkForm() throws Exception {
        UIFormStringInput titleTextBox = new UIFormStringInput(FIELD_TITLE_TEXT_BOX, FIELD_TITLE_TEXT_BOX, null);
        this.addUIFormInput(titleTextBox);

        this.setActions(new String[]{"Clone", "Cancel"});

    }

    @Override
    public void activate() {
        UIJCRExplorer uiExplorer = this.getAncestorOfType(UIJCRExplorer.class);
        Node currentNode = null;
        try {
            currentNode = uiExplorer.getCurrentNode();
            this.workspace = currentNode.getSession().getWorkspace().getName();
            this.nodePath = currentNode.getPath();
        } catch (Exception e) {
        }
    }

    @Override
    public void deActivate() {

    }

    public static class CloneActionListener extends EventListener<UICloudLinkForm> {

        @Override
        public void execute(Event<UICloudLinkForm> event) throws Exception {
            UICloudLinkForm uiCloudLinkForm = event.getSource();
            UIJCRExplorer uiExplorer = uiCloudLinkForm.getAncestorOfType(UIJCRExplorer.class);
            UIApplication uiApp = uiCloudLinkForm.getAncestorOfType(UIApplication.class);
            Node currentNode = uiExplorer.getCurrentNode();
            //uiApp.addMessage(new ApplicationMessage("hello.world", null, ApplicationMessage.WARNING));
            event.getRequestContext().addUIComponentToUpdateByAjax(uiExplorer);
        }
    }

    public static class OnChangeActionListener extends EventListener<UICloudLinkForm> {

        @Override
        public void execute(Event<UICloudLinkForm> event) throws Exception {

        }
    }

    public static class CancelActionListener extends EventListener<UICloudLinkForm> {

        @Override
        public void execute(Event<UICloudLinkForm> event) throws Exception {

        }
    }

    public String getWorkspace() {
        return workspace;
    }

    public String getNodePath() {
        return nodePath;
    }

}
