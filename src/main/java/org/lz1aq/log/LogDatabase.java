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
package org.lz1aq.log;

import com.db4o.Db4oEmbedded;
import com.db4o.ObjectContainer;
import com.db4o.config.EmbeddedConfiguration;
import java.util.List;

/**
 *
 * @author chavdar
 */
public class LogDatabase
{
  private final ObjectContainer db;
  
  public LogDatabase(String dbFile)
  {
    EmbeddedConfiguration config = Db4oEmbedded.newConfiguration();
    config.common().objectClass(Qso.class).cascadeOnUpdate(true);
    db = Db4oEmbedded.openFile(config, dbFile);
  }
  
  
  List<Qso> getAll()
  {
    return db.query(Qso.class);
  }

  
  void add(Qso qso)
  {
    db.store(qso);
  }
  

  void remove(Qso qso)
  {
    db.delete(qso);
  }
  

  void modify(Qso qso)
  {
    db.store(qso);
  }
  
  void commit()
  {
    db.commit();
  }
  
  void close()
  {
    db.close();
  }
}
