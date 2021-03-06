package core.game.util.json;

import java.io.File;
import java.io.FileReader;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.istack.internal.logging.Logger;

import core.Configuration;
import core.game.sound.MusicManager;
import core.game.sound.region.Music;

/**
 * A class which loads all music into the game.
 * @author 7Winds
 */
public class MusicLoader {
	
	public static final Logger logger = Logger.getLogger(MusicLoader.class);
	
	/**
	 * Loads the music data from a .JSON file.
	 * 
	 * @throws Exception
	 *            If any exception happens.
	 */
	public static void load() throws Exception {
		logger.info("Loading music...");
		
		JsonParser parser = new JsonParser();
        JsonArray array = (JsonArray) parser.parse(new FileReader(new File(Configuration.DATA_DIR + "json/music.json")));
        MusicManager.music = new Music[array.size()];
		int count = 0;
		
		for (int i = 0; i < array.size(); i++) {
			JsonObject reader = (JsonObject) array.get(i);

			int region = -1;
			if (reader.has("region"))
				region = reader.get("region").getAsInt();
			String name = reader.get("name").getAsString();
			int song = reader.get("song").getAsInt();
			int frame = reader.get("frame").getAsInt();
			int button = reader.get("button").getAsInt();
			
			MusicManager.music[i] = new Music(region, name, song, frame, button);
			count++;
		}		
		logger.info("Loaded: "+ count +" songs.");	
	}	
}
