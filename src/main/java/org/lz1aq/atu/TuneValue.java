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

import java.io.Serializable;


public class TuneValue implements Serializable
{
  private static final long serialVersionUID = 1L;
  
  private int c1;
  private int c2;
  private int l;

  public TuneValue()
  {
    this.c1 = 0;
    this.c2 = 0;
    this.l = 0;
  }
  
  public void set(int c1, int c2, int l)
  {
    this.c1 = c1;
    this.c2 = c2;
    this.l = l;
  }
  
  public int getC1()
  {
    return c1;
  }

  public void setC1(int c1)
  {
    this.c1 = c1;
  }

//  public int getC2()
//  {
//    return c2;
//  }

//  public void setC2(int c2)
//  {
//    this.c2 = c2;
//  }

  public int getL()
  {
    return l;
  }

  public void setL(int l)
  {
    this.l = l;
  }
  
  
  
}
