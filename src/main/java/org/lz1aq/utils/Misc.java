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


public class Misc
{

    /**
     * Converts array of bytes to string in which the bytes are presented in hex
     * format
     *
     * @param bytes - the bytes to be converted
     * @return String with the values represented in hex format
     */
    public static String toHexString(byte[] bytes)
    {
        StringBuilder stringBuilder = new StringBuilder();
        for(byte b : bytes)
        {
            stringBuilder.append(String.format("%02x ", b));
        }

        return stringBuilder.toString();
    }

    /**
     * Inserts "," between the thousands
     *
     * @param freqInHz - the frequency that we would like to format
     * @return - new frequency with the "," between the thousands
     */
    public static String formatFrequency(String freqInHz)
    {
        StringBuilder buf = new StringBuilder();
        int counter = 0;

        for(int i = freqInHz.length() - 1; i >= 0; i--)
        {
            counter++;
            buf.append(freqInHz.charAt(i));
            if(counter == 3 && i != 0)
            {
                buf.append(",");
                counter = 0;
            }
        }
        buf.reverse();

        return buf.toString();
    }

    /**
     * Inserts "," between the thousands and remove the deca hertz. Example
     * 3555543 becomes 3,555.5
     *
     * @param freqInHz - the frequency that we would like to format
     * @return - new frequency with the "," between the thousands
     */
    public static String toIncomingQsoFreq(String freqInHz)
    {
        String freq = formatFrequency(freqInHz);
        freq = freq.substring(0, freq.length() - 2);

        return freq;
    }

    /**
     * E.g. makes 3500500 to 550.5
     *
     * @param freqInHz - the frequency that we would like to format
     * @return - new frequency with the "," between the thousands
     */
    public static String toBandmapFreq(int freqInHz)
    {
        String freq = Integer.toString(freqInHz);

        StringBuilder buf = new StringBuilder();
        int counter = 0;

        for(int i = freq.length() - 3; i >= 0 && counter < 4; i--)
        {
            counter++;
            buf.append(freq.charAt(i));
            if(counter == 1 && i != 0)
            {
                buf.append(".");
            }
        }
        buf.reverse();

        return buf.toString();
    }

    public static String toShortCallsign(String callsign, String defaultPrefix)
    {
        return callsign.substring(defaultPrefix.length());

    }

    /**
     * Returns the current stack of the Thread
     *
     * @return
     */
    public static String getStack()
    {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();

        StringBuilder stack = new StringBuilder();

        // Do not show the last to functions from the stack (user does not care about them)
        for(int st = 2; st < stackTrace.length; st++)
        {
            stack.append(stackTrace[st].toString());
            stack.append("\n");
        }
        return stack.toString();
    }

    /**
     * Converts frequency [Hz] to band [m].E.g.7100000 => "40"
     *
     * @param freq
     * @return
     */
    public static String freqToBand(int freq)
    {
        if(freq > 1700000 && freq < 2500000)
        {
            return "160";
        }
        else if(freq > 2500000 && freq <= 5000000)
        {
            return "80";
        }
        else if(freq > 5000000 && freq <= 8400000)
        {
            return "40";
        }
        else if(freq > 8400000 && freq <= 12000000)
        {
            return "30";
        }
        else if(freq > 12000000 && freq <= 15000000)
        {
            return "20";
        }
        else if(freq > 15000000 && freq <= 19500000)
        {
            return "17";
        }
        else if(freq > 19500000 && freq <= 22000000)
        {
            return "15";
        }
        else if(freq > 22000000 && freq <= 25000000)
        {
            return "12";
        }
        else if(freq > 25000000 && freq <= 30000000)
        {
            return "10";
        }

        else
        {
            return "160";
        }
    }

    public static long getSecondsLeft(long elapsedTimeInSeconds, long repeatPeriod)
    {
        return repeatPeriod * 60 - elapsedTimeInSeconds;
    }

    public static String getFirstPart(String exchange)
    {
        return exchange.split(" ", 2)[0];
    }

    public static boolean isInteger(String s)
    {
        return isInteger(s, 10);
    }

    public static boolean isInteger(String s, int radix)
    {
        if(s.isEmpty())
        {
            return false;
        }
        for(int i = 0; i < s.length(); i++)
        {
            if(i == 0 && s.charAt(i) == '-')
            {
                if(s.length() == 1)
                {
                    return false;
                }
                else
                {
                    continue;
                }
            }
            if(Character.digit(s.charAt(i), radix) < 0)
            {
                return false;
            }
        }
        return true;
    }
}
