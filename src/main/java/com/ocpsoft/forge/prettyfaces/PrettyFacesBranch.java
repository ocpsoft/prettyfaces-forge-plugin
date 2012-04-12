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

import org.jboss.forge.project.dependencies.Dependency;
import org.jboss.forge.project.dependencies.DependencyBuilder;

/**
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 *
 */
public enum PrettyFacesBranch
{
   JSF_1_1("JSF 1.1 and Servlet <= 2.3", DependencyBuilder.create("com.ocpsoft:prettyfaces-jsf11")),
   JSF_1_2("JSF 1.2 and Servlet >= 2.4", DependencyBuilder.create("com.ocpsoft:prettyfaces-jsf12")),
   JSF_2_0("JSF 2.0 and Servlet >= 2.5", DependencyBuilder.create("com.ocpsoft:prettyfaces-jsf2")),
   SERVLET_3("Java EE 6 and Servlet >= 3.0", DependencyBuilder.create("com.ocpsoft:prettyfaces-jsf2")),
   PF4("Java EE 6 and Servlet >= 3.0 [experimental]", DependencyBuilder.create("org.ocpsoft:prettyfaces"));

   private Dependency dependency;
   private String name;

   private PrettyFacesBranch(String name, Dependency dep)
   {
      this.name = name;
      this.dependency = dep;
   }

   public Dependency getDependency()
   {
      return dependency;
   }

   @Override
   public String toString()
   {
      return name;
   }
}
