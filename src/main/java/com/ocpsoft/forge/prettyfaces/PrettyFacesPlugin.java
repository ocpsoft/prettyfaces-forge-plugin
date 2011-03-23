package com.ocpsoft.forge.prettyfaces;

import javax.enterprise.event.Event;
import javax.inject.Inject;

import org.jboss.seam.forge.project.Project;
import org.jboss.seam.forge.project.dependencies.Dependency;
import org.jboss.seam.forge.project.dependencies.DependencyBuilder;
import org.jboss.seam.forge.shell.ShellMessages;
import org.jboss.seam.forge.shell.events.InstallFacets;
import org.jboss.seam.forge.shell.plugins.Alias;
import org.jboss.seam.forge.shell.plugins.Command;
import org.jboss.seam.forge.shell.plugins.DefaultCommand;
import org.jboss.seam.forge.shell.plugins.PipeOut;
import org.jboss.seam.forge.shell.plugins.Plugin;
import org.jboss.seam.forge.shell.plugins.RequiresProject;

@Alias("prettyfaces")
@RequiresProject
public class PrettyFacesPlugin implements Plugin
{
   public static final Dependency PF_JSF2 = DependencyBuilder.create("com.ocpsoft:prettyfaces-jsf2");
   public static final Dependency PF_JSF12 = DependencyBuilder.create("com.ocpsoft:prettyfaces-jsf12");
   public static final Dependency PF_JSF11 = DependencyBuilder.create("com.ocpsoft:prettyfaces-jsf11");

   private final Project project;
   private final Event<InstallFacets> installFacets;

   @Inject
   public PrettyFacesPlugin(Project project, Event<InstallFacets> event)
   {
      this.project = project;
      this.installFacets = event;
   }

   @DefaultCommand
   public void status(PipeOut out)
   {
      if (project.hasFacet(PrettyFacesFacet.class))
      {
         out.println("PrettyFaces is installed.");
      }
      else
      {
         out.println("PrettyFaces is not installed. Use 'prettyfaces mapping-add' to map a URL");
      }
   }

   @Command("setup")
   public void setup(PipeOut out)
   {
      if (!project.hasFacet(PrettyFacesFacet.class))
      {
         installFacets.fire(new InstallFacets(PrettyFacesFacet.class));
      }
      if (project.hasFacet(PrettyFacesFacet.class))
         ShellMessages.success(out, "PrettyFaces is configured.");
   }

   @Command("map-url")
   public void mapUrl(PipeOut out)
   {
      throw new RuntimeException("Not yet implemented.");
   }

   @Command("list-mappings")
   public void listMappings(PipeOut out)
   {
      throw new RuntimeException("Not yet implemented.");
   }

   @Command("list-patterns")
   public void listPatterns(PipeOut out)
   {
      throw new RuntimeException("Not yet implemented.");
   }
}
