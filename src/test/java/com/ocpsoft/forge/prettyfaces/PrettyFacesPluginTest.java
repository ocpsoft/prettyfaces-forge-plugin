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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;

import javax.enterprise.event.Event;
import javax.inject.Inject;

import org.jboss.arquillian.api.Deployment;
import org.jboss.seam.forge.project.Project;
import org.jboss.seam.forge.project.facets.WebResourceFacet;
import org.jboss.seam.forge.project.services.FacetFactory;
import org.jboss.seam.forge.resources.FileResource;
import org.jboss.seam.forge.resources.Resource;
import org.jboss.seam.forge.scaffold.plugins.events.ScaffoldGeneratedResources;
import org.jboss.seam.forge.spec.javaee6.jsf.FacesFacet;
import org.jboss.seam.forge.test.AbstractShellTest;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;

import com.ocpsoft.pretty.faces.config.PrettyConfig;
import com.ocpsoft.pretty.faces.config.mapping.UrlMapping;

/**
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 * 
 */
public class PrettyFacesPluginTest extends AbstractShellTest
{
   @Inject
   private FacetFactory factory;

   @Inject
   private Event<ScaffoldGeneratedResources> event;

   @Deployment
   public static JavaArchive getDeployment()
   {
      return AbstractShellTest.getDeployment().addPackages(true, PrettyFacesPlugin.class.getPackage());
   }

   @Test
   public void testInstallPrettyfaces() throws Exception
   {
      Project p = initializePrettyFacesProject();

      assertTrue(p.hasFacet(PrettyFacesFacet.class));
   }

   private Project initializePrettyFacesProject() throws IOException
   {
      Project p = initializeJavaProject();

      queueInputLines("", "", "4", "8");
      getShell().execute("prettyfaces setup");
      return p;
   }

   @Test
   public void testRegex() throws Exception
   {
      PrettyFacesPlugin plugin = new PrettyFacesPlugin(initializeJavaProject(), null, getShell(), getShell());

      Matcher matcher = plugin.getMatcher("/faces/index.xhtml", new ArrayList<String>());
      assertTrue(matcher.matches());
      assertEquals("/index", matcher.group(2));

      matcher = plugin.getMatcher("/index.xhtml", new ArrayList<String>());
      assertTrue(matcher.matches());
      assertEquals("/index", matcher.group(2));

      matcher = plugin.getMatcher("/index", new ArrayList<String>());
      assertTrue(matcher.matches());
      assertEquals("/index", matcher.group(2));

      matcher = plugin.getMatcher("/faces/scaffold/customer/view.xhtml", Arrays.asList("scaffold"));
      assertTrue(matcher.matches());
      assertEquals("/customer/view", matcher.group(2));
   }

   @Test
   public void testAutoMap() throws Exception
   {
      Project project = initializePrettyFacesProject();

      project.installFacet(factory.getFacet(FacesFacet.class));

      PrettyFacesPlugin plugin = new PrettyFacesPlugin(project, null, getShell(), getShell());

      WebResourceFacet web = project.getFacet(WebResourceFacet.class);
      FileResource<?> child = (FileResource<?>) web.getWebRootDirectory().getChild("index.xhtml");
      child.createNewFile();

      plugin.autoMap(child.getName());

      PrettyFacesFacet pf = project.getFacet(PrettyFacesFacet.class);
      PrettyConfig prettyConfig = pf.getPrettyConfig();

      assertEquals(1, prettyConfig.getMappings().size());
      UrlMapping m = prettyConfig.getMappings().get(0);

      assertEquals("index", m.getId());
      assertEquals("/", m.getPattern());
      assertEquals("/index.jsf", m.getViewId());
   }

   @Test
   public void testListenToEventPlainHtmlWithJSFInstalled() throws Exception
   {
      Project project = initializePrettyFacesProject();

      project.installFacet(factory.getFacet(FacesFacet.class));

      WebResourceFacet web = project.getFacet(WebResourceFacet.class);
      FileResource<?> child = (FileResource<?>) web.getWebRootDirectory().getChild("index.html");
      FileResource<?> child2 = (FileResource<?>) web.getWebRootDirectory().getChild("foo.xhtml");
      child.createNewFile();
      child2.createNewFile();

      List<Resource<?>> resources = new ArrayList<Resource<?>>();
      resources.add(child);
      resources.add(child2);

      queueInputLines("1", "", "2", "", "");
      event.fire(new ScaffoldGeneratedResources(new MockScaffoldProvider(project.getFacet(FacesFacet.class)), resources));

      PrettyFacesFacet pf = project.getFacet(PrettyFacesFacet.class);
      PrettyConfig prettyConfig = pf.getPrettyConfig();

      assertEquals(2, prettyConfig.getMappings().size());
      UrlMapping m = prettyConfig.getMappings().get(0);

      assertEquals("index", m.getId());
      assertEquals("/", m.getPattern());
      assertEquals("/index.html", m.getViewId());

      m = prettyConfig.getMappings().get(1);

      assertEquals("foo", m.getId());
      assertEquals("/foo", m.getPattern());
      assertEquals("/faces/foo.xhtml", m.getViewId());
   }
}
