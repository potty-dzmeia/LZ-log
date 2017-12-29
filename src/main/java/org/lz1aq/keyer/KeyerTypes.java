// ***************************************************************************
package org.lz1aq.keyer;

/**
 *
 * @author potty
 */
public enum KeyerTypes
{
  WINKEYER(0), DTR(1), RTS(2);
  private final int code;

  KeyerTypes(int code)
  {
    this.code = code;
  }

  public int toInt()
  {
    return code;
  }
  
}
