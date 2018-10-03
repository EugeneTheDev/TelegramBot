package bot;

import bot.tasks.ClearTimer;
import bot.tasks.Translator;
import bot.utilites.Util;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.*;
import com.mongodb.client.MongoCollection;
import org.bson.Document;
import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.TelegramBotsApi;
import org.telegram.telegrambots.api.objects.User;
import org.telegram.telegrambots.exceptions.TelegramApiRequestException;
import org.telegram.telegrambots.logging.BotLogger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;
import java.util.Properties;

/**
 * Main app class.
 */
public class App {

    /**
     * Database.
     */
    private MongoCollection<Document> collection;

    /**
     * Parser for mapping JSON from DB to UserDBO class
     */
    private ObjectMapper mapper;

    /**
     * Tasks for bot.
     */
    private Translator translator;


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
            translator =new Translator();
            mapper = new ObjectMapper();
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
        collection = client.getDatabase(uri.getDatabase()).getCollection("users");
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
     * Tasks.
     */
    public String translate(String text){
        return translator.translate(text);
    }

    /**
     * Interacting with DB.
     */
    public void newInteraction(User newUser){
       boolean isFound=false;
       Document findQuery = new Document("name", "totalInteractions"),
               interactions = collection.find(findQuery).iterator().next();

       for(Document doc: (List<Document>)interactions.get("users")){

           try {
               User user = mapper.readValue(doc.toJson(), User.class);
               if(newUser.getId().equals(user.getId())){
                   isFound=true;
                   break;
               }
           } catch (IOException e) {
               BotLogger.error("Error when deserialize JSON", e);
           }

       }


       if(!isFound){

           try {
               collection.updateOne(findQuery, new Document("$push",
                       new Document("users", Document.parse(mapper.writeValueAsString(newUser)))));
               collection.updateOne(findQuery, new Document("$set",
                       new Document("count", interactions.getInteger("count")+1)));

           } catch (JsonProcessingException e) {
               BotLogger.error("Error when serialize JSON", e);
           }

       }

    }

    public String interactionsStats(){
        StringBuffer buffer = new StringBuffer();
        Document interactions = collection.find(new Document("name", "totalInteractions")).iterator().next();
        buffer.append("<strong>Count of interactions:</strong> ")
                .append(interactions.getInteger("count"))
                .append("\n")
                .append("<strong>Users:</strong> ")
                .append("\n");

        for(Document doc: (List<Document>)interactions.get("users")){

            try {
                User user = mapper.readValue(doc.toJson(),User.class);
                buffer.append(user.getFirstName());
                if (user.getLastName()!=null) buffer.append(" ").append(user.getLastName());
                if (user.getUserName()!=null) buffer.append(" @").append(user.getUserName());
                buffer.append("\n");
            } catch (IOException e) {
                BotLogger.error("Error when deserialize JSON", e);
            }

        }

        return buffer.toString();
    }

    public void clearDB(){
        Document findQuery = new Document("name", "totalInteractions");
        collection.updateOne(findQuery, new Document("$set",
                new Document("count", 0).append("users", new ArrayList())));

    }


    public void shutdown(){
        System.exit(0);
    }
}
