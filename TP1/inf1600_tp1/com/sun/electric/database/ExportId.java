/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ExportId.java
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
package com.sun.electric.database;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.EDatabase;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.prototype.PortProtoId;
import com.sun.electric.database.text.Name;

import java.io.InvalidObjectException;
import java.io.ObjectStreamException;
import java.io.Serializable;



/**
 * The ExportId immutable class identifies a type of PortInst independently of threads.
 * It differs from Export objects, which will be owned by threads in transactional database.
 * This class is thread-safe except inCurrentThread method .
 */
public final class ExportId implements PortProtoId, Serializable {
    /** Empty ExportId array for initialization. */
    public static final ExportId[] NULL_ARRAY = {};
    
    /** Parent CellId of this ExportId */
    public final CellId parentId;
    
    /** chronological index of this PortProtoId in parent. */
    public final int chronIndex;
    
    /** representation of ExportId in disk files.
     * This name isn't chaged when Export is renamed.
     */
    public final String externalId;
    
    /**
     * ExportId constructor.
     */
    ExportId(CellId parentId, int chronIndex, String externalId) {
        assert parentId != null;
        if (externalId.length() == 0)
            throw new IllegalArgumentException("ExportId");
        this.parentId = parentId;
        this.chronIndex = chronIndex;
        this.externalId = externalId;
    }
    
    private Object writeReplace() { return new ExportIdKey(this); }
    private Object readResolve() throws ObjectStreamException { throw new InvalidObjectException("ExportId"); }
    
    private static class ExportIdKey extends EObjectInputStream.Key {
        private final int cellIndex;
        private final int chronIndex;
        
        private ExportIdKey(ExportId exportId) {
            cellIndex = exportId.parentId.cellIndex;
            chronIndex = exportId.chronIndex;
        }
        
        protected Object readResolveInDatabase(EDatabase database) throws InvalidObjectException {
            return database.getIdManager().getCellId(cellIndex).getPortId(chronIndex);
        }
    }
    
    /**
     * Method to return the parent NodeProtoId of this ExportId.
     * @return the parent NodeProtoId of this ExportId.
     */
    public CellId getParentId() { return parentId; }
    
    /**
     * Method to return chronological index of this ExportId in parent.
     * @return chronological index of this ExportId in parent.
     */
    public int getChronIndex() { return chronIndex; }
    
	/**
	 * Method to return the name key of this PortProtoId in a specified Snapshot.
     * @param snapshot snapshot for name search.
	 * @return the Name key of this PortProtoId.
	 */
	public Name getNameKey(Snapshot snapshot) { return inSnapshot(snapshot).name; }

	/**
	 * Method to return the name of this PortProtoId in a specified Snapshot.
     * @param snapshot snapshot for name search.
	 * @return the name of this PortProtoId.
	 */
	public String getName(Snapshot snapshot) { return inSnapshot(snapshot).name.toString(); }

    /**
     * Method to return the ImmutableExport representing ExportId in the specified Snapshot.
     * @param snapshot Snapshot where to get from.
     * @return the ImmutableExport representing ExportId in the specified snapshot.
     */
    public ImmutableExport inSnapshot(Snapshot snapshot) {
        CellBackup cellBackup = snapshot.getCell(parentId);
        return cellBackup != null ? cellBackup.getExport(this) : null;
    }
    
    /**
     * Method to return the Export representing ExportId in the specified EDatabase.
     * @param database EDatabase where to get from.
     * @return the Export representing ExportId in the specified database.
     * This method is not properly synchronized.
     */
    public Export inDatabase(EDatabase database) {
        Cell cell = database.getCell(parentId);
        if (cell == null) return null;
        return cell.getExportChron(chronIndex);
    }
    
    @Override
    public int hashCode() {
        return externalId.hashCode();
    }
    
    @Override
    public String toString() {
        return parentId + ":" + externalId;
    }
    
    /**
     * Check invariants of this ExportId.
     * @throws AssertionError if this ExportId is not valid.
     */
     void check() {
         assert externalId.length() > 0;
     }
}
