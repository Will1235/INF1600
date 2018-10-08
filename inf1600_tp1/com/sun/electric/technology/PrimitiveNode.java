/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: PrimitiveNode.java
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

import com.sun.electric.database.geometry.DBMath;
import com.sun.electric.database.geometry.Dimension2D;
import com.sun.electric.database.geometry.EPoint;
import com.sun.electric.database.hierarchy.EDatabase;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.NodeProtoId;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.prototype.PortProtoId;
import com.sun.electric.database.text.ArrayIterator;
import com.sun.electric.database.text.Name;
import com.sun.electric.database.text.Pref;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.tool.user.User;
import java.io.PrintWriter;
import java.util.Arrays;

import java.util.Iterator;
import java.util.List;
import java.util.HashMap;
import java.util.NoSuchElementException;

/**
 * A PrimitiveNode represents information about a NodeProto that lives in a
 * Technology.  It has a name, and several functions that describe how
 * to draw it
 */
public class PrimitiveNode implements NodeProtoId, NodeProto, Comparable<PrimitiveNode>
{
	/**
	 * Function is a typesafe enum class that describes the function of a NodeProto.
	 * Functions are technology-independent and include different types of transistors,
	 * contacts, and other circuit elements.
	 */
	public static enum Function
	{
		/**
		 * Describes a node with unknown behavior.
		 */
		UNKNOWN("unknown", "node"),
		/**
		 * Describes a single-layer pin.
		 * Pins connects wires of a single layer, have no geometry, and connect in the center of the node.
		 */
		PIN("pin", "pin"),
		/**
		 * Describes a two-layer contact.
		 * Contacts connects wires of two different layers in the center of the node.
		 */
		CONTACT("contact", "contact"),
		/**
		 * Describes a pure-layer node.
		 * Pure-layer nodes have a solid piece of geometry on a single layer.
		 */
		NODE("pure-layer-node", "plnode"),
		/**
		 * node a node that connects all ports.
		 */
		CONNECT("connection", "conn"),
		/**
		 * Describes a MOS enhancement transistor.
		 * It has gate on the first and third ports, the source on the second port, and the drain on the fourth port.
		 */
		TRANMOS("nMOS-transistor", "nmos"),
		/**
		 * Describes a MOS depletion transistor.
		 * It has gate on the first and third ports, the source on the second port, and the drain on the fourth port.
		 */
		TRADMOS("DMOS-transistor", "dmos"),
		/**
		 * Describes a MOS complementary transistor.
		 * It has gate on the first and third ports, the source on the second port, and the drain on the fourth port.
		 */
		TRAPMOS("pMOS-transistor", "pmos"),
		/**
		 * Describes a NPN junction transistor.
		 * It has base on the first port, emitter on the second port, and collector on the third port.
		 */
		TRANPN("NPN-transistor", "npn"),
		/**
		 * Describes a PNP junction transistor.
		 * It has base on the first port, emitter on the second port, and collector on the third port.
		 */
		TRAPNP("PNP-transistor", "pnp"),
		/**
		 * Describes a N-channel junction transistor.
		 * It has gate on the first port, source on the second port, and drain on the third port.
		 */
		TRANJFET("n-type-JFET-transistor", "njfet"),
		/**
		 * Describes a P-channel junction transistor.
		 * It has gate on the first port, source on the second port, and drain on the third port.
		 */
		TRAPJFET("p-type-JFET-transistor", "pjfet"),
		/**
		 * Describes a MESFET depletion transistor.
		 * It has gate on the first port, source on the second port, and drain on the third port.
		 */
		TRADMES("depletion-mesfet", "dmes"),
		/**
		 * Describes a MESFET enhancement transistor.
		 * It has gate on the first port, source on the second port, and drain on the third port.
		 */
		TRAEMES("enhancement-mesfet", "emes"),
		/**
		 * Describes a general-purpose transistor.
		 * It is defined self-referentially by the prototype name of the primitive.
		 */
		TRANSREF("prototype-defined-transistor","tref"),
		/**
		 * Describes an undetermined transistor.
		 * It has gate on the first port, source on the second port, and drain on the third port.
		 * The specific transistor type can be determined by examining the value from the NodeInst's "getTechSpecific" method.
		 */
		TRANS("transistor", "trans"),
		/**
		 * Describes a 4-port MOS enhancement transistor.
		 * It has gate on the first port, source on the second port, drain on the third port, and substrate on the fourth port.
		 */
		TRA4NMOS("4-port-nMOS-transistor", "nmos4p"),
		/**
		 * Describes a 4-port MOS depletion transistor.
		 * It has gate on the first port, source on the second port, drain on the third port, and substrate on the fourth port.
		 */
		TRA4DMOS("4-port-DMOS-transistor", "dmos4p"),
		/**
		 * Describes a 4-port MOS complementary transistor.
		 * It has gate on the first port, source on the second port, drain on the third port, and substrate on the fourth port.
		 */
		TRA4PMOS("4-port-pMOS-transistor", "pmos4p"),
		/**
		 * Describes a 4-port NPN junction transistor.
		 * It has base on the first port, emitter on the second port, collector on the third port, and substrate on the fourth port.
		 */
		TRA4NPN("4-port-NPN-transistor", "npn4p"),
		/**
		 * Describes a 4-port PNP junction transistor.
		 * It has base on the first port, emitter on the second port, collector on the third port, and substrate on the fourth port.
		 */
		TRA4PNP("4-port-PNP-transistor", "pnp4p"),
		/**
		 * Describes a 4-port N-channel junction transistor.
		 * It has gate on the first port, source on the second port, drain on the third port, and substrate on the fourth port.
		 */
		TRA4NJFET("4-port-n-type-JFET-transistor","njfet4p"),
		/**
		 * Describes a 4-port P-channel junction transistor.
		 * It has gate on the first port, source on the second port, drain on the third port, and substrate on the fourth port.
		 */
		TRA4PJFET("4-port-p-type-JFET-transistor","pjfet4p"),
		/**
		 * Describes a 4-port MESFET depletion transistor.
		 * It has gate on the first port, source on the second port, drain on the third port, and substrate on the fourth port.
		 */
		TRA4DMES("4-port-depletion-mesfet", "dmes4p"),
		/**
		 * Describes a 4-port MESFET enhancement transistor.
		 * It has gate on the first port, source on the second port, drain on the third port, and substrate on the fourth port.
		 */
		TRA4EMES("4-port-enhancement-mesfet",	"emes4p"),
		/**
		 * Describes a general-purpose transistor.
		 * It has gate on the first port, source on the second port, drain on the third port, and substrate on the fourth port.
		 * The specific transistor type can be determined by examining the value from the NodeInst's "getTechSpecific" method.
		 */
		TRANS4("4-port-transistor", "trans4p"),
		/**
		 * Describes a resistor.
		 */
		RESIST("resistor", "res"),
        /**
		 * Describes a poly resistor.
		 */
		PRESIST("poly-resistor", "pres"),
		/**
		 * Describes a well resistor.
		 */
		WRESIST("well-resistor", "wres"),
        /**
		 * Describes an esd device
		 */
		ESDDEVICE("esd-device", "esdd"),
        /**
		 * Describes a capacitor.
		 */
		CAPAC("capacitor", "cap"),
		/**
		 * Describes an electrolytic capacitor.
		 */
		ECAPAC("electrolytic-capacitor", "ecap"),
		/**
		 * Describes a diode.
		 */
		DIODE("diode", "diode"),
		/**
		 * Describes a zener diode.
		 */
		DIODEZ("zener-diode", "zdiode"),
		/**
		 * Describes an inductor.
		 */
		INDUCT("inductor", "ind"),
		/**
		 * Describes a meter.
		 */
		METER("meter", "meter"),
		/**
		 * Describes a transistor base.
		 */
		BASE("base", "base"),
		/**
		 * Describes a transistor emitter.
		 */
		EMIT("emitter", "emit"),
		/**
		 * Describes a transistor collector.
		 */
		COLLECT("collector", "coll"),
		/**
		 * Describes a buffer.
		 * It has input on the first port, clocking on the second port, and output on the third port.
		 */
		BUFFER("buffer", "buf"),
		/**
		 * Describes an AND gate.
		 * It has inputs on the first port and output on the second port.
		 */
		GATEAND("AND-gate", "and"),
		/**
		 * Describes an OR gate.
		 * It has inputs on the first port and output on the second port.
		 */
		GATEOR("OR-gate", "or"),
		/**
		 * Describes an XOR gate.
		 * It has inputs on the first port and output on the second port.
		 */
		GATEXOR("XOR-gate", "xor"),
		/**
		 * Describes a RS flip-flop with master-slave triggering.
		 */
		FLIPFLOPRSMS("flip-flop-RS-MS", "ffRSms"),
		/**
		 * Describes a RS flip-flop with positive triggering.
		 */
		FLIPFLOPRSP("flip-flop-RS-P", "ffRSp"),
		/**
		 * Describes a RS flip-flop with negative triggering.
		 */
		FLIPFLOPRSN("flip-flop-RS-N", "ffRSn"),
		/**
		 * Describes a JK flip-flop with master-slave triggering.
		 */
		FLIPFLOPJKMS("flip-flop-JK-MS", "ffJKms"),
		/**
		 * Describes a JK flip-flop with positive triggering.
		 */
		FLIPFLOPJKP("flip-flop-JK-P", "ffJKp"),
		/**
		 * Describes a JK flip-flop with negative triggering.
		 */
		FLIPFLOPJKN("flip-flop-JK-N", "ffJKn"),
		/**
		 * Describes a D flip-flop with master-slave triggering.
		 */
		FLIPFLOPDMS("flip-flop-D-MS", "ffDms"),
		/**
		 * Describes a D flip-flop with positive triggering.
		 */
		FLIPFLOPDP("flip-flop-D-P", "ffDp"),
		/**
		 * Describes a D flip-flop with negative triggering.
		 */
		FLIPFLOPDN("flip-flop-D-N", "ffDn"),
		/**
		 * Describes a T flip-flop with master-slave triggering.
		 */
		FLIPFLOPTMS("flip-flop-T-MS", "ffTms"),
		/**
		 * Describes a T flip-flop with positive triggering.
		 */
		FLIPFLOPTP("flip-flop-T-P", "ffTp"),
		/**
		 * Describes a T flip-flop with negative triggering.
		 */
		FLIPFLOPTN("flip-flop-T-N", "ffTn"),
		/**
		 * Describes a multiplexor.
		 */
		MUX("multiplexor", "mux"),
		/**
		 * Describes a power connection.
		 */
		CONPOWER("power", "pwr"),
		/**
		 * Describes a ground connection.
		 */
		CONGROUND("ground", "gnd"),
		/**
		 * Describes voltage or current source.
		 */
		SOURCE("source", "source"),
		/**
		 * Describes a substrate contact.
		 */
		SUBSTRATE("substrate", "substr"),
		/**
		 * Describes a well contact.
		 */
		WELL("well", "well"),
		/**
		 * Describes a pure artwork.
		 */
		ART("artwork", "art"),
		/**
		 * Describes an array.
		 */
		ARRAY("array", "array"),
		/**
		 * Describes an alignment object.
		 */
		ALIGN("align", "align"),
		/**
		 * Describes a current-controlled voltage source.
		 */
		CCVS("ccvs", "ccvs"),
		/**
		 * Describes a current-controlled current source.
		 */
		CCCS("cccs", "cccs"),
		/**
		 * Describes a voltage-controlled voltage source.
		 */
		VCVS("vcvs", "vcvs"),
		/**
		 * Describes a voltage-controlled current source.
		 */
		VCCS("vccs", "vccs"),
		/**
		 * Describes a transmission line.
		 */
		TLINE("transmission-line", "transm");
        
		private final String name;
		private final String shortName;
		private final Name basename;

		private Function(String name, String shortName)
		{
			this.name = name;
			this.shortName = shortName;
			this.basename = Name.findName(TextUtils.canonicString(shortName)+"@0").getBasename();
		}

		/**
		 * Method to return a List of all Functions that exist.
		 * @return a List of all Functions that exist.
		 */
		public static List<Function> getFunctions() { return Arrays.asList(Function.class.getEnumConstants()); }

		/**
		 * Returns a name of this Function.
		 * @return a name of this Function.
		 */
		public String getName() { return name; }

		/**
		 * Returns the constant name for this Function.
		 * Constant names are used when writing Java code, so they must be the same as the actual symbol name.
		 * @return the constant name for this Function.
		 */
		public String getConstantName() { return name(); }

		/**
		 * Returns a short name of this Function.
		 * @return a short name of this Function.
		 */
		public String getShortName() { return shortName; }

		/**
		 * Returns a base name of this Function for autonaming.
		 * @return a base name of this Function for autonaming.
		 */
		public Name getBasename() { return basename; }

        /**
         * Method to tell whether this function describes a capacitor (normal or electrolytic).
         * @return true if this function describes a capacitor (normal or electrolytic).
         */
        public boolean isCapacitor() {return (this == CAPAC || this == ECAPAC);}

        /**
         * Method to tell whether this function describes a resistor (normal, poly or nwell resistor).
         * @return true if this function describes a resistor (normal, poly or nwell resistor).
         */
        public boolean isResistor() {return (this == RESIST || this == PRESIST || this == WRESIST);}

        /**
         * Method to tell whether this function describes an ESD device.
         * @return true if this function describes an ESD device.
         */
        public boolean isESDDevice() {return this == ESDDEVICE;}

		/**
		 * Method to tell whether this function describes a transistor.
		 * @return true if this function describes a transistor.
		 */
		public boolean isTransistor()
		{
			if (this == TRANMOS || this == TRAPMOS || this == TRADMOS ||
				this == TRA4NMOS || this == TRA4PMOS || this == TRA4DMOS ||
				this == TRANPN || this == TRAPNP || this == TRANJFET || this == TRAPJFET || this == TRAEMES || this == TRADMES ||
				this == TRA4NPN || this == TRA4PNP || this == TRA4NJFET || this == TRA4PJFET || this == TRA4EMES || this == TRA4DMES ||
				this == TRANSREF || this == TRANS || this == TRANS4) return true;
			return false;
		}

		/**
		 * Method to tell whether this function describes a flip-flop.
		 * @return true if this function describes a flip-flop.
		 */
		public boolean isFlipFlop()
		{
			if (this == FLIPFLOPRSMS || this == FLIPFLOPRSP || this == FLIPFLOPRSN ||
				this == FLIPFLOPJKMS || this == FLIPFLOPJKP || this == FLIPFLOPJKN ||
				this == FLIPFLOPDMS || this == FLIPFLOPDP || this == FLIPFLOPDN ||
				this == FLIPFLOPTMS || this == FLIPFLOPTP || this == FLIPFLOPTN) return true;
			return false;
		}

		/**
		 * Returns a printable version of this Function.
		 * @return a printable version of this Function.
		 */
		public String toString() { return name; }
	}

	// constants used in the "specialType" field
	/** Defines a normal node. */					public static final int NORMAL = 0;
	/** Defines a serpentine transistor. */			public static final int SERPTRANS = 1;
	/** Defines a polygonal transistor. */			public static final int POLYGONAL = 2;

	/** set if nonmanhattan instances shrink */				private static final int NODESHRINK =           01;
	/** set if instances can be wiped */					private static final int ARCSWIPE =          01000;
	/** set if node is to be kept square in size */			private static final int NSQUARE =           02000;
	/** primitive can hold trace information */				private static final int HOLDSTRACE =        04000;
	/** set if this primitive can be zero-sized */			private static final int CANBEZEROSIZE =    010000;
	/** set to erase if connected to 1 or 2 arcs */			private static final int WIPEON1OR2 =       020000;
	/** set if primitive is lockable (cannot move) */		private static final int LOCKEDPRIM =       040000;
	/** set if primitive is selectable by edge, not area */	private static final int NEDGESELECT =     0100000;
	/** set if nonmanhattan arcs on this shrink */			private static final int ARCSHRINK =       0200000;
	/** set if nonmanhattan arcs on this shrink */			private static final int NINVISIBLE =      0400000;
	/** set if node will be considered in palette */        private static final int SKIPSIZEINPALETTE =    01000000;
	/** set if not used (don't put in menu) */				private static final int NNOTUSED =       02000000;
    /** set if node is a low vt transistor */               public static final int LOWVTBIT =          010;
    /** set if node is a high vt transistor */              public static final int HIGHVTBIT =         020;
    /** set if node is a native transistor */               public static final int NATIVEBIT =         040;
    /** set if node is a od18 transistor */                 public static final int OD18BIT =          0100;
    /** set if node is a od25 transistor */                 public static final int OD25BIT =          0200;
    /** set if node is a od33 transistor */                 public static final int OD33BIT =          0400;

	// --------------------- private data -----------------------------------
	
	/** The name of this PrimitiveNode. */			private String protoName;
	/** The full name of this PrimitiveNode. */		private String fullName;
	/** This PrimitiveNode's Technology. */			private Technology tech;
	/** The function of this PrimitiveNode. */		private Function function;
	/** layers describing this primitive */			private Technology.NodeLayer [] layers;
	/** electrical layers describing this */		private Technology.NodeLayer [] electricalLayers;
	/** PrimitivePorts on the PrimitiveNode. */		private PrimitivePort[] primPorts;
	/** flag bits */								private int userBits;
	/** Global index of this PrimitiveNode. */		private int globalPrimNodeIndex;
    /** Index of this PrimitiveNode per tech */     private int techPrimNodeIndex = -1;
	/** special type of unusual primitives */		private int specialType;
	/** special factors for unusual primitives */	private double[] specialValues;
    /** true if contains MULTICUTBOX layers */      private int numMultiCuts;
    /** minimum width and height rule */            private NodeSizeRule minNodeSize;
    /** size corrector */                           EPoint sizeCorrector;
	/** offset from database to user */				private SizeOffset offset;
	/** amount to automatically grow to fit arcs */	private Dimension2D autoGrowth;

	/** counter for enumerating primitive nodes */	private static int primNodeNumber = 0;
	/** Pref map for node width. */					private static HashMap<PrimitiveNode,Pref> defaultExtendXPrefs = new HashMap<PrimitiveNode,Pref>();
	/** Pref map for node height. */				private static HashMap<PrimitiveNode,Pref> defaultExtendYPrefs = new HashMap<PrimitiveNode,Pref>();

	// ------------------ private and protected methods ----------------------

	/**
	 * The constructor is never called externally.  Use the factory "newInstance" instead.
	 */
	protected PrimitiveNode(String protoName, Technology tech, EPoint sizeCorrector, double defWidth, double defHeight,
		SizeOffset offset, Technology.NodeLayer [] layers)
	{
		// things in the base class
		if (!Technology.jelibSafeName(protoName))
			System.out.println("PrimitiveNode name " + protoName + " is not safe to write in the JELIB");
		this.protoName = protoName;
		this.fullName = tech.getTechName() + ":" + protoName;
		this.function = Function.UNKNOWN;

		// things in this class
		this.tech = tech;
		this.layers = layers;
		this.electricalLayers = null;
		this.userBits = 0;
		specialType = NORMAL;
        this.sizeCorrector = sizeCorrector;
		setFactoryDefSize(defWidth, defHeight);
		if (offset == null) offset = new SizeOffset(0,0,0,0);
		this.offset = offset;
		this.autoGrowth = null;
		this.minNodeSize = null;
		globalPrimNodeIndex = primNodeNumber++;

        int numMultiCuts = 0;
        for (Technology.NodeLayer nodeLayer: layers) {
            if (nodeLayer.getRepresentation() == Technology.NodeLayer.MULTICUTBOX)
                numMultiCuts++;
        }
        this.numMultiCuts = numMultiCuts;
        
		// add to the nodes in this technology
		tech.addNodeProto(this);
	}

	// ------------------------- public methods -------------------------------

	/**
	 * Method to create a new PrimitiveNode from the parameters.
     * Size corrector of PrimitiveNode is determined from width and height.
	 * @param protoName the name of the PrimitiveNode.
	 * Primitive names may not contain unprintable characters, spaces, tabs, a colon (:), semicolon (;) or curly braces ({}).
	 * @param tech the Technology of the PrimitiveNode.
	 * @param width the width of the PrimitiveNode.
	 * @param height the height of the PrimitiveNode.
	 * @param offset the offset from the edges of the reported/selected part of the PrimitiveNode.
	 * @param layers the Layers that comprise the PrimitiveNode.
	 * @return the newly created PrimitiveNode.
	 */
	public static PrimitiveNode newInstance(String protoName, Technology tech, double width, double height,
		SizeOffset offset, Technology.NodeLayer [] layers)
	{
        return newInstance(protoName, tech, EPoint.fromLambda(-0.5*width, -0.5*height), width, height, offset, layers);
	}

	/**
	 * Method to create a new PrimitiveNode from the parameters.
     * PrimitiveNode has zero size corrector.
	 * @param protoName the name of the PrimitiveNode.
	 * Primitive names may not contain unprintable characters, spaces, tabs, a colon (:), semicolon (;) or curly braces ({}).
	 * @param tech the Technology of the PrimitiveNode.
	 * @param width the width of the PrimitiveNode.
	 * @param height the height of the PrimitiveNode.
	 * @param offset the offset from the edges of the reported/selected part of the PrimitiveNode.
	 * @param layers the Layers that comprise the PrimitiveNode.
	 * @return the newly created PrimitiveNode.
	 */
	public static PrimitiveNode newInstance0(String protoName, Technology tech, double width, double height,
		SizeOffset offset, Technology.NodeLayer [] layers)
	{
        return newInstance(protoName, tech, EPoint.ORIGIN, width, height, offset, layers);
	}

	/**
	 * Method to create a new PrimitiveNode from the parameters.
	 * @param protoName the name of the PrimitiveNode.
	 * Primitive names may not contain unprintable characters, spaces, tabs, a colon (:), semicolon (;) or curly braces ({}).
	 * @param tech the Technology of the PrimitiveNode.
     * @param sizeCorrector size corrector for the PrimitiveNode,
	 * @param width the width of the PrimitiveNode.
	 * @param height the height of the PrimitiveNode.
	 * @param offset the offset from the edges of the reported/selected part of the PrimitiveNode.
	 * @param layers the Layers that comprise the PrimitiveNode.
	 * @return the newly created PrimitiveNode.
	 */
	static PrimitiveNode newInstance(String protoName, Technology tech, EPoint sizeCorrector, double width, double height,
		SizeOffset offset, Technology.NodeLayer [] layers)
	{
		// check the arguments
		if (tech.findNodeProto(protoName) != null)
		{
			System.out.println("Error: technology " + tech.getTechName() + " has multiple nodes named " + protoName);
			return null;
		}
		if (width < 0.0 || height < 0.0)
		{
			System.out.println("Error: technology " + tech.getTechName() + " node " + protoName + " has negative size");
			return null;
		}

		PrimitiveNode pn = new PrimitiveNode(protoName, tech, sizeCorrector, width, height, offset, layers);
		return pn;
	}

    /**
     * Returns PrimitivePort in this PrimitiveNode with specified chronological index.
     * @param chronIndex chronological index of PrimitivePort.
     * @return PrimitivePort whith specified chronological index.
     * @throws ArrayIndexOutOfBoundsException if no such PrimitivePort.
     */
    public PrimitivePort getPortId(int chronIndex) { return primPorts[chronIndex]; }
    
   /**
     * Method to return the NodeProto representing NodeProtoId in the specified EDatabase.
     * @param database EDatabase where to get from.
     * PrimitiveNodes are shared among threads, so this method returns this PrimitiveNode.
	 * @return this.
     */
    public PrimitiveNode inDatabase(EDatabase database) { return this; }
    
    /** Method to return NodeProtoId of this NodeProto.
     * NodeProtoId identifies NodeProto independently of threads.
     * PrimitiveNodes are shared among threads, so this method returns this PrimitiveNode.
     * @return NodeProtoId of this NodeProto.
     */
    public PrimitiveNode getId() { return this; }
    
	/**
	 * Method to return the name of this PrimitiveNode in the Technology.
	 * @return the name of this PrimitiveNode.
	 */
	public String getName() { return protoName; }

	/**
	 * Method to return the full name of this PrimitiveNode.
	 * Full name has format "techName:primName"
	 * @return the full name of this PrimitiveNode.
	 */
	public String getFullName() { return fullName; }

	/**
	 * Method to set the function of this PrimitiveNode.
	 * The Function is a technology-independent description of the behavior of this PrimitiveNode.
	 * @param function the new function of this PrimitiveNode.
	 */
	public void setFunction(Function function) { checkChanging(); this.function = function; }

	/**
	 * Method to return the function of this PrimitiveNode.
	 * The Function is a technology-independent description of the behavior of this PrimitiveNode.
	 * @return the function of this PrimitiveNode.
	 */
	public Function getFunction() { return function; }

	/**
	 * Method to return the function of this PrimitiveNode, grouped according to its
	 * general function.
	 * For example, all transistors return the same value.
	 * @return the group function of this PrimitiveNode.
	 */
	public Function getGroupFunction()
	{
		if (function == Function.TRANMOS || function == Function.TRA4NMOS ||
			function == Function.TRAPMOS || function == Function.TRA4PMOS ||
			function == Function.TRADMOS || function == Function.TRA4DMOS ||
			function == Function.TRANPN || function == Function.TRA4NPN ||
			function == Function.TRAPNP || function == Function.TRA4PNP ||
			function == Function.TRANJFET || function == Function.TRA4NJFET ||
			function == Function.TRAPJFET || function == Function.TRA4PJFET ||
			function == Function.TRADMES || function == Function.TRA4DMES ||
			function == Function.TRAEMES || function == Function.TRA4EMES ||
			function == Function.TRANS4)
				return Function.TRANS;
		if (function.isResistor() ||
			function.isCapacitor() ||
            function == Function.DIODE || function == Function.DIODEZ || function == Function.INDUCT)
				return Function.INDUCT;
		if (function == Function.CCVS || function == Function.CCCS ||
			function == Function.VCVS || function == Function.VCCS ||
			function == Function.TLINE)
				return Function.TLINE;
		if (function == Function.BASE || function == Function.EMIT ||
			function == Function.COLLECT)
				return Function.COLLECT;
		if (function == Function.BUFFER || function == Function.GATEAND ||
			function == Function.GATEOR || function == Function.MUX ||
			function == Function.GATEXOR)
				return Function.GATEXOR;
		if (function == Function.CONPOWER || function == Function.CONGROUND)
			return Function.CONGROUND;
		if (function == Function.METER || function == Function.SOURCE)
			return Function.SOURCE;
		if (function == Function.SUBSTRATE || function == Function.WELL)
			return Function.WELL;
		return function;
	}

	/**
	 * Method to return the list of Layers that comprise this PrimitiveNode.
	 * @return the list of Layers that comprise this PrimitiveNode.
	 */
	public Technology.NodeLayer [] getLayers() { return layers; }

    /**
     * Method to reset the list of Layers that comprise this PrimitiveNode.
     * @param layers
     */
    public void setLayers(Technology.NodeLayer [] layers) { this.layers = layers; }

	/**
	 * Method to return an iterator over the layers in this PrimitiveNode.
	 * @return an iterator over the layers in this PrimitiveNode.
	 */
	public Iterator<Layer> getLayerIterator()
	{
		return new NodeLayerIterator(layers);
	}

	/** 
	 * Iterator for Layers on this NodeProto
	 */ 
	private static class NodeLayerIterator implements Iterator<Layer> 
	{ 
		Technology.NodeLayer [] array; 
		int pos; 

		public NodeLayerIterator(Technology.NodeLayer [] a) 
		{ 
			array = a; 
			pos = 0; 
		} 

		public boolean hasNext() 
		{ 
			return pos < array.length; 
		} 

		public Layer next() throws NoSuchElementException
		{ 
			if (pos >= array.length) 
				throw new NoSuchElementException(); 
			return array[pos++].getLayer(); 
		} 

		public void remove() throws UnsupportedOperationException, IllegalStateException 
		{ 
			throw new UnsupportedOperationException(); 
		}
	}

	/**
	 * Method to return the list of electrical Layers that comprise this PrimitiveNode.
	 * Like the list returned by "getLayers", the results describe this PrimitiveNode,
	 * but each layer is tied to a specific port on the node.
	 * If any piece of geometry covers more than one port,
	 * it must be split for the purposes of an "electrical" description.<BR>
	 * For example, the MOS transistor has 2 layers: Active and Poly.
	 * But it has 3 electrical layers: Active, Active, and Poly.
	 * The active must be split since each half corresponds to a different PrimitivePort on the PrimitiveNode.
	 * @return the list of electrical Layers that comprise this PrimitiveNode.
	 */
	public Technology.NodeLayer [] getElectricalLayers() { return electricalLayers; }

	/**
	 * Method to set the list of electrical Layers that comprise this PrimitiveNode.
	 * Like the list returned by "getLayers", the results describe this PrimitiveNode,
	 * but each layer is tied to a specific port on the node.
	 * If any piece of geometry covers more than one port,
	 * it must be split for the purposes of an "electrical" description.<BR>
	 * For example, the MOS transistor has 2 layers: Active and Poly.
	 * But it has 3 electrical layers: Active, Active, and Poly.
	 * The active must be split since each half corresponds to a different PrimitivePort on the PrimitiveNode.
	 * @param electricalLayers the list of electrical Layers that comprise this PrimitiveNode.
	 */
	public void setElectricalLayers(Technology.NodeLayer [] electricalLayers) {	this.electricalLayers = electricalLayers; }

	/**
	 * Method to find the NodeLayer on this PrimitiveNode with a given Layer.
	 * If there are more than 1 with the given Layer, the first is returned.
	 * @param layer the Layer to find.
	 * @return the NodeLayer that has this Layer.
	 */
	public Technology.NodeLayer findNodeLayer(Layer layer, boolean electrical)
	{
        // Give higher priority to electrical layers
        Technology.NodeLayer[] nodes = (electrical) ? electricalLayers : layers;

        if (nodes != null)
        {
            for(int j=0; j<nodes.length; j++)
            {
                Technology.NodeLayer oneLayer = nodes[j];
                if (oneLayer.getLayer() == layer) return oneLayer;
            }
        }
		return null;
	}

    /**
     * Tells whether this PrimitiveNode has NodeLayer with MULTICUTBOX representation.
     * For now, multicut primitives and resistor primitives have such NodeLayers.
     * @return true if this PrimitiveNode has NodeLayer with MULTICUTBOX representation.
     */
    public boolean hasMultiCuts() { return numMultiCuts > 0; }
    
    /**
     * Find a NodeLayer of this PrimitiveNode has NodeLayer with MULTICUTBOX representation.
     * If no such NodeLayer exists, returns null, if many - returns any of them..
     * @return a NodeLayer of this PrimitiveNode has NodeLayer with MULTICUTBOX representation.
     */
    public Technology.NodeLayer findMulticut() {
        for (Technology.NodeLayer nl: layers) {
            if (nl.getRepresentation() == Technology.NodeLayer.MULTICUTBOX)
                return nl;
        }
        return null;
    }
    
    /**
     * Tells whether this PrimitiveNode is multicut, i.e. it has exactly one NodeLayer with MULTICUTBOX representation,
     * @return true if this PrimitiveNode is multicut.
     */
    public boolean isMulticut() { return numMultiCuts == 1; }
    
	/**
	 * Abstract method to return the default rotation for new instances of this PrimitiveNode.
	 * @return the angle, in tenth-degrees to use when creating new NodeInsts of this PrimitiveNode.
	 * If the value is 3600 or greater, it means that X should be mirrored.
	 */
	public int getDefPlacementAngle()
	{
		int defAngle = User.getNewNodeRotation();
		return defAngle;
	}

	/**
	 * Method to return the Pref that describes the defaut width of this PrimitiveNode.
	 * @param factoryExtendX the "factory" default extend of this PrimitiveNode over minimal width.
	 * @return a Pref that stores the proper default width of this PrimitiveNode.
	 */
	private Pref getNodeProtoExtendXPref(double factoryExtendX)
	{
		Pref pref = defaultExtendXPrefs.get(this);
		if (pref == null)
		{
			pref = Pref.makeDoublePref("DefaultExtendXFor" + protoName + "IN" + tech.getTechName(), Technology.getTechnologyPreferences(), factoryExtendX);
			defaultExtendXPrefs.put(this, pref);
		}
		return pref;
	}

	/**
	 * Method to return the Pref that describes the defaut height of this PrimitiveNode.
	 * @param factoryExtendY the "factory" default extend of this PrimitiveNode over minimal height.
	 * @return a Pref that stores the proper default height of this PrimitiveNode.
	 */
	private Pref getNodeProtoExtendYPref(double factoryExtendY)
	{
		Pref pref = defaultExtendYPrefs.get(this);
		if (pref == null)
		{
			pref = Pref.makeDoublePref("DefaultExtendYFor" + protoName + "IN" + tech.getTechName(), Technology.getTechnologyPreferences(), factoryExtendY);
			defaultExtendYPrefs.put(this, pref);
		}
		return pref;
	}

	/**
	 * Method to set the factory-default width of this PrimitiveNode.
	 * This is only called during construction.
	 * @param defWidth the factory-default width of this PrimitiveNode.
	 * @param defHeight the factory-default height of this PrimitiveNode.
	 */
	protected void setFactoryDefSize(double defWidth, double defHeight)
	{
		getNodeProtoExtendXPref(DBMath.round(0.5*defWidth + sizeCorrector.getLambdaX()));
		getNodeProtoExtendYPref(DBMath.round(0.5*defHeight + sizeCorrector.getLambdaY()));
	}

	/**
	 * Method to set the default size of this PrimitiveNode.
	 * @param defWidth the new default width of this PrimitiveNode.
	 * @param defHeight the new default height of this PrimitiveNode.
	 */
	public void setDefSize(double defWidth, double defHeight)
	{
		getNodeProtoExtendXPref(0).setDouble(DBMath.round(0.5*defWidth + sizeCorrector.getLambdaX()));
		getNodeProtoExtendYPref(0).setDouble(DBMath.round(0.5*defHeight + sizeCorrector.getLambdaY()));
	}

	/**
	 * Method to return the default width of this PrimitiveNode.
	 * @return the default width of this PrimitiveNode.
	 */
	public double getDefWidth() { return 2*(getNodeProtoExtendXPref(0).getDouble() - sizeCorrector.getLambdaX()); }

	/**
	 * Method to return the default height of this PrimitiveNode.
	 * @return the default height of this PrimitiveNode.
	 */
	public double getDefHeight() { return 2*(getNodeProtoExtendYPref(0).getDouble() - sizeCorrector.getLambdaY()); }

	/**
	 * Method to get the size offset of this PrimitiveNode.
	 * To get the SizeOffset for a specific NodeInst, use Technology.getSizeOffset(ni).
	 * Use this method only to get the SizeOffset of a PrimitiveNode.
	 * @return the size offset of this PrimitiveNode.
	 */
	public SizeOffset getProtoSizeOffset() { return offset; }

//	/**
//	 * Method to return the minimum width of this PrimitiveNode.
//	 * @return the minimum width of this PrimitiveNode.
//	 */
//	public double getMinWidth() { return minNodeSize.getWidth(); }
//
//	/**
//	 * Method to return the minimum height of this PrimitiveNode.
//	 * @return the minimum height of this PrimitiveNode.
//	 */
//	public double getMinHeight() { return minNodeSize.getHeight(); }

	/**
	 * Method to return the minimum size rule for this PrimitiveNode.
	 * @return the minimum size rule for this PrimitiveNode.
	 */
	public NodeSizeRule getMinSizeRule() { return minNodeSize; }

	/**
	 * Method to set the minimum height of this PrimitiveNode.
	 * @param minHeight the minimum height of this PrimitiveNode.
	 */
	public void setMinSize(double minWidth, double minHeight, String minSizeRule)
	{
        setSizeCorrector(minWidth, minHeight);
        minNodeSize = new NodeSizeRule(minWidth, minHeight, minSizeRule);
    }
    
    public void setSizeCorrector(double refWidth, double refHeight) {
        sizeCorrector = EPoint.fromLambda(-0.5*refWidth, -0.5*refHeight);
    }

	/**
	 * Method to set the size offset of this PrimitiveNode.
	 * @param offset the size offset of this PrimitiveNode.
	 */
	public void setSizeOffset(SizeOffset offset) { this.offset = offset; }

	/**
	 * Method to set the auto-growth factor on this PrimitiveNode.
	 * The auto-growth factor is the amount to exand the node when new arcs
	 * want to connect to an expandable port and there is no room for the arcs.
	 * The only nodes that have auto-growth factors are the AND, OR, XOR, SWITCH, and MUX
	 * nodes of the Schematics technology.
	 * These nodes have ports that can accomodate any number of arcs.
	 * @param dX the X amount to grow this PrimitiveNode when arcs don't fit.
	 * @param dY the Y amount to grow this PrimitiveNode when arcs don't fit.
	 */
	public void setAutoGrowth(double dX, double dY) { autoGrowth = new Dimension2D.Double(dX, dY); }

	/**
	 * Method to get the auto-growth factor for this PrimitiveNode.
	 * The auto-growth factor is the amount to exand the node when new arcs
	 * want to connect to an expandable port and there is no room for the arcs.
	 * The only nodes that have auto-growth factors are the AND, OR, XOR, SWITCH, and MUX
	 * nodes of the Schematics technology.
	 * These nodes have ports that can accomodate any number of arcs.
	 * @return the amount to grow this PrimitiveNode when arcs don't fit.
	 */
	public Dimension2D getAutoGrowth() { return autoGrowth; }

	/**
	 * Method to return the Technology of this PrimitiveNode.
	 * @return the Technology of this PrimitiveNode.
	 */
	public Technology getTechnology() { return tech; }

	/**
	 * Method to add an array of PrimitivePorts to this PrimitiveNode.
	 * The method is only used during initialization.
	 * @param ports the array of PrimitivePorts to add.
	 */
	public void addPrimitivePorts(PrimitivePort [] ports)
	{
		assert primPorts == null : this + " addPrimitivePorts twice";
		primPorts = ports.clone();
//		Arrays.sort(primPorts);
		for (int i = 0; i < primPorts.length; i++)
		{
			primPorts[i].setPortIndex(this, i);
// 			if (i > 0)
// 				assert primPorts[i - 1].compareTo(primPorts[i]) < 0;
		}
	}

	/**
	 * Method to find the PortProto that has a particular name.
	 * @return the PortProto, or null if there is no PortProto with that name.
	 */
	public PortProto findPortProto(String name)
	{
        if (name == null) return null;
		return findPortProto(Name.findName(name));
	}

	/**
	 * Method to find the PortProto that has a particular Name.
	 * @return the PortProto, or null if there is no PortProto with that name.
	 */
	public PortProto findPortProto(Name name)
	{
        if (name == null) return null;
		String nameString = name.canonicString();
		for (int i = 0; i < primPorts.length; i++)
		{
			PrimitivePort pp = primPorts[i];
			if (pp.getNameKey().canonicString() == nameString)
				return pp;
		}
		return null;
	}

	/**
	 * Method to return an iterator over all PortProtos of this NodeProto.
	 * @return an iterator over all PortProtos of this NodeProto.
	 */
	public Iterator<PortProto> getPorts()
	{
		return ArrayIterator.iterator((PortProto[])primPorts);
	}

	/**
	 * Method to return an iterator over all PrimitivePorts of this PrimitiveNode.
	 * @return an iterator over all PrimitvePorts of this NodeProto.
	 */
	public Iterator<PrimitivePort> getPrimitivePorts()
	{
		return ArrayIterator.iterator(primPorts);
	}

	/**
	 * Method to return the number of PortProtos on this NodeProto.
	 * @return the number of PortProtos on this NodeProto.
	 */
	public int getNumPorts()
	{
		return primPorts.length;
	}

	/**
	 * Method to return the PortProto at specified position.
	 * @param portIndex specified position of PortProto.
	 * @return the PortProto at specified position..
	 */
	public final PrimitivePort getPort(int portIndex)
	{
		return primPorts[portIndex];
	}

	/**
	 * Method to return the PortProto by thread-independent PortProtoId.
	 * @param portProtoId thread-independent PortProtoId.
	 * @return the PortProto.
     * @throws IllegalArgumentException if portProtoId is not from this NodeProto.
	 */
	public PrimitivePort getPort(PortProtoId portProtoId) {
        if (portProtoId.getParentId() != this) throw new IllegalArgumentException();
        PrimitivePort pp = (PrimitivePort)portProtoId;
        assert primPorts[pp.getPortIndex()] == pp;
        return pp;
    }

	/**
	 * Method to return the PrimitivePort on this PrimitiveNode that can connect to an arc of the specified type.
	 * The method finds a PrimitivePort that can make the connection.
	 * @param arc the type of arc to connect to an instance of this PrimitiveNode.
	 * @return a PrimitivePort that can connect to this type of ArcProto.
	 * Returns null if this ArcProto cannot connect to anything on this PrimitiveNode.
	 */
	public PrimitivePort connectsTo(ArcProto arc)
	{
		for (int i = 0; i < primPorts.length; i++)
		{
			PrimitivePort pp = primPorts[i];
			if (pp.connectsTo(arc))
				return pp;
		}
		return null;
	}

	/**
	 * Method to return the special type of this PrimitiveNode.
	 * It can be one of NORMAL, SERPTRANS, POLYGONAL, or MULTICUT.
	 * @return the special type of this PrimitiveNode.
	 */
	public int getSpecialType() { return specialType; }

	/**
	 * Method to set the special type of this PrimitiveNode.
	 * @param specialType the newspecial type of this PrimitiveNode.
	 * It can be NORMAL, SERPTRANS, POLYGONAL, or MULTICUT.
	 */
	public void setSpecialType(int specialType) { this.specialType = specialType; }

	/**
	 * Method to return the name of a special PrimitiveNode type.
	 * @param t the integer special type.
	 * @return the name of that type.
	 */
	public static String getSpecialTypeName(int t)
	{
		if (t == NORMAL) return "normal";
		if (t == SERPTRANS) return "serp-trans";
		if (t == POLYGONAL) return "outline";
		return "?";
	}
	/**
	 * Method to return the special values stored on this PrimitiveNode.
	 * The special values are an array of integers that describe unusual features of the PrimitiveNode.
	 * They are only relevant for certain specialType cases:
	 * <UL>
	 * <LI>for MULTICUT:
	 *   <UL>
	 *   <LI>cut size is [0] x [1]
	 *   <LI>cut indented [2] x [3] from highlighting
	 *   <LI>cuts spaced [4] apart for 1-dimensional contact
	 *   <LI>cuts spaced [5] apart for 2-dimensional contact
	 *   </UL>
	 * <LI>for SERPTRANS:
	 *   <UL>
	 *   <LI>layer count is [0]
	 *   <LI>active port inset [1] from end of serpentine path
	 *   <LI>active port is [2] from poly edge
	 *   <LI>poly width is [3]
	 *   <LI>poly port inset [4] from poly edge
	 *   <LI>poly port is [5] from active edge
	 *   </UL>
	 * @return the special values stored on this PrimitiveNode.
	 */
	public double [] getSpecialValues() { return specialValues; }
    
    public EPoint getMulticut2Size() {
        Technology.NodeLayer cutLayer = findMulticut();
        assert cutLayer.getLeftEdge().getMultiplier() == -0.5;
        assert cutLayer.getBottomEdge().getMultiplier() == -0.5;
        assert cutLayer.getRightEdge().getMultiplier() == 0.5;
        assert cutLayer.getTopEdge().getMultiplier() == 0.5;
        double x = cutLayer.getMulticutSizeX() + cutLayer.getMulticutSep2D() + cutLayer.getLeftEdge().getAdder() - cutLayer.getRightEdge().getAdder();
        double y = cutLayer.getMulticutSizeY() + cutLayer.getMulticutSep2D() + cutLayer.getBottomEdge().getAdder() - cutLayer.getTopEdge().getAdder();
        return EPoint.fromLambda(x, y);
    }
    
	/**
	 * Method to set the special values stored on this PrimitiveNode.
	 * The special values are an array of values that describe unusual features of the PrimitiveNode.
	 * The meaning depends on the specialType (see the documentation for "getSpecialValues").
	 * @param specialValues the special values for this PrimitiveNode.
	 */
	public void setSpecialValues(double [] specialValues)
	{
		if (specialValues.length != 6)
			throw new IndexOutOfBoundsException("Invalid number of values in setSpecialValues");
		this.specialValues = specialValues;
	}

	/**
	 * Method to tell whether this PrimitiveNode is a Pin.
	 * Pin nodes have one port, no valid geometry, and are used to connect arcs.
	 * @return true if this PrimitiveNode is a Pin.
	 */
	public boolean isPin()
	{
		return (getFunction() == Function.PIN);
	}

	/**
	 * Method to describe this PrimitiveNode as a string.
	 * If the primitive is not from the current technology, prepend the technology name.
     * @param withQuotes to wrap description between quotes
	 * @return a description of this PrimitiveNode.
	 */
	public String describe(boolean withQuotes)
	{
		String name = "";
		if (tech != Technology.getCurrent())
			name += tech.getTechName() + ":";
		name += protoName;
        return (withQuotes) ? "'"+name+"'" : name;
	}

    /**
     * Method to determine if node has a given bit on. This is usefull for different
     * @param bit bit containing information to query. It could be LOWVTTRANS,
     * HIGHVTTRANS, NATIVETRANS, OD18TRANS, OD25TRANS or OD33TRANS in case of transistors.
     * @return true if the given bit is on in the node.
     */
    public boolean isNodeBitOn(int bit)
    {
        assert (bit == LOWVTBIT ||
            bit == HIGHVTBIT ||
            bit == NATIVEBIT ||
            bit == OD18BIT ||
            bit == OD25BIT ||
            bit == OD33BIT);

        return (userBits & bit) != 0;
    }

    /**
     * Method to set certain bit during construction
     * @param bit
     */
    public void setNodeBit(int bit) { checkChanging(); userBits |= bit; }

	/**
	 * Method to allow instances of this PrimitiveNode not to be considered in
     * tech palette for the calculation of the largest icon.
	 * Valid for menu display
	 */
	public void setSkipSizeInPalette() { checkChanging(); userBits |= SKIPSIZEINPALETTE; }

	/**
	 * Method to tell if instaces of this PrimitiveNode are special (don't appear in menu).
	 * Valid for menu display
	 */
	public boolean isSkipSizeInPalette() { return (userBits & SKIPSIZEINPALETTE) != 0; }

	/**
	 * Method to allow instances of this PrimitiveNode to shrink.
	 * Shrinkage occurs on MOS transistors when they are connected to wires at angles that are not manhattan
	 * (the angle between the transistor and the wire is not a multiple of 90 degrees).
	 * The actual transistor must be shrunk back appropriately to prevent little tabs from emerging at the connection site.
	 * This state is only set on primitive node prototypes.
	 * If the actual NodeInst is to shrink, it must be marked with "setShortened".
	 * Note that shrinkage does not apply if there is no arc connected.
	 */
	public void setCanShrink() { checkChanging(); userBits |= NODESHRINK; }

	/**
	 * Method to prevent instances of this PrimitiveNode from shrinking.
	 * Shrinkage occurs on MOS transistors when they are connected to wires at angles that are not manhattan
	 * (the angle between the transistor and the wire is not a multiple of 90 degrees).
	 * The actual transistor must be shrunk back appropriately to prevent little tabs from emerging at the connection site.
	 * This state is only set on primitive node prototypes.
	 * If the actual NodeInst is to shrink, it must be marked with "setShortened".
	 * Note that shrinkage does not apply if there is no arc connected.
	 */
	public void clearCanShrink() { checkChanging(); userBits &= ~NODESHRINK; }

	/**
	 * Method to tell if instances of this PrimitiveNode can shrink.
	 * Shrinkage occurs on MOS transistors when they are connected to wires at angles that are not manhattan
	 * (the angle between the transistor and the wire is not a multiple of 90 degrees).
	 * The actual transistor must be shrunk back appropriately to prevent little tabs from emerging at the connection site.
	 * This state is only set on primitive node prototypes.
	 * If the actual NodeInst is to shrink, it must be marked with "setShortened".
	 * Note that shrinkage does not apply if there is no arc connected.
	 * @return true if instances of this PrimitiveNode can shrink.
	 */
	public boolean canShrink() { return (userBits & NODESHRINK) != 0; }

	/**
	 * Method to set this PrimitiveNode so that instances of it are "arc-wipable".
	 * For display efficiency reasons, pins that have arcs connected to them should not bother being drawn.
	 * Therefore, pin prototypes have this state set, and when instances of the
	 * appropriate arc prototypes connect to instances of these pins, they stop being drawn.
	 * It is necessary for the arc prototype to enable wiping (with setWipable).
	 * A NodeInst that becomes wiped out has "setWiped" called.
	 */
	public void setArcsWipe() { checkChanging(); userBits |= ARCSWIPE; }

	/**
	 * Method to set this PrimitiveNode so that instances of it are not "arc-wipable".
	 * For display efficiency reasons, pins that have arcs connected to them should not bother being drawn.
	 * Therefore, pin prototypes have this state set, and when instances of the
	 * appropriate arc prototypes connect to instances of these pins, they stop being drawn.
	 * It is necessary for the arc prototype to enable wiping (with setWipable).
	 * A NodeInst that becomes wiped out has "setWiped" called.
	 */
	public void clearArcsWipe() { checkChanging(); userBits &= ~ARCSWIPE; }

	/**
	 * Method to tell if instances of this PrimitiveNode are "arc-wipable" by when created.
	 * For display efficiency reasons, pins that have arcs connected to them should not bother being drawn.
	 * Therefore, pin prototypes have this state set, and when instances of the
	 * appropriate arc prototypes connect to instances of these pins, they stop being drawn.
	 * It is necessary for the arc prototype to enable wiping (with setWipable).
	 * A NodeInst that becomes wiped out has "setWiped" called.
	 * @return true if instances of this PrimitiveNode are "arc-wipable" by when created.
	 */
	public boolean isArcsWipe() { return (userBits & ARCSWIPE) != 0; }

	/**
	 * Method to set this PrimitiveNode so that instances of it are "square".
	 * Square nodes must have the same X and Y size.
	 * This is useful for round components that really have only one dimension.
	 */
	public void setSquare() { checkChanging(); userBits |= NSQUARE; }

	/**
	 * Method to set this PrimitiveNode so that instances of it are not "square".
	 * Square nodes must have the same X and Y size.
	 * This is useful for round components that really have only one dimension.
	 */
	public void clearSquare() { checkChanging(); userBits &= ~NSQUARE; }

	/**
	 * Method to tell if instances of this PrimitiveNode are square.
	 * Square nodes must have the same X and Y size.
	 * This is useful for round components that really have only one dimension.
	 * @return true if instances of this PrimitiveNode are square.
	 */
	public boolean isSquare() { return (userBits & NSQUARE) != 0; }

	/**
	 * Method to set this PrimitiveNode so that instances of it may hold outline information.
	 * Outline information is an array of coordinates that define the node.
	 * It can be as simple as an opened-polygon that connects the points,
	 * or a serpentine transistor that lays down polysilicon to follow the points.
	 */
	public void setHoldsOutline() { checkChanging(); userBits |= HOLDSTRACE; }

	/**
	 * Method to set this PrimitiveNode so that instances of it may not hold outline information.
	 * Outline information is an array of coordinates that define the node.
	 * It can be as simple as an opened-polygon that connects the points,
	 * or a serpentine transistor that lays down polysilicon to follow the points.
	 */
	public void clearHoldsOutline() { checkChanging(); userBits &= ~HOLDSTRACE; }

	/**
	 * Method to tell if instances of this PrimitiveNode can hold an outline.
	 * Outline information is an array of coordinates that define the node.
	 * It can be as simple as an opened-polygon that connects the points,
	 * or a serpentine transistor that lays down polysilicon to follow the points.
	 * @return true if instances of this PrimitiveNode can hold an outline.
	 */
	public boolean isHoldsOutline() { return (userBits & HOLDSTRACE) != 0; }

	/**
	 * Method to set this PrimitiveNode so that it can be zero in size.
	 * The display system uses this to eliminate zero-size nodes that cannot be that way.
	 */
	public void setCanBeZeroSize() { checkChanging(); userBits |= CANBEZEROSIZE; }

	/**
	 * Method to set this PrimitiveNode so that it cannot be zero in size.
	 * The display system uses this to eliminate zero-size nodes that cannot be that way.
	 */
	public void clearCanBeZeroSize() { checkChanging(); userBits &= ~CANBEZEROSIZE; }

	/**
	 * Method to tell if instances of this PrimitiveNode can be zero in size.
	 * The display system uses this to eliminate zero-size nodes that cannot be that way.
	 * @return true if instances of this PrimitiveNode can be zero in size.
	 */
	public boolean isCanBeZeroSize() { return (userBits & CANBEZEROSIZE) != 0; }

	/**
	 * Method to set this PrimitiveNode so that instances of it are wiped when 1 or 2 arcs connect.
	 * This is used in Schematics pins, which are not shown if 1 or 2 arcs connect, but are shown
	 * when standing alone, or when 3 or more arcs make a "T" or other connection to it.
	 */
	public void setWipeOn1or2() { checkChanging(); userBits |= WIPEON1OR2; }

	/**
	 * Method to set this PrimitiveNode so that instances of it are not wiped when 1 or 2 arcs connect.
	 * Only Schematics pins enable this state.
	 */
	public void clearWipeOn1or2() { checkChanging(); userBits &= ~WIPEON1OR2; }

	/**
	 * Method to tell if instances of this PrimitiveNode are wiped when 1 or 2 arcs connect.
	 * This is used in Schematics pins, which are not shown if 1 or 2 arcs connect, but are shown
	 * when standing alone, or when 3 or more arcs make a "T" or other connection to it.
	 * @return true if instances of this PrimitiveNode are wiped when 1 or 2 arcs connect.
	 */
	public boolean isWipeOn1or2() { return (userBits & WIPEON1OR2) != 0; }

	/**
	 * Method to set this PrimitiveNode so that instances of it are locked.
	 * Locked Primitives cannot be created, deleted, or modified.
	 * Typically, array technologies (such as FPGA) have lockable primitives which are used for the fixed part of a design,
	 * and then locked to prevent the customization work from damaging the circuit.
	 */
	public void setLockedPrim() { checkChanging(); userBits |= LOCKEDPRIM; }

	/**
	 * Method to set this PrimitiveNode so that instances of it are not locked.
	 * Locked Primitives cannot be created, deleted, or modified.
	 * Typically, array technologies (such as FPGA) have lockable primitives which are used for the fixed part of a design,
	 * and then locked to prevent the customization work from damaging the circuit.
	 */
	public void clearLockedPrim() { checkChanging(); userBits &= ~LOCKEDPRIM; }

	/**
	 * Method to tell if instances of this PrimitiveNode are loced.
	 * Locked Primitives cannot be created, deleted, or modified.
	 * Typically, array technologies (such as FPGA) have lockable primitives which are used for the fixed part of a design,
	 * and then locked to prevent the customization work from damaging the circuit.
	 * @return true if instances of this PrimitiveNode are loced.
	 */
	public boolean isLockedPrim() { return (userBits & LOCKEDPRIM) != 0; }

	/**
	 * Method to set this PrimitiveNode so that instances of it are selectable only by their edges.
	 * Artwork primitives that are not filled-in or are outlines want edge-selection, instead
	 * of allowing a click anywhere in the bounding box to work.
	 */
	public void setEdgeSelect() { checkChanging(); userBits |= NEDGESELECT; }

	/**
	 * Method to set this PrimitiveNode so that instances of it are not selectable only by their edges.
	 * Artwork primitives that are not filled-in or are outlines want edge-selection, instead
	 * of allowing a click anywhere in the bounding box to work.
	 */
	public void clearEdgeSelect() { checkChanging(); userBits &= ~NEDGESELECT; }

	/**
	 * Method to tell if instances of this PrimitiveNode are selectable on their edges.
	 * Artwork primitives that are not filled-in or are outlines want edge-selection, instead
	 * of allowing a click anywhere in the bounding box to work.
	 * @return true if instances of this PrimitiveNode are selectable on their edges.
	 */
	public boolean isEdgeSelect() { return (userBits & NEDGESELECT) != 0; }

	/**
	 * Method to set this PrimitiveNode so that arcs connected to instances will shrink in nonmanhattan situations.
	 * This happens to pins where any combination of multiple arcs in angles that are not increments of 90 degrees
	 * will cause tabs to emerge at the connection site.
	 */
	public void setArcsShrink() { checkChanging(); userBits |= ARCSHRINK; }

	/**
	 * Method to set this PrimitiveNode so that arcs connected to instances will not shrink in nonmanhattan situations.
	 * This happens to pins where any combination of multiple arcs in angles that are not increments of 90 degrees
	 * will cause tabs to emerge at the connection site.
	 */
	public void clearArcsShrink() { checkChanging(); userBits &= ~ARCSHRINK; }

	/**
	 * Method to tell if instances of this PrimitiveNode cause arcs to shrink in nonmanhattan situations.
	 * This happens to pins where any combination of multiple arcs in angles that are not increments of 90 degrees
	 * will cause tabs to emerge at the connection site.
	 * @return true if instances of this PrimitiveNode cause arcs to shrink in nonmanhattan situations.
	 */
	public boolean isArcsShrink() { return (userBits & ARCSHRINK) != 0; }

	/**
	 * Method to set this PrimitiveNode to be completely invisible, and unselectable.
	 * When all of its layers have been made invisible, the node is flagged to be invisible.
	 * @param invisible true to make this PrimitiveNode completely invisible and unselectable.
	 */
	public void setNodeInvisible(boolean invisible)
	{
		/*checkChanging();*/
		if (invisible) userBits |= NINVISIBLE; else
			userBits &= ~NINVISIBLE;
	}

	/**
	 * Method to tell if instances of this PrimitiveNode are invisible.
	 * When all of its layers have been made invisible, the node is flagged to be invisible.
	 * @return true if instances of this PrimitiveNode are invisible.
	 */
	public boolean isNodeInvisible() { return (userBits & NINVISIBLE) != 0; }

	/**
	 * Method to set this PrimitiveNode so that it is not used.
	 * Unused nodes do not appear in the component menus and cannot be created by the user.
	 * The state is useful for hiding primitives that the user should not use.
     * @param set
     */
	public void setNotUsed(boolean set)
    {
        checkChanging();
        if (set)
            userBits |= NNOTUSED;
        else
            userBits &= ~NNOTUSED; // clear

    }

	/**
	 * Method to tell if this PrimitiveNode is used.
	 * Unused nodes do not appear in the component menus and cannot be created by the user.
	 * The state is useful for hiding primitives that the user should not use.
	 * @return true if this PrimitiveNode is used.
	 */
	public boolean isNotUsed() { return (userBits & NNOTUSED) != 0; }

    /**
     * Method to determine if PrimitiveNode represents a well node
     * @return true if this PrimitiveNode is a well node
     */
	public boolean isPureWellNode()
	{
	    // Not even looking at
		if (function != PrimitiveNode.Function.NODE) return false;
	    // only one layer
	    if (layers.length != 1) return false;
	    Layer layer = layers[0].getLayer();
	    return (layer.getFunction().isWell());
	}
    /**
     * Method to determine if PrimitiveNode represents substrate node
     * @return true if this PrimitiveNode is a substrate node
     */
	public boolean isPureSubstrateNode()
	{
	    // Not even looking at
		if (function != PrimitiveNode.Function.NODE) return false;
	    // only one layer
	    if (layers.length != 1) return false;
	    Layer layer = layers[0].getLayer();
	    return (layer.getFunction().isSubstrate());
	}

// 	/**
// 	 * Method to get the index of this PrimitiveNode.
// 	 * @return the index of this PrimitiveNode.
// 	 */
// 	public final int getPrimNodeIndex() { return primNodeIndex; }

    /**
     * Method to retrieve index of the node in the given technology
     * @return the index of this node in its Technology.
     */
    public final int getPrimNodeIndexInTech() { return techPrimNodeIndex;}

    /**
     * Method to set the index of this node in its Technology.
     * @param index the index to use for this node in its Technology.
     */
    public void setPrimNodeIndexInTech(int index) { techPrimNodeIndex = index; }

    /**
     * Compares PrimtiveNodes by their Technologies and definition order.
     * @param that the other PrimitiveNode.
     * @return a comparison between the PrimitiveNodes.
     */
	public int compareTo(PrimitiveNode that)
	{
		if (this.tech != that.tech)
		{
			int cmp = this.tech.compareTo(that.tech);
			if (cmp != 0) return cmp;
		}
		return this.globalPrimNodeIndex - that.globalPrimNodeIndex;
	}

	/**
	 * Returns a printable version of this PrimitiveNode.
	 * @return a printable version of this PrimitiveNode.
	 */
	public String toString()
	{
		return "node " + describe(true);
	}

    private static final String[] nodeBits = {
        "NODESHRINK", null, null,
        "LOWVTBIT", "HIGHVTBIT", "NATIVEBIT",
        "OD18BIT", "OD25BIT", "OD33BIT",
        "ARCSWIPE", "NSQUARE", "HOLDSTRACE",
        "CANBEZEROSIZE", "WIPEON1OR2", "LOCKEDPRIM",
        "NEDGESELECT", "ARCSHRINK", "NINVISIBLE",
        "SKIPSIZEINPALETTE", "NNOTUSED", null
    };
    
    void dump(PrintWriter out) {
        out.print("PrimitiveNode " + getName() + " " + getFunction());
        Technology.printlnBits(out, nodeBits, userBits);
        out.print("\tspecialType=" + specialType + " numMultiCuts=" + numMultiCuts);
        if (specialValues != null) {
            for (double v: specialValues)
                out.print(" " + v);
        }
        out.println();
        if (offset != null)
            out.println("\t" + offset);
        out.println("\trefWidth=" + -sizeCorrector.getLambdaX() + " refHeight=" + -sizeCorrector.getLambdaY());
        if (minNodeSize != null)
            out.println("\tminNodeSize w=" + minNodeSize.getWidth() + " h=" + minNodeSize.getHeight() + " rule=" + minNodeSize.getRuleName());
        if (autoGrowth != null)
            out.println("\tautoGrowth " + autoGrowth);
        Technology.printlnPref(out, 1, defaultExtendXPrefs.get(this));
        Technology.printlnPref(out, 1, defaultExtendYPrefs.get(this));
        out.println("\tlayers:");
        boolean isSerp = specialType == SERPTRANS;
        dumpNodeLayers(out, layers, isSerp);
        if (electricalLayers != null) {
            out.println("\telectricalLayers:");
            dumpNodeLayers(out, electricalLayers, isSerp);
        }
        for (PrimitivePort pp: primPorts)
            pp.dump(out);
    }
    
    private void dumpNodeLayers(PrintWriter out, Technology.NodeLayer[] layers, boolean isSerp) {
        for (Technology.NodeLayer nl: layers) {
            out.println("\tlayer=" + nl.getLayerOrPseudoLayer().getName() + " port=" + nl.getPortNum() + " style=" + nl.getStyle().name() + " repr=" + nl.getRepresentation());
            if (nl.getMessage() != null) {
                TextDescriptor td = nl.getDescriptor();
                out.println("\t\tmessage=\"" + nl.getMessage() + "\" td=" + Long.toHexString(td.lowLevelGet()) + " colorIndex=" + td.getColorIndex() + " disp=" + td.isDisplay());
            }
            if (nl.getMulticutSizeX() != 0 || nl.getMulticutSizeY() != 0 || nl.getMulticutSep1D() != 0 || nl.getMulticutSep2D() != 0)
                out.println("\t\tmultiSizeX=" + nl.getMulticutSizeX() + " multiSizeY=" + nl.getMulticutSizeY() + " multiSep=" + nl.getMulticutSep1D() + " multiSpe2D=" + nl.getMulticutSep2D());
            
            if (isSerp)
                out.println("\t\tLWidth=" + nl.getSerpentineLWidth() + " rWidth=" + nl.getSerpentineRWidth() + " bExtend=" + nl.getSerpentineExtentB() + " tExtend=" + nl.getSerpentineExtentT());
            for (Technology.TechPoint p: nl.getPoints())
                out.println("\t\tpoint xm=" + p.getX().getMultiplier() + " xa=" + p.getX().getAdder() + " ym=" + p.getY().getMultiplier() + " ya=" + p.getY().getAdder());
        }
    }
    
	/**
	 * Method to get MinZ and MaxZ of the cell calculated based on nodes
	 * @param array array[0] is minZ and array[1] is max
	 */
	public void getZValues(double [] array)
	{
		for(int j=0; j<layers.length; j++)
		{
			Layer layer = layers[j].getLayer();

			// Skipping Glyph node
			if (layer.getTechnology() instanceof Generic) continue;
			double distance = layer.getDistance();
			double thickness = layer.getThickness();
			double z = distance + thickness;

			array[0] = (array[0] > distance) ? distance : array[0];
			array[1] = (array[1] < z) ? z : array[1];
		}
	}

	private void checkChanging() {}

    /**
     * Class to define a single rule on a node.
     */
    public static class NodeSizeRule
    {
        private final double sizeX, sizeY;
        private final String rule;

        public NodeSizeRule(double sizeX, double sizeY, String rule)
        {
            this.sizeX = DBMath.round(sizeX);
            this.sizeY = DBMath.round(sizeY);
            this.rule = rule;
        }

        public String getRuleName() { return rule; }
        public double getWidth() { return sizeX; }
        public double getHeight() { return sizeY; }
    }
}
