package com.ocpsoft.forge.prettyfaces;

/**
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 * 
 */
public class AutomapTarget
{
   private final String id;
   private final String viewId;
   private final String pattern;

   public AutomapTarget(String pattern, String viewId, String id)
   {
      this.id = id;
      this.viewId = viewId;
      this.pattern = pattern;
   }

   public String getId()
   {
      return id;
   }

   public String getViewId()
   {
      return viewId;
   }

   public String getPattern()
   {
      return pattern;
   }

   @Override
   public String toString()
   {
      return " " + getPattern() + " -> " + getViewId() + " ";
   }

   @Override
   public int hashCode()
   {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((id == null) ? 0 : id.hashCode());
      result = prime * result + ((pattern == null) ? 0 : pattern.hashCode());
      result = prime * result + ((viewId == null) ? 0 : viewId.hashCode());
      return result;
   }

   @Override
   public boolean equals(Object obj)
   {
      if (this == obj)
         return true;
      if (obj == null)
         return false;
      if (getClass() != obj.getClass())
         return false;
      AutomapTarget other = (AutomapTarget) obj;
      if (id == null)
      {
         if (other.id != null)
            return false;
      }
      else if (!id.equals(other.id))
         return false;
      if (pattern == null)
      {
         if (other.pattern != null)
            return false;
      }
      else if (!pattern.equals(other.pattern))
         return false;
      if (viewId == null)
      {
         if (other.viewId != null)
            return false;
      }
      else if (!viewId.equals(other.viewId))
         return false;
      return true;
   }
}