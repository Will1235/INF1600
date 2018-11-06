/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Technology.java
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
package com.sun.electric.technology;

import com.sun.electric.Main;
import com.sun.electric.database.ImmutableArcInst;
import com.sun.electric.database.ImmutableNodeInst;
import com.sun.electric.database.geometry.DBMath;
import com.sun.electric.database.geometry.EGraphics;
import com.sun.electric.database.geometry.EPoint;
import com.sun.electric.database.geometry.Orientation;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.PortCharacteristic;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.text.Pref;
import com.sun.electric.database.text.Setting;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.text.Version;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.Connection;
import com.sun.electric.database.topology.Geometric;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.database.variable.UserInterface;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.technologies.Artwork;
import com.sun.electric.technology.technologies.FPGA;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.technology.technologies.Schematics;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.erc.ERC;
import com.sun.electric.tool.user.ActivityLogger;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.projectSettings.ProjSettings;
import com.sun.electric.tool.user.projectSettings.ProjSettingsNode;

import java.awt.Color;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import javax.swing.SwingUtilities;

/**
 * Technology is the base class for all of the specific technologies in Electric.
 *
 * It is organized into two main areas: nodes and arcs.
 * Both nodes and arcs are composed of Layers.
 *<P>
 * Subclasses of Technology usually start by defining the Layers (such as Metal-1, Metal-2, etc.)
 * Then the ArcProto objects are created, built entirely from Layers.
 * Next PrimitiveNode objects are created, and they have Layers as well as connectivity to the ArcProtos.
 * The Technology concludes with miscellaneous data assignments of technology-wide information.
 * <P>
 * Here are the nodes in a sample CMOS technology.
 * Note that there are two types of transistors and diffusion contacts, one for Well and one for Substrate.
 * Each layer that can exist as a wire must have a pin node (in this case, metal, polysilicon, and two flavors of diffusion.
 * Note that there are pure-layer nodes at the bottom which allow arbitrary geometry to be constructed.
 * <CENTER><IMG SRC="doc-files/Technology-1.gif"></CENTER>
 * <P>
 * The Schematic technology has some unusual features.
 * <CENTER><IMG SRC="doc-files/Technology-2.gif"></CENTER>
 * <P>
 * Conceptually, a Technology has 3 types of information:
 * <UL><LI><I>Geometry</I>.  Each node and arc can be described in terms of polygons on differnt Layers.
 * The ArcLayer and NodeLayer subclasses help define those polygons.
 * <LI><I>Connectivity</I>.  The very structure of the nodes and arcs establisheds a set of rules of connectivity.
 * Examples include the list of allowable arc types that may connect to each port, and the use of port "network numbers"
 * to identify those that are connected internally.
 * <LI><I>Behavior</I>.  Behavioral information takes many forms, but they can all find a place here.
 * For example, each layer, node, and arc has a "function" that describes its general behavior.
 * Some information applies to the technology as a whole, for example SPICE model cards.
 * Other examples include Design Rules and technology characteristics.
 * </UL>
 * @author Steven M. Rubin
 */
public class Technology implements Comparable<Technology>
{
    private static final boolean LAZY_TECHNOLOGIES = false;

    /** key of Variable for saving scalable transistor contact information. */
    public static final Variable.Key TRANS_CONTACT = Variable.newKey("MOCMOS_transcontacts");

   /**
	 * Defines a single layer of a ArcProto.
	 * A ArcProto has a list of these ArcLayer objects, one for
	 * each layer in a typical ArcInst.
	 * Each ArcProto is composed of a number of ArcLayer descriptors.
	 * A descriptor converts a specific ArcInst into a polygon that describe this particular layer.
	 */
	protected static class ArcLayer
	{
		private final Layer layer;
		private final int gridOffset;
		private final Poly.Type style;

		/**
		 * Constructs an <CODE>ArcLayer</CODE> with the specified description.
		 * @param layer the Layer of this ArcLayer.
		 * @param lambdaOffset the distance from the outside of the ArcInst to this ArcLayer in lambda units.
		 * @param style the Poly.Style of this ArcLayer.
		 */
        public ArcLayer(Layer layer, double lambdaOffset, Poly.Type style) {
            this(layer, DBMath.lambdaToSizeGrid(lambdaOffset), style);
        }

        private ArcLayer(Layer layer, long gridOffset, Poly.Type style) {
            if (gridOffset < 0 || gridOffset >= Integer.MAX_VALUE/4 || (gridOffset&1) != 0)
                throw new IllegalArgumentException("gridOffset=" + gridOffset);
            this.layer = layer;
            this.gridOffset = (int)gridOffset;
            this.style = style;
        }

		/**
		 * Returns the Layer from the Technology to be used for this ArcLayer.
		 * @return the Layer from the Technology to be used for this ArcLayer.
		 */
		Layer getLayer() { return layer; }

		/**
		 * Returns the distance from the outside of the ArcInst to this ArcLayer in lambda units.
		 * This is the difference between the width of this layer and the overall width of the arc.
		 * For example, a value of 4 on an arc that is 6 wide indicates that this layer should be only 2 wide.
		 * @return the distance from the outside of the ArcInst to this ArcLayer in lambda units.
		 */
		double getLambdaOffset() { return DBMath.gridToLambda(getGridOffset()); }

		/**
		 * Returns the distance from the outside of the ArcInst to this ArcLayer in grid units.
		 * This is the difference between the width of this layer and the overall width of the arc.
		 * For example, a value of 4 on an arc that is 6 wide indicates that this layer should be only 2 wide.
		 * @return the distance from the outside of the ArcInst to this ArcLayer in grid units.
		 */
		int getGridOffset() { return gridOffset; }

		/**
         * Returns ArcLayer which differs from this ArcLayer by offset.
         * Offset is specified in grid units.
         * This is the difference between the width of this layer and the overall width of the arc.
         * For example, a value of 4 on an arc that is 6 wide indicates that this layer should be only 2 wide.
         * @param gridOffset the distance from the outside of the ArcInst to this ArcLayer in grid units.
         */
		ArcLayer withGridOffset(long gridOffset) {
            if (this.gridOffset == gridOffset) return this;
            return new ArcLayer(layer, gridOffset, style);
        }

		/**
		 * Returns the Poly.Style of this ArcLayer.
		 * @return the Poly.Style of this ArcLayer.
		 */
		Poly.Type getStyle() { return style; }
	}

	/**
	 * Defines a point in space that is relative to a NodeInst's bounds.
	 * The TechPoint has two coordinates: X and Y.
	 * Each of these coordinates is represented by an Edge class (EdgeH for X
	 * and EdgeV for Y).
	 * The Edge classes have two numbers: a multiplier and an adder.
	 * The desired coordinate takes the NodeInst's center, adds in the
	 * product of the Edge multiplier and the NodeInst's size, and then adds
	 * in the Edge adder.
	 * <P>
	 * Arrays of TechPoint objects can be used to describe the bounds of
	 * a particular layer in a NodeInst.  Typically, four TechPoint objects
	 * can describe a rectangle.  Circles only need two (center and edge).
	 * The <CODE>Poly.Style</CODE> class defines the possible types of
	 * geometry.
	 * @see EdgeH
	 * @see EdgeV
	 */
	public static class TechPoint
	{
		private EdgeH x;
		private EdgeV y;

		/**
		 * Constructs a <CODE>TechPoint</CODE> with the specified description.
		 * @param x the EdgeH that converts a NodeInst into an X coordinate on that NodeInst.
		 * @param y the EdgeV that converts a NodeInst into a Y coordinate on that NodeInst.
		 */
		public TechPoint(EdgeH x, EdgeV y)
		{
			this.x = x;
			this.y = y;
		}

		/**
		 * Method to make a copy of this TechPoint, with all newly allocated parts.
		 * @return a new TechPoint with the values in this one.
		 */
		public TechPoint duplicate()
		{
			TechPoint newTP = new TechPoint(new EdgeH(x.getMultiplier(), x.getAdder()), new EdgeV(y.getMultiplier(), y.getAdder()));
			return newTP;
		}

		/**
		 * Method to make a 2-long TechPoint array that describes a point at the center of the node.
		 * @return a new TechPoint array that describes a point at the center of the node.
		 */
		public static TechPoint [] makeCenterBox()
		{
			return new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.fromCenter(0), EdgeV.fromCenter(0)),
					new Technology.TechPoint(EdgeH.fromCenter(0), EdgeV.fromCenter(0))};
		}

		/**
		 * Method to make a 2-long TechPoint array that describes a box that fills the node.
		 * @return a new TechPoint array that describes a box that fills the node.
		 */
		public static TechPoint [] makeFullBox()
		{
			return makeIndented(0);
		}

		/**
		 * Method to make a 2-long TechPoint array that describes indentation by a specified amount.
		 * @param amount the amount to indent the box.
		 * @return a new TechPoint array that describes this indented box.
		 */
		public static TechPoint [] makeIndented(double amount)
		{
			return new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.fromLeft(amount), EdgeV.fromBottom(amount)),
					new Technology.TechPoint(EdgeH.fromRight(amount), EdgeV.fromTop(amount))};
		}

        /**
         * Method similat to makeIndented(double amount) where the X and Y specified amounts are different
         * @param amountX the amount to indent the box along X.
         * @param amountY the amount to indent the box along Y.
         * @return a new TechPoint array that describes this indented box.
         */
        public static TechPoint [] makeIndented(double amountX, double amountY)
		{
			return new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.fromLeft(amountX), EdgeV.fromBottom(amountY)),
					new Technology.TechPoint(EdgeH.fromRight(amountX), EdgeV.fromTop(amountY))};
		}

        /**
         * Method to make a 2-long TechPoint array that describes indentation from the center by a specified amount.
         * @param amountX the amount to indent from the center the box along X.
         * @param amountY the amount to indent from the center the box along Y.
         * @return a new TechPoint array that describes this indented box.
         */
        public static TechPoint [] makeIndentedFromCenter(double amountX, double amountY)
		{
			return new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.fromCenter(-amountX), EdgeV.fromCenter(-amountY)),
					new Technology.TechPoint(EdgeH.fromCenter(amountX), EdgeV.fromCenter(amountY))};
		}

        /**
		 * Returns the EdgeH that converts a NodeInst into an X coordinate on that NodeInst.
		 * @return the EdgeH that converts a NodeInst into an X coordinate on that NodeInst.
		 */
		public EdgeH getX() { return x; }

		/**
		 * Returns the EdgeV that converts a NodeInst into a Y coordinate on that NodeInst.
		 * @return the EdgeV that converts a NodeInst into a Y coordinate on that NodeInst.
		 */
		public EdgeV getY() { return y; }
	}

	/**
	 * Defines a single layer of a PrimitiveNode.
	 * A PrimitiveNode has a list of these NodeLayer objects, one for
	 * each layer in a typical NodeInst.
	 * Each PrimitiveNode is composed of a number of NodeLayer descriptors.
	 * A descriptor converts a specific NodeInst into a polygon that describe this particular layer.
	 */
	public static class NodeLayer
	{
		private Layer layer;
		private int portNum;
		private Poly.Type style;
		private int representation;
		private TechPoint [] points;
		private String message;
		private TextDescriptor descriptor;
		private double lWidth, rWidth, extentT, extendB;
        private long cutGridSizeX, cutGridSizeY, cutGridSep1D, cutGridSep2D;

		// the meaning of "representation"
		/**
		 * Indicates that the "points" list defines scalable points.
		 * Each point here becomes a point on the Poly.
		 */
		public static final int POINTS = 0;

		/**
		 * Indicates that the "points" list defines a rectangle.
		 * It contains two diagonally opposite points.
		 */
		public static final int BOX = 1;

//		/**
//		 * Indicates that the "points" list defines a minimum sized rectangle.
//		 * It contains two diagonally opposite points, like BOX,
//		 * and also contains a minimum box size beyond which the polygon will not shrink
//		 * (again, two diagonally opposite points).
//		 */
//		public static final int MINBOX = 2;

		/**
		 * Indicates that the "points" list defines a rectangle,
         * where centers of multi-cut are located
		 * It contains two diagonally opposite points.
		 */
		public static final int MULTICUTBOX = 3;

		/**
		 * Constructs a <CODE>NodeLayer</CODE> with the specified description.
		 * @param layer the <CODE>Layer</CODE> this is on.
		 * @param portNum a 0-based index of the port (from the actual NodeInst) on this layer.
		 * A negative value indicates that this layer is not connected to an electrical layer.
		 * @param style the Poly.Type this NodeLayer will generate (polygon, circle, text, etc.).
		 * @param representation tells how to interpret "points".  It can be POINTS, BOX, or MULTICUTBOX.
		 * @param points the list of coordinates (stored as TechPoints) associated with this NodeLayer.
		 */
		public NodeLayer(Layer layer, int portNum, Poly.Type style, int representation, TechPoint [] points)
		{
			this.layer = layer;
			this.portNum = portNum;
			this.style = style;
			this.representation = representation;
			this.points = points;
			descriptor = TextDescriptor.EMPTY;
			this.lWidth = this.rWidth = this.extentT = this.extendB = 0;
		}

		/**
		 * Constructs a <CODE>NodeLayer</CODE> with the specified description.
		 * This form of the method, with 4 additional parameters at the end,
		 * is only used for serpentine transistors.
		 * @param layer the <CODE>Layer</CODE> this is on.
		 * @param portNum a 0-based index of the port (from the actual NodeInst) on this layer.
		 * A negative value indicates that this layer is not connected to an electrical layer.
		 * @param style the Poly.Type this NodeLayer will generate (polygon, circle, text, etc.).
		 * @param representation tells how to interpret "points".  It can be POINTS, BOX, or MULTICUTBIX.
		 * @param points the list of coordinates (stored as TechPoints) associated with this NodeLayer.
		 * @param lWidth the left extension of this layer, measured from the <I>centerline</I>.
		 * The centerline is the path that the serpentine transistor follows (it defines the path of the
		 * polysilicon).  So, for example, if lWidth is 4 and rWidth is 4, it creates a NodeLayer that is 8 wide
		 * (with 4 to the left and 4 to the right of the centerline).
		 * Left and Right widths define the size of the Active layers.
		 * @param rWidth the right extension the right of this layer, measured from the <I>centerline</I>.
		 * @param extentT the top extension of this layer, measured from the end of the <I>centerline</I>.
		 * The top and bottom extensions apply to the ends of the centerline, and not to each segment
		 * along it.  They define the extension of the polysilicon.  For example, if extendT is 2,
		 * it indicates that the NodeLayer extends by 2 from the top end of the centerline.
		 * @param extendB the bottom extension of this layer, measured from the end of the <I>centerline</I>.
		 */
		public NodeLayer(Layer layer, int portNum, Poly.Type style, int representation, TechPoint [] points,
			double lWidth, double rWidth, double extentT, double extendB)
		{
			this.layer = layer;
			this.portNum = portNum;
			this.style = style;
			this.representation = representation;
			this.points = points;
			descriptor = TextDescriptor.EMPTY;
			this.lWidth = lWidth;
			this.rWidth = rWidth;
			this.extentT = extentT;
			this.extendB = extendB;
		}

        /**
         * Constructs a <CODE>NodeLayer</CODE> from given node
          * @param node
         */
        public NodeLayer(NodeLayer node)
        {
            this.layer = node.getLayerOrPseudoLayer();
			this.portNum = node.getPortNum();
			this.style = node.getStyle();
			this.representation = node.getRepresentation();
            this.descriptor = TextDescriptor.EMPTY;
            TechPoint [] oldPoints = node.getPoints();
			this.points = new TechPoint[oldPoints.length];
			for(int i=0; i<oldPoints.length; i++) points[i] = oldPoints[i].duplicate();
			this.lWidth = this.rWidth = this.extentT = this.extendB = 0;
        }

        public static NodeLayer makeMulticut(Layer layer, int portNum, Poly.Type style, TechPoint[] techPoints,
                double cutSizeX, double cutSizeY, double cutSep1D, double cutSep2D) {
			NodeLayer nl = new NodeLayer(layer, portNum, style, Technology.NodeLayer.MULTICUTBOX, techPoints);
            nl.cutGridSizeX = DBMath.lambdaToGrid(cutSizeX);
            nl.cutGridSizeY = DBMath.lambdaToGrid(cutSizeY);
            nl.cutGridSep1D = DBMath.lambdaToGrid(cutSep1D);
            nl.cutGridSep2D = DBMath.lambdaToGrid(cutSep2D);
            return nl;
        }

		/**
		 * Returns the <CODE>Layer</CODE> object associated with this NodeLayer.
		 * @return the <CODE>Layer</CODE> object associated with this NodeLayer.
		 */
		public Layer getLayer() { return Layer.PSEUDO_HIDDEN ? layer.getNonPseudoLayer() : layer; }

		/**
		 * Tells whether this NodeLayer is associated with pseudo-layer.
		 * @return true if this NodeLayer is associated with pseudo-layer.
		 */
		public boolean isPseudoLayer() { return layer.isPseudoLayer(); }

		/**
		 * Returns the <CODE>Layer</CODE> or pseudo-layer object associated with this NodeLayer.
		 * @return the <CODE>Layer</CODE> or pseudo-layer object associated with this NodeLayer.
		 */
		public Layer getLayerOrPseudoLayer() { return layer; }

		/**
		 * Returns the 0-based index of the port associated with this NodeLayer.
		 * @return the 0-based index of the port associated with this NodeLayer.
		 */
		public int getPortNum() { return portNum; }

		/**
		 * Returns the Poly.Type this NodeLayer will generate.
		 * @return the Poly.Type this NodeLayer will generate.
		 * Examples are polygon, lines, splines, circle, text, etc.
		 */
		public Poly.Type getStyle() { return style; }

		/**
		 * Returns the method of interpreting "points".
		 * @return the method of interpreting "points".
		 * It can be POINTS, BOX, MINBOX, or MULTICUTBOX.
		 */
		public int getRepresentation() { return representation; }

		public static String getRepresentationName(int rep)
		{
			if (rep == POINTS) return "points";
			if (rep == BOX) return "box";
//			if (rep == MINBOX) return "min-box";
			if (rep == MULTICUTBOX) return "multi-cut-box";
			return "?";
		}

		/**
		 * Returns the list of coordinates (stored as TechPoints) associated with this NodeLayer.
		 * @return the list of coordinates (stored as TechPoints) associated with this NodeLayer.
		 */
		public TechPoint [] getPoints() { return points; }

        /**
         * Method to set new points to this NodeLayer
         * @param pts
         */
        public void setPoints(TechPoint [] pts) {points = pts; }

		/**
		 * Returns the left edge coordinate (a scalable EdgeH object) associated with this NodeLayer.
		 * @return the left edge coordinate associated with this NodeLayer.
		 * It only makes sense if the representation is BOX or MINBOX.
		 * The returned coordinate is a scalable EdgeH object.
		 */
		public EdgeH getLeftEdge() { return points[0].getX(); }

		/**
		 * Returns the bottom edge coordinate (a scalable EdgeV object) associated with this NodeLayer.
		 * @return the bottom edge coordinate associated with this NodeLayer.
		 * It only makes sense if the representation is BOX or MINBOX.
		 * The returned coordinate is a scalable EdgeV object.
		 */
		public EdgeV getBottomEdge() { return points[0].getY(); }

		/**
		 * Returns the right edge coordinate (a scalable EdgeH object) associated with this NodeLayer.
		 * @return the right edge coordinate associated with this NodeLayer.
		 * It only makes sense if the representation is BOX or MINBOX.
		 * The returned coordinate is a scalable EdgeH object.
		 */
		public EdgeH getRightEdge() { return points[1].getX(); }

		/**
		 * Returns the top edge coordinate (a scalable EdgeV object) associated with this NodeLayer.
		 * @return the top edge coordinate associated with this NodeLayer.
		 * It only makes sense if the representation is BOX or MINBOX.
		 * The returned coordinate is a scalable EdgeV object.
		 */
		public EdgeV getTopEdge() { return points[1].getY(); }

		/**
		 * Returns the text message associated with this list NodeLayer.
		 * @return the text message associated with this list NodeLayer.
		 * This only makes sense if the style is one of the TEXT types.
		 */
		public String getMessage() { return message; }

		/**
		 * Sets the text to be drawn by this NodeLayer.
		 * @param message the text to be drawn by this NodeLayer.
		 * This only makes sense if the style is one of the TEXT types.
		 */
		public void setMessage(String message) { this.message = message; }

		/**
		 * Returns the text descriptor associated with this list NodeLayer.
		 * @return the text descriptor associated with this list NodeLayer.
		 * This only makes sense if the style is one of the TEXT types.
		 */
		public TextDescriptor getDescriptor() { return descriptor; }

		/**
		 * Sets the text descriptor to be drawn by this NodeLayer.
		 * @param descriptor the text descriptor to be drawn by this NodeLayer.
		 * This only makes sense if the style is one of the TEXT types.
		 */
		public void setDescriptor(TextDescriptor descriptor)
		{
			this.descriptor = descriptor;
		}

		/**
		 * Returns the left extension of this layer.
		 * Only makes sense when this is a layer in a serpentine transistor.
		 * @return the left extension of this layer.
		 */
		public double getSerpentineLWidth() { return lWidth; }
		/**
		 * Sets the left extension of this layer.
		 * Only makes sense when this is a layer in a serpentine transistor.
		 * @param lWidth the left extension of this layer.
		 */
		public void setSerpentineLWidth(double lWidth) { this.lWidth = lWidth; }

		/**
		 * Returns the right extension of this layer.
		 * Only makes sense when this is a layer in a serpentine transistor.
		 * @return the right extension of this layer.
		 */
		public double getSerpentineRWidth() { return rWidth; }
		/**
		 * Sets the right extension of this layer.
		 * Only makes sense when this is a layer in a serpentine transistor.
		 * @param rWidth the right extension of this layer.
		 */
		public void setSerpentineRWidth(double rWidth) { this.rWidth = rWidth; }

		/**
		 * Returns the top extension of this layer.
		 * Only makes sense when this is a layer in a serpentine transistor.
		 * @return the top extension of this layer.
		 */
		public double getSerpentineExtentT() { return extentT; }
		/**
		 * Sets the top extension of this layer.
		 * Only makes sense when this is a layer in a serpentine transistor.
		 * @param extentT the top extension of this layer.
		 */
		public void setSerpentineExtentT(double extentT) { this.extentT = extentT; }

		/**
		 * Returns the bottom extension of this layer.
		 * Only makes sense when this is a layer in a serpentine transistor.
		 * @return the bottom extension of this layer.
		 */
		public double getSerpentineExtentB() { return extendB; }
		/**
		 * Sets the bottom extension of this layer.
		 * Only makes sense when this is a layer in a serpentine transistor.
		 * @param extendB the bottom extension of this layer.
		 */
		public void setSerpentineExtentB(double extendB) { this.extendB = extendB; }

        public double getMulticutSizeX() { return DBMath.gridToLambda(cutGridSizeX); }
        public double getMulticutSizeY() { return DBMath.gridToLambda(cutGridSizeY); }
        public double getMulticutSep1D() { return DBMath.gridToLambda(cutGridSep1D); }
        public double getMulticutSep2D() { return DBMath.gridToLambda(cutGridSep2D); }
	}

    public class SizeCorrector {
        public final HashMap<ArcProto,Integer> arcExtends = new HashMap<ArcProto,Integer>();
        public final HashMap<PrimitiveNode,EPoint> nodeExtends = new HashMap<PrimitiveNode,EPoint>();

        private SizeCorrector(Version version, boolean isJelib) {
            if (xmlTech != null) {
                int techVersion = 0;
                if (isJelib) {
                    for (Xml.Version xmlVersion: xmlTech.versions) {
                        if (version.compareTo(xmlVersion.electricVersion) >= 0 && techVersion < xmlVersion.techVersion)
                            techVersion = xmlVersion.techVersion;
                    }
                }
                for (Xml.ArcProto xap: xmlTech.arcs) {
                    ArcProto ap = arcs.get(xap.name);
                    double correction = 0;
                    for (Map.Entry<Integer,Double> e: xap.diskOffset.entrySet()) {
                        if (techVersion < e.getKey()) {
                            correction = e.getValue();
                            break;
                        }
                    }
                    arcExtends.put(ap, Integer.valueOf((int)DBMath.lambdaToGrid(correction)));
                }
                for (Xml.PrimitiveNode xpn: xmlTech.nodes) {
                    PrimitiveNode pn = nodes.get(xpn.name);
                    EPoint correction = EPoint.ORIGIN;
                    for (Map.Entry<Integer,EPoint> e: xpn.diskOffset.entrySet()) {
                        if (techVersion < e.getKey()) {
                            correction = e.getValue();
                            break;
                        }
                    }
                    correction = EPoint.fromGrid(correction.getGridX() + pn.sizeCorrector.getGridX(), correction.getGridY() + pn.sizeCorrector.getGridY());
                    nodeExtends.put(pn, correction);
                }
                for (Xml.Layer l: xmlTech.layers) {
                    if (l.pureLayerNode == null) continue;
                    PrimitiveNode pn = nodes.get(l.pureLayerNode.name);
                    nodeExtends.put(pn, EPoint.ORIGIN);
                }
            } else {
                boolean oldRevision = !isJelib || version.compareTo(Version.parseVersion("8.05g")) < 0;
                boolean newestRevision = isJelib && version.compareTo(Version.parseVersion("8.05o")) >= 0;
                for (ArcProto ap: arcs.values()) {
                    arcExtends.put(ap, Integer.valueOf(newestRevision ? 0 : oldRevision ? ap.getGridFullExtend() : ap.getGridBaseExtend()));
                }
                for (PrimitiveNode pn: nodes.values()) {
                    EPoint correction = EPoint.ORIGIN;
                    if (newestRevision) {
                        correction = pn.sizeCorrector;
                    } else if (!oldRevision) {
                        SizeOffset so = pn.getProtoSizeOffset();
                        correction = EPoint.fromLambda(-0.5*(so.getLowXOffset() + so.getHighXOffset()), -0.5*(so.getLowYOffset() + so.getHighYOffset()));
                    }
                    nodeExtends.put(pn, correction);
                }
            }
        }

        public long getExtendFromDisk(ArcProto ap, double width) {
            return DBMath.lambdaToGrid(0.5*width) - arcExtends.get(ap);
        }

        public long getWidthToDisk(ImmutableArcInst a) {
            return 2*(a.getGridExtendOverMin() + arcExtends.get(a.protoType));
        }

        public EPoint getSizeFromDisk(PrimitiveNode pn, double width, double height) {
            EPoint correction = nodeExtends.get(pn);
            return EPoint.fromLambda(width - 2*correction.getLambdaX(), height - 2*correction.getLambdaY());
        }

        public EPoint getSizeToDisk(ImmutableNodeInst n) {
            EPoint size = n.size;
            EPoint correction = nodeExtends.get(n.protoId);
            if (!correction.equals(EPoint.ORIGIN)) {
                size = EPoint.fromLambda(size.getLambdaX() + 2*correction.getLambdaX(), size.getLambdaY() + 2*correction.getLambdaY());
            }
            return size;
        }
    }

    public SizeCorrector getSizeCorrector(Version version, Map<Setting,Object> projectSettings, boolean isJelib, boolean keepExtendOverMin) {
        return new SizeCorrector(version, isJelib);
    }

    protected void setArcCorrection(SizeCorrector sc, String arcName, double lambdaBaseWidth) {
        ArcProto ap = findArcProto(arcName);
        Integer correction = sc.arcExtends.get(ap);
        int gridBaseExtend = (int)DBMath.lambdaToGrid(0.5*lambdaBaseWidth);
        if (gridBaseExtend != ap.getGridBaseExtend()) {
            correction = Integer.valueOf(correction.intValue() + gridBaseExtend - ap.getGridBaseExtend());
            sc.arcExtends.put(ap, correction);
        }
    }

	/** technology is not electrical */									private static final int NONELECTRICAL =       01;
	/** has no directional arcs */										private static final int NODIRECTIONALARCS =   02;
	/** has no negated arcs */											private static final int NONEGATEDARCS =       04;
	/** nonstandard technology (cannot be edited) */					private static final int NONSTANDARD =        010;
	/** statically allocated (don't deallocate memory) */				private static final int STATICTECHNOLOGY =   020;
	/** no primitives in this technology (don't auto-switch to it) */	private static final int NOPRIMTECHNOLOGY =   040;

	/** preferences for all technologies */					private static Pref.Group prefs = null;
	/** static list of all Technologies in Electric */		private static TreeMap<String,Technology> technologies = new TreeMap<String,Technology>();
	/** static list of all Technologies in Electric */		private static TreeMap<String,String> lazyClasses = new TreeMap<String,String>();
	/** static list of xml Technologies in Electric */		private static TreeMap<String,URL> lazyUrls = new TreeMap<String,URL>();
	/** the current technology in Electric */				private static Technology curTech = null;
	/** the current tlayout echnology in Electric */		private static Technology curLayoutTech = null;
	/** counter for enumerating technologies */				private static int techNumber = 0;

	/** name of this technology */							private final String techName;
	/** short, readable name of this technology */			private String techShortName;
	/** full description of this technology */				private String techDesc;
	/** flags for this technology */						private int userBits;
	/** 0-based index of this technology */					private int techIndex;
	/** true if "scale" is relevant to this technology */	private boolean scaleRelevant;
	/** number of transparent layers in technology */		private int transparentLayers;
	/** the saved transparent colors for this technology */	private Pref [] transparentColorPrefs;
	/** the color map for this technology */				private Color [] colorMap;
	/** list of layers in this technology */				private final List<Layer> layers = new ArrayList<Layer>();
    /** True when layer allocation is finished. */          private boolean layersAllocationLocked;
	/** list of primitive nodes in this technology */		private final LinkedHashMap<String,PrimitiveNode> nodes = new LinkedHashMap<String,PrimitiveNode>();
    /** Old names of primitive nodes */                     protected final HashMap<String,PrimitiveNode> oldNodeNames = new HashMap<String,PrimitiveNode>();
    /** count of primitive nodes in this technology */      private int nodeIndex = 0;
	/** list of arcs in this technology */					private final LinkedHashMap<String,ArcProto> arcs = new LinkedHashMap<String,ArcProto>();
    /** Old names of arcs */                                protected final HashMap<String,ArcProto> oldArcNames = new HashMap<String,ArcProto>();
	/** Spice header cards, level 1. */						private String [] spiceHeaderLevel1;
	/** Spice header cards, level 2. */						private String [] spiceHeaderLevel2;
	/** Spice header cards, level 3. */						private String [] spiceHeaderLevel3;
    /** resolution for this Technology */                   private Pref prefResolution;
    /** static list of all Manufacturers in Electric */     protected final List<Foundry> foundries = new ArrayList<Foundry>();
    /** default foundry for this Technology */              private final Setting cacheFoundry;
	/** scale for this Technology. */						private Setting cacheScale;
    /** number of metals for this Technology. */            private final Setting cacheNumMetalLayers;
	/** Minimum resistance for this Technology. */			private Setting cacheMinResistance;
	/** Minimum capacitance for this Technology. */			private Setting cacheMinCapacitance;
    /** Gate Length subtraction (in microns) for this Tech*/private final Setting cacheGateLengthSubtraction;
    /** Include gate in Resistance calculation */           private final Setting cacheIncludeGate;
    /** Include ground network in parasitics calculation */ private final Setting cacheIncludeGnd;
    /** Include ground network in parasitics calculation */ private final Setting cacheMaxSeriesResistance;
//	/** Logical effort global fanout preference. */			private final Setting cacheGlobalFanout;
//	/** Logical effort convergence (epsilon) preference. */	private final Setting cacheConvergenceEpsilon;
//	/** Logical effort maximum iterations preference. */	private final Setting cacheMaxIterations;
	/** Logical effort gate capacitance preference. */		private Setting cacheGateCapacitance;
	/** Logical effort wire ratio preference. */			private Setting cacheWireRatio;
	/** Logical effort diff alpha preference. */			private Setting cacheDiffAlpha;
//	/** Logical effort keeper ratio preference. */			private final Setting cacheKeeperRatio;

//	/** Default Logical effort global fanout. */			private static double DEFAULT_GLOBALFANOUT = 4.7;
//	/** Default Logical effort convergence (epsilon). */	private static double DEFAULT_EPSILON      = 0.001;
//	/** Default Logical effort maximum iterations. */		private static int    DEFAULT_MAXITER      = 30;
//	/** Default Logical effort keeper ratio. */				private static double DEFAULT_KEEPERRATIO  = 0.1;
	/** Default Logical effort gate capacitance. */			private static double DEFAULT_GATECAP      = 0.4;
	/** Default Logical effort wire ratio. */				private static double DEFAULT_WIRERATIO    = 0.16;
	/** Default Logical effort diff alpha. */				private static double DEFAULT_DIFFALPHA    = 0.7;

	/** To group elements for the component menu */         protected Object[][] nodeGroups;
	/** indicates n-type objects. */						public static final int N_TYPE = 1;
	/** indicates p-type objects. */						public static final int P_TYPE = 0;
	/** Cached rules for the technology. */		            protected DRCRules cachedRules = null;
    /** Xml representation of this Technology */            protected Xml.Technology xmlTech;

	/****************************** CONTROL ******************************/

	/**
	 * Constructs a <CODE>Technology</CODE>.
	 * This should not be called directly, but instead is invoked through each subclass's factory.
	 */
	protected Technology(String techName) {
        this(techName, Foundry.Type.NONE, 0);
    }

	/**
	 * Constructs a <CODE>Technology</CODE>.
	 * This should not be called directly, but instead is invoked through each subclass's factory.
	 */
	protected Technology(String techName, Foundry.Type defaultFoundry, int defaultNumMetals)
	{
		this.techName = techName;
		//this.scale = 1.0;
		this.scaleRelevant = true;
		this.techIndex = techNumber++;
		userBits = 0;
		if (prefs == null) prefs = Pref.groupForPackage(Schematics.class);
        cacheFoundry = TechSetting.makeStringSetting(this, "SelectedFoundryFor"+techName,
        	"Technology tab", techName + " foundry", getProjectSettings(), "Foundry", defaultFoundry.name().toUpperCase());
        cacheNumMetalLayers = TechSetting.makeIntSetting(this, techName + "NumberOfMetalLayers",
            "Technology tab", techName + ": Number of Metal Layers", getProjectSettings(), "NumMetalLayers", defaultNumMetals);

        cacheMaxSeriesResistance = makeParasiticSetting("MaxSeriesResistance", 10.0);
        cacheGateLengthSubtraction = makeParasiticSetting("GateLengthSubtraction", 0.0);
		cacheIncludeGate = makeParasiticSetting("Gate Inclusion", false);
		cacheIncludeGnd = makeParasiticSetting("Ground Net Inclusion", false);
//		cacheGlobalFanout = makeLESetting("GlobalFanout", DEFAULT_GLOBALFANOUT);
//		cacheConvergenceEpsilon = makeLESetting("ConvergenceEpsilon", DEFAULT_EPSILON);
//		cacheMaxIterations = makeLESetting("MaxIterations", DEFAULT_MAXITER);
//		cacheGateCapacitance = makeLESetting("GateCapacitance", DEFAULT_GATECAP);
//		cacheWireRatio = makeLESetting("WireRatio", DEFAULT_WIRERATIO);
//		cacheDiffAlpha = makeLESetting("DiffAlpha", DEFAULT_DIFFALPHA);
//        cacheKeeperRatio = makeLESetting("KeeperRatio", DEFAULT_KEEPERRATIO);

		// add the technology to the global list
		assert findTechnology(techName) == null;
		technologies.put(techName, this);
	}

//    protected Technology(URL urlXml, boolean full) {
//        this(Xml.parseTechnology(urlXml), full);
//    }
//
    public Technology(Xml.Technology t) {
        this(t.techName, Foundry.Type.valueOf(t.defaultFoundry), t.defaultNumMetals);
        xmlTech = t;
        setTechShortName(t.shortTechName);
        setTechDesc(t.description);
        int techVersion = 0;
        for (Xml.Version xmlVersion: t.versions) {
            if (Version.getVersion().compareTo(xmlVersion.electricVersion) >= 0 && techVersion < xmlVersion.techVersion)
                techVersion = xmlVersion.techVersion;
        }
        setFactoryScale(t.scaleValue, t.scaleRelevant);
        setFactoryParasitics(t.minResistance, t.minCapacitance);
        if (!t.transparentLayers.isEmpty())
            setFactoryTransparentLayers(t.transparentLayers.toArray(new Color[t.transparentLayers.size()]));
        HashMap<String,Layer> layers = new HashMap<String,Layer>();
        for (Xml.Layer l: t.layers) {
            Layer layer = Layer.newInstance(this, l.name, l.desc);
            layers.put(l.name, layer);
            layer.setFunction(l.function, l.extraFunction);
            if (l.cif != null)
                layer.setFactoryCIFLayer(l.cif);
            if (l.skill != null)
                layer.setFactorySkillLayer(l.skill);
            layer.setFactory3DInfo(l.thick3D, l.height3D, l.mode3D, l.factor3D);
            layer.setFactoryParasitics(l.resistance, l.capacitance, l.edgeCapacitance);
        }
        HashMap<String,ArcProto> arcs = new HashMap<String,ArcProto>();
        for (Xml.ArcProto a: t.arcs) {
            if (findArcProto(a.name) != null) {
                System.out.println("Error: technology " + getTechName() + " has multiple arcs named " + a.name);
                continue;
            }
            ArcLayer[] arcLayers = new ArcLayer[a.arcLayers.size()];
            long minGridExtend = Long.MAX_VALUE;
            long maxGridExtend = Long.MIN_VALUE;
            for (int i = 0; i < arcLayers.length; i++) {
                Xml.ArcLayer al = a.arcLayers.get(i);
                long gridLayerExtend = DBMath.lambdaToGrid(al.extend.value);
                minGridExtend = Math.min(minGridExtend, gridLayerExtend);
                maxGridExtend = Math.max(maxGridExtend, gridLayerExtend);
            }
            if (maxGridExtend < 0 || maxGridExtend > Integer.MAX_VALUE/8) {
                System.out.println("ArcProto " + getTechName() + ":" + a.name + " has invalid width offset " + DBMath.gridToLambda(2*maxGridExtend));
                continue;
            }
            double widthOffset = 0;
            if (!a.diskOffset.isEmpty())
                widthOffset = a.diskOffset.values().iterator().next()*2;
//            for (Map.Entry<Integer,Double> e: a.widthOffset.entrySet()) {
//                if (e.getKey() <= techVersion)
//                    widthOffset = e.getValue();
//            }
            for (int i = 0; i < arcLayers.length; i++) {
                Xml.ArcLayer al = a.arcLayers.get(i);
                long gridLayerExtend = DBMath.lambdaToGrid(al.extend.value);
                double layerWidthOffset = DBMath.gridToLambda(DBMath.lambdaToSizeGrid(widthOffset) - 2*gridLayerExtend);
//                double layerWidthOffset = DBMath.gridToLambda(2*(maxGridExtend - gridLayerExtend));
                arcLayers[i] = new ArcLayer(layers.get(al.layer), layerWidthOffset, al.style);
            }
            assert minGridExtend >= 0 && minGridExtend == DBMath.lambdaToGrid(a.arcLayers.get(0).extend.value);
            long gridExtendOverMin = DBMath.lambdaToGrid(0.5*a.defaultWidth.value);
            ArcProto ap = new ArcProto(this, a.name, DBMath.lambdaToSizeGrid(widthOffset)/2, minGridExtend, gridExtendOverMin, a.function, arcLayers, arcs.size());
//            ArcProto ap = new ArcProto(this, a.name, (int)maxGridExtend, (int)minGridExtend, defaultWidth, a.function, arcLayers, arcs.size());
            addArcProto(ap);

            if (a.oldName != null)
                oldArcNames.put(a.oldName, ap);
            arcs.put(a.name, ap);
            if (a.wipable)
                ap.setWipable();
            if (a.curvable)
                ap.setCurvable();
            if (a.special)
                ap.setSpecialArc();
            if (a.notUsed)
                ap.setNotUsed(true);
            if (a.skipSizeInPalette)
                ap.setSkipSizeInPalette();
            ap.setFactoryExtended(a.extended);
            ap.setFactoryFixedAngle(a.fixedAngle);
            ap.setFactoryAngleIncrement(a.angleIncrement);
            ERC.getERCTool().setAntennaRatio(ap, a.antennaRatio);
        }
        setNoNegatedArcs();
        for (Xml.PrimitiveNode n: t.nodes) {
            EPoint correction = EPoint.ORIGIN;
            if (!n.diskOffset.isEmpty())
                correction = n.diskOffset.values().iterator().next();
            boolean needElectricalLayers = false;
            ArrayList<NodeLayer> nodeLayers = new ArrayList<NodeLayer>();
            ArrayList<NodeLayer> electricalNodeLayers = new ArrayList<NodeLayer>();
            for (int i = 0; i < n.nodeLayers.size(); i++) {
                Xml.NodeLayer nl = n.nodeLayers.get(i);
                TechPoint[] techPoints;
                if (nl.representation == NodeLayer.BOX || nl.representation == NodeLayer.MULTICUTBOX) {
                    techPoints = new TechPoint[2];
                    if (nl.lx.value > nl.hx.value || nl.lx.k > nl.hx.k || nl.ly.value > nl.hy.value || nl.ly.k > nl.hy.k)
                        System.out.println("Strange polygon in " + getTechName() + ":" + n.name);
                    techPoints[0] = makeTechPoint(nl.lx, nl.ly, correction);
                    techPoints[1] = makeTechPoint(nl.hx, nl.hy, correction);
                } else {
                    techPoints = nl.techPoints.toArray(new TechPoint[nl.techPoints.size()]);
                    for (int j = 0; j < techPoints.length; j++)
                        techPoints[j] = makeTechPoint(techPoints[j], correction);
                }
                NodeLayer nodeLayer;
                Layer layer = layers.get(nl.layer);
                if (n.shrinkArcs) {
                    if (layer.getPseudoLayer() == null)
                        layer.makePseudo();
                    layer = layer.getPseudoLayer();
                }
                if (nl.representation == NodeLayer.MULTICUTBOX)
                    nodeLayer = NodeLayer.makeMulticut(layer, nl.portNum, nl.style, techPoints, nl.sizex, nl.sizey, nl.sep1d, nl.sep2d);
                else if (n.specialType == PrimitiveNode.SERPTRANS)
                    nodeLayer = new NodeLayer(layer, nl.portNum, nl.style, nl.representation, techPoints, nl.lWidth, nl.rWidth, nl.tExtent, nl.bExtent);
                else
                    nodeLayer = new NodeLayer(layer, nl.portNum, nl.style, nl.representation, techPoints);
                if (!(nl.inLayers && nl.inElectricalLayers))
                    needElectricalLayers = true;
                if (nl.inLayers)
                    nodeLayers.add(nodeLayer);
                if (nl.inElectricalLayers)
                    electricalNodeLayers.add(nodeLayer);
            }
            PrimitiveNode pnp = PrimitiveNode.newInstance(n.name, this, EPoint.fromGrid(-correction.getGridX(), -correction.getGridY()),
                    DBMath.round(n.defaultWidth.value + 2*correction.getLambdaX()), DBMath.round(n.defaultHeight.value + 2*correction.getLambdaY()), n.sizeOffset,
                    nodeLayers.toArray(new NodeLayer[nodeLayers.size()]));
            if (n.oldName != null)
                oldNodeNames.put(n.oldName, pnp);
            pnp.setFunction(n.function);
            if (needElectricalLayers)
                pnp.setElectricalLayers(electricalNodeLayers.toArray(new NodeLayer[electricalNodeLayers.size()]));
            if (n.shrinkArcs) {
                pnp.setArcsWipe();
                pnp.setArcsShrink();
            }
            if (n.square)
                pnp.setSquare();
            if (n.canBeZeroSize)
                pnp.setCanBeZeroSize();
            if (n.wipes)
                pnp.setWipeOn1or2();
            if (n.lockable)
                pnp.setLockedPrim();
            if (n.edgeSelect)
                pnp.setEdgeSelect();
            if (n.skipSizeInPalette)
                pnp.setSkipSizeInPalette();
            if (n.notUsed)
                pnp.setNotUsed(true);
            if (n.lowVt)
                pnp.setNodeBit(PrimitiveNode.LOWVTBIT);
            if (n.highVt)
                pnp.setNodeBit(PrimitiveNode.HIGHVTBIT);
            if (n.nativeBit)
                pnp.setNodeBit(PrimitiveNode.NATIVEBIT);
            if (n.od18)
                pnp.setNodeBit(PrimitiveNode.OD18BIT);
            if (n.od25)
                pnp.setNodeBit(PrimitiveNode.OD25BIT);
            if (n.od33)
                pnp.setNodeBit(PrimitiveNode.OD33BIT);

            PrimitivePort[] ports = new PrimitivePort[n.ports.size()];
            for (int i = 0; i < ports.length; i++) {
                Xml.PrimitivePort p = n.ports.get(i);
//                TechPoint p0 = makeTechPoint(p.p0, correction);
//                TechPoint p1 = makeTechPoint(p.p1, correction);
                ArcProto[] connections = new ArcProto[p.portArcs.size()];
                for (int j = 0; j < connections.length; j++)
                    connections[j] = arcs.get(p.portArcs.get(j));
                if (p.lx.value > p.hx.value || p.lx.k > p.hx.k || p.ly.value > p.hy.value || p.ly.k > p.hy.k)
                    System.out.println("Strange polygon in " + getTechName() + ":" + n.name + ":" + p.name);
                ports[i] = PrimitivePort.newInstance(this, pnp, connections, p.name, p.portAngle, p.portRange, p.portTopology,
                        PortCharacteristic.UNKNOWN, makeEdgeH(p.lx, correction), makeEdgeV(p.ly, correction), makeEdgeH(p.hx, correction), makeEdgeV(p.hy, correction));
//                        PortCharacteristic.UNKNOWN, p0.getX(), p0.getY(), p1.getX(), p1.getY());
            }
            pnp.addPrimitivePorts(ports);
            pnp.setSpecialType(n.specialType);
            switch (n.specialType) {
                case com.sun.electric.technology.PrimitiveNode.POLYGONAL:
					pnp.setHoldsOutline();
                    break;
                case com.sun.electric.technology.PrimitiveNode.SERPTRANS:
					pnp.setHoldsOutline();
                    pnp.setCanShrink();
                    pnp.setSpecialValues(n.specialValues);
                    break;
                default:
                    break;
            }
            if (n.function == PrimitiveNode.Function.NODE) {
                assert pnp.getLayers().length == 1;
                Layer layer = pnp.getLayers()[0].getLayer();
                assert layer.getPureLayerNode() == null;
                layer.setPureLayerNode(pnp);
            }
            if (n.nodeSizeRule != null)
                pnp.setMinSize(n.nodeSizeRule.getWidth(), n.nodeSizeRule.getHeight(), n.nodeSizeRule.getRuleName());
        }
        for (Xml.Layer l: t.layers) {
            if (l.pureLayerNode == null) continue;
            Layer layer = layers.get(l.name);
            ArcProto[] connections = new ArcProto[l.pureLayerNode.portArcs.size()];
            for (int j = 0; j < connections.length; j++)
                connections[j] = arcs.get(l.pureLayerNode.portArcs.get(j));
            PrimitiveNode pn = layer.makePureLayerNode(l.pureLayerNode.name, l.pureLayerNode.size.value, l.pureLayerNode.style, l.pureLayerNode.port, connections);
            if (l.pureLayerNode.oldName != null)
                oldNodeNames.put(l.pureLayerNode.oldName, pn);
        }
        if (t.menuPalette != null) {
            int numColumns = t.menuPalette.numColumns;
            ArrayList<Object[]> rows = new ArrayList<Object[]>();
            Object[] row = null;
            for (int i = 0; i < t.menuPalette.menuBoxes.size(); i++) {
                int column = i % numColumns;
                if (column == 0) {
                    row = new Object[numColumns];
                    rows.add(row);
                }
                ArrayList<Object> menuBoxList = t.menuPalette.menuBoxes.get(i);
                if (menuBoxList == null || menuBoxList.isEmpty()) continue;
                if (menuBoxList.size() == 1) {
                    row[column] = convertMenuItem(menuBoxList.get(0));
                } else {
                    ArrayList<Object> list = new ArrayList<Object>();
                    for (Object o: menuBoxList)
                        list.add(convertMenuItem(o));
                    row[column] = list;
                }
            }
            nodeGroups = rows.toArray(new Object[rows.size()][]);
        }
        for (Xml.SpiceHeader h: t.spiceHeaders) {
            String[] spiceLines = h.spiceLines.toArray(new String[h.spiceLines.size()]);
            switch (h.level) {
                case 1:
                    setSpiceHeaderLevel1(spiceLines);
                    break;
                case 2:
                    setSpiceHeaderLevel2(spiceLines);
                    break;
                case 3:
                    setSpiceHeaderLevel3(spiceLines);
                    break;
            }
        }
        for (Xml.Foundry f: t.foundries) {
            ArrayList<String> gdsLayers = new ArrayList<String>();
            for (Layer layer: this.layers) {
                String gds = f.layerGds.get(layer.getName());
                if (gds == null) continue;
                gdsLayers.add(layer.getName() + " " + gds);
            }
            Foundry foundry = new Foundry(this, Foundry.Type.valueOf(f.name), f.rules, gdsLayers.toArray(new String[gdsLayers.size()]));
            foundries.add(foundry);
        }
    }

    private TechPoint makeTechPoint(TechPoint p, EPoint correction) {
        EdgeH h = p.getX();
        EdgeV v = p.getY();
        h = new EdgeH(h.getMultiplier(), h.getAdder() - correction.getLambdaX()*h.getMultiplier()*2);
        v = new EdgeV(v.getMultiplier(), v.getAdder() - correction.getLambdaY()*v.getMultiplier()*2);
        return new TechPoint(h, v);
    }

    private TechPoint makeTechPoint(Xml.Distance x, Xml.Distance y, EPoint correction) {
        return new TechPoint(makeEdgeH(x, correction), makeEdgeV(y, correction));
    }

    private EdgeH makeEdgeH(Xml.Distance x, EPoint correction) {
        return new EdgeH(x.k*0.5, x.value - correction.getLambdaX()*x.k);
    }

    private EdgeV makeEdgeV(Xml.Distance y, EPoint correction) {
        return new EdgeV(y.k*0.5, y.value - correction.getLambdaY()*y.k);
    }

    private Object convertMenuItem(Object menuItem) {
        if (menuItem instanceof Xml.ArcProto)
            return findArcProto(((Xml.ArcProto)menuItem).name);
        if (menuItem instanceof Xml.PrimitiveNode)
            return findNodeProto(((Xml.PrimitiveNode)menuItem).name);
        if (menuItem instanceof Xml.MenuNodeInst) {
            Xml.MenuNodeInst n = (Xml.MenuNodeInst)menuItem;
            return makeNodeInst(findNodeProto(n.protoName), n.function, 0, true, n.text, n.fontSize);
        }
        return menuItem.toString();
    }

    public Xml.Technology getXmlTech() { return xmlTech; }

    protected void resizeXml(XMLRules rules) {
//        for (Xml.ArcProto xap: xmlTech.arcs) {
//            ArcProto ap = findArcProto(xap.name);
//            assert xap.arcLayers.size() == ap.layers.length;
//            Double widthOffsetObject = xap.widthOffset.get(Integer.valueOf(0))*2;
//            double widthOffset = widthOffsetObject != null ? widthOffsetObject.doubleValue() : 0;
//            for (int i = 0; i < ap.layers.length; i++) {
//                Xml.ArcLayer xal = xap.arcLayers.get(i);
//                double layerWidthOffset = widthOffset - 2*xal.extend.value;
//                ap.layers[i] = ap.layers[i].withGridOffset(DBMath.lambdaToSizeGrid(layerWidthOffset));
//            }
//            ap.computeLayerGridExtendRange();
//        }
    }

	/**
	 * This is called once, at the start of Electric, to initialize the technologies.
	 * Because of Java's "lazy evaluation", the only way to force the technology constructors to fire
	 * and build a proper list of technologies, is to call each class.
	 * So, each technology is listed here.  If a new technology is created, this must be added to this list.
	 */
	public static void initAllTechnologies()
	{
		// technology initialization may set preferences, so batch them
		Pref.delayPrefFlushing();

		// Because of lazy evaluation, technologies aren't initialized unless they're referenced here
		Generic.tech.setup();
		Artwork.tech.setup();
		FPGA.tech.setup();
		Schematics.tech.setup();

//		MoCMOS.tech.setup();

		// finished batching preferences
		Pref.resumePrefFlushing();

		// setup the generic technology to handle all connections
		Generic.tech.makeUnivList();

        lazyUrls.put("bicmos",       Technology.class.getResource("technologies/bicmos.xml"));
        lazyUrls.put("bipolar",      Technology.class.getResource("technologies/bipolar.xml"));
        lazyUrls.put("cmos",         Technology.class.getResource("technologies/cmos.xml"));
        lazyClasses.put("efido",     "com.sun.electric.technology.technologies.EFIDO");
        lazyClasses.put("gem",       "com.sun.electric.technology.technologies.GEM");
        lazyClasses.put("pcb",       "com.sun.electric.technology.technologies.PCB");
        lazyClasses.put("rcmos",     "com.sun.electric.technology.technologies.RCMOS");
        if (true) {
            lazyClasses.put("mocmos","com.sun.electric.technology.technologies.MoCMOS");
        } else {
            lazyUrls.put("mocmos",   Technology.class.getResource("technologies/mocmos.xml"));
        }
        lazyUrls.put("mocmosold",    Technology.class.getResource("technologies/mocmosold.xml"));
        lazyUrls.put("mocmossub",    Technology.class.getResource("technologies/mocmossub.xml"));
        lazyUrls.put("nmos",         Technology.class.getResource("technologies/nmos.xml"));
        lazyUrls.put("tsmc180",      Main.class.getResource("plugins/tsmc/tsmc180.xml"));
        if (true) {
            lazyClasses.put("cmos90","com.sun.electric.plugins.tsmc.CMOS90");
        } else {
            lazyUrls.put("cmos90",   Main.class.getResource("plugins/tsmc/cmos90.xml"));
        }

        if (!LAZY_TECHNOLOGIES) {
            // initialize technologies that may not be present
            for(String techClassName: lazyClasses.values()) {
                setupTechnology(techClassName);
            }
            for(URL techUrl: lazyUrls.values()) {
                if (techUrl != null)
                    setupTechnology(techUrl);
            }
        }

		// set the current technology, given priority to user defined
        curLayoutTech = getMocmosTechnology();
        Technology  tech = Technology.findTechnology(User.getDefaultTechnology());
        if (tech == null) tech = curLayoutTech;
        tech.setCurrent();

	}

    private static void setupTechnology(String techClassName) {
        Pref.delayPrefFlushing();
        try {
            Class<?> techClass = Class.forName(techClassName);
            Constructor[] constructors = techClass.getConstructors();
            Technology tech = null;
            if (constructors.length != 0)
                tech = (Technology)techClass.getConstructor().newInstance();
            else
                tech = (Technology)techClass.getField("tech").get(null);
            tech.setup();
            Generic.tech.makeUnivList();
        } catch (ClassNotFoundException e) {
            if (Job.getDebug())
                System.out.println("GNU Release can't find extra technologies");

        } catch (Exception e) {
            System.out.println("Exceptions while importing extra technologies");
            ActivityLogger.logException(e);
        } finally {
            Pref.resumePrefFlushing();
        }
    }

    private static void setupTechnology(URL urlXml) {
        Pref.delayPrefFlushing();
        try {
            Xml.Technology t = Xml.parseTechnology(urlXml);
            if (t == null)
            {
                if (Job.getDebug())
                    System.out.println("Can't find extra technology: " + urlXml.getFile());
            }
            Class<?> techClass = Technology.class;
            if (t.className != null)
                techClass = Class.forName(t.className);
            Technology tech = (Technology)techClass.getConstructor(Xml.Technology.class).newInstance(t);
            tech.setup();
            Generic.tech.makeUnivList();
        } catch (ClassNotFoundException e) {
            if (Job.getDebug())
                System.out.println("GNU Release can't find extra technologies");

        } catch (Exception e) {
            System.out.println("Exceptions while importing extra technologies");
            ActivityLogger.logException(e);
        } finally {
            Pref.resumePrefFlushing();
        }
    }

    private static Technology mocmos = null;
    private static boolean mocmosCached = false;
    /**
     * Method to return the MOSIS CMOS technology.
     * @return the MOSIS CMOS technology object.
     */
    public static Technology getMocmosTechnology() {
        if (!mocmosCached) {
            mocmosCached = true;
            mocmos = findTechnology("mocmos");
        }
        return mocmos;
    }

    private static Technology tsmc180 = null;
    private static boolean tsmc180Cached = false;
	/**
	 * Method to return the TSMC 180 nanometer technology.
	 * Since the technology is a "plugin" and not distributed universally, it may not exist.
	 * @return the TSMC180 technology object (null if it does not exist).
	 */
    public static Technology getTSMC180Technology() {
    	if (!tsmc180Cached) {
            tsmc180Cached = true;
            tsmc180 = findTechnology("tsmc180");
        }
 		return tsmc180;
    }

    private static Technology cmos90 = null;
    private static boolean cmos90Cached = false;
	/**
	 * Method to return the CMOS 90 nanometer technology.
	 * Since the technology is a "plugin" and not distributed universally, it may not exist.
	 * @return the CMOS90 technology object (null if it does not exist).
	 */
    public static Technology getCMOS90Technology()
    {
    	if (!cmos90Cached) {
            cmos90Cached = true;
            cmos90 = findTechnology("cmos90");
        }
 		return cmos90;
    }

	/**
	 * Method to initialize a technology.
	 * Calls the technology's specific "init()" method (if any).
	 * Also sets up mappings from pseudo-layers to real layers.
	 */
	public void setup()
	{
		// do any specific intialization
		init();

        if (cacheMinResistance == null || cacheMinCapacitance == null) {
            setFactoryParasitics(10, 0);
        }
        if (cacheGateCapacitance == null || cacheWireRatio == null || cacheDiffAlpha == null) {
            setFactoryLESettings(DEFAULT_GATECAP, DEFAULT_WIRERATIO, DEFAULT_DIFFALPHA);
        }
        layersAllocationLocked = true;
        for (Foundry foundry: foundries) {
            foundry.finish();
        }
        for (Layer layer: layers) {
            if (!layer.isPseudoLayer())
                layer.finish();
        }

        check();
	}

	/**
	 * Method to set state of a technology.
	 * It gets overridden by individual technologies.
     */
	public void setState() {}

    protected void setNotUsed(int numPolys) {
        int numMetals = getNumMetals();
        for (PrimitiveNode pn: nodes.values()) {
            boolean isUsed = true;
            for (NodeLayer nl: pn.getLayers())
                isUsed = isUsed && nl.getLayer().getFunction().isUsed(numMetals, numPolys);
            pn.setNotUsed(!isUsed);
        }
        for (ArcProto ap: arcs.values()) {
            boolean isUsed = true;
            for (ArcLayer al: ap.layers)
                isUsed = isUsed && al.getLayer().getFunction().isUsed(numMetals, numPolys);
            ap.setNotUsed(!isUsed);
        }
    }

	/**
	 * Method to initialize a technology. This will check and restore
	 * default values stored as preferences
	 */
	public void init()
	{
//		// remember the arc widths as specified by previous defaults
//		HashMap<ArcProto,Double> arcWidths = new HashMap<ArcProto,Double>();
//		for(Iterator<ArcProto> it = getArcs(); it.hasNext(); )
//		{
//			ArcProto ap = it.next();
//			double width = ap.getDefaultLambdaBaseWidth();
////			double width = ap.getDefaultLambdaFullWidth();
//			arcWidths.put(ap, width); // autoboxing
//		}
//
//		// remember the node sizes as specified by previous defaults
//		HashMap<PrimitiveNode,Point2D.Double> nodeSizes = new HashMap<PrimitiveNode,Point2D.Double>();
//		for(Iterator<PrimitiveNode> it = getNodes(); it.hasNext(); )
//		{
//			PrimitiveNode np = it.next();
//			double width = np.getDefWidth();
//			double height = np.getDefHeight();
//			nodeSizes.put(np, new Point2D.Double(width, height));
//		}

		// initialize all design rules in the technology (overwrites arc widths)
		setState();

//		// now restore arc width defaults if they are wider than what is set
//		for(Iterator<ArcProto> it = getArcs(); it.hasNext(); )
//		{
//			ArcProto ap = it.next();
//			Double origWidth = arcWidths.get(ap);
//			if (origWidth == null) continue;
//			double width = ap.getDefaultLambdaBaseWidth();
//			if (origWidth > width) ap.setDefaultLambdaBaseWidth(origWidth); //autoboxing
////			double width = ap.getDefaultLambdaFullWidth();
////			if (origWidth > width) ap.setDefaultLambdaFullWidth(origWidth); //autoboxing
//		}
//
//		// now restore node size defaults if they are larger than what is set
//		for(Iterator<PrimitiveNode> it = getNodes(); it.hasNext(); )
//		{
//			PrimitiveNode np = it.next();
//			Point2D size = nodeSizes.get(np);
//			if (size == null) continue;
//			double width = np.getDefWidth();
//			double height = np.getDefHeight();
//			if (size.getX() > width || size.getY() > height)
//                np.setDefSize(size.getX(), size.getY());
//		}
	}

	/**
	 * Returns the current Technology.
	 * @return the current Technology.
	 * The current technology is maintained by the system as a default
	 * in situations where a technology cannot be determined.
	 */
	public static Technology getCurrent() { return curTech; }

	/**
	 * Set this to be the current Technology
	 * The current technology is maintained by the system as a default
	 * in situations where a technology cannot be determined.
	 */
	public void setCurrent()
	{
		curTech = this;
		if (this != Generic.tech && this != Schematics.tech && this != Artwork.tech)
			curLayoutTech = this;
	}

//	/**
//	 * Returns the total number of Technologies currently in Electric.
//	 * @return the total number of Technologies currently in Electric.
//	 */
//	public static int getNumTechnologies()
//	{
//		return technologies.size();
//	}

	/**
	 * Find the Technology with a particular name.
	 * @param name the name of the desired Technology
	 * @return the Technology with the same name, or null if no
	 * Technology matches.
	 */
	public static Technology findTechnology(String name)
	{
		if (name == null) return null;
		Technology tech = technologies.get(name);
		if (tech != null) return tech;

		for (Iterator<Technology> it = getTechnologies(); it.hasNext(); )
		{
			Technology t = it.next();
			if (t.techName.equalsIgnoreCase(name))
				return t;
		}
		return null;
	}

	/**
	 * Get an iterator over all of the Technologies.
	 * @return an iterator over all of the Technologies.
	 */
	public static Iterator<Technology> getTechnologies()
	{
		return technologies.values().iterator();
	}

	/**
	 * Method to convert any old-style variable information to the new options.
	 * May be overrideen in subclasses.
	 * @param varName name of variable
	 * @param value value of variable
	 * @return map from project settings to sitting values if variable was converted
	 */
	public Map<Setting,Object> convertOldVariable(String varName, Object value)
	{
		return null;
	}

    /**
     * Method to clean libraries with unused primitive nodes.
     * May be overridden in technologies. By default it does nothing
     * @param ni NodeInst node to analyze
     * @param list nodes that will be removed in a remove job.
     * @return true if node is not in used
     */
    public boolean cleanUnusedNodesInLibrary(NodeInst ni, List<Geometric> list) {return false;}

    public void dump(PrintWriter out) {
        final String[] techBits = {
            "NONELECTRICAL", "NODIRECTIONALARCS", "NONEGATEDARCS",
            "NONSTANDARD", "STATICTECHNOLOGY", "NOPRIMTECHNOLOGY"
        };
        final String[] layerBits = {
            null, null, null,
            null, null, null,
            "PTYPE", "NTYPE", "DEPLETION",
            "ENHANCEMENT",  "LIGHT", "HEAVY",
            null, "NONELEC", "CONMETAL",
            "CONPOLY", "CONDIFF", null,
            null, null, null,
            "HLVT", "INTRANS", "THICK"
        };

        out.println("Technology " + getTechName());
        out.println(getClass().toString());
        out.println("shortName=" + getTechShortName());
        out.println("techDesc=" + getTechDesc());
        out.print("Bits: "); printlnBits(out, techBits, userBits);
        out.print("isScaleRelevant=" + isScaleRelevant()); printlnSetting(out, getScaleSetting());
        printlnSetting(out, getPrefFoundrySetting());
        printlnSetting(out, getNumMetalsSetting());
        dumpExtraProjectSettings(out);
        printlnSetting(out, getMinResistanceSetting());
        printlnSetting(out, getGateLengthSubtractionSetting());
        printlnSetting(out, getGateIncludedSetting());
        printlnSetting(out, getGroundNetIncludedSetting());
        printlnSetting(out, getMaxSeriesResistanceSetting());
        printlnSetting(out, getGateCapacitanceSetting());
        printlnSetting(out, getWireRatioSetting());
        printlnSetting(out, getDiffAlphaSetting());

        printlnPref(out, 0, prefResolution);
        assert getNumTransparentLayers() == (transparentColorPrefs != null ? transparentColorPrefs.length : 0);
        for (int i = 0; i < getNumTransparentLayers(); i++)
            out.println("TRANSPARENT_" + (i+1) + "=" + Integer.toHexString(transparentColorPrefs[i].getIntFactoryValue()));

        for (Layer layer: layers) {
            if (layer.isPseudoLayer()) continue;
            out.print("Layer " + layer.getName() + " " + layer.getFunction().name());
            printlnBits(out, layerBits, layer.getFunctionExtras());
            out.print("\t"); printlnSetting(out, layer.getCIFLayerSetting());
            out.print("\t"); printlnSetting(out, layer.getDXFLayerSetting());
            out.print("\t"); printlnSetting(out, layer.getSkillLayerSetting());
            out.print("\t"); printlnSetting(out, layer.getResistanceSetting());
            out.print("\t"); printlnSetting(out, layer.getCapacitanceSetting());
            out.print("\t"); printlnSetting(out, layer.getEdgeCapacitanceSetting());
            // GDS
            EGraphics desc = layer.getGraphics();
            out.println("\tpatternedOnDisplay=" + desc.isPatternedOnDisplay() + "(" + desc.isFactoryPatternedOnDisplay() + ")");
            out.println("\tpatternedOnPrinter=" + desc.isPatternedOnPrinter() + "(" + desc.isFactoryPatternedOnPrinter() + ")");
            out.println("\toutlined=" + desc.getOutlined() + "(" + desc.getFactoryOutlined() + ")");
            out.println("\ttransparent=" + desc.getTransparentLayer() + "(" + desc.getFactoryTransparentLayer() + ")");
            out.println("\tcolor=" + Integer.toHexString(desc.getColor().getRGB()) + "(" + Integer.toHexString(desc.getFactoryColor()) + ")");
            out.println("\topacity=" + desc.getOpacity() + "(" + desc.getFactoryOpacity() + ")");
            out.println("\tforeground=" + desc.getForeground());
            int pattern[] = desc.getFactoryPattern();
            out.print("\tpattern");
            for (int p: pattern)
                out.print(" " + Integer.toHexString(p));
            out.println();
            out.println("\tdistance3D=" + layer.getDistance());
            out.println("\tthickness3D=" + layer.getThickness());
            out.println("\tmode3D=" + layer.getTransparencyMode());
            out.println("\tfactor3D=" + layer.getTransparencyFactor());

            if (layer.getPseudoLayer() != null)
                out.println("\tpseudoLayer=" + layer.getPseudoLayer().getName());
        }
        for (ArcProto ap: arcs.values())
            ap.dump(out);
        if (!oldArcNames.isEmpty()) {
            out.println("OldArcNames:");
            for (Map.Entry<String,ArcProto> e: getOldArcNames().entrySet())
                out.println("\t" + e.getKey() + " --> " + e.getValue().getFullName());
        }
        for (PrimitiveNode pnp: nodes.values())
            pnp.dump(out);
        if (!oldNodeNames.isEmpty()) {
            out.println("OldNodeNames:");
            for (Map.Entry<String,PrimitiveNode> e: getOldNodeNames().entrySet())
                out.println("\t" + e.getKey() + " --> " + e.getValue().getFullName());
        }
        for (Foundry foundry: foundries) {
            out.println("Foundry " + foundry.getType());
            for (Layer layer: layers) {
                if (layer.isPseudoLayer()) continue;
                Setting setting = foundry.getGDSLayerSetting(layer);
                out.print("\t"); printlnSetting(out,setting);
            }
        }

        printSpiceHeader(out, 1, getSpiceHeaderLevel1());
        printSpiceHeader(out, 2, getSpiceHeaderLevel2());
        printSpiceHeader(out, 3, getSpiceHeaderLevel3());

        if (nodeGroups != null) {
            for (int i = 0; i < nodeGroups.length; i++) {
                Object[] nodeLine = nodeGroups[i];
                for (int j = 0; j < nodeLine.length; j++) {
                    Object entry = nodeLine[j];
                    if (entry == null) continue;
                    out.print(" menu " + i + " " + j);
                    if (entry instanceof List) {
                        List<?> list = (List<?>)entry;
                        for (Object o: list)
                            printMenuEntry(out, o);
                    } else {
                        printMenuEntry(out, entry);
                    }
                    out.println();
                }
            }
        }

        for (Iterator<Foundry> it = getFoundries(); it.hasNext();) {
            Foundry foundry = it.next();
            out.println("    <Foundry name=\"" + foundry.getType().name() + "\">");
            for (Map.Entry<Layer,String> e: foundry.getGDSLayers().entrySet())
                out.println("        <layerGds layer=\"" + e.getKey().getName() + "\" gds=\"" + e.getValue() + "\"/>");
            List<DRCTemplate> rules = foundry.getRules();
            if (rules != null) {
                for (DRCTemplate rule: rules)
                    DRCTemplate.exportDRCRule(out, rule);
            }
            out.println("    </Foundry>");
        }
    }

    protected void dumpExtraProjectSettings(PrintWriter out) {}

    protected static void printlnSetting(PrintWriter out, Setting setting) {
        out.println(setting.getXmlPath() + "=" + setting.getValue() + "(" + setting.getFactoryValue() + ")");
    }

    static void printlnPref(PrintWriter out, int indent, Pref pref) {
        if (pref == null) return;
        while (indent-- > 0)
            out.print("\t");
        out.println(pref.getPrefName() + "=" + pref.getValue() + "(" + pref.getFactoryValue() + ")");
    }

    private static void printMenuEntry(PrintWriter out, Object entry) {
        if (entry instanceof ArcProto) {
            out.print(" arc " + ((ArcProto)entry).getName());
        } else if (entry instanceof PrimitiveNode) {
            out.print(" node " + ((PrimitiveNode)entry).getName());
        } else if (entry instanceof NodeInst) {
            NodeInst ni = (NodeInst)entry;
            PrimitiveNode pn = (PrimitiveNode)ni.getProto();
            out.print(" nodeInst " + pn.getName() + ":" + ni.getFunction() + ":" + ni.getOrient());
            for (Iterator<Variable> it = ni.getVariables(); it.hasNext(); ) {
                Variable var = it.next();
                out.print(":" + var.getObject()+ ":" + var.isDisplay() + ":" + var.getSize().getSize());
            }
        } else if (entry instanceof String) {
            out.print(" " + entry);
        } else {
            assert false;
        }
    }

    protected static void printlnBits(PrintWriter out, String[] bitNames, int bits) {
        for (int i = 0; i < Integer.SIZE; i++) {
            if ((bits & (1 << i)) == 0) continue;
            String bitName = i < bitNames.length ? bitNames[i] : null;
            if (bitName == null)
                bitName = "BIT" + i;
            out.print(" " + bitName);
        }
        out.println();
    }

    private void printSpiceHeader(PrintWriter out, int level, String[] header) {
        if (header == null) return;
        out.println("SpiceHeader " + level);
        for (String s: header)
            out.println("\t\"" + s + "\"");
    }
//	/** Cached rules for the technology. */		            protected DRCRules cachedRules = null;
//    /** old-style DRC rules. */                             protected double[] conDist, unConDist;
//    /** Xml representation of this Technology */            protected Xml.Technology xmlTech;

    /****************************** LAYERS ******************************/

	/**
	 * Returns an Iterator on the Layers in this Technology.
	 * @return an Iterator on the Layers in this Technology.
	 */
	public Iterator<Layer> getLayers()
	{
        layersAllocationLocked = true;
		return layers.iterator();
	}

	/**
	 * Returns a specific Layer number in this Technology.
	 * @param index the index of the desired Layer.
	 * @return the indexed Layer in this Technology.
	 */
	public Layer getLayer(int index)
	{
		return layers.get(index);
	}

	/**
	 * Returns the number of Layers in this Technology.
	 * @return the number of Layers in this Technology.
	 */
	public int getNumLayers()
	{
        layersAllocationLocked = true;
		return layers.size();
	}

	/**
	 * Method to find a Layer with a given name.
	 * @param layerName the name of the desired Layer.
	 * @return the Layer with that name (null if none found).
	 */
	public Layer findLayer(String layerName)
	{
		for(Iterator<Layer> it = getLayers(); it.hasNext(); )
		{
			Layer layer = it.next();
			if (layer.getName().equalsIgnoreCase(layerName)) return layer;
		}
		for(Iterator<Layer> it = getLayers(); it.hasNext(); )
		{
			Layer layer = it.next().getPseudoLayer();
            if (layer == null) continue;
			if (layer.getName().equalsIgnoreCase(layerName)) return layer;
		}
		return null;
	}

    /**
	 * Method to determine the index in the upper-left triangle array for two layers/nodes.
	 * @param index1 the first layer/node index.
	 * @param index2 the second layer/node index.
	 * @return the index in the array that corresponds to these two layers/nodes.
	 */
//	public int getRuleIndex(int index1, int index2)
//	{
//		if (index1 > index2) { int temp = index1; index1 = index2;  index2 = temp; }
//		int pIndex = (index1+1) * (index1/2) + (index1&1) * ((index1+1)/2);
//		pIndex = index2 + (getNumLayers()) * index1 - pIndex;
//		return getNumLayers() + getNumNodes() + pIndex;
//	}

    /**
     * Method to determine index of layer or node involved in the rule
     * @param name name of the layer or node
     * @return the index of the rule.
     */
    public int getRuleNodeIndex(String name)
    {
        // Checking if node is found
        // Be careful because iterator might change over time?
        int count = 0;
        for (Iterator<PrimitiveNode> it = getNodes(); it.hasNext(); count++)
        {
            PrimitiveNode pn = it.next();
            if (pn.getName().equalsIgnoreCase(name))
                return (getNumLayers() + count);   // it should use get
        }
        return -1;
    }

    public static Layer getLayerFromOverride(String override, int startPos, char endChr, Technology tech)
    {
        int endPos = override.indexOf(endChr, startPos);
        if (endPos < 0) return null;
        String layerName = override.substring(startPos, endPos);
        return tech.findLayer(layerName);
    }

	/**
	 * Method to find the Layer in this Technology that matches a function description.
	 * @param fun the layer function to locate.
	 * @return the Layer that matches this description (null if not found).
	 */
	public Layer findLayerFromFunction(Layer.Function fun)
	{
		for(Iterator<Layer> it = this.getLayers(); it.hasNext(); )
		{
			Layer lay = it.next();
			Layer.Function lFun = lay.getFunction();
			if (lFun == fun) return lay;
		}
		return null;
	}

	/**
	 * Method to add a new Layer to this Technology.
	 * This is usually done during initialization.
	 * @param layer the Layer to be added to this Technology.
	 */
	public void addLayer(Layer layer)
	{
        if (layersAllocationLocked)
            throw new IllegalStateException("layers allocation is locked");
        layer.setIndex(layers.size());
        layers.add(layer);
	}

	/**
	 * Method to tell whether two layers should be considered equivalent for the purposes of cropping.
	 * The method is overridden by individual technologies to provide specific answers.
	 * @param layer1 the first Layer.
	 * @param layer2 the second Layer.
	 * @return true if the layers are equivalent.
	 */
	public boolean sameLayer(Layer layer1, Layer layer2)
	{
		if (layer1 == layer2) return true;
		if (layer1.getFunction() == Layer.Function.POLY1 && layer2.getFunction() == Layer.Function.GATE) return true;
		if (layer2.getFunction() == Layer.Function.POLY1 && layer1.getFunction() == Layer.Function.GATE) return true;
		return false;
	}

	/**
	 * Method to make a sorted list of layers in this Technology.
	 * The list is sorted by depth (from bottom to top).
	 * @return a sorted list of Layers in this Technology.
	 */
	public List<Layer> getLayersSortedByHeight()
	{
		// determine order of overlappable layers in current technology
		List<Layer> layerList = new ArrayList<Layer>();
		for(Iterator<Layer> it = getLayers(); it.hasNext(); )
		{
			layerList.add(it.next());
		}
		Collections.sort(layerList, LAYERS_BY_HEIGHT);
		return(layerList);
	}

    public static final LayerHeight LAYERS_BY_HEIGHT = new LayerHeight(false);
    public static final LayerHeight LAYERS_BY_HEIGHT_LIFT_CONTACTS = new LayerHeight(true);

	private static class LayerHeight implements Comparator<Layer>
	{
        final boolean liftContacts;

        private LayerHeight(boolean liftContacts) {
            this.liftContacts = liftContacts;
        }

		public int compare(Layer l1, Layer l2)
		{
            Layer.Function f1 = l1.getFunction();
            Layer.Function f2 = l2.getFunction();
			int h1 = f1.getHeight();
			int h2 = f2.getHeight();
            if (liftContacts) {
                if (f1.isContact())
                    h1++;
                else if (f1.isMetal())
                    h1--;
                if (f2.isContact())
                    h2++;
                else if (f2.isMetal())
                    h2--;
            }
            int cmp = h1 - h2;
            if (cmp != 0) return cmp;
            Technology tech1 = l1.getTechnology();
            Technology tech2 = l2.getTechnology();
            if (tech1 != tech2) {
                int techIndex1 = tech1 != null ? tech1.getIndex() : -1;
                int techIndex2 = tech2 != null ? tech2.getIndex() : -1;
                return techIndex1 - techIndex2;
            }
			return l1.getIndex() - l2.getIndex();
		}
	}

    /**
     * Dummy method overridden by implementing technologies to define
     * the number of metal layers in the technology.  Applies to layout
     * technologies.  Can by changed by user preferences.
     * @return the number of metal layers currently specified for the technology
     */
    public int getNumMetals() { return cacheNumMetalLayers.getInt(); }
	/**
	 * Returns project Setting to tell the number of metal layers in the MoCMOS technology.
	 * @return project Setting to tell the number of metal layers in the MoCMOS technology (from 2 to 6).
	 */
	public Setting getNumMetalsSetting() { return cacheNumMetalLayers; }

	/****************************** ARCS ******************************/

	/**
	 * Method to create a new ArcProto from the parameters.
	 * @param protoName the name of this ArcProto.
	 * It may not have unprintable characters, spaces, or tabs in it.
     * @param lambdaWidthOffset width offset in lambda units.
	 * @param defaultWidth the default width of this ArcProto.
	 * @param layers the Layers that make up this ArcProto.
	 * @return the newly created ArcProto.
	 */
	protected ArcProto newArcProto(String protoName, double lambdaWidthOffset, double defaultWidth, ArcProto.Function function, Technology.ArcLayer... layers)
	{
		// check the arguments
		if (findArcProto(protoName) != null)
		{
			System.out.println("Error: technology " + getTechName() + " has multiple arcs named " + protoName);
			return null;
		}
        long gridWidthOffset = DBMath.lambdaToSizeGrid(lambdaWidthOffset);
		if (gridWidthOffset < 0 || gridWidthOffset > Integer.MAX_VALUE)
		{
			System.out.println("ArcProto " + getTechName() + ":" + protoName + " has invalid width offset " + lambdaWidthOffset);
			return null;
		}
		if (defaultWidth < DBMath.gridToLambda(gridWidthOffset))
		{
			System.out.println("ArcProto " + getTechName() + ":" + protoName + " has negative width");
			return null;
		}
        long defaultGridWidth = DBMath.lambdaToSizeGrid(defaultWidth);

		ArcProto ap = new ArcProto(this, protoName, defaultGridWidth/2, (defaultGridWidth - gridWidthOffset)/2, 0, function, layers, arcs.size());
		addArcProto(ap);
		return ap;
	}

	/**
	 * Returns the ArcProto in this technology with a particular name.
	 * @param name the name of the ArcProto.
	 * @return the ArcProto in this technology with that name.
	 */
	public ArcProto findArcProto(String name)
	{
		if (name == null) return null;
		ArcProto primArc = arcs.get(name);
		if (primArc != null) return primArc;

		for (Iterator<ArcProto> it = getArcs(); it.hasNext(); )
		{
			ArcProto ap = it.next();
			if (ap.getName().equalsIgnoreCase(name))
				return ap;
		}
		return null;
	}

	/**
	 * Returns an Iterator on the ArcProto objects in this technology.
	 * @return an Iterator on the ArcProto objects in this technology.
	 */
	public Iterator<ArcProto> getArcs()
	{
		return arcs.values().iterator();
	}

	/**
	 * Returns the number of ArcProto objects in this technology.
	 * @return the number of ArcProto objects in this technology.
	 */
	public int getNumArcs()
	{
		return arcs.size();
	}

	/**
	 * Method to add a new ArcProto to this Technology.
	 * This is usually done during initialization.
	 * @param ap the ArcProto to be added to this Technology.
	 */
	public void addArcProto(ArcProto ap)
	{
		assert findArcProto(ap.getName()) == null;
		assert ap.primArcIndex == arcs.size();
		arcs.put(ap.getName(), ap);
	}

	/**
	 * Sets the technology to have no directional arcs.
	 * Users should never call this method.
	 * It is set once by the technology during initialization.
	 * Directional arcs are those with arrows on them, indicating (only graphically) the direction of flow through the arc.
	 */
	protected void setNoDirectionalArcs() { userBits |= NODIRECTIONALARCS; }

	/**
	 * Returns true if this technology does not have directional arcs.
	 * @return true if this technology does not have directional arcs.
	 * Directional arcs are those with arrows on them, indicating (only graphically) the direction of flow through the arc.
	 */
	public boolean isNoDirectionalArcs() { return (userBits & NODIRECTIONALARCS) != 0; }

	/**
	 * Sets the technology to have no negated arcs.
	 * Users should never call this method.
	 * It is set once by the technology during initialization.
	 * Negated arcs have bubbles on them to graphically indicated negation.
	 * Only Schematics and related technologies allow negated arcs.
	 */
	protected void setNoNegatedArcs() { userBits |= NONEGATEDARCS; }

	/**
	 * Returns true if this technology does not have negated arcs.
	 * @return true if this technology does not have negated arcs.
	 * Negated arcs have bubbles on them to graphically indicated negation.
	 * Only Schematics and related technologies allow negated arcs.
	 */
	public boolean isNoNegatedArcs() { return (userBits & NONEGATEDARCS) != 0; }

	/**
	 * Returns the polygons that describe arc "ai".
	 * @param ai the ArcInst that is being described.
	 * @return an array of Poly objects that describes this ArcInst graphically.
	 */
	public Poly [] getShapeOfArc(ArcInst ai)
	{
		return getShapeOfArc(ai, null);
	}

	/**
	 * Returns the polygons that describe arc "ai".
	 * @param ai the ArcInst that is being described.
	 * @param onlyTheseLayers to filter the only required layers
	 * @return an array of Poly objects that describes this ArcInst graphically.
	 */
	public Poly [] getShapeOfArc(ArcInst ai, Layer.Function.Set onlyTheseLayers) {
        Poly.Builder polyBuilder = Poly.threadLocalLambdaBuilder();
        polyBuilder.setOnlyTheseLayers(onlyTheseLayers);
        return polyBuilder.getShapeArray(ai);
    }

    /**
     * Fill the polygons that describe arc "a".
     * @param b AbstractShapeBuilder to fill polygons.
     * @param a the ImmutableArcInst that is being described.
     */
    protected void getShapeOfArc(AbstractShapeBuilder b, ImmutableArcInst a) {
        getShapeOfArc(b, a, null);
    }

    /**
     * Fill the polygons that describe arc "a".
     * @param b AbstractShapeBuilder to fill polygons.
     * @param a the ImmutableArcInst that is being described.
     * @param layerOverride the layer to use for all generated polygons (if not null).
     */
    protected void getShapeOfArc(AbstractShapeBuilder b, ImmutableArcInst a, Layer layerOverride) {
        // get information about the arc
        ArcProto ap = a.protoType;
        assert ap.getTechnology() == this;
        int numArcLayers = ap.getNumArcLayers();

        // construct the polygons that describe the basic arc
        if (!isNoNegatedArcs() && (a.isHeadNegated() || a.isTailNegated())) {
            for (int i = 0; i < numArcLayers; i++) {
                Technology.ArcLayer primLayer = ap.getArcLayer(i);
                Layer layer = primLayer.getLayer();
                if (b.onlyTheseLayers != null && !b.onlyTheseLayers.contains(layer.getFunction())) continue;
                if (layerOverride != null) layer = layerOverride;

                // remove a gap for the negating bubble
                int angle = a.getAngle();
                double gridBubbleSize = Schematics.getNegatingBubbleSize()*DBMath.GRID;
                double cosDist = DBMath.cos(angle) * gridBubbleSize;
                double sinDist = DBMath.sin(angle) * gridBubbleSize;
                if (a.isTailNegated())
                    b.pushPoint(a.tailLocation, cosDist, sinDist);
                else
                    b.pushPoint(a.tailLocation);
                if (a.isHeadNegated())
                    b.pushPoint(a.headLocation, -cosDist, -sinDist);
                else
                    b.pushPoint(a.headLocation);
                b.pushPoly(Poly.Type.OPENED, layer);
            }
        } else {
            for (int i = 0; i < numArcLayers; i++) {
                Technology.ArcLayer primLayer = ap.getArcLayer(i);
                Layer layer = primLayer.getLayer();
                if (b.onlyTheseLayers != null && !b.onlyTheseLayers.contains(layer.getFunction())) continue;
                if (layerOverride != null) layer = layerOverride;
                b.makeGridPoly(a, a.getGridFullWidth() - primLayer.getGridOffset(), primLayer.getStyle(), layer);
            }
        }

        // add an arrow to the arc description
        if (!isNoDirectionalArcs()) {
            final double lambdaArrowSize = 1.0*DBMath.GRID;
            int angle = a.getAngle();
            if (a.isBodyArrowed()) {
                b.pushPoint(a.headLocation);
                b.pushPoint(a.tailLocation);
                b.pushPoly(Poly.Type.VECTORS, Generic.tech.glyphLay);
            }
            if (a.isTailArrowed()) {
                int angleOfArrow = 3300;		// -30 degrees
                int backAngle1 = angle - angleOfArrow;
                int backAngle2 = angle + angleOfArrow;
                b.pushPoint(a.tailLocation);
                b.pushPoint(a.tailLocation, DBMath.cos(backAngle1)*lambdaArrowSize, DBMath.sin(backAngle1)*lambdaArrowSize);
                b.pushPoint(a.tailLocation);
                b.pushPoint(a.tailLocation, DBMath.cos(backAngle2)*lambdaArrowSize, DBMath.sin(backAngle2)*lambdaArrowSize);
                b.pushPoly(Poly.Type.VECTORS, Generic.tech.glyphLay);
            }
            if (a.isHeadArrowed()) {
                angle = (angle + 1800) % 3600;
                int angleOfArrow = 300;		// 30 degrees
                int backAngle1 = angle - angleOfArrow;
                int backAngle2 = angle + angleOfArrow;
                b.pushPoint(a.headLocation);
                b.pushPoint(a.headLocation, DBMath.cos(backAngle1)*lambdaArrowSize, DBMath.sin(backAngle1)*lambdaArrowSize);
                b.pushPoint(a.headLocation);
                b.pushPoint(a.headLocation, DBMath.cos(backAngle2)*lambdaArrowSize, DBMath.sin(backAngle2)*lambdaArrowSize);
                b.pushPoly(Poly.Type.VECTORS, Generic.tech.glyphLay);
            }
        }
    }

	/**
	 * Method to convert old primitive arc names to their proper ArcProtos.
	 * @param name the unknown arc name, read from an old Library.
	 * @return the proper ArcProto to use for this name.
	 */
	public ArcProto convertOldArcName(String name) {
        return oldArcNames.get(name);
    }

    public Map<String,ArcProto> getOldArcNames() { return new TreeMap<String,ArcProto>(oldArcNames); }

	/****************************** NODES ******************************/

	/**
	 * Method to return a sorted list of nodes in the technology
	 * @return a list with all nodes sorted
	 */
	public List<PrimitiveNode> getNodesSortedByName()
	{
		TreeMap<String,PrimitiveNode> sortedMap = new TreeMap<String,PrimitiveNode>(TextUtils.STRING_NUMBER_ORDER);
		for(Iterator<PrimitiveNode> it = getNodes(); it.hasNext(); )
		{
			PrimitiveNode pn = it.next();
			sortedMap.put(pn.getName(), pn);
		}
		return new ArrayList<PrimitiveNode>(sortedMap.values());
	}

	/**
	 * Returns the PrimitiveNode in this technology with a particular name.
	 * @param name the name of the PrimitiveNode.
	 * @return the PrimitiveNode in this technology with that name.
	 */
	public PrimitiveNode findNodeProto(String name)
	{
		if (name == null) return null;
		PrimitiveNode primNode = nodes.get(name);
		if (primNode != null) return primNode;

		for (Iterator<PrimitiveNode> it = getNodes(); it.hasNext(); )
		{
			PrimitiveNode pn = it.next();
			if (pn.getName().equalsIgnoreCase(name))
				return pn;
		}
		return null;
	}

	/**
	 * Returns an Iterator on the PrimitiveNode objects in this technology.
	 * @return an Iterator on the PrimitiveNode objects in this technology.
	 */
	public Iterator<PrimitiveNode> getNodes()
	{
		return nodes.values().iterator();
	}

	/**
	 * Returns the number of PrimitiveNodes objects in this technology.
	 * @return the number of PrimitiveNodes objects in this technology.
	 */
	public int getNumNodes()
	{
		return nodes.size();
	}

	/**
	 * Method to add a new PrimitiveNode to this Technology.
	 * This is usually done during initialization.
	 * @param np the PrimitiveNode to be added to this Technology.
	 */
	public void addNodeProto(PrimitiveNode np)
	{
		assert findNodeProto(np.getName()) == null;
        np.setPrimNodeIndexInTech(nodeIndex++);
		nodes.put(np.getName(), np);
	}

	/**
	 * Method to return the pure "NodeProto Function" a PrimitiveNode in this Technology.
	 * This method is overridden by technologies (such as Schematics) that know the node's function.
	 * @param pn PrimitiveNode to check.
     * @param techBits tech bits
	 * @return the PrimitiveNode.Function that describes the PrinitiveNode with specific tech bits.
	 */
	public PrimitiveNode.Function getPrimitiveFunction(PrimitiveNode pn, int techBits) { return pn.getFunction(); }

    private static final Layer.Function.Set diffLayers = new Layer.Function.Set(Layer.Function.DIFFP, Layer.Function.DIFFN);

    /**
	 * Method to return the size of a resistor-type NodeInst in this Technology.
	 * @param ni the NodeInst.
     * @param context the VarContext in which any vars will be evaluated,
     * pass in VarContext.globalContext if no context needed, or set to null
     * to avoid evaluation of variables (if any).
	 * @return the size of the NodeInst.
	 */
    public PrimitiveNodeSize getResistorSize(NodeInst ni, VarContext context)
    {
        if (ni.isCellInstance()) return null;
        SizeOffset so = ni.getSizeOffset();
        double length = ni.getXSize() - so.getLowXOffset() - so.getHighXOffset();
        double width = ni.getYSize() - so.getLowYOffset() - so.getHighYOffset();

        PrimitiveNodeSize size = new PrimitiveNodeSize(new Double(width), new Double(length));
        return size;
    }

    /**
     * Method to return length of active reqion. This will be used for
     * parasitics extraction. Electric layers are used for the calculation
     * @param ni the NodeInst.
     * @return length of the any active region
     */
    public double getTransistorActiveLength(NodeInst ni)
    {
        Poly [] diffList = getShapeOfNode(ni, true, false, diffLayers);
        double activeLen = 0;
        if (diffList.length > 0)
        {
            // Since electric layers are used, it takes the first active region
            Poly poly = diffList[0];
            activeLen = poly.getBounds2D().getHeight();
        }
        return activeLen;
    }

	/**
	 * Method to return the size of a transistor NodeInst in this Technology.
     * You should most likely be calling NodeInst.getTransistorSize instead of this.
	 * @param ni the NodeInst.
     * @param context the VarContext in which any vars will be evaluated,
     * pass in VarContext.globalContext if no context needed, or set to null
     * to avoid evaluation of variables (if any).
	 * @return the size of the NodeInst.
	 */
	public TransistorSize getTransistorSize(NodeInst ni, VarContext context)
	{
		SizeOffset so = ni.getSizeOffset();
		double width = ni.getXSize() - so.getLowXOffset() - so.getHighXOffset();
		double height = ni.getYSize() - so.getLowYOffset() - so.getHighYOffset();

		// override if there is serpentine information
		Point2D [] trace = ni.getTrace();
		if (trace != null)
		{
			width = 0;
			for(int i=1; i<trace.length; i++)
				width += trace[i-1].distance(trace[i]);
			height = 2;
			double serpentineLength = ni.getSerpentineTransistorLength();
			if (serpentineLength > 0) height = serpentineLength;
            System.out.println("No calculating length for active regions yet");
		}
        double activeLen = getTransistorActiveLength(ni);
		TransistorSize size = new TransistorSize(new Double(width), new Double(height), new Double(activeLen));
		return size;
	}

    /**
     * Method to set the size of a transistor NodeInst in this Technology.
     * You should be calling NodeInst.setTransistorSize instead of this.
     * @param ni the NodeInst
     * @param width the new width (positive values only)
     * @param length the new length (positive values only)
     */
    public void setPrimitiveNodeSize(NodeInst ni, double width, double length)
    {
        SizeOffset so = ni.getSizeOffset();
        double oldWidth = ni.getXSize() - so.getLowXOffset() - so.getHighXOffset();
        double oldLength = ni.getYSize() - so.getLowYOffset() - so.getHighYOffset();
        double dW = width - oldWidth;
        double dL = length - oldLength;
		ni.resize(dW, dL);
    }

    /**
     * Method to return a gate PortInst for this transistor NodeInst.
     * Implementation Note: May want to make this a more general
     * method, getPrimitivePort(PortType), if the number of port
     * types increases.  Note: You should be calling
     * NodeInst.getTransistorGatePort() instead of this, most likely.
     * @param ni the NodeInst
     * @return a PortInst for the gate of the transistor
     */
	public PortInst getTransistorGatePort(NodeInst ni) { return ni.getPortInst(0); }
    /**
     * Method to return a base PortInst for this transistor NodeInst.
     * @param ni the NodeInst
     * @return a PortInst for the base of the transistor
     */
	public PortInst getTransistorBasePort(NodeInst ni) { return ni.getPortInst(0); }

    /**
     * Method to return a source PortInst for this transistor NodeInst.
     * Implementation Note: May want to make this a more general
     * method, getPrimitivePort(PortType), if the number of port
     * types increases.  Note: You should be calling
     * NodeInst.getTransistorSourcePort() instead of this, most likely.
     * @param ni the NodeInst
     * @return a PortInst for the source of the transistor
     */
	public PortInst getTransistorSourcePort(NodeInst ni) { return ni.getPortInst(1); }
    /**
     * Method to return a emitter PortInst for this transistor NodeInst.
     * @param ni the NodeInst
     * @return a PortInst for the emitter of the transistor
     */
	public PortInst getTransistorEmitterPort(NodeInst ni) { return ni.getPortInst(1); }

    /**
     * Method to return a drain PortInst for this transistor NodeInst.
     * Implementation Note: May want to make this a more general
     * method, getPrimitivePort(PortType), if the number of port
     * types increases.  Note: You should be calling
     * NodeInst.getTransistorDrainPort() instead of this, most likely.
     * @param ni the NodeInst
     * @return a PortInst for the drain of the transistor
     */
	public PortInst getTransistorDrainPort(NodeInst ni)
	{
		if (ni.getProto().getTechnology() == Schematics.tech) return ni.getPortInst(2);
		return ni.getPortInst(3);
	}
    /**
     * Method to return a collector PortInst for this transistor NodeInst.
     * @param ni the NodeInst
     * @return a PortInst for the collector of the transistor
     */
	public PortInst getTransistorCollectorPort(NodeInst ni) { return ni.getPortInst(2); }

    /**
     * Method to return a bias PortInst for this transistor NodeInst.
     * Implementation Note: May want to make this a more general
     * method, getPrimitivePort(PortType), if the number of port
     * types increases.  Note: You should be calling
     * NodeInst.getTransistorBiasPort() instead of this, most likely.
     * @param ni the NodeInst
     * @return a PortInst for the bias of the transistor
     */
	public PortInst getTransistorBiasPort(NodeInst ni)
	{
		if (ni.getNumPortInsts() < 4) return null;
		if (ni.getProto().getTechnology() != Schematics.tech) return null;
		return ni.getPortInst(3);
	}

    /**
	 * Method to set the pure "NodeProto Function" for a primitive NodeInst in this Technology.
	 * This method is overridden by technologies (such as Schematics) that can change a node's function.
	 * @param ni the NodeInst to check.
	 * @param function the PrimitiveNode.Function to set on the NodeInst.
	 */
	public void setPrimitiveFunction(NodeInst ni, PrimitiveNode.Function function) {}

	/**
	 * Sets the technology to have no primitives.
	 * Users should never call this method.
	 * It is set once by the technology during initialization.
	 * This indicates to the user interface that it should not switch to this technology.
	 * The FPGA technology has this bit set because it initially contains no primitives,
	 * and they are only created dynamically.
	 */
	public void setNoPrimitiveNodes() { userBits |= NOPRIMTECHNOLOGY; }

	/**
	 * Returns true if this technology has no primitives.
	 * @return true if this technology has no primitives.
	 * This indicates to the user interface that it should not switch to this technology.
	 * The FPGA technology has this bit set because it initially contains no primitives,
	 * and they are only created dynamically.
	 */
	public boolean isNoPrimitiveNodes() { return (userBits & NOPRIMTECHNOLOGY) != 0; }

    /**
	 * Method to set default outline information on a NodeInst.
	 * Very few primitives have default outline information (usually just in the Artwork Technology).
	 * This method is overridden by the appropriate technology.
	 * @param ni the NodeInst to load with default outline information.
	 */
	public void setDefaultOutline(NodeInst ni) {}

	/**
	 * Method to get the SizeOffset associated with a NodeInst in this Technology.
	 * By having this be a method of Technology, it can be overridden by
	 * individual Technologies that need to make special considerations.
	 * @param ni the NodeInst to query.
	 * @return the SizeOffset object for the NodeInst.
	 */
	public SizeOffset getNodeInstSizeOffset(NodeInst ni)
	{
		NodeProto np = ni.getProto();
		return np.getProtoSizeOffset();
	}

	private static final Technology.NodeLayer [] nullPrimLayers = new Technology.NodeLayer [0];

	/**
	 * Returns the polygons that describe node "ni".
	 * @param ni the NodeInst that is being described.
	 * The prototype of this NodeInst must be a PrimitiveNode and not a Cell.
	 * @return an array of Poly objects that describes this NodeInst graphically.
	 */
	public Poly [] getShapeOfNode(NodeInst ni)
	{
		return getShapeOfNode(ni, false, false, null);
	}

	/**
	 * Returns the polygons that describe node "ni".
	 * @param ni the NodeInst that is being described.
	 * The prototype of this NodeInst must be a PrimitiveNode and not a Cell.
	 * @param electrical true to get the "electrical" layers.
	 * When electrical layers are requested, each layer is tied to a specific port on the node.
	 * If any piece of geometry covers more than one port,
	 * it must be split for the purposes of an "electrical" description.
	 * For example, the MOS transistor has 2 layers: Active and Poly.
	 * But it has 3 electrical layers: Active, Active, and Poly.
	 * The active must be split since each half corresponds to a different PrimitivePort on the PrimitiveNode.
	 * @param reasonable true to get only a minimal set of contact cuts in large contacts.
	 * The minimal set covers all edge contacts, but ignores the inner cuts in large contacts.
	 * @param onlyTheseLayers a set of layers to draw (if null, draw all layers).
	 * @return an array of Poly objects that describes this NodeInst graphically.
	 */
	public Poly [] getShapeOfNode(NodeInst ni, boolean electrical, boolean reasonable, Layer.Function.Set onlyTheseLayers)
	{
		if (ni.isCellInstance()) return null;

		PrimitiveNode np = (PrimitiveNode)ni.getProto();
		NodeLayer [] primLayers = np.getLayers();
		if (electrical)
		{
			NodeLayer [] eLayers = np.getElectricalLayers();
			if (eLayers != null) primLayers = eLayers;
		}

		if (onlyTheseLayers != null)
		{
			List<NodeLayer> layerArray = new ArrayList<NodeLayer>();

			for (int i = 0; i < primLayers.length; i++)
			{
				NodeLayer primLayer = primLayers[i];
				if (onlyTheseLayers.contains(primLayer.layer.getFunction()))
					layerArray.add(primLayer);
			}
			primLayers = new NodeLayer [layerArray.size()];
			layerArray.toArray(primLayers);
		}
		if (primLayers.length == 0)
			return new Poly[0];

		return getShapeOfNode(ni, electrical, reasonable, primLayers, null);
	}

	/**
	 * Returns the polygons that describe node "ni", given a set of
	 * NodeLayer objects to use.
	 * This method is overridden by specific Technologys.
	 * @param ni the NodeInst that is being described.
	 * @param electrical true to get the "electrical" layers
	 * Like the list returned by "getLayers", the results describe this PrimitiveNode,
	 * but each layer is tied to a specific port on the node.
	 * If any piece of geometry covers more than one port,
	 * it must be split for the purposes of an "electrical" description.<BR>
	 * For example, the MOS transistor has 2 layers: Active and Poly.
	 * But it has 3 electrical layers: Active, Active, and Poly.
	 * The active must be split since each half corresponds to a different PrimitivePort on the PrimitiveNode.
	 * @param reasonable true to get only a minimal set of contact cuts in large contacts.
	 * The minimal set covers all edge contacts, but ignores the inner cuts in large contacts.
	 * @param primLayers an array of NodeLayer objects to convert to Poly objects.
	 * @param layerOverride the layer to use for all generated polygons (if not null).
	 * The prototype of this NodeInst must be a PrimitiveNode and not a Cell.
	 * @return an array of Poly objects that describes this NodeInst graphically.
	 * This array includes displayable variables on the NodeInst.
	 */
	protected Poly [] getShapeOfNode(NodeInst ni, boolean electrical, boolean reasonable,
		Technology.NodeLayer [] primLayers, Layer layerOverride)
	{
		// if node is erased, remove layers
		if (!electrical)
		{
			if (ni.isWiped()) primLayers = nullPrimLayers; else
			{
				PrimitiveNode np = (PrimitiveNode)ni.getProto();
				if (np.isWipeOn1or2())
				{
					if (ni.pinUseCount()) primLayers = nullPrimLayers;
				}
			}
		}

		return computeShapeOfNode(ni, electrical, reasonable, primLayers, layerOverride);
	}

	/**
	 * Returns the polygons that describe node "ni", given a set of
	 * NodeLayer objects to use.
	 * This method is called by the specific Technology overrides of getShapeOfNode().
	 * @param ni the NodeInst that is being described.
	 * @param electrical true to get the "electrical" layers
	 * Like the list returned by "getLayers", the results describe this PrimitiveNode,
	 * but each layer is tied to a specific port on the node.
	 * If any piece of geometry covers more than one port,
	 * it must be split for the purposes of an "electrical" description.<BR>
	 * For example, the MOS transistor has 2 layers: Active and Poly.
	 * But it has 3 electrical layers: Active, Active, and Poly.
	 * The active must be split since each half corresponds to a different PrimitivePort on the PrimitiveNode.
	 * @param reasonable true to get only a minimal set of contact cuts in large contacts.
	 * The minimal set covers all edge contacts, but ignores the inner cuts in large contacts.
	 * @param primLayers an array of NodeLayer objects to convert to Poly objects.
	 * @param layerOverride the layer to use for all generated polygons (if not null).
	 * The prototype of this NodeInst must be a PrimitiveNode and not a Cell.
	 * @return an array of Poly objects that describes this NodeInst graphically.
	 */
	protected Poly [] computeShapeOfNode(NodeInst ni, boolean electrical, boolean reasonable, Technology.NodeLayer [] primLayers, Layer layerOverride)
	{
		PrimitiveNode np = (PrimitiveNode)ni.getProto();
		int specialType = np.getSpecialType();
		if (specialType != PrimitiveNode.SERPTRANS && np.isHoldsOutline())
		{
			Point2D [] outline = ni.getTrace();
			if (outline != null)
			{
				int numPolys = 1;
				Poly [] polys = new Poly[numPolys];
				Point2D [] pointList = new Point2D.Double[outline.length];
				for(int i=0; i<outline.length; i++)
				{
					pointList[i] = new Point2D.Double(ni.getAnchorCenterX() + outline[i].getX(),
						ni.getAnchorCenterY() + outline[i].getY());
				}
				polys[0] = new Poly(pointList);
				Technology.NodeLayer primLayer = primLayers[0];
				polys[0].setStyle(primLayer.getStyle());
				if (layerOverride != null) polys[0].setLayer(layerOverride); else
					polys[0].setLayer(primLayer.getLayer());
				if (electrical)
				{
					int portIndex = primLayer.getPortNum();
					if (portIndex >= 0) polys[0].setPort(np.getPort(portIndex));
				}
				return polys;
			}
		}

		// determine the number of polygons (considering that it may be "wiped")
		int numBasicLayers = primLayers.length;

		// if a MultiCut contact, determine the number of extra cuts
		int numExtraLayers = 0;
		MultiCutData mcd = null;
		SerpentineTrans std = null;
		if (np.hasMultiCuts())
		{
            for (NodeLayer nodeLayer: primLayers) {
                if (nodeLayer.representation == NodeLayer.MULTICUTBOX) {
                    mcd = new MultiCutData(ni.getD().size, nodeLayer);
                    if (reasonable) numExtraLayers += (mcd.cutsReasonable - 1); else
                        numExtraLayers += (mcd.cutsTotal - 1);
                }
            }
//			mcd = new MultiCutData(ni.getD());
//			if (reasonable) numExtraLayers = mcd.cutsReasonable; else
//			numExtraLayers = mcd.cutsTotal;
//			numBasicLayers--;
		} else if (specialType == PrimitiveNode.SERPTRANS)
		{
			std = new SerpentineTrans(ni.getD(), primLayers);
			if (std.layersTotal > 0)
			{
				numExtraLayers = std.layersTotal;
				numBasicLayers = 0;
			}
		}

		// determine the number of negating bubbles
		int numNegatingBubbles = 0;
		for(Iterator<Connection> it = ni.getConnections(); it.hasNext(); )
		{
			Connection con = it.next();
			if (con.isNegated()) numNegatingBubbles++;
		}

		// construct the polygon array
		int numPolys = numBasicLayers + numExtraLayers + numNegatingBubbles;
		Poly [] polys = new Poly[numPolys];

        double xCenter = ni.getAnchorCenterX();
        double yCenter = ni.getAnchorCenterY();
// 			double xCenter = ni.getTrueCenterX();
// 			double yCenter = ni.getTrueCenterY();
        double xSize = ni.getXSize();
        double ySize = ni.getYSize();

		// add in the basic polygons
		int fillPoly = 0;
		for(int i = 0; i < numBasicLayers; i++)
		{
			Technology.NodeLayer primLayer = primLayers[i];
			int representation = primLayer.getRepresentation();
			if (representation == Technology.NodeLayer.BOX)
			{
				EdgeH leftEdge = primLayer.getLeftEdge();
				EdgeH rightEdge = primLayer.getRightEdge();
				EdgeV topEdge = primLayer.getTopEdge();
				EdgeV bottomEdge = primLayer.getBottomEdge();
				double portLowX = xCenter + leftEdge.getMultiplier() * xSize + leftEdge.getAdder();
				double portHighX = xCenter + rightEdge.getMultiplier() * xSize + rightEdge.getAdder();
				double portLowY = yCenter + bottomEdge.getMultiplier() * ySize + bottomEdge.getAdder();
				double portHighY = yCenter + topEdge.getMultiplier() * ySize + topEdge.getAdder();
				Point2D [] pointList = Poly.makePoints(portLowX, portHighX, portLowY, portHighY);
				polys[fillPoly] = new Poly(pointList);
			} else if (representation == Technology.NodeLayer.POINTS)
			{
				TechPoint [] points = primLayer.getPoints();
				Point2D [] pointList = new Point2D.Double[points.length];
				for(int j=0; j<points.length; j++)
				{
					EdgeH xFactor = points[j].getX();
					EdgeV yFactor = points[j].getY();
					double x = 0, y = 0;
					if (xFactor != null && yFactor != null)
					{
						x = xCenter + xFactor.getMultiplier() * xSize + xFactor.getAdder();
						y = yCenter + yFactor.getMultiplier() * ySize + yFactor.getAdder();
					}
					pointList[j] = new Point2D.Double(x, y);
				}
				polys[fillPoly] = new Poly(pointList);
			} else if (representation == Technology.NodeLayer.MULTICUTBOX) {
                mcd = new MultiCutData(ni.getD().size, primLayer);
                Poly.Type style = primLayer.getStyle();
                PortProto port = null;
                if (electrical) port = np.getPort(0);
                if (reasonable) numExtraLayers = mcd.cutsReasonable; else
                    numExtraLayers = mcd.cutsTotal;
                for(int j = 0; j < numExtraLayers; j++) {
                    polys[fillPoly] = mcd.fillCutPoly(ni.getD(), j);
                    polys[fillPoly].setStyle(style);
                    polys[fillPoly].setLayer(primLayer.getLayer());
                    polys[fillPoly].setPort(port);
                    fillPoly++;
                }
                continue;
            }

			Poly.Type style = primLayer.getStyle();
			if (style.isText())
			{
				polys[fillPoly].setString(primLayer.getMessage());
				polys[fillPoly].setTextDescriptor(primLayer.getDescriptor());
			}
			polys[fillPoly].setStyle(style);
			if (layerOverride != null) polys[fillPoly].setLayer(layerOverride); else
				polys[fillPoly].setLayer(primLayer.getLayerOrPseudoLayer());
			if (electrical)
			{
				int portIndex = primLayer.getPortNum();
				if (portIndex >= 0) polys[fillPoly].setPort(np.getPort(portIndex));
			}
			fillPoly++;
		}

		// add in negating bubbles
		if (numNegatingBubbles > 0)
		{
			double bubbleRadius = Schematics.getNegatingBubbleSize() / 2;
			for(Iterator<Connection> it = ni.getConnections(); it.hasNext(); )
			{
				Connection con = it.next();
				if (!con.isNegated()) continue;

				// add a negating bubble
				AffineTransform trans = ni.rotateIn();
				Point2D portLocation = new Point2D.Double(con.getLocation().getX(), con.getLocation().getY());
				trans.transform(portLocation, portLocation);
				double x = portLocation.getX();
				double y = portLocation.getY();
				PrimitivePort pp = (PrimitivePort)con.getPortInst().getPortProto();
				int angle = pp.getAngle() * 10;
				double dX = DBMath.cos(angle) * bubbleRadius;
				double dY = DBMath.sin(angle) * bubbleRadius;
				Point2D [] points = new Point2D[2];
				points[0] = new Point2D.Double(x+dX, y+dY);
				points[1] = new Point2D.Double(x, y);
				polys[fillPoly] = new Poly(points);
				polys[fillPoly].setStyle(Poly.Type.CIRCLE);
				polys[fillPoly].setLayer(Schematics.tech.node_lay);
				fillPoly++;
			}
		}

		// add in the extra transistor layers
		if (std != null)
		{
			for(int i = 0; i < numExtraLayers; i++)
			{
				polys[fillPoly] = std.fillTransPoly(i, electrical);
				fillPoly++;
			}
		}
        assert fillPoly == polys.length;
		return polys;
	}

	/**
	 * Method to determine if cut case is considered multi cut
	 * It gets overridden by CMOS90
	 */
	public boolean isMultiCutInTechnology(MultiCutData mcd)
	{
		return (mcd.numCuts() > 1);
	}

	/**
	 * Method to decide whether a NodeInst is a multi-cut contact.
	 * The function is done by the Technologies so that it can be subclassed.
	 * @param ni the NodeInst being tested.
	 * @return true if it is a Multiple-cut contact.
	 */
	public boolean isMultiCutCase(NodeInst ni)
	{
		if (ni.isCellInstance()) return false;
		PrimitiveNode pnp = (PrimitiveNode)ni.getProto();
		if (!pnp.isMulticut()) return false;

		return (isMultiCutInTechnology(new MultiCutData(ni.getD())));
	}

	/**
	 * Class MultiCutData determines the locations of cuts in a multi-cut contact node.
	 */
	public static class MultiCutData
	{
		/** the size of each cut */													private long cutSizeX, cutSizeY;
		/** the separation between cuts */											private long cutSep;
		/** the separation between cuts */											private long cutSep1D;
		/** the separation between cuts in 3-neiboring or more cases*/				private long cutSep2D;
		/** the number of cuts in X and Y */										private int cutsX, cutsY;
		/** the total number of cuts */												private int cutsTotal;
		/** the "reasonable" number of cuts (around the outside only) */			private int cutsReasonable;
		/** the X coordinate of the leftmost cut's center */						private long cutBaseX;
		/** the Y coordinate of the topmost cut's center */							private long cutBaseY;
		/** cut position of last top-edge cut (for interior-cut elimination) */		private double cutTopEdge;
		/** cut position of last left-edge cut  (for interior-cut elimination) */	private double cutLeftEdge;
		/** cut position of last right-edge cut  (for interior-cut elimination) */	private double cutRightEdge;


        private void calculateInternalData(EPoint size, NodeLayer cutLayer)
        {
           assert cutLayer.representation == NodeLayer.MULTICUTBOX;
            TechPoint[] techPoints = cutLayer.points;
            long lx = techPoints[0].getX().getGridAdder() + (long)(size.getGridX()*techPoints[0].getX().getMultiplier());
            long hx = techPoints[1].getX().getGridAdder() + (long)(size.getGridX()*techPoints[1].getX().getMultiplier());
            long ly = techPoints[0].getY().getGridAdder() + (long)(size.getGridY()*techPoints[0].getY().getMultiplier());
            long hy = techPoints[1].getY().getGridAdder() + (long)(size.getGridY()*techPoints[1].getY().getMultiplier());
            cutSizeX = cutLayer.cutGridSizeX;
            cutSizeY = cutLayer.cutGridSizeY;
            cutSep1D = cutLayer.cutGridSep1D;
            cutSep2D = cutLayer.cutGridSep2D;
            calculateInternalData(lx, hx, ly, hy);
        }

        private void calculateInternalData(long lx, long hx, long ly, long hy)
        {
			// determine the actual node size
            cutBaseX = (lx + hx)>>1;
            cutBaseY = (ly + hy)>>1;
			long cutAreaWidth = hx - lx;
			long cutAreaHeight = hy - ly;

			// number of cuts depends on the size
			// Checking first if configuration gives 2D cuts
            int oneDcutsX = 1 + (int)(cutAreaWidth / (cutSizeX+cutSep1D));
			int oneDcutsY = 1 + (int)(cutAreaHeight / (cutSizeY+cutSep1D));
            int twoDcutsX = 1 + (int)(cutAreaWidth / (cutSizeX+cutSep2D));
			int twoDcutsY = 1 + (int)(cutAreaHeight / (cutSizeY+cutSep2D));

			cutSep = cutSep1D;
			cutsX = oneDcutsX;
			cutsY = oneDcutsY;
			if (cutsX > 1 && cutsY > 1)
			{
				cutSep = cutSep2D;
				cutsX = twoDcutsX;
				cutsY = twoDcutsY;
				if (cutsX == 1 || cutsY == 1)
				{
					// 1D separation sees a 2D grid, but 2D separation sees a linear array: use 1D linear settings
					cutSep = cutSep1D;
					if (cutAreaWidth > cutAreaHeight)
					{
						cutsX = oneDcutsX;
					} else
					{
						cutsY = oneDcutsY;
					}
				}
			}
			if (cutsX <= 0) cutsX = 1;
			if (cutsY <= 0) cutsY = 1;
			cutsReasonable = cutsTotal = cutsX * cutsY;
			if (cutsTotal != 1)
			{
				// prepare for the multiple contact cut locations
				if (cutsX > 2 && cutsY > 2)
				{
					cutsReasonable = cutsX * 2 + (cutsY-2) * 2;
					cutTopEdge = cutsX*2;
					cutLeftEdge = cutsX*2 + cutsY-2;
					cutRightEdge = cutsX*2 + (cutsY-2)*2;
				}
			}
        }

		/**
		 * Constructor to initialize for multiple cuts.
		 */
		private MultiCutData(EPoint size, NodeLayer cutLayer)
		{
            calculateInternalData(size, cutLayer);
		}

		/**
		 * Constructor to initialize for multiple cuts.
		 * @param niD the NodeInst with multiple cuts.
		 */
		public MultiCutData(ImmutableNodeInst niD)
		{
            this(niD.size, ((PrimitiveNode)niD.protoId).findMulticut());
		}

		/**
		 * Method to return the number of cuts in the contact node.
		 * @return the number of cuts in the contact node.
		 */
		public int numCuts() { return cutsTotal; }

		/**
		 * Method to return the number of cuts along X axis in the contact node.
		 * @return the number of cuts in the contact node along X axis.
		 */
		public int numCutsX() { return cutsX; }

		/**
		 * Method to return the number of cuts along Y axis in the contact node.
		 * @return the number of cuts in the contact node along Y axis.
		 */
		public int numCutsY() { return cutsY; }

        /**
         * Method to return the size of the cut along X.
         */
        public double getCutSizeX() { return cutSizeX; }

        /**
         * Method to return the size of the cut along Y.
         */
        public double getCutSizeY() { return cutSizeY; }

		/**
		 * Method to fill in the contact cuts of a contact when there are
		 * multiple cuts.  Node is in "ni" and the contact cut number (0 based) is
		 * in "cut".
		 */
		protected Poly fillCutPoly(ImmutableNodeInst ni, int cut)
		{
            return (fillCutPoly(ni.anchor, cut));
		}

        /**
         * Method to fill in the contact cuts based on anchor information.
        */
        public Poly fillCutPoly(EPoint anchor, int cut)
		{
            long cX = anchor.getGridX() + cutBaseX;
            long cY = anchor.getGridY() + cutBaseY;
            if (cutsX > 1 || cutsY > 1) {
                if (cutsX > 2 && cutsY > 2) {
                    // rearrange cuts so that the initial ones go around the outside
                    if (cut < cutsX) {
                        // bottom edge: it's ok as is
                    } else if (cut < cutTopEdge) {
                        // top edge: shift up
                        cut += cutsX * (cutsY-2);
                    } else if (cut < cutLeftEdge) {
                        // left edge: rearrange
                        cut = (int)((cut - cutTopEdge) * cutsX + cutsX);
                    } else if (cut < cutRightEdge) {
                        // right edge: rearrange
                        cut = (int)((cut - cutLeftEdge) * cutsX + cutsX*2-1);
                    } else {
                        // center: rearrange and scale down
                        cut = cut - (int)cutRightEdge;
                        int cutx = cut % (cutsX-2);
                        int cuty = cut / (cutsX-2);
                        cut = cuty * cutsX + cutx+cutsX+1;
                    }
                }

                // locate the X center of the cut
                if (cutsX != 1)
                    cX += ((cut % cutsX)*2 - (cutsX - 1))*(cutSizeX + cutSep)*0.5;
                // locate the Y center of the cut
                if (cutsY != 1)
                    cY += ((cut / cutsX)*2 - (cutsY - 1))*(cutSizeY + cutSep)*0.5;
            }
            double lX = DBMath.gridToLambda(cX - (cutSizeX >> 1));
            double hX = DBMath.gridToLambda(cX + (cutSizeX >> 1));
            double lY = DBMath.gridToLambda(cY - (cutSizeY >> 1));
            double hY = DBMath.gridToLambda(cY + (cutSizeY >> 1));
            Point2D.Double[] points = new Point2D.Double[] {
                new Point2D.Double(lX, lY),
                new Point2D.Double(hX, lY),
                new Point2D.Double(hX, hY),
                new Point2D.Double(lX, hY)};
			return new Poly(points);
		}
	}

	/**
	 * Class SerpentineTrans here.
	 */
	private static class SerpentineTrans
	{
		/** the ImmutableNodeInst that is this serpentine transistor */			private ImmutableNodeInst theNode;
		/** the prototype of this serpentine transistor */						private PrimitiveNode theProto;
		/** the number of polygons that make up this serpentine transistor */	private int layersTotal;
		/** the number of segments in this serpentine transistor */				private int numSegments;
		/** the extra gate width of this serpentine transistor */				private double extraScale;
		/** the node layers that make up this serpentine transistor */			private Technology.NodeLayer [] primLayers;
		/** the gate coordinates for this serpentine transistor */				private Point2D [] points;
		/** the defining values for this serpentine transistor */				private double [] specialValues;

		/**
		 * Constructor throws initialize for a serpentine transistor.
		 * @param niD the NodeInst with a serpentine transistor.
		 */
		public SerpentineTrans(ImmutableNodeInst niD, Technology.NodeLayer [] pLayers)
		{
			theNode = niD;

			layersTotal = 0;
			points = niD.getTrace();
			if (points != null)
			{
				if (points.length < 2) points = null;
			}
			if (points != null)
			{
				theProto = (PrimitiveNode)niD.protoId;
				specialValues = theProto.getSpecialValues();
				primLayers = pLayers;
				int count = primLayers.length;
				numSegments = points.length - 1;
				layersTotal = count * numSegments;

				extraScale = 0;
				double length = niD.getSerpentineTransistorLength();
				if (length > 0) extraScale = (length - specialValues[3]) / 2;
			}
		}

		/**
		 * Method to tell whether this SerpentineTrans object has valid outline information.
		 * @return true if the data exists.
		 */
		public boolean hasValidData() { return points != null; }

		private static final int LEFTANGLE =  900;
		private static final int RIGHTANGLE =  2700;

		/**
		 * Method to describe a box of a serpentine transistor.
		 * If the variable "trace" exists on the node, get that
		 * x/y/x/y information as the centerline of the serpentine path.  The outline is
		 * placed in the polygon "poly".
		 * NOTE: For each trace segment, the left hand side of the trace
		 * will contain the polygons that appear ABOVE the gate in the node
		 * definition. That is, the "top" port and diffusion will be above a
		 * gate segment that extends from left to right, and on the left of a
		 * segment that goes from bottom to top.
		 */
		private Poly fillTransPoly(int box, boolean electrical)
		{
			// compute the segment (along the serpent) and element (of transistor)
			int segment = box % numSegments;
			int element = box / numSegments;

			// see if nonstandard width is specified
			double lwid = primLayers[element].getSerpentineLWidth();
			double rwid = primLayers[element].getSerpentineRWidth();
			double extendt = primLayers[element].getSerpentineExtentT();
			double extendb = primLayers[element].getSerpentineExtentB();
			lwid += extraScale;
			rwid += extraScale;

			// prepare to fill the serpentine transistor
			double xoff = theNode.anchor.getX();
			double yoff = theNode.anchor.getY();
			int thissg = segment;   int next = segment+1;
			Point2D thisPt = points[thissg];
			Point2D nextPt = points[next];
			int angle = DBMath.figureAngle(thisPt, nextPt);

			// push the points at the ends of the transistor
			if (thissg == 0)
			{
				// extend "thissg" 180 degrees back
				int ang = angle+1800;
				thisPt = DBMath.addPoints(thisPt, DBMath.cos(ang) * extendt, DBMath.sin(ang) * extendt);
			}
			if (next == numSegments)
			{
				// extend "next" 0 degrees forward
				nextPt = DBMath.addPoints(nextPt, DBMath.cos(angle) * extendb, DBMath.sin(angle) * extendb);
			}

			// compute endpoints of line parallel to and left of center line
			int ang = angle+LEFTANGLE;
			double sin = DBMath.sin(ang) * lwid;
			double cos = DBMath.cos(ang) * lwid;
			Point2D thisL = DBMath.addPoints(thisPt, cos, sin);
			Point2D nextL = DBMath.addPoints(nextPt, cos, sin);

			// compute endpoints of line parallel to and right of center line
			ang = angle+RIGHTANGLE;
			sin = DBMath.sin(ang) * rwid;
			cos = DBMath.cos(ang) * rwid;
			Point2D thisR = DBMath.addPoints(thisPt, cos, sin);
			Point2D nextR = DBMath.addPoints(nextPt, cos, sin);

			// determine proper intersection of this and the previous segment
			if (thissg != 0)
			{
				Point2D otherPt = points[thissg-1];
				int otherang = DBMath.figureAngle(otherPt, thisPt);
				if (otherang != angle)
				{
					ang = otherang + LEFTANGLE;
					thisL = DBMath.intersect(DBMath.addPoints(thisPt, DBMath.cos(ang)*lwid, DBMath.sin(ang)*lwid),
						otherang, thisL,angle);
					ang = otherang + RIGHTANGLE;
					thisR = DBMath.intersect(DBMath.addPoints(thisPt, DBMath.cos(ang)*rwid, DBMath.sin(ang)*rwid),
						otherang, thisR,angle);
				}
			}

			// determine proper intersection of this and the next segment
			if (next != numSegments)
			{
				Point2D otherPt = points[next+1];
				int otherang = DBMath.figureAngle(nextPt, otherPt);
				if (otherang != angle)
				{
					ang = otherang + LEFTANGLE;
					Point2D newPtL = DBMath.addPoints(nextPt, DBMath.cos(ang)*lwid, DBMath.sin(ang)*lwid);
					nextL = DBMath.intersect(newPtL, otherang, nextL,angle);
					ang = otherang + RIGHTANGLE;
					Point2D newPtR = DBMath.addPoints(nextPt, DBMath.cos(ang)*rwid, DBMath.sin(ang)*rwid);
					nextR = DBMath.intersect(newPtR, otherang, nextR,angle);
				}
			}

			// fill the polygon
			Point2D [] points = new Point2D.Double[4];
			points[0] = DBMath.addPoints(thisL, xoff, yoff);
			points[1] = DBMath.addPoints(thisR, xoff, yoff);
			points[2] = DBMath.addPoints(nextR, xoff, yoff);
			points[3] = DBMath.addPoints(nextL, xoff, yoff);
			Poly retPoly = new Poly(points);

			// see if the sides of the polygon intersect
//			ang = figureangle(poly->xv[0], poly->yv[0], poly->xv[1], poly->yv[1]);
//			angle = figureangle(poly->xv[2], poly->yv[2], poly->xv[3], poly->yv[3]);
//			if (intersect(poly->xv[0], poly->yv[0], ang, poly->xv[2], poly->yv[2], angle, &x, &y) >= 0)
//			{
//				// lines intersect, see if the point is on one of the lines
//				if (x >= mini(poly->xv[0], poly->xv[1]) && x <= maxi(poly->xv[0], poly->xv[1]) &&
//					y >= mini(poly->yv[0], poly->yv[1]) && y <= maxi(poly->yv[0], poly->yv[1]))
//				{
//					if (abs(x-poly->xv[0])+abs(y-poly->yv[0]) > abs(x-poly->xv[1])+abs(y-poly->yv[1]))
//					{
//						poly->xv[1] = x;   poly->yv[1] = y;
//						poly->xv[2] = poly->xv[3];   poly->yv[2] = poly->yv[3];
//					} else
//					{
//						poly->xv[0] = x;   poly->yv[0] = y;
//					}
//					poly->count = 3;
//				}
//			}

			Technology.NodeLayer primLayer = primLayers[element];
			retPoly.setStyle(primLayer.getStyle());
			retPoly.setLayer(primLayer.getLayer());

			// include port information if requested
			if (electrical)
			{
				int portIndex = primLayer.getPortNum();
				if (portIndex >= 0)
				{
					PrimitiveNode np = (PrimitiveNode)theNode.protoId;
					PortProto port = np.getPort(portIndex);
					retPoly.setPort(port);
				}
			}
			return retPoly;
		}

		/**
		 * Method to describe a port in a transistor that is part of a serpentine path.
		 * The port path is shrunk by "diffInset" in the length and is pushed "diffExtend" from the centerline.
		 * The default width of the transistor is "defWid".
		 * The assumptions about directions are:
		 * Segments have port 1 to the left, and port 3 to the right of the gate trace.
		 * Port 0, the "left-hand" end of the gate, appears at the starting
		 * end of the first trace segment; port 2, the "right-hand" end of the gate,
		 * appears at the end of the last trace segment.  Port 3 is drawn as a
		 * reflection of port 1 around the trace.
		 * The poly ports are extended "polyExtend" beyond the appropriate end of the trace
		 * and are inset by "polyInset" from the polysilicon edge.
		 * The diffusion ports are extended "diffExtend" from the polysilicon edge
		 * and set in "diffInset" from the ends of the trace segment.
		 */
		private Poly fillTransPort(PortProto pp)
		{
			double diffInset = specialValues[1];
			double diffExtend = specialValues[2];
			double defWid = specialValues[3] + extraScale;
			double polyInset = specialValues[4];
			double polyExtend = specialValues[5];

			// prepare to fill the serpentine transistor port
			double xOff = theNode.anchor.getX();
			double yOff = theNode.anchor.getY();
			int total = points.length;
			AffineTransform trans = theNode.orient.rotateAbout(theNode.anchor.getX(), theNode.anchor.getY());

			// determine which port is being described
			int which = 0;
			for(Iterator<PortProto> it = theProto.getPorts(); it.hasNext(); )
			{
				PortProto lpp = it.next();
				if (lpp == pp) break;
				which++;
			}

			// ports 0 and 2 are poly (simple)
			if (which == 0)
			{
				Point2D thisPt = new Point2D.Double(points[0].getX(), points[0].getY());
				Point2D nextPt = new Point2D.Double(points[1].getX(), points[1].getY());
				int angle = DBMath.figureAngle(thisPt, nextPt);
				int ang = (angle+1800) % 3600;
				thisPt.setLocation(thisPt.getX() + DBMath.cos(ang) * polyExtend + xOff,
					thisPt.getY() + DBMath.sin(ang) * polyExtend + yOff);

				ang = (angle+LEFTANGLE) % 3600;
				Point2D end1 = new Point2D.Double(thisPt.getX() + DBMath.cos(ang) * (defWid/2-polyInset),
					thisPt.getY() + DBMath.sin(ang) * (defWid/2-polyInset));

				ang = (angle+RIGHTANGLE) % 3600;
				Point2D end2 = new Point2D.Double(thisPt.getX() + DBMath.cos(ang) * (defWid/2-polyInset),
					thisPt.getY() + DBMath.sin(ang) * (defWid/2-polyInset));

				Point2D [] portPoints = new Point2D.Double[2];
				portPoints[0] = end1;
				portPoints[1] = end2;
				trans.transform(portPoints, 0, portPoints, 0, 2);
				Poly retPoly = new Poly(portPoints);
				retPoly.setStyle(Poly.Type.OPENED);
				return retPoly;
			}
			if (which == 2)
			{
				Point2D thisPt = new Point2D.Double(points[total-1].getX(), points[total-1].getY());
				Point2D nextPt = new Point2D.Double(points[total-2].getX(), points[total-2].getY());
				int angle = DBMath.figureAngle(thisPt, nextPt);
				int ang = (angle+1800) % 3600;
				thisPt.setLocation(thisPt.getX() + DBMath.cos(ang) * polyExtend + xOff,
					thisPt.getY() + DBMath.sin(ang) * polyExtend + yOff);

				ang = (angle+LEFTANGLE) % 3600;
				Point2D end1 = new Point2D.Double(thisPt.getX() + DBMath.cos(ang) * (defWid/2-polyInset),
					thisPt.getY() + DBMath.sin(ang) * (defWid/2-polyInset));

				ang = (angle+RIGHTANGLE) % 3600;
				Point2D end2 = new Point2D.Double(thisPt.getX() + DBMath.cos(ang) * (defWid/2-polyInset),
					thisPt.getY() + DBMath.sin(ang) * (defWid/2-polyInset));

				Point2D [] portPoints = new Point2D.Double[2];
				portPoints[0] = end1;
				portPoints[1] = end2;
				trans.transform(portPoints, 0, portPoints, 0, 2);
				Poly retPoly = new Poly(portPoints);
				retPoly.setStyle(Poly.Type.OPENED);
				return retPoly;
			}

			// port 3 is the negated path side of port 1
			if (which == 3)
			{
				diffExtend = -diffExtend;
				defWid = -defWid;
			}

			// extra port on some n-transistors
			if (which == 4) diffExtend = defWid = 0;

			Point2D [] portPoints = new Point2D.Double[total];
			Point2D lastPoint = null;
			int lastAngle = 0;
			for(int nextIndex=1; nextIndex<total; nextIndex++)
			{
				int thisIndex = nextIndex-1;
				Point2D thisPt = new Point2D.Double(points[thisIndex].getX() + xOff, points[thisIndex].getY() + yOff);
				Point2D nextPt = new Point2D.Double(points[nextIndex].getX() + xOff, points[nextIndex].getY() + yOff);
				int angle = DBMath.figureAngle(thisPt, nextPt);

				// determine the points
				if (thisIndex == 0)
				{
					// extend "this" 0 degrees forward
					thisPt.setLocation(thisPt.getX() + DBMath.cos(angle) * diffInset,
						thisPt.getY() + DBMath.sin(angle) * diffInset);
				}
				if (nextIndex == total-1)
				{
					// extend "next" 180 degrees back
					int backAng = (angle+1800) % 3600;
					nextPt.setLocation(nextPt.getX() + DBMath.cos(backAng) * diffInset,
						nextPt.getY() + DBMath.sin(backAng) * diffInset);
				}

				// compute endpoints of line parallel to center line
				int ang = (angle+LEFTANGLE) % 3600;
				double sine = DBMath.sin(ang);
				double cosine = DBMath.cos(ang);
				thisPt.setLocation(thisPt.getX() + cosine * (defWid/2+diffExtend),
					thisPt.getY() + sine * (defWid/2+diffExtend));
				nextPt.setLocation(nextPt.getX() + cosine * (defWid/2+diffExtend),
					nextPt.getY() + sine * (defWid/2+diffExtend));

				if (thisIndex != 0)
				{
					// compute intersection of this and previous line
					thisPt = DBMath.intersect(lastPoint, lastAngle, thisPt, angle);
				}
				portPoints[thisIndex] = thisPt;
				lastPoint = thisPt;
				lastAngle = angle;
				if (nextIndex == total-1)
					portPoints[nextIndex] = nextPt;
			}
			if (total > 0)
				trans.transform(portPoints, 0, portPoints, 0, total);
			Poly retPoly = new Poly(portPoints);
			retPoly.setStyle(Poly.Type.OPENED);
			return retPoly;
		}
	}

	/**
	 * Method to convert old primitive node names to their proper NodeProtos.
	 * @param name the unknown node name, read from an old Library.
	 * @return the proper PrimitiveNode to use for this name.
	 */
	public PrimitiveNode convertOldNodeName(String name) {
        return oldNodeNames.get(name);
    }

    public Map<String,PrimitiveNode> getOldNodeNames() { return new TreeMap<String,PrimitiveNode>(oldNodeNames); }

	/****************************** PORTS ******************************/

	/**
	 * Returns a polygon that describes a particular port on a NodeInst.
	 * @param ni the NodeInst that has the port of interest.
	 * The prototype of this NodeInst must be a PrimitiveNode and not a Cell.
	 * @param pp the PrimitivePort on that NodeInst that is being described.
	 * @return a Poly object that describes this PrimitivePort graphically.
	 */
	public Poly getShapeOfPort(NodeInst ni, PrimitivePort pp)
	{
		return getShapeOfPort(ni, pp, null);
	}

	/**
	 * Returns a polygon that describes a particular port on a NodeInst.
	 * @param ni the NodeInst that has the port of interest.
	 * The prototype of this NodeInst must be a PrimitiveNode and not a Cell.
	 * @param pp the PrimitivePort on that NodeInst that is being described.
	 * @param selectPt if not null, it requests a new location on the port,
	 * away from existing arcs, and close to this point.
	 * This is useful for "area" ports such as the left side of AND and OR gates.
	 * @return a Poly object that describes this PrimitivePort graphically.
	 */
	public Poly getShapeOfPort(NodeInst ni, PrimitivePort pp, Point2D selectPt)
	{
		PrimitiveNode np = (PrimitiveNode)ni.getProto();
		if (np.getSpecialType() == PrimitiveNode.SERPTRANS)
		{
			// serpentine transistors use a more complex port determination
			SerpentineTrans std = new SerpentineTrans(ni.getD(), np.getLayers());
			if (std.hasValidData())
				return std.fillTransPort(pp);
		}

		// standard port determination, see if there is outline information
		if (np.isHoldsOutline())
		{
			// outline may determine the port
			Point2D [] outline = ni.getTrace();
			if (outline != null)
			{
				double cX = ni.getAnchorCenterX();
				double cY = ni.getAnchorCenterY();
				Point2D [] pointList = new Point2D.Double[outline.length];
				for(int i=0; i<outline.length; i++)
				{
					pointList[i] = new Point2D.Double(cX + outline[i].getX(), cY + outline[i].getY());
				}
				Poly portPoly = new Poly(pointList);
				if (ni.getFunction() == PrimitiveNode.Function.NODE)
				{
					portPoly.setStyle(Poly.Type.FILLED);
				} else
				{
					portPoly.setStyle(Poly.Type.OPENED);
				}
				portPoly.setTextDescriptor(TextDescriptor.getExportTextDescriptor());
				return portPoly;
			}
		}

		// standard port computation
		double portLowX = ni.getAnchorCenterX() + pp.getLeft().getMultiplier() * ni.getXSize() + pp.getLeft().getAdder();
		double portHighX = ni.getAnchorCenterX() + pp.getRight().getMultiplier() * ni.getXSize() + pp.getRight().getAdder();
		double portLowY = ni.getAnchorCenterY() + pp.getBottom().getMultiplier() * ni.getYSize() + pp.getBottom().getAdder();
		double portHighY = ni.getAnchorCenterY() + pp.getTop().getMultiplier() * ni.getYSize() + pp.getTop().getAdder();
		double portX = (portLowX + portHighX) / 2;
		double portY = (portLowY + portHighY) / 2;
		Poly portPoly = new Poly(portX, portY, portHighX-portLowX, portHighY-portLowY);
		portPoly.setStyle(Poly.Type.FILLED);
		portPoly.setTextDescriptor(TextDescriptor.getExportTextDescriptor());
		return portPoly;
	}

	/**
	 * Method to convert old primitive port names to their proper PortProtos.
	 * This method is overridden by those technologies that have any special port name conversion issues.
	 * By default, there is little to be done, because by the time this
	 * method is called, normal searches have failed.
	 * @param portName the unknown port name, read from an old Library.
	 * @param np the PrimitiveNode on which this port resides.
	 * @return the proper PrimitivePort to use for this name.
	 */
	public PrimitivePort convertOldPortName(String portName, PrimitiveNode np)
	{
		// some technologies switched from ports ending in "-bot" to the ending "-bottom"
		int len = portName.length() - 4;
		if (len > 0 && portName.substring(len).equals("-bot"))
		{
			PrimitivePort pp = (PrimitivePort)np.findPortProto(portName + "tom");
			if (pp != null) return pp;
		}
        if (np.getNumPorts() == 1)
            return np.getPort(0);
		return null;
	}

	/*********************** PARASITIC SETTINGS ***************************/

    private Setting makeParasiticSetting(String what, double factory) {
        String techShortName = getTechShortName();
        if (techShortName == null) techShortName = getTechName();
        return Setting.makeDoubleSetting(what + "IN" + getTechName(), prefs,
                getProjectSettings(), what,
                "Parasitic tab", techShortName + " " + what, factory);
    }

    private Setting makeParasiticSetting(String what, boolean factory) {
        String techShortName = getTechShortName();
        if (techShortName == null) techShortName = getTechName();
        return Setting.makeBooleanSetting(what + "IN" + getTechName(), prefs,
                getProjectSettings(), what,
                "Parasitic tab", techShortName + " " + what, factory);
    }

	/**
	 * Method to return the Pref object associated with all Technologies.
	 * The Pref object is used to save option information.
	 * Since preferences are organized by package, there is only one for
	 * the technologies (they are all in the same package).
	 * @return the Pref object associated with all Technologies.
	 */
	public static Pref.Group getTechnologyPreferences() { return prefs; }

	/**
	 * Returns the minimum resistance of this Technology.
     * Default value is 10.0
	 * @return the minimum resistance of this Technology.
	 */
	public double getMinResistance()
	{
		return cacheMinResistance.getDouble();
	}
	/**
	 * Returns project Setting to tell the minimum resistance of this Technology.
	 * @return project Setting to tell the minimum resistance of this Technology.
	 */
	public Setting getMinResistanceSetting() { return cacheMinResistance; }

	/**
	 * Returns the minimum capacitance of this Technology.
     * Default value is 0.0
	 * @return the minimum capacitance of this Technology.
	 */
	public double getMinCapacitance()
	{
        // 0.0 is the default value
		return cacheMinCapacitance.getDouble();
	}
	/**
	 * Returns project Setting to tell the minimum capacitance of this Technology.
	 * @return project Setting to tell the minimum capacitance of this Technology.
	 */
	public Setting getMinCapacitanceSetting() { return cacheMinCapacitance; }


    /**
     * Get the maximum series resistance for layout extraction
     *  for this Technology.
     * @return the maximum series resistance of extracted layout nets
     */
    public double getMaxSeriesResistance()
    {
        return cacheMaxSeriesResistance.getDouble();
    }
    /**
     * Returns project Setting to tell the maximum series resistance for layout extraction
     *  for this Technology.
     * @return project Setting to tell the maximum series resistance for layout extraction
     *  for this Technology.
     */
    public Setting getMaxSeriesResistanceSetting() { return cacheMaxSeriesResistance; }


	/**
	 * Returns true if gate is included in resistance calculation. False is the default.
	 * @return true if gate is included in resistance calculation.
	 */
	public boolean isGateIncluded()
	{
        // False is the default
		return cacheIncludeGate.getBoolean();
	}
    /**
     * Returns project Setting to tell gate inclusion.
     * @return project Setting to tell gate inclusion
     */
    public Setting getGateIncludedSetting() { return cacheIncludeGate; }

    /**
     * Returns true if ground network is included in parasitics calculation. False is the default.
     * @return true if ground network is included.
     */
    public boolean isGroundNetIncluded()
    {
        // False is the default
        return cacheIncludeGnd.getBoolean();
    }
	/**
	 * Returns project Setting to tell ground network inclusion.
	 * @return project Setting to tell ground network inclusion
	 */
	public Setting getGroundNetIncludedSetting() { return cacheIncludeGnd; }


    /**
     * Gets the gate length subtraction for this Technology (in microns).
     * This is used because there is sometimes a subtracted offset from the layout
     * to the drawn length.
     * @return the gate length subtraction for this Technology
     */
    public double getGateLengthSubtraction()
    {
        return cacheGateLengthSubtraction.getDouble();
    }
    /**
     * Returns project Setting to tell the gate length subtraction for this Technology (in microns)
     * This is used because there is sometimes a subtracted offset from the layout
     * to the drawn length.
     * @return project Setting to tell the subtraction value for a gate length in microns
     */
    public Setting getGateLengthSubtractionSetting() { return cacheGateLengthSubtraction; }


	/**
	 * Method to set default parasitic values on this Technology.
	 * These values are not saved in the options.
	 * @param minResistance the minimum resistance in this Technology.
	 * @param minCapacitance the minimum capacitance in this Technology.
	 */
	public void setFactoryParasitics(double minResistance, double minCapacitance)
	{
		cacheMinResistance = makeParasiticSetting("MininumResistance", minResistance);
		cacheMinCapacitance = makeParasiticSetting("MininumCapacitance", minCapacitance);
	}

    /*********************** LOGICAL EFFORT SETTINGS ***************************/

    private ProjSettingsNode getLESettingsNode() {
        ProjSettingsNode node = getProjectSettings().getNode("LogicalEffort");
//        if (node == null) {
//            node = new ProjSettingsNode();
//            getProjectSettings().putNode("LogicalEffort", node);
//        }
        return node;
    }

    private Setting makeLESetting(String what, double factory) {
        String techShortName = getTechShortName();
        if (techShortName == null) techShortName = getTechName();
        return Setting.makeDoubleSetting(what + "IN" + getTechName(), prefs,
                getLESettingsNode(), what,
                "Logical Effort tab", techShortName + " " + what, factory);
    }

//     private Setting makeLESetting(String what, int factory) {
//         String techShortName = getTechShortName();
//         if (techShortName == null) techShortName = getTechName();
//         return Setting.makeIntSetting(what + "IN" + getTechName(), prefs,
//                 getLESettingsNode(), what,
//                 "Logical Effort tab", techShortName + " " + what, factory);
//     }

    // ************************ tech specific?  - start *****************************
//    /**
//	 * Method to get the Global Fanout for Logical Effort.
//	 * The default is DEFAULT_GLOBALFANOUT.
//	 * @return the Global Fanout for Logical Effort.
//	 */
//	public double getGlobalFanout()
//	{
//		return cacheGlobalFanout.getDouble();
//	}
//	/**
//	 * Method to set the Global Fanout for Logical Effort.
//	 * @param fo the Global Fanout for Logical Effort.
//	 */
//	public void setGlobalFanout(double fo)
//	{
//		cacheGlobalFanout.setDouble(fo);
//	}
//
//	/**
//	 * Method to get the Convergence Epsilon value for Logical Effort.
//	 * The default is DEFAULT_EPSILON.
//	 * @return the Convergence Epsilon value for Logical Effort.
//	 */
//	public double getConvergenceEpsilon()
//	{
//		return cacheConvergenceEpsilon.getDouble();
//	}
//	/**
//	 * Method to set the Convergence Epsilon value for Logical Effort.
//	 * @param ep the Convergence Epsilon value for Logical Effort.
//	 */
//	public void setConvergenceEpsilon(double ep)
//	{
//		cacheConvergenceEpsilon.setDouble(ep);
//	}
//
//	/**
//	 * Method to get the maximum number of iterations for Logical Effort.
//	 * The default is DEFAULT_MAXITER.
//	 * @return the maximum number of iterations for Logical Effort.
//	 */
//	public int getMaxIterations()
//	{
//		return cacheMaxIterations.getInt();
//	}
//	/**
//	 * Method to set the maximum number of iterations for Logical Effort.
//	 * @param it the maximum number of iterations for Logical Effort.
//	 */
//	public void setMaxIterations(int it)
//	{
//		cacheMaxIterations.setInt(it);
//	}
//
//    /**
//     * Method to get the keeper size ratio for Logical Effort.
//     * The default is DEFAULT_KEEPERRATIO.
//     * @return the keeper size ratio for Logical Effort.
//     */
//    public double getKeeperRatio()
//    {
//        return cacheKeeperRatio.getDouble();
//    }
//    /**
//     * Method to set the keeper size ratio for Logical Effort.
//     * @param kr the keeper size ratio for Logical Effort.
//     */
//    public void setKeeperRatio(double kr)
//    {
//        cacheKeeperRatio.setDouble(kr);
//    }

    // ************************ tech specific?  - end *****************************

    protected void setFactoryLESettings(double gateCapacitance, double wireRation, double diffAlpha) {
		cacheGateCapacitance = makeLESetting("GateCapacitance", gateCapacitance);
		cacheWireRatio = makeLESetting("WireRatio", wireRation);
		cacheDiffAlpha = makeLESetting("DiffAlpha", diffAlpha);
    }

	/**
	 * Method to get the Gate Capacitance for Logical Effort.
	 * The default is DEFAULT_GATECAP.
	 * @return the Gate Capacitance for Logical Effort.
	 */
	public double getGateCapacitance()
	{
		return cacheGateCapacitance.getDouble();
	}
	/**
	 * Returns project Setting to tell the Gate Capacitance for Logical Effort.
	 * @return project Setting to tell the Gate Capacitance for Logical Effort.
	 */
	public Setting getGateCapacitanceSetting() { return cacheGateCapacitance; }

	/**
	 * Method to get the wire capacitance ratio for Logical Effort.
	 * The default is DEFAULT_WIRERATIO.
	 * @return the wire capacitance ratio for Logical Effort.
	 */
	public double getWireRatio()
	{
		return cacheWireRatio.getDouble();
	}
	/**
	 * Returns project Setting to tell the wire capacitance ratio for Logical Effort.
	 * @return project Setting to tell the wire capacitance ratio for Logical Effort.
	 */
	public Setting getWireRatioSetting() { return cacheWireRatio; }

	/**
	 * Method to get the diffusion to gate capacitance ratio for Logical Effort.
	 * The default is DEFAULT_DIFFALPHA.
	 * @return the diffusion to gate capacitance ratio for Logical Effort.
	 */
	public double getDiffAlpha()
	{
		return cacheDiffAlpha.getDouble();
	}
	/**
	 * Returns project Setting to tell the diffusion to gate capacitance ratio for Logical Effort.
	 * @return project Setting to tell the diffusion to gate capacitance ratio for Logical Effort.
	 */
	public Setting getDiffAlphaSetting() { return cacheDiffAlpha; }

    // ================================================================

	/**
	 * Method to return the level-1 header cards for SPICE in this Technology.
	 * The default is [""].
	 * @return the level-1 header cards for SPICE in this Technology.
	 */
	public String [] getSpiceHeaderLevel1() { return spiceHeaderLevel1; }

	/**
	 * Method to set the level-1 header cards for SPICE in this Technology.
	 * @param lines the level-1 header cards for SPICE in this Technology.
	 */
	public void setSpiceHeaderLevel1(String [] lines) { spiceHeaderLevel1 = lines; }

	/**
	 * Method to return the level-2 header cards for SPICE in this Technology.
	 * The default is [""].
	 * @return the level-2 header cards for SPICE in this Technology.
	 */
	public String [] getSpiceHeaderLevel2() { return spiceHeaderLevel2; }

	/**
	 * Method to set the level-2 header cards for SPICE in this Technology.
	 * @param lines the level-2 header cards for SPICE in this Technology.
	 */
	public void setSpiceHeaderLevel2(String [] lines) { spiceHeaderLevel2 = lines; }

	/**
	 * Method to return the level-3 header cards for SPICE in this Technology.
	 * The default is [""].
	 * @return the level-3 header cards for SPICE in this Technology.
	 */
	public String [] getSpiceHeaderLevel3() { return spiceHeaderLevel3; }

	/**
	 * Method to set the level-3 header cards for SPICE in this Technology.
	 * @param lines the level-3 header cards for SPICE in this Technology.
	 */
	public void setSpiceHeaderLevel3(String [] lines) { spiceHeaderLevel3 = lines; }

	/****************************** MISCELANEOUS ******************************/

	/**
	 * Sets the technology to be "non-electrical".
	 * Users should never call this method.
	 * It is set once by the technology during initialization.
	 * Examples of non-electrical technologies are "Artwork" and "Gem".
	 */
	protected void setNonElectrical() { userBits |= NONELECTRICAL; }

	/**
	 * Returns true if this technology is "non-electrical".
	 * @return true if this technology is "non-electrical".
	 * Examples of non-electrical technologies are "Artwork" and "Gem".
	 */
	public boolean isNonElectrical() { return (userBits & NONELECTRICAL) != 0; }

	/**
	 * Sets the technology to be non-standard.
	 * Users should never call this method.
	 * It is set once by the technology during initialization.
	 * A non-standard technology cannot be edited in the technology editor.
	 * Examples are Schematics and Artwork, which have more complex graphics.
	 */
	protected void setNonStandard() { userBits |= NONSTANDARD; }

	/**
	 * Returns true if this technology is non-standard.
	 * @return true if this technology is non-standard.
	 * A non-standard technology cannot be edited in the technology editor.
	 * Examples are Schematics and Artwork, which have more complex graphics.
	 */
	public boolean isNonStandard() { return (userBits & NONSTANDARD) != 0; }

	/**
	 * Sets the technology to be "static".
	 * Users should never call this method.
	 * It is set once by the technology during initialization.
	 * Static technologies are the core set of technologies in Electric that are
	 * essential, and cannot be deleted.
	 * The technology-editor can create others later, and they can be deleted.
	 */
	protected void setStaticTechnology() { userBits |= STATICTECHNOLOGY; }

	/**
	 * Returns true if this technoology is "static" (cannot be deleted).
	 * @return true if this technoology is "static" (cannot be deleted).
	 * Static technologies are the core set of technologies in Electric that are
	 * essential, and cannot be deleted.
	 * The technology-editor can create others later, and they can be deleted.
	 */
	public boolean isStaticTechnology() { return (userBits & STATICTECHNOLOGY) != 0; }

	/**
	 * Returns the name of this technology.
	 * Each technology has a unique name, such as "mocmos" (MOSIS CMOS).
	 * @return the name of this technology.
	 */
	public String getTechName() { return techName; }

	/**
	 * Sets the name of this technology.
	 * Technology names must be unique.
	 */
	public void setTechName(String techName)
	{
        throw new UnsupportedOperationException(); // Correct implementation must also rename ProjectSettings and Preferences of this Technology

//		for(Iterator<Technology> it = Technology.getTechnologies(); it.hasNext(); )
//		{
//			Technology tech = it.next();
//			if (tech == this) continue;
//			if (tech.techName.equalsIgnoreCase(techName))
//			{
//				System.out.println("Cannot rename " + this + "to '" + techName + "' because that name is used by another technology");
//				return;
//			}
//		}
//		if (!jelibSafeName(techName))
//			System.out.println("Technology name " + techName + " is not safe to write into JELIB");
//		this.techName = techName;
	}

	/**
	 * Method checks that string is safe to write into JELIB file without
	 * conversion.
	 * @param str the string to check.
	 * @return true if string is safe to write into JELIB file.
	 */
	static boolean jelibSafeName(String str)
	{
		for (int i = 0; i < str.length(); i++)
		{
			char ch = str.charAt(i);
			if (ch == '\n' || ch == '|' || ch == '^' || ch == '"')
				return false;
		}
		return true;
	}

	/**
	 * Returns the short name of this technology.
	 * The short name is user readable ("MOSIS CMOS" instead of "mocmos")
	 * but is shorter than the "description" which often includes options.
	 * @return the short name of this technology.
	 */
	public String getTechShortName() { return techShortName; }

	/**
	 * Sets the short name of this technology.
	 * The short name is user readable ("MOSIS CMOS" instead of "mocmos")
	 * but is shorter than the "description" which often includes options.
	 * @param techShortName the short name for this technology.
	 */
	protected void setTechShortName(String techShortName) { this.techShortName = techShortName; }

	/**
	 * Returns the full description of this Technology.
	 * Full descriptions go beyond the one-word technology name by including such
	 * information as foundry, nuumber of available layers, and process specifics.
	 * For example, "Complementary MOS (from MOSIS, Submicron, 2-6 metals [4], double poly)".
	 * @return the full description of this Technology.
	 */
	public String getTechDesc() { return techDesc; }

	/**
	 * Sets the full description of this Technology.
	 * Full descriptions go beyond the one-word technology name by including such
	 * information as foundry, nuumber of available layers, and process specifics.
	 * For example, "Complementary MOS (from MOSIS, Submicron, 2-6 metals [4], double poly)".
	 */
	public void setTechDesc(String techDesc) { this.techDesc = techDesc; }

	/**
	 * Returns the scale for this Technology.
	 * The technology's scale is for manufacturing output, which must convert
	 * the unit-based values in Electric to real-world values (in nanometers).
	 * @return the scale for this Technology.
	 */
	public double getScale()
	{
		return cacheScale.getDouble();
	}

	/**
	 * Method to obtain the Variable name for scaling this Technology.
	 * Do not use this for arbitrary use.
	 * The method exists so that ELIB readers can handle the unusual location
	 * of scale information in the ELIB files.
	 * @return the Variable name for scaling this Technology.
	 */
	public String getScaleVariableName()
	{
		return "ScaleFOR" + getTechName();
	}

	/**
	 * Sets the factory scale of this technology.
	 * The technology's scale is for manufacturing output, which must convert
	 * the unit-based values in Electric to real-world values (in nanometers).
	 * @param factory the factory scale between this technology and the real units.
	 * @param scaleRelevant true if this is a layout technology, and the scale factor has meaning.
	 */
	protected void setFactoryScale(double factory, boolean scaleRelevant)
	{
		this.scaleRelevant = scaleRelevant;
        String techShortName = getTechShortName();
        if (techShortName == null) techShortName = getTechName();
		cacheScale = Setting.makeDoubleSetting(getScaleVariableName(), prefs,
                getProjectSettings(), "Scale", "Scale tab", techShortName + " scale", factory);
		cacheScale.setValidOption(isScaleRelevant());
    }

	/**
	 * Returns project Setting to tell the scale of this technology.
	 * The technology's scale is for manufacturing output, which must convert
	 * the unit-based values in Electric to real-world values (in nanometers).
	 * @return project Setting to tell the scale between this technology and the real units.
	 */
	public Setting getScaleSetting() { return cacheScale; }

	/**
	 * Method to tell whether scaling is relevant for this Technology.
	 * Most technolgies produce drawings that are exact images of a final product.
	 * For these technologies (CMOS, bipolar, etc.) the "scale" from displayed grid
	 * units to actual dimensions is a relevant factor.
	 * Other technologies, such as schematics, artwork, and generic,
	 * are not converted to physical objects, and "scale" is not relevant no meaning for them.
	 * @return true if scaling is relevant for this Technology.
	 */
	public boolean isScaleRelevant() { return scaleRelevant; }

    /**
     * Method to set Technology resolution in IO/DRC tools.
     * This has to be stored per technology.
     * @param factory factory value
     */
    protected void setFactoryResolution(double factory)
    {
        prefResolution = Pref.makeDoublePref("ResolutionValueFor"+techName, prefs, factory);
    }

    /**
     * Method to set the technology resolution.
     * This is the minimum size unit that can be represented.
     * @param resolution new resolution value.
     */
	public void setResolution(double resolution)
	{
		if (prefResolution == null) setFactoryResolution(0);
		prefResolution.setDouble(resolution);
	}

    /**
     * Method to retrieve the resolution associated to the technology.
     * This is the minimum size unit that can be represented.
     * @return the technology's resolution value.
     */
    public double getResolution()
	{
        if (prefResolution == null) setFactoryResolution(0);
		return prefResolution.getDouble();
	}

	/**
	 * Method to get foundry in Tech Palette. Different foundry can define different DRC rules.
	 * The default is "Generic".
	 * @return the foundry to use in Tech Palette
	 */
	public String getPrefFoundry()
    {
        return cacheFoundry.getString().toUpperCase();
    }

	/**
	 * Returns project Setting to tell foundry for DRC rules.
	 * @return project Setting to tell the foundry for DRC rules.
	 */
	public Setting getPrefFoundrySetting() { return cacheFoundry; }

    /**
	 * Find the Foundry in this technology with a particular name. Protected so sub classes will use it
	 * @param name the name of the desired Foundry.
	 * @return the Foundry with the same name, or null if no Foundry matches.
	 */
	protected Foundry findFoundry(String name)
	{
		if (name == null) return null;

        for (Foundry f : foundries)
        {
            Foundry.Type t = f.getType();
            if (t.name().equalsIgnoreCase(name))
                return f;
        }
		return null;
	}

    /**
	 * Get an iterator over all of the Manufacturers.
	 * @return an iterator over all of the Manufacturers.
	 */
	public Iterator<Foundry> getFoundries()
	{
		return foundries.iterator();
	}

    /**
     * Method to create a new on this technology.
     * @param mode factory type
     * @param fileURL URL of xml file with description of rules
     * @param gdsLayers stirngs with definition of gds numbers for layers
     */
    protected void newFoundry(Foundry.Type mode, URL fileURL, String... gdsLayers) {
        Foundry foundry = new Foundry(this, mode, fileURL, gdsLayers);
        foundries.add(foundry);
    }

	/**
	 * Method to get the foundry index associated with this technology.
	 * @return the foundry index associated with this technology.
	 */
    public Foundry getSelectedFoundry()
    {
        String foundryName = getPrefFoundry();
        Foundry f = findFoundry(foundryName);
        if (f != null) return f;
        if (foundries.size() > 0)
        {
            f = foundries.get(0);
            System.out.println("Foundry '" + foundryName + "' not available in Technology '" +  this.getTechName() +
            "'. Setting '" + f.toString() + "' as foundry.");
            return f;
        }
        return f;
    }

    /**
     * Method to return the map from Layers of this Technology to their GDS names in current foundry.
     * Only Layers with non-empty GDS names are present in the map
     * @return the map from Layers to GDS names
     */
    public Map<Layer,String> getGDSLayers() {
        Foundry foundry = getSelectedFoundry();
        Map<Layer,String> gdsLayers = Collections.emptyMap();
        if (foundry != null) gdsLayers = foundry.getGDSLayers();
        return gdsLayers;
    }
    
	/**
	 * Sets the color map for transparent layers in this technology.
	 * Users should never call this method.
	 * It is set once by the technology during initialization.
	 * @param layers is an array of colors, one per transparent layer.
	 * This is expanded to a map that is 2 to the power "getNumTransparentLayers()".
	 * Color merging is computed automatically.
	 */
	protected void setFactoryTransparentLayers(Color [] layers)
	{
		// pull these values from preferences
		transparentLayers = layers.length;
		transparentColorPrefs = new Pref[transparentLayers];
		for(int i=0; i<layers.length; i++)
		{
			transparentColorPrefs[i] = Pref.makeIntPref("TransparentLayer"+(i+1)+"For"+techName, prefs, layers[i].getRGB());
			layers[i] = new Color(transparentColorPrefs[i].getInt());
		}
		setColorMapFromLayers(layers);
	}

	/**
	 * Method to reload the color map when the layer color preferences have changed.
	 */
	public static void cacheTransparentLayerColors()
	{
        // recache technology color information
        for(Iterator<Technology> it = getTechnologies(); it.hasNext(); )
        {
            Technology tech = it.next();
            for(Iterator<Layer> lIt = tech.getLayers(); lIt.hasNext(); )
            {
                Layer layer = lIt.next();
                layer.getGraphics().recachePrefs();
            }

            if (tech.transparentLayers <= 0) continue;
            Color [] layers = new Color[tech.transparentLayers];
            for(int i=0; i<tech.transparentLayers; i++)
            {
                layers[i] = new Color(tech.transparentColorPrefs[i].getInt());
            }
            tech.setColorMapFromLayers(layers);
        }
	}

	public Color [] getFactoryColorMap()
	{
        if (transparentLayers <= 0) return null;
        Color [] layers = new Color[transparentLayers];
        for(int i=0; i<transparentLayers; i++)
            layers[i] = new Color(transparentColorPrefs[i].getIntFactoryValue());
		Color [] map = getColorMap(layers);
		return map;
	}

	/**
	 * Returns the number of transparent layers in this technology.
	 * Informs the display system of the number of overlapping or transparent layers
	 * in use.
	 * @return the number of transparent layers in this technology.
	 * There may be 0 transparent layers in technologies that don't do overlapping,
	 * such as Schematics.
	 */
	public int getNumTransparentLayers() { return transparentLayers; }

	/**
	 * Sets the color map for transparent layers in this technology.
	 * @param map the color map for transparent layers in this technology.
	 * There must be a number of entries in this map equal to 2 to the power "getNumTransparentLayers()".
	 */
	public void setColorMap(Color [] map)
	{
		colorMap = map;
	}

	/**
	 * Method to normalize a color stored in a 3-long array.
	 * @param a the array of 3 doubles that holds the color.
	 * All values range from 0 to 1.
	 * The values are adjusted so that they are normalized.
	 */
	private void normalizeColor(double [] a)
	{
		double mag = Math.sqrt(a[0] * a[0] + a[1] * a[1] + a[2] * a[2]);
		if (mag > 1.0e-11f)
		{
			a[0] /= mag;
			a[1] /= mag;
			a[2] /= mag;
		}
	}

	/**
	 * Sets the color map from transparent layers in this technology.
	 * @param layers an array of colors, one per transparent layer.
	 * This is expanded to a map that is 2 to the power "getNumTransparentLayers()".
	 * Color merging is computed automatically.
	 */
	public void setColorMapFromLayers(Color [] layers)
	{
		// update preferences
		if (transparentColorPrefs != null)
		{
			for(int i=0; i<layers.length; i++)
			{
				Pref pref = transparentColorPrefs[i];
                if (layers[i] != null)
				    pref.setInt(layers[i].getRGB());
			}
		}
		Color [] map = getColorMap(layers);
		setColorMap(map);
	}

	private Color [] getColorMap(Color [] layers)
	{
		int numEntries = 1 << transparentLayers;
		Color [] map = new Color[numEntries];
		for(int i=0; i<numEntries; i++)
		{
			int r=200, g=200, b=200;
			boolean hasPrevious = false;
			for(int j=0; j<transparentLayers; j++)
			{
				if ((i & (1<<j)) == 0) continue;
				if (hasPrevious)
				{
					// get the previous color
					double [] lastColor = new double[3];
					lastColor[0] = r / 255.0;
					lastColor[1] = g / 255.0;
					lastColor[2] = b / 255.0;
					normalizeColor(lastColor);

					// get the current color
					double [] curColor = new double[3];
					curColor[0] = layers[j].getRed() / 255.0;
					curColor[1] = layers[j].getGreen() / 255.0;
					curColor[2] = layers[j].getBlue() / 255.0;
					normalizeColor(curColor);

					// combine them
					for(int k=0; k<3; k++) curColor[k] += lastColor[k];
					normalizeColor(curColor);
					r = (int)(curColor[0] * 255.0);
					g = (int)(curColor[1] * 255.0);
					b = (int)(curColor[2] * 255.0);
				} else
				{
					r = layers[j].getRed();
					g = layers[j].getGreen();
					b = layers[j].getBlue();
					hasPrevious = true;
				}
			}
			map[i] = new Color(r, g, b);
		}
		return map;
	}

	/**
	 * Method to get the factory design rules.
	 * Individual technologies subclass this to create their own rules.
	 * @return the design rules for this Technology.
	 * Returns null if there are no design rules in this Technology.
     */
    public XMLRules getFactoryDesignRules() {
        XMLRules rules = new XMLRules(this);

        Foundry foundry = getSelectedFoundry();
        List<DRCTemplate> rulesList = foundry.getRules();

        // load the DRC tables from the explanation table
        if (rulesList != null) {
            for(DRCTemplate rule : rulesList) {
                if (rule.ruleType != DRCTemplate.DRCRuleType.NODSIZ)
                    rules.loadDRCRules(this, foundry, rule);
            }
            for(DRCTemplate rule : rulesList) {
                if (rule.ruleType == DRCTemplate.DRCRuleType.NODSIZ)
                    rules.loadDRCRules(this, foundry, rule);
            }
        }
        
        if (xmlTech != null)
            resizeXml(rules);
        return rules;
    }

	/**
	 * Method to compare a Rules set with the "factory" set and construct an override string.
	 * @param origRules
	 * @param newRules
	 * @return a StringBuffer that describes any overrides.  Returns "" if there are none.
	 */
	public static StringBuffer getRuleDifferences(DRCRules origRules, DRCRules newRules)
	{
		return (new StringBuffer(""));
	}

	/**
	 * Method to be called from DRC:setRules
	 * @param newRules
	 */
	public void setRuleVariables(DRCRules newRules) {}

	/**
	 * Returns the color map for transparent layers in this technology.
	 * @return the color map for transparent layers in this technology.
	 * The number of entries in this map equals 2 to the power "getNumTransparentLayers()".
	 */
	public Color [] getColorMap() { return colorMap; }

	/**
	 * Returns the 0-based index of this Technology.
	 * Each Technology has a unique index that can be used for array lookup.
	 * @return the index of this Technology.
	 */
	public int getIndex() { return techIndex; }

	/**
	 * Method to determine whether a new technology with the given name would be legal.
	 * All technology names must be unique, so the name cannot already be in use.
	 * @param techName the name of the new technology that will be created.
	 * @return true if the name is valid.
	 */
//	private static boolean validTechnology(String techName)
//	{
//		if (Technology.findTechnology(techName) != null)
//		{
//			System.out.println("ERROR: Multiple technologies named " + techName);
//			return false;
//		}
//		return true;
//	}

	/**
	 * Method to determine the appropriate Technology to use for a Cell.
	 * @param cell the Cell to examine.
	 * @return the Technology for that cell.
	 */
	public static Technology whatTechnology(NodeProto cell)
	{
		Technology tech = whatTechnology(cell, null, 0, 0, null, 0, 0);
		return tech;
	}

	/**
	 * Method to determine the appropriate technology to use for a cell.
	 * The contents of the cell can be defined by the lists of NodeInsts and ArcInsts, or
	 * if they are null, then by the contents of the Cell.
	 * @param cellOrPrim the Cell to examine.
	 * @param nodeProtoList the list of prototypes of NodeInsts in the Cell.
	 * @param startNodeProto the starting point in the "nodeProtoList" array.
	 * @param endNodeProto the ending point in the "nodeProtoList" array.
	 * @param arcProtoList the list of prototypes of ArcInsts in the Cell.
	 * @param startArcProto the starting point in the "arcProtoList" array.
	 * @param endArcProto the ending point in the "arcProtoList" array.
	 * @return the Technology for that cell.
	 */
	public static Technology whatTechnology(NodeProto cellOrPrim, NodeProto [] nodeProtoList, int startNodeProto, int endNodeProto,
		ArcProto [] arcProtoList, int startArcProto, int endArcProto)
	{
		// primitives know their technology
		if (cellOrPrim instanceof PrimitiveNode)
			return(((PrimitiveNode)cellOrPrim).getTechnology());
		Cell cell = (Cell)cellOrPrim;

		// count the number of technologies
		int maxTech = 0;
		for(Iterator<Technology> it = Technology.getTechnologies(); it.hasNext(); )
		{
			Technology tech = it.next();
			if (tech.getIndex() > maxTech) maxTech = tech.getIndex();
		}
		maxTech++;

		// create an array of counts for each technology
		int [] useCount = new int[maxTech];
		for(int i=0; i<maxTech; i++) useCount[i] = 0;

		// count technologies of all primitive nodes in the cell
		if (nodeProtoList != null)
		{
			// iterate over the NodeProtos in the list
			for(int i=startNodeProto; i<endNodeProto; i++)
			{
				NodeProto np = nodeProtoList[i];
				if (np == null) continue;
				Technology nodeTech = np.getTechnology();
				if (np instanceof Cell)
				{
					Cell subCell = (Cell)np;
					if (subCell.isIcon())
						nodeTech = Schematics.tech;
				}
				if (nodeTech != null) useCount[nodeTech.getIndex()]++;
			}
		} else
		{
			for(Iterator<NodeInst> it = cell.getNodes(); it.hasNext(); )
			{
				NodeInst ni = it.next();
				NodeProto np = ni.getProto();
				Technology nodeTech = np.getTechnology();
				if (ni.isCellInstance())
				{
					Cell subCell = (Cell)np;
					if (subCell.isIcon())
						nodeTech = Schematics.tech;
				}
				if (nodeTech != null) useCount[nodeTech.getIndex()]++;
			}
		}

		// count technologies of all arcs in the cell
		if (arcProtoList != null)
		{
			// iterate over the arcprotos in the list
			for(int i=startArcProto; i<endArcProto; i++)
			{
				ArcProto ap = arcProtoList[i];
				if (ap == null) continue;
				useCount[ap.getTechnology().getIndex()]++;
			}
		} else
		{
			for(Iterator<ArcInst> it = cell.getArcs(); it.hasNext(); )
			{
				ArcInst ai = it.next();
				ArcProto ap = ai.getProto();
				useCount[ap.getTechnology().getIndex()]++;
			}
		}

		// find a concensus
		int best = 0;         Technology bestTech = null;
		int bestLayout = 0;   Technology bestLayoutTech = null;
		for(Iterator<Technology> it = Technology.getTechnologies(); it.hasNext(); )
		{
			Technology tech = it.next();

			// always ignore the generic technology
			if (tech == Generic.tech) continue;

			// find the most popular of ALL technologies
			if (useCount[tech.getIndex()] > best)
			{
				best = useCount[tech.getIndex()];
				bestTech = tech;
			}

			// find the most popular of the layout technologies
			if (!tech.isLayout()) continue;
			if (useCount[tech.getIndex()] > bestLayout)
			{
				bestLayout = useCount[tech.getIndex()];
				bestLayoutTech = tech;
			}
		}

		Technology retTech = null;
		if (cell.isIcon() || cell.getView().isTextView())
		{
			// in icons, if there is any artwork, use it
			if (useCount[Artwork.tech.getIndex()] > 0) return(Artwork.tech);

			// in icons, if there is nothing, presume artwork
			if (bestTech == null) return(Artwork.tech);

			// use artwork as a default
			retTech = Artwork.tech;
		} else if (cell.isSchematic())
		{
			// in schematic, if there are any schematic components, use it
			if (useCount[Schematics.tech.getIndex()] > 0) return(Schematics.tech);

			// in schematic, if there is nothing, presume schematic
			if (bestTech == null) return(Schematics.tech);

			// use schematic as a default
			retTech = Schematics.tech;
		} else
		{
			// use the current layout technology as the default
			retTech = curLayoutTech;
		}

		// if a layout technology was voted the most, return it
		if (bestLayoutTech != null) retTech = bestLayoutTech; else
		{
			// if any technology was voted the most, return it
			if (bestTech != null) retTech = bestTech; else
			{
//				// if this is an icon, presume the technology of its contents
//				cv = contentsview(cell);
//				if (cv != NONODEPROTO)
//				{
//					if (cv->tech == NOTECHNOLOGY)
//						cv->tech = whattech(cv);
//					retTech = cv->tech;
//				} else
//				{
//					// look at the contents of the sub-cells
//					foundicons = FALSE;
//					for(ni = cell->firstnodeinst; ni != NONODEINST; ni = ni->nextnodeinst)
//					{
//						np = ni->proto;
//						if (np == NONODEPROTO) continue;
//						if (np->primindex != 0) continue;
//
//						// ignore recursive references (showing icon in contents)
//						if (isiconof(np, cell)) continue;
//
//						// see if the cell has an icon
//						if (np->cellview == el_iconview) foundicons = TRUE;
//
//						// do not follow into another library
//						if (np->lib != cell->lib) continue;
//						onp = contentsview(np);
//						if (onp != NONODEPROTO) np = onp;
//						tech = whattech(np);
//						if (tech == gen_tech) continue;
//						retTech = tech;
//						break;
//					}
//					if (ni == NONODEINST)
//					{
//						// could not find instances that give information: were there icons?
//						if (foundicons) retTech = sch_tech;
//					}
//				}
			}
		}

		// give up and report the generic technology
		return retTech;
	}

    /**
     * Returns true if this Technology is layout technology.
     * @return true if this Technology is layout technology.
     */
    public boolean isLayout() {
        return this != Schematics.tech && this != Artwork.tech && this != Generic.tech;
    }

    /**
     * Compares Technologies by their names.
     * @param that the other Technology.
     * @return a comparison between the Technologies.
     */
	public int compareTo(Technology that)
	{
		return TextUtils.STRING_NUMBER_ORDER.compare(techName, that.techName);
	}

	/**
	 * Returns a printable version of this Technology.
	 * @return a printable version of this Technology.
	 */
	public String toString()
	{
		return "Technology " + techName;
	}

   /**
     * Method to check invariants in this Technology.
     * @exception AssertionError if invariants are not valid
     */
    private void check() {
        for (ArcProto ap: arcs.values()) {
            ap.check();
        }
    }

	///////////////////// Generic methods //////////////////////////////////////////////////////////////

	/**
	 * Method to change the design rules for layer "layername" layers so that
	 * the layers are at least "width" wide.  Affects the default arc width
	 * and the default pin size.
	 */
	protected void setLayerMinWidth(String layername, String rulename, double width)
	{
		// find the arc and set its default width
		ArcProto ap = findArcProto(layername);
		if (ap == null) return;

		boolean hasChanged = false;

        if (ap.getDefaultLambdaBaseWidth() != width)
//        if (ap.getDefaultLambdaFullWidth() != width + ap.getLambdaWidthOffset())
            hasChanged = true;

		// find the arc's pin and set its size and port offset
		PrimitiveNode np = ap.findPinProto();
		if (np == null) return;
		SizeOffset so = np.getProtoSizeOffset();
		double newWidth = width + so.getLowXOffset() + so.getHighXOffset();
		double newHeight = width + so.getLowYOffset() + so.getHighYOffset();

        if (np.getDefHeight() != newHeight || np.getDefWidth() != newWidth)
            hasChanged = true;

		PrimitivePort pp = (PrimitivePort)np.getPorts().next();
		EdgeH left = pp.getLeft();
		EdgeH right = pp.getRight();
		EdgeV bottom = pp.getBottom();
		EdgeV top = pp.getTop();
		double indent = newWidth / 2;

        if (left.getAdder() != indent || right.getAdder() != -indent ||
            top.getAdder() != -indent || bottom.getAdder() != indent)
            hasChanged = true;
		if (hasChanged)
		{
			// describe the error
            String errorMessage = "User preference of " + width + " overwrites original layer minimum size in layer '"
					+ layername + "', primitive '" + np.getName() + ":" + getTechShortName() + "' by rule " + rulename;
			if (Job.LOCALDEBUGFLAG) System.out.println(errorMessage);
		}
	}

    protected void setDefNodeSize(PrimitiveNode nty, double wid, double hei)
    {
        double xindent = (nty.getDefWidth() - wid) / 2;
		double yindent = (nty.getDefHeight() - hei) / 2;
		nty.setSizeOffset(new SizeOffset(xindent, xindent, yindent, yindent));  // bug 1040
    }

	/**
	 * Method to set the surround distance of layer "layer" from the via in node "nodename" to "surround".
	 */
//	protected void setLayerSurroundVia(PrimitiveNode nty, Layer layer, double surround)
//	{
//		// find the via size
//		double [] specialValues = nty.getSpecialValues();
//		double viasize = specialValues[0];
//		double layersize = viasize + surround*2;
//		double indent = (nty.getDefWidth() - layersize) / 2;
//
//		Technology.NodeLayer oneLayer = nty.findNodeLayer(layer, false);
//		if (oneLayer != null)
//		{
//			TechPoint [] points = oneLayer.getPoints();
//			EdgeH left = points[0].getX();
//			EdgeH right = points[1].getX();
//			EdgeV bottom = points[0].getY();
//			EdgeV top = points[1].getY();
//			left.setAdder(indent);
//			right.setAdder(-indent);
//			top.setAdder(-indent);
//			bottom.setAdder(indent);
//		}
//	}

    /********************* FOR GUI **********************/

    /** Temporary variable for holding names */         public static final Variable.Key TECH_TMPVAR = Variable.newKey("TECH_TMPVAR");

    /**
     * Method to retrieve correct group of elements for the palette.
     */
    public Object[][] getNodesGrouped()
    {
        // Check if some metal layers are not used
        if (nodeGroups == null)
        {
        	// compute palette information automatically
        	List<Object> things = new ArrayList<Object>();
        	for(Iterator<ArcProto> it = getArcs(); it.hasNext(); )
        	{
        		ArcProto ap = it.next();
        		if (!ap.isNotUsed()) things.add(ap);
        	}
        	for(Iterator<PrimitiveNode> it = getNodes(); it.hasNext(); )
        	{
        		PrimitiveNode np = it.next();
        		if (np.isNotUsed()) continue;
        		if (np.getFunction() == PrimitiveNode.Function.NODE) continue;
        		things.add(np);
        	}
        	things.add("Pure");
        	things.add("Misc.");
        	things.add("Cell");
        	int columns = (things.size()+13) / 14;
        	int rows = (things.size() + columns-1) / columns;
        	nodeGroups = new Object[rows][columns];
        	int rowPos = 0, colPos = 0;
        	for(Object obj : things)
        	{
        		nodeGroups[rowPos][colPos] = obj;
        		rowPos++;
        		if (rowPos >= rows)
        		{
        			rowPos = 0;
        			colPos++;
        		}
        	}
        }
        List <Object>list = new ArrayList<Object>(nodeGroups.length);
        for (int i = 0; i < nodeGroups.length; i++)
        {
            Object[] objs = nodeGroups[i];
            if (objs != null)
            {
                Object obj = objs[0];
                boolean valid = true;
                if (obj instanceof ArcProto)
                {
                    ArcProto ap = (ArcProto)obj;
                    valid = !ap.isNotUsed();
                }
                if (valid)
                    list.add(objs);
            }
        }
        Object[][] newMatrix = new Object[list.size()][nodeGroups[0].length];
        for (int i = 0; i < list.size(); i++)
        {
            Object[] objs = (Object[])list.get(i);
            for (int j = 0; j < objs.length; j++)
            {
                Object obj = objs[j];
                // Element is not used or first element in list is not used
                if ((obj instanceof PrimitiveNode && ((PrimitiveNode)obj).isNotUsed()))
                    obj = null;
                else if (obj instanceof List)
                {
                    List<?> l = (List)obj;
                    Object o = l.get(0);
                    if (o instanceof NodeInst)
                    {
                    	NodeInst ni = (NodeInst)o;
                        if (!ni.isCellInstance() && ((PrimitiveNode)ni.getProto()).isNotUsed())
                            obj = null;
                    }
                    else if (o instanceof PrimitiveNode)
                    {
                        if (((PrimitiveNode)o).isNotUsed())
                            obj = null;
                    }
                }
                newMatrix[i][j] = obj;
            }
        }
        return newMatrix;
    }

    /**
     * Method to create temporary nodes for the palette
     * @param np prototype of the node to place in the palette.
     * @param func function of the node (helps parameterize the node).
     * @param angle initial placement angle of the node.
     */
    public static NodeInst makeNodeInst(NodeProto np, PrimitiveNode.Function func, int angle, boolean display,
                                        String varName, double fontSize)
    {
        SizeOffset so = np.getProtoSizeOffset();
        Point2D pt = new Point2D.Double((so.getHighXOffset() - so.getLowXOffset()) / 2,
            (so.getHighYOffset() - so.getLowYOffset()) / 2);
		Orientation orient = Orientation.fromAngle(angle);
		AffineTransform trans = orient.pureRotate();
        trans.transform(pt, pt);
        NodeInst ni = NodeInst.makeDummyInstance(np, new EPoint(pt.getX(), pt.getY()), np.getDefWidth(), np.getDefHeight(), orient);
        np.getTechnology().setPrimitiveFunction(ni, func);
        np.getTechnology().setDefaultOutline(ni);

	    if (varName != null)
	    {
            ni.newVar(TECH_TMPVAR, varName, TextDescriptor.getNodeTextDescriptor().withDisplay(display).withRelSize(fontSize).withOff(0, -6));
	    }

        return ni;
    }

    /**
     * This is the most basic function to determine the widest wire and the parallel distance
     * that run along them. Done because MOSRules doesn't consider the parallel distance as input.
     */
    public double[] getSpacingDistances(Poly poly1, Poly poly2)
    {
        double size1 = poly1.getMinSize();
        double size2 = poly1.getMinSize();
        double length = 0;
        double wideS = (size1 > size2) ? size1 : size2;
        double [] results = new double[2];
        results[0] = wideS;
        results[1] = length;
        return results;
    }

    /**
     * Method to retrieve cached rules
     * @return cached design rules.
     */
    public DRCRules getCachedRules() {return cachedRules;}

    /**
     * Method to set cached rules
     */
    public void setCachedRules(DRCRules rules) {cachedRules = rules;}

    /**
     * This method determines if one of the polysilicon polygons is covered by a vth layer. Only implemented in 90nm
     * doesn't apply
     * @param polys
     * @param layers
     * @param geoms
     * @param ignoreCenterCuts
     * @return true if one of the polysilicon polygons is covered by a vth layer.
     */
    public boolean polyCoverByAnyVTLayer(Cell cell, DRCTemplate theRule, Technology tech, Poly[] polys, Layer[] layers,
                                         Geometric[] geoms, boolean ignoreCenterCuts) { return false; }

    /**
	 * Class to extend prefs so that changes to MOSIS CMOS options will update the display.
	 */
	public static class TechSetting extends Setting
	{
        private Technology tech;

        private TechSetting(String prefName, Pref.Group group, Technology tech, ProjSettingsNode xmlNode, String xmlName, String location, String description, Object factoryObj) {
            super(prefName, Technology.prefs, xmlNode, xmlName, location, description, factoryObj);
            if (tech == null)
                throw new NullPointerException();
            this.tech = tech;
        }

        @Override
		protected void setSideEffect()
		{
			//technologyChangedFromDatabase(tech, true);
            if (tech == null) return;
            tech.setState();
            reloadUIData();
		}

        private static void reloadUIData()
        {
			SwingUtilities.invokeLater(new Runnable()
            {
	            public void run()
                {
                // Primitives cached must be redrawn
                // recache display information for all cells that use this
                    User.technologyChanged();
                    UserInterface ui = Job.getUserInterface();
                    ui.loadComponentMenuForTechnology();
                    ui.repaintAllEditWindows();
                }
            });
        }

		public static Setting makeBooleanSetting(Technology tech, String name, String location, String description,
                                              ProjSettingsNode xmlNode, String xmlName,
                                              boolean factory)
		{
            return new TechSetting(name, Technology.prefs, tech, xmlNode, xmlName, location, description, Boolean.valueOf(factory));
		}

		public static Setting makeIntSetting(Technology tech, String name, String location, String description,
                                          ProjSettingsNode xmlNode, String xmlName,
                                          int factory)
		{
            return new TechSetting(name, Technology.prefs, tech, xmlNode, xmlName, location, description, Integer.valueOf(factory));
		}

        public static Setting makeStringSetting(Technology tech, String name, String location, String description,
                                             ProjSettingsNode xmlNode, String xmlName,
                                             String factory)
		{
            return new TechSetting(name, Technology.prefs, tech, xmlNode, xmlName, location, description, factory);
		}
	}

    // -------------------------- Project Settings -------------------------

    public ProjSettingsNode getProjectSettings() {
        ProjSettingsNode node = ProjSettings.getSettings().getNode(getTechName());
//        if (node == null) {
//            node = new ProjSettingsNode();
//            ProjSettings.getSettings().putNode(getTechName(), node);
//        }
        return node;
    }
}
