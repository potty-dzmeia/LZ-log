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
package org.lz1aq.rsi;

import org.lz1aq.rsi.event.RadioListener;
import java.util.Iterator;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONObject;
import org.lz1aq.rsi.event.*;
import org.lz1aq.utils.RadioModes;
import org.lz1aq.utils.RadioVfos;


/**
 *  For parsing radio events in JSON format and notifying the interested parties 
 */
public class JsonMsgParser
{
  // Currently supported JSON messages comming from the radio
  public static final String NOT_SUPPORTED_MSG  = "not_supported";
  public static final String CONFIRMATION_MSG   = "confirmation";
  public static final String FREQUENCY_MSG      = "frequency";
  public static final String MODE_MSG           = "mode";
  public static final String SMETER_MSG         = "smeter";
  public static final String ACTIVE_VFO         = "active_vfo";
  
  // A field wich can be found inside the FREQUENCY_MSG and MODE_MSG
  public static final String VFO_PAR            = "vfo";

  
  private static final Logger logger = Logger.getLogger(JsonMsgParser.class.getName());
  
  
  /**
   * Parses the JSON formatted message and notifies the listeners
   * 
   * @param jsoString - JSON formatted string of the type: {"some_command_name": { ...data...}}
   * @param listeners - Listeners which will be notified for the content of the JSON message
   */
  public static void parseAndNotify(String jsoString, CopyOnWriteArrayList<RadioListener> listeners)
  {
    
     // Get the command (i.e. the name of the object) that the radio has sent us
    JSONObject jso = new JSONObject(jsoString);
    Iterator<?> keys = jso.keys();
    
    if(keys.hasNext()==false)
    {
      logger.log(Level.SEVERE, "dispatchEvent(): We received an empty decoded transaction");
      return;
    }
    
    // We can have several messages inside the JSON block...
    while(keys.hasNext())
    {
      String command = (String)keys.next();
      
      switch (command)
      {
        // -----------------------------
        case JsonMsgParser.CONFIRMATION_MSG:
          ConfirmationEvent cfmEv = parseConfirmationMsg(jso);
          for (RadioListener listener : listeners)
          {
            listener.eventConfirmation(cfmEv);
          }
          break;

        // -----------------------------
        case JsonMsgParser.FREQUENCY_MSG:
          // -----------------------------
          FrequencyEvent freqEv = parseFrequencyMsg(jso.getJSONObject(command));
          for (RadioListener listener : listeners)
          {
            listener.eventFrequency(freqEv);
          }
          break;

        // -----------------------------
        case JsonMsgParser.MODE_MSG:
        // -----------------------------
          ModeEvent modeEv = parseModeMsg(jso.getJSONObject(command));
          for (RadioListener listener : listeners)
          {
            listener.eventMode(modeEv);
          }
          break;
        
        // -----------------------------
        case JsonMsgParser.ACTIVE_VFO:
        // -----------------------------
          ActiveVfoEvent activeVfoEv = parseActiveVfoMsg(jso.getJSONObject(command));
          for (RadioListener listener : listeners)
          {
            listener.eventActiveVfo(activeVfoEv);
          }
          break;
          
        // -----------------------------
        case JsonMsgParser.SMETER_MSG:
        // -----------------------------
          SmeterEvent smeterEv = parseSmeterMsg(jso.getJSONObject(command));
          for (RadioListener listener : listeners)
          {
            listener.eventSmeter(smeterEv);
          }
          break;

        // -----------------------------
        case JsonMsgParser.NOT_SUPPORTED_MSG:
          // -----------------------------
          NotsupportedEvent unsupportedEv = parseNotsupportedMsg(jso);
          for (RadioListener listener : listeners)
          {
            listener.eventNotsupported(unsupportedEv);
          }
          break;
            

        // -----------------------------
        default:
          // -----------------------------
          logger.log(Level.SEVERE, "JSON message contained an unknown command: " + command);
          break;
      }
    }// for each message
    
  }
  
 
  /**
   * Parses an object containing data that couldn't be decoded by
   * @param jso - JSON object of the type: {"not-supported":"the data that couldn't be decoded in hex format"}
   * @return event holding the parsed data
   */
  private static NotsupportedEvent parseNotsupportedMsg(JSONObject jso)
  {
    if(jso.has(NOT_SUPPORTED_MSG))
    {
      return new NotsupportedEvent(jso.getString(NOT_SUPPORTED_MSG));
    }
    else
    {    
      logger.log(Level.SEVERE, "JSON object didn't contain " + NOT_SUPPORTED_MSG + " key as expected");
      return new NotsupportedEvent("");
    }
  }
  
  
  /**
   * Parser an object containing positive or negative Confirmation
   * 
   * @param jso - JSON object of the type: {"confirmation": "0" or "1"}     <-- "0" is for negative; "1" is for positive
   * @return event holding the parsed data
   */
  private static ConfirmationEvent parseConfirmationMsg(JSONObject jso)
  {
    if(jso.has(CONFIRMATION_MSG))
    {
      return new ConfirmationEvent(jso.getString(CONFIRMATION_MSG).equals("1"));
    }
    else
    {    
      logger.log(Level.SEVERE, "JSON object didn't contain " + CONFIRMATION_MSG + " key as expected");
      return new ConfirmationEvent(false);
    }
  }
  
  
  /**
   * Parser an object containing Frequency and some additional data (e.g. VFO)
   * 
   * @param jso - JSON object of the type:
   *    {
   *      "frequency": "14190000"
   *      "vfo": "0" or "1" and so on..     <--Optional - indicates which VFO changed its frequency
   *    }
   * @return event holding the parsed data
   */
  private static FrequencyEvent parseFrequencyMsg(JSONObject jso)
  {
    String  freq = "0";

    if(jso.has(FREQUENCY_MSG))
    {
      freq = jso.getString(FREQUENCY_MSG);
    }
    else
    {
      logger.log(Level.SEVERE, "JSON object didn't contained " + FREQUENCY_MSG + " key as expected");
    }
    
    return new FrequencyEvent(freq, parseVfoField(jso));
  }
  
  
  /**
   * Parser an object containing Mode and some additional data (e.g. VFO)
   * 
   * @param jso - JSON object of the type:
   *    {
   *      "mode": "cw"
   *      "vfo": "0" or "1" and so on..     <--Optional - indicates which VFO changed its frequency
   *    }
   * @return event holding the parsed data
   */
  private static ModeEvent parseModeMsg(JSONObject jso)
  {
    RadioModes  mode = RadioModes.NONE;
    
    if(jso.has(MODE_MSG))
    {    
      mode = RadioModes.valueOf(jso.getString(MODE_MSG).toUpperCase());
    }
    else
    {
      logger.severe("JSON object didn't contained " + MODE_MSG + " key as expected");
    }
    
    return new ModeEvent(mode, parseVfoField(jso));
  }
  
  
  
  private static ActiveVfoEvent parseActiveVfoMsg(JSONObject jso)
  {
    return new ActiveVfoEvent(parseVfoField(jso));
  }       
          
          
  /**
   * Parser an object containing Mode and some additional data (e.g. VFO)
   * 
   * @param jso - JSON object of the type:
   *    {
   *      "smeter": "30"
   *    }
   * @return event holding the parsed data
   */
  private static SmeterEvent parseSmeterMsg(JSONObject jso)
  {
    int smeter = 0; 
    
    if(jso.has(SMETER_MSG))
    {  
      try{
        smeter = Integer.parseInt(jso.getString(SMETER_MSG));
      }catch(Exception exc)
      {
          smeter = 0;
      }
    }
    else
    {
      logger.severe("JSON object didn't contained " + SMETER_MSG + " key as expected");
    }
    
    return new SmeterEvent(smeter);
  }

  
  /**
   *  Extracts the string from the "VFO" field
   * 
   * @param jso - JSON object containing that we will try to extract the field from
   * @return RadioVfos object specifying the VFO. RadioVfos.NONE in case the 
   *         field was missing or was set to None
   */
  private static RadioVfos parseVfoField(JSONObject jso)
  {
    if (jso.has(VFO_PAR))
    {
      int i = Integer.parseInt(jso.getString(VFO_PAR));
      for(RadioVfos vfo: RadioVfos.values())
      {
        if(vfo.getCode() == i)
          return vfo;
      }
    } 
    
    logger.warning("SON object didn't contained " + VFO_PAR +" key.");  
    return RadioVfos.NONE;
  }
  
}
