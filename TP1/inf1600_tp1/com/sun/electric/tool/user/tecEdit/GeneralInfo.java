/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: GeneralInfo.java
 * Technology Editor, general factors information
 * Written by Steven M. Rubin, Sun Microsystems.
 *
 * Copyright (c) 2005 Sun Microsystems and Static Free Software
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
package com.sun.electric.tool.user.tecEdit;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.tool.user.tecEdit.Info.SpecialTextDescr;

import java.awt.Color;
import java.util.Iterator;

/**
 * This class defines general information about a technology in the Technology Editor.
 */
public class GeneralInfo extends Info
{
    /** the short name of the technology */         String   shortName;
    /** true if technology is "non-electrical" */   boolean  nonElectrical;
	/** the scale factor of the technology */		double   scale;
    /** is scale factor relvant */                  boolean  scaleRelevant;
    /** factory resolution */                       double   resolution;
    /** default foundry name of the technology */   String   defaultFoundry;
    /** default number of metals */                 int      defaultNumMetals;
	/** the full description of the technology */	String   description;
	/** minimum resistance/capacitance */			double   minRes, minCap;
    /** max series resistance */                    double   maxSeriesResistance;
	/** gate shrinkage for the technology */		double   gateShrinkage;
	/** true to include gates in resistance calc */	boolean  includeGateInResistance;
	/** true to include ground in parasitics */		boolean  includeGround;
	/** Logical effort gate capacitance preference. */double gateCapacitance;
	/** Logical effort wire ratio preference. */	double wireRatio;
	/** Logical effort diff alpha preference. */	double diffAlpha;
	/** the transparent colors in the technology */	Color [] transparentColors;
	/** spice level 1 header */                     String [] spiceLevel1Header;
	/** spice level 2 header */                     String [] spiceLevel2Header;
	/** spice level 3 header */                     String [] spiceLevel3Header;
    /** spacing of connected polys */               double [] conDist;
    /** spacing of non-connected polys */           double [] unConDist;
    /** menu palette */                             Object[][] menuPalette;

	static SpecialTextDescr [] genTextTable =
	{
        new SpecialTextDescr(0,  12, TECHSHORTNAME),
		new SpecialTextDescr(0,   9, TECHSCALE),
        new SpecialTextDescr(0,   6, TECHFOUNDRY),
        new SpecialTextDescr(0,   3, TECHDEFMETALS),
		new SpecialTextDescr(0,   0, TECHDESCRIPT),
		new SpecialTextDescr(0,  -3, TECHSPICEMINRES),
		new SpecialTextDescr(0,  -6, TECHSPICEMINCAP),
		new SpecialTextDescr(0,  -9, TECHMAXSERIESRES),
		new SpecialTextDescr(0, -12, TECHGATESHRINK),
		new SpecialTextDescr(0, -15, TECHGATEINCLUDED),
		new SpecialTextDescr(0, -18, TECHGROUNDINCLUDED),
		new SpecialTextDescr(0, -21, TECHTRANSPCOLORS),
	};

	/**
	 * Method to build the appropriate descriptive information for a layer into
	 * cell "np".  The color is "colorindex"; the stipple array is in "stip"; the
	 * layer style is in "style", the CIF layer is in "ciflayer"; the function is
	 * in "functionindex"; the Calma GDS-II layer is in "gds"; the SPICE resistance is in "spires",
	 * the SPICE capacitance is in "spicap", the SPICE edge capacitance is in "spiecap",
	 * the 3D height is in "height3d", and the 3D thickness is in "thick3d".
	 */
	void generate(Cell np)
	{
		// load up the structure with the current values
		loadTableEntry(genTextTable, TECHSHORTNAME, shortName);
		loadTableEntry(genTextTable, TECHSCALE, Double.valueOf(scale));
        loadTableEntry(genTextTable, TECHFOUNDRY, defaultFoundry);
        loadTableEntry(genTextTable, TECHDEFMETALS, Integer.valueOf(defaultNumMetals));
		loadTableEntry(genTextTable, TECHDESCRIPT, description);
		loadTableEntry(genTextTable, TECHSPICEMINRES, Double.valueOf(minRes));
		loadTableEntry(genTextTable, TECHSPICEMINCAP, Double.valueOf(minCap));
		loadTableEntry(genTextTable, TECHMAXSERIESRES, Double.valueOf(maxSeriesResistance));
		loadTableEntry(genTextTable, TECHGATESHRINK, Double.valueOf(gateShrinkage));
		loadTableEntry(genTextTable, TECHGATEINCLUDED, Boolean.valueOf(includeGateInResistance));
		loadTableEntry(genTextTable, TECHGROUNDINCLUDED, Boolean.valueOf(includeGround));
		loadTableEntry(genTextTable, TECHTRANSPCOLORS, transparentColors);

		// now create those text objects
		createSpecialText(np, genTextTable);
	}

	/**
	 * Method to parse the miscellaneous-info cell in "np" and return a GeneralInfo object that describes it.
	 */
	static GeneralInfo parseCell(Cell np)
	{
		// create and initialize the GRAPHICS structure
		GeneralInfo gi = new GeneralInfo();

		for(Iterator<NodeInst> it = np.getNodes(); it.hasNext(); )
		{
			NodeInst ni = it.next();
			int opt = Manipulate.getOptionOnNode(ni);
			String str = getValueOnNode(ni);
			switch (opt)
			{
				case TECHSHORTNAME:
					gi.shortName = str;
					break;
				case TECHSCALE:
					gi.scale = TextUtils.atof(str);
                    gi.scaleRelevant = true;
					break;
				case TECHFOUNDRY:
					gi.defaultFoundry = str;
					break;
				case TECHDEFMETALS:
					gi.defaultNumMetals = TextUtils.atoi(str);
					break;
				case TECHDESCRIPT:
					gi.description = str;
					break;
				case TECHSPICEMINRES:
					gi.minRes = TextUtils.atof(str);
					break;
				case TECHSPICEMINCAP:
					gi.minCap = TextUtils.atof(str);
					break;
				case TECHMAXSERIESRES:
					gi.maxSeriesResistance = TextUtils.atof(str);
					break;
				case TECHGATESHRINK:
					gi.gateShrinkage = TextUtils.atof(str);
					break;
				case TECHGATEINCLUDED:
					gi.includeGateInResistance = str.equalsIgnoreCase("yes");
					break;
				case TECHGROUNDINCLUDED:
					gi.includeGround = str.equalsIgnoreCase("yes");
					break;
				case TECHTRANSPCOLORS:
					Variable var = ni.getVar(TRANSLAYER_KEY);
					if (var != null)
					{
						Color [] colors = getTransparentColors((String)var.getObject());
						if (colors != null) gi.transparentColors = colors;
					}
					break;
				case CENTEROBJ:
					break;
				default:
					LibToTech.pointOutError(ni, np);
					System.out.println("Unknown object in miscellaneous-information cell (" + ni + ")");
					break;
			}
		}
		return gi;
	}

	static Color [] getTransparentColors(String str)
	{
		String [] colorNames = str.split("/");
		Color [] colors = new Color[colorNames.length];
		for(int i=0; i<colorNames.length; i++)
		{
			String colorName = colorNames[i].trim();
			String [] rgb = colorName.split(",");
			if (rgb.length != 3) return null;
			int r = TextUtils.atoi(rgb[0]);
			int g = TextUtils.atoi(rgb[1]);
			int b = TextUtils.atoi(rgb[2]);
			colors[i] = new Color(r, g, b);
		}
		return colors;
	}

	static String makeTransparentColorsLine(Color [] trans)
	{
		String str = "The Transparent Colors: ";
		for(int j=0; j<trans.length; j++)
		{
			if (j != 0) str += " /";
			str += " " + trans[j].getRed() + "," + trans[j].getGreen() + "," + trans[j].getBlue();
		}
		return str;
	}
}
