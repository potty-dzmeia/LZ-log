from org.lz1aq.py.rig import I_EncodedTransaction


class EncodedTransaction(I_EncodedTransaction):
    """
    Contains the transaction that is to be send to the rig together with some
    additional details of how it should be send (e.g. timeout, retry etc.)
    """

    def __init__(self, transaction, is_cfm_expected=False, write_delay=0, post_write_delay=0, timeout=200, retry=0):
        """
         Creates an object holding the transaction together with some additional control variables.
        When the object is created the control variables are given default values. When needed the
        user has to change them manually.

        :param transaction: String of bytes with the encoded transaction ready to be send to the rig
        :type transaction: str
        :param is_cfm_expected: If the program should expect confirmation after sending this transaction to the rig
        :type is_cfm_expected: bool
        :param write_delay: If there should be a delay between each byte of the transaction being sent out (in milliseconds)
        :type write_delay: int
        :param post_write_delay: If there should be a delay between the transactions (in milliseconds)
        :type post_write_delay: int
        :param timeout: Timeout after which we should abandon sending the transaction to the rig (in milliseconds).
                        If confirmationExpected_ is 0 there will be no retries anyway.
        :type timeout: int
        :param retry: Maximum number of retries if command fails (0 for no retry)
        :type retry: int
        """
        # underscores are added because of a jython issue
        self.transaction_          = transaction        # The data (i.e. the transaction itself)
        self.confirmationExpected_ = int(is_cfm_expected)   # If the program should expect confirmation after sending this transaction to the rig
        self.writeDelay_           = write_delay        # If there should be a delay between each byte of the transaction being sent out (in milliseconds)
        self.postWriteDelay_       = post_write_delay;  # If there should be a delay between each transaction send out (in milliseconds)
        self.timeout_              = timeout            # Timeout after which we should abandon sending the transaction to the rig (in milliseconds)
        self.retry_                = retry              # Maximum number of retries if command fails (0 for no retry).


    def getTransaction(self):
        """
        :return: The encoded transaction ready to be send to the rig
        :rtype: str
        """
        return self.transaction_


    def getWriteDelay(self):
        """
        :return: Delay between each byte of the transaction being sent out (in milliseconds)
        :rtype: int
        """
        return self.writeDelay_


    def getPostWriteDelay(self):
        """
        :return: Delay between each transaction send out (in milliseconds)
        :rtype: int
        """
        return self.postWriteDelay_


    def getTimeout(self):
        """
        :return: Timeout after which we should not wait for confirmation from the rig (in milliseconds)
        :rtype: int
        """
        return self.timeout_


    def getRetry(self):
        """
        :return: Maximum number of retries if sending the transaction fails (e.g. timeout or negative response).
        Set to 0 for no retry.
        :rtype: int
        """
        return self.retry_


    def isConfirmationExpected(self):
        """
        :return: True - if we should wait for confirmation from the rig that the transaction was received
        :rtype: int
        """
        return self.confirmationExpected_




