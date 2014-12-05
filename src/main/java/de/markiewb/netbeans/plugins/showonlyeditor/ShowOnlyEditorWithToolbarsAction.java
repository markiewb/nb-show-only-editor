/* 
 * Copyright 2014 markiewb.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
import java.util.prefs.Preferences;
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
        Map<String, String> activeTabPerMode = new HashMap<>();
        Map<String, String> tabPositionPerMode = new HashMap<>();
        Map<String, Integer> tabsPerMode = new HashMap<>();

        TopComponent currentEditor = getCurrentEditor();
        WindowManager manager = org.openide.windows.WindowManager.getDefault();
        boolean shouldMinimize = shouldTCsBeMinimized(manager);
        final Set<? extends Mode> modes = manager.getModes();
        for (Mode mode : modes) {
            final boolean editorMode = manager.isEditorMode(mode);
            if (editorMode) {
                continue;
            }
            if (shouldMinimize) {
                final TopComponent selectedTopComponent = mode.getSelectedTopComponent();
                if (null != selectedTopComponent) {
                    String selectedTCperMode = manager.findTopComponentID(selectedTopComponent);
                    activeTabPerMode.put(mode.getName(), selectedTCperMode);

                    TopComponent[] topComponents = mode.getTopComponents();
                    tabsPerMode.put(mode.getName() + ".count", topComponents.length);
                    for (TopComponent tc : topComponents) {
                        String id = manager.findTopComponentID(tc);
                        final int tabPosition = tc.getTabPosition();
                        if (-1 != tabPosition) {
                            tabPositionPerMode.put(mode.getName() + "." + tabPosition, id);
                        }
                    }
                }
                TopComponent[] openedTopComponents = manager.getOpenedTopComponents(mode);
                //reverse TC to retain original order
                List<TopComponent> tcS = new ArrayList<>(asList(openedTopComponents));
                Collections.reverse(tcS);
                for (TopComponent openedTopComponent : tcS) {

                    if (manager.isTopComponentFloating(openedTopComponent)) {
                        continue;
                    }

                    //minimize
                    WindowManager.getDefault().setTopComponentMinimized(openedTopComponent, true);
                }
                //Persist selected TC in modes
                maximizeMainWindow(manager);

                try {
                    globalPreferences.clear();
                } catch (BackingStoreException ex) {
                    Exceptions.printStackTrace(ex);
                }
                persistStringMap(activeTabPerMode, globalPreferences);
                persistStringMap(tabPositionPerMode, globalPreferences);
                persistIntMap(tabsPerMode, globalPreferences);
            } else {
                int count = globalPreferences.getInt(mode.getName() + ".count", 0);
                for (int i = 0; i < count; i++) {
                    String get = globalPreferences.get(mode.getName() + "." + i, null);
                    TopComponent tc = manager.findTopComponent(get);
                    if (null != tc) {
                        WindowManager.getDefault().setTopComponentMinimized(tc, false);
                    }
                }

                //FIXME activate previously selected tab in tab groups
                String name = mode.getName();
                String get = globalPreferences.get(name, null);
                if (get != null) {
                    TopComponent previouslySelectedTC = manager.findTopComponent(get);
                    //HACK: prevent focusgrabbing of Navigator (hopefully)
                    if (null != previouslySelectedTC) {
                        previouslySelectedTC.requestVisible();
                    }
                }
            }
        }
        focusMainWindow();
        setInputFocusTo(currentEditor);
    }

    private void focusMainWindow() {
        WindowManager.getDefault().getMainWindow().requestFocusInWindow();
    }

    private TopComponent getCurrentEditor() {
        Set<TopComponent> opened = TopComponent.getRegistry().getOpened();
        for (TopComponent tc : opened) {
            boolean isEditor = WindowManager.getDefault().isOpenedEditorTopComponent(tc);
            if (isEditor){
                return tc;
            }
        }
        return null;
    }

    private void maximizeMainWindow(WindowManager manager) {
        manager.getMainWindow().setExtendedState(MAXIMIZED_BOTH);
    }

    private void persistIntMap(Map<String, Integer> map, Preferences globalPreferences) {
        for (Map.Entry<String, Integer> entrySet : map.entrySet()) {
            String key = entrySet.getKey();
            Integer value = entrySet.getValue();
            globalPreferences.putInt(key, value);
        }
    }

    private void persistStringMap(Map<String, String> map, Preferences globalPreferences) {
        for (Map.Entry<String, String> entrySet : map.entrySet()) {
            String key = entrySet.getKey();
            String value = entrySet.getValue();
            globalPreferences.put(key, value);
        }
    }

    private void setInputFocusTo(TopComponent toFocus) {
        if (null != toFocus) {
            toFocus.open();
            toFocus.requestActive();
            WindowManager.getDefault().getMainWindow().toFront();
            toFocus.requestFocus();
            toFocus.requestFocusInWindow();
        }
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

}
