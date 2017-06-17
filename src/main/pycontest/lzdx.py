from org.lz1aq.py.contest import I_Contest
from org.lz1aq.py.contest import I_QsoTemplates
import logging


logger = logging.getLogger(__name__)



class LzDx(I_Contest):

  #+--------------------------------------------------------------------------+
  #|  Configuration fields - change if needed                                 |
  #+--------------------------------------------------------------------------+
  NAME  = "LZ-DX"
  MODES = "cw cwr lsb usb"
  BANDS = "3.5 7 14 21 28"

  QSO_TEMPLATES_LZ = {"default":
                        {"snt": ["RST", "exchange"],
                        "rcv": ["RST", "exchange"]
                        },
                     }

  QSO_TEMPLATES_NON_LZ = {"default":
                            {"snt": ["RST", "exchange"],
                            "rcv": ["RST", "exchange"]
                            },
                         }

  QSO_TEMPLATES_NON_LZ = {"default":
                            {"rcv": ["callsing_worked", "sntRST", "sntExchange", "rcvRST", "rcvExchange"]
                            },
                         }

  # Configuration parameters
  LZ_STATION =  {"lz_station": ["true", "false"]}
  ITU_ZONE =    {"itu_zone": ["number from 1 to 90"]}
  LZ_DISTRICT = {"lz_district": ["none", "BU","BL","VN","VT","VD","VR","GA","DO","KA","KD","LV","MN","PA","PK","PL","PD","RZ","RS","SS","SL","SM","SF","SO","SZ","TA","HA","SN","YA"]}
  CATEGORY  =   {"category": ["A1 - Single Operator/All Bands/Mixed/High Power",
                              "A2 - Single Operator/All Bands/Mixed/Low Power",
                              "B1 - Single Operator/All Bands/CW/High Power",
                              "B2 - Single Operator/All Bands/CW/Low Power",
                              "C1 - Single Operator/All Bands/SSB/High Power",
                              "C2 - Single Operator/All Bands/SSB/Low Power",
                              "D10 - Single Operator/Single Band/Mixed - 10M",
                              "D15 - Single Operator/Single Band/Mixed - 15M",
                              "D20 - Single Operator/Single Band/Mixed - 20M",
                              "D40 - Single Operator/Single Band/Mixed - 40M",
                              "D80 - Single Operator/Single Band/Mixed - 80M",
                              "E - Multi Operators/All Bands/Single Transmitter/Mixed",
                              "F - Single Operator/All Bands/Mixed/QRP (max.10W)",
                              "G - SWL"]}
  
  CONFIGURATION_TEMPLATE = {LZ_STATION,
                            ITU_ZONE,
                            LZ_DISTRICT,
                            CATEGORY}


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