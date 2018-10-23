package bot;

import bot.db.Db;
import bot.messages.MessageFormatter;
import bot.tasks.ClearTimer;
import bot.tasks.ShipperGame;
import bot.tasks.Translator;
import bot.utilites.Util;
import com.mongodb.*;
import org.bson.Document;
import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.TelegramBotsApi;
import org.telegram.telegrambots.api.objects.User;
import org.telegram.telegrambots.exceptions.TelegramApiRequestException;
import org.telegram.telegrambots.logging.BotLogger;

import java.io.IOException;
import java.util.*;

/**
 * Main app class.
 */
public class App {

    /**
     * Database.
     */
    private Db db;


    /**
     * Tasks for bot.
     */
    private Translator translator;
    private ShipperGame shipperGame;

    /**
     * Format message
     */
    private MessageFormatter messageFormatter;


    public static void main(String[] args) {
        new App().initAndStart();
    }

    /**
     * Init processes.
     */
    private void initAndStart(){
        try {
            Util.disableWarning();
            Properties properties=new Properties();
            properties.load(App.class.getClassLoader().getResourceAsStream("config.properties"));
            translator = new Translator();
            shipperGame = new ShipperGame();
            messageFormatter = new MessageFormatter();
            new ClearTimer(this);
            initDB(properties);
            initBot(properties);
        } catch (IOException e) {
            BotLogger.error("Error on setup", e);
        }

    }

    private void initDB(Properties properties){
        MongoClientURI uri  = new MongoClientURI(new Formatter()
                .format("mongodb://%s:%s@%s:%s/%s",properties.getProperty("db-user"),
                properties.getProperty("db-password"), properties.getProperty("db-host"),
                properties.getProperty("db-port"), properties.getProperty("db-name")).toString());
        MongoClient client = new MongoClient(uri);
        db = new Db(client.getDatabase(uri.getDatabase()).getCollection(properties.getProperty("db-users")),
        client.getDatabase(uri.getDatabase()).getCollection(properties.getProperty("db-chats")));
    }

    private void initBot(Properties properties){
        try {

            ApiContextInitializer.init();
            TelegramBotsApi api=new TelegramBotsApi();
            Bot bot=new Bot(properties.getProperty("bot-token"),properties.getProperty("bot-username"),
                    Integer.valueOf(properties.getProperty("creator-id")),this);
            api.registerBot(bot);

            Runtime.getRuntime().addShutdownHook(new Thread(()-> {
                bot.shutdownInform();
                System.out.println("Bot has been shutdowned.");
            }));

            bot.startInform();
            System.out.println("Bot has been started.");

        } catch (TelegramApiRequestException e) {
            BotLogger.error("Error on setup", e);
        }
    }


    /**
     * Tasks, abilities and actions.
     */
    public String translate(String text){
        return translator.translate(text);
    }

    public String sayHello(){
        return messageFormatter.sayHello();
    }

    public String playShipperGame(long chatId){
        Document chat = db.chatsDocument(chatId);
        String result;
        try {
            String couple = shipperGame.playShipperGame(db.nextDate(chat),
                    db.membersList(chat));
            result = messageFormatter.succesfulShippering(couple);
            db.updateChatAfterPlaying(chatId, couple);
        } catch (ShipperGame.TooLessMembersException e) {
            result = messageFormatter.tooLessForShippering();
        } catch (ShipperGame.NotFirstGameException e){
            result = messageFormatter.notFirstShipperingGame(db.chatsCouple(chat));
        }
        return result;
    }
    public void clearInteractionsStats() {
        db.clearInteractionsStats();
    }
    public String chatsCouple(long chatId){
        String couple = db.chatsCouple(chatId);
        if (!couple.isEmpty()) return messageFormatter.lastCouple(couple);
        return messageFormatter.noLastCouple();
    }

    public void newMessageInChat(long chatId, User from) {
        db.newMessageInChat(chatId, from);
    }

    public void newInteraction(User user) {
        db.newInteraction(user);
    }

    public void removeFromChat(long chatId, int userId){
        db.removeFromChat(chatId, userId);
    }

    public String interactionsStats() {
        return db.interactionsStats();
    }

    public void shutdown(){
        System.exit(0);
    }
}
