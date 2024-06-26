package net.sourceforge.squirrel_sql.client.gui.db.modifyaliases;

import java.io.File;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;
import javax.swing.JOptionPane;

import net.sourceforge.squirrel_sql.client.Main;
import net.sourceforge.squirrel_sql.client.gui.db.AliasInternalFrame;
import net.sourceforge.squirrel_sql.client.gui.db.AliasWindowFactory;
import net.sourceforge.squirrel_sql.client.gui.db.SQLAlias;
import net.sourceforge.squirrel_sql.client.gui.db.aliasdndtree.AliasDndTreeHandler;
import net.sourceforge.squirrel_sql.client.util.ApplicationFiles;
import net.sourceforge.squirrel_sql.client.util.IdentifierFactory;
import net.sourceforge.squirrel_sql.fw.gui.GUIUtils;
import net.sourceforge.squirrel_sql.fw.gui.action.fileexport.ProgressAbortDialog;
import net.sourceforge.squirrel_sql.fw.id.IIdentifierFactory;
import net.sourceforge.squirrel_sql.fw.util.StringManager;
import net.sourceforge.squirrel_sql.fw.util.StringManagerFactory;
import net.sourceforge.squirrel_sql.fw.util.Utilities;
import net.sourceforge.squirrel_sql.fw.util.log.ILogger;
import net.sourceforge.squirrel_sql.fw.util.log.LoggerController;
import org.apache.commons.lang3.StringUtils;

public class ModifyMultipleAliasesCtrl
{
   private static final StringManager s_stringMgr = StringManagerFactory.getStringManager(ModifyMultipleAliasesCtrl.class);

   private static final ILogger s_log = LoggerController.createLogger(ModifyMultipleAliasesCtrl.class);
   public static final String LOG_PREFIX_MULTIPLE_ALIASES_UPDATE = "MULTIPLE ALIASES UPDATE: ";


   private final ModifyMultipleAliasesDlg _dlg;
   private AliasChangesHandler _aliasChangesHandler;

   public ModifyMultipleAliasesCtrl()
   {
      _dlg = new ModifyMultipleAliasesDlg();


      AliasDndTreeHandler modifyAliasesTreeHandler = new AliasDndTreeHandler(_dlg.treeAliasesToModify);

      _dlg.btnEditTemplateAlias.addActionListener(e -> onEditAliases(modifyAliasesTreeHandler));
      _dlg.btnApplyChanges.addActionListener(e -> onApplyChanges(modifyAliasesTreeHandler));
      _dlg.btnCancel.addActionListener(e -> close());

      updateApplyButton();

      GUIUtils.initLocation(_dlg, 800, 800);
      GUIUtils.enableCloseByEscape(_dlg);

      _dlg.setVisible(true);
   }

   private void onApplyChanges(AliasDndTreeHandler modifyAliasesTreeHandler)
   {
      // FOR AliasChangesHandler.applyChanges() TEST ONLY
      //SQLAlias newSelectedAlias = Main.getApplication().getWindowManager().getAliasesListInternalFrame().getAliasesList().getSelectedAlias(null);
      //IIdentifierFactory factory = IdentifierFactory.getInstance();
      //SQLAlias newAlias = Main.getApplication().getAliasesAndDriversManager().createAlias(factory.createIdentifier());
      //newAlias.assignFrom(newSelectedAlias, false);
      //_aliasChangesHandler.applyChanges(newAlias);
      //
      //AliasInternalFrame modifyMultipleSheet = AliasWindowFactory.getModifyMultipleSheet(newAlias, _dlg);
      //modifyMultipleSheet.setVisible(true);


      File aliasesBackupFile = null;
      try
      {
         s_log.info(LOG_PREFIX_MULTIPLE_ALIASES_UPDATE + "##################################################################");
         s_log.info(LOG_PREFIX_MULTIPLE_ALIASES_UPDATE + "########### Start applying modifications to multiple Aliases.");
         aliasesBackupFile = backupAliases();

         s_log.info(LOG_PREFIX_MULTIPLE_ALIASES_UPDATE + "Applying the following modifications to the Aliases named below:");
         for (String changeReportLine : _aliasChangesHandler.getChangeReport().getString().split("\n"))
         {
            s_log.info(LOG_PREFIX_MULTIPLE_ALIASES_UPDATE + changeReportLine);
         }

         for (SQLAlias sqlAlias : modifyAliasesTreeHandler.getSqlAliasList())
         {
            _aliasChangesHandler.applyChanges(sqlAlias);
            Main.getApplication().getWindowManager().getAliasesListInternalFrame().getAliasesList().getAliasTreeInterface().aliasNodeChanged(sqlAlias);
            s_log.info(LOG_PREFIX_MULTIPLE_ALIASES_UPDATE + "Modifying Alias: \"" + sqlAlias.getName() +   "\"" + " (internal Identifier=" + sqlAlias.getIdentifier() + ")");
         }
         s_log.info(LOG_PREFIX_MULTIPLE_ALIASES_UPDATE + "########### Finished applying modifications to multiple Aliases.");
         s_log.info(LOG_PREFIX_MULTIPLE_ALIASES_UPDATE + "##################################################################");

         String msg = s_stringMgr.getString("ModifyMultipleAliasesCtrl.success.message", new ApplicationFiles().getDatabaseAliasesFile(), aliasesBackupFile.getAbsolutePath());
         Main.getApplication().getMessageHandler().showMessage(msg);

      }
      catch (Exception e)
      {
         s_log.error(LOG_PREFIX_MULTIPLE_ALIASES_UPDATE + "Failed to modify multiple Aliases", e);

         if(null != aliasesBackupFile)
         {
            String msg = s_stringMgr.getString("ModifyMultipleAliasesCtrl.fail.message.with.backup", new ApplicationFiles().getDatabaseAliasesFile(), aliasesBackupFile.getAbsolutePath());
            Main.getApplication().getMessageHandler().showErrorMessage(msg, e);
         }
         else
         {
            String msg = s_stringMgr.getString("ModifyMultipleAliasesCtrl.fail.message.without.backup", new ApplicationFiles().getDatabaseAliasesFile());
            Main.getApplication().getMessageHandler().showErrorMessage(msg, e);
         }
      }
      finally
      {
         close();
      }
   }

   private void close()
   {
      _dlg.setVisible(false);
      _dlg.dispose();
   }

   private File backupAliases()
   {
      ProgressAbortDialog progressAbortDialog = new ProgressAbortDialog(_dlg, s_stringMgr.getString("ModifyMultipleAliasesCtrl.alias.backup"), null);

      File[] retRef = new File[1];
      Main.getApplication().getThreadPool().addTask(() -> retRef[0] = doBackupAliases(progressAbortDialog));

      progressAbortDialog.setModal(true);
      progressAbortDialog.setVisible(true);

      return retRef[0];
   }

   private static File doBackupAliases(ProgressAbortDialog progressAbortDialog)
   {
      try
      {
         progressAbortDialog.setTaskStatus(s_stringMgr.getString("ModifyMultipleAliasesCtrl.prepare.aliases.backup.begin.save.existing"));
         File aliasesFileToBackUp = Main.getApplication().saveAliases();
         progressAbortDialog.setTaskStatus(s_stringMgr.getString("ModifyMultipleAliasesCtrl.prepare.aliases.backup.finished.save.existing"));

         progressAbortDialog.setTaskStatus(s_stringMgr.getString("ModifyMultipleAliasesCtrl.preparing.aliases.backup.file"));
         String datePostfix = LocalDateTime.now().format(DateTimeFormatter.ofPattern("__yyyy-MM-dd__HH-mm-ss"));
         File databaseAliasesBackupDir = new ApplicationFiles().getDatabaseAliasesBackupDir();
         String aliasesBackupFileName = ApplicationFiles.ALIASES_FILE_NAME + datePostfix + "." +  ApplicationFiles.ALIASES_FILE_NAME_EXTENSION;
         databaseAliasesBackupDir.mkdirs();

         File backupFile = new File(databaseAliasesBackupDir, aliasesBackupFileName);
         progressAbortDialog.setTaskStatus(s_stringMgr.getString("ModifyMultipleAliasesCtrl.begin.write.alias.backup.file", backupFile.getAbsolutePath()));
         Files.copy(aliasesFileToBackUp.toPath(), backupFile.toPath());
         //Thread.sleep(2000);
         s_log.info(LOG_PREFIX_MULTIPLE_ALIASES_UPDATE + "Backuped Aliases file " + new ApplicationFiles().getDatabaseAliasesFile().getAbsolutePath() + " to " + backupFile.getAbsolutePath());
         progressAbortDialog.setTaskStatus(s_stringMgr.getString("ModifyMultipleAliasesCtrl.finished.write.alias.backup.file", backupFile.getAbsolutePath()));

         File[] files = databaseAliasesBackupDir.listFiles((dir, name) -> StringUtils.startsWithIgnoreCase(name, ApplicationFiles.ALIASES_FILE_NAME));

         if(10 < files.length)
         {
            progressAbortDialog.setTaskStatus(s_stringMgr.getString("ModifyMultipleAliasesCtrl.begin.cleaning.backups"));

            int delCount = 0;
            for (File file : files)
            {
               if(   false == file.isDirectory()
                  && file.lastModified() < System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1))
               {
                  Files.delete(file.toPath());
                  ++delCount;
               }
            }
            progressAbortDialog.setTaskStatus(s_stringMgr.getString("ModifyMultipleAliasesCtrl.finished.cleaning.backups", delCount));
            //Thread.sleep(20000);
         }

         //Thread.sleep(2000);

         return backupFile;
      }
      catch(Exception e)
      {
         throw Utilities.wrapRuntime(e);
      }
      finally
      {
         progressAbortDialog.closeProgressDialog();
      }
   }

   private void onEditAliases(AliasDndTreeHandler modifyAliasesTreeHandler)
   {
      SQLAlias selectedAlias = modifyAliasesTreeHandler.getSelectedAlias();

      if(null == selectedAlias)
      {
         JOptionPane.showMessageDialog(_dlg, s_stringMgr.getString("ModifyMultipleAliasesCtrl.no.alias.selected"));
         return;
      }

      IIdentifierFactory factory = IdentifierFactory.getInstance();
      SQLAlias newAliasToEdit = Main.getApplication().getAliasesAndDriversManager().createAlias(factory.createIdentifier());
      newAliasToEdit.assignFrom(selectedAlias, false);

      // This flag might have the wrong initial value because it was saved.
      // We initialize it here correctly.
      newAliasToEdit.getSchemaProperties().setSchemaTableWasCleared_transientForMultiAliasModificationOnly(false);

      AliasInternalFrame modifyMultipleSheet = AliasWindowFactory.getModifyMultipleSheet(newAliasToEdit, _dlg);
      modifyMultipleSheet.setOkListener(() -> onAliasSheetOk(selectedAlias, newAliasToEdit));
      modifyMultipleSheet.setVisible(true);
   }

   private void onAliasSheetOk(SQLAlias uneditedAlias, SQLAlias editedAlias)
   {
      try
      {
         AliasChangesHandler aliasChangesHandler = AliasChangesFinder.findChanges(uneditedAlias, editedAlias);

         _dlg.txtChangeReport.setText(null);
         if(false == aliasChangesHandler.isEmpty())
         {
            _dlg.txtChangeReport.setText(aliasChangesHandler.getChangeReport().getString());
            _aliasChangesHandler = aliasChangesHandler;
            _dlg._tabbedPane.setSelectedIndex(1); // The report tab.
            updateApplyButton();
         }


      }
      catch (Exception e)
      {
         throw Utilities.wrapRuntime(e);
      }
   }

   private void updateApplyButton()
   {
      _dlg.btnApplyChanges.setEnabled(null != _aliasChangesHandler);
   }

}
