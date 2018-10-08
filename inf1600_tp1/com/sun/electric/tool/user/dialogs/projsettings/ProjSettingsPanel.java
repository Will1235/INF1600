/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ProjSettingsPanel.java
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

import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.text.Setting;
import com.sun.electric.technology.Technology;
import com.sun.electric.tool.user.dialogs.EDialog;
import com.sun.electric.tool.user.dialogs.ProjectSettingsFrame;

import java.awt.Frame;
import java.util.List;
import javax.swing.JPanel;

/**
 * This class defines a superstructure for a panel in the Preferences dialog.
 */
public class ProjSettingsPanel extends EDialog
{
    private final ProjectSettingsFrame parent;
	protected Technology curTech = Technology.getCurrent();
	protected Library curLib = Library.getCurrent();

	private boolean inited = false;

	public ProjSettingsPanel(ProjectSettingsFrame parent, boolean modal)
	{
		super((Frame)parent.getOwner(), modal);
        this.parent = parent;
	}

	/** return the panel to use for this preferences tab. */
	public JPanel getPanel() { return null; }

	/** return the name of this preferences tab. */
	public String getName() { return ""; }

	/** Method to tell whether this preferences tab has been initialized.
	 * @return true if this preferences tab has been initialized.
	 */
	public boolean isInited() { return inited; }

	/**
	 * Method to mark that this preferences tab has been initialized.
	 */
	public void setInited() { inited = true; }

	/**
	 * Method to return the current technology for use in all preferences tabs.
	 * @return the current technology.
	 */
    public Technology getTech() { return curTech; }

    /**
     * Method to get the boolean value on the Setting object.
     * The object must have been created as "boolean".
     * @param setting setting object.
     * @return the boolean value on the Setting object.
     */
    public boolean getBoolean(Setting setting) { return setting.getBoolean(getContext()); }
    
    /**
     * Method to get the integer value on the Setting object.
     * The object must have been created as "integer".
     * @param setting setting object.
     * @return the integer value on the Setting object.
     */
    public int getInt(Setting setting) { return setting.getInt(getContext()); }
    
    /**
     * Method to get the long value on the Setting object.
     * The object must have been created as "long".
     * @param setting setting object.
     * @return the long value on the Setting object.
     */
    public long getLong(Setting setting) { return setting.getLong(getContext()); }
    
    /**
     * Method to get the double value on the Setting object.
     * The object must have been created as "double".
     * @param setting setting object.
     * @return the double value on the Setting object.
     */
    public double getDouble(Setting setting) { return setting.getDouble(getContext()); }
    
    /**
     * Method to get the string value on the Setting object.
     * The object must have been created as "string".
     * @return the string value on the Setting object.
     */
    public String getString(Setting setting) { return setting.getString(getContext()); }
    
    /**
     * Method to set a new boolean value on Setting object.
     * @param setting Setting object.
     * @param v the new boolean value of Setting object.
     */
    public void setBoolean(Setting setting, boolean v) {
        if (v != setting.getBoolean(getContext()))
            setting.set(getContext(), Boolean.valueOf(v));
    }
    
    /**
     * Method to set a new integer value on Setting object.
     * @param setting Setting object.
     * @param v the new integer value of Setting object.
     */
    public void setInt(Setting setting, int v) {
        if (v != setting.getInt(getContext()))
            setting.set(getContext(), Integer.valueOf(v));
    }
    
    /**
     * Method to set a new long value on Setting object.
     * @param setting Setting object.
     * @param v the new long value of Setting object.
     */
    public void setLong(Setting setting, long v) {
        if (v != setting.getLong(getContext()))
            setting.set(getContext(), Long.valueOf(v));
    }
    
    /**
     * Method to set a new double value on Setting object.
     * @param setting Setting object.
     * @param v the new double value of Setting object.
     */
    public void setDouble(Setting setting, double v) {
        if (v != setting.getDouble(getContext()))
            setting.set(getContext(), Double.valueOf(v));
    }
    
    /**
     * Method to set a new string value on Setting object.
     * @param setting Setting object.
     * @param str the new string value of Setting object.
     */
    public void setString(Setting setting, String str) {
        if (!str.equals(setting.getString(getContext())))
            setting.set(getContext(), str);
    }

    private List<Object> getContext() { return parent.getContext(); }
    
	/**
	 * Method called at the start of the dialog.
	 * Caches current values and displays them in the Frame tab.
	 */
	public void init() {}

	/**
	 * Method called when the "OK" panel is hit.
	 * Updates any changed fields in the Frame tab.
	 */
	public void term() {}

}
