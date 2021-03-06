/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: VerilogTab.java
 *
 * Copyright (c) 2006 Sun Microsystems and Static Free Software
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
package com.sun.electric.tool.user.dialogs.projsettings;

import com.sun.electric.database.text.Setting;
import com.sun.electric.tool.simulation.Simulation;
import com.sun.electric.tool.user.dialogs.ProjectSettingsFrame;

import javax.swing.JPanel;

/**
 * Class to handle the "Verilog" tab of the Project Settings dialog.
 */
public class VerilogTab extends ProjSettingsPanel
{
    private Setting verilogUseAssignSetting = Simulation.getVerilogUseAssignSetting();
    private Setting verilogUseTriregSetting = Simulation.getVerilogUseTriregSetting();
    
	/** Creates new form VerilogTab */
	public VerilogTab(ProjectSettingsFrame parent, boolean modal)
	{
		super(parent, modal);
		initComponents();
	}

	/** return the panel to use for this preferences tab. */
	public JPanel getPanel() { return verilog; }

	/** return the name of this preferences tab. */
	public String getName() { return "Verilog"; }

	/**
	 * Method called at the start of the dialog.
	 * Caches current values and displays them in the Verilog tab.
	 */
	public void init()
	{
		verUseAssign.setSelected(getBoolean(verilogUseAssignSetting));
		verDefWireTrireg.setSelected(getBoolean(verilogUseTriregSetting));
	}

	/**
	 * Method called when the "OK" panel is hit.
	 * Updates any changed fields in the Verilog tab.
	 */
	public void term()
	{
        setBoolean(verilogUseAssignSetting, verUseAssign.isSelected());
        setBoolean(verilogUseTriregSetting, verDefWireTrireg.isSelected());
	}

	/** This method is called from within the constructor to
	 * initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
    // <editor-fold defaultstate="collapsed" desc=" Generated Code ">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        verilog = new javax.swing.JPanel();
        verUseAssign = new javax.swing.JCheckBox();
        verDefWireTrireg = new javax.swing.JCheckBox();

        getContentPane().setLayout(new java.awt.GridBagLayout());

        setTitle("Tool Options");
        setName("");
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                closeDialog(evt);
            }
        });

        verilog.setLayout(new java.awt.GridBagLayout());

        verUseAssign.setText("Use ASSIGN Construct");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        verilog.add(verUseAssign, gridBagConstraints);

        verDefWireTrireg.setText("Default wire is Trireg");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        verilog.add(verDefWireTrireg, gridBagConstraints);

        getContentPane().add(verilog, new java.awt.GridBagConstraints());

        pack();
    }// </editor-fold>//GEN-END:initComponents

	/** Closes the dialog */
	private void closeDialog(java.awt.event.WindowEvent evt)//GEN-FIRST:event_closeDialog
	{
		setVisible(false);
		dispose();
	}//GEN-LAST:event_closeDialog

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JCheckBox verDefWireTrireg;
    private javax.swing.JCheckBox verUseAssign;
    private javax.swing.JPanel verilog;
    // End of variables declaration//GEN-END:variables

}
