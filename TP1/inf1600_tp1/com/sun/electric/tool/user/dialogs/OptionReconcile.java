/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: OptionReconcile.java
 *
 * Copyright (c) 2003 Sun Microsystems and Static Free Software
 *
 * Electric(tm) is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Electric(tm) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Electric(tm); see the file COPYING.  If not, write to
 * the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 * Boston, Mass 02111-1307, USA.
 */
package com.sun.electric.tool.user.dialogs;

import com.sun.electric.database.text.Setting;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.JobException;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.menus.FileMenu.ReadLibrary;

import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSeparator;


/**
 * Class to handle the "Project Setting Reconcile" dialog.
 */
public class OptionReconcile extends EDialog
{
    private Map<Setting,Object> settingsThatChanged;
	private HashMap<JRadioButton,Setting> changedSettings = new HashMap<JRadioButton,Setting>();
    private ArrayList<AbstractButton> currentSettings = new ArrayList<AbstractButton>();
    private ReadLibrary job;

	/** Creates new form Project Settings Reconcile */
	public OptionReconcile(Frame parent, boolean modal, Map<Setting,Object> settingsThatChanged, String libname, ReadLibrary job)
	{
		super(parent, modal);
        this.settingsThatChanged = settingsThatChanged;
        this.job = job;
		initComponents();
        getRootPane().setDefaultButton(ok);

		JPanel optionBox = new JPanel();
		optionBox.setLayout(new GridBagLayout());
		optionPane.setViewportView(optionBox);
		GridBagConstraints gbc = new GridBagConstraints();

		// the second column header: the option description
		gbc.gridx = 1;       gbc.gridy = 0;
		gbc.gridwidth = 1;   gbc.gridheight = 1;
		gbc.weightx = 0.2;   gbc.weighty = 0;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = new Insets(4, 4, 4, 4);
		optionBox.add(new JLabel("SETTING"), gbc);

		// the third column header: the current value
		gbc.gridx = 2;       gbc.gridy = 0;
		gbc.gridwidth = 1;   gbc.gridheight = 1;
		gbc.weightx = 0.2;   gbc.weighty = 0;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = new Insets(4, 4, 4, 4);
		optionBox.add(new JLabel("CURRENT VALUE"), gbc);

		// the fourth column header: the Libraries value
		gbc.gridx = 3;       gbc.gridy = 0;
		gbc.gridwidth = 1;   gbc.gridheight = 1;
		gbc.weightx = 0.2;   gbc.weighty = 0;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = new Insets(4, 4, 4, 4);
		optionBox.add(new JLabel("LIBRARY VALUE"), gbc);

		// the fifth column header: the location of the option
		gbc.gridx = 4;       gbc.gridy = 0;
		gbc.gridwidth = 1;   gbc.gridheight = 1;
		gbc.weightx = 0.2;   gbc.weighty = 0;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = new Insets(4, 4, 4, 4);
		optionBox.add(new JLabel("SETTING LOCATION"), gbc);

		// the separator between the header and the body
		gbc.gridx = 0;       gbc.gridy = 1;
		gbc.gridwidth = 5;   gbc.gridheight = 1;
		gbc.weightx = 1.0;   gbc.weighty = 0;
		gbc.anchor = GridBagConstraints.CENTER;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		optionBox.add(new JSeparator(), gbc);

		int rowNumber = 2;
		for (Map.Entry<Setting,Object> e: settingsThatChanged.entrySet())
		{
            Setting setting = e.getKey();
			Object obj = e.getValue();
            if (obj == null)
                obj = setting.getFactoryValue();
			if (obj.equals(setting.getValue())) continue;

            Object settingValue = setting.getValue();
			String oldValue = settingValue.toString();
            String newValue = obj.toString();
            String[] trueMeaning = setting.getTrueMeaning();
            if (settingValue instanceof Boolean) {
                oldValue = setting.getBoolean() ? "ON" : "OFF";
                boolean b = obj instanceof Boolean ? ((Boolean)obj).booleanValue() : ((Integer)obj).intValue() != 0;
                newValue = b ? "ON" : "OFF";
            } else if (trueMeaning != null) {
                oldValue = trueMeaning[setting.getInt()];
                newValue = trueMeaning[((Integer)obj).intValue()];
            }
            
/*
			// the first column: the "Accept" checkbox
			JCheckBox cb = new JCheckBox("Accept");
			cb.setSelected(true);
			gbc.gridx = 0;       gbc.gridy = rowNumber;
			gbc.gridwidth = 1;   gbc.gridheight = 1;
			gbc.weightx = 0.2;   gbc.weighty = 0;
			gbc.anchor = GridBagConstraints.WEST;
			gbc.fill = GridBagConstraints.NONE;
			optionBox.add(cb, gbc);
			changedOptions.put(cb, meaning);
*/

			// the second column is the option description
			gbc.gridx = 1;       gbc.gridy = rowNumber;
			gbc.gridwidth = 1;   gbc.gridheight = 1;
			gbc.weightx = 0.2;   gbc.weighty = 0;
			gbc.anchor = GridBagConstraints.WEST;
			gbc.fill = GridBagConstraints.NONE;
			optionBox.add(new JLabel(setting.getDescription()), gbc);

			// the third column is the current value
			gbc.gridx = 2;       gbc.gridy = rowNumber;
			gbc.gridwidth = 1;   gbc.gridheight = 1;
			gbc.weightx = 0.2;   gbc.weighty = 0;
			gbc.anchor = GridBagConstraints.WEST;
			gbc.fill = GridBagConstraints.NONE;
            JRadioButton curValue = new JRadioButton(oldValue, false);
            currentSettings.add(curValue);
			optionBox.add(curValue, gbc);
/*
            curValue.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    updateButtonState();
                }});
*/

			// the fourth column is the Libraries value
			gbc.gridx = 3;       gbc.gridy = rowNumber;
			gbc.gridwidth = 1;   gbc.gridheight = 1;
			gbc.weightx = 0.2;   gbc.weighty = 0;
			gbc.anchor = GridBagConstraints.WEST;
			gbc.fill = GridBagConstraints.NONE;
            JRadioButton libValue = new JRadioButton(newValue, true);
            changedSettings.put(libValue, setting);
			optionBox.add(libValue, gbc);
/*
            libValue.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    updateButtonState();
                }});
*/

            ButtonGroup group = new ButtonGroup();
            group.add(curValue);
            group.add(libValue);

			// the fifth column is the location of the option
			gbc.gridx = 4;       gbc.gridy = rowNumber;
			gbc.gridwidth = 1;   gbc.gridheight = 1;
			gbc.weightx = 0.2;   gbc.weighty = 0;
			gbc.anchor = GridBagConstraints.WEST;
			gbc.fill = GridBagConstraints.NONE;
			optionBox.add(new JLabel(setting.getLocation()), gbc);

			rowNumber++;
		}

        optionHeader.setText("Library \""+libname+"\" wants to use the following project settings which differ from the current project settings");        
        pack();
		finishInitialization();
	}

    public void termDialog() {
        Map<Setting,Object> settingsToReconcile = new HashMap<Setting,Object>();
        for(JRadioButton cb : changedSettings.keySet()) {
            if (!cb.isSelected()) continue;
            Setting setting = changedSettings.get(cb);
            settingsToReconcile.put(setting, settingsThatChanged.get(setting));
        }
        new DoReconciliation(settingsToReconcile, job);
    }

	/**
	 * Class to apply changes to tool options in a new thread.
	 */
	private static class DoReconciliation extends Job
	{
        private Map<String,Object> settingsToSerialize = new HashMap<String,Object>();
        private transient ReadLibrary job;
        
        private DoReconciliation(Map<Setting,Object> settingsToReconcile, ReadLibrary job) {
            super("Reconcile Project Settings", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
            this.job = job;
            for (Map.Entry<Setting,Object> e: settingsToReconcile.entrySet()) {
                Setting setting = e.getKey();
                Object newValue = e.getValue();
                settingsToSerialize.put(setting.getXmlPath(), newValue);
            }
            startJob();
        }
        
        @Override
		public boolean doIt() throws JobException
		{
            Map<Setting,Object> settingsToReconcile = new HashMap<Setting,Object>();
            for (Map.Entry<String,Object> e: settingsToSerialize.entrySet()) {
                String xmlPath = e.getKey();
                Object newValue = e.getValue();
                Setting setting = Setting.getSetting(xmlPath);
                if (setting == null) continue;
                settingsToReconcile.put(setting, newValue);
            }
            Setting.finishSettingReconcilation(settingsToReconcile);
			return true;
		}

        @Override
        public void terminateOK() {
            job.startJob();
        }
	}
 
	/** This method is called from within the constructor to
	 * initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
    // <editor-fold defaultstate="collapsed" desc=" Generated Code ">//GEN-BEGIN:initComponents
    private void initComponents()
    {
        java.awt.GridBagConstraints gridBagConstraints;

        ok = new javax.swing.JButton();
        optionPane = new javax.swing.JScrollPane();
        optionHeader = new javax.swing.JLabel();
        ignoreLibraryOptions = new javax.swing.JButton();
        useLibraryOptions = new javax.swing.JButton();

        getContentPane().setLayout(new java.awt.GridBagLayout());

        setTitle("Project Setting Reconciliation");
        setName("");
        addWindowListener(new java.awt.event.WindowAdapter()
        {
            public void windowClosing(java.awt.event.WindowEvent evt)
            {
                closeDialog(evt);
            }
        });

        ok.setText("Use Above Settings");
        ok.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                ok(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.weightx = 0.1;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(ok, gridBagConstraints);

        optionPane.setMinimumSize(new java.awt.Dimension(500, 150));
        optionPane.setPreferredSize(new java.awt.Dimension(650, 150));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(optionPane, gridBagConstraints);

        optionHeader.setText("The new Project Settings are different from the current Project Settings:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(optionHeader, gridBagConstraints);

        ignoreLibraryOptions.setText("Use All Current Settings");
        ignoreLibraryOptions.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                ignoreLibraryOptionsActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.weightx = 0.1;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(ignoreLibraryOptions, gridBagConstraints);

        useLibraryOptions.setText("Use All New Settings");
        useLibraryOptions.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                useLibraryOptionsActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.weightx = 0.1;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(useLibraryOptions, gridBagConstraints);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void useLibraryOptionsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_useLibraryOptionsActionPerformed
        // set all library options selected
        for(JRadioButton b : changedSettings.keySet())
        {
            b.setSelected(true);
        }
        ok(null);
    }//GEN-LAST:event_useLibraryOptionsActionPerformed

	private void ignoreLibraryOptionsActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_ignoreLibraryOptionsActionPerformed
	{//GEN-HEADEREND:event_ignoreLibraryOptionsActionPerformed
		// set all current options selected
        for (AbstractButton b : currentSettings) {
            b.setSelected(true);
        }
        ok(null);
	}//GEN-LAST:event_ignoreLibraryOptionsActionPerformed

	private void ok(java.awt.event.ActionEvent evt)//GEN-FIRST:event_ok
	{//GEN-HEADEREND:event_ok
		termDialog();
		closeDialog(null);
	}//GEN-LAST:event_ok

	/** Closes the dialog */
	private void closeDialog(java.awt.event.WindowEvent evt)//GEN-FIRST:event_closeDialog
	{
		setVisible(false);
		dispose();
	}//GEN-LAST:event_closeDialog

//    private void updateButtonState() {
//        boolean ignoreAllLibOptionsEnabled = false;
//        boolean useAllLibOptionsEnabled = false;
//        for (AbstractButton b : currentSettings) {
//            // if current setting selected, allow user to push "use all lib settings" button
//            if (b.isSelected()) useAllLibOptionsEnabled = true;
//            // if library setting selected, allow user to push "ignore all lib settings" button
//            if (!b.isSelected()) ignoreAllLibOptionsEnabled = true;
//        }
//        useLibraryOptions.setEnabled(useAllLibOptionsEnabled);
//        ignoreLibraryOptions.setEnabled(ignoreAllLibOptionsEnabled);
//    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton ignoreLibraryOptions;
    private javax.swing.JButton ok;
    private javax.swing.JLabel optionHeader;
    private javax.swing.JScrollPane optionPane;
    private javax.swing.JButton useLibraryOptions;
    // End of variables declaration//GEN-END:variables
}
