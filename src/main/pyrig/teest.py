

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


def toBcd(number, bcd_len, endian="little"):
    """
    Converts number to 4bit BCD values (big endian).
    Example: 439,700.000MHz toBcd(439700000, 8) --> [0x43, 0x97, 0x00, 0x00]    (note that the least significant 0 is dropped)

    :param number: number to be converted to BCD format
    :type number: int
    :param bcd_len: how many BCD character should the output contain (must be an even value)
    :type bcd_len: int
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


mode_codes ={'lsb':     0x00,
             'usb':     0x01,
             'cw':      0x02,
             'fm':      0x08,
             'am':      0x04,
             'rtty':    0x0A,
             'cwr':     0x03}

from misc_utils import  *

freq = 3700000
freq /= 10
freq = toBcd(freq, 4, "big")

freq.append(02)

print(printListInHex(freq))

if freq.__len__() != 5:
    print "error"

for mode, code in mode_codes.items():
    if code== freq[4]:
        result = mode

# Mode
print result
print(fromBcd(freq[0:4],"big")*10)