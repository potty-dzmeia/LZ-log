from radio import *
from serial_settings import SerialSettings
from encoded_transaction import EncodedTransaction
from decoded_transaction import DecodedTransaction
import misc_utils
import logging
import logging.config


logging.config.fileConfig(misc_utils.get_logging_config(), disable_existing_loggers=False)
logger = logging.getLogger(__name__)


class Kenwood(Radio):
    """
    Configuration file for Kenwood transceivers
    """

    #+--------------------------------------------------------------------------+
    #|  User configuration fields - change if needed                            |
    #+--------------------------------------------------------------------------+
    MANUFACTURER = "Kenwood"
    MODEL_NAME   = "All models"

    # Get default serial port settings
    serial_settings = SerialSettings() # If different values than the default ones are need - uncomment and set to desired value
    serial_settings.stop_bits_      = SerialSettings.STOPBITS_TWO
    serial_settings.data_bits_      = SerialSettings.DATABITS_EIGHT
    serial_settings.parity_       	= SerialSettings.PARITY_NONE	
    serial_settings.handshake_    	= SerialSettings.HANDSHAKE_NONE	
    #serial_settings.rts_           = SerialSettings.RTS_STATE_OFF   	# not used anymore. It is set from the Settings menu 
    #serial_settings.dtr_           = SerialSettings.DTR_STATE_OFF		# not used anymore. It is set from the Settings menu 
    
    			


    # The AI meta-command can be used to enable automatic responses from the Kenwood to a computer in response to Kenwood front panel control changes by the operator.
    AUTO_INFO_MODE = "AI2;" # Possible values are: "AI0;" "AI2;"












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
        return list([EncodedTransaction(cls.AUTO_INFO_MODE)])


    @classmethod
    def encodeCleanup(cls):
        """
        If the radio needs some cleanup after being used.

        :return: Cleanup command that is to be send to the Rig
        :rtype: EncodedTransaction
        """
        logger.info("encodeCleanup() not implemented")
        return list()


    @classmethod
    def encodeSetFreq(cls, freq, vfo):
        """
        Gets the command with which we can tell the radio to change frequency

        :param freq: Specifying the frequency. E.g. 7100000 for 7.1MHz
        :type freq: int
        :param vfo: The vfo for which we want to set the frequency
        :type vfo: int
        :return: Object containing transaction with some additional control settings
        :rtype: EncodedTransaction
        """
        result = "F%c%011ld;"%(cls.__vfo_number_to_letter(vfo), freq)
        logger.debug("returns: {0}".format(result))
        return list([EncodedTransaction(result)])


    @classmethod
    def encodeGetFreq(cls, vfo):
        """
        Gets the command with which we can tell the radio to send us the current frequency

        :param vfo: For which VFO we want the mode
        :type vfo: int
        :return: Object containing transaction with some additional control settings
        :rtype: EncodedTransaction
        """
        result = "F%c;"%(cls.__vfo_number_to_letter(vfo))
        logger.debug("returns: {0}".format(result))
        return list([EncodedTransaction(result)])


    @classmethod
    def encodeGetMode(cls, vfo):
        """
        Gets the command with which we can tell the radio to send us the Mode for the certain VFO
        :param vfo: For which VFO we want to get the mode (Kenwood does not support this)
        :type vfo: int
        :return:
        :rtype: EncodedTransaction
        """
        result = "MD;"
        logger.debug("returns: {0}".format(result))
        return list([EncodedTransaction(result)])


    @classmethod
    def encodeSetMode(cls, mode, vfo):
        """
        Get the command that must be send to the radio in order to set mode (e.g. CW)
        VFO param is not taken into account - we always set the currently active VFO

        :param mode: Specifies the mode - see Radio.MODES for expected values
        :type mode: str
        :param vfo: The vfo which mode must be changed - NOT SUPPORTED by Kenwood
        :type vfo: int
        :return: Object containing transaction with some additional control settings
        :rtype: EncodedTransaction
        """
        mode = mode.lower()
        if not cls.mode_codes.__contains__(mode):
            raise ValueError("Unsupported mode: " + mode + " !")

        #if vfo == Radio.VFO_A:
        result = "MD%d;"%(cls.mode_codes[mode])
        # elif vfo == Radio.VFO_B:
        #     result = "MD$%d;"%(cls.mode_codes[mode])
        # else:
        #     logger.warning("encodeSetMode(): Set VFO_NONE is not supported")

        logger.debug("returns: {0}".format(result))
        return list([EncodedTransaction(result)])


    @classmethod
    def encodeSendCW(cls, text):
        """
        Gets the command with which we can tell the radio to send morse code

        :param text: The text the we would likt to send as morse
        :type text: str
        :return: Object containing transaction with some additional control settings
        :rtype: EncodedTransaction
        """
        if len(text) > 24:
            logger.warning("Text for CW is too long.")

        result = "KY {0};".format('{: <24}'.format(text))

        #logger.debug("returns: {0}".format(result))
        return list([EncodedTransaction(result)])


    @classmethod
    def encodeSetKeyerSpeed(cls, keyerSpeed):
        """
        Gets the command(s) with which we can tell the radio to set the speed of the CW transmission.

        :param keyerSpeed: The desired speed that we would like to set
        :type keyerSpeed: int
        :return: Object containing transaction with some additional control settings
        :rtype: EncodedTransaction
        """
        result = "KS{0:03d};".format(keyerSpeed)

        logger.debug("returns: {0}".format(result))
        return list([EncodedTransaction(result)])


    @classmethod
    def encodeInterruptSendCW(cls):
        """
        Gets the command with which we can tell the radio to stop sending morse code
        :return:
        """
        result = "KY0;"
        return list([EncodedTransaction(result)])


    @classmethod
    def encodeGetActiveVfo(cls):
        """
        Gets the command with which we can tell the radio to send us the active VFO
        :return:
        """
        result = "FR;"
        return list([EncodedTransaction(result)])


    @classmethod
    def encodePoll(cls):
        """
        Gets the command with which we can tell the radio to send us status information (e.g. freq, mode, vfo etc.)
        :return:
        """
        result = "IF;"
        logger.debug("returns: {0}".format(result))
        return list([EncodedTransaction(result)])


    @classmethod
    def encodeDisableAutomaticInfo(cls):
        """
        Gets the command(s) with which we can tell the radio not to send any information automatically
        :return:
        """
        result = "AI0;"
        logger.debug("returns: {0}".format(result))
        return list([EncodedTransaction(result)])


    @classmethod
    def encodeEnableAutomaticInfo(cls):
        """
        Gets the command(s) with which we can tell the radio to send back information automatically when something changes
        :return:
        """
        result = "AI2;"
        logger.debug("returns: {0}".format(result))
        return list([EncodedTransaction(result)])

    #+--------------------------------------------------------------------------+
    #|  Decode methods below                                                    |
    #+--------------------------------------------------------------------------+


    @classmethod
    def decode(cls, data):
        """
        Extracts and decodes the first Elecraft command found within the supplied buffer.

        Example of an Kenwood command: "MD$1;" - which means that vfo 2 is in LSB mode

        :param data: Series of bytes from which we must extract the incoming command.
        :type data: array
        :return: Object containing the transaction and some additional control information
        :rtype: DecodedTransaction
        """

        # Find the character ";" which signals the end of the command
        data = data.tostring()
        end = data.find(';')

        # The incoming data does not contain one complete transaction...
        if end == -1:
            return DecodedTransaction(None, 0)

        json_result = cls.__parse(data[:end+1])

        # return the object with the decoded transaction and the amount of bytes that we have read from the supplied buffer(string)
        return DecodedTransaction(json_result, end+1)


    @classmethod
    def __parse(cls, trans):
        """
        Parses the string data into a meaningful JSON block containing the command coming from the radio

        This function actually calls the responsible parser depending on the incoming command code

        :param data: A single transaction string coming from the radio that we have to parse to a meaningful JSON block
        :type data: str
        :return: JSON formatted block containing the parsed data
        :rtype: str
        """
        logger.debug("input data: {0}".format(trans))

        try:
            result_dic = None
            for s in cls.parsers:
                if trans.startswith(s):  # if we have parser for the current command...
                    fn = cls.parsers[s]
                    result_dic = getattr(fn, '__func__')(cls, trans) # call the responsible parser
                    break

            #logger.debug(result_dic.__str__())

            if result_dic is None:
                result_dic = dict()
                DecodedTransaction.insertNotSupported(result_dic, trans)

            result_json = DecodedTransaction.toJson(result_dic)
        except:
            # something went wrong during converting to json string. Return not supported...
            result_dic = dict()
            DecodedTransaction.insertNotSupported(result_dic, "Unknown character found in the data coming from the radio.")
            result_json = DecodedTransaction.toJson(result_dic)
            return result_json


        #logger.debug(result_json.__str__())
        logger.debug("parsed result: {0}".format(result_json))
        return result_json


    @classmethod
    def __parse_frequency_vfo_a(cls, command):
        """
        Extracts the Frequency value from the command

        :param command: String of the type "FA00007000000;"
        :type command: str
        :return: The dict with the parsed data
        :rtype: dict
        """
        res = dict()
        DecodedTransaction.insertFreq(res, command[2:-1].lstrip('0'), vfo=Radio.VFO_A)
        return res;



    @classmethod
    def __parse_frequency_vfo_b(cls, command):
        """
        Extracts the Frequency value from the command

        :param command: String of the type "FB00007000000;"
        :type command: str
        :return: The dict with the parsed data
        :rtype: dict
        """
        result = dict()
        DecodedTransaction.insertFreq(result, command[2:-1].lstrip('0'), vfo=Radio.VFO_B)
        return result

    @classmethod
    def __parse_active_vfo(cls, command):
        """
        Extracts active VFO from the command

        :param command: String of the type "FR1;"
        :type command: str
        :return: The dict with the parsed data
        :rtype: dict
        """
        result = dict()
        if command[2] == '0':
            DecodedTransaction.insertActiveVfo(result, vfo=Radio.VFO_A)
        else:
            DecodedTransaction.insertActiveVfo(result, vfo=Radio.VFO_B)

        return result



    @classmethod
    def __parse_mode(cls, command):
        """
        Extracts the Mode value from the command

        :param command: String of the type "MD1;"
        :type command: str
        :return: The dict with the parsed data
        :rtype: dict
        """

        result = dict()

        m = cls.__mode_from_byte_to_string(int(command[2]))
        DecodedTransaction.insertMode(result, m, vfo=Radio.VFO_NONE)

        return result
        # if command[2] != '$':
        #     m = cls.__mode_from_byte_to_string(int(command[2]))
        #     DecodedTransaction.insertMode(result, m, vfo=Radio.VFO_A)
        # else:
        #     m = cls.__mode_from_byte_to_string(int(command[3]))
        #     DecodedTransaction.insertMode(result, m, vfo=Radio.VFO_B)

        return result


    @classmethod
    def __parse_info(cls, command):
        """
        Parse the IF command.

        ###   command IF00003530220      019000000030000080;
        ###   index   0123456789012345678901234567890

        [0-1] - IF
        [3-13] - frequency
        [31] - Operating mode (refer to the MD command)


        :param command: String containing the "IF" command
        :type command: str
        :return: The dict with the parsed data
        :rtype: dict
        """
        result = dict()


        mode = cls.__mode_from_byte_to_string(int(command[29]))
        vfo  = command[30]
        freq = command[2:13]
        DecodedTransaction.insertFreq(result, freq.lstrip('0'), vfo)
        DecodedTransaction.insertMode(result, mode, vfo)

        if command[30] == '0':
            DecodedTransaction.insertActiveVfo(result, vfo=Radio.VFO_A)
        else:
            DecodedTransaction.insertActiveVfo(result, vfo=Radio.VFO_B)

        return result


    @classmethod
    def __parse_smeter(cls, command):
        """
        Extracts the Smeter value from the command

        :param command: String starting of the type "SM00005;"
        :type command: str
        :return: The dict with the parsed data
        :rtype: dict
        """

        result = dict()

        smeter = command[2:-1]
        # DecodedTransaction.insertSmeter(result, command[2:-1].lstrip('0'))
        DecodedTransaction.insertSmeter(result, smeter.lstrip('0'))
        return result

    #+--------------------------------------------------------------------------+
    #|   Private methods                                                        |
    #+--------------------------------------------------------------------------+


    @classmethod
    def __vfo_number_to_letter(cls, vfo_number):
        """
        Converts VFO number to a letter that can be used in the communication with the radio
        Example: 0-->"A"; 1-->"B"
        :param vfo_number: VFO number (starting from 0)
        :type vfo_number: int
        :return: The VFO letter
        :rtype: str
        """
        if vfo_number == 0:
            return "A"
        if vfo_number == 1:
            return "B"
        else:
            raise Exception("Not allowed VFO number")


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
                logger.info("returns = " + key)
                return key

        # In case of unknown mode integer
        return "none"




    # Commands coming from the Elecraft that we can understand(parse)
    parsers = { "FA": __parse_frequency_vfo_a,       # VFO A frequency
                "FB": __parse_frequency_vfo_b,       # VFO B frequency
                "FR": __parse_active_vfo,            # active Vfo
                "MD": __parse_mode,                  # Operating mode
                "IF": __parse_info,                  # IF (Transceiver Information; GET only)
                "SM": __parse_smeter}                # S-meter values


    #+--------------------------------------------------------------------------+
    #|   Elecraft command codes
    #+--------------------------------------------------------------------------+

    # Codes used for changing the mode
    mode_codes ={'lsb':     0x01,
                 'usb':     0x02,
                 'cw':      0x03,
                 'fm':      0x04,
                 'am':      0x05,
                 'rtty':    0x06,
                 'cwr':     0x07,
                 'rttyr':   0x09}


