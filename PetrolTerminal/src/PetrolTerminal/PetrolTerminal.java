package PetrolTerminal;

import java.util.List;

import javax.smartcardio.Card;
import javax.smartcardio.CardChannel;
import javax.smartcardio.CardException;
import javax.smartcardio.CardTerminal;
import javax.smartcardio.CardTerminals;
import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;
import javax.smartcardio.TerminalFactory;

public class PetrolTerminal {

    static final byte[] PETROL_APPLET_AID = { (byte) 0xDE, (byte) 0xAD,
        (byte) 0xBE, (byte) 0xEF, (byte) 0x01};

    static final CommandAPDU SELECT_APDU = new CommandAPDU(
		(byte) 0x00, (byte) 0xA4, (byte) 0x04, (byte) 0x00, PETROL_APPLET_AID);
	
    static final CommandAPDU PIN_APDU = new CommandAPDU(new byte[] {(byte) 0x01, (byte) 0x04, (byte) 0xD2, (byte) 0x04});
	
	public PetrolTerminal() {
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
	    					CardChannel ch = card.getBasicChannel();
	    					ResponseAPDU resp = ch.transmit(SELECT_APDU);
	    					System.out.println("SELECT SW: " + Integer.toHexString(resp.getSW()));
	    					
	    					resp = ch.transmit(PIN_APDU);
	    					System.out.println("Pin SW: " + Integer.toHexString(resp.getSW()));
	    				} catch (Exception e) {
	    					System.err.println("Card is not a Chipknip?!");
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
	
	
	
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		new PetrolTerminal();
	}

}
