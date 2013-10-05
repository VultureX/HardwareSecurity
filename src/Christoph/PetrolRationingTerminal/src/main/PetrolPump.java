package main;

import java.util.List;

import javax.smartcardio.*;

public class PetrolPump{
	
	// CONSTANTS AND VARIABLES
	final static byte ALLOWANCE_SIZE = 0x01;
	static byte[] pin;
	static byte debitAmount = 0;
	
	// SELECT APPLICATION
	final static byte[] PETROL_RATIONING_APPLET_AID = {
		(byte) 0xAB, (byte) 0xCD, (byte) 0xEF, (byte) 0x12, (byte) 0x35};
	
	// SELECT INSTRUCTIONS
	final static byte Smartcard_CLA = (byte)0x00;
	final static byte VERIFY_PIN = (byte) 0x01;
	final static byte GET_ALLOWANCE = (byte) 0x02;
	final static byte REFRESH_ALLOWANCE = (byte) 0x03;
	final static byte DEBIT_ALLOWANCE = (byte) 0x04;
	
	// SIGNALS
	final static short SW_VERIFICATION_FAILED = 0x6300;
	final static short SW_VERIFICATION_REQUIRED = 0x6301;
	private static final short SW_INCORRECT_PIN_FORMAT = 0x6302;
	final static short SW_INVALID_TRANSACTION_AMOUNT = 0x6303;
	final static short SW_INSUFFICIENT_ALLOWANCE = 0x6304;
	

	public PetrolPump() {
	    try {
	    	TerminalFactory tf = TerminalFactory.getDefault();
	    	CardTerminals ct = tf.terminals();
	    	List<CardTerminal> cs = ct.list(CardTerminals.State.CARD_PRESENT);
	    	if (cs.isEmpty()) {
	    		System.err.println("No terminals with a card found.");
	    		return;
	    	}
	    	
	    	for(CardTerminal c : cs) {
	    		if (c.isCardPresent()) {
	    			try {
	    				Card card = c.connect("*");
	    				try {
	    					System.out.println("--- Init channel ---");
	    					CardChannel ch = card.getBasicChannel();
	    					
	    					/* SELECT APPLICATION */
	    					if (! selectApplication(ch)){
	    						throw new Exception("Selecting applet failed");
	    					}
	    					
	    					/* VERIFY PIN */
	    					pin = new byte[]{ (byte) 0x11, (byte) 0x22, (byte) 0x33, (byte) 0x44 };
	    					if (! verifyPIN(pin, ch)){
	    						throw new Exception("PIN verification failed");
	    					}
	    					
	    					/* RETRIEVE BALANCE */
	    					if (! getBalance(ch)){
	    						throw new Exception("Allowance retrieval failed");
	    					}
	    					
	    					
	    					System.out.println("--- Attempting to debit 80 litres ---");
	    					if (! debitAllowance((byte)80, ch)){
	    						throw new Exception("Debiting allowance failed");
	    					}

	    					
	    				} catch (Exception e) {
	    					e.printStackTrace();
	    				}
	    				card.disconnect(false);
	    			} catch (CardException e) {
	    				System.err.println("Couldn't connect to card!");
	    			}
	    			return;
	    		} else {
	    			System.err.println("No card present!");
	    		}
	    	}
    	} catch (CardException e) {
    		System.err.println("Card status problem!");
	    }
	}

	private void printMsg(byte[] msg){
		System.out.println("Message length: " + msg.length);
		for(int i=0; i < msg.length; i++){
			System.out.format(" %02d", Integer.parseInt(Integer.toHexString(msg[i] & 0x000000FF)));
			if(i == msg.length - 1)
				System.out.print("\n");
		}
		
		
		for(int i=0; i < msg.length; i++){
			if(i % 8 == 0)
				System.out.print("|");
			System.out.print("++ ");
		}
		System.out.println();
	}
	
	private boolean selectApplication(CardChannel ch) throws CardException{
		final CommandAPDU SELECT =
			new CommandAPDU((byte) 0x00, (byte) 0xA4, (byte) 0x04, (byte) 0x00, PETROL_RATIONING_APPLET_AID);
		
		ResponseAPDU resp = ch.transmit(SELECT);
		
		if (resp.getSW() != 0x9000) {
			System.out.println("--- SELECTING APPLET FAILED ---");
			return false;
		}
		return true;
	}
	
	
	private boolean verifyPIN(byte[] pin, CardChannel ch) throws CardException{
		final CommandAPDU VERIFY = new CommandAPDU(Smartcard_CLA, VERIFY_PIN, 0, 0, pin);
		ResponseAPDU resp = ch.transmit(VERIFY);

		if (errorHandler(resp.getSW())) {
			System.out.println("+ PIN SUCCESSFUL");
			return true;
		}else{
			return false;
		}
	}
	
	private boolean getBalance(CardChannel ch) throws CardException{
		final CommandAPDU BALANCE = new CommandAPDU(Smartcard_CLA, GET_ALLOWANCE, 0, 0, ALLOWANCE_SIZE);
		ResponseAPDU resp = ch.transmit(BALANCE);
		
		if (errorHandler(resp.getSW())) {
			System.out.println("+ Remaining allowance: " + (byte)resp.getData()[0]);
			return true;
		}else{
			return false;
		}
	}
	
	@SuppressWarnings("unused")
	private boolean refreshAllowance(CardChannel ch) throws CardException{
		final CommandAPDU REFRESH = new CommandAPDU(Smartcard_CLA, REFRESH_ALLOWANCE, 0, 0);
		ResponseAPDU resp = ch.transmit(REFRESH);
		
		if (errorHandler(resp.getSW())) {
			System.out.println("Successfully refreshed allowance");
			return true;
		}else{
			return false;
		}
	}
	
	private boolean debitAllowance(byte amount, CardChannel ch) throws CardException{
		final CommandAPDU DEBIT = new CommandAPDU(Smartcard_CLA, DEBIT_ALLOWANCE, 0, 0, new byte[]{amount});
		ResponseAPDU resp = ch.transmit(DEBIT);

		if (errorHandler(resp.getSW())) {
			System.out.println("Successfully debited card");
			return true;
		}else{
			return false;
		}
	}
	
	private boolean errorHandler(int exitCode){
		switch(exitCode){
			case 0x9000:
				return true;
			case SW_VERIFICATION_FAILED:
				System.out.println("--- PIN VERIFICATION FAILED ---");
				break;
			case SW_VERIFICATION_REQUIRED:
				System.out.println("--- PIN VERIFICATION REQUIRED ---");
				break;
			case SW_INCORRECT_PIN_FORMAT:
				System.out.println("--- INCORRECT PIN FORMAT ---");
				break;
			case SW_INVALID_TRANSACTION_AMOUNT:
				System.out.println("--- INVALID TRANSACTION AMOUNT ---");
				break;
			case SW_INSUFFICIENT_ALLOWANCE:
				System.out.println("--- INSUFFICIENT ALLOWANCE ---");
				break;
			case 0x6700:
				System.out.println("--- WRONG LENGTH ---");
				break;
			default:
				System.out.println("--- UNKNOWN ERROR CODE " + Integer.toHexString(exitCode) + " ---");
				break;
		}
		return false;
	}
	
	public static void main(String[] arg) {
		new PetrolPump();
	}
	
}
