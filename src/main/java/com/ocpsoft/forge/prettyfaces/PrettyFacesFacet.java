/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011, Red Hat, Inc., and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package com.ocpsoft.forge.prettyfaces;

import java.util.Arrays;

import javax.inject.Inject;
import javax.servlet.DispatcherType;

import org.jboss.seam.forge.project.dependencies.Dependency;
import org.jboss.seam.forge.project.facets.BaseFacet;
import org.jboss.seam.forge.project.facets.DependencyFacet;
import org.jboss.seam.forge.project.facets.WebResourceFacet;
import org.jboss.seam.forge.resources.FileResource;
import org.jboss.seam.forge.shell.ShellPrompt;
import org.jboss.seam.forge.shell.plugins.Alias;
import org.jboss.seam.forge.shell.plugins.RequiresFacet;
import org.jboss.seam.forge.spec.servlet.ServletFacet;
import org.jboss.shrinkwrap.descriptor.api.spec.servlet.web.FilterDef;
import org.jboss.shrinkwrap.descriptor.api.spec.servlet.web.FilterMappingDef;

import com.ocpsoft.forge.prettyfaces.config.PrettyFacesConfig;
import com.ocpsoft.pretty.PrettyFilter;

/**
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 * 
 */
@Alias("com.ocpsoft.prettyfaces")
@RequiresFacet({ WebResourceFacet.class, ServletFacet.class })
public class PrettyFacesFacet extends BaseFacet
{
   @Inject
   public ShellPrompt prompt;

   @Override
   public boolean install()
   {
      DependencyFacet deps = getProject().getFacet(DependencyFacet.class);

      Dependency dep = prompt.promptChoiceTyped("Install which branch of PrettyFaces?",
               Arrays.asList(PrettyFacesPlugin.PF_JSF11, PrettyFacesPlugin.PF_JSF12, PrettyFacesPlugin.PF_JSF2),
               PrettyFacesPlugin.PF_JSF2);

      dep = prompt.promptChoiceTyped("Install which version?", deps.resolveAvailableVersions(dep));

      FileResource<?> prettyConfig = getProject().getFacet(WebResourceFacet.class)
               .getWebResource("WEB-INF/pretty-config.xml");

      if (!prettyConfig.exists())
      {
         prettyConfig.createNewFile();
         prettyConfig.setContents(new PrettyFacesConfig(dep.getVersion()).exportAsString());
      }

      deps.addDependency(dep);

      if (project.hasFacet(ServletFacet.class))
      {
         ServletFacet servlet = project.getFacet(ServletFacet.class);
         String version = servlet.getConfig().getVersion();
         if (version != null && version.trim().startsWith("2"))
         {
            // servlet version does not support auto-registration of the filter. do it for them
            FilterDef filter = servlet.getConfig().filter(PrettyFilter.class);
            FilterMappingDef mappingDef = filter.asyncSupported(true).mapping().dispatchTypes(DispatcherType.values());
            servlet.saveConfig(mappingDef);
         }
      }

      return true;
   }

   @Override
   @SuppressWarnings("unchecked")
   public boolean isInstalled()
   {
      if (getProject().hasAllFacets(Arrays.asList(WebResourceFacet.class, ServletFacet.class)))
      {
         DependencyFacet deps = getProject().getFacet(DependencyFacet.class);
         if (deps.hasDependency(PrettyFacesPlugin.PF_JSF11)
                  || deps.hasDependency(PrettyFacesPlugin.PF_JSF12)
                  || deps.hasDependency(PrettyFacesPlugin.PF_JSF2))
         {
            return true;
         }
      }
      return false;
   }

}
