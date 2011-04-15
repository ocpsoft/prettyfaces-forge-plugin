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

import java.util.List;

import org.jboss.seam.forge.parser.java.JavaClass;
import org.jboss.seam.forge.project.Project;
import org.jboss.seam.forge.resources.Resource;
import org.jboss.seam.forge.scaffold.AccessStrategy;
import org.jboss.seam.forge.scaffold.ScaffoldProvider;
import org.jboss.seam.forge.spec.javaee6.jsf.FacesFacet;

/**
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 * 
 */
public class MockScaffoldProvider implements ScaffoldProvider
{
   private final FacesFacet facesFacet;

   public MockScaffoldProvider(FacesFacet facet)
   {
      this.facesFacet = facet;
   }

   @Override
   public List<Resource<?>> generateFromEntity(Project project, JavaClass entity, boolean overwrite)
   {
      throw new IllegalStateException("Stub!");
   }

   @Override
   public List<Resource<?>> generateIndex(Project project, boolean overwrite)
   {
      throw new IllegalStateException("Stub!");
   }

   @Override
   public List<Resource<?>> getGeneratedResources(Project project)
   {
      throw new IllegalStateException("Stub!");
   }

   @Override
   public List<Resource<?>> generateTemplates(Project project, boolean overwrite)
   {
      throw new IllegalStateException("Stub!");
   }

   @Override
   public AccessStrategy getAccessStrategy(Project project)
   {
      return new AccessStrategy()
      {

         @Override
         public List<String> getWebPaths(Resource<?> r)
         {
            return facesFacet.getWebPaths(r);
         }

         @Override
         public Resource<?> fromWebPath(String path)
         {
            return facesFacet.getResourceForWebPath(path);
         }
      };
   }

   @Override
   public List<Resource<?>> install(Project project)
   {
      throw new IllegalStateException("Stub!");
   }

   @Override
   public boolean installed(Project project)
   {
      throw new IllegalStateException("Stub!");
   }

}
