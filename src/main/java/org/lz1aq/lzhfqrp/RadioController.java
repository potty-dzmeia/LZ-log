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
package org.lz1aq.lzhfqrp;

import java.util.EventListener;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import jssc.SerialPortException;
import org.apache.commons.lang3.StringUtils;
import org.lz1aq.py.rig.I_Radio;
import org.lz1aq.py.rig.I_SerialSettings;
import org.lz1aq.rsi.Radio;
import org.lz1aq.rsi.event.ActiveVfoEvent;
import org.lz1aq.rsi.event.ConfirmationEvent;
import org.lz1aq.rsi.event.FrequencyEvent;
import org.lz1aq.rsi.event.ModeEvent;
import org.lz1aq.rsi.event.NotsupportedEvent;
import org.lz1aq.rsi.event.RadioListener;
import org.lz1aq.rsi.event.SmeterEvent;
import org.lz1aq.utils.RadioModes;
import org.lz1aq.utils.RadioVfos;

/**
 *
 * @author potty
 * 
 * Represents the 
 */
public class RadioController
{
  private boolean isConnected = false;
  private int freqVfoA = 14000000;
  private int freqVfoB = 14000000; 
  private RadioModes modeVfoA = RadioModes.NONE;
  private RadioModes modeVfoB = RadioModes.NONE;
  private RadioVfos activeVfo = RadioVfos.A;
  private final CopyOnWriteArrayList<RadioControllerListener>  eventListeners;
  private Radio         radio;
  private I_Radio       radioParser;  
  
  private static final Logger logger = Logger.getLogger(Radio.class.getName());
  
  
  /**
   * Before being able to use this class you need to call the following methods:
   * 1. loadProtocolParser()
   * 2. connect()
   */
  public RadioController()
  {
    eventListeners        = new CopyOnWriteArrayList<>();
  }
  
  
  /**
   *  
   * @param filenameOfPythonFile
   * @return 
   */
  public boolean loadProtocolParser(String filenameOfPythonFile)
  {
    try
    {
      // Create radioParser object from the python Class
      String moduleName = StringUtils.removeEnd(filenameOfPythonFile, ".py");

      String className = StringUtils.capitalize(moduleName); // The name of the Class withing the module(file) should be with the same name but with capital letter

      // Create radioParser object from the python Class
      JythonObjectFactory f2 = new JythonObjectFactory(I_Radio.class, moduleName, className);
      radioParser = (I_Radio) f2.createObject(); 
      return true;
      
    }catch(Exception exc)
    {
      Logger.getLogger(RadioController.class.getName()).log(Level.SEVERE, null, exc);
      return false;
    }
  }
  
  
  public boolean connect(String commport, RadioControllerListener listener)
  {
    if(radioParser == null)
      return false;
    
    
    try
    {
      //Create the radio object using the selected Com port
      radio = new Radio(radioParser, commport);
      radio.addEventListener(new RadioController.LocalRadioListener());
      radio.connect(); // Let's not forget to call connect(). Calling disconnects() later will close the Com Port
      eventListeners.add(listener);
      
      
      radio.getFrequency(RadioVfos.A.getCode());
      radio.getMode(RadioVfos.A.getCode());
      radio.getFrequency(RadioVfos.B.getCode());
      radio.getMode(RadioVfos.B.getCode());
      radio.getActiveVfo();
    }
    catch(jssc.SerialPortException exc)
    {
      JOptionPane.showMessageDialog(null, exc.getExceptionType(), "Error...", JOptionPane.ERROR_MESSAGE);
      isConnected = false;
      return false;
    }
    catch(Exception exc)
    {
      Logger.getLogger(RadioController.class.getName()).log(Level.SEVERE, null, exc);
      isConnected = false;
      return false;
    }
   
    
    isConnected = true;
    return true;
  }
  
  
  public void disconnect()
  {
    try
    {
      radio.disconnect();
      isConnected = false;
    }
    catch (SerialPortException ex)
    {
      Logger.getLogger(RadioController.class.getName()).log(Level.SEVERE, null, ex);
    }
  }
  
  
  /**
   * Checks if there is active serial connection to the radio
   * @return true - if connected
   */
  public boolean isConnected()
  {
    return isConnected;
  }
  
  
  /**
   * Read the frequency from the VFO that is currently in use
   * 
   * @return 
   */
  public int getFrequency()
  {
    if(activeVfo == RadioVfos.A)
      return freqVfoA;
    else
      return freqVfoB;
  }
  
  public RadioVfos getActiveVfo()
  {
    return activeVfo;
  }
  /**
   *  Set the frequency of the currently active VFO
   * @param freq
   */
  public void setFrequency(long freq)
  {
    if (!isConnected())
      return;
    
    
    try
    {
      if(activeVfo == RadioVfos.A)
      {
        radio.setFrequency(freq, RadioVfos.A.getCode());
      }
      else
      {
        radio.setFrequency(freq, RadioVfos.B.getCode());
      }
    }catch (Exception ex) 
    {
      Logger.getLogger(RadioController.class.getName()).log(Level.SEVERE, null, ex);
    }
  }
  
  
  public RadioModes getMode()
  {
    if(activeVfo == RadioVfos.A)
      return modeVfoA;
    else
      return modeVfoB;
  }
  
  
  public void sendMorse(String text)
  {
    if (!isConnected())
      return;
    
    try
    {
      radio.sendCW(text);
    }
    catch (Exception ex)
    {
      Logger.getLogger(RadioController.class.getName()).log(Level.SEVERE, null, ex);
    }
  }
  
  public void setKeyerSpeed(int speed)
  {
    if (!isConnected())
      return;
    
    try
    {
      radio.setKeyerSpeed(speed);
    }
    catch (Exception ex)
    {
      Logger.getLogger(RadioController.class.getName()).log(Level.SEVERE, null, ex);
    }
    
  }
  
  
  public void interruptMorseSending()
  {
    if (!isConnected())
      return;
    
    try
    {
      radio.interruptSendCW();
    }
    catch (Exception ex)
    {
      Logger.getLogger(RadioController.class.getName()).log(Level.SEVERE, null, ex);
    }
  }

  
  
  public String getInfo()
  {
    I_SerialSettings serialSettings = radioParser.getSerialPortSettings();
    
    String info = "manufacturer: " + radioParser.getManufacturer() + "\n" + 
                  "\n" +
                  "model: " + radioParser.getModel() +
                  "\n"+
                  "serial port settings: " + serialSettings.toString()+
                  "\n";
   return info;
  }
  
    
  
  public void addEventListener(RadioControllerListener listener) throws Exception
  {
    this.eventListeners.add(listener);
  }
  
  
  public void removeEventListener(RadioControllerListener listener)
  { 
    this.eventListeners.remove(listener);
  }
  
  
  /**
   * Handlers for events coming from the radio
   */
  private class LocalRadioListener implements RadioListener
  {
    @Override
    public void eventNotsupported(NotsupportedEvent e)
    {
    // not interested
    }

    @Override
    public void eventConfirmation(ConfirmationEvent e)
    {
    // not interested
    }

    @Override
    public void eventFrequency(final FrequencyEvent e)
    {
      try
      {
        if (e.getVfo() == RadioVfos.A)
        {
          freqVfoA = Integer.parseInt(e.getFrequency()); //Misc.formatFrequency(e.getFrequency());
        } else if (e.getVfo() == RadioVfos.B)
        {
          freqVfoB = Integer.parseInt(e.getFrequency());
        } else
        {
          logger.warning("Frequency event from unknown VFO!");
          return;
        }

        // Notify any listeners
        for (RadioControllerListener listener : eventListeners)
        {
          listener.frequency();
        }
      }catch(Exception exc)
      {
        try
        {
          // frequency data was damaged - request the data again
          radio.getFrequency(e.getVfo().getCode());
        }
        catch (Exception ex)
        {
          // do nothing
        }
      }
    }

    
    @Override
    public void eventMode(final ModeEvent e)
    {
      if (e.getVfo() == RadioVfos.A)
      {
        modeVfoA = e.getMode();
      } 
      else if (e.getVfo() == RadioVfos.B)
      {
        modeVfoB = e.getMode();
      } 
      // No information for the VFO - we need to deduce which Vfo mode was changed
      else if(e.getVfo() == RadioVfos.NONE)
      {
        if(activeVfo == RadioVfos.A)
          modeVfoA = e.getMode();
        else
          modeVfoB = e.getMode();
      }
      else
      {
        logger.warning("Unknown VFO number was received!");
      }

      // Notify any listeners
      for (RadioControllerListener listener : eventListeners)
      {
        listener.mode();
      }
    }

    
    @Override
    public void eventSmeter(SmeterEvent e)
    {
      // Not interested
    }

    
    @Override
    public void eventActiveVfo(ActiveVfoEvent e)
    {
      activeVfo = e.getVfo();
      
       // Notify any listeners
      for (RadioControllerListener listener : eventListeners)
      {
        listener.vfo();
      }
    }
  }
  
  public interface RadioControllerListener extends EventListener
  {
    public void frequency();
    public void mode();
    public void vfo();
  }
}
