/**
 * 
 */
package applet;

import javacard.framework.APDU;
import javacard.framework.ISO7816;
import javacard.framework.Applet;
import javacard.framework.ISOException;
import javacard.framework.OwnerPIN;

/**
 * @author javacard
 *
 */
public class Smartcard extends Applet implements ISO7816{
	
	// FIRST BYTE - CLA
	// Instruction class value for processing
	// Has to remain 0x00 as JCOP does not support anything else
	final static byte Smartcard_CLA = (byte)0x00;
	
	// SECOND BYTE - INS
	// Instruction addressing
	final static byte VERIFY_PIN = (byte) 0x01;
	final static byte GET_ALLOWANCE = (byte) 0x02;
	final static byte REFRESH_ALLOWANCE = (byte) 0x03;
	final static byte DEBIT_ALLOWANCE = (byte) 0x04;
	
	// OTHER CONSTANTS
	// Maximum allowance
	final static byte MAX_ALLOWANCE = 100;
	// Maximum of 3 PIN tries
	final static byte MAX_PIN_TRIES = (byte) 0x03;
	// Maximum PIN length of 2 bytes
	final static byte MAX_PIN_LENGTH = (byte) 0x04;
	
	// SIGNALS
	// PIN verification failed
	final static short SW_VERIFICATION_FAILED = 0x6300;
	// PIN verification required
	final static short SW_VERIFICATION_REQUIRED = 0x6301;
	// Incorrect PIN format
	private static final short SW_INCORRECT_PIN_FORMAT = 0x6302;
	// Invalid transaction amount
	final static short SW_INVALID_TRANSACTION_AMOUNT = 0x6303;
	// Insufficient allowance
	final static short SW_INSUFFICIENT_ALLOWANCE = 0x6304;

	
	
	
	// VARIABLES
	OwnerPIN PIN;
	byte allowance;
	
	// CONSTRUCTOR
	private Smartcard(byte[] bArray, short bOffset, byte bLen){
		// Instantiate new PIN
		PIN = new OwnerPIN(MAX_PIN_TRIES, MAX_PIN_LENGTH);
		PIN.update(bArray, bOffset, bLen);
		
		// Set allowance
		allowance = 100;
		
		this.register();
	}
	
	// MAIN METHOD
	// Install method retrieves values during applet installation
	public static void install(byte[] bArray, short bOffset, byte bLength) {
		bArray[0] = (byte) 0x11;
		bArray[1] = (byte) 0x22;
		bArray[2] = (byte) 0x33;
		bArray[3] = (byte) 0x44;
		bOffset = 0;
		bLength = 4;
		new Smartcard(bArray, bOffset, bLength);
	}
	
	// If there are no remaining tries, the application is blocked
	public boolean select(){
		if(PIN.getTriesRemaining() == 0){
			return false;	
		}else{
			return true;
		}
	}
	
	// Cleanup function for application switching
	// Unfortunately it resets the PIN try counter too (bad?)
	public void deselect(){
		PIN.reset();
	}
	
	// When this applet is selected and returns true then this method is called
	// 
	public void process(APDU apdu) {
		
		byte[] buf = apdu.getBuffer();
		
		// Good practice: Return 9000 on SELECT
		// Returns control to JCRE when another SELECT comes
		if ((buf[ISO7816.OFFSET_CLA] == (byte) 0x00) &&
			(buf[ISO7816.OFFSET_INS] == (byte) 0xA4)) {
			return;
		}
		
		// If command APDU has wrong class of command
		if(buf[ISO7816.OFFSET_CLA] != Smartcard_CLA){
			ISOException.throwIt(SW_CLA_NOT_SUPPORTED);
		}
			
		
		
		switch (buf[ISO7816.OFFSET_INS]) {
		case VERIFY_PIN:
			verifyPIN(apdu);
			break;
		case GET_ALLOWANCE:
			getAllowance(apdu);
			break;
		case REFRESH_ALLOWANCE:
			refreshAllowance();
			break;
		case DEBIT_ALLOWANCE:
			debitAllowance(apdu);
			break;
		default:
			// good practice: If you don't know the instruction, say so:
			ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
		}
	}

	private void verifyPIN(APDU apdu) {
		// TODO Auto-generated method stub
		
		// Get APDU buffer
		byte[] buf = apdu.getBuffer();
		
		// Length of data field in APDU
		// Data starts from ISO7816.OFFSET_CDATA
		byte bufLen = buf[ISO7816.OFFSET_LC];
		
		// Check how much data was read into the buffer
		byte bytesRead = (byte)apdu.setIncomingAndReceive();
		
		// Sanity checks
		if((bufLen != MAX_PIN_LENGTH) || (bytesRead != MAX_PIN_LENGTH)){
			ISOException.throwIt(SW_INCORRECT_PIN_FORMAT);
		}
		
		if(! PIN.check(buf, OFFSET_CDATA, MAX_PIN_LENGTH)){
			ISOException.throwIt(SW_VERIFICATION_FAILED);
		}
		
	}
	
	private void getAllowance(APDU apdu) {
		// TODO Auto-generated method stub
		if(! PIN.isValidated()){
			ISOException.throwIt(SW_VERIFICATION_REQUIRED);
		}
		
		byte[] buf = apdu.getBuffer();
		
		// Signalize sending modus to JCRE and
		// set length of expected answer
		short lengthExpected = apdu.setOutgoing();
		if(lengthExpected != 1){
			ISOException.throwIt(SW_WRONG_LENGTH);
		}
		
		// Inform CAD about length of returned bytes
		apdu.setOutgoingLength((byte)1);
		buf[0] = allowance;
		
		// Send allowance information
		apdu.sendBytes((short)0, (short)1);
	}
	
	private void refreshAllowance() {
		// TODO Auto-generated method stub
		if(! PIN.isValidated()){
			ISOException.throwIt(SW_VERIFICATION_REQUIRED);
		}
		
		// TODO More sanity checks
		
		// Refresh allowance
		allowance = MAX_ALLOWANCE;
		
	}
	
	private void debitAllowance(APDU apdu){
		// Get APDU buffer
		byte[] buf = apdu.getBuffer();
		
		// Length of data field in APDU
		// Data starts from ISO7816.OFFSET_CDATA
		byte bufLen = buf[ISO7816.OFFSET_LC];
		
		// Check how much data was read into the buffer
		byte bytesRead = (byte)apdu.setIncomingAndReceive();
		
		// Sanity checks:
		// The balance should be 1 byte long
		// The amount of bytes read should be 1 byte long
		if((bufLen != 1) || (bytesRead != 1)){
			ISOException.throwIt(SW_WRONG_LENGTH);
		}
		
		// TRANSACTION
		byte debitAmount = buf[ISO7816.OFFSET_CDATA];
		
		// Sanity checks:
		// 
		if((debitAmount > MAX_ALLOWANCE) || (debitAmount < 0)){
			ISOException.throwIt(SW_INVALID_TRANSACTION_AMOUNT);
		}
		
		if((allowance - debitAmount) < 0){
			ISOException.throwIt(SW_INSUFFICIENT_ALLOWANCE); 
		}
		
		allowance = (byte)(allowance - debitAmount);
	}
	
}