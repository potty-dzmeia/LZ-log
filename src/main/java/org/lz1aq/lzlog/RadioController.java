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

import java.util.EventListener;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import jssc.SerialPort;
import jssc.SerialPortException;
import org.apache.commons.lang3.StringUtils;
import org.lz1aq.keyer.Keyer;
import org.lz1aq.ptt.Ptt;
import org.lz1aq.py.rig.I_Radio;
import org.lz1aq.py.rig.I_SerialSettings;
import org.lz1aq.radio.Radio;
import org.lz1aq.radio.event.ActiveVfoEvent;
import org.lz1aq.radio.event.ConfirmationEvent;
import org.lz1aq.radio.event.FrequencyEvent;
import org.lz1aq.radio.event.ModeEvent;
import org.lz1aq.radio.event.NotsupportedEvent;
import org.lz1aq.radio.event.RadioListener;
import org.lz1aq.radio.event.SmeterEvent;
import org.lz1aq.radio.RadioModes;
import org.lz1aq.radio.RadioVfos;
import org.lz1aq.utils.MorseCode;

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
  private RadioModes modeVfoA = RadioModes.CW;
  private RadioModes modeVfoB = RadioModes.CW;
  private RadioVfos activeVfo = RadioVfos.A;
  private final CopyOnWriteArrayList<RadioControllerListener>  eventListeners;
  private Radio         radio;
  private I_Radio       radioParser;  
  private final Keyer   keyer = new RadioKeyer();
  
  
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
      logger.log(Level.SEVERE, null, exc);
      return false;
    }
  }
  
  /**
   * Establishes connection with the Radio.
   * Preconditions:
   * - Call loadProtocolParser() before using this function.
   * @param listener
   * @param commport  - should be an already opened commport
   * @param baudRate
   * @return 
   */
  public boolean connect(RadioControllerListener listener, SerialPort commport, int baudRate)
  {
    if(radioParser == null)
      return false;
    
    if(commport==null || !commport.isOpened())
    {
      logger.warning("The supplied commport is not opened!");
      return false;
    }
 
    
    try
    {
      //Create the radio object using the selected Com port
      radio = new Radio(radioParser, commport);
      setComPortParams(commport, baudRate);
      radio.addEventListener(new RadioController.LocalRadioListener());
      radio.connect(); // Let's not forget to call connect(). Calling disconnects() later will close the Com Port
      eventListeners.add(listener);
      
      radio.getFrequency(RadioVfos.A);
      radio.getMode(RadioVfos.A);
      radio.getFrequency(RadioVfos.B);
      radio.getMode(RadioVfos.B);
      radio.getActiveVfo();
    }
    catch(SerialPortException exc)
    {
      if(exc.getExceptionType().equals(SerialPortException.TYPE_PORT_BUSY))
      {
        JOptionPane.showMessageDialog(null, "You are trying to connect to com port that is already in use. \nIf you want to use the DTR/STR keyer on the same com port \n with the radio - please connect first to the radio and then to the keyer.", exc.getExceptionType(), JOptionPane.ERROR_MESSAGE);
      }
      else
      {
        JOptionPane.showMessageDialog(null, exc, exc.getExceptionType(), JOptionPane.ERROR_MESSAGE);
      }
      isConnected = false;
      disconnect();
      return false;
    }               
    catch(Exception exc)
    {
      JOptionPane.showMessageDialog(null, exc, "Error...", JOptionPane.ERROR_MESSAGE);
      isConnected = false;
      disconnect();
      return false;
    }
    
    isConnected = true;
    return true;
  }
  
  
  public void disconnect()
  {
    if(!isConnected)
      return;
    
    try
    {
      radio.disconnect();
      isConnected = false;
    }
    catch (SerialPortException ex)
    {
      logger.log(Level.SEVERE, null, ex);
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
   *  Set the Frequency of the currently active VFO
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
        radio.setFrequency(freq, RadioVfos.A);
        radio.getFrequency(RadioVfos.A); // workaround for Icom/Yeasu transcievers. They don't send update when frequency is changed through the CAT
      }
      else
      {
        radio.setFrequency(freq, RadioVfos.B);
        radio.getFrequency(RadioVfos.B); // workaround for Icom/Yeasu transcievers. They don't send update when frequency is changed through the CAT
      }
      
      
    }catch (Exception ex) 
    {
      logger.log(Level.SEVERE, null, ex);
    }
  }
  
   /**
   *  Set the Mode of the currently active VFO
   * @param mode 
   */
  public void setMode(RadioModes mode)
  {
    if (!isConnected())
      return;
    
    try
    {
      if(activeVfo == RadioVfos.A)
      {
        radio.setMode(mode, RadioVfos.A);
        radio.getMode(RadioVfos.A); // workaround for Icom/Yaesu transcievers. They don't send update when frequency is changed through the CAT
      }
      else
      {
        radio.setMode(mode, RadioVfos.B);
        radio.getMode(RadioVfos.B); // workaround for Icom/Yaesu transcievers. They don't send update when frequency is changed through the CAT
      }
      
      
    }catch (Exception ex) 
    {
      logger.log(Level.SEVERE, null, ex);
    }
  }
  
  
  public RadioModes getMode()
  {
    if(activeVfo == RadioVfos.A)
      return modeVfoA;
    else
      return modeVfoB;
  }
  
  
  /**
   * Call this function to get access to a Keyer object which is responsible for transmitting Morse code.
   * @return Keyer interface for sending CW
   */
  public Keyer getKeyer()
  {
    return keyer;
  }
  
  
  private void transmitCW(String text)
  {
    if (!isConnected())
      return;
    
    try
    {
      radio.sendCW(text);
    }
    catch (Exception ex)
    {
      logger.log(Level.SEVERE, null, ex);
    }
  }
  
 
  private void setCwWpm(int wpm)
  {
    if (!isConnected())
      return;
    
    try
    {
      radio.setKeyerSpeed(wpm);
    }
    catch (Exception ex)
    {
      logger.log(Level.SEVERE, null, ex);
    }
    
  }
  
  
  private void interruptCwTransmit()
  {
    if (!isConnected())
      return;
    
    try
    {
      radio.interruptSendCW();
    }
    catch (Exception ex)
    {
      logger.log(Level.SEVERE, null, ex);
    }
  }

  
  public void poll()
  {
    if (!isConnected())
      return;
    
    try
    {
      radio.poll();
    }
    catch (Exception ex)
    {
      logger.log(Level.SEVERE, null, ex);
    }
  }
  
  public void setAutomaticInfo(boolean isActive)
  {
    if (!isConnected())
      return;
    
    try
    {
      radio.setAutomaticInfo(isActive);
    }
    catch (Exception ex)
    {
      logger.log(Level.SEVERE, null, ex);
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
  
  public SerialPort getSerialPort()
  {
    return radio.getSerialPort();
  }
  
  
  
  /**
   * Sets Com port parameters
   *
   * @param port The serial port which parameters will be adjusted
   * @param settings Source from which the values will be taken
   */
  private void setComPortParams(SerialPort commport, int baudrate) throws Exception
  {
    I_SerialSettings serialSettings = radioParser.getSerialPortSettings();
    
    int databits; 
    int stopbits;
    int handshake;
    int parity;

    
    switch(serialSettings.getStopBits()) // stopbits values are defined in serial_setting.py
    {
      case 1:
        stopbits = SerialPort.STOPBITS_1;
        break;
      case 2:
        stopbits = SerialPort.STOPBITS_1_5;
        break;
      case 3:
        stopbits = SerialPort.STOPBITS_2;
        break;
      default:
        throw new Exception("Unsupported StopBits: " + serialSettings.getStopBits() + ". Correct the python configuration file!");
    }
    
    switch(serialSettings.getDataBits()) // stopbits values are defined in serial_setting.py
    {
      case 5:
        databits = SerialPort.DATABITS_5;
        break;
      case 6:
        databits = SerialPort.DATABITS_6;
        break;
      case 7:
        databits = SerialPort.DATABITS_7;
        break;
      case 8:
        databits = SerialPort.DATABITS_8;
        break;
      default:
        throw new Exception("Unsupported Databits: " + serialSettings.getDataBits() + ". Correct the python configuration file!");
    }

    switch(serialSettings.getHandshake().toLowerCase())
    {
      case "none":
        handshake = SerialPort.FLOWCONTROL_NONE;
        break;
      default:
        throw new Exception("Unsupported Handshake: " + serialSettings.getHandshake() + ". Correct the python configuration file!");
    }
    
    switch(serialSettings.getParity().toLowerCase())
    {
      case "none":
        parity = SerialPort.PARITY_NONE;
        break;

      case "odd":
        parity = SerialPort.PARITY_ODD;
        break;

      case "even":
        parity = SerialPort.PARITY_EVEN;
        break;

      case "mark":
        parity = SerialPort.PARITY_MARK;
        break;

      case "space":
        parity = SerialPort.PARITY_SPACE;
        break;
        
      default:
        throw new Exception("Unsupported Parity: " + serialSettings.getParity() + ". Correct the python configuration file!");
    }

    
    commport.setFlowControlMode(handshake);
    commport.setParams(baudrate,
                       databits,
                       stopbits,
                       parity);
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
        } 
        else if (e.getVfo() == RadioVfos.B)
        {
          freqVfoB = Integer.parseInt(e.getFrequency());
        } 
        else //(e.getVfo() == RadioVfos.NONE)
        {
          if(activeVfo == RadioVfos.A)
            freqVfoA = Integer.parseInt(e.getFrequency());
          else
            freqVfoB = Integer.parseInt(e.getFrequency());  
          
          logger.warning("Frequency event from unknown VFO! Will consider it as VFO A.");
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
          radio.getFrequency(e.getVfo());
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
      
      try
      {
        // Work around for Yaesu
        // Yaesu radios first are reporting Mode change and then VFO change.
        // Thus, we need to get the Mode for the new VFO
        if(radio.getManufacturer().equalsIgnoreCase("Yaesu"))
        {
          radio.getMode(activeVfo);
        }
      }
      catch(Exception ex)
      {
        logger.log(Level.SEVERE, null, ex);
      }
    }
  }
  
  public interface RadioControllerListener extends EventListener
  {
    public void frequency();
    public void mode();
    public void vfo();
  }
  
  private class RadioKeyer implements Keyer
  {

    @Override
    public void init() throws Exception
    {
      // nothing to do during connect
    }

    @Override
    public void terminate()
    {
      // nothing to do during disconnect
    }

    @Override
    public void sendCw(String text)
    {
      if(MorseCode.isValidMessage(text))
      {
        transmitCW(text);
      }
    }

    @Override
    public void stopSendingCw()
    {
      interruptCwTransmit();
    }

    @Override
    public void setCwSpeed(int wpm)
    {
      setCwWpm(wpm);
    }

    @Override
    public void usePtt(Ptt ptt)
    {
      throw new UnsupportedOperationException("Not supported yet."); 
    }
    
    @Override
    public SerialPort getCommport()
    {
      throw new UnsupportedOperationException("Not supported yet."); 
    }
  }
  
  
}
