package PetrolRationingCardApplet;

import javacard.framework.APDU;
import javacard.framework.ISO7816;
import javacard.framework.Applet;
import javacard.framework.ISOException;
import javacard.framework.Util;

/**
 * @author javacard
 *
 *
 *	Error codes:
 *	0x6701		Card blocked
 *	0x6702		Already in transaction
 *	0x6703		No current transaction
 *	0x6704		Wrong transaction id
 *	0x6705		Cannot update balance
 *	0x6706		Not authenticated
 *	0x6707		Wrong pincode
 *
 */
public class PRCApplet extends Applet {
	private byte blocked = 0x00;
	
	// TODO move transient memory
	private byte authenticated = 0x00;
	
	// TODO use build in pincode support
	private short pin_code = 1234; 
	
	// TODO allow fractions
	private short balance = 0;
	
	private byte in_transaction = 0x00;
	private byte transaction_id = 0x00;
	
	public static void install(byte[] bArray, short bOffset, byte bLength) {
		// GP-compliant JavaCard applet registration
		new PetrolRationingCardApplet.PRCApplet().register(bArray,
				(short) (bOffset + 1), bArray[bOffset]);
	}

	public void process(APDU apdu) {
		if (selectingApplet()) {
			return;
		}
		
		// if blocked, panic
		if(blocked == 0x01) {
			ISOException.throwIt((short)0x6701);
		}
		
		byte[] buf = apdu.getBuffer();

		switch (buf[ISO7816.OFFSET_INS]) {
		case (byte) 0x00:
			break;
		
		case (byte) 0x01:
			readBalance(apdu);
			break;
		
		case (byte) 0x06:
			generateUpdateRequest(apdu);
			break;

		case (byte) 0x02:
			updateBalance(apdu);
			break;
		
		case (byte) 0x03:
			beginTransaction(apdu);
			break;
		
		case (byte) 0x04:
			finalizeTransaction(apdu);
			break;
			
		case (byte) 0x05:
			verifyPIN(apdu);			
			break;
		
		default:
			ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
		}
		
		apdu.setOutgoingLength((short) 5);
        apdu.sendBytes((short) 0, (short) 5);
	}

	/**
	 *	APDU code: 0x01
	 *  Function: return the current balance of the card
	 *	TODO: return expiration date also
	 */
	private void readBalance(APDU apdu) {
		// if the user is not yet authenicated, panic
		if(authenticated == 0x00) {
			ISOException.throwIt((short)0x6706);
		}
		
		byte[] buf = apdu.getBuffer();
		Util.setShort(buf, (short)1, balance);
	}

	/**
	 *	APDU code: 0x02
	 *  Function: update the balance
	 *	TODO: update expiration date also
	 *	TODO: check signature of the update
	 */
	private void updateBalance(APDU apdu) {
		byte[] buf = apdu.getBuffer();
		balance = (short) (balance + Util.makeShort(buf[1], buf[2]));
	}

	/**
	 *	APDU code: 0x03
	 *  Function: begin a transaction
	 *	TODO: check expiration date
	 */
	private void beginTransaction(APDU apdu) {
		byte[] buf = apdu.getBuffer();

		// if already in transaction, panic
		if(in_transaction == 0x01) {
			ISOException.throwIt((short)0x6702);
		}
	
		// if the user is not yet authenicated, panic
		if(authenticated == 0x00) {
			ISOException.throwIt((short)0x6706);
		}
		
		// TODO do this in a transaction
		in_transaction = 0x01;
		transaction_id = buf[1];
		
		// return maximum allowed
		Util.setShort(buf, (short)1, balance);
	}


	/**
	 *	APDU code: 0x04
	 *  Function: finalize a transaction
	 */
	private void finalizeTransaction(APDU apdu) {
		byte[] buf = apdu.getBuffer();

		// if there is no current transaction, panic
		if(in_transaction == 0x00) {
			ISOException.throwIt((short)0x6703);
		}
	
		// wrong transaction id
		if(buf[1] != transaction_id) {
			// TODO should this block the card?
			ISOException.throwIt((short)0x6704);
		}
		
		short balance_update = Util.makeShort(buf[1], buf[2]);
		
		// check if we can actually update the balance
		if(balance_update > balance || balance_update < 0) {
			ISOException.throwIt((short)0x6705);
		}
		
		// TODO do this in a transaction
		in_transaction = 0x00;
		balance = (short) (balance - balance_update);
	}

	/**
	 *	APDU code: 0x05
	 *  Function: authenticate card owner by verifying the PIN code
	 *	TODO: use the Jcard built-in functions
	 */
	private void verifyPIN(APDU apdu) {
		byte[] buf = apdu.getBuffer();	
		
		// TODO use built in pincode support
		short new_pin_code = Util.makeShort(buf[1], buf[2]);
		
		if(new_pin_code == pin_code) {
			authenticated = 0x01;
		} else {
			// TODO block card if > 3 guesses
			// Wrong pin..
			ISOException.throwIt((short)0x6707);
		}
	}

	/**
	 *	APDU code: 0x06
	 *  Function: generate an update request
	 *	TODO: generate random number
	 *	TODO: add current balance and expiration date
	 *	TODO: sign the request
	 */
	private void generateUpdateRequest(APDU apdu) {
		byte[] buf = apdu.getBuffer();

		// if the user is not yet authenicated, panic
		if(authenticated == 0x00) {
			ISOException.throwIt((short)0x6706);
		}

		// TODO: make random
		buf[1] = 0x66;
	}
}