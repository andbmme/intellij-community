/*
 * Copyright 2000-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.lang.properties.editor;

import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.codeInsight.daemon.impl.ShowIntentionsPass;
import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.codeInsight.intention.impl.IntentionListStep;
import com.intellij.codeInspection.QuickFix;
import com.intellij.icons.AllIcons;
import com.intellij.lang.properties.PropertiesBundle;
import com.intellij.lang.properties.editor.inspections.ResourceBundleEditorProblemDescriptor;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.intellij.ui.popup.PopupFactoryImpl;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Dmitry Batkovich
 */
public class ResourceBundleEditorShowQuickFixesAction extends AnAction {
  private final static Logger LOG = Logger.getInstance(ResourceBundleEditorShowQuickFixesAction.class);

  public ResourceBundleEditorShowQuickFixesAction() {
    super(PropertiesBundle.message("resource.bundle.editor.show.quick.fixes.action.text"));
  }

  @Override
  public void actionPerformed(AnActionEvent e) {
    final ResourceBundleEditor editor = getEditor(e);
    LOG.assertTrue(editor != null);
    final ResourceBundlePropertyStructureViewElement element = (ResourceBundlePropertyStructureViewElement)editor.getSelectedElementIfOnlyOne();
    LOG.assertTrue(element != null);

    final PsiFile file = editor.getResourceBundle().getDefaultPropertiesFile().getContainingFile();
    final ShowIntentionsPass.IntentionsInfo intentions = new ShowIntentionsPass.IntentionsInfo();

    boolean isQuickFixListEmpty = true;
    ResourceBundleEditorProblemDescriptor[] descriptors = element.getProblemDescriptors();
    for (ResourceBundleEditorProblemDescriptor d : descriptors) {
      QuickFix[] fixes = d.getFixes();
      if (fixes != null) {
        for (int i = 0; i < fixes.length; i++) {
          intentions.inspectionFixesToShow.add(new HighlightInfo.IntentionActionDescriptor(new RBEQuickFixWrapper(d, i),
                                                                                           AllIcons.Actions.IntentionBulb));
          isQuickFixListEmpty = false;
        }
      }
    }

    if (isQuickFixListEmpty) {
      return;
    }

    final Project project = e.getProject();
    LOG.assertTrue(project != null);
    PopupFactoryImpl
      .getInstance()
      .createListPopup(new IntentionListStep(null, intentions, null, file, project))
      .showInBestPositionFor(e.getDataContext());
  }

  @Override
  public void update(AnActionEvent e) {
    final ResourceBundleEditor editor = getEditor(e);
    e.getPresentation().setEnabledAndVisible(editor != null &&
                                             editor.getSelectedElementIfOnlyOne() instanceof ResourceBundlePropertyStructureViewElement);
  }

  private ResourceBundleEditor getEditor(AnActionEvent e) {
    final FileEditor editor = PlatformDataKeys.FILE_EDITOR.getData(e.getDataContext());
    return editor instanceof ResourceBundleEditor ? (ResourceBundleEditor)editor : null;
  }

  private static class RBEQuickFixWrapper implements IntentionAction {
    private final ResourceBundleEditorProblemDescriptor myDescriptor;
    private final int myIndex;

    private RBEQuickFixWrapper(ResourceBundleEditorProblemDescriptor descriptor, int index) {
      myDescriptor = descriptor;
      myIndex = index;
    }

    @Nls
    @NotNull
    @Override
    public String getText() {
      return getFamilyName();
    }

    @Nls
    @NotNull
    @Override
    public String getFamilyName() {
      return getQuickFix().getFamilyName();
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
      return true;
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
      getQuickFix().applyFix(project, myDescriptor);
    }

    @Override
    public boolean startInWriteAction() {
      return false;
    }

    private QuickFix<ResourceBundleEditorProblemDescriptor> getQuickFix() {
      return myDescriptor.getFixes()[myIndex];
    }
  }

}

