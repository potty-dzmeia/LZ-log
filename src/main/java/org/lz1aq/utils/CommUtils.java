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
package org.lz1aq.utils;

import javax.swing.DefaultComboBoxModel;
import jssc.SerialPort;

/**
 *
 * @author potty
 */
public class CommUtils
{

  public enum BaudRate
  {
    B_1200(1200),
    B_2400(2400),
    B_4800(4800),
    B_9600(9600),
    B_19200(19200),
    B_38400(38400),
    B_57600(57600),
    B_115200(115200);

    private final int value;

    BaudRate(int value)
    {
      this.value = value;
    }

    public int getInt()
    {
      return value;
    }

    static public DefaultComboBoxModel getComboxModel()
    {
      return new DefaultComboBoxModel(new String[]
      {
        B_1200.toString(), B_2400.toString(), B_4800.toString(), B_9600.toString(), B_19200.toString(), B_38400.toString(), B_57600.toString(), B_115200.toString()
      });
    }
    
    static public int getBaudRate(String baudRate)
    {
      return BaudRate.valueOf(baudRate).getInt();
    }
    
    static public BaudRate getBaudRate(int value)
    {
      for(BaudRate e : BaudRate.values())
      {
        if(e.value == value)
        {
          return e;
        }
      }
      return null;// not found
    }
    
    static public String getName(int value)
    {
      for(BaudRate e : BaudRate.values())
      {
        if(e.value == value)
        {
          return e.toString();
        }
      }
      return null;// not found
    }
  }

  
  public enum Parity
  {

    NONE(SerialPort.PARITY_NONE),
    EVEN(SerialPort.PARITY_EVEN),
    MARK(SerialPort.PARITY_MARK),
    ODD(SerialPort.PARITY_ODD),
    SPACE(SerialPort.PARITY_SPACE);

    private final int value;

    Parity(int value)
    {
      this.value = value;
    }

    public int getValue()
    {
      return value;
    }

    static public DefaultComboBoxModel getComboxModel()
    {
      return new DefaultComboBoxModel(new String[]
      {
        NONE.toString(), EVEN.toString(), MARK.toString(), ODD.toString(), SPACE.toString()
      });
    }
    
    static public Parity getParity(int value)
    {
      for(Parity e : Parity.values())
      {
        if(e.value == value)
        {
          return e;
        }
      }
      return null;// not found
    }
  }

  
  public enum StopBits
  {

    ONE(SerialPort.STOPBITS_1),
    ONE_AND_HALF(SerialPort.STOPBITS_1_5),
    TWO(SerialPort.STOPBITS_2);

    private final int value;

    StopBits(int value)
    {
      this.value = value;
    }

    public int getValue()
    {
      return value;
    }

    static public DefaultComboBoxModel getComboxModel()
    {
      return new DefaultComboBoxModel(new String[]
      {
        ONE.toString(), ONE_AND_HALF.toString(), TWO.toString()
      });
    }
    
    static public StopBits getStopBits(int value)
    {
      for(StopBits e : StopBits.values())
      {
        if(e.value == value)
        {
          return e;
        }
      }
      return null;// not found
    }
  }

  
  public enum DataBits
  {

    FIVE(SerialPort.DATABITS_5),
    SIX(SerialPort.DATABITS_6),
    SEVEN(SerialPort.DATABITS_7),
    EIGHT(SerialPort.DATABITS_8);

    private final int value;

    DataBits(int value)
    {
      this.value = value;
    }

    public int getValue()
    {
      return value;
    }

    static public DefaultComboBoxModel getComboxModel()
    {
      return new DefaultComboBoxModel(new String[]
      {
        FIVE.toString(), SIX.toString(), SEVEN.toString(), EIGHT.toString()
      });
    }
    
    static public DataBits getDataBits(int value)
    {
      for(DataBits e : DataBits.values())
      {
        if(e.value == value)
        {
          return e;
        }
      }
      return null;// not found
    }
  }

  
  public enum FlowControl
  {

    FLOWCONTROL_NONE(SerialPort.FLOWCONTROL_NONE),
    FLOWCONTROL_RTSCTS_IN(SerialPort.FLOWCONTROL_RTSCTS_IN),
    FLOWCONTROL_RTSCTS_OUT(SerialPort.FLOWCONTROL_RTSCTS_OUT),
    FLOWCONTROL_XONXOFF_IN(SerialPort.FLOWCONTROL_XONXOFF_IN),
    FLOWCONTROL_XONXOFF_OUT(SerialPort.FLOWCONTROL_XONXOFF_OUT);

    private final int value;

    FlowControl(int value)
    {
      this.value = value;
    }

    public int getValue()
    {
      return value;
    }

    static  public DefaultComboBoxModel getComboxModel()
    {
      return new DefaultComboBoxModel(new String[]
      {
        FLOWCONTROL_NONE.toString(), FLOWCONTROL_RTSCTS_IN.toString(), FLOWCONTROL_RTSCTS_OUT.toString(), FLOWCONTROL_XONXOFF_IN.toString(), FLOWCONTROL_XONXOFF_OUT.toString()
      });
    }
    
    static public FlowControl getFlowControl(int value)
    {
      for(FlowControl e : FlowControl.values())
      {
        if(e.value == value)
        {
          return e;
        }
      }
      return null;// not found
    }
     
  }

}