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
package org.lz1aq.tuner;

import junit.framework.TestCase;
import org.lz1aq.utils.ComPortProperties;

/**
 *
 * @author levkov_cha_ext
 */
public class TunerControllerTest extends TestCase
{
  private boolean isAdcEvent = false;
  private boolean isPosCfmEvent = false;
  private boolean isNegCfmEvent = false;
  
  public TunerControllerTest(String testName)
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
  
  private void cleanFlags()
  {
    isAdcEvent = false;
    isPosCfmEvent = false;
    isNegCfmEvent = false;
  }
  /**
   * Test of decodeSerialData method, of class TunerController.
   */
  public void testDecodeSerialData()
  {
    ComPortProperties comProp = new ComPortProperties("foobar");
    TunerController instance = new TunerController(comProp);
    instance.addEventListener(new LocalListener());
    
    
    // 0 bytes read
    // -------------
    System.out.println("decodeSerialData");
    byte[] data = null;
    int expResult = 0;
    int result = instance.decodeSerialData(data);
    assertEquals(expResult, result);
    cleanFlags();
    
    data = new byte[]{};
    expResult = 0;
    result = instance.decodeSerialData(data);
    assertEquals(expResult, result);
    assertTrue(!isPosCfmEvent);
    assertTrue(!isNegCfmEvent);
    assertTrue(!isAdcEvent);  
       
    data = new byte[]{(byte)0xfe};
    expResult = 0;
    result = instance.decodeSerialData(data);
    assertEquals(expResult, result);
    assertTrue(!isPosCfmEvent);
    assertTrue(!isNegCfmEvent);
    assertTrue(!isAdcEvent);  
    
    data = new byte[]{(byte)0xfe, (byte)0xfe};
    expResult = 0;
    result = instance.decodeSerialData(data);
    assertEquals(expResult, result);
    assertTrue(!isPosCfmEvent);
    assertTrue(!isNegCfmEvent);
    assertTrue(!isAdcEvent);  
    
    
    // Pos cfm
    // -------------
    data = new byte[]{(byte)0xfe, (byte)0xfe, (byte)1};
    expResult = 3;
    result = instance.decodeSerialData(data);
    assertEquals(expResult, result);
    assertTrue(isPosCfmEvent);
    cleanFlags();
    
    data = new byte[]{(byte)0x45, (byte)0xfe, (byte)0xfe, (byte)1};
    expResult = 4;
    result = instance.decodeSerialData(data);
    assertEquals(expResult, result);
    assertTrue(isPosCfmEvent);
    cleanFlags();
    
    data = new byte[]{(byte)0x45, (byte)0xfe, (byte)0xfe, (byte)1, (byte)1};
    expResult = 4;
    result = instance.decodeSerialData(data);
    assertEquals(expResult, result);
    assertTrue(isPosCfmEvent);
    cleanFlags();
    
    data = new byte[]{(byte)0x45, (byte)0x45, (byte)0xfe, (byte)0xfe, (byte)1, (byte)1};
    expResult = 5;
    result = instance.decodeSerialData(data);
    assertEquals(expResult, result);
    assertTrue(isPosCfmEvent);
    cleanFlags();
    
    data = new byte[]{(byte)0xFE, (byte)0x0, (byte)0xfe, (byte)0xfe, (byte)1, (byte)0xfe, (byte)0xfe, (byte)1,};
    expResult = 5;
    result = instance.decodeSerialData(data);
    assertEquals(expResult, result);
    assertTrue(isPosCfmEvent);
    cleanFlags();
    
    
    
    // Neg cfm
    // -------------
    data = new byte[]{(byte)0xfe, (byte)0xfe, (byte)3};
    expResult = 3;
    result = instance.decodeSerialData(data);
    assertEquals(expResult, result);
    assertTrue(isNegCfmEvent);
    cleanFlags();
    
    data = new byte[]{(byte)0xfe, (byte)0xfe, (byte)0};
    expResult = 3;
    result = instance.decodeSerialData(data);
    assertEquals(expResult, result);
    assertTrue(isNegCfmEvent);
    cleanFlags();
    
    data = new byte[]{(byte)0xfe, (byte)0xfe, (byte)3, (byte)0xfe,};
    expResult = 3;
    result = instance.decodeSerialData(data);
    assertEquals(expResult, result);
    assertTrue(isNegCfmEvent);
    cleanFlags();
    
    data = new byte[]{(byte)0xff, (byte)0xfe, (byte)0xfe, (byte)3};
    expResult = 4;
    result = instance.decodeSerialData(data);
    assertEquals(expResult, result);
    assertTrue(isNegCfmEvent);
    cleanFlags();
    
    data = new byte[]{ (byte)3,  (byte)3, (byte)0xfe, (byte)0xfe, (byte)3};
    expResult = 5;
    result = instance.decodeSerialData(data);
    assertEquals(expResult, result);
    assertTrue(isNegCfmEvent);
    cleanFlags();
    
    data = new byte[]{(byte)0xfe, (byte)0xfe, (byte)3, (byte)0xfe, (byte)0xfe, (byte)3};
    expResult = 3;
    result = instance.decodeSerialData(data);
    assertEquals(expResult, result);
    assertTrue(isNegCfmEvent);
    cleanFlags();
    
    data = new byte[]{(byte)0xfc, (byte)0xfa, (byte)0xfe, (byte)0xfe, (byte)3};
    expResult = 5;
    result = instance.decodeSerialData(data);
    assertEquals(expResult, result);
    assertTrue(isNegCfmEvent);
    cleanFlags();
    
    data = new byte[]{(byte)0xfe, (byte)0xfe, (byte)0xfe, (byte)1};
    expResult = 3;
    result = instance.decodeSerialData(data);
    assertEquals(expResult, result);
    assertTrue(!isPosCfmEvent);
    assertTrue(isNegCfmEvent);
    assertTrue(!isAdcEvent);
    cleanFlags();
    
    data = new byte[]{(byte)0xfe, (byte)0xfe, (byte)0xfe, (byte)0xfe, (byte)1};
    expResult = 3;
    result = instance.decodeSerialData(data);
    assertEquals(expResult, result);
    assertTrue(!isPosCfmEvent);
    assertTrue(isNegCfmEvent);
    assertTrue(!isAdcEvent);
    cleanFlags();
    
    // ADC values
    // -------------
    data = new byte[]{(byte)0xfe, (byte)0xfe, (byte)2, (byte)1, (byte)1, (byte)1, (byte)1, (byte)1, (byte)1, (byte)1, (byte)1};
    expResult = 11;
    result = instance.decodeSerialData(data);
    assertEquals(expResult, result);
    assertTrue(isPosCfmEvent);
    assertTrue(!isNegCfmEvent);
    assertTrue(isAdcEvent);
    cleanFlags();
    
    data = new byte[]{(byte)0xfe, (byte)0xfe, (byte)2, (byte)1, (byte)1, (byte)1, (byte)1, (byte)1, (byte)1, (byte)1};
    expResult = 0;
    result = instance.decodeSerialData(data);
    assertEquals(expResult, result);
    assertTrue(!isPosCfmEvent);
    assertTrue(!isNegCfmEvent);
    assertTrue(!isAdcEvent);
    cleanFlags();
    
    data = new byte[]{(byte)0x2, (byte)0xfe, (byte)0xfe, (byte)2, (byte)1, (byte)1, (byte)1, (byte)1, (byte)1, (byte)1, (byte)1, (byte)1};
    expResult = 12;
    result = instance.decodeSerialData(data);
    assertEquals(expResult, result);
    assertTrue(isPosCfmEvent);
    assertTrue(!isNegCfmEvent);
    assertTrue(isAdcEvent);
    cleanFlags();
    
    data = new byte[]{(byte)0x2, (byte)0x2, (byte)0xfe, (byte)0xfe, (byte)2, (byte)1, (byte)1, (byte)1, (byte)1, (byte)1, (byte)1, (byte)1, (byte)1};
    expResult = 13;
    result = instance.decodeSerialData(data);
    assertEquals(expResult, result);
    assertTrue(isPosCfmEvent);
    assertTrue(!isNegCfmEvent);
    assertTrue(isAdcEvent);
    cleanFlags();
    
    data = new byte[]{(byte)0xfe, (byte)0xfe, (byte)2, (byte)1, (byte)1, (byte)1, (byte)1, (byte)1, (byte)1, (byte)1, (byte)1, (byte)0xfe, (byte)0xfe};
    expResult = 11;
    result = instance.decodeSerialData(data);
    assertEquals(expResult, result);
    assertTrue(isPosCfmEvent);
    assertTrue(!isNegCfmEvent);
    assertTrue(isAdcEvent);
    cleanFlags();
    
    data = new byte[]{(byte)0xfe, (byte)0xfe, (byte)2, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xfe};
    expResult = 11;
    result = instance.decodeSerialData(data);
    assertEquals(expResult, result);
    assertTrue(isPosCfmEvent);
    assertTrue(!isNegCfmEvent);
    assertTrue(isAdcEvent);
    assertTrue(instance.getForwardV()==26214);
    assertTrue(instance.getBackwardV()==26214);
    assertTrue(instance.getAntennaV()==26214);
    assertTrue(instance.getPowerSupplyV()==26214);
    cleanFlags();
    
    data = new byte[]{(byte)0xfd, (byte)0xfe, (byte)0xfe, (byte)2, (byte)0xab, (byte)0x11, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xfe};
    expResult = 12;
    result = instance.decodeSerialData(data);
    assertEquals(expResult, result);
    assertTrue(isPosCfmEvent);
    assertTrue(!isNegCfmEvent);
    assertTrue(isAdcEvent);
    assertTrue(instance.getForwardV()==17517);
    assertTrue(instance.getBackwardV()==26214);
    assertTrue(instance.getAntennaV()==26214);
    assertTrue(instance.getPowerSupplyV()==26214);
    cleanFlags();
    
    data = new byte[]{(byte)0xfd, (byte)0xfe, (byte)0xfe, (byte)2, (byte)0xFF, (byte)0xFF, (byte)0xab, (byte)0x11,  (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xfe};
    expResult = 12;
    result = instance.decodeSerialData(data);
    assertEquals(expResult, result);
    assertTrue(isPosCfmEvent);
    assertTrue(!isNegCfmEvent);
    assertTrue(isAdcEvent);
    assertTrue(instance.getForwardV()==26214);
    assertTrue(instance.getBackwardV()==17517);
    assertTrue(instance.getAntennaV()==26214);
    assertTrue(instance.getPowerSupplyV()==26214);
    cleanFlags();
    
    data = new byte[]{(byte)0xfd, (byte)0xfe, (byte)0xfe, (byte)2, (byte)0xFF, (byte)0xFF,  (byte)0xFF, (byte)0xFF, (byte)0xab, (byte)0x11, (byte)0xFF, (byte)0xFF, (byte)0xfe};
    expResult = 12;
    result = instance.decodeSerialData(data);
    assertEquals(expResult, result);
    assertTrue(isPosCfmEvent);
    assertTrue(!isNegCfmEvent);
    assertTrue(isAdcEvent);
    assertTrue(instance.getForwardV()==26214);
    assertTrue(instance.getBackwardV()==26214);
    assertTrue(instance.getAntennaV()==17517);
    assertTrue(instance.getPowerSupplyV()==26214);
    cleanFlags();
    
    data = new byte[]{(byte)0xfd, (byte)0xfe, (byte)0xfe, (byte)2, (byte)0xFF, (byte)0xFF,  (byte)0xFF, (byte)0xFF,  (byte)0xFF, (byte)0xFF, (byte)0xab, (byte)0x11, (byte)0xfe};
    expResult = 12;
    result = instance.decodeSerialData(data);
    assertEquals(expResult, result);
    assertTrue(isPosCfmEvent);
    assertTrue(!isNegCfmEvent);
    assertTrue(isAdcEvent);
    assertTrue(instance.getForwardV()==26214);
    assertTrue(instance.getBackwardV()==26214);
    assertTrue(instance.getAntennaV()==26214);
    assertTrue(instance.getPowerSupplyV()==17517);
    cleanFlags();
    
    data = new byte[]{(byte)0xfd, (byte)0xfe, (byte)0xfe, (byte)2, (byte)0x0, (byte)0x0,  (byte)0x0, (byte)0x1,  (byte)0x0, (byte)0x2, (byte)0x0, (byte)0x3, (byte)0xfe};
    expResult = 12;
    result = instance.decodeSerialData(data);
    assertEquals(expResult, result);
    assertTrue(isPosCfmEvent);
    assertTrue(!isNegCfmEvent);
    assertTrue(isAdcEvent);
    assertTrue(instance.getForwardV()==0);
    assertTrue(instance.getBackwardV()==0);
    assertTrue(instance.getAntennaV()==1);
    assertTrue(instance.getPowerSupplyV()==1);
    cleanFlags();
    
    data = new byte[]{(byte)0xfd, (byte)0xfe, (byte)0xfe, (byte)2, (byte)0x0, (byte)0x4,  (byte)0x0, (byte)0x5,  (byte)0x0, (byte)0x6, (byte)0x0, (byte)0x7, (byte)0xfe};
    expResult = 12;
    result = instance.decodeSerialData(data);
    assertEquals(expResult, result);
    assertTrue(isPosCfmEvent);
    assertTrue(!isNegCfmEvent);
    assertTrue(isAdcEvent);
    assertTrue(instance.getForwardV()==2);
    assertTrue(instance.getBackwardV()==2);
    assertTrue(instance.getAntennaV()==2);
    assertTrue(instance.getPowerSupplyV()==3);
    cleanFlags();
    
    data = new byte[]{(byte)0xfd, (byte)0xfe, (byte)0xfe, (byte)2, (byte)0xff, (byte)0xfe,  (byte)0xff, (byte)0xfd,  (byte)0xff, (byte)0xfc, (byte)0xff, (byte)0xfb, (byte)0xfe};
    expResult = 12;
    result = instance.decodeSerialData(data);
    assertEquals(expResult, result);
    assertTrue(isPosCfmEvent);
    assertTrue(!isNegCfmEvent);
    assertTrue(isAdcEvent);
    assertTrue(instance.getForwardV()==26214);
    assertTrue(instance.getBackwardV()==26213);
    assertTrue(instance.getAntennaV()==26213);
    assertTrue(instance.getPowerSupplyV()==26212);
    cleanFlags();
    
    data = new byte[]{(byte)0xfd, (byte)0xfe, (byte)0xfe, (byte)2, (byte)0xff, (byte)0xff,  (byte)0x0, (byte)0x0,  (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff};
    expResult = 12;
    result = instance.decodeSerialData(data);
    assertEquals(expResult, result);
    assertTrue(isPosCfmEvent);
    assertTrue(!isNegCfmEvent);
    assertTrue(isAdcEvent);
    assertTrue(instance.getSwr()==1);
    assertTrue(instance.getForwardV()==26214);
    assertTrue(instance.getBackwardV()==0);
    assertTrue(instance.getAntennaV()==26214);
    assertTrue(instance.getPowerSupplyV()==26214);
    cleanFlags();
    
    data = new byte[]{(byte)0xfd, (byte)0xfe, (byte)0xfe, (byte)2, (byte)0xff, (byte)0xff,  (byte)0xFF, (byte)0xFF,  (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff};
    expResult = 12;
    result = instance.decodeSerialData(data);
    assertEquals(expResult, result);
    assertTrue(isPosCfmEvent);
    assertTrue(!isNegCfmEvent);
    assertTrue(isAdcEvent);
    assertTrue(instance.getSwr()==100);
    assertTrue(instance.getForwardV()==26214);
    assertTrue(instance.getBackwardV()==26214);
    assertTrue(instance.getAntennaV()==26214);
    assertTrue(instance.getPowerSupplyV()==26214);
    cleanFlags();
    
    data = new byte[]{(byte)0xfd, (byte)0xfe, (byte)0xfe, (byte)2, (byte)0xff, (byte)0xff,  (byte)0xFF, (byte)0xFF,  (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff};
    expResult = 12;
    result = instance.decodeSerialData(data);
    assertEquals(expResult, result);
    assertTrue(isPosCfmEvent);
    assertTrue(!isNegCfmEvent);
    assertTrue(isAdcEvent);
    assertTrue(instance.getSwr()==100);
    assertTrue(instance.getForwardV()==26214);
    assertTrue(instance.getBackwardV()==26214);
    assertTrue(instance.getAntennaV()==26214);
    assertTrue(instance.getPowerSupplyV()==26214);
    cleanFlags();

  }

  
  class LocalListener implements TunerController.TunerControllerListener
  {
      @Override
      public void eventAdc()
      {
        isAdcEvent = true;
      }

      @Override
      public void eventPosConfirmation()
      {
        isPosCfmEvent = true;
      }

      @Override
      public void eventNegConfirmation()
      {
        isNegCfmEvent = true;
      }
    };
  
}
