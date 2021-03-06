/*
 * Copyright (c) 2012, the Dart project authors.
 * 
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.github.sdbg.debug.core.internal.source;

import com.github.sdbg.debug.core.ISourceLookupExtensions;
import com.github.sdbg.debug.core.SDBGDebugCorePlugin;
import com.github.sdbg.debug.core.SDBGLaunchConfigWrapper;
import com.github.sdbg.debug.core.internal.webkit.model.WebkitSourcePathComputerDelegate;
import com.github.sdbg.debug.core.util.SDBGNoSourceFoundElement;
import com.github.sdbg.utilities.AdapterUtilities;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.Platform;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.sourcelookup.AbstractSourceLookupDirector;
import org.eclipse.debug.core.sourcelookup.ISourceLookupParticipant;
import org.eclipse.debug.core.sourcelookup.ISourcePathComputerDelegate;

/**
 * A collection of SDBG specific source lookup participants.
 * 
 * @see org.eclipse.debug.core.sourcelookup.ISourceLookupParticipant
 */
public class SDBGSourceLookupDirector extends AbstractSourceLookupDirector {
  private Collection<ISourceLookupExtensions> sourceLookupExtensions;

  public SDBGSourceLookupDirector() {
  }

  @Override
  public Object getSourceElement(Object element) {
    Object sourceElement = super.getSourceElement(element);

    if (sourceElement == null) {
      ILaunch launch = AdapterUtilities.getAdapter(element, ILaunch.class);

      if (launch != null) {
        return new SDBGNoSourceFoundElement(launch, element.toString());
      }
    }

    return sourceElement;
  }

  @Override
  public void initializeParticipants() {
    IProject project = new SDBGLaunchConfigWrapper(getLaunchConfiguration()).getProject();

    List<ISourceLookupParticipant> participants = new ArrayList<ISourceLookupParticipant>();
    for (ISourceLookupExtensions extensions : getSourceLookupExtensions()) {
      try {
        participants.addAll(Arrays.asList(extensions.getSourceLookupParticipants(project)));
      } catch (CoreException e) {
        SDBGDebugCorePlugin.logError(e);
      }
    }

    participants.add(new SDBGSourceLookupParticipant());
    addParticipants(participants.toArray(new ISourceLookupParticipant[0]));
  }

  @Override
  protected void setLaunchConfiguration(ILaunchConfiguration configuration) {
    super.setLaunchConfiguration(configuration);

    IProject project = new SDBGLaunchConfigWrapper(getLaunchConfiguration()).getProject();

    List<ISourcePathComputerDelegate> computerDelegates = new ArrayList<ISourcePathComputerDelegate>();
    for (ISourceLookupExtensions extensions : getSourceLookupExtensions()) {
      try {
        ISourcePathComputerDelegate computerDelegate = extensions.getSourcePathComputerDelegate(project);
        if (computerDelegate != null) {
          computerDelegates.add(computerDelegate);
        }
      } catch (CoreException e) {
        SDBGDebugCorePlugin.logError(e);
      }
    }

    computerDelegates.add(new WebkitSourcePathComputerDelegate());
    setSourcePathComputer(new SDBGSourcePathComputer(
        computerDelegates.toArray(new ISourcePathComputerDelegate[0])));
  }

  private Collection<ISourceLookupExtensions> getSourceLookupExtensions() {
    if (sourceLookupExtensions == null) {
      sourceLookupExtensions = new ArrayList<ISourceLookupExtensions>();

      IExtensionPoint extensionPoint = Platform.getExtensionRegistry().getExtensionPoint(
          ISourceLookupExtensions.EXTENSION_ID);
      for (IConfigurationElement element : extensionPoint.getConfigurationElements()) {
        try {
          sourceLookupExtensions.add((ISourceLookupExtensions) element.createExecutableExtension("class"));
        } catch (CoreException e) {
          SDBGDebugCorePlugin.logError(e);
        }
      }
    }

    return sourceLookupExtensions;
  }
}
