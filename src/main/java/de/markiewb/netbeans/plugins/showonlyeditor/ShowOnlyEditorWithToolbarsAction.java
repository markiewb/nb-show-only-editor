/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.markiewb.netbeans.plugins.showonlyeditor;

import static java.awt.Frame.MAXIMIZED_BOTH;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import static java.util.Arrays.asList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.prefs.BackingStoreException;
import javax.swing.Action;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle.Messages;
import org.openide.util.NbPreferences;
import org.openide.windows.Mode;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;

@ActionID(
        category = "Window",
        id = "de.markiewb.netbeans.plugins.showonlyeditor.ShowOnlyEditorWithToolbarsAction"
)
@ActionRegistration(
        displayName = "#CTL_ShowOnlyEditorWithToolbarsAction"
)
@ActionReferences({
    @ActionReference(path = "Menu/View", position = 1195),
    @ActionReference(path = "Shortcuts", name = "DOS-ENTER")
})
@Messages("CTL_ShowOnlyEditorWithToolbarsAction=Show Only Editor (With Toolbars)")
public final class ShowOnlyEditorWithToolbarsAction implements ActionListener {

    @Override
    public void actionPerformed(ActionEvent e) {
        java.util.prefs.Preferences globalPreferences = NbPreferences.forModule(ShowOnlyEditorWithToolbarsAction.class);
        Map<String, String> modeMap = new HashMap<>();

        WindowManager manager = org.openide.windows.WindowManager.getDefault();
        boolean shouldMinimize = shouldTCsBeMinimized(manager);

        Action action;
        if (!shouldMinimize) {
            //Undo minimize
            action = org.openide.awt.Actions.forID("Window", "org.netbeans.core.windows.actions.DockWindowAction");
        } else {
            //minimize
            action = org.openide.awt.Actions.forID("Window", "org.netbeans.core.windows.actions.MinimizeWindowAction");
        }
        for (Mode mode : manager.getModes()) {
            final boolean editorMode = manager.isEditorMode(mode);
            if (editorMode) {
                continue;
            }
            TopComponent[] openedTopComponents = manager.getOpenedTopComponents(mode);
            if (openedTopComponents.length <= 0) {
                continue;
            }
            final TopComponent selectedTopComponent = mode.getSelectedTopComponent();
            if (null != selectedTopComponent) {
                String selectedTCperMode = manager.findTopComponentID(selectedTopComponent);
                modeMap.put(mode.getName(), selectedTCperMode);
            }

            //reverse TC to retain original order
            List<TopComponent> tcS = new ArrayList<>(asList(openedTopComponents));
            Collections.reverse(tcS);
            for (TopComponent openedTopComponent : tcS) {

                if (manager.isTopComponentFloating(openedTopComponent)) {
                    continue;
                }

                openedTopComponent.requestActive();
                openedTopComponent.requestFocusInWindow();

                action.actionPerformed(null);
            }
            if (!shouldMinimize) {
                String name = mode.getName();
                String get = globalPreferences.get(name, null);
                if (get != null) {
                    TopComponent previouslySelectedTC = manager.findTopComponent(get);
                    previouslySelectedTC.requestActive();
                    previouslySelectedTC.requestFocus();
                }
            }
        }
        maximizeMainWindow(manager);

        //Persist selected TC in modes
        if (shouldMinimize) {
//            System.out.println("modeMap = " + modeMap);
            try {
                globalPreferences.clear();
            } catch (BackingStoreException ex) {
                Exceptions.printStackTrace(ex);
            }
            for (Map.Entry<String, String> entrySet : modeMap.entrySet()) {
                String key = entrySet.getKey();
                String value = entrySet.getValue();
                globalPreferences.put(key, value);
            }

        }

        focusEditor(manager);
    }

    private boolean shouldTCsBeMinimized(WindowManager manager) {
        long countTC = 0;
        long countMinimized = 0;
        long countFloating = 0;
        for (Mode mode : manager.getModes()) {
            final boolean editorMode = manager.isEditorMode(mode);
            if (editorMode) {
                continue;
            }

            TopComponent[] openedTopComponents = manager.getOpenedTopComponents(mode);
            for (TopComponent openedTopComponent : openedTopComponents) {

                countTC++;
                if (manager.isTopComponentFloating(openedTopComponent)) {
                    countFloating++;
                }

                if (manager.isTopComponentMinimized(openedTopComponent)) {
                    countMinimized++;
                }
            }
        }
        final boolean shouldMinimize = (countMinimized) != (countTC - countFloating);
        return shouldMinimize;
    }

    private void maximizeMainWindow(WindowManager manager) {
        manager.getMainWindow().setExtendedState(MAXIMIZED_BOTH);
    }

    private void focusEditor(WindowManager manager) {
        Mode findMode = manager.findMode("editor");
        TopComponent selectedTopComponent = findMode.getSelectedTopComponent();
        if (null != selectedTopComponent) {
            selectedTopComponent.requestFocusInWindow();
        }
    }
}
