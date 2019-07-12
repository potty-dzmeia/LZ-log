from radio import *
from serial_settings import SerialSettings
from encoded_transaction import EncodedTransaction
from decoded_transaction import DecodedTransaction
import misc_utils
import logging
import logging.config

logging.config.fileConfig(misc_utils.get_logging_config(), disable_existing_loggers=False)
logger = logging.getLogger(__name__)


class Ft857(Radio):
    """
    Configuration file for Ft857 transceivers
    """

    #+--------------------------------------------------------------------------+
    #|  User configuration fields - change if needed                            |
    #+--------------------------------------------------------------------------+
    MANUFACTURER = "Yaesu"
    MODEL_NAME   = "FT-857"

    # Get default serial port settings
    serial_settings = SerialSettings() # If different values than the default ones are need - uncomment and set to desired value
    serial_settings.stop_bits_      = SerialSettings.STOPBITS_TWO
    serial_settings.rts_            = SerialSettings.RTS_STATE_OFF
    serial_settings.dtr_            = SerialSettings.DTR_STATE_OFF
    # serial_settings.data_bits_    = SerialSettings.DATABITS_EIGHT
    # serial_settings.handshake_    = SerialSettings.HANDSHAKE_CTSRTS
    # serial_settings.parity_       = SerialSettings.PARITY_NONE









    #+--------------------------------------------------------------------------+
    #|   End of user configuration fields                                       |
    #+--------------------------------------------------------------------------+

    @classmethod
    def getManufacturer(cls):
        """
        :return: The manufacturer of the rig - E.g. "Kenwood"
        :rtype: str
        """
        return cls.MANUFACTURER


    @classmethod
    def getModel(cls):
        """
        :return: The model of the Rig - E.g. "IC-756PRO"
        :rtype: str
        """
        return cls.MODEL_NAME


    @classmethod
    def getSerialPortSettings(cls):
        """
        Returns the serial settings to be used when connecting to this rig

        :return: [SerialSettings] object holding the serial port settings
        :rtype: SerialSettings
        """
        return cls.serial_settings


    @classmethod
    def getAvailableModes(cls):
        """
        The function returns a string with all the modes that the radio supports.
        Example: "cw ssb lsb"

        :return: A string with the supported modes. Each mode is separated from the next with space.
        :rtype: str
        """
        return " ".join("%s" % key for key in cls.mode_codes)


    # @classmethod
    # def getAvailableBands(cls):
    #     """
    #     The function returns a string with all the bands that it supports.
    #     Example: "3.5 7 14"
    #
    #     :return: A string with the supported bands. Each band is separated from the next with space.
    #     :rtype: str
    #     """
    #     return " ".join("%s" % key for key in cls.mode_codes)


    #+--------------------------------------------------------------------------+
    #|  Encode methods below                                                    |
    #+--------------------------------------------------------------------------+


    @classmethod
    def encodeInit(cls):
        """
        If the radio needs some initialization before being able to be used.

        :return: Initialization command that is to be send to the Rig
        :rtype: EncodedTransaction
        """
        return list()


    @classmethod
    def encodeCleanup(cls):
        """
        If the radio needs some cleanup after being used.

        :return: Cleanup command that is to be send to the Rig
        :rtype: EncodedTransaction
        """
        return list()


    @classmethod
    def encodeSetFreq(cls, freq, vfo):
        """
        Gets the command with which we can tell the radio to change frequency

        :param freq: Specifying the frequency. E.g. 7100000 for 7.1MHz
        :type freq: int
        :param vfo: The vfo for which we want to set the frequency (not supported/used by ft857)
        :type vfo: int
        :return: Object containing transaction with some additional control settings
        :rtype: EncodedTransaction
        """

        # Remove the least significant digit (e.g. 7,150,251 -> 7,150,25) as it is not handled by the ft857
        freq /= 10

        # To set Freq we have to send to the radio the following bytes: [P1, P2, P3, P4, 01]
        # E.g. [01, 42, 34, 56, 01] = 14.23456 MHz
        command = misc_utils.toBcd(freq, 8)
        command.append(0x01)  # command 01

        tr1 = EncodedTransaction(bytearray(command).__str__(), post_write_delay=50)
        #logger.debug("returns: {0}".format(tr1))
        return [tr1]


    @classmethod
    def encodeGetFreq(cls, vfo):
        """
        Gets the command with which we can tell the radio to send us the current frequency

        :param vfo: For which VFO we want the mode (not supported by ft857)
        :type vfo: int
        :return: Object containing transaction with some additional control settings
        :rtype: EncodedTransaction
        """

        # To get Freq and Mode we have to send to the radio the following bytes: [xx, xx, xx, xx, 01]
        # E.g. [xx, xx, xx, xx, 03] - bytes marked with "xx" are not relevant
        command = [0xFF, 0xFF, 0xFF, 0xFF, 0x03]
        tr1 = EncodedTransaction(bytearray(command).__str__(), post_write_delay=50)
        return [tr1]


    @classmethod
    def encodeGetMode(cls, vfo):
        """
        Gets the command with which we can tell the radio to send us the Mode for the certain VFO
        :param vfo: For which VFO we want to get the mode (not supported by ft857)
        :type vfo: int
        :return:
        :rtype: EncodedTransaction
        """

        # To get Freq and Mode we have to send to the radio the following bytes: [xx, xx, xx, xx, 01]
        # E.g. [xx, xx, xx, xx, 03] - bytes marked with "xx" are not relevant

        command = [0xFF, 0xFF, 0xFF, 0xFF, 0x03]
        tr1 = EncodedTransaction(bytearray(command).__str__(), post_write_delay=50)
        return [tr1]


    @classmethod
    def encodeSetMode(cls, mode, vfo):
        """
        Get the command that must be send to the radio in order to set mode (e.g. CW)
        VFO param is not taken into account - we always set the currently active VFO

        :param mode: Specifies the mode - see Radio.MODES for expected values
        :type mode: str
        :param vfo: The vfo which mode must be changed (not used/supported by ft857)
        :type vfo: int
        :return: Object containing transaction with some additional control settings
        :rtype: EncodedTransaction
        """

        new_mode = mode.lower()
        if not cls.mode_codes.__contains__(new_mode):
            raise ValueError("Unsupported mode: " + mode + "!")

        # To set Mode we have to send to the radio the following bytes: [P1, xx, xx, xx, 01]
        # E.g. [P1, xx, xx, xx, 03] - P1 is defined in mode_code, bytes marked with "xx" are not relevant

        p1 = cls.mode_codes[new_mode]
        command = [p1, 0xFF, 0xFF, 0xFF, 0x07]
        tr1 = EncodedTransaction(bytearray(command).__str__(), post_write_delay=50)
        return [tr1]


    @classmethod
    def encodeSendCW(cls, text):
        """
        Gets the command with which we can tell the radio to send morse code

        :param text: The text the we would likt to send as morse
        :type text: str
        :return: Object containing transaction with some additional control settings
        :rtype: EncodedTransaction
        """
        logger.info("not supported")
        return list()


    @classmethod
    def encodeSetKeyerSpeed(cls, keyerSpeed):
        """
        Gets the command(s) with which we can tell the radio to set the speed of the CW transmission.

        :param keyerSpeed: The desired speed that we would like to set
        :type keyerSpeed: int
        :return: Object containing transaction with some additional control settings
        :rtype: EncodedTransaction
        """
        logger.info("not supported")
        return list()


    @classmethod
    def encodeInterruptSendCW(cls):
        """
        Gets the command with which we can tell the radio to stop sending morse code
        :return:
        """
        logger.info("not supported")
        return list()


    @classmethod
    def encodeGetActiveVfo(cls):
        """
        Gets the command with which we can tell the radio to send us the active VFO
        :return:
        """
        logger.info("not supported")
        return list()


    @classmethod
    def encodePoll(cls):
        """
        Gets the command with which we can tell the radio to send us status information (in this case freq and mode)
        :return:
        """
        return cls.encodeGetMode(0)


    @classmethod
    def encodeDisableAutomaticInfo(cls):
        """
        Gets the command(s) with which we can tell the radio not to send any information automatically
        :return:
        """
        logger.info("not supported")
        return list()


    @classmethod
    def encodeEnableAutomaticInfo(cls):
        """
        Gets the command(s) with which we can tell the radio to send back information automatically when something changes
        :return:
        """
        logger.info("not supported")
        return list()

    #+--------------------------------------------------------------------------+
    #|  Decode methods below                                                    |
    #+--------------------------------------------------------------------------+

    @classmethod
    def decode(cls, data):
        """
        Extracts and decodes the first ft857 command found within the supplied buffer.

        :param data: Series of bytes from which we must extract the incoming command. There is no guarantee
        that the first byte is the beginning of the transaction (i.e. there might be some trash in the beginning).
        We expect 5bytes: b1(freq), b2(freq), b3(freq), b4(freq), b5(mode)
        :type data: array
        :return: Object containing the transaction and some additional control information
        :rtype: DecodedTransaction
        """

        trans = bytearray(data)

        #  Discard all bytes if we don't get exactly 5
        if trans.__len__() != 5:
            logger.info("Received different than 5 bytes.")
            logger.info("Len = "+str(trans.__len__()))
            logger.info("Content = " + str(bytearray(trans)))

            result_dic = dict()
            DecodedTransaction.insertNotSupported(result_dic, "Unknown character found in the data coming from the radio.")
            result_json = DecodedTransaction.toJson(result_dic)
            return DecodedTransaction(result_json, trans.__len__())

        result_dic = dict()

        # Mode
        mode = cls.__mode_from_byte_to_string(trans[4])
        logger.info("mode: "+mode)
        DecodedTransaction.insertMode(result_dic, mode)
        # Frequency
        freq = misc_utils.fromBcd(trans[0:4],"big")
        freq *= 10  # ft857 does not send the least significant digit for the HZ (e.g. 7,150,45)
        logger.info("freq: " + str(freq))
        DecodedTransaction.insertFreq(result_dic, str(freq))

        try:
            result_json = DecodedTransaction.toJson(result_dic)
        except:
            # something went wrong during parsing. Return not supported...
            result_dic = dict()
            DecodedTransaction.insertNotSupported(result_dic, "Unknown character found in the data coming from the radio.")
            result_json = DecodedTransaction.toJson(result_dic)
            return DecodedTransaction(result_json, trans.__len__())

        # return the object with the decoded transaction and the amount of bytes that we have read from the supplied buffer(string)
        return DecodedTransaction(result_json, trans.__len__())


    #+--------------------------------------------------------------------------+
    #|   Private methods                                                        |
    #+--------------------------------------------------------------------------+

    @classmethod
    def __mode_from_byte_to_string(cls, mode):
        """
        Returns a string describing the current working mode
        :param mode: Integer describing the mode see cls.mode_codes
        :type mode: int
        :return: String describing the working mode cls.mode_codes
        :rtype: str
        """

        # Convert the "mode" to valid string
        for key, value in cls.mode_codes.items():
            if mode == value:
                return key

        # In case of unknown mode integer
        return "none"



    #+--------------------------------------------------------------------------+
    #|   ft857 codes
    #+--------------------------------------------------------------------------+

    # Codes used for changing the mode
    mode_codes ={'lsb':     0x00,
                 'usb':     0x01,
                 'cw':      0x02,
                 'fm':      0x08,
                 'am':      0x04,
                 'rtty':    0x0A,
                 'cwr':     0x03}
