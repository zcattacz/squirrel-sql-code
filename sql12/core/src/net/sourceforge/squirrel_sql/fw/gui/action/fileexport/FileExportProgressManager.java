package net.sourceforge.squirrel_sql.fw.gui.action.fileexport;

import net.sourceforge.squirrel_sql.client.Main;
import net.sourceforge.squirrel_sql.client.session.ISession;
import net.sourceforge.squirrel_sql.client.util.codereformat.CodeReformator;
import net.sourceforge.squirrel_sql.client.util.codereformat.CodeReformatorConfigFactory;
import net.sourceforge.squirrel_sql.fw.gui.GUIUtils;
import net.sourceforge.squirrel_sql.fw.sql.ProgressAbortCallback;
import net.sourceforge.squirrel_sql.fw.util.StringManager;
import net.sourceforge.squirrel_sql.fw.util.StringManagerFactory;

public class FileExportProgressManager
{
   private static final StringManager s_stringMgr = StringManagerFactory.getStringManager(FileExportProgressManager.class);

   private final ISession _session;
   private final String _sqlsJoined;
   private final ExportFileProvider _exportFileProvider;
   private ProgressAbortDialog progressDialog;

   public FileExportProgressManager(ISession session, String sqlsJoined, ExportFileProvider exportFileProvider)
   {
      _session = session;
      _sqlsJoined = sqlsJoined;
      _exportFileProvider = exportFileProvider;
   }

   public ProgressAbortCallback getOrCreateProgressCallback()
   {
      return getOrCreateProgressCallback(null);
   }
   public ProgressAbortCallback getOrCreateProgressCallback(DisplayReachedCallBack displayReachedCallBack)
   {
      if (null == progressDialog)
      {
         createProgressAbortDialog(displayReachedCallBack);
      }
      return progressDialog;
   }

   /**
    * Create and show a new  progress monitor with the ability to cancel the task.
    * @param displayReachedCallBack
    */
   private void createProgressAbortDialog(DisplayReachedCallBack displayReachedCallBack)
   {
      GUIUtils.processOnSwingEventThread(() -> showProgressDialog(displayReachedCallBack), true);
   }

   private void showProgressDialog(DisplayReachedCallBack displayReachedCallBack)
   {
      CodeReformator cr = new CodeReformator(CodeReformatorConfigFactory.createConfig(_session));

      String reformatedSQL = cr.reformat(_sqlsJoined);

      String targetFile = _exportFileProvider.getExportFile().getAbsolutePath();

      String title = s_stringMgr.getString("CreateFileOfCurrentSQLCommand.progress.title", targetFile);
      progressDialog = new ProgressAbortDialog(Main.getApplication().getMainFrame(), title, targetFile, reformatedSQL, 0, () -> onCancel(), displayReachedCallBack);

      progressDialog.setVisible(true);
   }

   private void onCancel()
   {
      Main.getApplication().getMessageHandler().showWarningMessage(s_stringMgr.getString("FileExportProgressManager.file.export.user.canceled"));

   }

   /**
    * Hide the progress monitor.
    * The progress monitor will not be destroyed.
    */
   public void hideProgressMonitor()
   {
      if (progressDialog != null)
      {
         progressDialog.setVisible(false);
         progressDialog.dispose();
      }
   }

   /**
    * Check, if the user has canceled the task.
    *
    * @return true, if the user has canceled the task, otherwise false.
    */
   public boolean isAborted()
   {
      if (progressDialog != null && progressDialog.isUserCanceled())
      {
         return true;
      }
      return false;

   }

}
