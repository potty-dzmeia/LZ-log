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
package org.lz1aq.lzlog;

import java.util.Collections;
import java.util.Comparator;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.table.AbstractTableModel;
import org.lz1aq.log.Log;
import org.lz1aq.log.LogListener;
import org.lz1aq.log.Qso;
import org.lz1aq.radio.RadioModes;
import org.lz1aq.utils.Misc;
import org.lz1aq.utils.TimeUtils;

/**
 *
 * @author potty
 */
public class TimeToNextQsoTableModel extends AbstractTableModel
{

    private final static int NUMBER_OF_COLUMNS = 5;
    private static final Logger logger = Logger.getLogger(TimeToNextQsoTableModel.class.getName());
    
    private final Log log;
    private final CopyOnWriteArrayList<Qso> listTimeToNextQso;
    private final ApplicationSettings appSettings;
    private final TimeToNextQsoTableModel.LocalLogListener logListener;

    
    public TimeToNextQsoTableModel(Log log, ApplicationSettings appsettings)
    {
        this.log = log;
        this.appSettings = appsettings;

        listTimeToNextQso = new CopyOnWriteArrayList<>();
        logListener = new TimeToNextQsoTableModel.LocalLogListener();
        this.log.addEventListener(logListener);
    }

    @Override
    public int getRowCount()
    {
        return listTimeToNextQso.size();
    }

    @Override
    public int getColumnCount()
    {
        return NUMBER_OF_COLUMNS;
    }

    @Override
    public String getColumnName(int col)
    {
        switch (col)
        {
            case 0:
                return "callsign";
            case 1:
                return "freq";
            case 2:
                return "mode";
            case 3:
                return "type";
            case 4:
                return "time left";
            default:
                return "should not be used";
        }
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex)
    {
        switch (columnIndex)
        {
            case 0:
                return listTimeToNextQso.get(rowIndex).getHisCallsign();
            case 1:
                return Misc.toIncomingQsoFreq(listTimeToNextQso.get(rowIndex).getFrequency());
            case 2:
                return listTimeToNextQso.get(rowIndex).getMode();
            case 3:
                return listTimeToNextQso.get(rowIndex).getType(); // CQ or S&P
            case 4:
                long secsLeft = log.getSecondsLeft(listTimeToNextQso.get(rowIndex), appSettings.getQsoRepeatPeriod());
                if(secsLeft<-3599)
                    secsLeft = -3599;
                return TimeUtils.getTimeLeftFormatted(secsLeft);

            default:
                return "should not happen";
        }

    }

    @Override
    public boolean isCellEditable(int row, int col)
    {
        return false;
    }

    public int getFrequency(int row) throws Exception
    {
        return Integer.parseInt(listTimeToNextQso.get(row).getFrequency());
    }

    public RadioModes getMode(int row) throws Exception
    {
        return listTimeToNextQso.get(row).getMode();
    }

    public String getCallsign(int row) throws Exception
    {
        return listTimeToNextQso.get(row).getHisCallsign();
    }

    public boolean isSpQso(int row) throws Exception
    {
        return listTimeToNextQso.get(row).getType().equalsIgnoreCase(Qso.TYPE_OF_WORK_SP);
    }

    /**
     * If we should go and work the callsign contained in this cell.
     *
     * @param row
     * @param col
     * @return
     */
    public boolean containsExpiredCallsign(int row, int col)
    {
        long secsLeft = log.getSecondsLeft(listTimeToNextQso.get(row), appSettings.getQsoRepeatPeriod());

        return secsLeft < 0;
        //return incomingQsoArrayList.get(row).isExpired();
    }

    /**
     * Updates the content of the table.
     */
    public synchronized void refresh()
    {

        for(Qso qso:listTimeToNextQso)
        {
            if( log.getSecondsLeft(qso, appSettings.getQsoRepeatPeriod()) < appSettings.getIncomingQsoHiderAfter()*(-1) )
            {
                listTimeToNextQso.remove(qso);
            }
        }
        this.fireTableDataChanged();
    }
    
    
    public synchronized void init()
    {
        logListener.eventInit();
    }

    
    /**
     * Finds a QSO within listTimeToNextQso[] which has the same combination of callsign/mode
     * @param call
     * @param mode
     * @return 
     */
    private int find(String call, RadioModes mode)
    {
        for(int i=0; i<listTimeToNextQso.size(); i++)
        {
            if(call.equalsIgnoreCase(listTimeToNextQso.get(i).getHisCallsign()) && mode==listTimeToNextQso.get(i).getMode())
            {
                return i;
            }
        }
        return -1;
    }
    
    /**
     * Sorts the listTimeToNextQso[]
     */
    private void sort()
    {
        Collections.sort(listTimeToNextQso, new Comparator<Qso>()
        {
            @Override
            public int compare(Qso qso1, Qso qso2)
            {
                if (qso1.getElapsedSeconds() > qso2.getElapsedSeconds())
                {
                    return -1;
                } else if (qso1.getElapsedSeconds() == qso2.getElapsedSeconds())
                {
                    return 0;
                } else
                {
                    return 1;
                }
            }
        });
    }
    
    
    private class LocalLogListener implements LogListener
    {

        /**
         * Insert all the latest QSOs per callsign/mode inside listTimeToNextQso
         */
        @Override
        public void eventInit()
        {
            listTimeToNextQso.clear();
            
            // For each logged QSO check if it is the latest per callsign/mode
            for (int i = 0; i < log.getSize(); i++)
            {
                
                if( log.getSecondsLeft(log.get(i), appSettings.getQsoRepeatPeriod()) < appSettings.getIncomingQsoHiderAfter()*(-1) )
                {
                    // Do not insert QSOs older than "appSettings.getIncomingQsoHiderAfter"
                    continue;
                }
                  
                String call = log.get(i).getHisCallsign();
                RadioModes mode = log.get(i).getMode();
                int local_index = find(call, mode);   
                
                // Callsign/Mode combination already available inside listTimeToNextQso
                if(local_index >= 0)
                {
                    // Make sure that the locally found QSO has an older date before substituting
                    if(listTimeToNextQso.get(local_index).getElapsedSeconds()>log.get(i).getElapsedSeconds())
                    {
                        listTimeToNextQso.remove(local_index);
                        listTimeToNextQso.add(log.get(i));
                    }
                }
                // Callsign/Mode not available inside listTimeToNextQso --> add
                else
                {
                    listTimeToNextQso.add(log.get(i));
                }
            }

            sort();
        }

        @Override
        public void eventQsoAdded(Qso qso)
        {
            int local_index = find(qso.getHisCallsign(), qso.getMode());

            // Callsign/Mode combination already available inside listTimeToNextQso
            if (local_index >= 0)
            {
                // Make sure that the locally found QSO has an older date before substituting
                if (listTimeToNextQso.get(local_index).getElapsedSeconds() > qso.getElapsedSeconds())
                {
                    listTimeToNextQso.remove(local_index);
                    listTimeToNextQso.add(qso);
                }
            } 
            // Callsign/Mode not available inside listTimeToNextQso --> add
            else
            {
                listTimeToNextQso.add(qso);
            }
        }

        @Override
        public void eventQsoRemoved(Qso qso)
        {
            // Remove QSO from list
            boolean res = listTimeToNextQso.remove(qso);
            
            // The QSO which was removed from the log was not inside the local list
            if(res == false)
            {
               logger.log(Level.INFO, "Deleted QSO not in listTimeToNextQso[]");
               return; 
            }
            
            
            String call = qso.getHisCallsign();
            RadioModes mode = qso.getMode();
            
            // Sanity check
            int local_index = find(call, mode);
            if(local_index >= 0)
            {
                logger.log(Level.SEVERE, "Second combination of callsign/mode was found!");
            }
            
            // Search for substitution of this entry
            for (int i=0; i<log.getSize(); i++)
            {
                eventQsoAdded(log.get(i));
            }
            
        }

        @Override
        public void eventQsoModified(Qso qso)
        {
            sort();
        }
    }

}
