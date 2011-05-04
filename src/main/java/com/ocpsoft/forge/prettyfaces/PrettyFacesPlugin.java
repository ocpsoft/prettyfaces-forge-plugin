package com.ocpsoft.forge.prettyfaces;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.enterprise.event.Event;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.jboss.forge.parser.xml.XMLParser;
import org.jboss.forge.project.Project;
import org.jboss.forge.project.facets.FacetInstallationAborted;
import org.jboss.forge.project.facets.WebResourceFacet;
import org.jboss.forge.resources.DirectoryResource;
import org.jboss.forge.resources.FileResource;
import org.jboss.forge.resources.Resource;
import org.jboss.forge.resources.ResourceFilter;
import org.jboss.forge.scaffold.AccessStrategy;
import org.jboss.forge.scaffold.events.ScaffoldGeneratedResources;
import org.jboss.forge.shell.ShellColor;
import org.jboss.forge.shell.ShellMessages;
import org.jboss.forge.shell.ShellPrintWriter;
import org.jboss.forge.shell.ShellPrompt;
import org.jboss.forge.shell.events.InstallFacets;
import org.jboss.forge.shell.plugins.Alias;
import org.jboss.forge.shell.plugins.Command;
import org.jboss.forge.shell.plugins.DefaultCommand;
import org.jboss.forge.shell.plugins.Option;
import org.jboss.forge.shell.plugins.PipeOut;
import org.jboss.forge.shell.plugins.Plugin;
import org.jboss.forge.shell.plugins.RequiresProject;
import org.jboss.forge.spec.javaee.FacesFacet;
import org.jboss.forge.spec.javaee.util.ServletUtil;
import org.jboss.shrinkwrap.descriptor.impl.base.Strings;
import org.jboss.shrinkwrap.descriptor.spi.Node;
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
   private static final AutomapTarget CUSTOM_MAPPING = new AutomapTarget("custom", "?", null);
   private final Project project;
   private final Event<InstallFacets> installFacets;
   private final ShellPrintWriter out;
   private final ShellPrompt prompt;

   @Inject
   public PrettyFacesPlugin(final Project project, final Event<InstallFacets> event, final ShellPrintWriter writer,
            final ShellPrompt prompt)
   {
      this.project = project;
      this.installFacets = event;
      this.out = writer;
      this.prompt = prompt;
   }

   @DefaultCommand
   public void status(final PipeOut out)
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

   public void handleScaffoldGeneration(@Observes final ScaffoldGeneratedResources event)
   {
      if (project.hasFacet(PrettyFacesFacet.class))
      {
         WebResourceFacet web = project.getFacet(WebResourceFacet.class);

         List<Resource<?>> resources = event.getResources();
         AccessStrategy strategy = event.getProvider().getAccessStrategy();

         List<ResourceMappingRequest> resourceRequests = new ArrayList<ResourceMappingRequest>();
         for (Resource<?> resource : resources)
         {
            for (DirectoryResource d : web.getWebRootDirectories())
            {
               if ((resource != null) && resource.getFullyQualifiedName().startsWith(d.getFullyQualifiedName()))
               {
                  Map<String, AutomapTarget> mappings = new HashMap<String, AutomapTarget>();

                  List<String> webPaths = strategy.getWebPaths(resource);
                  for (String path : webPaths)
                  {
                     mappings.put(path, getAutomapTarget(path));
                  }
                  ResourceMappingRequest request = new ResourceMappingRequest(resource, mappings);
                  resourceRequests.add(request);
                  break;
               }
            }
         }

         if (!resourceRequests.isEmpty())
         {
            out.println();
            ShellMessages.info(out, out.renderColor(ShellColor.BOLD, "PrettyFaces detected generated Resources:")
                     + " This event provides an" +
                     " opportunity to create URL-mappings for web-accessible resources in your project. If you" +
                     " do not wish to perform URL-rewriting, then you may skip this step; otherwise, continue" +
                     " by selecting the resource for which you'd like to generate a \"pretty URL\", and follow" +
                     " the instructions. For more information, please visit [ http://ocpsoft.com/prettyfaces/ ]");

            resourceRequests.add(null);
            while (!resourceRequests.isEmpty())
            {
               out.println();
               ResourceMappingRequest request = prompt
                        .promptChoiceTyped(
                                       "Select one of the following candidate resources to continue" +
                                                " (or press " + out.renderColor(ShellColor.ITALIC, "ENTER")
                                                + " to skip.)",
                                 resourceRequests, null);

               out.println();
               if (request != null)
               {
                  Resource<?> resource = request.getResource();
                  ShellMessages.info(out, "Generating URL-mapping for ["
                                    + out.renderColor(ShellColor.GREEN,
                                             request.getResource().getName()) + "]");
                  out.println();

                  Map<String, AutomapTarget> mappings = request.getMappings();

                  List<AutomapTarget> views = new ArrayList<AutomapTarget>();
                  views.addAll(mappings.values());

                  String message = "Select the pre-configured mapping that most closely matches your" +
                           " application's needs, or select "
                           + out.renderColor(ShellColor.BOLD, "\"" + CUSTOM_MAPPING + "\"")
                           + " to use your own settings.";

                  if (views.size() > 1)
                  {
                     message = "This resource is accessible by multiple URLs. " + message;
                  }
                  views.add(CUSTOM_MAPPING);

                  AutomapTarget choice = prompt.promptChoiceTyped(message, views, views.get(0));

                  if (CUSTOM_MAPPING.equals(choice))
                  {
                     ArrayList<String> viewIds = new ArrayList<String>();
                     viewIds.addAll(mappings.keySet());

                     out.println();
                     String viewId = promptChoiceForViewId(resource.getName(), viewIds);

                     out.println();
                     String pattern = prompt
                              .prompt("Choose an inbound pattern to use in place of the original URL [e.g: '"
                                       + out.renderColor(ShellColor.BOLD, "/login") + "' or '"
                                       + out.renderColor(ShellColor.BOLD, "/item/")
                                       + out.renderColor(ShellColor.GREEN, "#{id}") + "']."
                                       + " Note that " + out.renderColor(ShellColor.GREEN, "#{param}")
                                       + " declarations will convert query-parameters to path-parameters"
                                       + " (See http://bit.ly/prettyparams for more information):");

                     out.println();
                     String id = prompt.prompt("The mapping-ID [e.g: 'login' or 'home']"
                                    + " (See http://bit.ly/prettymapping for more information):");

                     out.println();
                     String parentId = prompt.promptCompleter(out.renderColor(ShellColor.BOLD, "(OPTIONAL)")
                              + " The parent mapping-ID [e.g: 'base' or 'home']." +
                              " Use the URL-mapping with the given ID as this mapping's parent ( See" +
                              " http://bit.ly/prettyparent for more infromation):", new MappingIdCompleter(project));

                     if (Strings.isNullOrEmpty(id))
                     {
                        id = getUniqueMappingId(id);
                     }
                     mapUrl(viewId, pattern, id, parentId, true);
                  }
                  else
                  {
                     mapUrl(choice.getViewId(), choice.getPattern(), choice.getId(), null, true);
                  }
                  resourceRequests.remove(request);
               }
               else
               {
                  break;
               }
            }

            ShellMessages.info(out, "To continue customizing URL-mappings, use the 'prettyfaces' command,"
                     + " or you may 'edit "
                     + project.getFacet(PrettyFacesFacet.class).getConfigFile().getFullyQualifiedName() + "'."
                     + " For more information, please visit [ http://ocpsoft.com/prettyfaces/ ]");

         }
      }
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
                  out.println();
                  viewId = promptChoiceForViewId(viewId, webPaths);
               }
            }
         }

         List<String> viewMappings = faces.getFaceletsViewMapping();
         for (String mapping : viewMappings)
         {
            Matcher matcher = ServletUtil.mappingToRegex(mapping).matcher(viewId);
            if (matcher.matches())
            {
               System.out.println(viewId);
            }
         }
      }

      AutomapTarget target = getAutomapTarget(viewId);
      mapUrl(target.getViewId(), target.getPattern(), target.getId(), null, true);
   }

   private AutomapTarget getAutomapTarget(String viewId)
   {
      if (!viewId.startsWith("/"))
      {
         viewId = "/" + viewId;
      }

      List<String> prefixes = new ArrayList<String>();

      WebResourceFacet web = project.getFacet(WebResourceFacet.class);
      for (DirectoryResource dir : web.getWebRootDirectories())
      {
         ResourceFilter filter = new ResourceFilter()
         {
            @Override
            public boolean accept(final Resource<?> resource)
            {
               return (resource instanceof DirectoryResource) && resource.exists();
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

         id = getUniqueMappingId(id);

         if ("/index".equals(pattern))
         {
            pattern = "/";
         }
         return new AutomapTarget(pattern, viewId, id);
      }
      else
      {
         return null;
      }
   }

   private String getUniqueMappingId(String id)
   {
      if (Strings.isNullOrEmpty(id))
      {
         id = "automatic";
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
      return id;
   }

   private String promptChoiceForViewId(String originalPath, final List<String> accessibleURLs)
   {
      originalPath = prompt.promptChoiceTyped(
               "This resource [" + out.renderColor(ShellColor.BOLD, originalPath)
                        + "] is accessible by multiple URLs. Select the URL that most closely"
                        + " matches your application's needs. (See http://bit.ly/prettymapping for more"
                        + " information):", accessibleURLs, accessibleURLs.get(0));
      return originalPath;
   }

   protected Matcher getMatcher(final String viewId, final List<String> prefixes)
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
            @Option(name = "resource",
                     completer = ViewIdCompleter.class, description = "The server resource (ViewID) to be displayed"
                              + " (See http://bit.ly/prettymapping for more information.)", required = true) final String viewId,
            @Option(name = "pattern", description = "the URL pattern [e.g: '/', '/login', or '/view/#{param}']"
                     + " (See http://bit.ly/prettymapping for more information.)",
                     required = true) final String pattern,
            @Option(name = "id",
                     description = "The mapping-ID"
                              + " (See http://bit.ly/prettymapping for more information.)", required = true) final String id,
            @Option(name = "parentId",
                     completer = MappingIdCompleter.class, description = "Parent mapping ID to inherit from"
                              + " (See http://bit.ly/prettyparent for more information.)") final String parentId,
            @Option(name = "outbound", description = "Rewrite outbound URLs matching this viewId?"
                     + " (See http://bit.ly/disableOutbound for more information.)",
                     defaultValue = "true") final boolean outbound)
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

      if ((parentId != null) && !parentId.trim().isEmpty())
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
      out.println();
      ShellMessages.success(out, out.renderColor(ShellColor.BOLD, newMapping.getPattern())
               + out.renderColor(ShellColor.ITALIC, " -> ") + newMapping.getViewId()
               + " [" + (Strings.isNullOrEmpty(newMapping.getId()) ? "" : "id=" + newMapping.getId() + ", ")
               + (Strings.isNullOrEmpty(newMapping.getParentId()) ? "" : "id=" + newMapping.getId() + ", ")
               + "outbound=" + newMapping.isOutbound() + "]");
   }

   @Command("action")
   public void mapUrlAction(
            final PipeOut out,
            @Option(name = "mappingId", completer = MappingIdCompleter.class, description = "mapping to which the action will be added", required = true) final String mappingId,
            @Option(name = "methodExpression", description = "the EL action method expression (surround with quotes)", required = true) final String actionExpression,
            @Option(name = "phaseId", description = "the JSF lifecycle action phase [default: RESTORE_VIEW]") final PhaseId phaseId,
            @Option(name = "onPostback", description = "invoke action on form POST [default: true]", defaultValue = "true") final boolean onPostback)
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
   public void listMappings(final PipeOut out,
            @Option(name = "sort", shortName = "s") final boolean sort,
            @Option(name = "all", shortName = "a") final boolean showAll)
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
            public int compare(final UrlMapping l, final UrlMapping r)
            {
               if ((l.getPattern() == null) || (r.getPattern() == null))
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

   @Command("faces-message-propagation")
   public void multiPageMessagesSupport(final PipeOut out,
            @Option(description = "Enable or disable multi-page-messages support"
                     + " (See http://bit.ly/multipage for more information)") final Action action)
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
   public void removeMapping(final PipeOut out,
            @Option(name = "id", completer = MappingIdCompleter.class,
                     description = "the mapping id to remove from configuration", required = true) final String id)
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

   @Command(value = "setup", help = "Install PrettyFaces into the current project.")
   public void setup(final PipeOut out)
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
