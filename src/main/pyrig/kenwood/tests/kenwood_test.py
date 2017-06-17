import sys

#sys.path.append("/home/potty/development/projects/jatu/target/classes/")
#import unittest
#from icom import Icom


civ_address = 0x5C

raw_transactions = {
'empty_transaction':             ";",
'not_supported':                 "AA33434353;",
'not_supported2':                "SFDSFSFSFDF;",
'TRASH':                         "SFDSFS44FSFDF",
'vfoA_mode_lsb':                 "MD1;",
'vfoA_mode_usb':                 "MD2;",
'vfoA_mode_am' :                 "MD5;",
'vfoA_mode_cw' :                 "MD3;",
'vfoA_mode_rtty':                "MD6;",
'vfoA_mode_fm'  :                "MD4;",
'vfoA_mode_cwr' :                "MD7;",
'vfoA_mode_rttyr':               "MD9;",

'vfoB_mode_lsb':                 "MD$1;",
'vfoB_mode_usb':                 "MD$2;",
'vfoB_mode_am' :                 "MD$5;",
'vfoB_mode_cw' :                 "MD$3;",
'vfoB_mode_rtty':                "MD$6;",
'vfoB_mode_fm'  :                "MD$4;",
'vfoB_mode_cwr' :                "MD$7;",
'vfoB_mode_rttyr':               "MD$9;",

'vfoA_freq_5bytes_1234567890':   "FA01234567890;",
'vfoA_freq_5bytes_1234567890':   "FA00000007890;",
'vfoA_freq_5bytes_1234567890':   "FA01234567890;",
'vfoA_freq_5bytes_12345678901':  "FA00005678901;",
'vfoA_freq_5bytes_0':            "FA00000000000;",

'vfoB_freq_5bytes_1234567890':   "FB01234567890;",
'vfoB_freq_5bytes_12345678901':  "FB12345678901;",
'vfoB_freq_5bytes_0':            "FB00000000000;",

'IF_vfoA':            "IF00014267890+yyyyrx*00t10spbd1*;",
'IF_vfoA1':            "IF00015267890+yyyyrx*00t20spbd1*;",

'IF_vfoB1':            "IF00007267891+yyyyrx*00t11spbd1*;",
'IF_vfoB2':            "IF00007267892+yyyyrx*00t11spbd1*;",
'IF_vfoB3':            "IF00007267893+yyyyrx*00t11spbd1*;",
'IF_vfoB4':            "IF00007267894+yyyyrx*00t11spbd1*;",
'IF_vfoB5':            "IF00007267895+yyyyrx*00t11spbd1*;",
'IF_vfoB61':           "IF00008267896+yyyyrx*00t21spbd1*;",
'IF_vfoB71':           "IF00008267897+yyyyrx*00t21spbd1*;",
'IF_vfoB81':           "IF00008267898+yyyyrx*00t21spbd1*;",
'IF_vfoB91':           "IF00008267899+yyyyrx*00t21spbd1*;",
'IF_vfoB01':           "IF00008267900+yyyyrx*00t21spbd1*;",

}


import time
import serial

# configure the serial connections (the parameters differs on the device you are connecting to)
ser = serial.Serial(
    port='/dev/ttyUSB1',
    baudrate=38400,
    parity=serial.PARITY_NONE,
    stopbits=serial.STOPBITS_TWO,
    bytesize=serial.EIGHTBITS
)

print ser.isOpen()


while 1:
    for key, value in raw_transactions.items():
        ser.write(value)
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