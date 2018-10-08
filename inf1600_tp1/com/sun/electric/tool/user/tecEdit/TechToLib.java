/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: TechToLib.java
 * Technology Editor, conversion of technologies to libraries
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
import com.sun.electric.database.geometry.EGraphics;
import com.sun.electric.database.geometry.EPoint;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.technology.ArcProto;
import com.sun.electric.technology.DRCRules;
import com.sun.electric.technology.DRCTemplate;
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.PrimitivePort;
import com.sun.electric.technology.SizeOffset;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.technologies.Artwork;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.erc.ERC;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.ui.TopLevel;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.JOptionPane;

/**
 * This class generates technology libraries from technologys.
 */
public class TechToLib
{
	/**
	 * Method to convert the current technology into a library.
	 */
	public static void makeLibFromTech()
	{
		List<Technology> techs = new ArrayList<Technology>();
		for(Iterator<Technology> it = Technology.getTechnologies(); it.hasNext(); )
		{
			Technology tech = it.next();
//			if (tech.isNonStandard()) continue;
			techs.add(tech);
		}
		String [] techChoices = new String[techs.size()];
		for(int i=0; i<techs.size(); i++)
			techChoices[i] = techs.get(i).getTechName();
		String chosen = (String)JOptionPane.showInputDialog(TopLevel.getCurrentJFrame(), "Technology to Edit",
			"Choose a technology to edit", JOptionPane.QUESTION_MESSAGE, null, techChoices, Technology.getCurrent().getTechName());
		if (chosen == null) return;
		Technology tech = Technology.findTechnology(chosen);
		Library already = Library.findLibrary(tech.getTechName());
		if (already != null)
		{
			JOptionPane.showMessageDialog(TopLevel.getCurrentJFrame(),
				"There is already a library called '" + tech.getTechName() + "'.  Delete it first.",
				"Cannot Convert Technology", JOptionPane.ERROR_MESSAGE);
			System.out.println();
			return;
		}
		new LibFromTechJob(tech, false);
	}

	/**
	 * Class to create a technology-library from a technology (in a Job).
	 */
	private static class LibFromTechJob extends Job
	{
		private Technology tech;
        private String libraryName;
        private boolean doItNow;

		private LibFromTechJob(Technology tech, boolean doItNow)
		{
			super("Make Technology Library from Technology", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.tech = tech;
            this.doItNow = doItNow;
            if (!doItNow)
                startJob();
		}

        public String getLibraryName() { return libraryName; }

		public boolean doIt()
		{
			Library lib = makeLibFromTech(tech);
			if (lib == null) return false;

			// switch to the library and show a cell
			lib.setCurrent();
            if (!doItNow)
            fieldVariableChanged("libraryName");
            libraryName = lib.getName();
			return true;
		}
	}

	/**
	 * Method to convert technology "tech" into a library and return that library.
	 * Returns NOLIBRARY on error
	 */
	public static Library makeLibFromTech(Technology tech)
	{
		Library lib = Library.newInstance(tech.getTechName(), null);
		if (lib == null)
		{
			System.out.println("Cannot create library " + tech.getTechName());
			return null;
		}
		System.out.println("Created library " + tech.getTechName() + "...");

		// create the miscellaneous info cell (called "factors")
		Cell fNp = Cell.newInstance(lib, "factors");
		if (fNp == null) return null;
		fNp.setInTechnologyLibrary();

		// compute the number of layers (ignoring pseudo-layers)
		int layerTotal = 0;
		for(Iterator<Layer> it = tech.getLayers(); it.hasNext(); )
		{
			Layer layer = it.next();
			if (!layer.isPseudoLayer()) layerTotal++;
		}

		// build the general information cell
		GeneralInfo gi = new GeneralInfo();
        gi.shortName = tech.getTechShortName();
        if (gi.shortName == null)
            gi.shortName = tech.getTechName();
        gi.nonElectrical = tech.isNonElectrical();
		gi.scale = tech.getScale();
        gi.scaleRelevant = tech.isScaleRelevant();
        gi.resolution = tech.getResolution();
        gi.defaultFoundry = tech.getPrefFoundry();
        gi.defaultNumMetals = tech.getNumMetals();
		gi.description = tech.getTechDesc();
		gi.minRes = tech.getMinResistanceSetting().getDoubleFactoryValue();
		gi.minCap = tech.getMinCapacitanceSetting().getDoubleFactoryValue();
        gi.maxSeriesResistance = tech.getMaxSeriesResistance();
		gi.gateShrinkage = tech.getGateLengthSubtraction();
		gi.includeGateInResistance = tech.isGateIncluded();
		gi.includeGround = tech.isGroundNetIncluded();
        gi.gateCapacitance = tech.getGateCapacitanceSetting().getDoubleFactoryValue();
        gi.wireRatio = tech.getWireRatioSetting().getDoubleFactoryValue();
        gi.diffAlpha = tech.getDiffAlphaSetting().getDoubleFactoryValue();
		Color [] wholeMap = tech.getColorMap();
		int numLayers = tech.getNumTransparentLayers();
		gi.transparentColors = new Color[numLayers];
		for(int i=0; i<numLayers; i++)
			gi.transparentColors[i] = wholeMap[1<<i];
        gi.spiceLevel1Header = tech.getSpiceHeaderLevel1();
        gi.spiceLevel2Header = tech.getSpiceHeaderLevel2();
        gi.spiceLevel3Header = tech.getSpiceHeaderLevel3();
        DRCRules drcRules = tech.getFactoryDesignRules();
        if (drcRules != null) {
            int rulesSize = layerTotal*(layerTotal + 1)/2;
            gi.conDist = new double[rulesSize];
            gi.unConDist = new double[rulesSize];
            Arrays.fill(gi.conDist, -1);
            Arrays.fill(gi.unConDist, -1);
            int ruleIndex = 0;
            for (int i1 = 0; i1 < layerTotal; i1++) {
                for (int i2 = i1; i2 < layerTotal; i2++) {
                    for (DRCTemplate t: drcRules.getSpacingRules(drcRules.getRuleIndex(i1, i2), DRCTemplate.DRCRuleType.SPACING, false)) {
                        if (t.ruleType == DRCTemplate.DRCRuleType.CONSPA)
                            gi.conDist[ruleIndex] = t.getValue(0);
                        else if (t.ruleType == DRCTemplate.DRCRuleType.UCONSPA)
                            gi.unConDist[ruleIndex] = t.getValue(0);
                    }
                    ruleIndex++;
                }
            }
        }
		gi.generate(fNp);

		// create the layer node names
		HashMap<Layer,Cell> layerCells = new HashMap<Layer,Cell>();

		// create the layer nodes
		System.out.println("Creating the layers...");
		ArrayList<String> layerSequence = new ArrayList<String>();
        LayerInfo [] lList = new LayerInfo[layerTotal];
        Map<Layer,String> gdsLayers = tech.getGDSLayers();

        int layIndex = 0;
		for(Iterator<Layer> it = tech.getLayers(); it.hasNext(); )
		{
			Layer layer = it.next();
			if (layer.isPseudoLayer()) continue;
			EGraphics desc = layer.getGraphics();
			String fName = "layer-" + layer.getName() + "{lay}";

			// make sure the layer doesn't exist
			if (lib.findNodeProto(fName) != null)
			{
				System.out.println("Warning: multiple layers named '" + fName + "'");
				break;
			}

			Cell lNp = Cell.newInstance(lib, fName);
			if (lNp == null) return null;
			lNp.setTechnology(Artwork.tech);
			lNp.setInTechnologyLibrary();
			layerCells.put(layer, lNp);

			LayerInfo li = new LayerInfo();
            lList[layIndex++] = li;
            li.name = layer.getName();
			li.fun = layer.getFunction();
			li.funExtra = layer.getFunctionExtras();
            li.pseudo = layer.isPseudoLayer();
			li.desc = desc;
            if (li.pseudo) {
                String masterName = layer.getNonPseudoLayer().getName();
                for(int j=0; j<layIndex; j++) {
                    if (lList[j].name.equals(masterName)) { lList[j].myPseudo = li;   break; }
                }
                continue;
            }

			// compute foreign file formats
			li.cif = (String)layer.getCIFLayerSetting().getFactoryValue();
            li.dxf = (String)layer.getDXFLayerSetting().getFactoryValue();
            li.skill = (String)layer.getSkillLayerSetting().getFactoryValue();
            String gdsLayer = gdsLayers.get(layer);
            if (gdsLayer != null)
                li.gds = gdsLayer;

			// compute the SPICE information
			li.spiRes = layer.getResistanceSetting().getDoubleFactoryValue();
			li.spiCap = layer.getCapacitanceSetting().getDoubleFactoryValue();
			li.spiECap = layer.getEdgeCapacitanceSetting().getDoubleFactoryValue();

			// compute the 3D information
			li.height3d = layer.getDistance();
			li.thick3d = layer.getThickness();

			// build the layer cell
			li.generate(lNp);
			layerSequence.add(lNp.getName().substring(6));
		}

		// save the layer sequence
        String[] layerSequenceArray = layerSequence.toArray(new String[layerSequence.size()]);
		lib.newVar(Info.LAYERSEQUENCE_KEY, layerSequenceArray);

		// create the arc cells
		System.out.println("Creating the arcs...");
        int arcTotal = 0;
		for(Iterator<ArcProto> it = tech.getArcs(); it.hasNext(); )
			if (!it.next().isNotUsed()) arcTotal++;

		ArcInfo[] aList = new ArcInfo[arcTotal];
		String [] arcSequence = new String[arcTotal];
        int arcCount = 0;
		HashMap<ArcProto,Cell> arcCells = new HashMap<ArcProto,Cell>();
		for(Iterator<ArcProto> it = tech.getArcs(); it.hasNext(); )
		{
			ArcProto ap = it.next();
			if (ap.isNotUsed()) continue;
			ArcInfo aIn = makeArcInfo(ap, lList);
            aList[arcCount] = aIn;
			arcSequence[arcCount] = ap.getName();
			arcCount++;
            
			String fName = "arc-" + ap.getName() + "{lay}";

			// make sure the arc doesn't exist
			if (lib.findNodeProto(fName) != null)
			{
				System.out.println("Warning: multiple arcs named '" + fName + "'");
				break;
			}

			Cell aNp = Cell.makeInstance(lib, fName);
			if (aNp == null) return null;
			aNp.setTechnology(Artwork.tech);
			aNp.setInTechnologyLibrary();

			arcCells.put(ap, aNp);
			aIn.generate(aNp);

			// now create the arc layers
			double wid = ap.getDefaultLambdaBaseWidth();
			double widX4 = wid * 4;
			if (widX4 <= 0) widX4 = 10;
			Poly [] polys = ap.getShapeOfDummyArc(widX4);
			double xOff = wid*2 + wid/2 + ap.getLambdaWidthOffset()/2;
			for(int i=0; i<polys.length; i++)
			{
				Poly poly = polys[i];
				Layer arcLayer = poly.getLayer().getNonPseudoLayer();
				if (arcLayer == null) continue;
				EGraphics arcDesc = arcLayer.getGraphics();

				// scale the arc geometry appropriately
				Point2D [] points = poly.getPoints();
				for(int k=0; k<points.length; k++)
					poly.setPoint(k, points[k].getX() - xOff - 20, points[k].getY() - 5);

				// create the node to describe this layer
				NodeInst ni = placeGeometry(poly, aNp);
				if (ni == null) continue;

				// get graphics for this layer
				Manipulate.setPatch(ni, arcDesc);
				Cell layerCell = layerCells.get(arcLayer);
				if (layerCell != null) ni.newVar(Info.LAYER_KEY, layerCell.getId());
				ni.newVar(Info.OPTION_KEY, new Integer(Info.LAYERPATCH));
			}
			double i = ap.getLambdaWidthOffset() / 2;
			NodeInst ni = NodeInst.makeInstance(Artwork.tech.boxNode, new Point2D.Double(-20 - wid*2.5 - i, -5), wid*5, wid, aNp);
			if (ni == null) return null;
			ni.newVar(Artwork.ART_COLOR, new Integer(EGraphics.WHITE));
			ni.newVar(Info.OPTION_KEY, new Integer(Info.HIGHLIGHTOBJ));

			// compact it accordingly
			ArcInfo.compactCell(aNp);
		}

		// save the arc sequence
		lib.newVar(Info.ARCSEQUENCE_KEY, arcSequence);

		// create the node cells
		System.out.println("Creating the nodes...");
		int nodeTotal = 0;
		for(Iterator<PrimitiveNode> it = tech.getNodes(); it.hasNext(); )
			if (!it.next().isNotUsed()) nodeTotal++;
        NodeInfo[] nList = new NodeInfo[nodeTotal];
		String [] nodeSequence = new String[nodeTotal];

		int nodeIndex = 0;
		for(Iterator<PrimitiveNode> it = tech.getNodes(); it.hasNext(); )
		{
			PrimitiveNode pnp = it.next();
			if (pnp.isNotUsed()) continue;
            Technology.NodeLayer[] nodeLayers = pnp.getLayers();
            NodeInfo nIn = makeNodeInfo(pnp, lList, aList);
            nList[nodeIndex] = nIn;
            nodeSequence[nodeIndex] = pnp.getName();
			nodeIndex++;

			// create the node layers
			boolean first = true;
			NodeInst oNi = NodeInst.makeDummyInstance(pnp);
			double xS = pnp.getDefWidth() * 2;
			double yS = pnp.getDefHeight() * 2;
			if (xS < 3) xS = 3;
			if (yS < 3) yS = 3;
			double nodeXPos = -xS*2;
			Point2D [] pos = new Point2D[4];
			pos[0] = new Point2D.Double(nodeXPos - xS, -5 + yS);
			pos[1] = new Point2D.Double(nodeXPos + xS, -5 + yS);
			pos[2] = new Point2D.Double(nodeXPos - xS, -5 - yS);
			pos[3] = new Point2D.Double(nodeXPos + xS, -5 - yS);

			SizeOffset so = pnp.getProtoSizeOffset();
			xS = pnp.getDefWidth() - so.getLowXOffset() - so.getHighXOffset();
			yS = pnp.getDefHeight() - so.getLowYOffset() - so.getHighYOffset();
			double [] xsc = new double[4];
			double [] ysc = new double[4];
			xsc[0] = xS*1;   ysc[0] = yS*1;
			xsc[1] = xS*2;   ysc[1] = yS*1;
			xsc[2] = xS*1;   ysc[2] = yS*2;
			xsc[3] = xS*2;   ysc[3] = yS*2;

			// for multicut contacts, make large size be just right for 2 cuts
			if (pnp.isMulticut())
			{
                EPoint min2size = pnp.getMulticut2Size();
                double min2X = min2size.getLambdaX();
                double min2Y = min2size.getLambdaY();
                xsc[1] = min2X;
                xsc[3] = min2X;
                ysc[2] = min2Y;
                ysc[3] = min2Y;
//				double [] values = pnp.getSpecialValues();
//				double min2X = values[0]*2 + values[2]*2 + values[5];
//				double min2Y = values[1]*2 + values[3]*2 + values[5];
//				xsc[1] = min2X;
//				xsc[3] = min2X;
//				ysc[2] = min2Y;
//				ysc[3] = min2Y;
			}
			Cell nNp = null;
			Rectangle2D mainBounds = null;
			for(int e=0; e<4; e++)
			{
				// do not create node if main example had no polygons
				if (e != 0 && first) continue;

				// square nodes have only two examples
                if (pnp.isSquare() && (e == 1 || e == 2)) continue;
                double newXSize = xsc[e] + so.getLowXOffset() + so.getHighXOffset();
                double newYSize = ysc[e] + so.getLowYOffset() + so.getHighYOffset();
                EPoint newSize = EPoint.fromLambda(newXSize, newYSize);
                oNi.lowLevelModify(oNi.getD().withAnchor(EPoint.snap(pos[e])).withSize(newSize));
				Poly [] polys = tech.getShapeOfNode(oNi);
				int j = polys.length;
				for(int i=0; i<j; i++)
				{
					Poly poly = polys[i];
					Layer nodeLayer = poly.getLayer().getNonPseudoLayer();
					if (nodeLayer == null) continue;
					EGraphics desc = nodeLayer.getGraphics();

					// accumulate total size of main example
					if (e == 0)
					{
						Rectangle2D polyBounds = poly.getBounds2D();
						if (i == 0)
						{
							mainBounds = polyBounds;
						} else
						{
							Rectangle2D.union(mainBounds, polyBounds, mainBounds);
						}
					}

					// create the node cell on the first valid layer
					if (first)
					{
						first = false;
						String fName = "node-" + pnp.getName() + "{lay}";

						// make sure the node doesn't exist
						if (lib.findNodeProto(fName) != null)
						{
							System.out.println("Warning: multiple nodes named '" + fName + "'");
							break;
						}

						nNp = Cell.makeInstance(lib, fName);
						if (nNp == null) return null;

						nNp.setTechnology(Artwork.tech);
						nNp.setInTechnologyLibrary();
						nIn.generate(nNp);
					}

					// create the node to describe this layer
					NodeInst ni = placeGeometry(poly, nNp);
					if (ni == null) {
                        System.out.println("Error placing geometry " + poly.getStyle() + " on " + nNp);
                        continue;
                    }

					// get graphics for this layer
					Manipulate.setPatch(ni, desc);
					Cell layerCell = layerCells.get(nodeLayer);
					if (layerCell != null) ni.newVar(Info.LAYER_KEY, layerCell.getId());
					ni.newVar(Info.OPTION_KEY, new Integer(Info.LAYERPATCH));

// 					// set minimum polygon factor on smallest example
// 					if (e != 0) continue;
// 					if (i < nodeLayers.length)
// 					{
// 						if (nodeLayers[i].getRepresentation() == Technology.NodeLayer.MINBOX)
// 						{
// 							ni.newDisplayVar(Info.MINSIZEBOX_KEY, "MIN");
// 						}
// 					}
				}
				if (first) continue;

				// create the highlight node
				xS = pnp.getDefWidth() - so.getLowXOffset() - so.getHighXOffset();
				yS = pnp.getDefHeight() - so.getLowYOffset() - so.getHighYOffset();
				Point2D loc = new Point2D.Double(pos[e].getX() + (so.getLowXOffset() - so.getHighXOffset())/2,
					pos[e].getY() + (so.getLowYOffset() - so.getHighYOffset())/2);
				NodeInst ni = NodeInst.makeInstance(Artwork.tech.boxNode, loc, xsc[e], ysc[e], nNp);
				if (ni == null) return null;
				ni.newVar(Artwork.ART_COLOR, new Integer(EGraphics.makeIndex(Color.WHITE)));
				ni.newVar(Info.OPTION_KEY, new Integer(Info.HIGHLIGHTOBJ));

				// create a grab node (only in main example)
//				if (e == 0)
//				{
//					var = getvalkey((INTBIG)pnp, VNODEPROTO, VINTEGER|VISARRAY, el_prototype_center_key);
//					if (var != NOVARIABLE)
//					{
//						lx = hx = xpos[0] + ((INTBIG *)var.addr)[0];
//						ly = hy = ypos[0] + ((INTBIG *)var.addr)[1];
//						lx = muldiv(lx, lambda, oldlam);
//						hx = muldiv(hx, lambda, oldlam);
//						ly = muldiv(ly, lambda, oldlam);
//						hy = muldiv(hy, lambda, oldlam);
//						nodeprotosizeoffset(gen_cellcenterprim, &lxo, &lyo, &hxo, &hyo, np);
//						ni = newnodeinst(gen_cellcenterprim, lx-lxo, hx+hxo, ly-lyo, hy+hyo, 0, 0, np);
//						if (ni == null) return(NOLIBRARY);
//					}
//				}

				// also draw ports
				HashMap<PrimitivePort,NodeInst> portNodes = new HashMap<PrimitivePort,NodeInst>();
				for(Iterator<PortProto> pIt = pnp.getPorts(); pIt.hasNext(); )
				{
					PrimitivePort pp = (PrimitivePort)pIt.next();
					Poly poly = tech.getShapeOfPort(oNi, pp);
					SizeOffset pSo = Generic.tech.portNode.getProtoSizeOffset();
					double width = poly.getBounds2D().getWidth() + pSo.getLowXOffset() + pSo.getHighXOffset();
					double height = poly.getBounds2D().getHeight() + pSo.getLowYOffset() + pSo.getHighYOffset();
					NodeInst pNi = NodeInst.makeInstance(Generic.tech.portNode, new Point2D.Double(poly.getCenterX(), poly.getCenterY()),
						width, height, nNp);
					if (pNi == null) return null;
					portNodes.put(pp, pNi);
					pNi.newVar(Info.OPTION_KEY, new Integer(Info.LAYERPATCH));
					pNi.newDisplayVar(Info.PORTNAME_KEY, pp.getName());

					// on the first sample, also show angle and connection
					if (e != 0) continue;
					if (pp.getAngle() != 0 || pp.getAngleRange() != 180)
					{
						pNi.newVar(Info.PORTANGLE_KEY, new Integer(pp.getAngle()));
						pNi.newVar(Info.PORTRANGE_KEY, new Integer(pp.getAngleRange()));
					}

					// add in the "local" port connections (from this tech)
					ArcProto [] connects = pp.getConnections();
					List<Cell> validConns = new ArrayList<Cell>();
					for(int i=0; i<connects.length; i++)
					{
						if (connects[i].getTechnology() != tech) continue;
						Cell cell = arcCells.get(connects[i]);
						if (cell != null) validConns.add(cell);
                        for (int k = 0; k < aList.length; k++) {
                            if (aList[k].name.equals(connects[i].getName())) {
                                break;
                            }
                        }
					}
					if (validConns.size() > 0)
					{
						CellId [] aplist = new CellId[validConns.size()];
						for(int i=0; i<validConns.size(); i++) {
                            Cell cell = validConns.get(i);
							aplist[i] = cell.getId();
                        }
						pNi.newVar(Info.CONNECTION_KEY, aplist);
					}

					// connect the connected ports
					for(Iterator<PortProto> oPIt = pnp.getPorts(); oPIt.hasNext(); )
					{
						PrimitivePort opp = (PrimitivePort)oPIt.next();
						if (opp == pp) break;
						if (opp.getTopology() != pp.getTopology()) continue;
						NodeInst nni = portNodes.get(opp);
						if (nni == null) continue;
						PortInst head = nni.getOnlyPortInst();
						PortInst tail = pNi.getOnlyPortInst();
						ArcInst.newInstanceBase(Generic.tech.universal_arc, 0, head, tail);
						break;
					}
				}
			}

			// compact it accordingly
			NodeInfo.compactCell(nNp);
		}

		// save the node sequence
		lib.newVar(Info.NODESEQUENCE_KEY, nodeSequence);

//		// create the design rule information
//		rules = dr_allocaterules(layerTotal, nodeTotal, tech.techname);
//		if (rules == NODRCRULES) return(NOLIBRARY);
//		for(i=0; i<layerTotal; i++)
//			(void)allocstring(&rules.layernames[i], layername(tech, i), el_tempcluster);
//		i = 0;
//		for(np = tech.firstnodeproto; np != NONODEPROTO; np = np.nextnodeproto)
//			if (np.temp1 != 0)
//				(void)allocstring(&rules.nodenames[i++],  &((NODEPROTO *)np.temp1).protoname[5],
//					el_tempcluster);
//		var = getvalkey((INTBIG)tech, VTECHNOLOGY, VFRACT|VISARRAY, dr_min_widthkey);
//		if (var != NOVARIABLE)
//			for(i=0; i<rules.numlayers; i++) rules.minwidth[i] = ((INTBIG *)var.addr)[i];
//		var = getvalkey((INTBIG)tech, VTECHNOLOGY, VSTRING|VISARRAY, dr_min_width_rulekey);
//		if (var != NOVARIABLE)
//			for(i=0; i<rules.numlayers; i++)
//				(void)reallocstring(&rules.minwidthR[i], ((CHAR **)var.addr)[i], el_tempcluster);
//		var = getvalkey((INTBIG)tech, VTECHNOLOGY, VFRACT|VISARRAY, dr_connected_distanceskey);
//		if (var != NOVARIABLE)
//			for(i=0; i<rules.utsize; i++) rules.conlist[i] = ((INTBIG *)var.addr)[i];
//		var = getvalkey((INTBIG)tech, VTECHNOLOGY, VSTRING|VISARRAY, dr_connected_distances_rulekey);
//		if (var != NOVARIABLE)
//			for(i=0; i<rules.utsize; i++)
//				(void)reallocstring(&rules.conlistR[i], ((CHAR **)var.addr)[i], el_tempcluster);
//		var = getvalkey((INTBIG)tech, VTECHNOLOGY, VFRACT|VISARRAY, dr_unconnected_distanceskey);
//		if (var != NOVARIABLE)
//			for(i=0; i<rules.utsize; i++) rules.unconlist[i] = ((INTBIG *)var.addr)[i];
//		var = getvalkey((INTBIG)tech, VTECHNOLOGY, VSTRING|VISARRAY, dr_unconnected_distances_rulekey);
//		if (var != NOVARIABLE)
//			for(i=0; i<rules.utsize; i++)
//				(void)reallocstring(&rules.unconlistR[i], ((CHAR **)var.addr)[i], el_tempcluster);
//		var = getvalkey((INTBIG)tech, VTECHNOLOGY, VFRACT|VISARRAY, dr_connected_distancesWkey);
//		if (var != NOVARIABLE)
//			for(i=0; i<rules.utsize; i++) rules.conlistW[i] = ((INTBIG *)var.addr)[i];
//		var = getvalkey((INTBIG)tech, VTECHNOLOGY, VSTRING|VISARRAY, dr_connected_distancesW_rulekey);
//		if (var != NOVARIABLE)
//			for(i=0; i<rules.utsize; i++)
//				(void)reallocstring(&rules.conlistWR[i], ((CHAR **)var.addr)[i], el_tempcluster);
//		var = getvalkey((INTBIG)tech, VTECHNOLOGY, VFRACT|VISARRAY, dr_unconnected_distancesWkey);
//		if (var != NOVARIABLE)
//			for(i=0; i<rules.utsize; i++) rules.unconlistW[i] = ((INTBIG *)var.addr)[i];
//		var = getvalkey((INTBIG)tech, VTECHNOLOGY, VSTRING|VISARRAY, dr_unconnected_distancesW_rulekey);
//		if (var != NOVARIABLE)
//			for(i=0; i<rules.utsize; i++)
//				(void)reallocstring(&rules.unconlistWR[i], ((CHAR **)var.addr)[i], el_tempcluster);
//		var = getvalkey((INTBIG)tech, VTECHNOLOGY, VFRACT|VISARRAY, dr_connected_distancesMkey);
//		if (var != NOVARIABLE)
//			for(i=0; i<rules.utsize; i++) rules.conlistM[i] = ((INTBIG *)var.addr)[i];
//		var = getvalkey((INTBIG)tech, VTECHNOLOGY, VSTRING|VISARRAY, dr_connected_distancesM_rulekey);
//		if (var != NOVARIABLE)
//			for(i=0; i<rules.utsize; i++)
//				(void)reallocstring(&rules.conlistMR[i], ((CHAR **)var.addr)[i], el_tempcluster);
//		var = getvalkey((INTBIG)tech, VTECHNOLOGY, VFRACT|VISARRAY, dr_unconnected_distancesMkey);
//		if (var != NOVARIABLE)
//			for(i=0; i<rules.utsize; i++) rules.unconlistM[i] = ((INTBIG *)var.addr)[i];
//		var = getvalkey((INTBIG)tech, VTECHNOLOGY, VSTRING|VISARRAY, dr_unconnected_distancesM_rulekey);
//		if (var != NOVARIABLE)
//			for(i=0; i<rules.utsize; i++)
//				(void)reallocstring(&rules.unconlistMR[i], ((CHAR **)var.addr)[i], el_tempcluster);
//		var = getvalkey((INTBIG)tech, VTECHNOLOGY, VFRACT|VISARRAY, dr_edge_distanceskey);
//		if (var != NOVARIABLE)
//			for(i=0; i<rules.utsize; i++) rules.edgelist[i] = ((INTBIG *)var.addr)[i];
//		var = getvalkey((INTBIG)tech, VTECHNOLOGY, VSTRING|VISARRAY, dr_edge_distances_rulekey);
//		if (var != NOVARIABLE)
//			for(i=0; i<rules.utsize; i++)
//				(void)reallocstring(&rules.edgelistR[i], ((CHAR **)var.addr)[i], el_tempcluster);
//		var = getvalkey((INTBIG)tech, VTECHNOLOGY, VFRACT, dr_wide_limitkey);
//		if (var != NOVARIABLE) rules.widelimit = var.addr;
//		var = getvalkey((INTBIG)tech, VTECHNOLOGY, VFRACT|VISARRAY, dr_min_node_sizekey);
//		if (var != NOVARIABLE)
//		{
//			i = j = 0;
//			for(np = tech.firstnodeproto; np != NONODEPROTO; np = np.nextnodeproto)
//			{
//				if (np.temp1 != 0)
//				{
//					rules.minnodesize[i*2] = ((INTBIG *)var.addr)[j*2];
//					rules.minnodesize[i*2+1] = ((INTBIG *)var.addr)[j*2+1];
//
//					// if rule is valid, make sure it is no larger than actual size
//					if (rules.minnodesize[i*2] > 0 && rules.minnodesize[i*2+1] > 0)
//					{
//						if (rules.minnodesize[i*2] > minnodesize[i*2])
//							rules.minnodesize[i*2] = minnodesize[i*2];
//						if (rules.minnodesize[i*2+1] > minnodesize[i*2+1])
//							rules.minnodesize[i*2+1] = minnodesize[i*2+1];
//					}
//					i++;
//				}
//				j++;
//			}
//		}
//		var = getvalkey((INTBIG)tech, VTECHNOLOGY, VSTRING|VISARRAY, dr_min_node_size_rulekey);
//		if (var != NOVARIABLE)
//		{
//			i = j = 0;
//			for(np = tech.firstnodeproto; np != NONODEPROTO; np = np.nextnodeproto)
//			{
//				if (np.temp1 != 0)
//				{
//					reallocstring(&rules.minnodesizeR[i], ((CHAR **)var.addr)[j], el_tempcluster);
//					i++;
//				}
//				j++;
//			}
//		}
//
//		us_tecedloaddrcmessage(rules, lib);
//		dr_freerules(rules);
        
        Object[][] origPalette = tech.getNodesGrouped();
        int numRows = origPalette.length;
        int numCols = origPalette[0].length;
        for (Object[] row: origPalette) {
            assert row.length == numCols;
        }
        gi.menuPalette = new Object[numRows][numCols];
        for (int row = 0; row < numRows; row++) {
            for (int col = 0; col < numCols; col++) {
                Object origEntry = origPalette[row][col];
                Object newEntry = null;
                if (origEntry instanceof ArcProto) {
                    ArcProto ap = (ArcProto)origEntry;
                    for (ArcInfo ai: aList) {
                        if (ai.name.equals(ap.getName())) {
                            newEntry = ai;
                            break;
                        }
                    }
                } else if (origEntry instanceof PrimitiveNode) {
                    PrimitiveNode pnp = (PrimitiveNode)origEntry;
                     for (NodeInfo ni: nList) {
                        if (ni.name.equals(pnp.getName())) {
                            newEntry = ni;
                            break;
                        }
                    }
                } else if (origEntry != null) {
                    newEntry = origEntry.toString();
                }
                gi.menuPalette[row][col] = newEntry;
            }
        }
        
//        String techName = tech.getTechName();
//        LibToTech.writeXml(techName + ".xml", techName, gi, lList, nList, aList);

		// clean up
		System.out.println("Done.");
		return(lib);
	}

    private static ArcInfo makeArcInfo(ArcProto ap, LayerInfo[] lList)
    {
        ArcInfo aIn = new ArcInfo();
        aIn.name = ap.getName();
        aIn.func = ap.getFunction();
        aIn.widthOffset = ap.getLambdaWidthOffset();
        aIn.maxWidth = ap.getDefaultLambdaFullWidth();
        aIn.fixAng = ap.isFixedAngle();
        aIn.wipes = ap.isWipable();
        aIn.noExtend = !ap.isExtended();
        aIn.curvable = ap.isCurvable();
        aIn.special = ap.isSpecialArc();
        aIn.notUsed = ap.isNotUsed();
        aIn.skipSizeInPalette = ap.isSkipSizeInPalette();
        aIn.slidable = ap.isSlidable();
        aIn.angInc = ap.getAngleIncrement();
        aIn.antennaRatio = ERC.getERCTool().getAntennaRatio(ap);
        aIn.arcDetails = new ArcInfo.LayerDetails[ap.getNumArcLayers()];
        for(int i=0; i<aIn.arcDetails.length; i++) {
            ArcInfo.LayerDetails ald = new ArcInfo.LayerDetails();
            aIn.arcDetails[i] = ald;
            String layerName = ap.getLayer(i).getName();
            for(int j=0; j<lList.length; j++) {
                if (lList[j].name.equals(layerName)) { ald.layer = lList[j];   break; }
            }
            ald.style = ap.getLayerStyle(i);
            ald.width = DBMath.gridToLambda(ap.getGridWidthOffset() - 2*ap.getLayerGridExtend(i));
        }
        return aIn;
    }
    
    private static NodeInfo makeNodeInfo(PrimitiveNode pnp, LayerInfo[] lList, ArcInfo[] aList) {
        Technology tech = pnp.getTechnology();
        NodeInfo nIn = new NodeInfo();
        nIn.name = pnp.getName();
        nIn.func = pnp.getFunction();
        nIn.serp = false;
        if ((nIn.func == PrimitiveNode.Function.TRANMOS || nIn.func == PrimitiveNode.Function.TRAPMOS ||
                nIn.func == PrimitiveNode.Function.TRADMOS) && pnp.isHoldsOutline()) nIn.serp = true;
        nIn.arcsShrink = pnp.isArcsShrink();
        assert pnp.isArcsWipe() == nIn.arcsShrink;
        nIn.square = pnp.isSquare();
        assert pnp.isHoldsOutline() == (pnp.getSpecialType() == PrimitiveNode.POLYGONAL || pnp.getSpecialType() == PrimitiveNode.SERPTRANS);
        nIn.canBeZeroSize = pnp.isCanBeZeroSize();
        nIn.wipes = pnp.isWipeOn1or2();
        nIn.lockable = pnp.isLockedPrim();
        nIn.edgeSelect = pnp.isEdgeSelect();
        nIn.skipSizeInPalette = pnp.isSkipSizeInPalette();
        nIn.notUsed = pnp.isNotUsed();
        nIn.lowVt = pnp.isNodeBitOn(PrimitiveNode.LOWVTBIT);
        nIn.highVt = pnp.isNodeBitOn(PrimitiveNode.HIGHVTBIT);
        nIn.nativeBit = pnp.isNodeBitOn(PrimitiveNode.NATIVEBIT);
        nIn.od18 = pnp.isNodeBitOn(PrimitiveNode.OD18BIT);
        nIn.od25 = pnp.isNodeBitOn(PrimitiveNode.OD25BIT);
        nIn.od33 = pnp.isNodeBitOn(PrimitiveNode.OD33BIT);
        nIn.xSize = pnp.getDefWidth();
        nIn.ySize = pnp.getDefHeight();
        nIn.so = pnp.getProtoSizeOffset();
        if (nIn != null && nIn.so.getLowXOffset() == 0 && nIn.so.getHighXOffset() == 0 && nIn.so.getLowYOffset() == 0 && nIn.so.getHighYOffset() == 0)
            nIn.so = null;
        nIn.nodeSizeRule = pnp.getMinSizeRule();
        nIn.autoGrowth = pnp.getAutoGrowth();
        nIn.specialType = pnp.getSpecialType();
        nIn.specialValues = pnp.getSpecialValues();
        List<Technology.NodeLayer> nodeLayers = Arrays.asList(pnp.getLayers());
        List<Technology.NodeLayer> electricalNodeLayers = nodeLayers;
        if (pnp.getElectricalLayers() != null)
            electricalNodeLayers = Arrays.asList(pnp.getElectricalLayers());
        List<NodeInfo.LayerDetails> layerDetails = new ArrayList<NodeInfo.LayerDetails>();
        int m = 0;
        for (Technology.NodeLayer nld: electricalNodeLayers) {
            int j = nodeLayers.indexOf(nld);
            if (j < 0) {
                layerDetails.add(makeNodeLayerDetails(nld, lList, false, true));
                continue;
            }
            while (m < j)
                layerDetails.add(makeNodeLayerDetails(nodeLayers.get(m++), lList, true, false));
            layerDetails.add(makeNodeLayerDetails(nodeLayers.get(m++), lList, true, true));
        }
        while (m < nodeLayers.size())
            layerDetails.add(makeNodeLayerDetails(nodeLayers.get(m++), lList, true, false));
        nIn.nodeLayers = layerDetails.toArray(new NodeInfo.LayerDetails[layerDetails.size()]);
        
        nIn.nodePortDetails = new NodeInfo.PortDetails[pnp.getNumPorts()];
        for (int i = 0; i < nIn.nodePortDetails.length; i++) {
            PrimitivePort pp = pnp.getPort(i);
            NodeInfo.PortDetails pd = new NodeInfo.PortDetails();
            nIn.nodePortDetails[i] = pd;
            pd.name = pp.getName();
            pd.netIndex = pp.getTopology();
            pd.angle = pp.getAngle();
            pd.range = pp.getAngleRange();
            pd.values = new Technology.TechPoint[] {
                new Technology.TechPoint(pp.getLeft(), pp.getBottom()),
                new Technology.TechPoint(pp.getRight(), pp.getTop())};
            pd.characterisitic = pp.getCharacteristic();
            pd.isolated = pp.isIsolated();
            pd.negatable = pp.isNegatable();
            
            ArcProto [] connects = pp.getConnections();
            List<ArcInfo> validArcInfoConns = new ArrayList<ArcInfo>();
            for(int j=0; j<connects.length; j++) {
                ArcProto ap = connects[j];
                if (ap.getTechnology() != tech) continue;
                for (int k = 0; k < aList.length; k++) {
                    if (aList[k].name.equals(ap.getName())) {
                        validArcInfoConns.add(aList[k]);
                        break;
                    }
                }
            }
            pd.connections = validArcInfoConns.toArray(new ArcInfo[validArcInfoConns.size()]);
        }
        if (nIn.func == PrimitiveNode.Function.NODE) {
            assert nIn.nodeLayers.length == 1;
            LayerInfo l = nIn.nodeLayers[0].layer;
            assert l.pureLayerNode == null;
            l.pureLayerNode = nIn;
        }
        return nIn;
    }
    
    private static NodeInfo.LayerDetails makeNodeLayerDetails(Technology.NodeLayer nl, LayerInfo[] lList, boolean inLayers, boolean inElectricalLayers) {
        NodeInfo.LayerDetails nld = new NodeInfo.LayerDetails();
        nld.inLayers = inLayers;
        nld.inElectricalLayers = inElectricalLayers;
        nld.style = nl.getStyle();
        nld.portIndex = nl.getPortNum();
        nld.representation = nl.getRepresentation();
        nld.values = nl.getPoints();
        for(int k=0; k<lList.length; k++) {
            if (nl.getLayer().getNonPseudoLayer().getName().equals(lList[k].name)) { nld.layer = lList[k];   break; }
        }
        nld.multiCut = nld.representation == Technology.NodeLayer.MULTICUTBOX;
        nld.multiXS = nl.getMulticutSizeX();
        nld.multiYS = nl.getMulticutSizeY();
        nld.multiSep = nl.getMulticutSep1D();
        nld.multiSep2D = nl.getMulticutSep2D();
        return nld;
    }
    
	private static NodeInst placeGeometry(Poly poly, Cell cell)
	{
		Rectangle2D box = poly.getBox();
		Rectangle2D bounds = poly.getBounds2D();
		Poly.Type style = poly.getStyle();
		if (style == Poly.Type.FILLED)
		{
			if (box != null)
			{
				return NodeInst.makeInstance(Artwork.tech.filledBoxNode, new Point2D.Double(box.getCenterX(), box.getCenterY()),
					box.getWidth(), box.getHeight(), cell);
			} else
			{
				NodeInst ni = NodeInst.makeInstance(Artwork.tech.filledPolygonNode, new Point2D.Double(bounds.getCenterX(), bounds.getCenterY()),
					bounds.getWidth(), bounds.getHeight(), cell);
				if (ni == null) return null;
				ni.setTrace(poly.getPoints());
				return ni;
			}
		}
		if (style == Poly.Type.CLOSED)
		{
			if (box != null)
			{
				return NodeInst.makeInstance(Artwork.tech.boxNode, new Point2D.Double(box.getCenterX(), box.getCenterY()),
					box.getWidth(), box.getHeight(), cell);
			} else
			{
				NodeInst ni = NodeInst.makeInstance(Artwork.tech.closedPolygonNode, new Point2D.Double(bounds.getCenterX(), bounds.getCenterY()),
					bounds.getWidth(), bounds.getHeight(), cell);
				if (ni == null) return null;
				ni.setTrace(poly.getPoints());
				return ni;
			}
		}
		if (style == Poly.Type.CROSSED)
		{
			NodeInst ni = NodeInst.makeInstance(Artwork.tech.crossedBoxNode, new Point2D.Double(bounds.getCenterX(), bounds.getCenterY()),
				bounds.getWidth(), bounds.getHeight(), cell);
			if (ni == null) return null;
			return ni;
		}
		if (style == Poly.Type.OPENED)
		{
			NodeInst ni = NodeInst.makeInstance(Artwork.tech.openedPolygonNode, new Point2D.Double(bounds.getCenterX(), bounds.getCenterY()),
				bounds.getWidth(), bounds.getHeight(), cell);
			if (ni == null) return null;
			ni.setTrace(poly.getPoints());
			return ni;
		}
		if (style == Poly.Type.OPENEDT1)
		{
			NodeInst ni = NodeInst.makeInstance(Artwork.tech.openedDottedPolygonNode, new Point2D.Double(bounds.getCenterX(), bounds.getCenterY()),
				bounds.getWidth(), bounds.getHeight(), cell);
			if (ni == null) return null;
			ni.setTrace(poly.getPoints());
			return ni;
		}
		if (style == Poly.Type.OPENEDT2)
		{
			NodeInst ni = NodeInst.makeInstance(Artwork.tech.openedDashedPolygonNode, new Point2D.Double(bounds.getCenterX(), bounds.getCenterY()),
				bounds.getWidth(), bounds.getHeight(), cell);
			if (ni == null) return null;
			ni.setTrace(poly.getPoints());
			return ni;
		}
		if (style == Poly.Type.OPENEDT3)
		{
			NodeInst ni = NodeInst.makeInstance(Artwork.tech.openedThickerPolygonNode, new Point2D.Double(bounds.getCenterX(), bounds.getCenterY()),
				bounds.getWidth(), bounds.getHeight(), cell);
			if (ni == null) return null;
			ni.setTrace(poly.getPoints());
			return ni;
		}
		if (style == Poly.Type.CIRCLE)
		{
			NodeInst ni = NodeInst.makeInstance(Artwork.tech.circleNode, new Point2D.Double(bounds.getCenterX(), bounds.getCenterY()),
				bounds.getWidth(), bounds.getHeight(), cell);
			if (ni == null) return null;
			return ni;
		}
		if (style == Poly.Type.THICKCIRCLE)
		{
			NodeInst ni = NodeInst.makeInstance(Artwork.tech.thickCircleNode, new Point2D.Double(bounds.getCenterX(), bounds.getCenterY()),
				bounds.getWidth(), bounds.getHeight(), cell);
			if (ni == null) return null;
			return ni;
		}
		if (style == Poly.Type.DISC)
		{
			NodeInst ni = NodeInst.makeInstance(Artwork.tech.filledCircleNode, new Point2D.Double(bounds.getCenterX(), bounds.getCenterY()),
				bounds.getWidth(), bounds.getHeight(), cell);
			if (ni == null) return null;
			return ni;
		}
		if (style == Poly.Type.CIRCLEARC)
		{
			NodeInst ni = NodeInst.makeInstance(Artwork.tech.circleNode, new Point2D.Double(bounds.getCenterX(), bounds.getCenterY()),
				bounds.getWidth(), bounds.getHeight(), cell);
			if (ni == null) return null;
			ni.setArcDegrees(0.0, 45.0*Math.PI/180.0);
			return ni;
		}
		if (style == Poly.Type.THICKCIRCLEARC)
		{
			NodeInst ni = NodeInst.makeInstance(Artwork.tech.thickCircleNode, new Point2D.Double(bounds.getCenterX(), bounds.getCenterY()),
				bounds.getWidth(), bounds.getHeight(), cell);
			if (ni == null) return null;
			ni.setArcDegrees(0.0, 45.0*Math.PI/180.0);
			return ni;
		}
		if (style == Poly.Type.TEXTCENT)
		{
			NodeInst ni = NodeInst.makeInstance(Generic.tech.invisiblePinNode, new Point2D.Double(bounds.getCenterX(), bounds.getCenterY()),
				bounds.getWidth(), bounds.getHeight(), cell);
			if (ni == null) return null;
			ni.newVar(Artwork.ART_MESSAGE, poly.getString(), TextDescriptor.getNodeTextDescriptor().withPos(TextDescriptor.Position.CENT));
			return ni;
		}
		if (style == Poly.Type.TEXTBOTLEFT)
		{
			NodeInst ni = NodeInst.makeInstance(Generic.tech.invisiblePinNode, new Point2D.Double(bounds.getCenterX(), bounds.getCenterY()),
				bounds.getWidth(), bounds.getHeight(), cell);
			if (ni == null) return null;
			ni.newVar(Artwork.ART_MESSAGE, poly.getString(), TextDescriptor.getNodeTextDescriptor().withPos(TextDescriptor.Position.UPRIGHT));
			return ni;
		}
		if (style == Poly.Type.TEXTBOTRIGHT)
		{
			NodeInst ni = NodeInst.makeInstance(Generic.tech.invisiblePinNode, new Point2D.Double(bounds.getCenterX(), bounds.getCenterY()),
				bounds.getWidth(), bounds.getHeight(), cell);
			if (ni == null) return null;
			ni.newVar(Artwork.ART_MESSAGE, poly.getString(), TextDescriptor.getNodeTextDescriptor().withPos(TextDescriptor.Position.UPLEFT));
			return ni;
		}
		if (style == Poly.Type.TEXTBOX)
		{
			NodeInst ni = NodeInst.makeInstance(Generic.tech.invisiblePinNode, new Point2D.Double(bounds.getCenterX(), bounds.getCenterY()),
				bounds.getWidth(), bounds.getHeight(), cell);
			if (ni == null) return null;
			ni.newVar(Artwork.ART_MESSAGE, poly.getString(), TextDescriptor.getNodeTextDescriptor().withPos(TextDescriptor.Position.BOXED));
			return ni;
		}
		return(null);
	}
}
