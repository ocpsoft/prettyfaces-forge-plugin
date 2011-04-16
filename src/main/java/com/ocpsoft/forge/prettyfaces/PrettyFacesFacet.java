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

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.servlet.DispatcherType;

import org.jboss.seam.forge.parser.xml.XMLParser;
import org.jboss.seam.forge.project.dependencies.Dependency;
import org.jboss.seam.forge.project.dependencies.DependencyResolver;
import org.jboss.seam.forge.project.facets.BaseFacet;
import org.jboss.seam.forge.project.facets.DependencyFacet;
import org.jboss.seam.forge.project.facets.WebResourceFacet;
import org.jboss.seam.forge.resources.FileResource;
import org.jboss.seam.forge.shell.ShellPrintWriter;
import org.jboss.seam.forge.shell.ShellPrompt;
import org.jboss.seam.forge.shell.plugins.Alias;
import org.jboss.seam.forge.shell.plugins.RequiresFacet;
import org.jboss.seam.forge.spec.javaee6.servlet.ServletFacet;
import org.jboss.shrinkwrap.descriptor.api.Node;
import org.jboss.shrinkwrap.descriptor.api.spec.servlet.web.FilterDef;
import org.jboss.shrinkwrap.descriptor.api.spec.servlet.web.FilterMappingDef;
import org.xml.sax.SAXException;

import com.ocpsoft.pretty.PrettyException;
import com.ocpsoft.pretty.PrettyFilter;
import com.ocpsoft.pretty.faces.config.DigesterPrettyConfigParser;
import com.ocpsoft.pretty.faces.config.PrettyConfig;
import com.ocpsoft.pretty.faces.config.PrettyConfigBuilder;
import com.ocpsoft.pretty.faces.config.PrettyConfigParser;

/**
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 * 
 */
@Alias("com.ocpsoft.prettyfaces")
@RequiresFacet({ WebResourceFacet.class, ServletFacet.class })
public class PrettyFacesFacet extends BaseFacet
{
   @Inject
   private ShellPrompt prompt;
   @Inject
   private ShellPrintWriter writer;

   @Inject
   private DependencyResolver resolver;

   @Override
   public boolean install()
   {
      DependencyFacet deps = getProject().getFacet(DependencyFacet.class);

      writer.println();
      PrettyFacesBranch branch = prompt.promptChoiceTyped("Install PrettyFaces for which technology?",
               Arrays.asList(PrettyFacesBranch.values()), PrettyFacesBranch.SERVLET_3);

      List<Dependency> versions = resolver.resolveVersions(branch.getDependency());
      Dependency dep = prompt.promptChoiceTyped("Install which version?",
               versions, versions.isEmpty() ? null : versions.get(versions.size() - 1));

      FileResource<?> prettyConfig = getConfigFile();

      if (!prettyConfig.exists())
      {
         prettyConfig.createNewFile();
         prettyConfig.setContents(XMLParser.toXMLString(newConfig(dep.getVersion())));
      }

      Dependency existing = deps.getDependency(dep);
      if (existing != null
               && prompt.promptBoolean("Existing PrettyFaces dependency was found. Replace [" + existing + "] with ["
                        + dep + "]?"))
      {
         deps.removeDependency(existing);
      }
      deps.addDependency(dep);

      if (project.hasFacet(ServletFacet.class))
      {
         ServletFacet servletFacet = project.getFacet(ServletFacet.class);
         String servlet = servletFacet.getConfig().getVersion();
         if (servlet != null && servlet.trim().startsWith("2"))
         {
            // servlet version does not support auto-registration of the filter. do it for them
            FilterDef filter = servletFacet.getConfig().filter(PrettyFilter.class);
            FilterMappingDef mappingDef = filter.asyncSupported(true).mapping().dispatchTypes(DispatcherType.values());
            servletFacet.saveConfig(mappingDef);
         }
      }

      return true;
   }

   public FileResource<?> getConfigFile()
   {
      return getProject().getFacet(WebResourceFacet.class)
               .getWebResource("WEB-INF/pretty-config.xml");
   }

   @Override
   @SuppressWarnings("unchecked")
   public boolean isInstalled()
   {
      DependencyFacet deps = getProject().getFacet(DependencyFacet.class);
      if (getProject().hasAllFacets(Arrays.asList(WebResourceFacet.class, ServletFacet.class))
               && getConfigFile().exists())
      {
         for (PrettyFacesBranch version : PrettyFacesBranch.values())
         {
            if (deps.hasDependency(version.getDependency()))
            {
               return true;
            }
         }
      }
      return false;
   }

   public Node getConfig()
   {
      return XMLParser.parse(getConfigFile().getResourceInputStream());
   }

   public void saveConfig(Node config)
   {
      getConfigFile().setContents(XMLParser.toXMLInputStream(config));
   }

   public PrettyConfig getPrettyConfig()
   {
      try
      {
         PrettyConfigBuilder builder = new PrettyConfigBuilder();
         PrettyConfigParser parser = new DigesterPrettyConfigParser();
         parser.parse(builder, getConfigFile().getResourceInputStream());
         return builder.build();
      }
      catch (IOException e)
      {
         throw new PrettyException(e);
      }
      catch (SAXException e)
      {
         throw new PrettyException(e);
      }
   }

   public Node newConfig(String version)
   {
      return new Node("pretty-config")
               .attribute("xmlns", "http://ocpsoft.com/prettyfaces/" + version)
               .attribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance")
               .attribute("xsi:schemaLocation", "http://ocpsoft.com/prettyfaces/" + version + " " +
                        "http://ocpsoft.com/xml/ns/prettyfaces/ocpsoft-pretty-faces-" + version + ".xsd");
   }
}
