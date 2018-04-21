package b_tp_java;

public class B_TP_Class {
	
	/** received result */
	private short resultRecLength;
	private byte[] resultRectBuf = new byte[256];
	
	/** send result */
	private short resultSendLength;
	private byte[] resultSendtBuf = new byte[256];
	
	private byte rec_len_tmp = 0;
	private byte c_number = (byte)200;
	private byte next_f_number = 0;
	
	private static final byte protocolHead = (byte)(0XA5);
	private static final byte protocolHeadLen = (byte)(4);
	private static final byte protocolHeadCRClen = (byte)(6);
	private static final byte BLE_MTU = (byte)(20);

	/**
	 * @brief calculate CRC16
	 * */
	private short crc16(byte dat[], byte len)
	{
		short crc = (short)(0xffff);
		for(byte i = 0;i < len;i++)
		{
			crc = (short) (((short)(crc >> 8) & 0x00ff) | ((short)((crc << 8) & 0xff00)));
			crc ^= (short)(dat[i] & 0x00ff);
	        crc ^= (crc & 0xFF) >> 4;
	        crc ^= (crc << 8) << 4;
	        crc ^= ((crc & 0xFF) << 4) << 1;
		}
		return crc;
	}
	
	private void _b_tpUNPackSend(byte dat[], byte len)
	{
		byte count = (byte)((len < BLE_MTU) ? 0 : (1 + ((len - BLE_MTU) / (BLE_MTU - 1))));
		byte l_count = (byte)((len < BLE_MTU) ? len : ((len - BLE_MTU) % (BLE_MTU - 1)));
		byte i = 0;
		byte f_number = 1;
		byte index = 0;
		
		resultSendLength = 0;     /**<  reset   */
		
		for(i = 0;i < count;i++)
		{
			for(byte j = 0;j < BLE_MTU;j++)
			{
				if(i != 0 && j == 0)
				{
					resultSendtBuf[resultSendLength] = f_number;    /**< add frame number  */
					continue;
				}
				resultSendtBuf[resultSendLength + j] = dat[index];
				index++;
			}
			f_number++;
			resultSendLength = (short)((i + 1) * BLE_MTU);
		}
		
		if(l_count > 0)
		{
			if(resultSendLength > 0)
			{
				for(byte j = 0;j < (l_count + 1);j++)
				{
					if(j == 0)
					{
						resultSendtBuf[resultSendLength] = f_number;     /**< add frame number  */
						continue;
					}
					resultSendtBuf[resultSendLength + j] = dat[index];
					index++;
				}
				resultSendLength += l_count + 1;
			}
			else
			{
				for(byte j = 0;j < l_count;j++)
				{
					resultSendtBuf[resultSendLength + j] = dat[index];
					index++;
				}
				resultSendLength = l_count;
			}
		}
	}

	
	

	private byte _b_tpAnalyseSinglePacket(byte dat[], byte len)
	{
		byte retval = 1;
		
		short crc = crc16(dat, (byte)(len - 2));
		if(dat[len -2] != (crc & 0xff) || dat[len - 1] != (0xff & (crc >> 8)))
		{
			return retval;
		}
		retval = 0;
		resultRecLength = dat[3];             /**< get valid data length  */
	    for(byte i = 0;i < dat[3];i++)
	    {
	    	resultRectBuf[i] = dat[i + 4];
	    }
	    return retval;
	}

	private byte _b_tpCollectPacket(byte dat[], byte len)
	{
		byte retval = 1;
		if(dat[0] != next_f_number)
		{
			return retval;
		}
		next_f_number++;    				/**< set the next frame number   */
		for(byte i = 1;i < len;i++)
		{
    		resultRectBuf[resultRecLength] = dat[i];
    		resultRecLength++;
    		rec_len_tmp--;
    		if(rec_len_tmp == 0)
    		{
    			short crc = crc16(resultRectBuf, (byte)(resultRecLength - 2));     /**< calculate CRC16 and except crc field (2 bytes)  */
    			if((0xff & resultRectBuf[resultRecLength -2]) != (crc & 0xff) || (0xff & resultRectBuf[resultRecLength - 1]) != (0xff & (crc >> 8)))
    			{                
    				retval = 1;                     /**<  CRC16 error  */
    			}
    			else
    			{                 
    				resultRecLength = (short)(resultRecLength - protocolHeadCRClen);
    				for(short k = 0;k < resultRecLength;k++)             /**< separate the head and CRC field  */
    				{
    					resultRectBuf[k] = resultRectBuf[k + protocolHeadLen];
    				}
    				retval = 0;                    /**< all operation finished  */
    			}
    		}
		}
		return retval;
	}


	private byte _b_tpWaitFirstPacket(byte dat[], byte len)
	{
		byte retval = 1;
	    if(dat[0] != protocolHead || dat[2] != c_number)
	    {
	        return retval;
	    }
	    
	    if(dat[1] == 0x1 && len < BLE_MTU)
	    {  
	    	return retval;
	    }
	    	    
	    resultRecLength = 0;
	    if(dat[1] == 0x1)
	    {
	    	rec_len_tmp = (byte)(dat[3] + protocolHeadCRClen);    /**<  calculate total length : HEAD DATA CRC  */
	    	for(byte i = 0;i < BLE_MTU;i++)
	    	{
	    		resultRectBuf[resultRecLength] = dat[i];
	    		resultRecLength++;
	    		rec_len_tmp--;
	    	}
	    	next_f_number = 2;                                 /**< the frame number that be expected at the next time */
	    }
	    else
	    {
	    	retval = _b_tpAnalyseSinglePacket(dat, len);
	    }
	    return retval;
	}
	
	
	
	private byte _b_tpParseRecData(byte dat[], byte len)
	{
		byte retval = 1;

		if(dat[0] == protocolHead)
		{
			retval = _b_tpWaitFirstPacket(dat, len);
		}
		else
		{
			retval = _b_tpCollectPacket(dat, len);
		}
		return retval;
	}
	
	/**
	 * @brief After parsing completely, to get the result information
	 * @{
	 * */
	
	/**
	 * @param t: 0 received   
	 * 		  t: 1 send
	 * */
	public short getResultLenght(byte t) {
		short tmp = 0;
		if(t == 0)
		{
			tmp =  resultRecLength;
		}
		else
		{
			tmp = resultSendLength;
		}
		return tmp;
	}
	
	/**
	 * @param t: 0 received   
	 * 		  t: 1 send
	 * */
	public byte[] getResultBuf(byte t) {
		if(t == 0)
		{
			return resultRectBuf;
		}
		else
		{
			return resultSendtBuf;
		}
	}
	
	/**
	 * @}
	 * */

	/**
	 * @brief send/received functions
	 * @{
	 * */	
	
	/**
	 * @retval 0: ok   1: GG
	 * */
	public byte b_tpSend(byte dat[], byte len)
	{
		byte[] tmp_buf = new byte[256];
		byte total_len = (byte)(len + protocolHeadCRClen);
		byte i = 0;

	    c_number++;
	    if((c_number & 0xff) > 200) c_number = 0;		
		
		tmp_buf[0] = protocolHead;
		tmp_buf[1] = (byte)((total_len > BLE_MTU) ? 1 : 0);
		tmp_buf[2] = c_number;
		tmp_buf[3] = len;
		for(i = 0;i < len;i++)
		{
			tmp_buf[4 + i] = dat[i];
		}
		short crc = crc16(tmp_buf, (byte)(len + protocolHeadLen));
		tmp_buf[4 + i] = (byte)((crc & 0xff) & 0xff);
		tmp_buf[4 + i + 1] = (byte)(((crc >> 8) & 0xff) & 0xff);
		_b_tpUNPackSend(tmp_buf, total_len);
		return 0;
	}
	
	/**
	 * @retval 0: ok   1: GG
	 * */
	public byte b_tpReceived(byte dat[], byte len)
	{
		byte retval = 1;
		if(len > BLE_MTU)
		{
			return retval;
		}
		retval = _b_tpParseRecData(dat, len);
		return retval;
	}
	/**
	 * @}
	 * */
	
}
