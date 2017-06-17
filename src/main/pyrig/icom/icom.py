import radio
from serial_settings import SerialSettings
from encoded_transaction import EncodedTransaction
from decoded_transaction import DecodedTransaction
import misc_utils
import logging
import logging.config


logging.config.fileConfig(misc_utils.get_logging_config(), disable_existing_loggers=False)
logger = logging.getLogger(__name__)


class Icom(radio.Radio):
    """
    Configuration file for Icom transceivers
    """

    #+--------------------------------------------------------------------------+
    #|  User configuration fields - change if needed                            |
    #+--------------------------------------------------------------------------+
    MANUFACTURER = "Icom"
    MODEL_NAME = "None"

    # Get default serial port settings
    serial_settings = SerialSettings() # If different values than the default ones are need - uncomment and set to desired value
    serial_settings.baudrate_max_   = 9600
    serial_settings.rts_            = SerialSettings.RTS_STATE_ON  # This is used to power the electronics
    # serial_settings.baudrate_min_ = 2400
    # serial_settings.data_bits_    = SerialSettings.DATABITS_EIGTH
    # serial_settings.stop_bits_    = SerialSettings.STOPBITS_ONE
    # serial_settings.handshake_    = SerialSettings.HANDSHAKE_CTSRTS
    # serial_settings.parity_       = SerialSettings.PARITY_NONE
    #serial_settings.dtr_           = SerialSettings.DTR_STATE_NONE


    CIV_ADDRESS  = 0x5c # The address of the Icom transceiver. Value of 0x5c is good for 756Pro
    CTRL_ADDRESS = 0xE0 # Controller's address (default is 0xE0).














    #+--------------------------------------------------------------------------+
    #|   End of user configuration fields                                       |
    #+--------------------------------------------------------------------------+


    @classmethod
    def getManufacturer(cls):
        """
        :return: The manufacturer of the rig - E.g. "Kenwood"
        :rtype: str
        """
        logger.debug("returns: " + cls.MANUFACTURER)
        return cls.MANUFACTURER


    @classmethod
    def getModel(cls):
        """
        :return: The model of the Rig - E.g. "IC-756PRO"
        :rtype: str
        """
        logger.debug("returns: {0}".format(cls.MODEL_NAME))
        return cls.MODEL_NAME


    @classmethod
    def getSerialPortSettings(cls):
        """
        Returns the serial settings to be used when connecting to this rig

        :return: [SerialSettings] object holding the serial port settings
        :rtype: SerialSettings
        """
        logger.debug("returns: {0}".format(cls.serial_settings))
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
        Gets the command with which we can tell an Icom radio to change frequency

        :param freq: Frequency in Hz. E.g. 7100000 for 7.1MHz
        :type freq: int
        :param vfo: The vfo for which we want to set the frequency
        :type vfo: int
        :return: Object containing transaction with some additional control settings
        :rtype: EncodedTransaction
        """

        temp = cls.__transaction(0x07)                              # Select VFO mode
        tr1 = EncodedTransaction(bytearray(temp).__str__(), is_cfm_expected=True)
        temp = cls.__transaction(0x07, 0xD0 + vfo)                  # Select the desired VFO
        tr2 = EncodedTransaction(bytearray(temp).__str__(), is_cfm_expected=True)
        temp = cls.__transaction(0x05, data=misc_utils.toBcd(freq, 10))  # Select the frequency
        tr3 = EncodedTransaction(bytearray(temp).__str__(), is_cfm_expected=True)
        return [tr1, tr2, tr3]


    @classmethod
    def encodeGetFreq(cls, vfo):
        """
        Gets the command with which we can tell the radio send us the current frequency

        :param vfo: For which VFO we want the mode
        :type vfo: int
        :return: Object containing transaction with some additional control settings
        :rtype: EncodedTransaction
        """

        temp = cls.__transaction(0x07)                              # Select VFO mode
        tr1 = EncodedTransaction(bytearray(temp).__str__(), is_cfm_expected=True)
        temp = cls.__transaction(0x07, 0xD0 + vfo)                  # Select the desired VFO
        tr2 = EncodedTransaction(bytearray(temp).__str__(), is_cfm_expected=True)
        temp = cls.__transaction(0x03)                              # Read the frequency
        tr3 = EncodedTransaction(bytearray(temp).__str__(), post_write_delay=100) # Add delay to give the radio time to send the Freq

        return [tr1, tr2, tr3]



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
        new_mode = mode.lower()
        if not cls.mode_codes.__contains__(new_mode):
            raise ValueError("Unsupported mode: "+mode+"!")

        temp = cls.__transaction(0x07)                              # Select VFO mode
        tr1 = EncodedTransaction(bytearray(temp).__str__(), is_cfm_expected=True)
        temp = cls.__transaction(0x07, 0xD0 + vfo)                  # Select the desired VFO
        tr2 = EncodedTransaction(bytearray(temp).__str__(), is_cfm_expected=True)
        temp = cls.__transaction(0x06, sub_command=cls.mode_codes[new_mode])   # Set the Mode
        tr3 = EncodedTransaction(bytearray(temp).__str__(), is_cfm_expected=True)

        return [tr1, tr2, tr3]


    @classmethod
    def encodeGetMode(cls, vfo):
        """
        Gets the command with which we can tell the radio to send us the current mode

        :param vfo: The VFO for which we want the current mode
        :type vfo: int
        :return: Object containing transaction with some additional control settings
        :rtype: EncodedTransaction
        """
        temp = cls.__transaction(0x07)                         # Select VFO mode
        tr1 = EncodedTransaction(bytearray(temp).__str__(), is_cfm_expected=True)
        temp = cls.__transaction(0x07, 0xD0 + vfo)             # Select the desired VFO
        tr2 = EncodedTransaction(bytearray(temp).__str__(), is_cfm_expected=True)
        temp = cls.__transaction(0x04)                         # Get the Mode
        tr3 = EncodedTransaction(bytearray(temp).__str__(), post_write_delay=100) # Add a delay to give the radio time to send the Mode back

        return [tr1, tr2, tr3]



    #+--------------------------------------------------------------------------+
    #|  Decode methods below                                                    |
    #+--------------------------------------------------------------------------+


    @classmethod
    def decode(cls, data):
        """
        Extracts and decodes the first Icom command found within the supplied buffer.

        :param data: Series of bytes from which we must extract the incoming command. There is no guarantee
        that the first byte is the beginning of the transaction (i.e. there might be some trash in the beginning).
        :type data: array
        :return: Object containing the transaction and some additional control information
        :rtype: DecodedTransaction
        """

        trans = bytearray(data)

        # Find the beginning of the transaction
        trans_start_index = trans.find(cls.TRANS_START)
        if trans_start_index == -1:
            return DecodedTransaction(None, 0)

        # Find the end of the transaction (must be after trans_start_index)
        trans_end_index = trans.find(cls.TRANS_END, trans_start_index)
        if trans_end_index == -1:
            return DecodedTransaction(None, 0)

        cmd_idx = trans_start_index + 4  # get the index of the command

        result_dic = dict()

        if trans[cmd_idx] == cls.CFM_POSITIVE:      # <------------------------- positive confirm
            DecodedTransaction.insertPositiveCfm(result_dic)

        elif trans[cmd_idx] == cls.CFM_NEGATIVE:    # <------------------------- negative confirm
            DecodedTransaction.insertNegativeCfm(result_dic)

        elif trans[cmd_idx] == cls.SEND_FREQ:       # <------------------------- frequency
            freq = cls.__frequency_from_bcd_to_string(trans[(cmd_idx + 1):trans_end_index])
            DecodedTransaction.insertFreq(result_dic, freq)

        elif trans[cmd_idx] == cls.SEND_MODE:       # <------------------------- mode
            mode = cls.__mode_from_byte_to_string(trans[cmd_idx+1])
            DecodedTransaction.insertMode(result_dic, mode)

        else:                                       # <------------------------- not-supported
            DecodedTransaction.insertNotSupported(result_dic, misc_utils.getListInHex(trans[trans_start_index:trans_end_index+1]))

        # Convert to JSON string
        result_json = DecodedTransaction.toJson(result_dic)

        logger.debug("input bytes: {0}".format(misc_utils.getListInHex(bytearray(data))))
        logger.debug("returns: {0}; bytes removed: {1}".format(result_json, trans_end_index+1))

        # return the object with the decoded transaction and the amount of bytes that we have read from the supplied buffer(string)
        return DecodedTransaction(result_json, trans_end_index+1)


    #+--------------------------------------------------------------------------+
    #|   Private methods                                                        |
    #+--------------------------------------------------------------------------+


    @classmethod
    def __transaction(cls, command, sub_command=None, data=None):
        """
        Assembles an Icom specific transaction ready to be send to the transceiver

        Protocol is the following:
        [preamble(0xFE), preamble(0xFE), ctrl_id, civ-address, command, sub_command, data...., 0xFD]

        :param command: Command code. E.g. 0x05 is set frequency)
        :type command: int
        :param sub_command: Sub-command code (optional)
        :type sub_command: int
        :param data: Additional data bytes(optional)
        :type data: list
        :return: The ready transaction bytes
        :rtype: list
        """
        transaction= [0xFE, 0xFE, cls.CIV_ADDRESS, cls.CTRL_ADDRESS, command]
        if sub_command is not None:
            transaction.append(sub_command)
        if data is not None:
            transaction += data
        transaction.append(0xFD)

        logger.debug("returns: {0}".format(misc_utils.getListInHex(transaction)))
        return transaction


    @classmethod
    def __mode_from_byte_to_string(cls, mode):
        """
        Returns a string describing the current working mode
        :param mode: Byte describing the mode see cls.mode_codes
        :type mode: int
        :return: String describing the working mode (e.g. "CW"). "none" if we couldn't recognize the mode.
        :rtype: str
        """

        # Convert the "mode" to valid string
        for key, value in cls.mode_codes.items():
            if mode == value:
                return key

        # In case of unknown mode integer
        return "none"



    @classmethod
    def __frequency_from_bcd_to_string(cls, frequency):
        """
        Converts the byte array with the frequency to a string representation
        Example: 0x00, 0x02, 0x10, 0x14, 0x00 is converted to "14,100,200"
        :param frequency: Bytearray containing the frequency in a little endian format bcd format
        :type frequency: bytearray
        :return: Frequency in string format
        :rtype: str
        """
        return misc_utils.fromBcd(frequency).__str__()





    #+--------------------------------------------------------------------------+
    #|   Icom command codes
    #+--------------------------------------------------------------------------+

    TRANS_START = bytearray([0xFE, 0xFE, CTRL_ADDRESS, CIV_ADDRESS])  # Trans send by the Icom starts with: 0xFE 0xFE CTRL CIV
    TRANS_END = bytearray([0xFD])  # Transactions ends with: 0xFD

    SEND_FREQ = 0x00        # send by the Icom when frequency has changed
    SEND_MODE = 0x01        # send by the Icom whe mode has changed
    CFM_POSITIVE = 0xFB     # Positive confirmation send by the Icom radio
    CFM_NEGATIVE = 0xFA     # Negative confirmation send by the Icom radio


    # Codes used for changing the mode
    mode_codes ={'lsb':     0x00,
                 'usb':     0x01,
                 'am':      0x02,
                 'cw':      0x03,
                 'rtty':    0x04,
                 'fm':      0x05,
                 'cwr':     0x07,
                 'rttyr':   0x08}


