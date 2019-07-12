import sys
from misc_utils import *
#sys.path.append("/home/potty/development/projects/jatu/target/classes/")
#import unittest
#from icom import Icom


raw_transactions = {
'valid_lsb':        bytearray([0x00, 0x35, 0x00, 0x00, 0x00]),
'valid_usb':        bytearray([0x00, 0x35, 0x01, 0x11, 0x01]),
'valid_cw':         bytearray([0x00, 0x35, 0x02, 0x22, 0x02]),
'valid_cwr':        bytearray([0x00, 0x35, 0x03, 0x33, 0x03]),
'valid_am':         bytearray([0x00, 0x35, 0x04, 0x44, 0x04]),
'valid_fm':         bytearray([0x00, 0x35, 0x05, 0x55, 0x08]),
'valid_dig':        bytearray([0x00, 0x35, 0x06, 0x66, 0x0A]),
'valid11':          bytearray([0x44, 0x44, 0x44, 0x44, 0x02]),
'invalid12':        bytearray([0x44, 0x44, 0x44, 0x44]),
'invalid13':        bytearray([0x44, 0x44, 0x44, 0x44, 0x02, 0x02]),
'invalid14':        bytearray([0x44]),
'invalid15':        bytearray([0x44, 0x44,]),
'invalid16':        bytearray([0x44, 0x44, 0x44,]),
'invalid17':        bytearray([0x44, 0x44, 0x44, 0x44, 0x02, 0xff, 0xff])
}

import time
import serial

# configure the serial connections (the parameters differs on the device you are connecting to)
ser = serial.Serial(
     port='COM20',
    baudrate=9600,
    parity=serial.PARITY_NONE,
    stopbits=serial.STOPBITS_TWO,
    bytesize=serial.EIGHTBITS
)

if not ser.isOpen():
    print("Com not open")

# print(get_as_hex_string(raw_transactions["valid_lsb"]))
# ser.write(raw_transactions["valid_lsb"])
while 1:
    # print(fromBcd(bytearray(ser.read())))


    for key, value in raw_transactions.items():
        ser.write(value)
        print key + " -- " + value
        time.sleep(0.5)


ser.close()


#
# class IcomTest(unittest.TestCase):
#
#     def setUp(self):
#         self.icom = Icom()
#
#     def test_mytest(self):
#         trans =  self.icom.decode(raw_transactions["mode_usb"]).getTransaction()
#         print trans
#         self.assertEqual('string', 'string')
#
# if __name__ == '__main__':
#
#     unittest.main()