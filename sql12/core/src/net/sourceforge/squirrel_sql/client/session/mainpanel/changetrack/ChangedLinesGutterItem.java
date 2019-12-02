package net.sourceforge.squirrel_sql.client.session.mainpanel.changetrack;

import net.sourceforge.squirrel_sql.client.session.ISQLEntryPanel;
import net.sourceforge.squirrel_sql.fw.gui.CopyToClipboardUtil;
import net.sourceforge.squirrel_sql.fw.util.Utilities;

import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.text.BadLocationException;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;

public class ChangedLinesGutterItem implements GutterItem
{
   private ChangeTrackPanel _changeTrackPanel;
   private final ISQLEntryPanel _sqlEntry;
   private final int _beginLine;
   private final int _changedLinesCount;
   private final String _formerText;

   public ChangedLinesGutterItem(ChangeTrackPanel changeTrackPanel, ISQLEntryPanel sqlEntry, int beginLine, int changedLinesCount, String formerText)
   {
      _changeTrackPanel = changeTrackPanel;
      _sqlEntry = sqlEntry;
      _beginLine = beginLine;
      _changedLinesCount = changedLinesCount;
      _formerText = formerText;
   }

   public void leftPaint(Graphics g)
   {
      Rectangle rect = GutterItemUtil.getLeftGutterBoundsForLines(_sqlEntry, _beginLine, _changedLinesCount);

      if(null == rect)
      {
         return;
      }

      Color buf = g.getColor();
      g.setColor(getColor());
      g.fillRect(rect.x, rect.y, rect.width, rect.height);
      g.setColor(buf);

   }

   private Color getColor()
   {
      return new Color(200, 180, 230);
   }

   @Override
   public void rightPaint(Graphics g)
   {
      Rectangle mark =  GutterItemUtil.getRightGutterMarkBoundsForLines(_changeTrackPanel, _sqlEntry, _beginLine, _changedLinesCount);

      GutterItemUtil.paintRightGutterMark(g, mark, getColor());
   }

   @Override
   public void leftGutterMouseMoved(MouseEvent e, CursorHandler cursorHandler)
   {
      Rectangle rect = GutterItemUtil.getLeftGutterBoundsForLines(_sqlEntry, _beginLine, _changedLinesCount);

      if(null == rect)
      {
         return;
      }

      cursorHandler.setClickable(rect.intersects(new Rectangle(e.getPoint(), new Dimension(1,1))));
   }

   @Override
   public void rightMoveCursorWhenHit(MouseEvent e)
   {
      Rectangle mark =  GutterItemUtil.getRightGutterMarkBoundsForLines(_changeTrackPanel, _sqlEntry, _beginLine, _changedLinesCount);

      if(null == mark)
      {
         return;
      }


      if(mark.intersects(new Rectangle(e.getPoint(), new Dimension(1,1))))
      {
         try
         {
            int lineStartPosition = _sqlEntry.getTextComponent().getLineStartOffset(_beginLine - 1);
            _sqlEntry.setCaretPosition(lineStartPosition);
         }
         catch (BadLocationException ex)
         {
         }
      }
   }

   @Override
   public void rightGutterMouseMoved(MouseEvent e, CursorHandler cursorHandler)
   {
      Rectangle mark =  GutterItemUtil.getRightGutterMarkBoundsForLines(_changeTrackPanel, _sqlEntry, _beginLine, _changedLinesCount);

      if(null == mark)
      {
         return;
      }


      cursorHandler.setClickable(mark.intersects(new Rectangle(e.getPoint(), new Dimension(1,1))));
   }


   @Override
   public void leftShowPopupIfHit(MouseEvent me, JPanel trackingGutterLeft)
   {
      Rectangle rect = GutterItemUtil.getLeftGutterBoundsForLines(_sqlEntry, _beginLine, _changedLinesCount);

      if(null == rect)
      {
         return;
      }

      if(rect.intersects(new Rectangle(me.getPoint(), new Dimension(1,1))))
      {
         JPopupMenu popupMenu = new JPopupMenu();
         RevertablePopupPanel revertablePopupPanel = new RevertablePopupPanel(_formerText, _sqlEntry.getTextComponent().getFont());
         revertablePopupPanel.btnCopy.addActionListener(ae -> CopyToClipboardUtil.copyToClip(_formerText));

         revertablePopupPanel.btnRevert.addActionListener(ae -> onRevert(popupMenu));

         popupMenu.add(revertablePopupPanel);
         popupMenu.show(trackingGutterLeft, ChangeTrackPanel.LEFT_GUTTER_WIDTH, me.getY());

      }

   }

   private void onRevert(JPopupMenu popupMenu)
   {
      try
      {
         int beginPos = _sqlEntry.getTextComponent().getLineStartOffset(_beginLine - 1);
         int endPos = _sqlEntry.getTextComponent().getLineEndOffset(_beginLine + _changedLinesCount - 2);
         _sqlEntry.setSelectionStart(beginPos);
         _sqlEntry.setSelectionEnd(endPos - 1);

         _sqlEntry.replaceSelection(_formerText);

         popupMenu.setVisible(false);
      }
      catch (BadLocationException e)
      {
         throw Utilities.wrapRuntime(e);
      }
   }

}