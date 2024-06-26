/*
 * Copyright (C) 2006 Rob Manning
 * manningr@users.sourceforge.net
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package net.sourceforge.squirrel_sql.client.session.action.sqlscript.prefs;

import java.io.File;

import net.sourceforge.squirrel_sql.client.Main;
import net.sourceforge.squirrel_sql.client.util.ApplicationFiles;
import net.sourceforge.squirrel_sql.fw.util.JsonMarshalUtil;
import net.sourceforge.squirrel_sql.fw.util.StringManager;
import net.sourceforge.squirrel_sql.fw.util.StringManagerFactory;

public class SQLScriptPreferencesManager
{
   private static final StringManager s_stringMgr = StringManagerFactory.getStringManager(SQLScriptPreferencesManager.class);


   private SQLScriptPreferenceBean _scriptPrefs = null;

   public SQLScriptPreferenceBean getPreferences()
   {
      if (null == _scriptPrefs)
      {
         File scriptPrefsJsonFile = new ApplicationFiles().getScriptPrefsJsonFile();

         if(false == scriptPrefsJsonFile.exists() && new File(new ApplicationFiles().getPluginsUserSettingsDirectory(), "sqlscript").exists())
         {
            Main.getApplication().getMessageHandler().showWarningMessage(s_stringMgr.getString("SQLScriptPreferencesManager.warn.check.script.prefs"));
         }

         _scriptPrefs = JsonMarshalUtil.readObjectFromFileSave(scriptPrefsJsonFile, SQLScriptPreferenceBean.class, new SQLScriptPreferenceBean());
      }

      return _scriptPrefs;
   }

   /**
    * Save preferences to disk.
    */
   public void savePrefs()
   {
      JsonMarshalUtil.writeObjectToFile(new ApplicationFiles().getScriptPrefsJsonFile(), _scriptPrefs);
   }

}
