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

public enum RadioModes
{
  NONE,
  AM,       // AM -- Amplitude Modulation 
  CW,       // CW - CW "normal" sideband
  USB,      // USB - Upper Side Band
  LSB,      // LSB - Lower Side Band 
  RTTY,     // RTTY - Radio Teletype 
  FM,       // FM - "narrow" band FM 
  WFM,      // WFM - broadcast wide FM 
  CWR,      // CWR - CW "reverse" sideband
  RTTYR,    // RTTYR - RTTY "reverse" sideband
  AMS,      // AMS - Amplitude Modulation Synchronous 
  PKTLSB,   // PKTLSB - Packet/Digital LSB mode (dedicated port) 
  PKTUSB,   // PKTUSB - Packet/Digital USB mode (dedicated port) 
  PKTFM,    // PKTFM - Packet/Digital FM mode (dedicated port) 
  ECSSUSB,  // ECSSUSB - Exalted Carrier Single Sideband USB 
  ECSSLSB,  // ECSSLSB - Exalted Carrier Single Sideband LSB 
  FAX,      // FAX - Facsimile Mode
  SAM,      // SAM - Synchronous AM double sideband
  SAL,      // SAL - Synchronous AM lower sideband
  SAH,      // SAH - Synchronous AM upper (higher) sideband
  DSB       // DSB - Double sideband suppressed carrier
}
