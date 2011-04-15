package com.ocpsoft.forge.prettyfaces;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.enterprise.event.Event;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.jboss.seam.forge.parser.xml.XMLParser;
import org.jboss.seam.forge.project.Project;
import org.jboss.seam.forge.project.facets.FacetInstallationAborted;
import org.jboss.seam.forge.project.facets.WebResourceFacet;
import org.jboss.seam.forge.resources.DirectoryResource;
import org.jboss.seam.forge.resources.FileResource;
import org.jboss.seam.forge.resources.Resource;
import org.jboss.seam.forge.resources.ResourceFilter;
import org.jboss.seam.forge.scaffold.AccessStrategy;
import org.jboss.seam.forge.scaffold.plugins.events.ScaffoldGeneratedResources;
import org.jboss.seam.forge.shell.Shell;
import org.jboss.seam.forge.shell.ShellColor;
import org.jboss.seam.forge.shell.ShellMessages;
import org.jboss.seam.forge.shell.ShellPrintWriter;
import org.jboss.seam.forge.shell.events.InstallFacets;
import org.jboss.seam.forge.shell.plugins.Alias;
import org.jboss.seam.forge.shell.plugins.Command;
import org.jboss.seam.forge.shell.plugins.DefaultCommand;
import org.jboss.seam.forge.shell.plugins.Option;
import org.jboss.seam.forge.shell.plugins.PipeOut;
import org.jboss.seam.forge.shell.plugins.Plugin;
import org.jboss.seam.forge.shell.plugins.RequiresProject;
import org.jboss.seam.forge.spec.javaee6.jsf.FacesFacet;
import org.jboss.seam.forge.spec.javaee6.servlet.ServletFacet;
import org.jboss.shrinkwrap.descriptor.api.Node;
import org.jboss.shrinkwrap.descriptor.impl.base.Strings;
import org.xml.sax.SAXException;

import com.ocpsoft.pretty.faces.annotation.URLAction.PhaseId;
import com.ocpsoft.pretty.faces.config.PrettyConfig;
import com.ocpsoft.pretty.faces.config.mapping.UrlAction;
import com.ocpsoft.pretty.faces.config.mapping.UrlMapping;
import com.ocpsoft.pretty.faces.event.MultiPageMessagesSupport;
import com.ocpsoft.pretty.faces.url.URL;
import com.ocpsoft.pretty.faces.url.URLPatternParser;

@RequiresProject
@Alias("prettyfaces")
public class PrettyFacesPlugin implements Plugin
{
   private final Project project;
   private final Event<InstallFacets> installFacets;
   private final ShellPrintWriter out;

   @Inject
   public PrettyFacesPlugin(Project project, Event<InstallFacets> event, ShellPrintWriter writer)
   {
      this.project = project;
      this.installFacets = event;
      this.out = writer;
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
         out.println("PrettyFaces is not installed. Use 'prettyfaces setup' to get started.");
      }
   }

   public void handleScaffoldGeneration(@Observes ScaffoldGeneratedResources event, Shell shell)
   {
      if (project.hasFacet(PrettyFacesFacet.class))
      {
         List<Resource<?>> resources = event.getResources();
         AccessStrategy strategy = event.getProvider().getAccessStrategy(project);
         ShellMessages.info(shell, "PrettyFaces detected generated Resources:");
         List<String> paths = new ArrayList<String>();
         for (Resource<?> resource : resources)
         {
            List<String> webPaths = strategy.getWebPaths(resource);
            paths.addAll(webPaths);
         }
         paths.add(null);

         String path = "";
         while (path != null && !paths.isEmpty())
         {
            out.println();
            path = shell.promptChoiceTyped(
                     "Create URL-mapping for generated resource?",
                     paths, paths.get(paths.size() - 1));

            if (path != null)
            {
               ShellMessages.info(shell, "Generating URL-mapping for [" + shell.renderColor(ShellColor.GREEN, path)
                        + "]");
               try
               {
                  if (shell.promptBoolean("Attempt to map this URL *automagically*?"))
                  {
                     try
                     {
                        autoMap(path);
                     }
                     catch (Exception e)
                     {
                        ShellMessages.error(shell, e.getMessage());
                        doMappingEvent(shell, path);
                     }
                  }
                  else
                  {
                     doMappingEvent(shell, path);
                  }
                  paths.remove(path);
               }
               catch (Exception e)
               {
                  ShellMessages.error(shell, e.getMessage());
               }
            }
         }
      }
   }

   private void doMappingEvent(Shell shell, String path) throws IOException, SAXException
   {
      mapUrl(
               shell.prompt("The URL pattern [e.g: '/login' or '/home']"),
               path,
               shell.prompt("The mapping-ID [e.g: 'login' or 'home']"),
               shell.promptCompleter("The parent mapping-ID [e.g: 'base' or 'home']",
                        new MappingIdCompleter(project)),
               true);
   }

   @Command("auto-map")
   public void autoMap(
            @Option(name = "resource", completer = ViewIdCompleter.class, description = "the server resource to be displayed", required = true) String viewId)
            throws IOException, SAXException
   {
      if (!viewId.startsWith("/"))
      {
         viewId = "/" + viewId;
      }
      // Attempt to convert the view resource into a faces resource
      if (project.hasFacet(FacesFacet.class))
      {
         FacesFacet faces = project.getFacet(FacesFacet.class);

         List<String> suffixes = faces.getFacesSuffixes();
         for (String suffix : suffixes)
         {
            if (viewId.endsWith(suffix))
            {
               List<String> webPaths = faces.getWebPaths(viewId);
               if (!webPaths.isEmpty())
               {
                  if (faces.getResourceForWebPath(webPaths.get(0)) != null)
                     viewId = webPaths.get(0);
               }
            }
         }

         List<String> viewMappings = faces.getFaceletsViewMapping();
         for (String mapping : viewMappings)
         {
            Matcher matcher = Pattern.compile(ServletFacet.mappingToRegex(mapping)).matcher(viewId);
            if (matcher.matches())
            {
               System.out.println(viewId);
            }
         }
      }

      List<String> prefixes = new ArrayList<String>();

      WebResourceFacet web = project.getFacet(WebResourceFacet.class);
      for (DirectoryResource dir : web.getWebRootDirectories())
      {
         ResourceFilter filter = new ResourceFilter()
         {
            @Override
            public boolean accept(Resource<?> resource)
            {
               return resource instanceof DirectoryResource && resource.exists();
            }
         };

         for (Resource<?> d : dir.listResources(filter))
         {
            prefixes.add(d.getName());
         }
      }

      Matcher matcher = getMatcher(viewId, prefixes);
      if (matcher.matches())
      {
         String pattern = matcher.group(2);
         String id = pattern.replaceAll("/", "-");
         while (id.startsWith("-"))
         {
            id = id.substring(1);
         }

         PrettyFacesFacet pf = project.getFacet(PrettyFacesFacet.class);
         PrettyConfig prettyConfig = pf.getPrettyConfig();
         UrlMapping mapping = prettyConfig.getMappingById(id);

         int i = 1;
         while (mapping != null)
         {
            id = id + i;
            mapping = prettyConfig.getMappingById(id);
         }

         if ("/index".equals(pattern))
         {
            pattern = "/";
         }
         mapUrl(pattern, viewId, id, null, true);
      }
      else
      {
         throw new RuntimeException("Unable to parse URL scheme. Use 'prettyfaces mapping' to map this URL manually.");
      }

   }

   protected Matcher getMatcher(String viewId, List<String> prefixes)
   {
      String prefixList = Strings.join(prefixes, "|/");
      if (!prefixes.isEmpty())
      {
         prefixList = "|/" + prefixList;
      }

      Matcher matcher = Pattern.compile("^(/faces|/pages|/views" + prefixList + "){0,2}(/.*?)(\\.\\w+){0,1}+$")
               .matcher(
                        viewId);
      return matcher;
   }

   @Command("mapping")
   public void mapUrl(
            @Option(name = "pattern", description = "the URL pattern", required = true) String pattern,
            @Option(name = "viewId", completer = ViewIdCompleter.class, description = "the server resource to be displayed", required = true) String viewId,
            @Option(name = "id", description = "the mapping id", required = true) String id,
            @Option(name = "parentId", completer = MappingIdCompleter.class, description = "parent mapping to inherit from") String parentId,
            @Option(name = "outbound", description = "rewrite outbound URLs matching this viewId", defaultValue = "true") boolean outbound)
                     throws IOException, SAXException
   {
      assertInstalled();

      PrettyFacesFacet pf = project.getFacet(PrettyFacesFacet.class);
      PrettyConfig prettyConfig = pf.getPrettyConfig();
      List<UrlMapping> mappings = prettyConfig.getMappings();

      // verify not already mapped
      for (UrlMapping m : mappings)
      {
         if (m.getPattern().equals(pattern))
         {
            throw new RuntimeException("Pattern is already mapped by the " + m.toString());
         }
      }

      Node config = pf.getConfig();
      Node m = config.create("url-mapping");

      if (id != null)
         m.attribute("id", id);

      if (outbound == false)
         m.attribute("outbound", outbound);

      m.create("pattern").attribute("value", pattern);
      m.create("view-id").attribute("value", viewId);

      if (parentId != null && !parentId.trim().isEmpty())
      {
         UrlMapping parent = prettyConfig.getMappingById(parentId);
         if (parent == null)
         {
            throw new RuntimeException("No parent with id of [" + parentId + "]");
         }
         // validate the pattern
         new URLPatternParser(parent.getPattern() + pattern);
         m.attribute("parentId", parentId);
      }
      new URLPatternParser(pattern).matches(new URL(""));

      pf.saveConfig(config);

      UrlMapping newMapping = pf.getPrettyConfig().getMappingById(id);
      ShellMessages.success(out, out.renderColor(ShellColor.BOLD, newMapping.getPattern())
               + out.renderColor(ShellColor.ITALIC, " -> ") + newMapping.getViewId()
               + " [" + (Strings.isNullOrEmpty(newMapping.getId()) ? "" : "id=" + newMapping.getId() + ", ")
               + (Strings.isNullOrEmpty(newMapping.getParentId()) ? "" : "id=" + newMapping.getId() + ", ")
               + "outbound=" + newMapping.isOutbound() + "]");
   }

   @Command("action")
   public void mapUrlAction(
            PipeOut out,
            @Option(name = "mappingId", completer = MappingIdCompleter.class, description = "mapping to which the action will be added", required = true) String mappingId,
            @Option(name = "methodExpression", description = "the EL action method expression (surround with quotes)", required = true) String actionExpression,
            @Option(name = "phaseId", description = "the JSF lifecycle action phase [default: RESTORE_VIEW]") PhaseId phaseId,
            @Option(name = "onPostback", description = "invoke action on form POST [default: true]", defaultValue = "true") boolean onPostback,
            boolean inbound) throws IOException, SAXException
   {
      assertInstalled();
      if (!project.hasFacet(FacesFacet.class))
      {
         ShellMessages.info(out, "This feature requires JavaServer(tm) Faces.");
         installFacets.fire(new InstallFacets(true, FacesFacet.class));
      }

      PrettyFacesFacet pf = project.getFacet(PrettyFacesFacet.class);
      PrettyConfig prettyConfig = pf.getPrettyConfig();

      UrlMapping m = prettyConfig.getMappingById(mappingId);
      for (UrlAction a : m.getActions())
      {
         if (a.getAction().getELExpression().equals(actionExpression))
         {
            throw new RuntimeException("Action already exists [" + a + "]");
         }
      }

      Node config = pf.getConfig();
      Node mapping = config.getSingle("url-mapping@id=" + mappingId);
      Node action = mapping.create("action").text(actionExpression);
      if (onPostback == false)
      {
         action.attribute("onPostback", onPostback);
      }
      if (phaseId != null)
      {
         action.attribute("phaseId", phaseId);
      }

      pf.saveConfig(config);
      ShellMessages.success(out, "Added new action [action" +
               ", phaseId=\"" + (phaseId == null ? PhaseId.ANY_PHASE : phaseId) + "\"" +
               ", onPostback=\"" + (onPostback) + "\"]" +
               " for mapping [" + mappingId + "]");
   }

   @Command("list-mappings")
   public void listMappings(PipeOut out,
            @Option(name = "sort", shortName = "s") boolean sort,
            @Option(name = "all", shortName = "a") boolean showAll
            ) throws IOException, SAXException
   {
      assertInstalled();
      PrettyFacesFacet pf = project.getFacet(PrettyFacesFacet.class);
      PrettyConfig config = pf.getPrettyConfig();

      List<UrlMapping> mappings = new ArrayList<UrlMapping>();
      mappings.addAll(config.getMappings());

      if (sort)
      {
         Collections.sort(mappings, new Comparator<UrlMapping>()
         {
            @Override
            public int compare(UrlMapping l, UrlMapping r)
            {
               if (l.getPattern() == null || r.getPattern() == null)
               {
                  if (l.getPattern() != null)
                     return -1;
                  if (r.getPattern() != null)
                     return 1;
                  return 0;
               }
               return l.getPattern().compareTo(r.getPattern());
            }
         });
      }

      for (UrlMapping m : mappings)
      {
         out.println(
                  out.renderColor(ShellColor.BOLD, m.getPattern())
                           + out.renderColor(ShellColor.ITALIC, " -> ") + m.getViewId()
                           + " [" + (Strings.isNullOrEmpty(m.getId()) ? "" : "id=" + m.getId() + ", ")
                           + (Strings.isNullOrEmpty(m.getParentId()) ? "" : "id=" + m.getId() + ", ")
                           + "outbound=" + m.isOutbound() + "]");

         if (showAll)
         {
            out.println();
            out.println(ShellColor.BLUE, m.toString());
            out.println();
         }

      }

      if (mappings.isEmpty())
      {
         ShellMessages.info(out, "Nothing to list.");
      }
   }

   // confirmed working
   @Command("faces-message-propagation")
   public void multiPageMessagesSupport(PipeOut out, Action action)
   {
      assertInstalled();

      switch (action)
      {
      case INSTALL:
         try
         {
            if (!project.hasFacet(FacesFacet.class))
            {
               ShellMessages.info(out, "This feature requires JavaServer(tm) Faces.");
               installFacets.fire(new InstallFacets(true, FacesFacet.class));
            }

            FacesFacet facesFacet = project.getFacet(FacesFacet.class);
            FileResource<?> facesConfigFile = facesFacet.getConfigFile();
            Node facesConfig = XMLParser.parse(facesConfigFile.getResourceInputStream());

            Node lifecycle = facesConfig.getOrCreate("lifecycle");
            List<Node> list = lifecycle.get("phase-listener");
            Node messagesListener = null;
            for (Node n : list)
            {
               if (MultiPageMessagesSupport.class.getName().equals(n.text()))
               {
                  messagesListener = n;
               }
            }

            if (messagesListener == null)
            {
               messagesListener = lifecycle.create("phase-listener").text(MultiPageMessagesSupport.class.getName());
               facesConfigFile.setContents(XMLParser.toXMLString(facesConfig));
            }
            ShellMessages.success(out, "MultiPageMessagesListener is installed.");
         }
         catch (FacetInstallationAborted e)
         {
         }

         break;
      case REMOVE:

         if (project.hasFacet(FacesFacet.class))
         {
            FacesFacet facesFacet = project.getFacet(FacesFacet.class);
            FileResource<?> facesConfigFile = facesFacet.getConfigFile();
            Node facesConfig = XMLParser.parse(facesConfigFile.getResourceInputStream());

            Node lifecycle = facesConfig.getSingle("lifecycle");
            if (lifecycle != null)
            {
               List<Node> list = lifecycle.get("phase-listener");
               Node messagesListener = null;
               for (Node n : list)
               {
                  if (MultiPageMessagesSupport.class.getName().equals(n.text()))
                  {
                     messagesListener = n;
                  }
               }

               if (messagesListener != null)
               {
                  lifecycle.removeSingle(messagesListener);
                  if (lifecycle.children().isEmpty())
                  {
                     facesConfig.removeSingle(lifecycle);
                  }
                  facesConfigFile.setContents(XMLParser.toXMLString(facesConfig));
                  ShellMessages.success(out, "Removed MultiPageMessagesListener.");
                  return;
               }
            }
            ShellMessages.info(out, "No action required. MultiPageMessagesListener not installed.");
         }
         else
         {
            ShellMessages.info(out, "No action required. JSF is not installed.");
         }
         break;
      }

   }

   @Command("remove-mapping")
   public void removeMapping(
            PipeOut out,
            @Option(name = "id",
                     completer = MappingIdCompleter.class,
                     description = "the mapping id",
                     required = true) String id
            ) throws IOException, SAXException
   {
      assertInstalled();

      PrettyFacesFacet pf = project.getFacet(PrettyFacesFacet.class);
      PrettyConfig prettyConfig = pf.getPrettyConfig();

      UrlMapping mapping = prettyConfig.getMappingById(id);
      if (mapping == null)
      {
         throw new RuntimeException("No mapping found for id [" + id
                  + "]. To see a list of mappings, type 'prettyfaces list-mappings'");
      }

      Node config = pf.getConfig();
      Node m = config.removeSingle("url-mapping@id=" + id);
      if (m == null)
      {
         throw new RuntimeException("Could not remove " + mapping);
      }

      pf.saveConfig(config);
      ShellMessages.success(out, "Removed " + mapping);
   }

   // confirmed working
   @Command("setup")
   public void setup(PipeOut out)
   {
      if (!project.hasFacet(PrettyFacesFacet.class))
         installFacets.fire(new InstallFacets(PrettyFacesFacet.class));

      if (project.hasFacet(PrettyFacesFacet.class))
         ShellMessages.success(out, "PrettyFaces is configured.");
   }

   private void assertInstalled()
   {
      if (!project.hasFacet(PrettyFacesFacet.class))
         throw new RuntimeException("PrettyFaces is not installed. Use 'prettyfaces setup' to get started.");
   }
}
