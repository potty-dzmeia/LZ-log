from org.lz1aq.py.contest import I_Contest
from org.lz1aq.py.contest import I_QsoTemplates
import logging


logger = logging.getLogger(__name__)



class CqWwDx_Cw(I_Contest):

  #+--------------------------------------------------------------------------+
  #|  Configuration fields - change if needed                                 |
  #+--------------------------------------------------------------------------+
  NAME  = "CQ-WW-CW"
  MODES = "cw cwr"
  BANDS = "1.8 3.5 7 14 21 28"

  QSO_TEMPLATES = {"default":
                     {"snt": ["RST", "CQ-Zone"],
                      "rcv": ["RST", "CQ-Zone"]}
                  }

  CONFIGURATION_PARAMS = {"CQ-Zone": ["1","2"]}


  #+--------------------------------------------------------------------------+
  #|   End of user configuration fields                                       |
  #+--------------------------------------------------------------------------+



  @classmethod
  def getQsoTemplates(cls):
    return cls.NAME

  @classmethod
  def getName(cls):
    return cls.NAME


  @classmethod
  def getAvailableModes(cls):
    """
    The function returns a string with all the modes that the contest supports
    Example: "usb lsb"
    :return: A string with the supported modes. Each mode is separated from the next with space.
    :rtype: str
    """
    return cls.MODES

  @classmethod
  def getAvailableBands(cls):
    """
    The function returns a string with all the bands that the contest supports.
    Example: "3.5 7 14"
    :return: A string with the supported bands in MHz. Each band is separated from the next one with space.
    :rtype: str
    """
    raise cls.BANDS