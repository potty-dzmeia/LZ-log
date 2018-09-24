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
package org.lz1aq.atu;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;


/**
 *
 * @author levkov_cha_ext
 */
public class TuneSettings
{
  TuneValue[][][][] tuneValues;

  
  public TuneSettings(int bandCount, int antCount, int modeCount, int tuneValueCount)
  { 
    if(loadFromFile())
    {
      return;
    }

    // Init to default tune values
    tuneValues = new TuneValue[bandCount][antCount][modeCount][tuneValueCount];
    for (int band = 0; band < bandCount; band++)
      for (int ant = 0; ant < antCount; ant++)
        for (int mode = 0; mode < modeCount; mode++)
          for (int tune = 0; tune < tuneValueCount; tune++)
            tuneValues[band][ant][mode][tune] = new TuneValue();
      
  }
  
  TuneValue get(int band, int ant, int mode, int tune)
  {
    return tuneValues[band][ant][mode][tune];
  }
  
  
  void set(int band, int ant, int mode, int tune, TuneValue tuneValue)
  {
    tuneValues[band][ant][mode][tune] = tuneValue;
  }
  
  void save()
  {
    try
    {
      FileOutputStream fileOut = new FileOutputStream("tuneSettings.ser");
      ObjectOutputStream out = new ObjectOutputStream(fileOut);
      out.writeObject(tuneValues);
      out.close();
      fileOut.close();
    }
    catch (IOException i)
    {
      i.printStackTrace();
    }

  }
  
  private Boolean loadFromFile()
  {
    try
    {
      FileInputStream fileIn = new FileInputStream("tuneSettings.ser");
      ObjectInputStream in = new ObjectInputStream(fileIn);
      tuneValues = (TuneValue[][][][]) in.readObject();
      in.close();
      fileIn.close();
    }
    catch (IOException i)
    {
      i.printStackTrace();
      return false;
    }
    catch (ClassNotFoundException c)
    {
      c.printStackTrace();
      return false;
    }
    
    return true;
  }
}
