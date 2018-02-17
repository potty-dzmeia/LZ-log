// ***************************************************************************
package org.lz1aq.keyer;

import javax.swing.DefaultComboBoxModel;

/**
 *
 * @author potty
 */
public enum KeyerTypes
{
  WINKEYER(0), DTR(1), RTS(2);
  private final int value;

  KeyerTypes(int value)
  {
    this.value = value;
  }

  public int getValue()
  {
    return value;
  }
  
  static public DefaultComboBoxModel getComboxModel()
  {
    return new DefaultComboBoxModel(new String[]
    {
      WINKEYER.toString(), DTR.toString(), RTS.toString()
    });
  }
  
   static public String getName(int value)
  {
    for(KeyerTypes e : KeyerTypes.values())
    {
      if(e.value == value)
      {
        return e.toString();
      }
    }
    return null;// not found
  }
  
}
