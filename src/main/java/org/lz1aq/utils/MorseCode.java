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

import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.python.jline.internal.Log;

public class MorseCode
{
  public static final char DOT = '.';
  public static final char DASH = '-';
  
  private static final int dashDotRatio = 3;
  private static final int markSpaceRatio = 1;
  private static final int charSpaceRatio = 3;
  private static final int wordSpaceRatio = 9;
  
  
  private static final HashMap morse = new HashMap();
  
  private static final Logger logger = Logger.getLogger(MorseCode.class.getName());

  static
  {  // perhaps not the best way to represent morse, but it's easy to deal with
    morse.put(new Character('A'), ".-");
    morse.put(new Character('B'), "-...");
    morse.put(new Character('C'), "-.-.");
    morse.put(new Character('D'), "-..");
    morse.put(new Character('E'), ".");
    morse.put(new Character('F'), "..-.");
    morse.put(new Character('G'), "--.");
    morse.put(new Character('H'), "....");
    morse.put(new Character('I'), "..");
    morse.put(new Character('J'), ".---");
    morse.put(new Character('K'), "-.-");
    morse.put(new Character('L'), ".-..");
    morse.put(new Character('M'), "--");
    morse.put(new Character('N'), "-.");
    morse.put(new Character('O'), "---");
    morse.put(new Character('P'), ".--.");
    morse.put(new Character('Q'), "--.-");
    morse.put(new Character('R'), ".-.");
    morse.put(new Character('S'), "...");
    morse.put(new Character('T'), "-");
    morse.put(new Character('U'), "..-");
    morse.put(new Character('V'), "...-");
    morse.put(new Character('W'), ".--");
    morse.put(new Character('X'), "-..-");
    morse.put(new Character('Y'), "-.--");
    morse.put(new Character('Z'), "--..");
    
    morse.put(new Character('a'), ".-");
    morse.put(new Character('b'), "-...");
    morse.put(new Character('c'), "-.-.");
    morse.put(new Character('d'), "-..");
    morse.put(new Character('e'), ".");
    morse.put(new Character('f'), "..-.");
    morse.put(new Character('g'), "--.");
    morse.put(new Character('h'), "....");
    morse.put(new Character('i'), "..");
    morse.put(new Character('j'), ".---");
    morse.put(new Character('k'), "-.-");
    morse.put(new Character('l'), ".-..");
    morse.put(new Character('m'), "--");
    morse.put(new Character('n'), "-.");
    morse.put(new Character('o'), "---");
    morse.put(new Character('p'), ".--.");
    morse.put(new Character('q'), "--.-");
    morse.put(new Character('r'), ".-.");
    morse.put(new Character('s'), "...");
    morse.put(new Character('t'), "-");
    morse.put(new Character('u'), "..-");
    morse.put(new Character('v'), "...-");
    morse.put(new Character('w'), ".--");
    morse.put(new Character('x'), "-..-");
    morse.put(new Character('y'), "-.--");
    morse.put(new Character('z'), "--..");

    morse.put(new Character('0'), "-----");
    morse.put(new Character('1'), ".----");
    morse.put(new Character('2'), "..---");
    morse.put(new Character('3'), "...--");
    morse.put(new Character('4'), "....-");
    morse.put(new Character('5'), ".....");
    morse.put(new Character('6'), "-....");
    morse.put(new Character('7'), "--...");
    morse.put(new Character('8'), "---..");
    morse.put(new Character('9'), "----.");

    morse.put(new Character('/'), "-..-.");
    morse.put(new Character('?'), "..--..");
    morse.put(new Character(','), "--..--");
    morse.put(new Character('.'), ".-.-.-");
    morse.put(new Character('-'), "-....-"); 
    morse.put(new Character('='), "-...-"); 
    morse.put(new Character(':'), "---...");
    morse.put(new Character(';'), "-.-.-.");
    morse.put(new Character('('), "-.--.");
    morse.put(new Character(')'), "-.--.-");
    morse.put(new Character('*'), "...-.-");  // SK (unnatural)
    morse.put(new Character('^'), ".-.-.");   // AR (unnatural)
  }


  /**
   * Get the Morse code representation of a character
   * 
   * @param ch character for which we will get the Morse code representation.
   * @return string containing DOTs and DASHes (see this.DOT and this.DASH)
   */
  static public String getCode(Character ch)
  {
    return (String)morse.get(ch);
  }
  
  
  /**
   * How much time it will take to transmit the specified "text" message
   * 
   * @param text the message that we would like to know how long it will take to transmit
   * @param wpm the speed with which we will transmit the message (words per minute)
   * @return Duration of the transmission in milliseconds
   */
  static public int getDurationOfMessage(String text, int wpm)
  {
    int durationInMsec = 0; // duration it milliseconds
    
    for(char ch:text.toCharArray())
    {
      durationInMsec +=getDurationOfCharacter(ch, wpm);
    }  
    
    return durationInMsec;
  }
  
  
  /**
   * How much time it will take to transmit the given character
   * 
   * 
   * @param character - the char which length we want to know
   * @param wpm - the speed with which the char will be transmitted
   * @return duration in seconds. Will return 0 if the char has no Morse code representation
   * 
   * 
   */
  static private int getDurationOfCharacter(char character, int wpm)
  {
    String tempStr; 
    double dotTime = 1.2 / (double) wpm;
    int dotMillis = (int) (dotTime * 1000);
    int duration = 0; // duration in dotTime
    
    if(isValidCharacter(character))
    {
      tempStr = (String)morse.get(character);
      for(char ch:tempStr.toCharArray())
      {
        if(ch == DOT)
        {
          duration +=1;
        }
        else if(ch == DASH)
        {
          duration +=dashDotRatio;
        }
        duration +=markSpaceRatio; //between mark
      }
      duration -= markSpaceRatio; // remove the extra space mark space that we have added
      duration +=charSpaceRatio;
    }
    else if(character==' ')
    {
      duration = wordSpaceRatio;
    }
    else
    {
      logger.log(Level.SEVERE, "Not a valid character");
    }
    
    
    return duration*dotMillis;
  }
  
  
  static public boolean isValidCharacter(char ch)
  {
    return morse.containsKey(new Character(ch));
  }
  
}
