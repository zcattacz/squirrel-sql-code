package net.sourceforge.squirrel_sql.plugins.postgres.gui;
/*
* Copyright (C) 2007 Daniel Regli & Yannick Winiger
* http://sourceforge.net/projects/squirrel-sql
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

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import net.sourceforge.squirrel_sql.fw.gui.GUIUtils;
import net.sourceforge.squirrel_sql.fw.util.StringManager;
import net.sourceforge.squirrel_sql.fw.util.StringManagerFactory;

public class MessageDialog extends JDialog {
    /**
     * The text field of the main panel
     */
    protected JTextArea _messageTextArea;
    protected JScrollPane scrollPane;
    /**
     * The buttons of the button panel
     */
    protected JButton _closeButton;

    protected boolean _autoScrolling = true;


    /**
     * Internationalized strings for this class.
     */
    private static final StringManager s_stringMgr = StringManagerFactory.getStringManager(AbstractPostgresDialog.class);

    protected interface i18n {
        String CLOSE_BUTTON_LABEL = s_stringMgr.getString("MessageDialog.closeButtonLabel");
    }


    public MessageDialog() {
        init();
    }


    /**
     * Creates the UI for this dialog.
     */
    protected void init() {
        setModal(true);
        setSize(500, 250);

        Container contentPane = super.getContentPane();
        contentPane.setLayout(new BorderLayout());

        _messageTextArea = new JTextArea();
        _messageTextArea.setLineWrap(true);
        _messageTextArea.setEditable(false);
        scrollPane = new JScrollPane(_messageTextArea);
        contentPane.add(scrollPane, BorderLayout.CENTER);

        _closeButton = new JButton(i18n.CLOSE_BUTTON_LABEL);
        _closeButton.setEnabled(false);
        replaceCloseListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                setVisible(false);
            }
        });
        contentPane.add(_closeButton, BorderLayout.SOUTH);
    }


    public void replaceCloseListener(ActionListener listener) {
        if (listener == null) throw new IllegalArgumentException("ActionListener cannot be null");
        for (ActionListener l : _closeButton.getListeners(ActionListener.class)) {
            _closeButton.removeActionListener(l);
        }
        _closeButton.addActionListener(listener);
    }


    public void writeLine(String text) {
        _messageTextArea.append(text + "\n");
        if (_autoScrolling) scrollToBottom();
    }


    public void writeEmptyLine() {
        _messageTextArea.append("\n");
        if (_autoScrolling) scrollToBottom();
    }


    public void enableCloseButton() {
        GUIUtils.processOnSwingEventThread(new Runnable() {
            public void run() {
                _closeButton.setEnabled(true);
            }
        });
    }


    public void scrollToBottom() {
        GUIUtils.processOnSwingEventThread(new Runnable() {
            public void run() {
                _messageTextArea.setCaretPosition(_messageTextArea.getText().length());
            }
        });
    }
}
