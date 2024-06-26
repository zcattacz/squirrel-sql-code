/*
 * Copyright (C) 2003 Gerd Wagner
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package net.sourceforge.squirrel_sql.plugins.codecompletion;

import net.sourceforge.squirrel_sql.client.session.ExtendedColumnInfo;
import net.sourceforge.squirrel_sql.client.session.ISession;
import net.sourceforge.squirrel_sql.client.session.schemainfo.SchemaInfo;
import net.sourceforge.squirrel_sql.fw.sql.TableColumnInfo;
import net.sourceforge.squirrel_sql.fw.sql.databasemetadata.SQLDatabaseMetaData;
import net.sourceforge.squirrel_sql.fw.util.Utilities;
import net.sourceforge.squirrel_sql.plugins.codecompletion.prefs.CodeCompletionPreferences;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;

public class CodeCompletionTableInfo extends CodeCompletionInfo
{
   private String _tableName;
   private String _tableType;
   private ArrayList<CodeCompletionColumnInfo> _colInfos;
   String _toString;
   private String _catalog;
   private String _schema;
   private boolean _useCompletionPrefs;
   private CodeCompletionPreferences _prefs;
   ////SH add the session object
   private ISession _session;


   //SH add the session object as last parameter
   public CodeCompletionTableInfo(String tableName, String tableType, String catalog, String schema, boolean useCompletionPrefs, CodeCompletionPreferences prefs, ISession session)
   {
      _tableName = tableName;
      _tableType = tableType;
      _catalog = catalog;
      _schema = schema;
      _useCompletionPrefs = useCompletionPrefs;
      _prefs = prefs;
      //SH
      _session = session;

      if(null != _tableType && !"TABLE".equals(_tableType))
      {
         _toString = _tableName  + " (" + _tableType + ")";
      }
      else
      {
         _toString = _tableName;
      }
   }

   void setHasDuplicateNameInDfifferentSchemas()
   {

      String tabNameWithSchemaHint = _tableName + (null == _catalog ? "": " catalog=" + _catalog) + (null == _schema ? "":" schema=" + _schema);

      if(null != _tableType && !"TABLE".equals(_tableType))
      {
         _toString = tabNameWithSchemaHint  + " (" + _tableType + ")";
      }
      else
      {
         _toString = tabNameWithSchemaHint;
      }

   }


   public String getCompareString()
   {
      return _tableName;
   }

   public ArrayList<? extends CodeCompletionInfo> getColumns(SchemaInfo schemaInfo, String colNamePattern)
   {
      try
      {
         if(null == _colInfos || _colInfos.size() == 0)
         {
            ExtendedColumnInfo[] schemColInfos = schemaInfo.getExtendedColumnInfos(_catalog, _schema, _tableName);


            ArrayList<CodeCompletionColumnInfo> colInfosBuf = new ArrayList<>();
            HashSet<String> uniqCols = new HashSet<>();
            for (int i = 0; i < schemColInfos.length; i++)
            {
               if(   (null == _catalog || null == schemColInfos[i].getCatalog() || ("" + _catalog).equalsIgnoreCase("" + schemColInfos[i].getCatalog()))
                  && (null == _schema || null == schemColInfos[i].getSchema() || ("" + _schema).equalsIgnoreCase("" + schemColInfos[i].getSchema()))   )
               {
                  String columnName = schemColInfos[i].getColumnName();
                  String columnType = schemColInfos[i].getColumnType();
                  String remarks = schemColInfos[i].getRemarks();
                  int columnSize = schemColInfos[i].getColumnSize();
                  int decimalDigits = schemColInfos[i].getDecimalDigits();
                  boolean nullable = schemColInfos[i].isNullable();

                  CodeCompletionColumnInfo buf =
                     new CodeCompletionColumnInfo(_tableName, columnName, remarks, columnType, columnSize, decimalDigits, nullable, _prefs);

                  String bufStr = buf.toString();
                  if (!uniqCols.contains(bufStr))
                  {
                     uniqCols.add(bufStr);
                     colInfosBuf.add(buf);
                  }
               }
            }


            //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
            // According to Stefan Hohenstein this block makes column completion work on db2/400 work in special cases where it didn't work before.
            // The code is executed only in the peculiar case when no columns could be found for a table.
            if(colInfosBuf.size() == 0 && _session != null)
            {
               SQLDatabaseMetaData sdmd = _session.getSQLConnection().getSQLMetaData();
               TableColumnInfo[] ti = sdmd.getColumnInfo(_catalog, _schema, _tableName);
               for(int x = 0; x < ti.length; x++)
               {
                  CodeCompletionColumnInfo buf =
                        new CodeCompletionColumnInfo(
                              _tableName,
                              ti[x].getColumnName(),
                              ti[x].getRemarks(),
                              ti[x].getTypeName(),
                              ti[x].getColumnSize(),
                              ti[x].getDecimalDigits(),
                              (ti[x].isNullable().equals("NO")) ? false : true,
                              _prefs
                        );

                  String bufStr = buf.toString();
                  if (!uniqCols.contains(bufStr))
                  {
                     uniqCols.add(bufStr);
                     colInfosBuf.add(buf);
                  }
               }
            }
            //
            //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

            _colInfos = colInfosBuf;
         }

         String trimmedColNamePattern = colNamePattern.trim();

         ArrayList<CodeCompletionColumnInfo> ret = new ArrayList<>();

         if("".equals(trimmedColNamePattern))
         {
            ret = _colInfos;
         }
         else
         {
            for (CodeCompletionColumnInfo colInfo : _colInfos)
            {
               if (colInfo.matchesCompletionStringStart(trimmedColNamePattern, CompletionMatchTypeUtil.matchTypeOf(_useCompletionPrefs, _prefs)))
               {
                  ret.add(colInfo);
               }
            }
         }

         if (_prefs.isSortColumnsAlphabetically())
         {
            ret.sort(Comparator.comparing(codeCompletionColumnInfo -> codeCompletionColumnInfo.getCompareString().toUpperCase()));
         }

         return ret;
      }
      catch (SQLException e)
      {
         throw Utilities.wrapRuntime(e);
      }
   }

   public boolean hasColumns()
   {
      return true;
   }


   public String toString()
   {
      return _toString;
   }

   @Override
   public String getCompletionString()
   {
      return CompletionCaseSpelling.valueOf(_prefs.getTableViewCaseSpelling()).adjustCaseSpelling(super.getCompletionString());
   }

   public String getTableType()
   {
      return _tableType;
   }
}
