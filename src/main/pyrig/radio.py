from org.lz1aq.py.rig import I_Radio
import logging


logger = logging.getLogger(__name__)


class Radio(I_Radio):



    @classmethod
    def getManufacturer(cls):
        """
        :return: The manufacturer of the rig - E.g. "Kenwood"
        :rtype: str
        """
        raise NotImplementedError("getManufacturer")


    @classmethod
    def getModel(cls):
        """
        :return: The model of the Rig - E.g. "IC-756PRO"
        :rtype: str
        """
        raise NotImplementedError("getModel")


    @classmethod
    def getAvailableModes(cls):
        """
        The function returns a string with all the modes that it supports.
        Example: "cw ssb lsb"

        :return: A string with the supported modes. Each mode is separated from the next with space.
        :rtype: str
        """
        raise NotImplementedError("getAvailableModes")


    @classmethod
    def getAvailableBands(cls):
        """
        The function returns a string with all the bands that it supports.
        Example: "3.5 7 14"

        :return: A string with the supported bands. Each band is separated from the next with space.
        :rtype: str
        """
        raise NotImplementedError("getAvailableBands")


    @classmethod
    def encodeSetFreq(cls, freq, vfo):
        """
        Gets the command(s) with which we can tell the radio to change frequency

        :param freq: Specifying the frequency. E.g. 7100000 for 7.1MHz
        :type freq: int
        :param vfo: The vfo for which we want to set the frequency
        :type vfo: int
        :return: list of EncodedTransactions - each containing a transactions with some additional control settings
        :rtype: list
        """
        raise NotImplementedError("encode_SetFreq")


    @classmethod
    def encodeGetFreq(cls, vfo):
        """
        Gets the command(s) with which we can tell the radio send us the current frequency

        :param vfo: For which VFO we want the mode
        :type vfo: int
        :return: list of EncodedTransactions - each containing a transactions with some additional control settings
        :rtype: list
        """
        raise NotImplementedError("encode_SetFreq")


    @classmethod
    def encodeSetMode(cls, mode, vfo):
        """
        Get the command that must be send to the radio in order to set mode (e.g. CW)

        :param mode: Specifies the mode - see Radio.modes
        :type mode: str
        :param vfo: The vfo which mode must be changed
        :type vfo: int
        :return: Object containing transaction with some additional control settings
        :rtype: EncodedTransaction
        """
        raise NotImplementedError("encode_SetFreq")


    @classmethod
    def encodeGetMode(cls, vfo):
        """
        Gets the command with which we can tell the radio to send us the current mode

        :param vfo: The VFO for which we want the current mode
        :type vfo: int
        :return: Object containing transaction with some additional control settings
        :rtype: EncodedTransaction
        """
        raise NotImplementedError("encode_SetFreq")


    @classmethod
    def encodeSendCW(cls, text):
        """
        Gets the command with which we can tell the radio to send morse code

        :param text: The VFO for which we want the current mode
        :type vfo: str
        :return: Object containing transaction with some additional control settings
        :rtype: EncodedTransaction
        """

        raise NotImplementedError("encodeSendCW")


    @classmethod
    def encodeSetKeyerSpeed(cls, keyerSpeed):
        """
        Gets the command(s) with which we can tell the radio to set the speed of the CW transmission.

        :param keyerSpeed: The desired speed that we would like to set
        :type keyerSpeed: int
        :return: Object containing transaction with some additional control settings
        :rtype: EncodedTransaction
        """
        raise NotImplementedError("encode_SetFreq")


    @classmethod
    def encodeInterruptSendCW(cls):
        """
        Gets the command with which we can tell the radio to stop sending morse code
        :return:
        """
        raise NotImplementedError("encodeSendCW")


    @classmethod
    def encodeGetActiveVfo(cls):
        raise NotImplementedError("encodeGetActiveVfo")

    #Freq - frequency of the target VFO
    #rig_set_mode(RIG *rig, vfo_t vfo, rmode_t mode, pbwidth_t width)
    #rig_set_vfo(RIG *rig, vfo_t vfo) - set the current VFO
    #rig_get_ant(RIG *rig, vfo_t vfo, ant_t *ant) - get the current antenna
    #rig_set_powerstat(RIG *rig, powerstat_t status) - turn on/off the radio
    #rig_reset(RIG *rig, reset_t reset) - reset the radio
    #rig_send_morse(RIG *rig, vfo_t vfo, const char *msg) - send morse code
    #rig_get_info(RIG *rig) -  get general information from the radio
    #set_power(watts) -


    # Possible radio modes
    MODES =['none',
            'am',        # AM -- Amplitude Modulation
            'cw',        # CW - CW "normal" sideband
            'usb',       # USB - Upper Side Band
            'lsb',       # LSB - Lower Side Band
            'rtty',      # RTTY - Radio Teletype
            'fm',        # FM - "narrow" band FM
            'wfm',      # WFM - broadcast wide FM
            'cwr',       # CWR - CW "reverse" sideband
            'rttyr',     # RTTYR - RTTY "reverse" sideband
            'ams',       # AMS - Amplitude Modulation Synchronous
            'pktlsb',    # PKTLSB - Packet/Digital LSB mode (dedicated port)
            'pktusb',    # PKTUSB - Packet/Digital USB mode (dedicated port)
            'pktfn',     # PKTFM - Packet/Digital FM mode (dedicated port)
            'ecssusb',   # ECSSUSB - Exalted Carrier Single Sideband USB
            'ecsslsb',   # ECSSLSB - Exalted Carrier Single Sideband LSB
            'fax',       # FAX - Facsimile Mode
            'sam',       # SAM - Synchronous AM double sideband
            'sal',       # SAL - Synchronous AM lower sideband
            'sah',       # SAH - Synchronous AM upper (higher) sideband
            'dsb',      # DSB - Double sideband suppressed carrier
            ]

    # Mapping of VFO letters to numbers
    VFO_NONE    = -1
    VFO_A       = 0
    VFO_B       = 1
    VFO_C       = 2
    VFO_D       = 3
    VFO_E       = 4
    VFO_F       = 5
    