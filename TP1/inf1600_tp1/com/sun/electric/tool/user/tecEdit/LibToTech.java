/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: LibToTech.java
 * Technology Editor, conversion of technology libraries to technologies
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

import com.sun.electric.database.CellId;
import com.sun.electric.database.geometry.DBMath;
import com.sun.electric.database.geometry.EPoint;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.EDatabase;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.network.Netlist;
import com.sun.electric.database.network.Network;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.PortCharacteristic;
import com.sun.electric.database.text.Setting;
import com.sun.electric.database.text.Version;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.variable.EditWindow_;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.ArcProto;
import com.sun.electric.technology.DRCTemplate;
import com.sun.electric.technology.EdgeH;
import com.sun.electric.technology.EdgeV;
import com.sun.electric.technology.Foundry;
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.PrimitivePort;
import com.sun.electric.technology.SizeOffset;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.Xml;
import com.sun.electric.technology.Xml.ArcLayer;
import com.sun.electric.technology.technologies.Artwork;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.erc.ERC;
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.dialogs.EDialog;
import com.sun.electric.tool.user.dialogs.OpenFile;
import com.sun.electric.tool.user.ui.WindowFrame;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
* This class creates technologies from technology libraries.
*/
public class LibToTech
{    
	/* the meaning of "us_tecflags" */
//	private static final int HASDRCMINWID  =          01;				/* has DRC minimum width information */
//	private static final int HASDRCMINWIDR =          02;				/* has DRC minimum width information */
//	private static final int HASCOLORMAP   =          04;				/* has color map */
//	private static final int HASARCWID     =         010;				/* has arc width offset factors */
//	private static final int HASCIF        =         020;				/* has CIF layers */
//	private static final int HASDXF        =         040;				/* has DXF layers */
//	private static final int HASGDS        =        0100;				/* has Calma GDS-II layers */
//	private static final int HASGRAB       =        0200;				/* has grab point information */
//	private static final int HASSPIRES     =        0400;				/* has SPICE resistance information */
//	private static final int HASSPICAP     =       01000;				/* has SPICE capacitance information */
//	private static final int HASSPIECAP    =       02000;				/* has SPICE edge capacitance information */
//	private static final int HAS3DINFO     =       04000;				/* has 3D height/thickness information */
//	private static final int HASCONDRC     =      010000;				/* has connected design rules */
//	private static final int HASCONDRCR    =      020000;				/* has connected design rules reasons */
//	private static final int HASUNCONDRC   =      040000;				/* has unconnected design rules */
//	private static final int HASUNCONDRCR  =     0100000;				/* has unconnected design rules reasons */
//	private static final int HASCONDRCW    =     0200000;				/* has connected wide design rules */
//	private static final int HASCONDRCWR   =     0400000;				/* has connected wide design rules reasons */
//	private static final int HASUNCONDRCW  =    01000000;				/* has unconnected wide design rules */
//	private static final int HASUNCONDRCWR =    02000000;				/* has unconnected wide design rules reasons */
//	private static final int HASCONDRCM    =    04000000;				/* has connected multicut design rules */
//	private static final int HASCONDRCMR   =   010000000;				/* has connected multicut design rules reasons */
//	private static final int HASUNCONDRCM  =   020000000;				/* has unconnected multicut design rules */
//	private static final int HASUNCONDRCMR =   040000000;				/* has unconnected multicut design rules reasons */
//	private static final int HASEDGEDRC    =  0100000000;				/* has edge design rules */
//	private static final int HASEDGEDRCR   =  0200000000;				/* has edge design rules reasons */
//	private static final int HASMINNODE    =  0400000000;				/* has minimum node size */
//	private static final int HASMINNODER   = 01000000000;				/* has minimum node size reasons */
//	private static final int HASPRINTCOL   = 02000000000;				/* has print colors */

	/* the globals that define a technology */
//	static int           us_tecflags;
//	static INTBIG           us_teclayer_count;
//	static CHAR           **us_teclayer_iname = 0;
//	static CHAR           **us_teclayer_names = 0;
//	static DRCRULES        *us_tecdrc_rules = 0;
//	static INTBIG          *us_tecnode_grab = 0;
//	static INTBIG           us_tecnode_grabcount;

	/**
	 * Method invoked for the "technology edit library-to-tech" command.  Dumps
	 * code if "dumpformat" is nonzero
	 */
	public static void makeTechFromLib()
	{
		GenerateTechnology dialog = new GenerateTechnology();
		dialog.initComponents();
		dialog.setVisible(true);
	}

//	private static class SoftTech extends Technology
//	{
//		private SoftTech(String name, String foundryName, int numMetals)
//		{
//			super(name, Foundry.Type.valueOf(foundryName), numMetals);
//			setNoNegatedArcs();
//		}
//
//        public ArcProto newArcProto(String protoName, double lambdaWidthOffset, double defaultWidth, ArcProto.Function function, ArcInfo.LayerDetails [] ad) {
//			ArcLayer [] arcLayers = new ArcLayer[ad.length];
//			for(int j=0; j<ad.length; j++)
//				arcLayers[j] = new ArcLayer(ad[j].layer.generated, ad[j].width, ad[j].style);
//            return newArcProto(protoName, lambdaWidthOffset, defaultWidth, function, arcLayers);
//        }
//        
//        private void setTheScale(double scale)
//		{
//			setFactoryScale(scale, true);
//		}
//
//        protected void setTechShortName(String techShortName)
//		{
//			super.setTechShortName(techShortName);
//		}
//
//		private void setTransparentColors(Color [] colors)
//		{
//			setFactoryTransparentLayers(colors);
//		}
//        
//        protected void newFoundry(Foundry.Type mode, URL fileURL, String... gdsLayers) {
//            super.newFoundry(mode, fileURL, gdsLayers);
//        }
//	}

	/**
	 * Class to create a technology-library from a technology (in a Job).
	 */
	private static class TechFromLibJob extends Job
	{
        private String newName;
        private String fileName;

        private TechFromLibJob(String newName, boolean alsoXML) {
            super("Make Technology from Technolog Library", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
            this.newName = newName;
            if (alsoXML) {
                // print the technology as XML
                fileName = OpenFile.chooseOutputFile(FileType.XML, "File for Technology's XML Code",
                        newName + ".xml");
            }
            startJob();
		}
        
        @Override
		public boolean doIt()
		{
			makeTech(newName, fileName);
			return true;
		}
    }

	public static Technology makeTech(String newName, String fileName)
	{
		Library lib = Library.getCurrent();

		// get a new name for the technology
		String newTechName = newName;
		boolean modified = false;
		for(;;)
		{
			// search by hand because "gettechnology" handles partial matches
			if (Technology.findTechnology(newTechName) == null) break;
			newTechName += "X";
			modified = true;
		}
		if (modified)
			System.out.println("Warning: already a technology called " + newName + ".  Naming this " + newTechName);

		// get list of dependent libraries
		Library [] dependentLibs = Info.getDependentLibraries(lib);

		// get general information from the "factors" cell
		Cell np = null;
		for(int i=dependentLibs.length-1; i>=0; i--)
		{
			np = dependentLibs[i].findNodeProto("factors");
			if (np != null) break;
		}
		if (np == null)
		{
			System.out.println("Cell with general information, called 'factors', is missing");
			return null;
		}
		GeneralInfo gi = GeneralInfo.parseCell(np);

		// get layer information
		LayerInfo [] lList = extractLayers(dependentLibs);
		if (lList == null) return null;

		// get arc information
		ArcInfo [] aList = extractArcs(dependentLibs, lList);
		if (aList == null) return null;

		// get node information
		NodeInfo [] nList = extractNodes(dependentLibs, lList, aList);
		if (nList == null) return null;

		for(NodeInfo ni: nList)
			ni.arcsShrink = ni.func == PrimitiveNode.Function.PIN && !ni.wipes;

		// create the pure-layer associations
		for(int i=0; i<lList.length; i++)
		{
			if (lList[i].pseudo) continue;

			// find the pure layer node
			for(int j=0; j<nList.length; j++)
			{
				if (nList[j].func != PrimitiveNode.Function.NODE) continue;
				NodeInfo.LayerDetails nld = nList[j].nodeLayers[0];
				if (nld.layer == lList[i])
				{
					lList[i].pureLayerNode = nList[j];
					break;
				}
			}
		}

        Xml.Technology t = makeXml(newTechName, gi, lList, nList, aList);
        if (fileName != null)
            t.writeXml(fileName);
        Technology tech = new Technology(t);
        tech.setup();
        
		// switch to this technology
		System.out.println("Technology " + tech.getTechName() + " built.");
		WindowFrame.updateTechnologyLists();
		return tech;
    }
    
//	public static Technology makeTech(String newName, String fileName)
//	{
//		Library lib = Library.getCurrent();
//
//		// get a new name for the technology
//		String newTechName = newName;
//		boolean modified = false;
//		for(;;)
//		{
//			// search by hand because "gettechnology" handles partial matches
//			if (Technology.findTechnology(newTechName) == null) break;
//			newTechName += "X";
//			modified = true;
//		}
//		if (modified)
//			System.out.println("Warning: already a technology called " + newName + ".  Naming this " + newTechName);
//
//		// get list of dependent libraries
//		Library [] dependentLibs = Info.getDependentLibraries(lib);
//
//		// initialize the state of this technology
////		us_tecflags = 0;
//
//		// get general information from the "factors" cell
//		Cell np = null;
//		for(int i=dependentLibs.length-1; i>=0; i--)
//		{
//			np = dependentLibs[i].findNodeProto("factors");
//			if (np != null) break;
//		}
//		if (np == null)
//		{
//			System.out.println("Cell with general information, called 'factors', is missing");
//			return null;
//		}
//		GeneralInfo gi = GeneralInfo.parseCell(np);
//
//		// get layer information
//		LayerInfo [] lList = extractLayers(dependentLibs);
//		if (lList == null) return null;
//
//		// get arc information
//		ArcInfo [] aList = extractArcs(dependentLibs, lList);
//		if (aList == null) return null;
//
//		// get node information
//		NodeInfo [] nList = extractNodes(dependentLibs, lList, aList);
//		if (nList == null) return null;
//
//		// create the technology
//		SoftTech tech = new SoftTech(newTechName, gi.defaultFoundry, gi.defaultNumMetals);
//        tech.setTechShortName(gi.shortName);
//		tech.setTheScale(gi.scale);
//		tech.setTechDesc(gi.description);
//        tech.setFactoryParasitics(gi.minRes, gi.minCap);
////		tech.setMinResistance(gi.minRes);
////		tech.setMinCapacitance(gi.minCap);
//        Setting.SettingChangeBatch changeBatch = new Setting.SettingChangeBatch();
//        changeBatch.add(tech.getMaxSeriesResistanceSetting(), Double.valueOf(gi.maxSeriesResistance));
//        changeBatch.add(tech.getGateLengthSubtractionSetting(), Double.valueOf(gi.gateShrinkage));
//		changeBatch.add(tech.getGateIncludedSetting(), Boolean.valueOf(gi.includeGateInResistance));
//		changeBatch.add(tech.getGroundNetIncludedSetting(), Boolean.valueOf(gi.includeGround));
////		tech.setGateLengthSubtraction(gi.gateShrinkage);
////		tech.setGateIncluded(gi.includeGateInResistance);
////		tech.setGroundNetIncluded(gi.includeGround);
//		if (gi.transparentColors != null) tech.setTransparentColors(gi.transparentColors);
//
//		// create the layers
//        String[] gdsLayers = new String[lList.length];
//		for(int i=0; i<lList.length; i++)
//		{
//            LayerInfo li = lList[i];
//			Layer lay = Layer.newInstance(tech, li.name, li.desc);
//			lay.setFunction(li.fun, li.funExtra, li.pseudo);
//			lay.setFactoryCIFLayer(li.cif);
//            gdsLayers[i] = li.name + " " + li.gds;
////            mosis.setGDSLayer(lay, li.gds);
//            lay.setFactoryParasitics(li.spiRes, li.spiCap, li.spiECap);
////			lay.setResistance(li.spiRes);
////			lay.setCapacitance(li.spiCap);
////			lay.setEdgeCapacitance(li.spiECap);
//			lay.setDistance(li.height3d);
//			lay.setThickness(li.thick3d);
//			li.generated = lay;
//		}
//        
//        tech.newFoundry(Foundry.Type.MOSIS, null, gdsLayers);
//        
//        Setting.implementSettingChanges(changeBatch);
//        
//		// create the arcs
//		for(int i=0; i<aList.length; i++)
//		{
//			ArcInfo.LayerDetails [] ad = aList[i].arcDetails;
//			ArcProto newArc = tech.newArcProto(aList[i].name, aList[i].widthOffset, aList[i].maxWidth, aList[i].func, ad);
//			newArc.setFactoryFixedAngle(aList[i].fixAng);
//			if (aList[i].wipes) newArc.setWipable(); else newArc.clearWipable();
//			newArc.setFactoryAngleIncrement(aList[i].angInc);
//			newArc.setFactoryExtended(!aList[i].noExtend);
//            if (aList[i].curvable) newArc.setCurvable();
//			ERC.getERCTool().setAntennaRatio(newArc, aList[i].antennaRatio);
//			aList[i].generated = newArc;
//		}
//
//		// create the nodes
//		for(int i=0; i<nList.length; i++)
//		{
//			NodeInfo.LayerDetails [] nd = nList[i].nodeLayers;
//			Technology.NodeLayer [] nodeLayers = new Technology.NodeLayer[nd.length];
//			for(int j=0; j<nd.length; j++)
//			{
//				LayerInfo li = nd[j].layer;
//				Layer lay = li.generated;
//				Technology.TechPoint [] points = nd[j].values;
//				if (nd[j].representation == Technology.NodeLayer.MULTICUTBOX)
//				{
//					nodeLayers[j] = Technology.NodeLayer.makeMulticut(lay, nd[j].portIndex, nd[j].style, points,
//						nd[j].multiXS, nd[j].multiYS, nd[j].multiSep, nd[j].multiSep2D);
//				} else if (nList[i].specialType == PrimitiveNode.SERPTRANS)
//				{
//					nodeLayers[j] = new Technology.NodeLayer(lay, nd[j].portIndex, nd[j].style, nd[j].representation, points,
//						nd[j].lWidth, nd[j].rWidth, nd[j].extendB, nd[j].extendT);
//				} else
//				{
//					nodeLayers[j] = new Technology.NodeLayer(lay, nd[j].portIndex, nd[j].style, nd[j].representation, points);
//				}
//			}
//			PrimitiveNode prim = PrimitiveNode.newInstance(nList[i].name, tech, nList[i].xSize, nList[i].ySize, nList[i].so, nodeLayers);
//			nList[i].generated = prim;
//			prim.setFunction(nList[i].func);
//			if (nList[i].wipes) prim.setWipeOn1or2();
//			if (nList[i].square) prim.setSquare();
//			if (nList[i].lockable) prim.setLockedPrim();
//
//			// add special information if present
//			switch (nList[i].specialType)
//			{
//				case PrimitiveNode.SERPTRANS:
//					prim.setHoldsOutline();
//					prim.setCanShrink();
//					prim.setSpecialValues(nList[i].specialValues);
//					prim.setSpecialType(nList[i].specialType);
//					break;
////				case PrimitiveNode.MULTICUT:
////					prim.setSpecialValues(nList[i].specialValues);
////					prim.setSpecialType(nList[i].specialType);
////					break;
//				case PrimitiveNode.POLYGONAL:
//					prim.setHoldsOutline();
//					prim.setSpecialType(nList[i].specialType);
//					break;
//			}
//
//			// analyze special node function circumstances
//			if (nList[i].func == PrimitiveNode.Function.NODE)
//			{
//				prim.setHoldsOutline();
//			} else if (nList[i].func == PrimitiveNode.Function.PIN)
//			{
//				if (!prim.isWipeOn1or2())
//				{
//					prim.setArcsWipe();
//					prim.setArcsShrink();
//					nList[i].arcsShrink = true;
//				}
//			}
//
//			int numPorts = nList[i].nodePortDetails.length;
//			PrimitivePort [] portList = new PrimitivePort[numPorts];
//			for(int j=0; j<numPorts; j++)
//			{
//				NodeInfo.PortDetails portDetail = nList[i].nodePortDetails[j];
//				int numConns = portDetail.connections.length;
//				ArcProto [] cons = new ArcProto[numConns];
//				for(int k=0; k<numConns; k++)
//					cons[k] = portDetail.connections[k].generated;
//				portList[j] = PrimitivePort.newInstance(tech, prim, cons, portDetail.name,
//					portDetail.angle, portDetail.range, portDetail.netIndex, PortCharacteristic.UNKNOWN,
//					portDetail.values[0].getX(), portDetail.values[0].getY(),
//					portDetail.values[1].getX(), portDetail.values[1].getY());
//			}
//			prim.addPrimitivePorts(portList);
//		}
//
//		// create the pure-layer associations
//		for(int i=0; i<lList.length; i++)
//		{
//			if (lList[i].pseudo) continue;
//
//			// find the pure layer node
//			for(int j=0; j<nList.length; j++)
//			{
//				if (nList[j].func != PrimitiveNode.Function.NODE) continue;
//				NodeInfo.LayerDetails nld = nList[j].nodeLayers[0];
//				if (nld.layer == lList[i])
//				{
//					lList[i].generated.setPureLayerNode(nList[j].generated);
//					lList[i].pureLayerNode = nList[j];
//					break;
//				}
//			}
//		}
//
//		// setup the generic technology to handle all connections
//		Generic.tech.makeUnivList();
//
//		// check technology for consistency
//		checkAndWarn(lList, aList, nList);
//
//        if (fileName != null)
//        	LibToTech.writeXml(fileName, newTechName, gi, lList, nList, aList);
////            dumpToJava(fileName, newTechName, gi, lList, nList, aList);
//
////		// finish initializing the technology
////		if ((us_tecflags&HASGRAB) == 0) us_tecvariables[0].name = 0; else
////		{
////			us_tecvariables[0].name = x_("prototype_center");
////			us_tecvariables[0].value = (CHAR *)us_tecnode_grab;
////			us_tecvariables[0].type = us_tecnode_grabcount/3;
////		}
//
//		// switch to this technology
//		System.out.println("Technology " + tech.getTechName() + " built.");
//		WindowFrame.updateTechnologyLists();
//		return tech;
//	}

	/**
	 * This class displays a dialog for converting a library to a technology.
	 */
	private static class GenerateTechnology extends EDialog
	{
		private JLabel lab2, lab3;
		private JTextField renameName, newName;
		private JCheckBox alsoXML;

		/** Creates new form convert library to technology */
		private GenerateTechnology()
		{
			super(null, true);
		}

//		private void ok() { exit(true); }

		protected void escapePressed() { exit(false); }

		// Call this method when the user clicks the OK button
		private void exit(boolean goodButton)
		{
			if (goodButton)
			{
				new TechFromLibJob(newName.getText(), alsoXML.isSelected());
			}
			dispose();
		}

		private void nameChanged()
		{
			String techName = newName.getText();
			if (Technology.findTechnology(techName) != null)
			{
				// name exists, offer to rename it
				lab2.setEnabled(true);
				lab3.setEnabled(true);
				renameName.setEnabled(true);
				renameName.setEditable(true);
			} else
			{
				// name is unique, don't offer to rename it
				lab2.setEnabled(false);
				lab3.setEnabled(false);
				renameName.setEnabled(false);
				renameName.setEditable(false);
			}
		}

		private void initComponents()
		{
			getContentPane().setLayout(new GridBagLayout());

			setTitle("Convert Library to Technology");
			setName("");
			addWindowListener(new WindowAdapter()
			{
				public void windowClosing(WindowEvent evt) { exit(false); }
			});

			JLabel lab1 = new JLabel("Creating new technology:");
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.gridx = 0;   gbc.gridy = 0;
			gbc.anchor = GridBagConstraints.WEST;
			gbc.insets = new java.awt.Insets(4, 4, 4, 4);
			getContentPane().add(lab1, gbc);

			newName = new JTextField(Library.getCurrent().getName());
			gbc = new GridBagConstraints();
			gbc.gridx = 1;   gbc.gridy = 0;
			gbc.gridwidth = 2;
			gbc.anchor = GridBagConstraints.WEST;
			gbc.fill = GridBagConstraints.HORIZONTAL;
			gbc.weightx = 1;
			gbc.insets = new java.awt.Insets(4, 4, 4, 4);
			getContentPane().add(newName, gbc);
			TechNameDocumentListener myDocumentListener = new TechNameDocumentListener(this);
			newName.getDocument().addDocumentListener(myDocumentListener);

			lab2 = new JLabel("Already a technology with this name");
			gbc = new GridBagConstraints();
			gbc.gridx = 0;   gbc.gridy = 1;
			gbc.gridwidth = 3;
			gbc.anchor = GridBagConstraints.WEST;
			gbc.insets = new java.awt.Insets(4, 4, 4, 4);
			getContentPane().add(lab2, gbc);

			lab3 = new JLabel("Rename existing technology to:");
			gbc = new GridBagConstraints();
			gbc.gridx = 0;   gbc.gridy = 2;
			gbc.anchor = GridBagConstraints.WEST;
			gbc.insets = new java.awt.Insets(4, 4, 4, 4);
			getContentPane().add(lab3, gbc);

			renameName = new JTextField();
			gbc = new GridBagConstraints();
			gbc.gridx = 1;   gbc.gridy = 2;
			gbc.gridwidth = 2;
			gbc.anchor = GridBagConstraints.WEST;
			gbc.fill = GridBagConstraints.HORIZONTAL;
			gbc.weightx = 1;
			gbc.insets = new java.awt.Insets(4, 4, 4, 4);
			getContentPane().add(renameName, gbc);

			alsoXML = new JCheckBox("Also write XML code");
			gbc = new GridBagConstraints();
			gbc.gridx = 0;   gbc.gridy = 3;
			gbc.anchor = GridBagConstraints.WEST;
			gbc.insets = new java.awt.Insets(4, 4, 4, 4);
			getContentPane().add(alsoXML, gbc);

			// OK and Cancel
			JButton cancel = new JButton("Cancel");
			gbc = new GridBagConstraints();
			gbc.gridx = 1;
			gbc.gridy = 3;
			gbc.insets = new java.awt.Insets(4, 4, 4, 4);
			getContentPane().add(cancel, gbc);
			cancel.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent evt) { exit(false); }
			});

			JButton ok = new JButton("OK");
			getRootPane().setDefaultButton(ok);
			gbc = new java.awt.GridBagConstraints();
			gbc.gridx = 2;
			gbc.gridy = 3;
			gbc.insets = new java.awt.Insets(4, 4, 4, 4);
			getContentPane().add(ok, gbc);
			ok.addActionListener(new java.awt.event.ActionListener()
			{
				public void actionPerformed(java.awt.event.ActionEvent evt) { exit(true); }
			});

			pack();
		}

		/**
		 * Class to handle special changes to changes to a GDS layer.
		 */
		private static class TechNameDocumentListener implements DocumentListener
		{
			GenerateTechnology dialog;

			TechNameDocumentListener(GenerateTechnology dialog) { this.dialog = dialog; }

			public void changedUpdate(DocumentEvent e) { dialog.nameChanged(); }
			public void insertUpdate(DocumentEvent e) { dialog.nameChanged(); }
			public void removeUpdate(DocumentEvent e) { dialog.nameChanged(); }
		}
	}

	private static void checkAndWarn(LayerInfo [] lList, ArcInfo [] aList, NodeInfo [] nList)
	{
		// make sure there is a pure-layer node for every nonpseudo layer
		for(int i=0; i<lList.length; i++)
		{
			if (lList[i].pseudo) continue;
			boolean found = false;
			for(int j=0; j<nList.length; j++)
			{
				NodeInfo nIn = nList[j];
				if (nIn.func != PrimitiveNode.Function.NODE) continue;
				if (nIn.nodeLayers[0].layer == lList[i])
				{
					found = true;
					break;
				}
			}
			if (found) continue;
			System.out.println("Warning: Layer " + lList[i].name + " has no associated pure-layer node");
		}

		// make sure there is a pin for every arc and that it uses pseudo-layers
		for(int i=0; i<aList.length; i++)
		{
			// find that arc's pin
			boolean found = false;
			for(int j=0; j<nList.length; j++)
			{
				NodeInfo nIn = nList[j];
				if (nIn.func != PrimitiveNode.Function.PIN) continue;

				for(int k=0; k<nIn.nodePortDetails.length; k++)
				{
					ArcInfo [] connections = nIn.nodePortDetails[k].connections;
					for(int l=0; l<connections.length; l++)
					{
						if (connections[l] == aList[i])
						{
							// pin found: make sure it uses pseudo-layers
							boolean allPseudo = true;
							for(int m=0; m<nIn.nodeLayers.length; m++)
							{
								LayerInfo lin = nIn.nodeLayers[m].layer;
								if (!lin.pseudo) { allPseudo = false;   break; }
							}
							if (!allPseudo)
								System.out.println("Warning: Pin " + nIn.name + " is not composed of pseudo-layers");

							found = true;
							break;
						}
					}
					if (found) break;
				}
				if (found) break;
			}
			if (!found)
				System.out.println("Warning: Arc " + aList[i].name + " has no associated pin node");
		}
	}

	/**
	 * Method to scan the "dependentlibcount" libraries in "dependentLibs",
	 * and build the layer structures for it in technology "tech".  Returns true on error.
	 */
	private static LayerInfo [] extractLayers(Library [] dependentLibs)
	{
		// first find the number of layers
		Cell [] layerCells = Info.findCellSequence(dependentLibs, "layer-", Info.LAYERSEQUENCE_KEY);
		if (layerCells.length <= 0)
		{
			System.out.println("No layers found");
			return null;
		}

		// create the layers
		LayerInfo [] lis = new LayerInfo[layerCells.length];
		for(int i=0; i<layerCells.length; i++)
		{
			lis[i] = LayerInfo.parseCell(layerCells[i]);
			if (lis[i] == null)
			{
				System.out.println("Error parsing description for " + layerCells[i].describe(false));
				continue;
			}
		}

//		// get the design rules
//		drcsize = us_teclayer_count*us_teclayer_count/2 + (us_teclayer_count+1)/2;
//		us_tecedgetlayernamelist();
//		if (us_tecdrc_rules != 0)
//		{
//			dr_freerules(us_tecdrc_rules);
//			us_tecdrc_rules = 0;
//		}
//		nodecount = Info.findCellSequence(dependentLibs, "node-", Generate.NODERSEQUENCE_KEY);
//		us_tecdrc_rules = dr_allocaterules(us_teceddrclayers, nodecount, x_("EDITED TECHNOLOGY"));
//		if (us_tecdrc_rules == NODRCRULES) return(TRUE);
//		for(i=0; i<us_teceddrclayers; i++)
//			(void)allocstring(&us_tecdrc_rules.layernames[i], us_teceddrclayernames[i], el_tempcluster);
//		for(i=0; i<nodecount; i++)
//			(void)allocstring(&us_tecdrc_rules.nodenames[i], &nodesequence[i].protoname[5], el_tempcluster);
//		if (nodecount > 0) efree((CHAR *)nodesequence);
//		var = NOVARIABLE;
//		for(i=dependentlibcount-1; i>=0; i--)
//		{
//			var = getval((INTBIG)dependentLibs[i], VLIBRARY, VSTRING|VISARRAY, x_("EDTEC_DRC"));
//			if (var != NOVARIABLE) break;
//		}
//		us_teceditgetdrcarrays(var, us_tecdrc_rules);

//		// see which design rules exist
//		for(i=0; i<us_teceddrclayers; i++)
//		{
//			if (us_tecdrc_rules.minwidth[i] >= 0) us_tecflags |= HASDRCMINWID;
//			if (*us_tecdrc_rules.minwidthR[i] != 0) us_tecflags |= HASDRCMINWIDR;
//		}
//		for(i=0; i<drcsize; i++)
//		{
//			if (us_tecdrc_rules.conlist[i] >= 0) us_tecflags |= HASCONDRC;
//			if (*us_tecdrc_rules.conlistR[i] != 0) us_tecflags |= HASCONDRCR;
//			if (us_tecdrc_rules.unconlist[i] >= 0) us_tecflags |= HASUNCONDRC;
//			if (*us_tecdrc_rules.unconlistR[i] != 0) us_tecflags |= HASUNCONDRCR;
//			if (us_tecdrc_rules.conlistW[i] >= 0) us_tecflags |= HASCONDRCW;
//			if (*us_tecdrc_rules.conlistWR[i] != 0) us_tecflags |= HASCONDRCWR;
//			if (us_tecdrc_rules.unconlistW[i] >= 0) us_tecflags |= HASUNCONDRCW;
//			if (*us_tecdrc_rules.unconlistWR[i] != 0) us_tecflags |= HASUNCONDRCWR;
//			if (us_tecdrc_rules.conlistM[i] >= 0) us_tecflags |= HASCONDRCM;
//			if (*us_tecdrc_rules.conlistMR[i] != 0) us_tecflags |= HASCONDRCMR;
//			if (us_tecdrc_rules.unconlistM[i] >= 0) us_tecflags |= HASUNCONDRCM;
//			if (*us_tecdrc_rules.unconlistMR[i] != 0) us_tecflags |= HASUNCONDRCMR;
//			if (us_tecdrc_rules.edgelist[i] >= 0) us_tecflags |= HASEDGEDRC;
//			if (*us_tecdrc_rules.edgelistR[i] != 0) us_tecflags |= HASEDGEDRCR;
//		}
//		for(i=0; i<us_tecdrc_rules.numnodes; i++)
//		{
//			if (us_tecdrc_rules.minnodesize[i*2] > 0 ||
//				us_tecdrc_rules.minnodesize[i*2+1] > 0) us_tecflags |= HASMINNODE;
//			if (*us_tecdrc_rules.minnodesizeR[i] != 0) us_tecflags |= HASMINNODER;
//		}

//		// store this information on the technology object
//		if ((us_tecflags&(HASCONDRCW|HASUNCONDRCW)) != 0)
//			(void)setvalkey((INTBIG)tech, VTECHNOLOGY, dr_wide_limitkey, us_tecdrc_rules.widelimit,
//				VFRACT|VDONTSAVE);
//		if ((us_tecflags&HASDRCMINWID) != 0)
//			(void)setvalkey((INTBIG)tech, VTECHNOLOGY, dr_min_widthkey, (INTBIG)us_tecdrc_rules.minwidth,
//				VFRACT|VDONTSAVE|VISARRAY|(tech.layercount<<VLENGTHSH));
//		if ((us_tecflags&HASDRCMINWIDR) != 0)
//			(void)setvalkey((INTBIG)tech, VTECHNOLOGY, dr_min_width_rulekey, (INTBIG)us_tecdrc_rules.minwidthR,
//				VSTRING|VDONTSAVE|VISARRAY|(tech.layercount<<VLENGTHSH));
//		if ((us_tecflags&HASCONDRC) != 0)
//			(void)setvalkey((INTBIG)tech, VTECHNOLOGY, dr_connected_distanceskey,
//				(INTBIG)us_tecdrc_rules.conlist, VFRACT|VDONTSAVE|VISARRAY|(drcsize<<VLENGTHSH));
//		if ((us_tecflags&HASCONDRCR) != 0)
//			(void)setvalkey((INTBIG)tech, VTECHNOLOGY, dr_connected_distances_rulekey,
//				(INTBIG)us_tecdrc_rules.conlistR, VSTRING|VDONTSAVE|VISARRAY|(drcsize<<VLENGTHSH));
//		if ((us_tecflags&HASUNCONDRC) != 0)
//			(void)setvalkey((INTBIG)tech, VTECHNOLOGY, dr_unconnected_distanceskey,
//				(INTBIG)us_tecdrc_rules.unconlist, VFRACT|VDONTSAVE|VISARRAY|(drcsize<<VLENGTHSH));
//		if ((us_tecflags&HASUNCONDRCR) != 0)
//			(void)setvalkey((INTBIG)tech, VTECHNOLOGY, dr_unconnected_distances_rulekey,
//				(INTBIG)us_tecdrc_rules.unconlistR, VSTRING|VDONTSAVE|VISARRAY|(drcsize<<VLENGTHSH));
//		if ((us_tecflags&HASCONDRCW) != 0)
//			(void)setvalkey((INTBIG)tech, VTECHNOLOGY, dr_connected_distancesWkey,
//				(INTBIG)us_tecdrc_rules.conlistW, VFRACT|VDONTSAVE|VISARRAY|(drcsize<<VLENGTHSH));
//		if ((us_tecflags&HASCONDRCWR) != 0)
//			(void)setvalkey((INTBIG)tech, VTECHNOLOGY, dr_connected_distancesW_rulekey,
//				(INTBIG)us_tecdrc_rules.conlistWR, VSTRING|VDONTSAVE|VISARRAY|(drcsize<<VLENGTHSH));
//		if ((us_tecflags&HASUNCONDRCW) != 0)
//			(void)setvalkey((INTBIG)tech, VTECHNOLOGY, dr_unconnected_distancesWkey,
//				(INTBIG)us_tecdrc_rules.unconlistW, VFRACT|VDONTSAVE|VISARRAY|(drcsize<<VLENGTHSH));
//		if ((us_tecflags&HASUNCONDRCWR) != 0)
//			(void)setvalkey((INTBIG)tech, VTECHNOLOGY, dr_unconnected_distancesW_rulekey,
//				(INTBIG)us_tecdrc_rules.unconlistWR, VSTRING|VDONTSAVE|VISARRAY|(drcsize<<VLENGTHSH));
//		if ((us_tecflags&HASCONDRCM) != 0)
//			(void)setvalkey((INTBIG)tech, VTECHNOLOGY, dr_connected_distancesMkey,
//				(INTBIG)us_tecdrc_rules.conlistM, VFRACT|VDONTSAVE|VISARRAY|(drcsize<<VLENGTHSH));
//		if ((us_tecflags&HASCONDRCMR) != 0)
//			(void)setvalkey((INTBIG)tech, VTECHNOLOGY, dr_connected_distancesM_rulekey,
//				(INTBIG)us_tecdrc_rules.conlistMR, VSTRING|VDONTSAVE|VISARRAY|(drcsize<<VLENGTHSH));
//		if ((us_tecflags&HASUNCONDRCM) != 0)
//			(void)setvalkey((INTBIG)tech, VTECHNOLOGY, dr_unconnected_distancesMkey,
//				(INTBIG)us_tecdrc_rules.unconlistM, VFRACT|VDONTSAVE|VISARRAY|(drcsize<<VLENGTHSH));
//		if ((us_tecflags&HASUNCONDRCMR) != 0)
//			(void)setvalkey((INTBIG)tech, VTECHNOLOGY, dr_unconnected_distancesM_rulekey,
//				(INTBIG)us_tecdrc_rules.unconlistMR, VSTRING|VDONTSAVE|VISARRAY|(drcsize<<VLENGTHSH));
//		if ((us_tecflags&HASEDGEDRC) != 0)
//			(void)setvalkey((INTBIG)tech, VTECHNOLOGY, dr_edge_distanceskey,
//				(INTBIG)us_tecdrc_rules.edgelist, VFRACT|VDONTSAVE|VISARRAY|(drcsize<<VLENGTHSH));
//		if ((us_tecflags&HASEDGEDRCR) != 0)
//			(void)setvalkey((INTBIG)tech, VTECHNOLOGY, dr_edge_distances_rulekey,
//				(INTBIG)us_tecdrc_rules.edgelistR, VSTRING|VDONTSAVE|VISARRAY|(drcsize<<VLENGTHSH));
//		if ((us_tecflags&HASMINNODE) != 0)
//			(void)setvalkey((INTBIG)tech, VTECHNOLOGY, dr_min_node_sizekey,
//				(INTBIG)us_tecdrc_rules.minnodesize, VFRACT|VDONTSAVE|VISARRAY|((us_tecdrc_rules.numnodes*2)<<VLENGTHSH));
//		if ((us_tecflags&HASMINNODER) != 0)
//			(void)setvalkey((INTBIG)tech, VTECHNOLOGY, dr_min_node_size_rulekey,
//				(INTBIG)us_tecdrc_rules.minnodesizeR, VSTRING|VDONTSAVE|VISARRAY|(us_tecdrc_rules.numnodes<<VLENGTHSH));

		return lis;
	}

	/**
	 * Method to scan the "dependentlibcount" libraries in "dependentLibs",
	 * and build the arc structures for it in technology "tech".  Returns true on error.
	 */
	private static ArcInfo [] extractArcs(Library [] dependentLibs, LayerInfo [] lList)
	{
		// count the number of arcs in the technology
		Cell [] arcCells = Info.findCellSequence(dependentLibs, "arc-", Info.ARCSEQUENCE_KEY);
		if (arcCells.length <= 0)
		{
			System.out.println("No arcs found");
			return null;
		}

		ArcInfo [] allArcs = new ArcInfo[arcCells.length];
		for(int i=0; i<arcCells.length; i++)
		{
			Cell np = arcCells[i];
			allArcs[i] = ArcInfo.parseCell(np);

			// build a list of examples found in this arc
			Example neList = Example.getExamples(np, false);
			if (neList == null) return null;
			if (neList.nextExample != null)
			{
				pointOutError(null, np);
				System.out.println("Can only be one example of " + np + " but more were found");
				return null;
			}

			// get width and polygon count information
			double maxWid = -1, hWid = -1;
			int count = 0;
			for(Sample ns : neList.samples)
			{
				double wid = Math.min(ns.node.getXSize(), ns.node.getYSize());
				if (wid > maxWid) maxWid = wid;
				if (ns.layer == null) hWid = wid; else count++;
			}
			allArcs[i].widthOffset = maxWid - hWid;
			allArcs[i].maxWidth = maxWid;

			// error if there is no highlight box
			if (hWid < 0)
			{
				pointOutError(null, np);
				System.out.println("No highlight layer found in " + np);
				return null;
			}
			allArcs[i].arcDetails = new ArcInfo.LayerDetails[count];

			// fill the individual arc layer structures
			int layerIndex = 0;
			for(int k=0; k<2; k++)
			{
				for(Sample ns : neList.samples)
				{
					if (ns.layer == null) continue;

					// get the layer index
					String sampleLayer = ns.layer.getName().substring(6);
					LayerInfo li = null;
					for(int j=0; j<lList.length; j++)
					{
						if (sampleLayer.equals(lList[j].name)) { li = lList[j];   break; }
					}
					if (li == null)
					{
						System.out.println("Cannot find layer " + sampleLayer + ", used in " + np);
						return null;
					}

					// only add transparent layers when k=0
					if (k == 0)
					{
						if (li.desc.getTransparentLayer() == 0) continue;
					} else
					{
						if (li.desc.getTransparentLayer() != 0) continue;
					}
					allArcs[i].arcDetails[layerIndex] = new ArcInfo.LayerDetails();
					allArcs[i].arcDetails[layerIndex].layer = li;

					// determine the style of this arc layer
					Poly.Type style = Poly.Type.CLOSED;
					if (ns.node.getProto() == Artwork.tech.filledBoxNode)
						style = Poly.Type.FILLED;
					allArcs[i].arcDetails[layerIndex].style = style;

					// determine the width offset of this arc layer
					double wid = Math.min(ns.node.getXSize(), ns.node.getYSize());
					allArcs[i].arcDetails[layerIndex].width = maxWid-wid;

					layerIndex++;
				}
			}
		}
		return allArcs;
	}

	/**
	 * Method to scan the "dependentlibcount" libraries in "dependentLibs",
	 * and build the node structures for it in technology "tech".  Returns true on error.
	 */
	private static NodeInfo [] extractNodes(Library [] dependentLibs, LayerInfo [] lList, ArcInfo [] aList)
	{
		Cell [] nodeCells = Info.findCellSequence(dependentLibs, "node-", Info.NODESEQUENCE_KEY);
		if (nodeCells.length <= 0)
		{
			System.out.println("No nodes found");
			return null;
		}

		NodeInfo [] nList = new NodeInfo[nodeCells.length];

		// get the nodes
		int nodeIndex = 0;
		for(int pass=0; pass<3; pass++)
			for(int m=0; m<nodeCells.length; m++)
		{
			// make sure this is the right type of node for this pass of the nodes
			Cell np = nodeCells[m];
			NodeInfo nIn = NodeInfo.parseCell(np);
			Netlist netList = np.acquireUserNetlist();
			if (netList == null)
			{
				System.out.println("Sorry, a deadlock technology generation (network information unavailable).  Please try again");
				return null;
			}

			// only want pins on pass 0, pure-layer nodes on pass 2
			if (pass == 0 && nIn.func != PrimitiveNode.Function.PIN) continue;
			if (pass == 1 && (nIn.func == PrimitiveNode.Function.PIN || nIn.func == PrimitiveNode.Function.NODE)) continue;
			if (pass == 2 && nIn.func != PrimitiveNode.Function.NODE) continue;
			if (nIn.func == PrimitiveNode.Function.NODE)
			{
				if (nIn.serp)
				{
					pointOutError(null, np);
					System.out.println("Pure layer " + nIn.name + " can not be serpentine");
					return null;
				}
				nIn.specialType = PrimitiveNode.POLYGONAL;
			}

			nList[nodeIndex] = nIn;
			nIn.name = np.getName().substring(5);

			// build a list of examples found in this node
			Example neList = Example.getExamples(np, true);
			if (neList == null)
			{
				System.out.println("Cannot analyze " + np);
				return null;
			}
			nIn.xSize = neList.hx - neList.lx;
			nIn.ySize = neList.hy - neList.ly;

			// associate the samples in each example
			if (associateExamples(neList, np))
			{
				System.out.println("Cannot match different examples in " + np);
				return null;
			}

			// derive primitives from the examples
			nIn.nodeLayers = makePrimitiveNodeLayers(neList, np, lList);
			if (nIn.nodeLayers == null)
			{
				System.out.println("Cannot derive stretching rules for " + np);
				return null;
			}

			// handle multicut layers
//			for(int i=0; i<nIn.nodeLayers.length; i++)
//			{
//				NodeInfo.LayerDetails nld = nIn.nodeLayers[i];
//				if (nld.multiCut)
//				{
//					nIn.specialType = PrimitiveNode.MULTICUT;
//					nIn.specialValues = new double[6];
//					nIn.specialValues[0] = nIn.nodeLayers[i].multiXS;
//					nIn.specialValues[1] = nIn.nodeLayers[i].multiYS;
//					nIn.specialValues[2] = nIn.nodeLayers[i].multiIndent;
//					nIn.specialValues[3] = nIn.nodeLayers[i].multiIndent;
//					nIn.specialValues[4] = nIn.nodeLayers[i].multiSep;
//					nIn.specialValues[5] = nIn.nodeLayers[i].multiSep;
//
//					// make the multicut layer the last one
//					NodeInfo.LayerDetails nldLast = nIn.nodeLayers[nIn.nodeLayers.length-1];
//					NodeInfo.LayerDetails nldMC = nIn.nodeLayers[i];
//					nIn.nodeLayers[i] = nldLast;
//					nIn.nodeLayers[nIn.nodeLayers.length-1] = nldMC;
//					break;
//				}
//			}

			// count the number of ports on this node
			int portCount = 0;
			for(Sample ns : neList.samples)
			{
				if (ns.layer == Generic.tech.portNode) portCount++;
			}
			if (portCount == 0)
			{
				pointOutError(null, np);
				System.out.println("No ports found in " + np);
				return null;
			}

			// allocate space for the ports
			nIn.nodePortDetails = new NodeInfo.PortDetails[portCount];

			// fill the port structures
			int pol1Port = -1, pol2Port = -1, dif1Port = -1, dif2Port = -1;
			int i = 0;
			for(Sample ns : neList.samples)
			{
				if (ns.layer != Generic.tech.portNode) continue;

				// port connections
				nIn.nodePortDetails[i] = new NodeInfo.PortDetails();
				nIn.nodePortDetails[i].connections = new ArcInfo[0];
				Variable var = ns.node.getVar(Info.CONNECTION_KEY);
				if (var != null)
				{
					// convert "arc-CELL" pointers to indices
					CellId [] arcCells = (CellId [])var.getObject();
					ArcInfo [] connections = new ArcInfo[arcCells.length];
					nIn.nodePortDetails[i].connections = connections;
					boolean portChecked = false;
					for(int j=0; j<arcCells.length; j++)
					{
						// find arc that connects
						Cell arcCell = EDatabase.serverDatabase().getCell(arcCells[j]);
						connections[j] = null;
						if (arcCell != null)
						{
							String cellName = arcCell.getName().substring(4);
							for(int k=0; k<aList.length; k++)
							{
								if (aList[k].name.equalsIgnoreCase(cellName))
								{
									connections[j] = aList[k];
									break;
								}
							}
						}
						if (connections[j] == null)
						{
							pointOutError(ns.node, ns.node.getParent());
							System.out.println("Invalid connection list on port in " + np);
							return null;
						}

						// find port characteristics for possible transistors
						if (portChecked) continue;
						if (connections[j].func.isPoly())
						{
							if (pol1Port < 0)
							{
								pol1Port = i;
								portChecked = true;
							} else if (pol2Port < 0)
							{
								pol2Port = i;
								portChecked = true;
							}
						} else if (connections[j].func.isDiffusion())
						{
							if (dif1Port < 0)
							{
								dif1Port = i;
								portChecked = true;
							} else if (dif2Port < 0)
							{
								dif2Port = i;
								portChecked = true;
							}
						}
					}
				}

				// link connection list to the port
				if (nIn.nodePortDetails[i].connections == null) return null;

				// port name
				String portName = Info.getPortName(ns.node);
				if (portName == null)
				{
					pointOutError(ns.node, np);
					System.out.println("Cell " + np.describe(true) + ": port does not have a name");
					return null;
				}
				for(int c=0; c<portName.length(); c++)
				{
					char str = portName.charAt(c);
					if (str <= ' ' || str >= 0177)
					{
						pointOutError(ns.node, np);
						System.out.println("Invalid port name '" + portName + "' in " + np);
						return null;
					}
				}
				nIn.nodePortDetails[i].name = portName;

				// port angle and range
				nIn.nodePortDetails[i].angle = 0;
				Variable varAngle = ns.node.getVar(Info.PORTANGLE_KEY);
				if (varAngle != null)
					nIn.nodePortDetails[i].angle = ((Integer)varAngle.getObject()).intValue();
				nIn.nodePortDetails[i].range = 180;
				Variable varRange = ns.node.getVar(Info.PORTRANGE_KEY);
				if (varRange != null)
					nIn.nodePortDetails[i].range = ((Integer)varRange.getObject()).intValue();

				// port connectivity
				nIn.nodePortDetails[i].netIndex = i;
				if (ns.node.hasConnections())
				{
					ArcInst ai1 = ns.node.getConnections().next().getArc();
					Network net1 = netList.getNetwork(ai1, 0);
					int j = 0;
					for(Sample oNs : neList.samples)
					{
						if (oNs == ns) break;
						if (oNs.layer != Generic.tech.portNode) continue;
						if (oNs.node.hasConnections())
						{
							ArcInst ai2 = oNs.node.getConnections().next().getArc();
							Network net2 = netList.getNetwork(ai2, 0);
							if (net1 == net2)
							{
								nIn.nodePortDetails[i].netIndex = j;
								break;
							}
						}
						j++;
					}
				}

				// port area rule
				nIn.nodePortDetails[i].values = ns.values;
				i++;
			}

			// on field-effect transistors, make sure ports 0 and 2 are poly
			if (nIn.func == PrimitiveNode.Function.TRANMOS || nIn.func == PrimitiveNode.Function.TRADMOS ||
				nIn.func == PrimitiveNode.Function.TRAPMOS || nIn.func == PrimitiveNode.Function.TRADMES ||
				nIn.func == PrimitiveNode.Function.TRAEMES)
			{
				if (pol1Port < 0 || pol2Port < 0 || dif1Port < 0 || dif2Port < 0)
				{
					pointOutError(null, np);
					System.out.println("Need 2 gate and 2 active ports on field-effect transistor " + np.describe(true));
					return null;
				}
				if (pol1Port != 0)
				{
					if (pol2Port == 0)
					{
						NodeInfo.PortDetails formerPortA = nIn.nodePortDetails[pol1Port];
						NodeInfo.PortDetails formerPortB = nIn.nodePortDetails[pol2Port];
						int swap = pol1Port;   pol1Port = pol2Port;   pol2Port = swap;
						nIn.nodePortDetails[pol1Port] = formerPortA;
						nIn.nodePortDetails[pol2Port] = formerPortB;
					} else if (dif1Port == 0)
					{
						NodeInfo.PortDetails formerPortA = nIn.nodePortDetails[pol1Port];
						NodeInfo.PortDetails formerPortB = nIn.nodePortDetails[dif1Port];
						int swap = pol1Port;   pol1Port = dif1Port;   dif1Port = swap;
						nIn.nodePortDetails[pol1Port] = formerPortA;
						nIn.nodePortDetails[dif1Port] = formerPortB;
					} else if (dif2Port == 0)
					{
						NodeInfo.PortDetails formerPortA = nIn.nodePortDetails[pol1Port];
						NodeInfo.PortDetails formerPortB = nIn.nodePortDetails[dif2Port];
						int swap = pol1Port;   pol1Port = dif2Port;   dif2Port = swap;
						nIn.nodePortDetails[pol1Port] = formerPortA;
						nIn.nodePortDetails[dif2Port] = formerPortB;
					}
				}
				if (pol2Port != 2)
				{
					if (dif1Port == 2)
					{
						NodeInfo.PortDetails formerPortA = nIn.nodePortDetails[pol2Port];
						NodeInfo.PortDetails formerPortB = nIn.nodePortDetails[dif1Port];
						int swap = pol2Port;   pol2Port = dif1Port;   dif1Port = swap;
						nIn.nodePortDetails[pol2Port] = formerPortA;
						nIn.nodePortDetails[dif1Port] = formerPortB;
					} else if (dif2Port == 2)
					{
						NodeInfo.PortDetails formerPortA = nIn.nodePortDetails[pol2Port];
						NodeInfo.PortDetails formerPortB = nIn.nodePortDetails[dif2Port];
						int swap = pol2Port;   pol2Port = dif2Port;   dif2Port = swap;
						nIn.nodePortDetails[pol2Port] = formerPortA;
						nIn.nodePortDetails[dif2Port] = formerPortB;
					}
				}
				if (dif1Port != 1)
				{
					NodeInfo.PortDetails formerPortA = nIn.nodePortDetails[dif1Port];
					NodeInfo.PortDetails formerPortB = nIn.nodePortDetails[dif2Port];
					int swap = dif1Port;   dif1Port = dif2Port;   dif2Port = swap;
					nIn.nodePortDetails[dif1Port] = formerPortA;
					nIn.nodePortDetails[dif2Port] = formerPortB;
				}

				// also make sure that dif1Port is positive and dif2Port is negative
				double x1Pos = (nIn.nodePortDetails[dif1Port].values[0].getX().getMultiplier() * nIn.xSize +
					nIn.nodePortDetails[dif1Port].values[0].getX().getAdder() +
					nIn.nodePortDetails[dif1Port].values[1].getX().getMultiplier() * nIn.xSize +
					nIn.nodePortDetails[dif1Port].values[1].getX().getAdder()) / 2;
				double x2Pos = (nIn.nodePortDetails[dif2Port].values[0].getX().getMultiplier() * nIn.xSize +
					nIn.nodePortDetails[dif2Port].values[0].getX().getAdder() +
					nIn.nodePortDetails[dif2Port].values[1].getX().getMultiplier() * nIn.xSize +
					nIn.nodePortDetails[dif2Port].values[1].getX().getAdder()) / 2;
				double y1Pos = (nIn.nodePortDetails[dif1Port].values[0].getY().getMultiplier() * nIn.ySize +
					nIn.nodePortDetails[dif1Port].values[0].getY().getAdder() +
					nIn.nodePortDetails[dif1Port].values[1].getY().getMultiplier() * nIn.ySize +
					nIn.nodePortDetails[dif1Port].values[1].getY().getAdder()) / 2;
				double y2Pos = (nIn.nodePortDetails[dif2Port].values[0].getY().getMultiplier() * nIn.ySize +
					nIn.nodePortDetails[dif2Port].values[0].getY().getAdder() +
					nIn.nodePortDetails[dif2Port].values[1].getY().getMultiplier() * nIn.ySize +
					nIn.nodePortDetails[dif2Port].values[1].getY().getAdder()) / 2;
				if (Math.abs(x1Pos-x2Pos) > Math.abs(y1Pos-y2Pos))
				{
					if (x1Pos < x2Pos)
					{
						NodeInfo.PortDetails formerPortA = nIn.nodePortDetails[dif1Port];
						NodeInfo.PortDetails formerPortB = nIn.nodePortDetails[dif2Port];
						nIn.nodePortDetails[dif1Port] = formerPortA;
						nIn.nodePortDetails[dif2Port] = formerPortB;
					}
				} else
				{
					if (y1Pos < y2Pos)
					{
						NodeInfo.PortDetails formerPortA = nIn.nodePortDetails[dif1Port];
						NodeInfo.PortDetails formerPortB = nIn.nodePortDetails[dif2Port];
						nIn.nodePortDetails[dif1Port] = formerPortA;
						nIn.nodePortDetails[dif2Port] = formerPortB;
					}
				}

				// also make sure that pol1Port is negative and pol2Port is positive
				x1Pos = (nIn.nodePortDetails[pol1Port].values[0].getX().getMultiplier() * nIn.xSize +
					nIn.nodePortDetails[pol1Port].values[0].getX().getAdder() +
					nIn.nodePortDetails[pol1Port].values[1].getX().getMultiplier() * nIn.xSize +
					nIn.nodePortDetails[pol1Port].values[1].getX().getAdder()) / 2;
				x2Pos = (nIn.nodePortDetails[pol2Port].values[0].getX().getMultiplier() * nIn.xSize +
					nIn.nodePortDetails[pol2Port].values[0].getX().getAdder() +
					nIn.nodePortDetails[pol2Port].values[1].getX().getMultiplier() * nIn.xSize +
					nIn.nodePortDetails[pol2Port].values[1].getX().getAdder()) / 2;
				y1Pos = (nIn.nodePortDetails[pol1Port].values[0].getY().getMultiplier() * nIn.ySize +
					nIn.nodePortDetails[pol1Port].values[0].getY().getAdder() +
					nIn.nodePortDetails[pol1Port].values[1].getY().getMultiplier() * nIn.ySize +
					nIn.nodePortDetails[pol1Port].values[1].getY().getAdder()) / 2;
				y2Pos = (nIn.nodePortDetails[pol2Port].values[0].getY().getMultiplier() * nIn.ySize +
					nIn.nodePortDetails[pol2Port].values[0].getY().getAdder() +
					nIn.nodePortDetails[pol2Port].values[1].getY().getMultiplier() * nIn.ySize +
					nIn.nodePortDetails[pol2Port].values[1].getY().getAdder()) / 2;
				if (Math.abs(x1Pos-x2Pos) > Math.abs(y1Pos-y2Pos))
				{
					if (x1Pos > x2Pos)
					{
						NodeInfo.PortDetails formerPortA = nIn.nodePortDetails[pol1Port];
						NodeInfo.PortDetails formerPortB = nIn.nodePortDetails[pol2Port];
						nIn.nodePortDetails[pol1Port] = formerPortA;
						nIn.nodePortDetails[pol2Port] = formerPortB;
					}
				} else
				{
					if (y1Pos > y2Pos)
					{
						NodeInfo.PortDetails formerPortA = nIn.nodePortDetails[pol1Port];
						NodeInfo.PortDetails formerPortB = nIn.nodePortDetails[pol2Port];
						nIn.nodePortDetails[pol1Port] = formerPortA;
						nIn.nodePortDetails[pol2Port] = formerPortB;
					}
				}
			}

			// count the number of layers on the node
			int layerCount = 0;
			for(Sample ns : neList.samples)
			{
				if (ns.values != null && ns.layer != Generic.tech.portNode &&
					ns.layer != Generic.tech.cellCenterNode && ns.layer != null)
						layerCount++;
			}

			// finish up serpentine transistors
			if (nIn.serp)
			{
				nIn.specialType = PrimitiveNode.SERPTRANS;

				// determine port numbers for serpentine transistors
				int polIndex = -1, difIndex = -1;
				for(int k=0; k<nIn.nodeLayers.length; k++)
				{
					NodeInfo.LayerDetails nld = nIn.nodeLayers[k];
					if (nld.layer.fun.isPoly())
					{
						polIndex = k;
					} else if (nld.layer.fun.isDiff())
					{
						if (difIndex >= 0)
						{
							// figure out which layer is the basic active layer
							int funExtraOld = nIn.nodeLayers[difIndex].layer.funExtra;
							int funExtraNew = nld.layer.funExtra;
							if (funExtraOld == funExtraNew) continue;
							if (funExtraOld == 0)
//							if ((funExtraOld & ~(Layer.Function.PTYPE|Layer.Function.NTYPE)) == 0)
								continue;
						}
						difIndex = k;
					}
				}
				if (difIndex < 0 || polIndex < 0)
				{
					pointOutError(null, np);
					System.out.println("No diffusion and polysilicon layers in transistor " + np);
					return null;
				}

				// compute port extension factors
				nIn.specialValues = new double[6];
				nIn.specialValues[0] = layerCount+1;
				if (nIn.nodePortDetails[dif1Port].values[0].getX().getAdder() >
					nIn.nodePortDetails[dif1Port].values[0].getY().getAdder())
				{
					// vertical diffusion layer: determine polysilicon width
					nIn.specialValues[3] = (nIn.ySize * nIn.nodeLayers[polIndex].values[1].getY().getMultiplier() +
						nIn.nodeLayers[polIndex].values[1].getY().getAdder()) -
						(nIn.ySize * nIn.nodeLayers[polIndex].values[0].getY().getMultiplier() +
						nIn.nodeLayers[polIndex].values[0].getY().getAdder());

					// determine diffusion port rule
					nIn.specialValues[1] = (nIn.xSize * nIn.nodePortDetails[dif1Port].values[0].getX().getMultiplier() +
						nIn.nodePortDetails[dif1Port].values[0].getX().getAdder()) -
						(nIn.xSize * nIn.nodeLayers[difIndex].values[0].getX().getMultiplier() +
						nIn.nodeLayers[difIndex].values[0].getX().getAdder());
					nIn.specialValues[2] = (nIn.ySize * nIn.nodePortDetails[dif1Port].values[0].getY().getMultiplier() +
						nIn.nodePortDetails[dif1Port].values[0].getY().getAdder()) -
						(nIn.ySize * nIn.nodeLayers[polIndex].values[1].getY().getMultiplier() +
						nIn.nodeLayers[polIndex].values[1].getY().getAdder());

					// determine polysilicon port rule
					nIn.specialValues[4] = (nIn.ySize * nIn.nodePortDetails[pol1Port].values[0].getY().getMultiplier() +
						nIn.nodePortDetails[pol1Port].values[0].getY().getAdder()) -
						(nIn.ySize * nIn.nodeLayers[polIndex].values[0].getY().getMultiplier() +
						nIn.nodeLayers[polIndex].values[0].getY().getAdder());
					nIn.specialValues[5] = (nIn.xSize * nIn.nodeLayers[difIndex].values[0].getX().getMultiplier() +
						nIn.nodeLayers[difIndex].values[0].getX().getAdder()) -
						(nIn.xSize * nIn.nodePortDetails[pol1Port].values[1].getX().getMultiplier() +
						nIn.nodePortDetails[pol1Port].values[1].getX().getAdder());
				} else
				{
					// horizontal diffusion layer: determine polysilicon width
					nIn.specialValues[3] = (nIn.xSize * nIn.nodeLayers[polIndex].values[1].getX().getMultiplier() +
						nIn.nodeLayers[polIndex].values[1].getX().getAdder()) -
						(nIn.xSize * nIn.nodeLayers[polIndex].values[0].getX().getMultiplier() +
						nIn.nodeLayers[polIndex].values[0].getX().getAdder());

					// determine diffusion port rule
					nIn.specialValues[1] = (nIn.ySize * nIn.nodePortDetails[dif1Port].values[0].getY().getMultiplier() +
						nIn.nodePortDetails[dif1Port].values[0].getY().getAdder()) -
						(nIn.ySize * nIn.nodeLayers[difIndex].values[0].getY().getMultiplier() +
						nIn.nodeLayers[difIndex].values[0].getY().getAdder());
					nIn.specialValues[2] = (nIn.xSize * nIn.nodeLayers[polIndex].values[0].getX().getMultiplier() +
						nIn.nodeLayers[polIndex].values[0].getX().getAdder()) -
						(nIn.xSize * nIn.nodePortDetails[dif1Port].values[1].getX().getMultiplier() +
						nIn.nodePortDetails[dif1Port].values[1].getX().getAdder());

					// determine polysilicon port rule
					nIn.specialValues[4] = (nIn.xSize * nIn.nodePortDetails[pol1Port].values[0].getX().getMultiplier() +
						nIn.nodePortDetails[pol1Port].values[0].getX().getAdder()) -
						(nIn.xSize * nIn.nodeLayers[polIndex].values[0].getX().getMultiplier() +
						nIn.nodeLayers[polIndex].values[0].getX().getAdder());
					nIn.specialValues[5] = (nIn.ySize * nIn.nodeLayers[difIndex].values[0].getY().getMultiplier() +
						nIn.nodeLayers[difIndex].values[0].getY().getAdder()) -
						(nIn.ySize * nIn.nodePortDetails[pol1Port].values[1].getY().getMultiplier() +
						nIn.nodePortDetails[pol1Port].values[1].getY().getAdder());
				}

				// find width and extension from comparison to poly layer
				for(int k=0; k<nIn.nodeLayers.length; k++)
				{
					NodeInfo.LayerDetails nld = nIn.nodeLayers[k];
					Sample ns = nld.ns;
					Rectangle2D nodeBounds = ns.node.getBounds();
					Sample polNs = nIn.nodeLayers[polIndex].ns;
					Rectangle2D polNodeBounds = polNs.node.getBounds();
					Sample difNs = nIn.nodeLayers[difIndex].ns;
					Rectangle2D difNodeBounds = difNs.node.getBounds();
					if (polNodeBounds.getWidth() > polNodeBounds.getHeight())
					{
						// horizontal layer
						nld.lWidth = nodeBounds.getMaxY() - (ns.parent.ly + ns.parent.hy)/2;
						nld.rWidth = (ns.parent.ly + ns.parent.hy)/2 - nodeBounds.getMinY();
						nld.extendT = difNodeBounds.getMinX() - nodeBounds.getMinX();
					} else
					{
						// vertical layer
						nld.lWidth = nodeBounds.getMaxX() - (ns.parent.lx + ns.parent.hx)/2;
						nld.rWidth = (ns.parent.lx + ns.parent.hx)/2 - nodeBounds.getMinX();
						nld.extendT = difNodeBounds.getMinY() - nodeBounds.getMinY();
					}
					nld.extendB = nld.extendT;
				}
			}

			// extract width offset information from highlight box
			double lX = 0, hX = 0, lY = 0, hY = 0;
			boolean found = false;
			for(Sample ns : neList.samples)
			{
				if (ns.layer != null) continue;
				found = true;
				if (ns.values != null)
				{
					boolean err = false;
					if (ns.values[0].getX().getMultiplier() == -0.5)		// left edge offset
					{
						lX = ns.values[0].getX().getAdder();
					} else if (ns.values[0].getX().getMultiplier() == 0.5)
					{
						lX = nIn.xSize + ns.values[0].getX().getAdder();
					} else err = true;
					if (ns.values[0].getY().getMultiplier() == -0.5)		// bottom edge offset
					{
						lY = ns.values[0].getY().getAdder();
					} else if (ns.values[0].getY().getMultiplier() == 0.5)
					{
						lY = nIn.ySize + ns.values[0].getY().getAdder();;
					} else err = true;
					if (ns.values[1].getX().getMultiplier() == 0.5)		// right edge offset
					{
						hX = -ns.values[1].getX().getAdder();
					} else if (ns.values[1].getX().getMultiplier() == -0.5)
					{
						hX = nIn.xSize - ns.values[1].getX().getAdder();
					} else err = true;
					if (ns.values[1].getY().getMultiplier() == 0.5)		// top edge offset
					{
						hY = -ns.values[1].getY().getAdder();
					} else if (ns.values[1].getY().getMultiplier() == -0.5)
					{
						hY = nIn.ySize - ns.values[1].getY().getAdder();
					} else err = true;
					if (err)
					{
						pointOutError(ns.node, ns.node.getParent());
						System.out.println("Highlighting cannot scale from center in " + np);
						return null;
					}
				} else
				{
					pointOutError(ns.node, ns.node.getParent());
					System.out.println("No rule found for highlight in " + np);
					return null;
				}
			}
			if (!found)
			{
				pointOutError(null, np);
				System.out.println("No highlight found in " + np);
				return null;
			}
			if (lX != 0 || hX != 0 || lY != 0 || hY != 0)
			{
				nList[nodeIndex].so = new SizeOffset(lX, hX, lY, hY);
			}

//			// get grab point information
//			for(ns = neList.firstSample; ns != NOSAMPLE; ns = ns.nextSample)
//				if (ns.layer == Generic.tech.cellCenterNode) break;
//			if (ns != NOSAMPLE)
//			{
//				us_tecnode_grab[us_tecnode_grabcount++] = nodeindex+1;
//				us_tecnode_grab[us_tecnode_grabcount++] = (ns.node.geom.lowx +
//					ns.node.geom.highx - neList.lx - neList.hx)/2 *
//					el_curlib.lambda[tech.techindex] / lambda;
//				us_tecnode_grab[us_tecnode_grabcount++] = (ns.node.geom.lowy +
//					ns.node.geom.highy - neList.ly - neList.hy)/2 *
//					el_curlib.lambda[tech.techindex] / lambda;
//				us_tecflags |= HASGRAB;
//			}

			// advance the fill pointer
			nodeIndex++;
		}
		return nList;
	}

	/**
	 * Method to associate the samples of example "neList" in cell "np"
	 * Returns true if there is an error
	 */
	private static boolean associateExamples(Example neList, Cell np)
	{
		// if there is only one example, no association
		if (neList.nextExample == null) return false;

		// associate each example "ne" with the original in "neList"
		for(Example ne = neList.nextExample; ne != null; ne = ne.nextExample)
		{
			// clear associations for every sample "ns" in the example "ne"
			for(Sample ns : ne.samples)
			{
				ns.assoc = null;
			}

			// associate every sample "ns" in the example "ne"
			for(Sample ns : ne.samples)
			{
				if (ns.assoc != null) continue;

				// cannot have center in other examples
				if (ns.layer == Generic.tech.cellCenterNode)
				{
					pointOutError(ns.node, ns.node.getParent());
					System.out.println("Grab point should only be in main example of " + np);
					return true;
				}

				// count number of similar layers in original example "neList"
				int total = 0;
				Sample nsFound = null;
				for(Sample nsList : neList.samples)
				{
					if (nsList.layer != ns.layer) continue;
					total++;
					nsFound = nsList;
				}

				// no similar layer found in the original: error
				if (total == 0)
				{
					pointOutError(ns.node, ns.node.getParent());
					System.out.println("Layer " + getSampleName(ns.layer) + " not found in main example of " + np);
					return true;
				}

				// just one in the original: simple association
				if (total == 1)
				{
					ns.assoc = nsFound;
					continue;
				}

				// if it is a port, associate by port name
				if (ns.layer == Generic.tech.portNode)
				{
					String name = Info.getPortName(ns.node);
					if (name == null)
					{
						pointOutError(ns.node, ns.node.getParent());
						System.out.println("Cell " + np.describe(true) + ": port does not have a name");
						return true;
					}

					// search the original for that port
					boolean found = false;
					for(Sample nsList : neList.samples)
					{
						if (nsList.layer == Generic.tech.portNode)
						{
							String otherName = Info.getPortName(nsList.node);
							if (otherName == null)
							{
								pointOutError(nsList.node, nsList.node.getParent());
								System.out.println("Cell " + np.describe(true) + ": port does not have a name");
								return true;
							}
							if (!name.equalsIgnoreCase(otherName)) continue;
							ns.assoc = nsList;
							found = true;
							break;
						}
					}
					if (!found)
					{
						pointOutError(null, np);
						System.out.println("Could not find port " + name + " in all examples of " + np);
						return true;
					}
					continue;
				}

				// count the number of this layer in example "ne"
				int i = 0;
				for(Sample nsList : ne.samples)
				{
					if (nsList.layer == ns.layer) i++;
				}

				// if number of similar layers differs: error
				if (total != i)
				{
					pointOutError(ns.node, ns.node.getParent());
					System.out.println("Layer " + getSampleName(ns.layer) + " found " + total + " times in main example, " + i + " in other");
					System.out.println("Make the counts consistent");
					return true;
				}

				// make a list of samples on this layer in original
				List<Sample> mainList = new ArrayList<Sample>();
				i = 0;
				for(Sample nsList : neList.samples)
				{
					if (nsList.layer == ns.layer) mainList.add(nsList);
				}

				// make a list of samples on this layer in example "ne"
				List<Sample> thisList = new ArrayList<Sample>();
				i = 0;
				for(Sample nsList : ne.samples)
				{
					if (nsList.layer == ns.layer) thisList.add(nsList);
				}

				// sort each list in X/Y/shape
				Collections.sort(mainList, new SampleCoordAscending());
				Collections.sort(thisList, new SampleCoordAscending());

				// see if the lists have duplication
				for(i=1; i<total; i++)
				{
					Sample thisSample = thisList.get(i);
					Sample lastSample = thisList.get(i-1);
					Sample thisMainSample = mainList.get(i);
					Sample lastMainSample = mainList.get(i-1);
					if ((thisSample.xPos == lastSample.xPos &&
							thisSample.yPos == lastSample.yPos &&
							thisSample.node.getProto() == lastSample.node.getProto()) ||
						(thisMainSample.xPos == lastMainSample.xPos &&
							thisMainSample.yPos == lastMainSample.yPos &&
							thisMainSample.node.getProto() == lastMainSample.node.getProto())) break;
				}
				if (i >= total)
				{
					// association can be made in X
					for(i=0; i<total; i++)
					{
						Sample thisSample = thisList.get(i);
						thisSample.assoc = mainList.get(i);
					}
					continue;
				}

				// don't know how to associate this sample
				Sample thisSample = thisList.get(i);
				pointOutError(thisSample.node, thisSample.node.getParent());
				System.out.println("Sample " + getSampleName(thisSample.layer) + " is unassociated in " + np);
				return true;
			}

			// final check: make sure every sample in original example associates
			for(Sample nsList : neList.samples)
			{
				nsList.assoc = null;
			}
			for(Sample ns : ne.samples)
			{
				ns.assoc.assoc = ns;
			}
			for(Sample nsList : neList.samples)
			{
				if (nsList.assoc == null)
				{
					if (nsList.layer == Generic.tech.cellCenterNode) continue;
					pointOutError(nsList.node, nsList.node.getParent());
					System.out.println("Layer " + getSampleName(nsList.layer) + " found in main example, but not others in " + np);
					return true;
				}
			}
		}
		return false;
	}

	static void pointOutError(NodeInst ni, Cell cell)
	{
		Job.getUserInterface().setCurrentCell(cell.getLibrary(), cell);
		EditWindow_ wnd = Job.getUserInterface().needCurrentEditWindow_();
		if (wnd != null && ni != null)
		{
			wnd.clearHighlighting();
			wnd.addElectricObject(ni, cell);
			wnd.finishedHighlighting();
		}
	}

	private static class SampleCoordAscending implements Comparator<Sample>
	{
		public int compare(Sample s1, Sample s2)
		{
			if (s1.xPos != s2.xPos) return (int)(s1.xPos - s2.xPos);
			if (s1.yPos != s2.yPos) return (int)(s1.yPos - s2.yPos);
			return s1.node.getName().compareTo(s2.node.getName());
		}
	}

	/* flags about the edge positions in the examples */
	private static final int TOEDGELEFT     =   01;		/* constant to left edge */
	private static final int TOEDGERIGHT    =   02;		/* constant to right edge */
	private static final int TOEDGETOP      =   04;		/* constant to top edge */
	private static final int TOEDGEBOT      =  010;		/* constant to bottom edge */
	private static final int FROMCENTX      =  020;		/* constant in X to center */
	private static final int FROMCENTY      =  040;		/* constant in Y to center */
	private static final int RATIOCENTX     = 0100;		/* fixed ratio from X center to edge */
	private static final int RATIOCENTY     = 0200;		/* fixed ratio from Y center to edge */

	private static Poly.Type getStyle(NodeInst ni)
	{
		// layer style
		Poly.Type sty = null;
		if (ni.getProto() == Artwork.tech.filledBoxNode)             sty = Poly.Type.FILLED; else
		if (ni.getProto() == Artwork.tech.boxNode)                   sty = Poly.Type.CLOSED; else
		if (ni.getProto() == Artwork.tech.crossedBoxNode)            sty = Poly.Type.CROSSED; else
		if (ni.getProto() == Artwork.tech.filledPolygonNode)         sty = Poly.Type.FILLED; else
		if (ni.getProto() == Artwork.tech.closedPolygonNode)         sty = Poly.Type.CLOSED; else
		if (ni.getProto() == Artwork.tech.openedPolygonNode)         sty = Poly.Type.OPENED; else
		if (ni.getProto() == Artwork.tech.openedDottedPolygonNode)   sty = Poly.Type.OPENEDT1; else
		if (ni.getProto() == Artwork.tech.openedDashedPolygonNode)   sty = Poly.Type.OPENEDT2; else
		if (ni.getProto() == Artwork.tech.openedThickerPolygonNode)  sty = Poly.Type.OPENEDT3; else
		if (ni.getProto() == Artwork.tech.filledCircleNode)          sty = Poly.Type.DISC; else
		if (ni.getProto() == Artwork.tech.circleNode)
		{
			sty = Poly.Type.CIRCLE;
			double [] angles = ni.getArcDegrees();
			if (angles[0] != 0 || angles[1] != 0) sty = Poly.Type.CIRCLEARC;
		} else if (ni.getProto() == Artwork.tech.thickCircleNode)
		{
			sty = Poly.Type.THICKCIRCLE;
			double [] angles = ni.getArcDegrees();
			if (angles[0] != 0 || angles[1] != 0) sty = Poly.Type.THICKCIRCLEARC;
		} else if (ni.getProto() == Generic.tech.invisiblePinNode)
		{
			Variable var = ni.getVar(Artwork.ART_MESSAGE);
			if (var != null)
			{
				TextDescriptor.Position pos = var.getTextDescriptor().getPos();
				if (pos == TextDescriptor.Position.BOXED)     sty = Poly.Type.TEXTBOX; else
				if (pos == TextDescriptor.Position.CENT)      sty = Poly.Type.TEXTCENT; else
				if (pos == TextDescriptor.Position.UP)        sty = Poly.Type.TEXTBOT; else
				if (pos == TextDescriptor.Position.DOWN)      sty = Poly.Type.TEXTTOP; else
				if (pos == TextDescriptor.Position.LEFT)      sty = Poly.Type.TEXTRIGHT; else
				if (pos == TextDescriptor.Position.RIGHT)     sty = Poly.Type.TEXTLEFT; else
				if (pos == TextDescriptor.Position.UPLEFT)    sty = Poly.Type.TEXTBOTRIGHT; else
				if (pos == TextDescriptor.Position.UPRIGHT)   sty = Poly.Type.TEXTBOTLEFT; else
				if (pos == TextDescriptor.Position.DOWNLEFT)  sty = Poly.Type.TEXTTOPRIGHT; else
				if (pos == TextDescriptor.Position.DOWNRIGHT) sty = Poly.Type.TEXTTOPLEFT;
			}
		}
		if (sty == null)
			System.out.println("Cannot determine style to use for " + ni.getProto() +
				" node in " + ni.getParent());
		return sty;
	}

	private static NodeInfo.LayerDetails [] makeNodeScaledUniformly(Example neList, NodeProto np, LayerInfo [] lis)
	{
		// count the number of real layers in the node
		int count = 0;
		for(Sample ns : neList.samples)
		{
			if (ns.layer != null && ns.layer != Generic.tech.portNode) count++;
		}

		NodeInfo.LayerDetails [] nodeLayers = new NodeInfo.LayerDetails[count];
		count = 0;
		for(Sample ns : neList.samples)
		{
			Rectangle2D nodeBounds = getBoundingBox(ns.node);
			AffineTransform trans = ns.node.rotateOut();

			// see if there is polygonal information
			Point2D [] pointList = null;
			int [] pointFactor = null;
			Point2D [] points = null;
			if (ns.node.getProto() == Artwork.tech.filledPolygonNode ||
				ns.node.getProto() == Artwork.tech.closedPolygonNode ||
				ns.node.getProto() == Artwork.tech.openedPolygonNode ||
				ns.node.getProto() == Artwork.tech.openedDottedPolygonNode ||
				ns.node.getProto() == Artwork.tech.openedDashedPolygonNode ||
				ns.node.getProto() == Artwork.tech.openedThickerPolygonNode)
			{
				points = ns.node.getTrace();
			}
			if (points != null)
			{
				// fill the array
				pointList = new Point2D[points.length];
				pointFactor = new int[points.length];
				for(int i=0; i<points.length; i++)
				{
					pointList[i] = new Point2D.Double(nodeBounds.getCenterX() + points[i].getX(),
						nodeBounds.getCenterY() + points[i].getY());
					trans.transform(pointList[i], pointList[i]);
					pointFactor[i] = RATIOCENTX|RATIOCENTY;
				}
			} else
			{
				// see if it is an arc of a circle
				double [] angles = null;
				if (ns.node.getProto() == Artwork.tech.circleNode || ns.node.getProto() == Artwork.tech.thickCircleNode)
				{
					angles = ns.node.getArcDegrees();
					if (angles[0] == 0 && angles[1] == 0) angles = null;
				}

				// set sample description
				if (angles != null)
				{
					// handle circular arc sample
					pointList = new Point2D[3];
					pointFactor = new int[3];
					pointList[0] = new Point2D.Double(nodeBounds.getCenterX(), nodeBounds.getCenterY());
					double dist = nodeBounds.getMaxX() - nodeBounds.getCenterX();
					pointList[1] = new Point2D.Double(nodeBounds.getCenterX() + dist * Math.cos(angles[0]),
						nodeBounds.getCenterY() + dist * Math.sin(angles[0]));
					trans.transform(pointList[1], pointList[1]);
					pointFactor[0] = FROMCENTX|FROMCENTY;
					pointFactor[1] = RATIOCENTX|RATIOCENTY;
					pointFactor[2] = RATIOCENTX|RATIOCENTY;
				} else if (ns.node.getProto() == Artwork.tech.circleNode || ns.node.getProto() == Artwork.tech.thickCircleNode ||
					ns.node.getProto() == Artwork.tech.filledCircleNode)
				{
					// handle circular sample
					pointList = new Point2D[2];
					pointFactor = new int[2];
					pointList[0] = new Point2D.Double(nodeBounds.getCenterX(), nodeBounds.getCenterY());
					pointList[1] = new Point2D.Double(nodeBounds.getMaxX(), nodeBounds.getCenterY());
					pointFactor[0] = FROMCENTX|FROMCENTY;
					pointFactor[1] = TOEDGERIGHT|FROMCENTY;
				} else
				{
					// rectangular sample: get the bounding box in (px, py)
					pointList = new Point2D[2];
					pointFactor = new int[2];
					pointList[0] = new Point2D.Double(nodeBounds.getMinX(), nodeBounds.getMinY());
					pointList[1] = new Point2D.Double(nodeBounds.getMaxX(), nodeBounds.getMaxY());

					// preset stretch factors to go to the edges of the box
					pointFactor[0] = TOEDGELEFT|TOEDGEBOT;
					pointFactor[1] = TOEDGERIGHT|TOEDGETOP;
				}
			}

			// add the rule to the collection
			Technology.TechPoint [] newRule = stretchPoints(pointList, pointFactor, ns, np, neList);
			if (newRule == null)
			{
				System.out.println("Error creating stretch point in " + np);
				return null;
			}
			ns.msg = Info.getValueOnNode(ns.node);
			if (ns.msg != null && ns.msg.length() == 0) ns.msg = null;
			ns.values = newRule;

			// stop now if a highlight or port object
			if (ns.layer == null || ns.layer == Generic.tech.portNode) continue;

			// determine the layer
			LayerInfo layer = null;
			String desiredLayer = ns.layer.getName().substring(6);
			for(int i=0; i<lis.length; i++)
			{
				if (desiredLayer.equals(lis[i].name)) { layer = lis[i];   break; }
			}
			if (layer == null)
			{
				System.out.println("Cannot find layer " + desiredLayer);
				return null;
			}
			nodeLayers[count] = new NodeInfo.LayerDetails();
			nodeLayers[count].layer = layer;
			nodeLayers[count].ns = ns;
			nodeLayers[count].style = getStyle(ns.node);
			nodeLayers[count].representation = Technology.NodeLayer.POINTS;
			nodeLayers[count].values = fixValues(np, ns.values);
			if (nodeLayers[count].values.length == 2)
			{
				if (nodeLayers[count].style == Poly.Type.CROSSED ||
					nodeLayers[count].style == Poly.Type.FILLED ||
					nodeLayers[count].style == Poly.Type.CLOSED)
				{
					nodeLayers[count].representation = Technology.NodeLayer.BOX;
//					Variable var2 = ns.node.getVar(Info.MINSIZEBOX_KEY);
//					if (var2 != null) nodeLayers[count].representation = Technology.NodeLayer.MINBOX;
				}
			}
			count++;
		}
		return nodeLayers;
	}

	private static Technology.TechPoint [] fixValues(NodeProto np, Technology.TechPoint [] currentList)
	{
		EdgeH h1 = null, h2 = null, h3 = null;
		EdgeV v1 = null, v2 = null, v3 = null;
		for(int p=0; p<currentList.length; p++)
		{
			EdgeH h = currentList[p].getX();
			if (h.equals(h1) || h.equals(h2) || h.equals(h3)) continue;
			if (h1 == null) h1 = h; else
				if (h2 == null) h2 = h; else
					h3 = h;	
			EdgeV v = currentList[p].getY();
			if (v.equals(v1) || v.equals(v2) || v.equals(v3)) continue;
			if (v1 == null) v1 = v; else
				if (v2 == null) v2 = v; else
					v3 = v;				
		}
		if (h1 != null && h2 != null && h3 == null &&
			v1 != null && v2 != null && v3 == null)
		{
			// reduce to a box with two points
			currentList = new Technology.TechPoint[2];
			currentList[0] = new Technology.TechPoint(h1, v1);
			currentList[1] = new Technology.TechPoint(h2, v2);
		}
		return currentList;
	}
	
	private static NodeInfo.LayerDetails [] makePrimitiveNodeLayers(Example neList, Cell np, LayerInfo [] lis)
	{
		// if there is only one example: make sample scale with edge
		if (neList.nextExample == null)
		{
			return makeNodeScaledUniformly(neList, np, lis);
		}

		// count the number of real layers in the node
		int count = 0;
		for(Sample ns : neList.samples)
		{
			if (ns.layer != null && ns.layer != Generic.tech.portNode) count++;
		}

		NodeInfo.LayerDetails [] nodeLayers = new NodeInfo.LayerDetails[count];
		count = 0;

		// look at every sample "ns" in the main example "neList"
		for(Sample ns : neList.samples)
		{
			// ignore grab point specification
			if (ns.layer == Generic.tech.cellCenterNode) continue;
			AffineTransform trans = ns.node.rotateOut();
			Rectangle2D nodeBounds = getBoundingBox(ns.node);

			// determine the layer
			LayerInfo giLayer = null;
			if (ns.layer != null && ns.layer != Generic.tech.portNode)
			{
				String desiredLayer = ns.layer.getName().substring(6);
				for(int i=0; i<lis.length; i++)
				{
					if (desiredLayer.equals(lis[i].name)) { giLayer = lis[i];   break; }
				}
				if (giLayer == null)
				{
					System.out.println("Cannot find layer " + desiredLayer);
					return null;
				}
			}

			// look at other examples and find samples associated with this
			neList.studySample = ns;
			NodeInfo.LayerDetails multiRule = null;
			for(Example ne = neList.nextExample; ne != null; ne = ne.nextExample)
			{
				// count number of samples associated with the main sample
				int total = 0;
				for(Sample nso : ne.samples)
				{
					if (nso.assoc == ns)
					{
						ne.studySample = nso;
						total++;
					}
				}
				if (total == 0)
				{
					pointOutError(ns.node, ns.node.getParent());
					System.out.println("Still unassociated sample in " + np + " (shouldn't happen)");
					return null;
				}

				// if there are multiple associations, it must be a contact cut
				if (total > 1)
				{
					// make sure the layer is real geometry, not highlight or a port
					if (ns.layer == null || ns.layer == Generic.tech.portNode)
					{
						pointOutError(ns.node, ns.node.getParent());
						System.out.println("Only contact layers may be iterated in examples of " + np);
						return null;
					}

					// add the rule
					multiRule = getMultiCutRule(ns, neList, np);
					if (multiRule != null) break;
				}
			}
			if (multiRule != null)
			{
				multiRule.layer = giLayer;
				nodeLayers[count] = multiRule;
				count++;
				continue;
			}

			// associations done for this sample, now analyze them
			Point2D [] pointList = null;
			int [] pointFactor = null;
			Point2D [] points = null;
			if (ns.node.getProto() == Artwork.tech.filledPolygonNode ||
				ns.node.getProto() == Artwork.tech.closedPolygonNode ||
				ns.node.getProto() == Artwork.tech.openedPolygonNode ||
				ns.node.getProto() == Artwork.tech.openedDottedPolygonNode ||
				ns.node.getProto() == Artwork.tech.openedDashedPolygonNode ||
				ns.node.getProto() == Artwork.tech.openedThickerPolygonNode)
			{
				points = ns.node.getTrace();
			}
			int trueCount = 0;
			int minFactor = 0;
			if (points != null)
			{
				// make sure the arrays hold "count" points
				pointList = new Point2D[points.length];
				pointFactor = new int[points.length];
				for(int i=0; i<points.length; i++)
				{
					pointList[i] = new Point2D.Double(nodeBounds.getCenterX() + points[i].getX(),
						nodeBounds.getCenterY() + points[i].getY());
					trans.transform(pointList[i], pointList[i]);
				}
				trueCount = points.length;
			} else
			{
				double [] angles = null;
				if (ns.node.getProto() == Artwork.tech.circleNode || ns.node.getProto() == Artwork.tech.thickCircleNode)
				{
					angles = ns.node.getArcDegrees();
					if (angles[0] == 0 && angles[1] == 0) angles = null;
				}
//				if (angles == null)
//				{
//					Variable var2 = ns.node.getVar(Info.MINSIZEBOX_KEY);
//					if (var2 != null) minFactor = 2;
//				}

				// set sample description
				if (angles != null)
				{
					// handle circular arc sample
					pointList = new Point2D[3];
					pointFactor = new int[3];
					pointList[0] = new Point2D.Double(nodeBounds.getCenterX(), nodeBounds.getCenterY());
					double dist = nodeBounds.getMaxX() - nodeBounds.getCenterX();
					pointList[1] = new Point2D.Double(nodeBounds.getCenterX() + dist * Math.cos(angles[0]),
						nodeBounds.getCenterY() + dist * Math.sin(angles[0]));
					trans.transform(pointList[1], pointList[1]);
					trueCount = 3;
				} else if (ns.node.getProto() == Artwork.tech.circleNode || ns.node.getProto() == Artwork.tech.thickCircleNode ||
					ns.node.getProto() == Artwork.tech.filledCircleNode)
				{
					// handle circular sample
					pointList = new Point2D[2+minFactor];
					pointFactor = new int[2+minFactor];
					pointList[0] = new Point2D.Double(nodeBounds.getCenterX(), nodeBounds.getCenterY());
					pointList[1] = new Point2D.Double(nodeBounds.getMaxX(), nodeBounds.getCenterY());
					trueCount = 2;
				} else
				{
					// rectangular sample: get the bounding box in (pointListx, pointListy)
					pointList = new Point2D[2+minFactor];
					pointFactor = new int[2+minFactor];
					pointList[0] = new Point2D.Double(nodeBounds.getMinX(), nodeBounds.getMinY());
					pointList[1] = new Point2D.Double(nodeBounds.getMaxX(), nodeBounds.getMaxY());
					trueCount = 2;
				}
//				if (minFactor > 1)
//				{
//					pointList[2] = new Point2D.Double(pointList[0].getX(),pointList[0].getY());
//					pointList[3] = new Point2D.Double(pointList[1].getX(),pointList[1].getY());
//				}
			}

			double [] pointLeftDist = new double[pointFactor.length];
			double [] pointRightDist = new double[pointFactor.length];
			double [] pointBottomDist = new double[pointFactor.length];
			double [] pointTopDist = new double[pointFactor.length];
			double [] centerXDist = new double[pointFactor.length];
			double [] centerYDist = new double[pointFactor.length];
			double [] pointXRatio = new double[pointFactor.length];
			double [] pointYRatio = new double[pointFactor.length];
			for(int i=0; i<pointFactor.length; i++)
			{
				pointLeftDist[i] = pointList[i].getX() - neList.lx;
				pointRightDist[i] = neList.hx - pointList[i].getX();
				pointBottomDist[i] = pointList[i].getY() - neList.ly;
				pointTopDist[i] = neList.hy - pointList[i].getY();
				centerXDist[i] = pointList[i].getX() - (neList.lx+neList.hx)/2;
				centerYDist[i] = pointList[i].getY() - (neList.ly+neList.hy)/2;
				if (neList.hx == neList.lx) pointXRatio[i] = 0; else
					pointXRatio[i] = (pointList[i].getX() - (neList.lx+neList.hx)/2) / (neList.hx-neList.lx);
				if (neList.hy == neList.ly) pointYRatio[i] = 0; else
					pointYRatio[i] = (pointList[i].getY() - (neList.ly+neList.hy)/2) / (neList.hy-neList.ly);
				if (i < trueCount)
					pointFactor[i] = TOEDGELEFT | TOEDGERIGHT | TOEDGETOP | TOEDGEBOT | FROMCENTX |
						FROMCENTY | RATIOCENTX | RATIOCENTY; else
							pointFactor[i] = FROMCENTX | FROMCENTY;
			}

			Point2D [] pointCoords = new Point2D[pointFactor.length];
			for(Example ne = neList.nextExample; ne != null; ne = ne.nextExample)
			{
				NodeInst ni = ne.studySample.node;
				AffineTransform oTrans = ni.rotateOut();
				Rectangle2D oNodeBounds = getBoundingBox(ni);
				Point2D [] oPoints = null;
				if (ni.getProto() == Artwork.tech.filledPolygonNode ||
					ni.getProto() == Artwork.tech.closedPolygonNode ||
					ni.getProto() == Artwork.tech.openedPolygonNode ||
					ni.getProto() == Artwork.tech.openedDottedPolygonNode ||
					ni.getProto() == Artwork.tech.openedDashedPolygonNode ||
					ni.getProto() == Artwork.tech.openedThickerPolygonNode)
				{
					oPoints = ni.getTrace();
				}
				int newCount = 2;
				if (oPoints != null)
				{
					newCount = oPoints.length;
					int numPoints = Math.min(trueCount, newCount);
					int bestOffset = 0;
					double bestDist = Double.MAX_VALUE;
					for(int offset = 0; offset < numPoints; offset++)
					{
						// determine total distance between points
						double dist = 0;
						for(int i=0; i<numPoints; i++)
						{
							double dX = points[i].getX() - oPoints[(i+offset)%numPoints].getX();
							double dY = points[i].getY() - oPoints[(i+offset)%numPoints].getY();
							dist += Math.hypot(dX, dY);
						}
						if (dist < bestDist)
						{
							bestDist = dist;
							bestOffset = offset;
						}
					}
					for(int i=0; i<numPoints; i++)
					{
						pointCoords[i] = new Point2D.Double(oNodeBounds.getCenterX() + oPoints[(i+bestOffset)%numPoints].getX(),
							oNodeBounds.getCenterY() + oPoints[(i+bestOffset)%numPoints].getY());
						oTrans.transform(pointCoords[i], pointCoords[i]);
					}
				} else
				{
					double [] angles = null;
					if (ni.getProto() == Artwork.tech.circleNode || ni.getProto() == Artwork.tech.thickCircleNode)
					{
						angles = ni.getArcDegrees();
						if (angles[0] == 0 && angles[1] == 0) angles = null;
					}
					if (angles != null)
					{
						pointCoords[0] = new Point2D.Double(oNodeBounds.getCenterX(), oNodeBounds.getCenterY());
						double dist = oNodeBounds.getMaxX() - oNodeBounds.getCenterX();
						pointCoords[1] = new Point2D.Double(oNodeBounds.getCenterX() + dist * Math.cos(angles[0]),
							oNodeBounds.getCenterY() + dist * Math.sin(angles[0]));
						oTrans.transform(pointCoords[1], pointCoords[1]);
					} else if (ni.getProto() == Artwork.tech.circleNode || ni.getProto() == Artwork.tech.thickCircleNode ||
						ni.getProto() == Artwork.tech.filledCircleNode)
					{
						pointCoords[0] = new Point2D.Double(oNodeBounds.getCenterX(), oNodeBounds.getCenterY());
						pointCoords[1] = new Point2D.Double(oNodeBounds.getMaxX(), oNodeBounds.getCenterY());
					} else
					{
						pointCoords[0] = new Point2D.Double(oNodeBounds.getMinX(), oNodeBounds.getMinY());
						pointCoords[1] = new Point2D.Double(oNodeBounds.getMaxX(), oNodeBounds.getMaxY());
					}
				}
				if (newCount != trueCount)
				{
					pointOutError(ni, ni.getParent());
					System.out.println("Main example of " + getSampleName(ne.studySample.layer) +
						" has " + trueCount + " points but this has " + newCount + " in " + np);
					return null;
				}

				for(int i=0; i<trueCount; i++)
				{
					// see if edges are fixed distance from example edge
					if (!DBMath.areEquals(pointLeftDist[i], pointCoords[i].getX() - ne.lx)) pointFactor[i] &= ~TOEDGELEFT;
					if (!DBMath.areEquals(pointRightDist[i], ne.hx - pointCoords[i].getX())) pointFactor[i] &= ~TOEDGERIGHT;
					if (!DBMath.areEquals(pointBottomDist[i], pointCoords[i].getY() - ne.ly)) pointFactor[i] &= ~TOEDGEBOT;
					if (!DBMath.areEquals(pointTopDist[i], ne.hy - pointCoords[i].getY())) pointFactor[i] &= ~TOEDGETOP;

					// see if edges are fixed distance from example center
					if (!DBMath.areEquals(centerXDist[i], pointCoords[i].getX() - (ne.lx+ne.hx)/2)) pointFactor[i] &= ~FROMCENTX;
					if (!DBMath.areEquals(centerYDist[i], pointCoords[i].getY() - (ne.ly+ne.hy)/2)) pointFactor[i] &= ~FROMCENTY;

					// see if edges are fixed ratio from example center
					double r = 0;
					if (ne.hx != ne.lx)
						r = (pointCoords[i].getX() - (ne.lx+ne.hx)/2) / (ne.hx-ne.lx);
					if (!DBMath.areEquals(r, pointXRatio[i])) pointFactor[i] &= ~RATIOCENTX;
					if (ne.hy == ne.ly) r = 0; else
						r = (pointCoords[i].getY() - (ne.ly+ne.hy)/2) / (ne.hy-ne.ly);
					if (!DBMath.areEquals(r, pointYRatio[i])) pointFactor[i] &= ~RATIOCENTY;
				}

				// make sure port information is on the primary example
				if (ns.layer != Generic.tech.portNode) continue;

				// check port angle
				Variable var = ns.node.getVar(Info.PORTANGLE_KEY);
				Variable var2 = ni.getVar(Info.PORTANGLE_KEY);
				if (var == null && var2 != null)
				{
					pointOutError(null, np);
					System.out.println("Warning: moving port angle to main example of " + np);
					ns.node.newVar(Info.PORTANGLE_KEY, var2.getObject());
				}

				// check port range
				var = ns.node.getVar(Info.PORTRANGE_KEY);
				var2 = ni.getVar(Info.PORTRANGE_KEY);
				if (var == null && var2 != null)
				{
					pointOutError(null, np);
					System.out.println("Warning: moving port range to main example of " + np);
					ns.node.newVar(Info.PORTRANGE_KEY, var2.getObject());
				}

				// check connectivity
				var = ns.node.getVar(Info.CONNECTION_KEY);
				var2 = ni.getVar(Info.CONNECTION_KEY);
				if (var == null && var2 != null)
				{
					pointOutError(null, np);
					System.out.println("Warning: moving port connections to main example of " + np);
					ns.node.newVar(Info.CONNECTION_KEY, var2.getObject());
				}
			}

			// error check for the highlight layer
			if (ns.layer == null)
			{
				for(int i=0; i<trueCount; i++)
					if ((pointFactor[i]&(TOEDGELEFT|TOEDGERIGHT)) == 0 ||
						(pointFactor[i]&(TOEDGETOP|TOEDGEBOT)) == 0)
				{
					pointOutError(ns.node, ns.node.getParent());
					System.out.println("Highlight must be constant distance from edge in " + np);
					return null;
				}
			}

			// finally, make a rule for this sample
			Technology.TechPoint [] newRule = stretchPoints(pointList, pointFactor, ns, np, neList);
			if (newRule == null) return null;

			// add the rule to the global list
			ns.msg = Info.getValueOnNode(ns.node);
			if (ns.msg != null && ns.msg.length() == 0) ns.msg = null;
			ns.values = newRule;

			// stop now if a highlight or port object
			if (ns.layer == null || ns.layer == Generic.tech.portNode) continue;

			nodeLayers[count] = new NodeInfo.LayerDetails();
			nodeLayers[count].layer = giLayer;
			nodeLayers[count].ns = ns;
			nodeLayers[count].style = getStyle(ns.node);
			nodeLayers[count].representation = Technology.NodeLayer.POINTS;
			nodeLayers[count].values = fixValues(np, ns.values);
			if (nodeLayers[count].values.length == 2)
			{
				if (nodeLayers[count].style == Poly.Type.CROSSED ||
					nodeLayers[count].style == Poly.Type.FILLED ||
					nodeLayers[count].style == Poly.Type.CLOSED)
				{
					nodeLayers[count].representation = Technology.NodeLayer.BOX;
//					if (minFactor != 0)
//						nodeLayers[count].representation = Technology.NodeLayer.MINBOX;
				}
			}
			count++;
		}
		if (count != nodeLayers.length)
			System.out.println("Generated only " + count + " of " + nodeLayers.length + " layers for " + np);
		return nodeLayers;
	}

	/**
	 * Method to return the actual bounding box of layer node "ni" in the
	 * reference variables "lx", "hx", "ly", and "hy"
	 */
	private static Rectangle2D getBoundingBox(NodeInst ni)
	{
		Rectangle2D bounds = ni.getBounds();
		if (ni.getProto() == Generic.tech.portNode)
		{
			double portShrink = 2;
			bounds.setRect(bounds.getMinX() + portShrink, bounds.getMinY() + portShrink,
				bounds.getWidth()-portShrink*2, bounds.getHeight()-portShrink*2);
		}
		bounds.setRect(DBMath.round(bounds.getMinX()), DBMath.round(bounds.getMinY()),
			DBMath.round(bounds.getWidth()), DBMath.round(bounds.getHeight()));
		return bounds;
	}

	/**
	 * Method to adjust the "count"-long array of points in "px" and "py" according
	 * to the stretch factor bits in "factor" and return an array that describes
	 * these points.  Returns zero on error.
	 */
	private static Technology.TechPoint [] stretchPoints(Point2D [] pts, int [] factor,
		Sample ns, NodeProto np, Example neList)
	{
		Technology.TechPoint [] newRule = new Technology.TechPoint[pts.length];

		for(int i=0; i<pts.length; i++)
		{
			// determine the X algorithm
			EdgeH horiz = null;
			if ((factor[i]&TOEDGELEFT) != 0)
			{
				// left edge rule
				horiz = EdgeH.fromLeft(pts[i].getX()-neList.lx);
			} else if ((factor[i]&TOEDGERIGHT) != 0)
			{
				// right edge rule
				horiz = EdgeH.fromRight(neList.hx-pts[i].getX());
			} else if ((factor[i]&FROMCENTX) != 0)
			{
				// center rule
				horiz = EdgeH.fromCenter(pts[i].getX()-(neList.lx+neList.hx)/2);
			} else if ((factor[i]&RATIOCENTX) != 0)
			{
				// constant stretch rule
				if (neList.hx == neList.lx)
				{
					horiz = EdgeH.makeCenter();
				} else
				{
					horiz = new EdgeH((pts[i].getX()-(neList.lx+neList.hx)/2) / (neList.hx-neList.lx), 0);
				}
			} else
			{
				pointOutError(ns.node, ns.node.getParent());
				System.out.println("Cannot determine X stretching rule for layer " + getSampleName(ns.layer) +
					" in " + np);
				return null;
			}

			// determine the Y algorithm
			EdgeV vert = null;
			if ((factor[i]&TOEDGEBOT) != 0)
			{
				// bottom edge rule
				vert = EdgeV.fromBottom(pts[i].getY()-neList.ly);
			} else if ((factor[i]&TOEDGETOP) != 0)
			{
				// top edge rule
				vert = EdgeV.fromTop(neList.hy-pts[i].getY());
			} else if ((factor[i]&FROMCENTY) != 0)
			{
				// center rule
				vert = EdgeV.fromCenter(pts[i].getY()-(neList.ly+neList.hy)/2);
			} else if ((factor[i]&RATIOCENTY) != 0)
			{
				// constant stretch rule
				if (neList.hy == neList.ly)
				{
					vert = EdgeV.makeCenter();
				} else
				{
					vert = new EdgeV((pts[i].getY()-(neList.ly+neList.hy)/2) / (neList.hy-neList.ly), 0);
				}
			} else
			{
				pointOutError(ns.node, ns.node.getParent());
				System.out.println("Cannot determine Y stretching rule for layer " + getSampleName(ns.layer) +
					" in " + np);
				return null;
			}
			newRule[i] = new Technology.TechPoint(horiz, vert);
		}
		return newRule;
	}

	private static Sample needHighlightLayer(Example neList, Cell np)
	{
		// find the highlight layer
		for(Sample ns : neList.samples)
		{
			if (ns.layer == null) return ns;
		}

		pointOutError(null, np);
		System.out.println("No highlight layer on contact " + np.describe(true));
		return null;
	}

	/**
	 * Method to build a rule for multiple contact-cut sample "ns" from the
	 * overall example list in "neList".  Returns true on error.
	 */
	private static NodeInfo.LayerDetails getMultiCutRule(Sample ns, Example neList, Cell np)
	{
		// find the highlight layer
		Sample hs = needHighlightLayer(neList, np);
		if (hs == null) return null;
		Rectangle2D highlightBounds = hs.node.getBounds();

		// determine size of each cut
		Rectangle2D nodeBounds = ns.node.getBounds();
		double multiXS = nodeBounds.getWidth();
		double multiYS = nodeBounds.getHeight();

		// determine indentation of cuts
		double multiIndent = nodeBounds.getMinX() - highlightBounds.getMinX();
		double realIndentX = nodeBounds.getMinX() - neList.lx + multiXS/2;
		double realIndentY = nodeBounds.getMinY() - neList.ly + multiYS/2;
		if (highlightBounds.getMaxX() - nodeBounds.getMaxX() != multiIndent ||
			nodeBounds.getMinY() - highlightBounds.getMinY() != multiIndent ||
			highlightBounds.getMaxY() - nodeBounds.getMaxY() != multiIndent)
		{
			pointOutError(ns.node, ns.node.getParent());
			System.out.println("Multiple contact cuts must be indented uniformly in " + np);
			return null;
		}

		// look at every example after the first
		double xSep = -1, ySep = -1;
		for(Example ne = neList.nextExample; ne != null; ne = ne.nextExample)
		{
			// count number of samples equivalent to the main sample
			int total = 0;
			for(Sample nso : ne.samples)
			{
				if (nso.assoc == ns)
				{
					// make sure size is proper
					Rectangle2D oNodeBounds = nso.node.getBounds();
					if (multiXS != oNodeBounds.getWidth() || multiYS != oNodeBounds.getHeight())
					{
						pointOutError(nso.node, nso.node.getParent());
						System.out.println("Multiple contact cuts must not differ in size in " + np);
						return null;
					}
					total++;
				}
			}

			// allocate space for these samples
			Sample [] nsList = new Sample[total];

			// fill the list of samples
			int fill = 0;
			for(Sample nso : ne.samples)
			{
				if (nso.assoc == ns) nsList[fill++] = nso;
			}

			// analyze the samples for separation
			for(int i=1; i<total; i++)
			{
				// find separation
				Rectangle2D thisNodeBounds = nsList[i].node.getBounds();
				Rectangle2D lastNodeBounds = nsList[i-1].node.getBounds();
				double sepX = Math.abs(lastNodeBounds.getCenterX() - thisNodeBounds.getCenterX());
				double sepY = Math.abs(lastNodeBounds.getCenterY() - thisNodeBounds.getCenterY());

				// check for validity
				if (sepX < multiXS && sepY < multiYS)
				{
					pointOutError(nsList[i].node, nsList[i].node.getParent());
					System.out.println("Multiple contact cuts must not overlap in " + np);
					return null;
				}

				// accumulate minimum separation
				if (sepX >= multiXS)
				{
					if (xSep < 0) xSep = sepX; else
					{
						if (xSep > sepX) xSep = sepX;
					}
				}
				if (sepY >= multiYS)
				{
					if (ySep < 0) ySep = sepY; else
					{
						if (ySep > sepY) ySep = sepY;
					}
				}
			}

			// finally ensure that all separations are multiples of "multiSep"
			for(int i=1; i<total; i++)
			{
				// find X separation
				Rectangle2D thisNodeBounds = nsList[i].node.getBounds();
				Rectangle2D lastNodeBounds = nsList[i-1].node.getBounds();
				double sepX = Math.abs(lastNodeBounds.getCenterX() - thisNodeBounds.getCenterX());
				double sepY = Math.abs(lastNodeBounds.getCenterY() - thisNodeBounds.getCenterY());
				if (sepX / xSep * xSep != sepX)
				{
					pointOutError(nsList[i].node, nsList[i].node.getParent());
					System.out.println("Multiple contact cut X spacing must be uniform in " + np);
					return null;
				}

				// find Y separation
				if (sepY / ySep * ySep != sepY)
				{
					pointOutError(nsList[i].node, nsList[i].node.getParent());
					System.out.println("Multiple contact cut Y spacing must be uniform in " + np);
					return null;
				}
			}
		}
		double multiSepX = xSep - multiXS;
		double multiSepY = ySep - multiYS;
		if (multiSepX != multiSepY)
		{
			pointOutError(null, np);
			System.out.println("Multiple contact cut X and Y spacing must be the same in " + np);
			return null;
		}
		ns.values = new Technology.TechPoint[2];
		ns.values[0] = new Technology.TechPoint(EdgeH.fromLeft(realIndentX), EdgeV.fromBottom(realIndentY));
		ns.values[1] = new Technology.TechPoint(EdgeH.fromRight(realIndentX), EdgeV.fromTop(realIndentY));

		NodeInfo.LayerDetails multiDetails = new NodeInfo.LayerDetails();
		multiDetails.style = getStyle(ns.node);
		multiDetails.representation = Technology.NodeLayer.POINTS;
		if (multiDetails.style == Poly.Type.CROSSED ||
			multiDetails.style == Poly.Type.FILLED ||
			multiDetails.style == Poly.Type.CLOSED)
		{
			multiDetails.representation = Technology.NodeLayer.BOX;
//			Variable var2 = ns.node.getVar(Info.MINSIZEBOX_KEY);
//			if (var2 != null) multiDetails.representation = Technology.NodeLayer.MINBOX;
		}
		multiDetails.values = ns.values;
		multiDetails.ns = ns;
		multiDetails.multiCut = true;
		multiDetails.representation = Technology.NodeLayer.MULTICUTBOX;
		multiDetails.multiXS = multiXS;
		multiDetails.multiYS = multiYS;
		multiDetails.multiIndent = multiIndent;
		multiDetails.multiSep = multiSepX;
		multiDetails.multiSep2D = multiSepX;
		return multiDetails;
	}

//	/****************************** WRITE TECHNOLOGY AS "JAVA" CODE ******************************/
//
//    private static int NUM_FRACTIONS = 0; // was 3
//
//    /**
//     * Dump technology information to Java
//     * @param fileName name of file to write
//     * @param newTechName new technology name
//     * @param gi general technology information
//     * @param lList information about layers
//     * @param nList information about primitive nodes
//     * @param aList information about primitive arcs.
//     */
//    private static void dumpToJava(String fileName, String newTechName, GeneralInfo gi, LayerInfo[] lList, NodeInfo[] nList, ArcInfo[] aList) {
//        try {
//            PrintStream buffWriter = new PrintStream(new FileOutputStream(fileName));
//            dumpToJava(buffWriter, newTechName, gi, lList, nList, aList);
//            buffWriter.close();
//            System.out.println("Wrote " + fileName);
//        } catch (IOException e) {
//            System.out.println("Error creating " + fileName);
//        }
//    }
//    
//    /**
//     * write the layers, arcs, and nodes
//     */
//    private static void dumpToJava(PrintStream buffWriter, String newTechName, GeneralInfo gi, LayerInfo[] lList, NodeInfo[] nList, ArcInfo[] aList) {
//        // write the layers, arcs, and nodes
//        dumpLayersToJava(buffWriter, newTechName, lList, gi);
//        dumpArcsToJava(buffWriter, newTechName, aList, gi);
//        dumpNodesToJava(buffWriter, newTechName, nList, lList, gi);
//        dumpPaletteToJava(buffWriter, gi.menuPalette);
//        dumpFoundryToJava(buffWriter, gi, lList);
//		buffWriter.println("\t};");
//
//		// write method to reset rules
//		if (gi.conDist != null || gi.unConDist != null)
//		{
//			String conword = gi.conDist != null ? "conDist" : "null";
//			String unconword = gi.unConDist != null ? "unConDist" : "null";
//            buffWriter.println("\t/**");
//            buffWriter.println("\t * Method to return the \"factory \"design rules for this Technology.");
//            buffWriter.println("\t * @return the design rules for this Technology.");
//            buffWriter.println("\t * @param resizeNodes");
//            buffWriter.println("\t */");
//			buffWriter.println("\tpublic DRCRules getFactoryDesignRules(boolean resizeNodes)");
//			buffWriter.println("\t{");
//			buffWriter.println("\t\treturn MOSRules.makeSimpleRules(this, " + conword + ", " + unconword + ");");
//			buffWriter.println("\t}");
//		}
//        buffWriter.println("}");
//    }
//    
//	/**
//	 * Method to dump the layer information in technology "tech" to the stream in
//	 * "f".
//	 */
//	private static void dumpLayersToJava(PrintStream buffWriter, String techName, LayerInfo [] lList, GeneralInfo gi)
//	{
//		// write header
//		buffWriter.println("// BE SURE TO INCLUDE THIS TECHNOLOGY IN Technology.initAllTechnologies()");
//		buffWriter.println();
//		buffWriter.println("/* -*- tab-width: 4 -*-");
//		buffWriter.println(" *");
//		buffWriter.println(" * Electric(tm) VLSI Design System");
//		buffWriter.println(" *");
//		buffWriter.println(" * File: " + techName + ".java");
//		buffWriter.println(" * " + techName + " technology description");
//		buffWriter.println(" * Generated automatically from a library");
//		buffWriter.println(" *");
//		Calendar cal = Calendar.getInstance();
//		cal.setTime(new Date());
//		buffWriter.println(" * Copyright (c) " + cal.get(Calendar.YEAR) + " Sun Microsystems and Static Free Software");
//		buffWriter.println(" *");
//		buffWriter.println(" * Electric(tm) is free software; you can redistribute it and/or modify");
//		buffWriter.println(" * it under the terms of the GNU General Public License as published by");
//		buffWriter.println(" * the Free Software Foundation; either version 2 of the License, or");
//		buffWriter.println(" * (at your option) any later version.");
//		buffWriter.println(" *");
//		buffWriter.println(" * Electric(tm) is distributed in the hope that it will be useful,");
//		buffWriter.println(" * but WITHOUT ANY WARRANTY; without even the implied warranty of");
//		buffWriter.println(" * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the");
//		buffWriter.println(" * GNU General Public License for more details.");
//		buffWriter.println(" *");
//		buffWriter.println(" * You should have received a copy of the GNU General Public License");
//		buffWriter.println(" * along with Electric(tm); see the file COPYING.  If not, write to");
//		buffWriter.println(" * the Free Software Foundation, Inc., 59 Temple Place, Suite 330,");
//		buffWriter.println(" * Boston, Mass 02111-1307, USA.");
//		buffWriter.println(" */");
//		buffWriter.println("package com.sun.electric.technology.technologies;");
//		buffWriter.println();
//
//		// write imports
//		buffWriter.println("import com.sun.electric.database.geometry.EGraphics;");
//		buffWriter.println("import com.sun.electric.database.geometry.Poly;");
//		buffWriter.println("import com.sun.electric.database.prototype.PortCharacteristic;");
//		buffWriter.println("import com.sun.electric.technology.ArcProto;");
//		buffWriter.println("import com.sun.electric.technology.DRCRules;");
//		buffWriter.println("import com.sun.electric.technology.EdgeH;");
//		buffWriter.println("import com.sun.electric.technology.EdgeV;");
//		buffWriter.println("import com.sun.electric.technology.Foundry;");
//		buffWriter.println("import com.sun.electric.technology.Layer;");
//		buffWriter.println("import com.sun.electric.technology.PrimitiveNode;");
//		buffWriter.println("import com.sun.electric.technology.PrimitivePort;");
//		buffWriter.println("import com.sun.electric.technology.SizeOffset;");
//		buffWriter.println("import com.sun.electric.technology.Technology;");
//		buffWriter.println("import com.sun.electric.technology.technologies.utils.MOSRules;");
//		buffWriter.println("import com.sun.electric.tool.erc.ERC;");
//		buffWriter.println();
//		buffWriter.println("import java.awt.Color;");
//		buffWriter.println();
//
//		buffWriter.println("/**");
//		buffWriter.println(" * This is the " + gi.description + " Technology.");
//		buffWriter.println(" */");
//		buffWriter.println("public class " + techName + " extends Technology");
//		buffWriter.println("{");
//		buffWriter.println("\t/** the " + gi.description + " Technology object. */	public static final " +
//				techName + " tech = new " + techName + "();");
//        buffWriter.println();
//        if (gi.conDist != null || gi.unConDist != null) {
//            buffWriter.println("\tprivate static final double XX = -1;");
//            buffWriter.println("\tprivate double [] conDist, unConDist;");
//            buffWriter.println();
//        }
//
//        buffWriter.println("	// -------------------- private and protected methods ------------------------");
//		buffWriter.println("\tprivate " + techName + "()");
//		buffWriter.println("\t{");
//		buffWriter.println("\t\tsuper(\"" + techName + "\", Foundry.Type." + gi.defaultFoundry + ", " + gi.defaultNumMetals + ");");
//		buffWriter.println("\t\tsetTechShortName(\"" + gi.shortName + "\");");
//		buffWriter.println("\t\tsetTechDesc(\"" + gi.description + "\");");
//		buffWriter.println("\t\tsetFactoryScale(" + TextUtils.formatDouble(gi.scale, NUM_FRACTIONS) + ", true);   // in nanometers: really " +
//			(gi.scale / 1000) + " microns");
//        buffWriter.println("\t\tsetFactoryResolution(" + gi.resolution + ");");
//        if (gi.nonElectrical)
//            buffWriter.println("\t\tsetNonElectrical();");
//		buffWriter.println("\t\tsetNoNegatedArcs();");
//		buffWriter.println("\t\tsetStaticTechnology();");
//
//		if (gi.transparentColors != null && gi.transparentColors.length > 0)
//		{
//			buffWriter.println("\t\tsetFactoryTransparentLayers(new Color []");
//			buffWriter.println("\t\t{");
//			for(int i=0; i<gi.transparentColors.length; i++)
//			{
//				Color col = gi.transparentColors[i];
//				buffWriter.print("\t\t\tnew Color(" + col.getRed() + "," + col.getGreen() + "," + col.getBlue() + ")");
//				if (i+1 < gi.transparentColors.length) buffWriter.print(",");
//				buffWriter.println();
//			}
//			buffWriter.println("\t\t});");
//		}
//		buffWriter.println();
//
//		// write the layer declarations
//		buffWriter.println("\t\t//**************************************** LAYERS ****************************************");
//		for(int i=0; i<lList.length; i++)
//		{
//			lList[i].javaName = makeJavaName(lList[i].name);
//			buffWriter.println("\t\t/** " + lList[i].name + " layer */");
//			buffWriter.println("\t\tLayer " + lList[i].javaName + "_lay = Layer.newInstance(this, \"" + lList[i].name + "\",");
//            EGraphics desc = lList[i].desc;
//			buffWriter.print("\t\t\tnew EGraphics(");
//			if (desc.isPatternedOnDisplay()) buffWriter.print("true"); else
//				buffWriter.print("false");
//			buffWriter.print(", ");
//
//			if (desc.isPatternedOnPrinter()) buffWriter.print("true"); else
//				buffWriter.print("false");
//			buffWriter.print(", ");
//
//			EGraphics.Outline o = desc.getOutlined();
//			if (o == EGraphics.Outline.NOPAT) buffWriter.print("null"); else
//				buffWriter.print("EGraphics.Outline." + o.getConstName());
//			buffWriter.print(", ");
//
//			String transparent = "0";
//			if (desc.getTransparentLayer() > 0)
//				transparent = "EGraphics.TRANSPARENT_" + desc.getTransparentLayer();
//			int red = desc.getColor().getRed();
//			int green = desc.getColor().getGreen();
//			int blue = desc.getColor().getBlue();
//			if (red < 0 || red > 255) red = 0;
//			if (green < 0 || green > 255) green = 0;
//			if (blue < 0 || blue > 255) blue = 0;
//			buffWriter.println(transparent + ", " + red + "," + green + "," + blue + ", " + desc.getOpacity() + ", " + desc.getForeground() + ",");
//
//			boolean hasPattern = false;
//			int [] pattern = desc.getPattern();
//			for(int j=0; j<16; j++) if (pattern[j] != 0) hasPattern = true;
//			if (hasPattern)
//			{
//				for(int j=0; j<16; j++)
//				{
//					buffWriter.print("\t\t\t");
//					if (j == 0) buffWriter.print("new int[] { "); else
//						buffWriter.print("\t\t\t");
//					String hexValue = Integer.toHexString(pattern[j] & 0xFFFF);
//					while (hexValue.length() < 4) hexValue = "0" + hexValue;
//					buffWriter.print("0x" + hexValue);
//					if (j == 15) buffWriter.print("}));"); else
//						buffWriter.print(",   ");
//
//					buffWriter.print("// ");
//					for(int k=0; k<16; k++)
//						if ((pattern[j] & (1 << (15-k))) != 0)
//							buffWriter.print("X"); else buffWriter.print(" ");
//					buffWriter.println();
//				}
//				buffWriter.println();
//			} else
//			{
//				buffWriter.println("\t\t\tnew int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));");
//			}
//		}
//
//		// write the layer functions
//		buffWriter.println();
//		buffWriter.println("\t\t// The layer functions");
//		for(int i=0; i<lList.length; i++)
//		{
//			Layer.Function fun = lList[i].fun;
//			int funExtra = lList[i].funExtra;
//			String infstr = lList[i].javaName + "_lay.setFunction(Layer.Function.";
//			infstr += fun.getConstantName();
//			boolean extraFunction = false;
//			int [] extras = Layer.Function.getFunctionExtras();
//			for(int j=0; j<extras.length; j++)
//			{
//				if ((funExtra&extras[j]) != 0)
//				{
//					if (extraFunction) infstr += "|"; else
//						infstr += ", ";
//					infstr += "Layer.Function.";
//					infstr += Layer.Function.getExtraConstantName(extras[j]);
//					extraFunction = true;
//				}
//			}
//            if (lList[i].pseudo) {
//					if (extraFunction) infstr += "|"; else
//						infstr += ", ";
//					infstr += "Layer.Function.PSEUDO";
//            }
//			infstr += ");";
//			buffWriter.println("\t\t" + infstr + "\t\t// " + lList[i].name);
//		}
//
//		// write the CIF layer names
//		for(int j=0; j<lList.length; j++)
//		{
//			if (lList[j].cif.length() > 0)
//			{
//				buffWriter.println("\n\t\t// The CIF names");
//				for(int i=0; i<lList.length; i++) {
//                    if (lList[i].pseudo) continue;
//					buffWriter.println("\t\t" + lList[i].javaName + "_lay.setFactoryCIFLayer(\"" + lList[i].cif +
//						"\");\t\t// " + lList[i].name);
//                }
//				break;
//			}
//		}
//
//		// write the DXF layer names
//		for(int j=0; j<lList.length; j++)
//		{
//			if (lList[j].dxf != null && lList[j].dxf.length() > 0)
//			{
//				buffWriter.println("\n\t\t// The DXF names");
//				for(int i=0; i<lList.length; i++) {
//                    if (lList[i].pseudo) continue;
//					buffWriter.println("\t\t" + lList[i].javaName + "_lay.setFactoryDXFLayer(\"" + lList[i].dxf +
//						"\");\t\t// " + lList[i].name);
//                }
//				break;
//			}
//		}
//
//		// write the Skill layer names
//		for(int j=0; j<lList.length; j++)
//		{
//			if (lList[j].skill != null && lList[j].skill.length() > 0)
//			{
//				buffWriter.println("\n\t\t// The Skill names");
//				for(int i=0; i<lList.length; i++) {
//                    if (lList[i].pseudo) continue;
//					buffWriter.println("\t\t" + lList[i].javaName + "_lay.setFactorySkillLayer(\"" + lList[i].skill +
//						"\");\t\t// " + lList[i].name);
//                }
//				break;
//			}
//		}
//
//		// write the 3D information
//		for(int j=0; j<lList.length; j++)
//		{
//			if (lList[j].thick3d != 0 || lList[j].height3d != 0)
//			{
//				buffWriter.println("\n\t\t// The layer height");
//				for(int i=0; i<lList.length; i++) {
//                    if (lList[i].pseudo) continue;
//					buffWriter.println("\t\t" + lList[i].javaName + "_lay.setFactory3DInfo(" +
//						TextUtils.formatDouble(lList[i].thick3d, NUM_FRACTIONS) + ", " + TextUtils.formatDouble(lList[i].height3d, NUM_FRACTIONS) +
//						");\t\t// " + lList[i].name);
//                }
//				break;
//			}
//		}
//
//		// write the SPICE information
//		for(int j=0; j<lList.length; j++)
//		{
//			if (lList[j].spiRes != 0 || lList[j].spiCap != 0 || lList[j].spiECap != 0)
//			{
//				buffWriter.println("\n\t\t// The SPICE information");
//				for(int i=0; i<lList.length; i++)
//					buffWriter.println("\t\t" + lList[i].javaName + "_lay.setFactoryParasitics(" +
//						TextUtils.formatDouble(lList[i].spiRes, NUM_FRACTIONS) + ", " +
//						TextUtils.formatDouble(lList[i].spiCap, NUM_FRACTIONS) + ", " +
//						TextUtils.formatDouble(lList[i].spiECap, NUM_FRACTIONS) + ");\t\t// " + lList[i].name);
//				break;
//			}
//		}
//		buffWriter.println("\t\tsetFactoryParasitics(" + gi.minRes + ", " + gi.minCap + ");");
//        
//        dumpSpiceHeader(buffWriter, 1, gi.spiceLevel1Header);
//        dumpSpiceHeader(buffWriter, 2, gi.spiceLevel2Header);
//        dumpSpiceHeader(buffWriter, 3, gi.spiceLevel3Header);
//
//		// write design rules
//        if (gi.conDist != null || gi.unConDist != null) {
//            buffWriter.println("\n\t\t//******************** DESIGN RULES ********************");
//            
//            if (gi.conDist != null) {
//                buffWriter.println("\n\t\tconDist = new double[] {");
//                dumpJavaDrcTab(buffWriter, gi.conDist, lList);
//            }
//            if (gi.unConDist != null) {
//                buffWriter.println("\n\t\tunConDist = new double[] {");
//                dumpJavaDrcTab(buffWriter, gi.unConDist, lList);
//            }
//        }
//    }
//
//	private static void dumpJavaDrcTab(PrintStream buffWriter, double[] distances, LayerInfo[] lList/*), void *distances, TECHNOLOGY *tech, BOOLEAN isstring*/)
//	{
////		REGISTER INTBIG i, j;
////		REGISTER INTBIG amt, *amtlist;
////		CHAR shortname[7], *msg, **distlist;
////
//		for(int i=0; i<6; i++)
//		{
//			buffWriter.print("\t\t\t//            ");
//			for(int j=0; j<lList.length; j++)
//			{
//				if (lList[j].name.length() <= i) buffWriter.print(" "); else
//					buffWriter.print(lList[j].name.charAt(i));
//				buffWriter.print("  ");
//			}
//			buffWriter.println();
//		}
////		if (isstring) distlist = (CHAR **)distances; else
////			amtlist = (INTBIG *)distances;
//        int ruleNum = 0;
//		for(int j=0; j<lList.length; j++)
//		{
//			String shortName = lList[j].name;
//            if (shortName.length() > 6)
//                shortName = shortName.substring(0, 6);
//            while (shortName.length() < 6)
//                shortName += ' ';
//			buffWriter.print("\t\t\t/* " + shortName + " */ ");
//			for(int i=0; i<j; i++) buffWriter.print("   ");
//			for(int i=j; i<lList.length; i++)
//			{
////				if (isstring)
////				{
////					msg = *distlist++;
////					buffWriter.println("x_(\"%s\")"), msg);
////				} else
////				{
//					double amt = distances[ruleNum++];
//					if (amt < 0) buffWriter.print("XX"); else
//					{
//                        String amtStr = Double.toString(amt);
//                        if (amtStr.endsWith(".0"))
//                            amtStr = amtStr.substring(0, amtStr.length() - 2);
//                        while (amtStr.length() < 2)
//                            amtStr = ' ' + amtStr;
//						buffWriter.print(amtStr)/*, (float)amt/WHOLE)*/;
////					}
//				}
//				if (j != lList.length-1 || i != lList.length-1)
//					buffWriter.print(",");
//			}
//			buffWriter.println();
//		}
//		buffWriter.println("\t\t};");
//	}
//
//    private static void dumpSpiceHeader(PrintStream buffWriter, int level, String[] headerLines) {
//        if (headerLines == null) return;
//        buffWriter.println("\t\tString [] headerLevel" + level + " =");
//        buffWriter.println("\t\t{");
//        for (int i = 0; i < headerLines.length; i++) {
//            buffWriter.print("\t\t\t\"" + headerLines[i]  + "\"");
//            if (i < headerLines.length - 1)
//                buffWriter.print(',');
//            buffWriter.println();
//        }
//        buffWriter.println("\t\t};");
//		buffWriter.println("\t\tsetSpiceHeaderLevel" + level + "(headerLevel" + level + ");");
//    }
//
//	/**
//	 * Method to dump the arc information in technology "tech" to the stream in
//	 * "f".
//	 */
//	private static void dumpArcsToJava(PrintStream buffWriter, String techName, ArcInfo [] aList, GeneralInfo gi)
//	{
//		// print the header
//		buffWriter.println();
//		buffWriter.println("\t\t//******************** ARCS ********************");
//
//		// now write the arcs
//		for(int i=0; i<aList.length; i++)
//		{
//            ArcInfo aIn = aList[i];
//			aIn.javaName = makeJavaName(aIn.name);
//			buffWriter.println("\n\t\t/** " + aIn.name + " arc */");
//			buffWriter.println("\t\tArcProto " + aIn.javaName + "_arc = newArcProto(\"" + aIn.name +
//                    "\", " + TextUtils.formatDouble(aIn.widthOffset, NUM_FRACTIONS) +
//                    ", " + TextUtils.formatDouble(aIn.maxWidth, NUM_FRACTIONS) +
//                    ", ArcProto.Function." + aIn.func.getConstantName() + ",");
//			for(int k=0; k<aIn.arcDetails.length; k++)
//			{
//                ArcInfo.LayerDetails al = aList[i].arcDetails[k];
//				buffWriter.print("\t\t\tnew Technology.ArcLayer(" + al.layer.javaName + "_lay, ");
//				buffWriter.print(TextUtils.formatDouble(al.width, NUM_FRACTIONS) + ",");
//				buffWriter.print(" Poly.Type." + al.style.name() + ")");
//				if (k+1 < aList[i].arcDetails.length) buffWriter.print(",");
//				buffWriter.println();
//			}
//			buffWriter.println("\t\t);");
//			if (aIn.wipes)
//				buffWriter.println("\t\t" + aIn.javaName + "_arc.setWipable();");
//            if (aIn.curvable)
//				buffWriter.println("\t\t" + aIn.javaName + "_arc.setCurvable();");
//            if (aIn.special)
//				buffWriter.println("\t\t" + aIn.javaName + "_arc.setSpecialArc();");
//            if (aIn.notUsed)
//				buffWriter.println("\t\t" + aIn.javaName + "_arc.setNotUsed(true);");
//            if (aIn.skipSizeInPalette)
//				buffWriter.println("\t\t" + aIn.javaName + "_arc.setSkipSizeInPalette();");
//			buffWriter.println("\t\t" + aIn.javaName + "_arc.setFactoryFixedAngle(" + aIn.fixAng + ");");
//            if (!aIn.slidable)
//    			buffWriter.println("\t\t" + aIn.javaName + "_arc.setFactorySlidable(" + aIn.slidable + ");");
//			buffWriter.println("\t\t" + aIn.javaName + "_arc.setFactoryExtended(" + !aIn.noExtend + ");");
//			buffWriter.println("\t\t" + aIn.javaName + "_arc.setFactoryAngleIncrement(" + aIn.angInc + ");");
//			buffWriter.println("\t\tERC.getERCTool().setAntennaRatio(" + aIn.javaName + "_arc, " +
//				TextUtils.formatDouble(aIn.antennaRatio, NUM_FRACTIONS) + ");");
//		}
//	}
//
//	/**
//	 * Method to make an abbreviation for a string.
//	 * @param pt the string to abbreviate.
//	 * @param upper true to make it an upper-case abbreviation.
//	 * @return the abbreviation for the string.
//	 */
//	private static String makeabbrev(String pt, boolean upper)
//	{
//		// generate an abbreviated name for this prototype
//		StringBuffer infstr = new StringBuffer();
//		for(int i=0; i<pt.length(); )
//		{
//			char chr = pt.charAt(i);
//			if (Character.isLetterOrDigit(chr))
//			{
//                if (i == 0 && Character.isDigit(chr))
//                    infstr.append("_");
//				if (upper) infstr.append(Character.toUpperCase(chr)); else
//					infstr.append(Character.toLowerCase(chr));
//				while (Character.isLetterOrDigit(chr))
//				{
//					i++;
//					if (i >= pt.length()) break;
//					chr = pt.charAt(i);
//				}
//			}
//			while (!Character.isLetterOrDigit(chr))
//			{
//				i++;
//				if (i >= pt.length()) break;
//				chr = pt.charAt(i);
//			}
//		}
//		return infstr.toString();
//	}
//
//	/**
//	 * Method to dump the node information in technology "tech" to the stream in
//	 * "f".
//	 */
//	private static void dumpNodesToJava(PrintStream buffWriter, String techName, NodeInfo [] nList,
//		LayerInfo [] lList, GeneralInfo gi)
//	{
//		// make abbreviations for each node
//		HashSet<String> abbrevs = new HashSet<String>();
//		for(int i=0; i<nList.length; i++)
//		{
//			String ab = makeabbrev(nList[i].name, false);
//
//			// loop until the name is unique
//			for(;;)
//			{
//				// see if a previously assigned abbreviation is the same
//				if (!abbrevs.contains(ab)) break;
//
//				// name conflicts: change it
//				int l = ab.length() - 1;
//				char last = ab.charAt(l);
//				if (last >= '0' && last <= '8')
//				{
//					ab = ab.substring(0, l) + (char)(last+1);
//				} else
//				{
//					ab += "0";
//				}
//			}
//			abbrevs.add(ab);
//			nList[i].abbrev = ab;
//		}
//		buffWriter.println();
//
//		// print node information
//		buffWriter.println("\t\t//******************** NODES ********************");
//		for(int i=0; i<nList.length; i++)
//		{
//            NodeInfo nIn = nList[i];
//			// header comment
//			String ab = nIn.abbrev;
//			buffWriter.println();
//			buffWriter.println("\t\t/** " + nIn.name + " */");
//
//			buffWriter.print("\t\tPrimitiveNode " + ab + "_node = PrimitiveNode.newInstance(\"" +
//				nIn.name + "\", this, " + TextUtils.formatDouble(nIn.xSize, NUM_FRACTIONS) + ", " +
//				TextUtils.formatDouble(nList[i].ySize, NUM_FRACTIONS) + ", ");
//			if (nIn.so == null) buffWriter.println("null,"); else
//			{
//				buffWriter.println("new SizeOffset(" + TextUtils.formatDouble(nIn.so.getLowXOffset(), NUM_FRACTIONS) + ", " +
//					TextUtils.formatDouble(nIn.so.getHighXOffset(), NUM_FRACTIONS) + ", " +
//					TextUtils.formatDouble(nIn.so.getLowYOffset(), NUM_FRACTIONS) + ", " +
//					TextUtils.formatDouble(nIn.so.getHighYOffset(), NUM_FRACTIONS) + "),");
//			}
//
//			// print layers
//            dumpNodeLayersToJava(buffWriter, nIn.nodeLayers, nIn.specialType == PrimitiveNode.SERPTRANS, false);
//            for(int k=0; k<nIn.nodeLayers.length; k++) {
//                NodeInfo.LayerDetails nld = nIn.nodeLayers[k];
//                if (nld.message != null)
//                    buffWriter.println("\t\t" + ab + "_node.getLayers()[" + k + "].setMessage(\"" + nld.message + "\");");
//                TextDescriptor td = nld.descriptor;
//                if (td != null) {
//                    buffWriter.println("\t\t" + ab + "_node.getLayers()[" + k + "].setDescriptor(TextDescriptor.newTextDescriptor(");
//                    buffWriter.println("\t\t\tnew MutableTextDescriptor(" + td.lowLevelGet() + ", " + td.getColorIndex() + ", " + td.isDisplay() +
//                            ", TextDescriptor.Code." + td.getCode().name() + ")));");
//                }
//            }
//
//			// print ports
//			buffWriter.println("\t\t" + ab + "_node.addPrimitivePorts(new PrimitivePort[]");
//			buffWriter.println("\t\t\t{");
//			int numPorts = nIn.nodePortDetails.length;
//			for(int j=0; j<numPorts; j++)
//			{
//				NodeInfo.PortDetails portDetail = nIn.nodePortDetails[j];
//				buffWriter.print("\t\t\t\tPrimitivePort.newInstance(this, " + ab + "_node, new ArcProto [] {");
//				ArcInfo [] conns = portDetail.connections;
//				for(int l=0; l<conns.length; l++)
//				{
//					buffWriter.print(conns[l].javaName + "_arc");
//					if (l+1 < conns.length) buffWriter.print(", ");
//				}
//                PortCharacteristic characteristic = portDetail.characterisitic;
//                if (characteristic == null)
//                    characteristic = PortCharacteristic.UNKNOWN;
//				buffWriter.println("}, \"" + portDetail.name + "\", " + portDetail.angle + "," +
//					portDetail.range + ", " + portDetail.netIndex + ", PortCharacteristic." + characteristic.name() + ",");
//				buffWriter.print("\t\t\t\t\t" + getEdgeLabel(portDetail.values[0], false) + ", " +
//					getEdgeLabel(portDetail.values[0], true) + ", " +
//					getEdgeLabel(portDetail.values[1], false) + ", " +
//					getEdgeLabel(portDetail.values[1], true) + ")");
//
//				if (j+1 < numPorts) buffWriter.print(",");
//				buffWriter.println();
//			}
//			buffWriter.println("\t\t\t});");
//            boolean needElectricalLayers = false;
//            for (NodeInfo.LayerDetails nld: nIn.nodeLayers) {
//                if (!(nld.inLayers && nld.inElectricalLayers))
//                    needElectricalLayers = true;
//            }
//            if (needElectricalLayers) {
//                buffWriter.println("\t\t" + ab + "_node.setElectricalLayers(");
//                dumpNodeLayersToJava(buffWriter, nIn.nodeLayers, nIn.specialType == PrimitiveNode.SERPTRANS, true);
//            }
//            for (int j = 0; j < numPorts; j++) {
//				NodeInfo.PortDetails portDetail = nIn.nodePortDetails[j];
//                if (portDetail.isolated)
//                    buffWriter.println("\t\t" + ab + "_node.getPortId(" + j + ").setIsolated();");
//                if (portDetail.negatable)
//                    buffWriter.println("\t\t" + ab + "_node.getPortId(" + j + ").setNegatable(true);");
//            }
//
//			// print the node information
//			PrimitiveNode.Function fun = nIn.func;
//			buffWriter.println("\t\t" + ab + "_node.setFunction(PrimitiveNode.Function." + fun.name() + ");");
//
//			if (nIn.wipes) buffWriter.println("\t\t" + ab + "_node.setWipeOn1or2();");
//			if (nIn.square) buffWriter.println("\t\t" + ab + "_node.setSquare();");
//            if (nIn.canBeZeroSize) buffWriter.println("\t\t" + ab + "_node.setCanBeZeroSize();");
//			if (nIn.lockable) buffWriter.println("\t\t" + ab + "_node.setLockedPrim();");
//            if (nIn.edgeSelect) buffWriter.println("\t\t" + ab + "_node.setEdgeSelect();");
//            if (nIn.skipSizeInPalette) buffWriter.println("\t\t" + ab + "_node.setSkipSizeInPalette();");
//            if (nIn.lowVt) buffWriter.println("\t\t" + ab + "_node.setNodeBit(PrimitiveNode.LOWVTBIT);");
//            if (nIn.highVt) buffWriter.println("\t\t" + ab + "_node.setNodeBit(PrimitiveNode.HIGHVTBIT);");
//            if (nIn.nativeBit) buffWriter.println("\t\t" + ab + "_node.setNodeBit(PrimitiveNode.NATIVEBIT);");
//            if (nIn.od18) buffWriter.println("\t\t" + ab + "_node.setNodeBit(PrimitiveNode.OD18BIT);");
//            if (nIn.od25) buffWriter.println("\t\t" + ab + "_node.setNodeBit(PrimitiveNode.OD25BIT);");
//            if (nIn.od33) buffWriter.println("\t\t" + ab + "_node.setNodeBit(PrimitiveNode.OD33BIT);");
//            if (nIn.notUsed) buffWriter.println("\t\t" + ab + "_node.setNotUsed(true);");
//			if (nIn.arcsShrink)
//			buffWriter.println("\t\t" + ab + "_node.setArcsWipe();");
//			buffWriter.println("\t\t" + ab + "_node.setArcsShrink();");
//            if (nIn.nodeSizeRule != null) {
//				buffWriter.println("\t\t" + ab + "_node.setMinSize(" + nIn.nodeSizeRule.getWidth() + ", " + nIn.nodeSizeRule.getHeight() +
//                        ", \"" + nIn.nodeSizeRule.getRuleName() + "\");");
//            }
//            if (nIn.autoGrowth != null) {
//				buffWriter.println("\t\t" + ab + "_node.setAutoGrowth(" + nIn.autoGrowth.getWidth() + ", " + nIn.autoGrowth.getHeight() + ");");
//            }
//			if (nIn.specialType != 0)
//			{
//				switch (nIn.specialType)
//				{
//					case PrimitiveNode.SERPTRANS:
//						buffWriter.println("\t\t" + ab + "_node.setHoldsOutline();");
//						buffWriter.println("\t\t" + ab + "_node.setCanShrink();");
//						buffWriter.println("\t\t" + ab + "_node.setSpecialType(PrimitiveNode.SERPTRANS);");
//						buffWriter.println("\t\t" + ab + "_node.setSpecialValues(new double [] {" +
//							nIn.specialValues[0] + ", " + nIn.specialValues[1] + ", " +
//							nIn.specialValues[2] + ", " + nIn.specialValues[3] + ", " +
//							nIn.specialValues[4] + ", " + nIn.specialValues[5] + "});");
//						break;
//					case PrimitiveNode.POLYGONAL:
//						buffWriter.println("\t\t" + ab + "_node.setHoldsOutline();");
//						buffWriter.println("\t\t" + ab + "_node.setSpecialType(PrimitiveNode.POLYGONAL);");
//						break;
//				}
//			}
//		}
//
//		// write the pure-layer associations
//		buffWriter.println();
//		buffWriter.println("\t\t// The pure layer nodes");
//		for(int i=0; i<lList.length; i++)
//		{
//			if (lList[i].pseudo) continue;
//
//			// find the pure layer node
//			for(int j=0; j<nList.length; j++)
//			{
//				if (nList[j].func != PrimitiveNode.Function.NODE) continue;
//				NodeInfo.LayerDetails nld = nList[j].nodeLayers[0];
//				if (nld.layer == lList[i])
//				{
//					buffWriter.println("\t\t" + lList[i].javaName + "_lay.setPureLayerNode(" +
//						nList[j].abbrev + "_node);\t\t// " + lList[i].name);
//					break;
//				}
//			}
//		}
//        buffWriter.println();
//	}
//    
//    private static void dumpNodeLayersToJava(PrintStream buffWriter, NodeInfo.LayerDetails[] mergedLayerDetails, boolean isSerpentine, boolean electrical) {
//        // print layers
//        buffWriter.println("\t\t\tnew Technology.NodeLayer []");
//        buffWriter.println("\t\t\t{");
//        ArrayList<NodeInfo.LayerDetails> layerDetails = new ArrayList<NodeInfo.LayerDetails>();
//        for (NodeInfo.LayerDetails nld: mergedLayerDetails) {
//            if (electrical ? nld.inElectricalLayers : nld.inLayers)
//                layerDetails.add(nld);
//        }
//        int tot = layerDetails.size();
//        for(int j=0; j<tot; j++) {
//            NodeInfo.LayerDetails nld = layerDetails.get(j);
//            int portNum = nld.portIndex;
//            switch (nld.representation) {
//                case Technology.NodeLayer.BOX:
//                    buffWriter.print("\t\t\t\tnew Technology.NodeLayer(" +
//                            nld.layer.javaName + "_lay, " + portNum + ", Poly.Type." +
//                            nld.style.name() + ", Technology.NodeLayer.BOX,");
//                    break;
//                case Technology.NodeLayer.MINBOX:
//                    buffWriter.print("\t\t\t\tnew Technology.NodeLayer(" +
//                            nld.layer.javaName + "_lay, " + portNum + ", Poly.Type." +
//                            nld.style.name() + ", Technology.NodeLayer.MINBOX,");
//                    break;
//                case Technology.NodeLayer.POINTS:
//                    buffWriter.print("\t\t\t\tnew Technology.NodeLayer(" +
//                            nld.layer.javaName + "_lay, " + portNum + ", Poly.Type." +
//                            nld.style.name() + ", Technology.NodeLayer.POINTS,");
//                    break;
//                case Technology.NodeLayer.MULTICUTBOX:
//                    buffWriter.print("\t\t\t\tTechnology.NodeLayer.makeMulticut(" +
//                            nld.layer.javaName + "_lay, " + portNum + ", Poly.Type." +
//                            nld.style.name() + ",");
//                    break;
//                default:
//                    buffWriter.print(" Technology.NodeLayer.????,");
//                    break;
//            }
//            buffWriter.println(" new Technology.TechPoint [] {");
//            int totLayers = nld.values.length;
//            for(int k=0; k<totLayers; k++) {
//                Technology.TechPoint tp = nld.values[k];
//                buffWriter.print("\t\t\t\t\tnew Technology.TechPoint(" +
//                        getEdgeLabel(tp, false) + ", " + getEdgeLabel(tp, true) + ")");
//                if (k < totLayers-1) buffWriter.println(","); else
//                    buffWriter.print("}");
//            }
//            if (isSerpentine) {
//                buffWriter.print(", " + nld.lWidth + ", " + nld.rWidth + ", " +
//                        nld.extendB + ", " + nld.extendT);
//            } else if (nld.representation == Technology.NodeLayer.MULTICUTBOX) {
//                buffWriter.print(", " + nld.multiXS + ", " + nld.multiYS + ", " + nld.multiSep + ", " + nld.multiSep2D);
//            }
//            buffWriter.print(")");
//            if (j+1 < tot) buffWriter.print(",");
//            buffWriter.println();
//        }
//        buffWriter.println("\t\t\t});");
//    }
//    
//    private static void dumpPaletteToJava(PrintStream buffWriter, Object[][] menuPalette) {
//        int numRows = menuPalette.length;
//        int numCols = menuPalette[0].length;
//        buffWriter.println("\t\t// Building information for palette");
//        buffWriter.println("\t\tnodeGroups = new Object[" + numRows + "][" + numCols + "];");
//        buffWriter.println("\t\tint count = -1;");
//        buffWriter.println();
//        for (int row = 0; row < numRows; row++) {
//            buffWriter.print("\t\t");
//            for (int col = 0; col < numCols; col++) {
//                if (col != 0) buffWriter.print(" ");
//                buffWriter.print("nodeGroups[");
//                if (col == 0) buffWriter.print("++");
//                buffWriter.print("count][" + col + "] = ");
//                Object menuObject = menuPalette[row][col];
//                if (menuObject instanceof ArcInfo) {
//                    buffWriter.print(((ArcInfo)menuObject).javaName + "_arc");
//                } else if (menuObject instanceof NodeInfo) {
//                    buffWriter.print(((NodeInfo)menuObject).abbrev + "_node");
//                } else if (menuObject instanceof String) {
//                    buffWriter.print("\"" + menuObject + "\"");
//                } else if (menuObject == null) {
//                    buffWriter.print("null");
//                }
//                buffWriter.print(";");
//            }
//            buffWriter.println();
//        }
//        buffWriter.println();
//    }
//    
//    private static void dumpFoundryToJava(PrintStream buffWriter, GeneralInfo gi, LayerInfo[] lList) {
//        List<String> gdsStrings = new ArrayList<String>();
//        for (LayerInfo li: lList) {
//            if (li.gds == null || li.gds.length() == 0) continue;
//            gdsStrings.add(li.name + " " + li.gds);
//        }
//        buffWriter.println("\t\t//Foundry");
//        buffWriter.print("\t\tnewFoundry(Foundry.Type." + gi.defaultFoundry + ", null");
//        for (String s: gdsStrings) {
//            buffWriter.println(",");
//            buffWriter.print("\t\t\t\"" + s + "\"");
//        }
//        buffWriter.println(");");
//        buffWriter.println();
//    }
//
//	/**
//	 * Method to remove illegal Java charcters from "string".
//	 */
//	private static String makeJavaName(String string)
//	{
//		StringBuffer infstr = new StringBuffer();
//		for(int i=0; i<string.length(); i++)
//		{
//			char chr = string.charAt(i);
//			if (i == 0)
//			{
//				if (!Character.isJavaIdentifierStart(chr)) chr = '_'; else
//					chr = Character.toLowerCase(chr);
//			} else
//			{
//				if (!Character.isJavaIdentifierPart(chr)) chr = '_';
//			}
//			infstr.append(chr);
//		}
//		return infstr.toString();
//	}
//
//	/**
//	 * Method to convert the multiplication and addition factors in "mul" and
//	 * "add" into proper constant names.  The "yAxis" is false for X and 1 for Y
//	 */
//	private static String getEdgeLabel(Technology.TechPoint pt, boolean yAxis)
//	{
//		double mul, add;
//		if (yAxis)
//		{
//			add = pt.getY().getAdder();
//			mul = pt.getY().getMultiplier();
//		} else
//		{
//			add = pt.getX().getAdder();
//			mul = pt.getX().getMultiplier();
//		}
//		StringBuffer infstr = new StringBuffer();
//
//		// handle constant distance from center
//		if (mul == 0)
//		{
//			if (yAxis) infstr.append("EdgeV."); else
//				infstr.append("EdgeH.");
//			if (add == 0)
//			{
//				infstr.append("makeCenter()");
//			} else
//			{
//				infstr.append("fromCenter(" + TextUtils.formatDouble(add, NUM_FRACTIONS) + ")");
//			}
//			return infstr.toString();
//		}
//
//		// handle constant distance from edge
//		if (mul == 0.5 || mul == -0.5)
//		{
//			if (yAxis) infstr.append("EdgeV."); else
//				infstr.append("EdgeH.");
//			double amt = Math.abs(add);
//			if (!yAxis)
//			{
//				if (mul < 0)
//				{
//					if (add == 0) infstr.append("makeLeftEdge()"); else
//						infstr.append("fromLeft(" + TextUtils.formatDouble(amt, NUM_FRACTIONS) + ")");
//				} else
//				{
//					if (add == 0) infstr.append("makeRightEdge()"); else
//						infstr.append("fromRight(" + TextUtils.formatDouble(amt, NUM_FRACTIONS) + ")");
//				}
//			} else
//			{
//				if (mul < 0)
//				{
//					if (add == 0) infstr.append("makeBottomEdge()"); else
//						infstr.append("fromBottom(" + TextUtils.formatDouble(amt, NUM_FRACTIONS) + ")");
//				} else
//				{
//					if (add == 0) infstr.append("makeTopEdge()"); else
//						infstr.append("fromTop(" + TextUtils.formatDouble(amt, NUM_FRACTIONS) + ")");
//				}
//			}
//			return infstr.toString();
//		}
//
//		// generate two-value description
//		if (!yAxis)
//			infstr.append("new EdgeH(" + TextUtils.formatDouble(mul, NUM_FRACTIONS) + ", " + TextUtils.formatDouble(add, NUM_FRACTIONS) + ")"); else
//			infstr.append("new EdgeV(" + TextUtils.formatDouble(mul, NUM_FRACTIONS) + ", " + TextUtils.formatDouble(add, NUM_FRACTIONS) + ")");
//		return infstr.toString();
//	}

	private static String getSampleName(NodeProto layerCell)
	{
		if (layerCell == Generic.tech.portNode) return "PORT";
		if (layerCell == Generic.tech.cellCenterNode) return "GRAB";
		if (layerCell == null) return "HIGHLIGHT";
		return layerCell.getName().substring(6);
	}
    
    /**
     * Dump technology information to Xml
     * @param fileName name of file to write
     * @param newTechName new technology name
     * @param gi general technology information
     * @param lList information about layers
     * @param nList information about primitive nodes
     * @param aList information about primitive arcs.
     */
    static void writeXml(String fileName, String newTechName, GeneralInfo gi, LayerInfo[] lList, NodeInfo[] nList, ArcInfo[] aList) {
        makeXml(newTechName, gi, lList, nList, aList).writeXml(fileName);
    }
    
    static Xml.Technology makeXml(String newTechName, GeneralInfo gi, LayerInfo[] lList, NodeInfo[] nList, ArcInfo[] aList) {
        Xml.Technology t = new Xml.Technology();
        t.techName = newTechName;
        t.shortTechName = gi.shortName;
        t.description = gi.description;
        Xml.Version version = new Xml.Version();
        version.techVersion = 1;
        version.electricVersion = Version.parseVersion("8.05g");
        t.versions.add(version);
        version = new Xml.Version();
        version.techVersion = 2;
        version.electricVersion = Version.parseVersion("8.05o");
        t.versions.add(version);
        t.minNumMetals = t.maxNumMetals = t.defaultNumMetals = gi.defaultNumMetals;
        t.scaleValue = gi.scale;
        t.scaleRelevant = gi.scaleRelevant;
        t.defaultFoundry = gi.defaultFoundry;
        t.minResistance = gi.minRes;
        t.minCapacitance = gi.minCap;
        
        if (gi.transparentColors != null) {
            for (int i = 0; i < gi.transparentColors.length; i++)
                t.transparentLayers.add(gi.transparentColors[i]);
        }
        
        for (LayerInfo li: lList) {
            if (li.pseudo) continue;
            Xml.Layer layer = new Xml.Layer();
            layer.name = li.name;
            layer.function = li.fun;
            layer.extraFunction = li.funExtra;
            layer.desc = li.desc;
            layer.thick3D = li.thick3d;
            layer.height3D = li.height3d;
            layer.mode3D = li.mode3d;
            layer.factor3D = li.factor3d;
            layer.cif = li.cif;
            layer.resistance = li.spiRes;
            layer.capacitance = li.spiCap;
            layer.edgeCapacitance = li.spiECap;
//            if (li.myPseudo != null)
//                layer.pseudoLayer = li.myPseudo.name;
            if (li.pureLayerNode != null) {
                layer.pureLayerNode = new Xml.PureLayerNode();
                layer.pureLayerNode.name = li.pureLayerNode.name;
                layer.pureLayerNode.style = li.pureLayerNode.nodeLayers[0].style;
                layer.pureLayerNode.port = li.pureLayerNode.nodePortDetails[0].name;
                layer.pureLayerNode.size.value = DBMath.round(li.pureLayerNode.xSize);
                for (ArcInfo a: li.pureLayerNode.nodePortDetails[0].connections)
                    layer.pureLayerNode.portArcs.add(a.name);
            }
            t.layers.add(layer);
        }
        
        for (ArcInfo ai: aList) {
            Xml.ArcProto ap = new Xml.ArcProto();
            ap.name = ai.name;
            ap.function = ai.func;
            ap.wipable = ai.wipes;
            ap.curvable = ai.curvable;
            ap.special = ai.special;
            ap.notUsed = ai.notUsed;
            ap.skipSizeInPalette = ai.skipSizeInPalette;
            ap.extended = !ai.noExtend;
            ap.fixedAngle = ai.fixAng;
            ap.angleIncrement = ai.angInc;
            ap.antennaRatio = ai.antennaRatio;
            if (ai.widthOffset != 0) {
                ap.diskOffset.put(Integer.valueOf(1), DBMath.round(0.5*ai.maxWidth));
                ap.diskOffset.put(Integer.valueOf(2), DBMath.round(0.5*(ai.maxWidth - ai.widthOffset)));
            } else {
                ap.diskOffset.put(Integer.valueOf(2), DBMath.round(0.5*ai.maxWidth));
            }
            ap.defaultWidth.value = 0;
            for (ArcInfo.LayerDetails al: ai.arcDetails) {
                Xml.ArcLayer l = new Xml.ArcLayer();
                l.layer = al.layer.name;
                l.style = al.style == Poly.Type.FILLED ? Poly.Type.FILLED : Poly.Type.CLOSED;
                l.extend.value = DBMath.round((ai.maxWidth - al.width)/2);
                ap.arcLayers.add(l);
            }
            t.arcs.add(ap);
        }
        
        for (NodeInfo ni: nList) {
            if (ni.func == PrimitiveNode.Function.NODE && ni.nodeLayers[0].layer.pureLayerNode == ni)
                continue;
            Xml.PrimitiveNode pn = new Xml.PrimitiveNode();
            pn.name = ni.name;
            pn.function = ni.func;
            pn.shrinkArcs = ni.arcsShrink;
            pn.square = ni.square;
            pn.canBeZeroSize = ni.canBeZeroSize;
            pn.wipes = ni.wipes;
            pn.lockable = ni.lockable;
            pn.edgeSelect = ni.edgeSelect;
            pn.skipSizeInPalette = ni.skipSizeInPalette;
            pn.notUsed = ni.notUsed;
            pn.lowVt = ni.lowVt;
            pn.highVt = ni.highVt;
            pn.nativeBit = ni.nativeBit;
            pn.od18 = ni.od18;
            pn.od25 = ni.od25;
            pn.od33 = ni.od33;
            EPoint minFullSize = EPoint.fromLambda(0.5*ni.xSize, 0.5*ni.ySize);
            SizeOffset so = ni.so;
            if (so != null && so.getLowXOffset() == 0 && so.getHighXOffset() == 0 && so.getLowYOffset() == 0 && so.getHighYOffset() == 0)
                so = null;
            if (so != null) {
                EPoint p2 = EPoint.fromGrid(
                        minFullSize.getGridX() - ((so.getLowXGridOffset() + so.getHighXGridOffset()) >> 1),
                        minFullSize.getGridY() - ((so.getLowYGridOffset() + so.getHighYGridOffset()) >> 1));
                pn.diskOffset.put(Integer.valueOf(1), minFullSize);
                pn.diskOffset.put(Integer.valueOf(2), p2);
            } else {
                pn.diskOffset.put(Integer.valueOf(2), minFullSize);
            }
//            pn.defaultWidth.value = DBMath.round(ni.xSize);
//            pn.defaultHeight.value = DBMath.round(ni.ySize);
            pn.sizeOffset = so;
            for(int j=0; j<ni.nodeLayers.length; j++) {
                NodeInfo.LayerDetails nl = ni.nodeLayers[j];
                pn.nodeLayers.add(makeNodeLayerDetails(nl, false, minFullSize, true, true));
            }
            for (int j = 0; j < ni.nodePortDetails.length; j++) {
                NodeInfo.PortDetails pd = ni.nodePortDetails[j];
                Xml.PrimitivePort pp = new Xml.PrimitivePort();
                pp.name = pd.name;
                pp.portAngle = pd.angle;
                pp.portRange = pd.range;
                pp.portTopology = pd.netIndex;
                
                EdgeH left = pd.values[0].getX();
                EdgeH right = pd.values[1].getX();
                EdgeV bottom = pd.values[0].getY();
                EdgeV top = pd.values[1].getY();
                
                pp.lx.k = left.getMultiplier()*2;
                pp.lx.value = left.getAdder() + minFullSize.getLambdaX()*left.getMultiplier()*2;
                pp.hx.k = right.getMultiplier()*2;
                pp.hx.value = right.getAdder() + minFullSize.getLambdaX()*right.getMultiplier()*2;
                pp.ly.k = bottom.getMultiplier()*2;
                pp.ly.value = bottom.getAdder() + minFullSize.getLambdaY()*bottom.getMultiplier()*2;
                pp.hy.k = top.getMultiplier()*2;
                pp.hy.value = top.getAdder() + minFullSize.getLambdaY()*top.getMultiplier()*2;
                
//                pp.p0 = pd.values[0];
//                pp.p1 = pd.values[1];
                for (ArcInfo a: pd.connections)
                    pp.portArcs.add(a.name);
                pn.ports.add(pp);
            }
            pn.specialType = ni.specialType;
            if (pn.specialType == PrimitiveNode.SERPTRANS) {
                pn.specialValues = new double[6];
                for (int i = 0; i < 6; i++)
                    pn.specialValues[i] = ni.specialValues[i];
            }
            pn.nodeSizeRule = ni.nodeSizeRule;
            t.nodes.add(pn);
            
        }
        
        addSpiceHeader(t, 1, gi.spiceLevel1Header);
        addSpiceHeader(t, 2, gi.spiceLevel2Header);
        addSpiceHeader(t, 3, gi.spiceLevel3Header);
        
        if (gi.menuPalette != null) {
            t.menuPalette = new Xml.MenuPalette();
            int numColumns = gi.menuPalette[0].length;
            t.menuPalette.numColumns = numColumns;
            for (Object[] menuLine: gi.menuPalette) {
                for (int i = 0; i < numColumns; i++)
                    t.menuPalette.menuBoxes.add(makeMenuBoxXml(t, menuLine[i]));
            }
        }
        
        Xml.Foundry foundry = new Xml.Foundry();
        foundry.name = gi.defaultFoundry;
        if (gi.conDist != null && gi.unConDist != null) {
            int layerTotal = lList.length;
            int ruleIndex = 0;
            for (int i1 = 0; i1 < layerTotal; i1++) {
                LayerInfo l1 = lList[i1];
                for (int i2 = i1; i2 < layerTotal; i2++) {
                    LayerInfo l2 = lList[i2];
                    double conSpa = gi.conDist[ruleIndex];
                    double uConSpa = gi.unConDist[ruleIndex];
                    if (conSpa > -1)
                        foundry.rules.add(makeDesignRule("C" + ruleIndex, l1, l2, DRCTemplate.DRCRuleType.CONSPA, conSpa));
                    if (uConSpa > -1)
                        foundry.rules.add(makeDesignRule("U" + ruleIndex, l1, l2, DRCTemplate.DRCRuleType.UCONSPA, uConSpa));
                    ruleIndex++;
                }
            }
        }
        t.foundries.add(foundry);
        
        return t;
    }
    
    private static Xml.NodeLayer makeNodeLayerDetails(NodeInfo.LayerDetails nl, boolean isSerp, EPoint correction, boolean inLayers, boolean inElectricalLayers) {
        Xml.NodeLayer nld = new Xml.NodeLayer();
        nld.layer = nl.layer.name;
        nld.style = nl.style;
        nld.portNum = nl.portIndex;
        nld.inLayers = inLayers;
        nld.inElectricalLayers = inElectricalLayers;
        nld.representation = nl.representation;
        Technology.TechPoint[] points = nl.values;
        if (nld.representation == Technology.NodeLayer.BOX || nld.representation == Technology.NodeLayer.MULTICUTBOX) {
            nld.lx.k = points[0].getX().getMultiplier()*2;
            nld.lx.value = DBMath.round(points[0].getX().getAdder() + correction.getLambdaX()*points[0].getX().getMultiplier()*2);
            nld.hx.k = points[1].getX().getMultiplier()*2;
            nld.hx.value = DBMath.round(points[1].getX().getAdder() + correction.getLambdaX()*points[1].getX().getMultiplier()*2);
            nld.ly.k = points[0].getY().getMultiplier()*2;
            nld.ly.value = DBMath.round(points[0].getY().getAdder() + correction.getLambdaY()*points[0].getY().getMultiplier()*2);
            nld.hy.k = points[1].getY().getMultiplier()*2;
            nld.hy.value = DBMath.round(points[1].getY().getAdder() + correction.getLambdaY()*points[1].getY().getMultiplier()*2);
        } else {
            for (Technology.TechPoint p: points)
                nld.techPoints.add(correction(p, correction));
        }
        nld.sizex = DBMath.round(nl.multiXS);
        nld.sizey = DBMath.round(nl.multiYS);
        nld.sep1d = DBMath.round(nl.multiSep);
        nld.sep2d = DBMath.round(nl.multiSep2D);
        if (isSerp) {
            nld.lWidth = DBMath.round(nl.lWidth);
            nld.rWidth = DBMath.round(nl.rWidth);
            nld.tExtent = DBMath.round(nl.extendT);
            nld.bExtent = DBMath.round(nl.extendB);
        }
        return nld;
    }
    
    private static Technology.TechPoint correction(Technology.TechPoint p, EPoint correction) {
        EdgeH h = p.getX();
        EdgeV v = p.getY();
        h = new EdgeH(h.getMultiplier(), h.getAdder() + correction.getLambdaX()*h.getMultiplier()*2);
        v = new EdgeV(v.getMultiplier(), v.getAdder() + correction.getLambdaY()*v.getMultiplier()*2);
        return new Technology.TechPoint(h, v);
    }
    
    private static void addSpiceHeader(Xml.Technology t, int level, String[] spiceLines) {
        if (spiceLines == null) return;
        Xml.SpiceHeader spiceHeader = new Xml.SpiceHeader();
        spiceHeader.level = level;
        for (String spiceLine: spiceLines)
            spiceHeader.spiceLines.add(spiceLine);
        t.spiceHeaders.add(spiceHeader);
    }
    
    private static ArrayList<Object> makeMenuBoxXml(Xml.Technology t, Object o) {
        ArrayList<Object> menuBox = new ArrayList<Object>();
        if (o instanceof ArcInfo)
            menuBox.add(t.findArc(((ArcInfo)o).name));
        else if (o instanceof NodeInfo)
            menuBox.add(t.findNode(((NodeInfo)o).name));
        else if (o != null)
            menuBox.add(o.toString());
        return menuBox;
    }
    
    private static DRCTemplate makeDesignRule(String ruleName, LayerInfo l1, LayerInfo l2, DRCTemplate.DRCRuleType type, double value) {
        return new DRCTemplate(ruleName, DRCTemplate.DRCMode.ALL.mode(), type, l1.name, l2.name, new double[] {value}, null);
    }
}
