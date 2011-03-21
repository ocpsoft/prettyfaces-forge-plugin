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
package com.ocpsoft.forge.prettyfaces.config;

import org.jboss.shrinkwrap.descriptor.api.Node;
import org.jboss.shrinkwrap.descriptor.impl.base.NodeProviderImplBase;
import org.jboss.shrinkwrap.descriptor.impl.base.XMLExporter;
import org.jboss.shrinkwrap.descriptor.spi.DescriptorExporter;

/**
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 * 
 */
public class PrettyFacesConfig extends NodeProviderImplBase
{
   private final Node root;

   public PrettyFacesConfig(String version)
   {
      super("prettyfaces");
      this.root = new Node("pretty-config")
               .attribute("xmlns", "http://ocpsoft.com/prettyfaces/" + version)
               .attribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance")
               .attribute("xsi:schemaLocation", "http://ocpsoft.com/prettyfaces/" + version + " " +
                        "http://ocpsoft.com/xml/ns/prettyfaces/ocpsoft-pretty-faces-" + version + ".xsd");
   }

   @Override
   public Node getRootNode()
   {
      return root;
   }

   @Override
   protected DescriptorExporter getExporter()
   {
      return new XMLExporter();
   }

}
