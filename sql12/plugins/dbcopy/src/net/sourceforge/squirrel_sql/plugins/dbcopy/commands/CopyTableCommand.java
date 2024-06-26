package net.sourceforge.squirrel_sql.plugins.dbcopy.commands;

/*
 * Copyright (C) 2005 Rob Manning
 * manningr@users.sourceforge.net
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

import net.sourceforge.squirrel_sql.client.Main;
import net.sourceforge.squirrel_sql.client.session.IObjectTreeAPI;
import net.sourceforge.squirrel_sql.fw.sql.*;
import net.sourceforge.squirrel_sql.fw.sql.databasemetadata.SQLDatabaseMetaData;
import net.sourceforge.squirrel_sql.fw.util.StringManager;
import net.sourceforge.squirrel_sql.fw.util.StringManagerFactory;
import net.sourceforge.squirrel_sql.fw.util.log.ILogger;
import net.sourceforge.squirrel_sql.fw.util.log.LoggerController;
import net.sourceforge.squirrel_sql.plugins.dbcopy.DBCopyPlugin;
import net.sourceforge.squirrel_sql.plugins.dbcopy.util.DBUtil;

import java.sql.SQLException;
import java.util.List;

public class CopyTableCommand
{
	private IObjectTreeAPI _objectTreeAPI;
	/**
	 * Current plugin.
	 */
	private final DBCopyPlugin _plugin;

	/** Logger for this class. */
	private final static ILogger log = LoggerController.createLogger(CopyTableCommand.class);

	private static final StringManager s_stringMgr = StringManagerFactory.getStringManager(CopyTableCommand.class);

	/**
	 * Ctor specifying the current session.
	 */
	public CopyTableCommand(IObjectTreeAPI objectTreeAPI, DBCopyPlugin plugin)
	{
		_objectTreeAPI = objectTreeAPI;
		_plugin = plugin;
	}

	/**
	 * Execute this command. Save the session and selected objects in the plugin for use in paste command.
    */
	public void execute()
	{
		if (_objectTreeAPI != null)
		{
			IDatabaseObjectInfo[] dbObjs = _objectTreeAPI.getSelectedDatabaseObjects();
			if (DatabaseObjectType.TABLE_TYPE_DBO.equals(dbObjs[0].getDatabaseObjectType()))
			{
				String catalog = dbObjs[0].getCatalogName();
				String schema = dbObjs[0].getSchemaName();
				if (log.isDebugEnabled())
				{
					log.debug("CopyTableCommand.execute: catalog=" + catalog);
					log.debug("CopyTableCommand.execute: schema=" + schema);
				}
				dbObjs = DBUtil.getTables(_objectTreeAPI.getSession(), catalog, schema, null);
				for (int i = 0; i < dbObjs.length; i++)
				{
					ITableInfo info = (ITableInfo) dbObjs[i];
					if (log.isDebugEnabled())
					{
						log.debug("dbObj[" + i + "] = " + info.getSimpleName());
					}
				}
			}

			_plugin.getSessionInfoProvider().initCopy(_objectTreeAPI.getSession());
			final IDatabaseObjectInfo[] fdbObjs = dbObjs;
			final SQLDatabaseMetaData md = _objectTreeAPI.getSession().getSQLConnection().getSQLMetaData();

			Main.getApplication().getThreadPool().addTask(new Runnable()
			{
				public void run()
				{
					try
					{
						getInsertionOrder(fdbObjs, md);
						_plugin.setPasteMenuEnabled(true);
					}
					catch (Throwable e)
					{
						Main.getApplication().getMessageHandler().showErrorMessage(e);
						log.error("Unexected exception: ", e);
					}
				}
			});

		}
	}

	private void getInsertionOrder(IDatabaseObjectInfo[] dbObjs, SQLDatabaseMetaData md) throws SQLException
	{
		List<ITableInfo> selectedTables = DBUtil.convertObjectArrayToTableList(dbObjs);
		
		// Only concerned about order when more than one table.
		if (selectedTables.size() > 1)
		{
			ProgressCallBack cb = new ProgressCallBackAdaptor(){
				@Override
				public void currentlyLoading(String simpleName)
				{
					Main.getApplication().getMessageHandler().showMessage(s_stringMgr.getString("CopyTablesCommand.loadingPrefix.new") + " " + simpleName);
				}
			};

			selectedTables = SQLUtilities.getInsertionOrder(selectedTables, md, cb);
			_plugin.getSessionInfoProvider().setSourceDatabaseObjects(DBUtil.convertTableToObjectList(selectedTables));

		}
		else
		{
			_plugin.getSessionInfoProvider().setSourceDatabaseObjects(DBUtil.convertTableToObjectList(selectedTables));
		}
	}

}