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

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.jboss.seam.forge.project.Project;
import org.jboss.seam.forge.project.facets.WebResourceFacet;
import org.jboss.seam.forge.resources.DirectoryResource;
import org.jboss.seam.forge.resources.Resource;
import org.jboss.seam.forge.resources.ResourceFlag;
import org.jboss.seam.forge.shell.completer.SimpleTokenCompleter;
import org.jboss.seam.forge.spec.javaee.ServletFacet;

/**
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 * 
 */
public class ViewIdCompleter extends SimpleTokenCompleter
{
   @Inject
   private Project project;

   @Override
   public List<Object> getCompletionTokens()
   {
      List<Object> result = new ArrayList<Object>();

      try
      {
         ServletFacet servlet = project.getFacet(ServletFacet.class);
         WebResourceFacet web = project.getFacet(WebResourceFacet.class);

         for (Resource<?> r : servlet.getResources())
         {
            if (!r.isFlagSet(ResourceFlag.Node))
            {
               if (!r.getFullyQualifiedName().matches(".*(WEB-INF|META-INF).*"))
               {
                  DirectoryResource webRoot = web.getWebRootDirectory();
                  String webRootPath = webRoot.getFullyQualifiedName();

                  result.add(r.getFullyQualifiedName().replaceAll("^" + webRootPath, ""));
               }
            }

         }
      }
      catch (Exception e)
      {
         // oh well, no completion for you
      }

      return result;
   }

}
