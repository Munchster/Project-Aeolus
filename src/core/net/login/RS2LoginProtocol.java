package core.net.login;

import java.security.SecureRandom;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.frame.FrameDecoder;

import core.Configuration;
import core.Server;
import core.game.GameConstants;
import core.game.model.entity.player.Player;
import core.game.model.entity.player.PlayerHandler;
import core.game.model.entity.player.Rights;
import core.game.model.entity.player.save.PlayerSave;
import core.game.util.Misc;
import core.net.PacketBuilder;
import core.net.security.ISAACCipher;

@SuppressWarnings("all")
public class RS2LoginProtocol extends FrameDecoder {

	private static final int CONNECTED = 0;
	private static final int LOGGING_IN = 1;
	private int state = CONNECTED;

	@Override
	protected Object decode(ChannelHandlerContext ctx, Channel channel, ChannelBuffer buffer) throws Exception {
		if(!channel.isConnected()) {
			return null;
		}
		switch (state) {
		case CONNECTED:
			if (buffer.readableBytes() < 2)
				return null;
			int request = buffer.readUnsignedByte();
			if (request != 14) {
				System.out.println("Invalid login request: " + request);
				channel.close();
				return null;
			}
			buffer.readUnsignedByte();
			channel.write(new PacketBuilder().putLong(0).put((byte) 0).putLong(new SecureRandom().nextLong()).toPacket());
			state = LOGGING_IN;
			return null;
		case LOGGING_IN:
			
			if (buffer.readableBytes() < 2) {
				return null;
			}
			
			int loginType = buffer.readByte();
			if (loginType != 16 && loginType != 18) {
				System.out.println("Invalid login type: " + loginType);
				//channel.close();
				//return null;
			}
			//System.out.println("Login type = "+loginType);
			int blockLength = buffer.readByte() & 0xff;
			if (buffer.readableBytes() < blockLength) {
				return null;
			}
			
			buffer.readByte();
			
			int clientVersion = buffer.readShort();
			/*if (clientVersion != 317) {
				System.out.println("Invalid client version: " + clientVersion);
				channel.close();
				return null;
			}*/
			
			buffer.readByte();
			
			for (int i = 0; i < 9; i++)
				buffer.readInt();
			
			
			buffer.readByte();
			
			int rsaOpcode = buffer.readByte();
			if (rsaOpcode != 10) {
				System.out.println("Unable to decode RSA block properly!");
				channel.close();
				return null;
			}
			
			final long clientHalf = buffer.readLong();
			final long serverHalf = buffer.readLong();
			final int[] isaacSeed = { (int) (clientHalf >> 32), (int) clientHalf, (int) (serverHalf >> 32), (int) serverHalf };
			final ISAACCipher inCipher = new ISAACCipher(isaacSeed);
			for (int i = 0; i < isaacSeed.length; i++)
				isaacSeed[i] += 50;
			final ISAACCipher outCipher = new ISAACCipher(isaacSeed);
			final int version = buffer.readInt();
			final String name = Misc.formatPlayerName(Misc.getRS2String(buffer));
			final String pass = Misc.getRS2String(buffer);
			channel.getPipeline().replace("decoder", "decoder", new RS2Decoder(inCipher));
			return login(channel, inCipher, outCipher, version, name, pass);
		}
		return null;
	}

	private static Player login(Channel channel, ISAACCipher inCipher, ISAACCipher outCipher, int version, String name, String pass) {
		int returnCode = 2;
		if (!name.matches("[A-Za-z0-9 ]+")) {
			returnCode = 4;
		}
		if (name.length() > 12) {
			returnCode = 8;
		}
		Player cl = new Player(channel, -1);
		cl.playerName = name;
		cl.playerName2 = cl.playerName;
		cl.playerPass = pass;
		cl.outStream.packetEncryption = outCipher;
		cl.saveCharacter = false;
		cl.isActive = true;
		if (PlayerHandler.isPlayerOn(name)) {
			returnCode = 5;
		}
		if (PlayerHandler.getPlayerCount() >= GameConstants.MAX_PLAYERS) {
			returnCode = 7;
		}
		if (Server.UpdateServer) {
			returnCode = 14;
		}
		if (returnCode == 2) {
			int load = PlayerSave.loadGame(cl, cl.playerName, cl.playerPass);
			if (load == 0)
				cl.addStarter = true;
			if (load == 3) {
				returnCode = 3;
				cl.saveFile = false;
			} else {
				for (int i = 0; i < cl.playerEquipment.length; i++) {
					if (cl.playerEquipment[i] == 0) {
						cl.playerEquipment[i] = -1;
						cl.playerEquipmentN[i] = 0;
					}
				}
				if (!Server.playerHandler.newPlayerClient(cl)) {
					returnCode = 7;
					cl.saveFile = false;
				} else {
					cl.saveFile = true;
				}
			}
		}
		if(returnCode == 2) {
			cl.saveCharacter = true;
			cl.packetType = -1;
			cl.packetSize = 0;
			final PacketBuilder bldr = new PacketBuilder();
			bldr.put((byte) 2);
			if (cl.getRights().equal(Rights.DEVELOPER)) {
				bldr.put((byte) 2);
			} else {
				bldr.put((byte) cl.getRights().getProtocolValue());
			}
			bldr.put((byte) 0);
			channel.write(bldr.toPacket());
		} else {
			System.out.println("returncode:" + returnCode);
			sendReturnCode(channel, returnCode);
			return null;
		}
		synchronized (PlayerHandler.lock) {
			cl.initialize();
			cl.initialized = true;
		}
		return cl;
	}

	public static void sendReturnCode(final Channel channel, final int code) {
		channel.write(new PacketBuilder().put((byte) code).toPacket()).addListener(new ChannelFutureListener() {
			@Override
			public void operationComplete(final ChannelFuture arg0) throws Exception {
				arg0.getChannel().close();
			}
		});
	}

}
