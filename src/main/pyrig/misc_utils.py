import os



def get_logging_config():
    """Returns the absolute path to the logging config file
    """

    # We need to find the path to the /pyrig
    from java.lang import ClassLoader
    cl = ClassLoader.getSystemClassLoader()
    paths = map(lambda url: url.getFile(), cl.getURLs())

    # Search all available paths for the one to /pyrig
    path = next(x for x in paths if "pyrig" in x)

    # Now we can return the absolute path to the logging file
    return os.path.join(path, "logging.conf")


def fromBcd(byte_array, endian="little"):
    """
    Converts a number in little endian 4bits per digit bcd format to binary

    Example:
    [0x31, 0x02, 0x10, 0x14, 0x0] converts to 14100231

    :param byteArray: digits in 4bit bcd format (little endian)
    :type byteArray: bytearray
    :param endian: "little" or "big"
    :type endian: str
    :return: number
    :rtype: int
    """
    if endian == "big":
        byte_array.reverse()

    result = 0;
    multi = 1;
    for b in byte_array:
        result += (b & 0x0F) * multi
        multi *= 10
        result += ((b>>4) & 0x0F) * multi
        multi *= 10

    return result


def toBcd(number, bcd_len, endian="little"):
    """
    Converts number to 4bit BCD values (big endian).
    Example: 439,700.00MHz toBcd(43970000, 8, "big") --> [0x43, 0x97, 0x00, 0x00]
    Example: 439,700.00MHz toBcd(43970000, 8)        --> [0x00, 0x00, 0x97, 0x43]

    :param number: number to be converted to BCD format
    :type number: int
    :param bcd_len: how many BCD character should the output contain (must be an even value)
    :type bcd_len: int
    :param endian: "little" or "big"
    :type endian: str
    :return: list of integers containing the BCD values
    :rtype: list
    """

    if bcd_len % 2 != 0:
        raise ValueError("bcd_len should be even number!")
    if len(str(number)) > bcd_len:
        raise ValueError("number is too big!")

    result = []

    for i in range(0, bcd_len / 2):
        byte = number % 10;
        number /= 10
        byte |= (number % 10) << 4
        number /= 10
        result.append(byte)

    if endian == "big":
        result.reverse()
    return result


def printListInHex(lst):
    print ' '.join('0x%02x' % b for b in lst)


def getListInHex(lst):
    return ' '.join('0x%02x' % b for b in lst)

def get_as_hex_string(data):
    if type(data) == str:
        return ' '.join('0x%02x' % ord(b) for b in data)
    elif type(data) == list:
        return ' '.join('0x%02x' % b for b in data)
    elif type(data) == bytearray:
        return ' '.join('{:02x}'.format(x) for x in data)


#----------------------------------------------------------------
# Unit testing below
#----------------------------------------------------------------
