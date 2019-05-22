// ***************************************************************************
package org.lz1aq.keyer;

import javax.swing.DefaultComboBoxModel;

/**
 *
 * @author potty
 */
public enum KeyerTypes
{
  WINKEYER(0), DTR(1), RTS(2), ;
  private final int code;

  KeyerTypes(int code)
  {
    this.code = code;
  }

  public int toInt()
  {
    return code;
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
      if(e.code == value)
      {
        return e.toString();
      }
    }
    return null;// not found
  }

}
