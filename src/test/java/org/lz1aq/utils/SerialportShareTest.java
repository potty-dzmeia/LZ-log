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

import jssc.SerialPort;
import junit.framework.TestCase;

/**
 *
 * @author levkov_cha_ext
 */
public class SerialportShareTest extends TestCase
{
  String portName = "Com11"; // Test will fail if this is not connected
  
  public SerialportShareTest(String testName)
  {
    super(testName);
  }
  
  @Override
  protected void setUp() throws Exception
  {
    super.setUp();
  }
  
  @Override
  protected void tearDown() throws Exception
  {
    super.tearDown();
  }

  /**
   * Test of getPort method, of class SerialportShare.
   */
  public void testGetPort() throws Exception
  {
    System.out.println("getPort");
    
    // Test - get port that is already in use by another Application
    // ----------------------- 
    SerialPort serialport0 = new SerialPort(portName);
    serialport0.openPort();
    SerialportShare instance = new SerialportShare();
    try
    {
      SerialPort port = instance.getPort(portName);
    }catch(Exception ex)
    {
      System.out.println("Trying already open port. Exception: "+ex);
    }
    assertTrue(serialport0.isOpened());
    assertEquals(serialport0.getPortName(), "Com11"); 
    assertTrue(serialport0.closePort());
    
    
    // Test - get and release port
    // ----------------------- 
    instance = new SerialportShare();
    SerialPort serialport1 = instance.getPort(portName);
    assertTrue(serialport1.isOpened());
    assertEquals(serialport1.getPortName(), "Com11");
    
    instance.releasePort(serialport1);  
    assertFalse(serialport1.isOpened());
    
    // Test - get, get and release, release port
    // -----------------------
    SerialPort serialport2 = instance.getPort(portName);
    SerialPort serialport3 = instance.getPort(portName);
    assertTrue(serialport2.isOpened());
    assertEquals(serialport2.getPortName(), "Com11");
    assertTrue(serialport3.isOpened());
    assertEquals(serialport3.getPortName(), "Com11");
    
    instance.releasePort(serialport3);  
    assertTrue(serialport3.isOpened());
    instance.releasePort(serialport2);  
    assertFalse(serialport2.isOpened());
    
    try
    {
      instance.releasePort(serialport3);
      fail("Line should not be executed!");
    }catch(Exception ex)
    {
      System.out.println("Calling release function one extra time. Exception: "+ex);
    }
    
    // Make sure we can open the port properly 
    SerialPort tempPort = new SerialPort(portName);
    assertTrue(tempPort.openPort());
    assertTrue(tempPort.closePort());
    
    
    // Test - get not existing commport
    // -----------------------
    SerialPort serialport4 = null;
    try
    {
       serialport4 = instance.getPort("Comm1992");
       fail("Line should not be executed!");
    }catch(Exception ex)
    {
      System.out.println("Trying to open not existing port. Exception: "+ex);
    }
    assertTrue(serialport4 == null);
    
    
    
    
    
    //assertEquals(expResult, result);
    // TODO review the generated test code and remove the default call to fail.
    //fail("The test case is a prototype.");
  }

  /**
   * Test of releasePort method, of class SerialportShare.
   */
  public void testReleasePort() throws Exception
  {
    System.out.println("releasePort");
    SerialportShare instance = new SerialportShare();
    
    // Open a port outside the class and make sure that the call fails
    // -----------------------
    SerialPort port = new SerialPort(portName);
    port.openPort();
    try
    {
      instance.releasePort(port);
      fail("Line should not be executed!");
    }catch(Exception ex)
    {
      assertTrue(port.isOpened()); // port should be still open.
      System.out.println("Trying to release a port that was open by another application. Exception: "+ex);
    }
    
    port.closePort();
   
    // Try to release port that is not open
    // -----------------------
    port = new SerialPort(portName);
    try
    {
      instance.releasePort(port);
      fail("Line should not be executed!");
    }catch(Exception ex)
    {
      assertFalse(port.isOpened()); // port should be still open.
      System.out.println("Trying to release a port that is not opened. Exception: "+ex);
    }
    
  }
  
}
