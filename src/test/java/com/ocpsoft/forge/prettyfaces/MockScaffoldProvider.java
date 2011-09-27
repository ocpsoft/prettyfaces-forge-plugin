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

import org.jboss.forge.parser.java.JavaClass;
import org.jboss.forge.project.facets.BaseFacet;
import org.jboss.forge.resources.Resource;
import org.jboss.forge.scaffold.AccessStrategy;
import org.jboss.forge.scaffold.ScaffoldProvider;
import org.jboss.forge.scaffold.TemplateStrategy;
import org.jboss.forge.spec.javaee.FacesFacet;

/**
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 * 
 */
public class MockScaffoldProvider extends BaseFacet implements ScaffoldProvider
{
   private final FacesFacet facesFacet;

   public MockScaffoldProvider(FacesFacet facet)
   {
      this.facesFacet = facet;
   }

   @Override
   public List<Resource<?>> getGeneratedResources()
   {
      throw new IllegalStateException("Stub!");
   }

   @Override
   public List<Resource<?>> generateTemplates(boolean overwrite)
   {
      throw new IllegalStateException("Stub!");
   }

   @Override
   public AccessStrategy getAccessStrategy()
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
   public boolean install()
   {
      return true;
   }

   @Override
   public boolean isInstalled()
   {
      return true;
   }

   @Override
   public List<Resource<?>> setup(Resource<?> template, boolean overwrite)
   {
      throw new IllegalStateException("Stub!");
   }

   @Override
   public List<Resource<?>> generateIndex(Resource<?> template, boolean overwrite)
   {
      throw new IllegalStateException("Stub!");
   }

   @Override
   public List<Resource<?>> generateFromEntity(Resource<?> template, JavaClass entity, boolean overwrite)
   {
      throw new IllegalStateException("Stub!");
   }

   @Override
   public TemplateStrategy getTemplateStrategy()
   {
      throw new IllegalStateException("Stub!");
   }

}
