package core.net.packets.incoming;

import core.Configuration;
import core.game.model.entity.player.Player;
import core.game.model.entity.player.Rights;
import core.net.packets.PacketType;

public class ItemOnGroundItem implements PacketType {

	@Override
	public void processPacket(Player p, int packetType, int packetSize) {
		int a1 = p.getInStream().readSignedWordBigEndian();
		int itemUsed = p.getInStream().readSignedWordA();
		int groundItem = p.getInStream().readUnsignedWord();
		int gItemY = p.getInStream().readSignedWordA();
		int itemUsedSlot = p.getInStream().readSignedWordBigEndianA();
		int gItemX = p.getInStream().readUnsignedWord();

		if(p.getRights().equal(Rights.DEVELOPER) && Configuration.SERVER_DEBUG) {
			p.sendMessage("ItemUsed: "+itemUsed+" groundItem: "+groundItem +
					" itemUsedSlot: " + itemUsedSlot + " gItemX: " + gItemX + " gItemY: " + gItemY);
		}		
	}
}
