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

import java.util.ArrayList;
import jssc.SerialPort;
import jssc.SerialPortException;

/**
 * Class which allows more than one user to work with a single SerialPort
 * 
 * @author levkov_cha_ext
 */
public class SerialportShare
{
  ArrayList<SerialPortWithRefCount> serialports = new ArrayList<>();
  
  /**
   * Returns a SerialPort which is ready for use (i.e. port is already opened).
   * When port is not needed anymore call the function releasePort(). Do not close the port
   * manually - the class will take care of it.
   * 
   * Will fail with exception if the port that we are trying to access has been opened outside
   * this class.
   * 
   * @param portName - name of the port that we want to access
   * @return SerialPort which is already open. You need to set the configuration params if needed (e.g. baudrate, dtr etc.)
   */
  SerialPort getPort(String portName)throws Exception
  {
    // Port already opened by the class
    if(findPort(portName)!=null)
    {
      return findPort(portName).getReference();
    }
    
    // We need to open the port. However, let's check if not used by another program
    if( CommUtils.isBusy(portName) )
      throw new Exception("Commport is used by another application: " + portName);
        
    SerialPortWithRefCount port = new SerialPortWithRefCount(portName);

    serialports.add(port);
    return port.getReference();
  }
  
  void releasePort(SerialPort port) throws Exception
  {
    SerialPortWithRefCount prt = findPort(port.getPortName());
    
    if(prt == null)
      throw new Exception("Cannot release Commport which was open by another application: " + port.getPortName());
    
    prt.releaseReference();
  }
  
  private SerialPortWithRefCount findPort(String name)
  {
    for(SerialPortWithRefCount port : serialports)
    {
      if(port.getPortName().equals(name))
        return port;      
    }
    return null;
  }
  
  
  /** 
   * Helper class for working with a single Port multiple times
   */
  private class SerialPortWithRefCount
  {
    SerialPort  port;
    private int reference = 0;
    
    public SerialPortWithRefCount(String portName) throws SerialPortException
    {
      port = new SerialPort(portName);
    }
    
    /**
     * Will open the port if this is the reference counter is 0
     * @return
     * @throws SerialPortException 
     */
    public SerialPort getReference() throws SerialPortException
    {
      if(reference == 0)
        port.openPort();

      reference++;
      return port;
    }
    
    /**
     * Will close the port if the reference counter drops to 0
     * 
     * @throws Exception 
     */
    public void releaseReference() throws Exception
    {
      if(reference == 0)
        throw new Exception("Call getReference() first.");
      
      reference--;
      if(reference == 0)
        port.closePort();
    }  
    
    public String getPortName()
    {
      return port.getPortName();
    }
    
    
  }
}
