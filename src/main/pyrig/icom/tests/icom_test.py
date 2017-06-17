import sys

#sys.path.append("/home/potty/development/projects/jatu/target/classes/")
#import unittest
#from icom import Icom


civ_address = 0x5C
ctrl_address = 0xE0

raw_transactions = {
'empty_transaction':        bytearray([0xFE, 0xFE, ctrl_address, civ_address, 0xFD]),
'not_supported':            bytearray([0xFE, 0xFE, ctrl_address, civ_address, 0x99, 0xFD]),
'positive_cfm':             bytearray([0xFE, 0xFE, ctrl_address, civ_address, 0xFB, 0xFD]),
'negative_cfm':             bytearray([0xFE, 0xFE, ctrl_address, civ_address, 0xFA, 0xFD]),
'mode_lsb':                 bytearray([0xFE, 0xFE, ctrl_address, civ_address, 0x01, 0x00, 0xFD]),
'mode_usb':                 bytearray([0xFE, 0xFE, ctrl_address, civ_address, 0x01, 0x01, 0xFD]),
'mode_am' :                 bytearray([0xFE, 0xFE, ctrl_address, civ_address, 0x01, 0x02, 0xFD]),
'mode_cw' :                 bytearray([0xFE, 0xFE, ctrl_address, civ_address, 0x01, 0x03, 0xFD]),
'mode_rtty':                bytearray([0xFE, 0xFE, ctrl_address, civ_address, 0x01, 0x04, 0xFD]),
'mode_fm'  :                bytearray([0xFE, 0xFE, ctrl_address, civ_address, 0x01, 0x05, 0xFD]),
'mode_cwr' :                bytearray([0xFE, 0xFE, ctrl_address, civ_address, 0x01, 0x07, 0xFD]),
'mode_rttyr':               bytearray([0xFE, 0xFE, ctrl_address, civ_address, 0x01, 0x08, 0xFD]),

'freq_5bytes_1234567890':   bytearray([0xFE, 0xFE, ctrl_address, civ_address, 0x00, 0x90, 0x78, 0x56, 0x34, 0x12, 0xFD]),
'freq_5bytes_123456789' :   bytearray([0xFE, 0xFE, ctrl_address, civ_address, 0x00, 0x89, 0x67, 0x45, 0x23, 0x01, 0xFD]),
'freq_5bytes_12345678'  :   bytearray([0xFE, 0xFE, ctrl_address, civ_address, 0x00, 0x78, 0x56, 0x34, 0x12, 0x00, 0xFD]),
'freq_5bytes_1234567'   :   bytearray([0xFE, 0xFE, ctrl_address, civ_address, 0x00, 0x67, 0x45, 0x23, 0x01, 0x00, 0xFD]),
'freq_5bytes_123456'    :   bytearray([0xFE, 0xFE, ctrl_address, civ_address, 0x00, 0x56, 0x34, 0x12, 0x00, 0x00, 0xFD]),
'freq_5bytes_12345'     :   bytearray([0xFE, 0xFE, ctrl_address, civ_address, 0x00, 0x45, 0x23, 0x01, 0x00, 0x00, 0xFD]),
'freq_5bytes_1234'      :   bytearray([0xFE, 0xFE, ctrl_address, civ_address, 0x00, 0x34, 0x12, 0x00, 0x00, 0x00, 0xFD]),
'freq_5bytes_123'       :   bytearray([0xFE, 0xFE, ctrl_address, civ_address, 0x00, 0x23, 0x01, 0x00, 0x00, 0x00, 0xFD]),
'freq_5bytes_12'        :   bytearray([0xFE, 0xFE, ctrl_address, civ_address, 0x00, 0x12, 0x00, 0x00, 0x00, 0x00, 0xFD]),
'freq_5bytes_1'         :   bytearray([0xFE, 0xFE, ctrl_address, civ_address, 0x00, 0x01, 0x00, 0x00, 0x00, 0x00, 0xFD]),
'freq_5bytes_0'         :   bytearray([0xFE, 0xFE, ctrl_address, civ_address, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0xFD]),

'freq_4bytes_12345678'  :   bytearray([0xFE, 0xFE, ctrl_address, civ_address, 0x00, 0x78, 0x56, 0x34, 0x12, 0xFD]),
'freq_4bytes_1234567'   :   bytearray([0xFE, 0xFE, ctrl_address, civ_address, 0x00, 0x67, 0x45, 0x23, 0x01, 0xFD]),
'freq_4bytes_123456'    :   bytearray([0xFE, 0xFE, ctrl_address, civ_address, 0x00, 0x56, 0x34, 0x12, 0x00, 0xFD]),
'freq_4bytes_12345'     :   bytearray([0xFE, 0xFE, ctrl_address, civ_address, 0x00, 0x45, 0x23, 0x01, 0x00, 0xFD]),
'freq_4bytes_1234'      :   bytearray([0xFE, 0xFE, ctrl_address, civ_address, 0x00, 0x34, 0x12, 0x00, 0x00, 0xFD]),
'freq_4bytes_123'       :   bytearray([0xFE, 0xFE, ctrl_address, civ_address, 0x00, 0x23, 0x01, 0x00, 0x00, 0xFD]),
'freq_4bytes_12'        :   bytearray([0xFE, 0xFE, ctrl_address, civ_address, 0x00, 0x12, 0x00, 0x00, 0x00, 0xFD]),
'freq_4bytes_1'         :   bytearray([0xFE, 0xFE, ctrl_address, civ_address, 0x00, 0x01, 0x00, 0x00, 0x00, 0xFD]),
'freq_4bytes_0'         :   bytearray([0xFE, 0xFE, ctrl_address, civ_address, 0x00, 0x00, 0x00, 0x00, 0x00, 0xFD]),
'not_supported1'        :   bytearray([0xfe, 0xfe, ctrl_address, civ_address, 0x05, 0x00, 0x00, 0x35, 0x00, 0x00, 0xfd])
}

import time
import serial

# configure the serial connections (the parameters differs on the device you are connecting to)
ser = serial.Serial(
     port='/dev/ttyUSB1',
    baudrate=9600,
    parity=serial.PARITY_NONE,
    stopbits=serial.STOPBITS_ONE,
    bytesize=serial.EIGHTBITS
)

print ser.isOpen()


while 1:
    for key, value in raw_transactions.items():
        ser.write(value)
        # ser.write("fsgsdsafsfasfadsfasfadsfdfadsfasgafsagfdghdhjhgjgjfgjfjgfjfjfjgjgf ")
        # ser.write("fsgsdsafsfasfadsfasfadsfdfadsfasgafsagfdghdhjhgjgjfgjfjgfjfjfjgjgf ")
        # ser.write("fsgsdsafsfasfadsfasfadsfdfadsfasgafsagfdghdhjhgjgjfgjfjgfjfjfjgjgf ")
        print key + " -- " + value
        time.sleep(0.5)


print ser.close()


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