package net.sourceforge.squirrel_sql.client.mainframe.action.findprefs;

import net.sourceforge.squirrel_sql.fw.util.StringUtilities;
import org.apache.commons.lang3.StringUtils;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class FindInPreferencesModel
{
   private TreeMap<List<String>, List<PrefComponentInfo>> _componentInfoByPath;

   public FindInPreferencesModel(TreeMap<List<String>, List<PrefComponentInfo>> componentInfoByPath)
   {
      _componentInfoByPath = componentInfoByPath;
   }

   public DefaultMutableTreeNode createFilteredTreeNodes(String filterText)
   {
      DefaultMutableTreeNode root = new DefaultMutableTreeNode("");

      for (Map.Entry<List<String>, List<PrefComponentInfo>> entry : _componentInfoByPath.entrySet())
      {
         DefaultMutableTreeNode parent = root;
         final List<String> pathEntryStrings = entry.getKey();
         for (int i = 0; i < pathEntryStrings.size(); i++)
         {
            if(false == matches(pathEntryStrings.subList(i, pathEntryStrings.size()), filterText))
            {
               // Neither pathEntryStrings.get(i) nor its kids match so the path from here is not needed.
               continue;
            }

            String pathEntryString = pathEntryStrings.get(i);

            boolean found = false;
            for (int j = 0; j < parent.getChildCount(); j++)
            {
               DefaultMutableTreeNode child = (DefaultMutableTreeNode) parent.getChildAt(j);
               final PathEntry pathEntry = (PathEntry) child.getUserObject();
               if(pathEntry.isSame(pathEntryString))
               {
                  parent = child;
                  found = true;
                  break;
               }
            }
            if(false == found)
            {
               final PathEntry newPathEntry = new PathEntry(pathEntryString);
               DefaultMutableTreeNode child = new DefaultMutableTreeNode(newPathEntry);
               parent.add(child);
               newPathEntry.setComponentInfoList(treeNodeToComponentInfoList(child));

               parent = child;
            }
         }
      }

      return root;
   }

   private boolean matches(List<String> path, String filterText)
   {
      if(StringUtilities.isEmpty(filterText, true))
      {
         return true;
      }

      filterText = filterText.trim();
      for (String pathEntry : path)
      {
         if(StringUtils.containsIgnoreCase(pathEntry, filterText))
         {
            return true;
         }
      }
      return false;
   }

   public List<String> treeNodeToComponentPath(DefaultMutableTreeNode node)
   {
      final Object[] pathIncludingRoot = node.getUserObjectPath();

      ArrayList<String> ret = new ArrayList<>();

      for (int i = 1; i < pathIncludingRoot.length; i++)
      {
         String pathEntryString = ((PathEntry)pathIncludingRoot[i]).getPathEntryString();
         ret.add(pathEntryString);
      }
      return ret;
   }

   public PrefComponentInfo treeNodeToFirstComponentInfo(DefaultMutableTreeNode node)
   {
      return treeNodeToComponentInfoList(node).get(0);
   }

   public List<PrefComponentInfo> treeNodeToComponentInfoList(DefaultMutableTreeNode node)
   {
      return _componentInfoByPath.get(treeNodeToComponentPath(node));
   }

   public String getDetailsText(DefaultMutableTreeNode node)
   {
      return ((PathEntry)node.getUserObject()).getPathEntryString();
   }

   public boolean detailsTextNeedsLineWrap(DefaultMutableTreeNode node)
   {
      final PathEntry pathEntry = (PathEntry) node.getUserObject();
      return pathEntry.detailsTextNeedsLineWrap();
   }
}