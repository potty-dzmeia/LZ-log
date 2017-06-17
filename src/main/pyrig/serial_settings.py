import json
from org.lz1aq.py.rig import I_SerialSettings

class SerialSettings(I_SerialSettings):

    PARITY_NONE, PARITY_EVEN, PARITY_ODD, PARITY_MARK, PARITY_SPACE = 'None', 'Even', 'Odd', 'Mark', 'Space'
    STOPBITS_ONE, STOPBITS_ONE_POINT_FIVE, STOPBITS_TWO = (1, 1.5, 2)
    DATABITS_FIVE, DATABITS_SIX, DATABITS_SEVEN, DATABITS_EIGTH = (5, 6, 7, 8)
    HANDSHAKE_NONE, HANDSHAKE_XONXOFF, HANDSHAKE_CTSRTS = 'None', 'XonXoff', 'CtsRts'
    RTS_STATE_NONE, RTS_STATE_ON, RTS_STATE_OFF = 'None', 'On', 'Off'
    DTR_STATE_NONE, DTR_STATE_ON, DTR_STATE_OFF = 'None', 'On', 'Off'

    B_300, B_1200, B_2400, B_4800, B_9600, B_14400, B_19200, B_28800, B_38400, B_57600, B_115200, B_230400 = \
    300, 1200, 2400, 4800, 9600, 14400, 19200, 28800, 38400, 57600, 115200, 230400


    def __init__(self):
        # underscore added because of a jython issue
        self.baudrate_min_ = 2400
        self.baudrate_max_ = 19200
        self.data_bits_ = self.DATABITS_EIGTH
        self.stop_bits_ = self.STOPBITS_ONE
        self.parity_ = self.PARITY_NONE
        self.handshake_ = self.HANDSHAKE_NONE
        self.rts_ = self.RTS_STATE_OFF
        self.dtr_ = self.DTR_STATE_OFF

    def __str__(self):
        """Packs the class variables into a JSON formatted string
        :rtype : str
        """
        jsonBlock = dict()
        jsonBlock["baudrate_min"] = self.baudrate_min_
        jsonBlock["baudrate_max"] = self.baudrate_max_
        jsonBlock["data_bits"] = self.baudrate_min_
        jsonBlock["data_bits"] = self.data_bits_
        jsonBlock["stop_bits"] = self.stop_bits_
        jsonBlock["parity"] = self.parity_
        jsonBlock["handshake"] = self.handshake_
        jsonBlock["rts"] = self.rts_
        jsonBlock["dtr"] = self.dtr_
        return json.dumps(jsonBlock, indent=4)


    def toString(self):
        """Packs the class variables into a JSON formatted string
        :rtype : str
        """
        jsonBlock = dict()
        jsonBlock["baudrate_min"] = self.baudrate_min_
        jsonBlock["baudrate_max"] = self.baudrate_max_
        jsonBlock["data_bits"] = self.baudrate_min_
        jsonBlock["data_bits"] = self.data_bits_
        jsonBlock["stop_bits"] = self.stop_bits_
        jsonBlock["parity"] = self.parity
        jsonBlock["handshake"] = self.handshake
        jsonBlock["rts"] = self.rts_
        jsonBlock["dtr"] = self.dtr_
        return json.dumps(jsonBlock, indent=4)

    def getBauderateMin(self):
        """
        :rtype: int
        """
        return self.baudrate_min_


    def getBauderateMax(self):
        """
        :rtype: int
        """
        return self.baudrate_max_


    def getDataBits(self):
        """
        :rtype: int
        """
        return self.data_bits_

    def getStopBits(self):
        """
        :rtype: int
        """
        return self.stop_bits_


    def getParity(self):
        """
        :return: Possible values are PARITY_NONE, PARITY_EVEN, PARITY_ODD, PARITY_MARK, PARITY_SPACE
        :rtype: str
        """
        return self.parity_


    def getHandshake(self):
        """
        :return: Possible values are HANDSHAKE_NONE, HANDSHAKE_XONXOFF, HANDSHAKE_CTSRTS
        :rtype: str
        """
        return self.handshake_


    def getRts(self):
        """
        :return: Possible values are RTS_STATE_NONE, RTS_STATE_ON, RTS_STATE_OFF
        :rtype: str
        """
        return self.rts_


    def getDtr(self):
        """
        :return: Possible values are DTR_STATE_NONE, DTR_STATE_ON, DTR_STATE_OFF
        :rtype: str
        """
        return self.dtr_
