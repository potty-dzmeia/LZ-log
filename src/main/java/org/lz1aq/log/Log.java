// ***************************************************************************
// *   Copyright (C) 2015 by Chavdar Levkov                              
// *   ch.levkov@gmail.com                                                   
// *                                                                         
// *   This program is free software; you can redistribute it and/or modify  
// *   it under the terms of the GNU General Public License as published by  
// *   the Free Software Foundation; either version 2 of the License, or     
// *   (at your option) any later version.                                   
// *                                                                         
// *   This program is distributed in the hope that it will be useful,       
// *   but WITHOUT ANY WARRANTY; without even the implied warranty of        
// *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the         
// *   GNU General Public License for more details.                          
// *                                                                         
// *   You should have received a copy of the GNU General Public License     
// *   along with this program; if not, write to the                         
// *   Free Software Foundation, Inc.,                                       
// *   59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.             
// ***************************************************************************
package org.lz1aq.log;

import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;
import org.lz1aq.radio.RadioModes;
import org.lz1aq.utils.Misc;

/**
 * This class is a container for Qso objects.
 *
 * We can think of the Log as a table: - The number of columns is equal to the
 * number of parameters of the first Qso object in the log. - The number of rows
 * is equal to the number of Qso objects contained in the Log.
 *
 */
public class Log
{

    private final LogDatabase db;         // Interface to a db4o database, stand-alone or client/server. 
    private final ArrayList<Qso> qsoList; // Log is also mirrored in RAM
    private final Qso templateQso;

    private final CopyOnWriteArrayList<LogListener> logListeners;

    /**
     *
     *
     * @param db interface to an already opened database
     * @param template
     */
    public Log(LogDatabase db, Qso template)
    {
        this.db = db;
        qsoList = new ArrayList<>(db.getAll()); // Load Qsos from the database

        this.templateQso = template;

        logListeners = new CopyOnWriteArrayList<>();
    }

    public void addEventListener(LogListener listener)
    {
        this.logListeners.add(listener);
        listener.eventInit();
    }

    public void removeEventListener(LogListener listener)
    {
        this.logListeners.remove(listener);
    }

    /**
     * Inserts a QSO object inside the log.
     *
     * No two references pointing to the same objects should be inserted to the
     * log as this will case a miss-alignment with the database.
     *
     * @param qso - Reference to a new Qso object. Insert only newly created Qso
     * objects.
     */
    public synchronized void add(Qso qso)
    {
        db.add(qso);      // Add the qso to the database
        qsoList.add(qso); // Add the qso to RAM (i.e local list)
        db.commit();

        for (LogListener listener : logListeners)
        {
            listener.eventQsoAdded(qso);
        }
    }

    /**
     * Removes a QSO object from the log.
     *
     * @param index Index of the Qso object to be removed
     */
    public synchronized void remove(int index)
    {
        // Do nothing if out of range
        if (index >= qsoList.size() || index < 0)
        {
            return;
        }

        Qso qso = qsoList.get(index);

        db.remove(qsoList.get(index)); // Remove the qso from the database
        qsoList.remove(index); // Remove the qso from the RAM (i.e local list)   
        db.commit();

        for (LogListener listener : logListeners)
        {
            listener.eventQsoRemoved(qso);
        }
    }

    public synchronized void close()
    {
        db.close();
    }

    /**
     * Method for accessing QSOs inside the log.
     *
     * @param index - Qso index inside the log (0 is being the first QSO in the
     * log)
     * @return Reference to the QSO object
     */
    public synchronized Qso get(int index)
    {
        if (index >= qsoList.size())
        {
            return null;
        }

        return qsoList.get(index);
    }

  
    public synchronized int getSize()
    {
        return qsoList.size();
    }

    /**
     * The number of columns inside the Log are equal to the number of
     * parameters of the first Qso that was added to the log.
     *
     * For example: (1) We create the Log object. (2) We add() a Qso which has 7
     * parameters (i.e. Qso.getParamsCount() == 7) (3) Then we add a second Qso
     * which has 8 parameters (4) If now we call the method getColumnCount() it
     * will return 7
     *
     * @return The number of columns inside the log. If log is empty the return
     * value will be 0.
     */
    public synchronized int getColumnCount()
    {
        return templateQso.getParamsCount();
    }

    /**
     * Returns the column name of the log which is equivalent to a Qso parameter
     * name.
     *
     * @param col Column index of which we would like to get the name
     * @return Name of the column (i.e. name of the Qso param)
     */
    public synchronized String getColumnName(int col)
    {
        return templateQso.getParamName(col);
    }

    /**
     * Returns the value from the specified cell.
     *
     * In other words this returns the parameter value of a certain Qso.
     *
     * @param row Row index (i.e. Qso index).
     * @param col Column index (i.e. index of Qso param)
     * @return
     */
    public synchronized String getValueAt(int row, int col)
    {
        if (row >= qsoList.size())
        {
            return "";
        }

        Qso qso = qsoList.get(row);
        return qso.getParamValue(col);
    }

    /**
     * Sets the value of the specified Cell.
     *
     * In other words this sets the parameter of a certain Qso to the specified
     * value.
     *
     * @param value - Value to be set.
     * @param row - Row index. This is equivalent to a Qso index.
     * @param col - Column index. This is equivalent to a Qso parameter index
     */
    public synchronized void setValueAt(String value, int row, int col)
    {
        if (row >= qsoList.size())
        {
            return;
        }

        Qso qso = qsoList.get(row);
        qso.setParamValue(col, value); // Update 
        
        db.modify(qso); // Update the database
        db.commit();

        for (LogListener listener : logListeners)
        {
            listener.eventQsoModified(qso);
        }
    }

    public synchronized int getQsoCount()
    {
        return qsoList.size();
    }

  
    /**
     * Will return the first word in Rcv from the last QSO. If there are no QSOs
     * in the log it will return "000".
     *
     * @return
     */
    public synchronized String getFirstPartOfLastRcv()
    {
        // If the log is empty send "000"
        if (getSize() == 0)
        {
            return "000";
        }

        Qso qso = qsoList.get(qsoList.size() - 1);

        return qso.getRcv().substring(0, 3);
    }

    
//    public synchronized Qso getLastQso(String callsign)
//    {
//        //find last Qso with this station
//        for (int i = qsoList.size() - 1; i >= 0; i--)
//        {
//            if (callsign.equalsIgnoreCase(qsoList.get(i).getHisCallsign()))
//            {
//                return qsoList.get(i);
//            }
//        }
//
//        return null;
//    }
    
    
    /**
     * Get the last Qso that we had with this station on a particular mode
     *
     * @param callsign station callsign
     * @param mode
     * @return Qso object
     */
    public synchronized Qso getLatestQso(String callsign, RadioModes mode)
    {
        //find last Qso with this station
        for (int i = qsoList.size() - 1; i >= 0; i--)
        {
            if (callsign.equalsIgnoreCase(qsoList.get(i).getHisCallsign()) &&
                qsoList.get(i).getMode() == mode)
            {
                return qsoList.get(i);
            }
        }

        return null;
    }
    

    /**
     * Checks if it is OK to work the station
     *
     * @param callsign
     * @param mode
     * @param allowedQsoRepeatPeriod - the repeat period for another QSO with
     * the same station (in seconds)
     * @return true if the required time has not elapsed
     */
    public boolean isDupe(String callsign, RadioModes mode, int allowedQsoRepeatPeriod)
    {
        Qso qso = getLatestQso(callsign, mode);

        // If there is no previous contact with this station
        if (qso == null)
        {
            return false;
        }

        //Previous contact was found...
        long secondsLeft = Misc.getSecondsLeft(qso.getElapsedSeconds(), allowedQsoRepeatPeriod);

        return secondsLeft > 0;
    }
}
