package b_tp_java;



public class Main {
	

	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		B_TP_Class proto_bt = new B_TP_Class();
		byte[] table = new byte[50];
		byte[] result;
		byte[] tmp = new byte[20];
		
		for(byte i = 0;i < 50;i++)
		{
			table[i] = i;
		}
		while(true)
		{
			for(byte i = 0;i < 50;i++)
			{
				table[i] += 1;
			}
			
			if(0 == proto_bt.b_tpSend(table, (byte)50))
			{
				short len = proto_bt.getResultLenght((byte)1);
				System.out.printf("%d\n", len);
				result = proto_bt.getResultBuf((byte)1);
				for(short j = 0;j < len;j++)
				{
					if(j % 20 == 0)
					{
						System.out.printf("\n");
					}
					System.out.printf("%d ", (result[j] & 0xff));
				}
				System.out.printf("\n");
				
				for(short j = 0; j < ((len + 19) / 20); j++)
				{
					byte tmp_len = (byte) (len - (j * 20));
					if(tmp_len >= 20)
					{
						for(byte i = 0;i < 20;i++)
						{
							tmp[i] = result[j * 20 + i];
						}
					}
					else
					{
						for(byte i = 0;i < 20;i++)
						{
							tmp[i] = result[j * 20 + i];
						}
					}
					tmp_len = (tmp_len > 20) ? 20 : tmp_len;
					if(0 == proto_bt.b_tpReceived(tmp, tmp_len))
					{
						len = proto_bt.getResultLenght((byte)0);
						System.out.printf("%d\n", len);
						result = proto_bt.getResultBuf((byte)0);
						for(j = 0;j < len;j++)
						{
							System.out.printf("%d ", (result[j] & 0xff));
						}
						break;
					}
					
				}
				
				
			}
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}

}
