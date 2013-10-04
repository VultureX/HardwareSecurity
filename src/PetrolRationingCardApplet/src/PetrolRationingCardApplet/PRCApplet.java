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
		// Good practice: Return 9000 on SELECT
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
		
		// readout balance
		case (byte) 0x01:
			Util.setShort(buf, (short)1, balance);
			break;
		
		// update balance
		case (byte) 0x02:
			balance = (short) (balance + Util.makeShort(buf[1], buf[2]));
			break;
		
		// begin transaction
		case (byte) 0x03:
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
			break;
		
		// finalize transaction
		case (byte) 0x04:
			
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
			
			break;
			
		// authenticate user
		case 0x05:
			
			// TODO use built in pincode support
			short new_pin_code = Util.makeShort(buf[1], buf[2]);
			
			if(new_pin_code == pin_code) {
				authenticated = 0x01;
			} else {
				// TODO block card if > 3 guesses
				// Wrong pin..
				ISOException.throwIt((short)0x6707);
			}
			
			break;
		
		default:
			// good practice: If you don't know the INStruction, say so:
			ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
		}
		
		apdu.setOutgoingLength((short) 5);
        apdu.sendBytes((short) 0, (short) 5);
	}
}