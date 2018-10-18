package bot.db;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.MongoCollection;
import org.bson.Document;
import org.telegram.telegrambots.api.objects.User;
import org.telegram.telegrambots.logging.BotLogger;

import java.io.IOException;
import java.util.*;

public class Db {

    /**
     * Database.
     */
    private MongoCollection<Document> usersCollection, chatsCollection;

    /**
     * Parser for mapping JSON from DB to UserDBO class
     */
    private ObjectMapper mapper;


    public Db(MongoCollection<Document> usersCollection, MongoCollection<Document> chatsCollection) {
        this.usersCollection = usersCollection;
        this.chatsCollection = chatsCollection;
        mapper = new ObjectMapper();
    }

    public void newInteraction(User user){
        synchronized (usersCollection) {
            boolean isFound = false;
            Document findQuery = new Document("name", "totalInteractions"),
                    interactions = usersCollection.find(findQuery).first();

            for(Document doc: (List<Document>)interactions.get("users")){
                try {
                    if(user.getId().equals(mapper.readValue(doc.toJson(), User.class).getId())){
                        isFound=true;
                        break;
                    }
                } catch (IOException e) {
                    BotLogger.error("Error when deserializing JSON", e);
                }
            }


            if(!isFound){
                try {
                    usersCollection.updateOne(findQuery, new Document("$push",
                                    new Document("users", Document.parse(mapper.writeValueAsString(user))))
                                    .append("$set",
                                    new Document("count", interactions.getInteger("count")+1)));
                } catch (JsonProcessingException e) {
                    BotLogger.error("Error when serializing JSON", e);
                }
            }
        }

    }

    public void newMessageInChat(long chatId, User user){
        synchronized (chatsCollection){
            Document chat = chatsDocument(chatId);
            boolean isFound = false;

            if (chat==null)
                chatsCollection.insertOne(new Document("chatId", chatId).append("couple", "")
                        .append("nextDate", new Date().getTime())
                        .append("members", Collections.EMPTY_LIST));
            else {

                ArrayList<Document> members = (ArrayList<Document>) chat.get("members");

                for (Document doc : members) {
                    try {
                        if (user.getId().equals(mapper.readValue(doc.toJson(), User.class).getId())) {
                            isFound = true;
                            break;
                        }
                    } catch (IOException e) {
                        BotLogger.error("Error when deserializing JSON", e);
                    }
                }
            }

            if (!isFound){
                try {
                    chatsCollection.updateOne(new Document("chatId", chatId),new Document("$push",
                            new Document("members", Document.parse(mapper.writeValueAsString(user)))));
                } catch (JsonProcessingException e) {
                    BotLogger.error("Error when serializing JSON", e);
                }
            }

        }
    }

    public String interactionsStats(){
        StringBuffer buffer = new StringBuffer();
        Document interactions = usersCollection.find(new Document("name", "totalInteractions")).iterator().next();
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

    public void clearInteractionsStats(){
        synchronized (usersCollection) {
            Document findQuery = new Document("name", "totalInteractions");
            usersCollection.updateOne(findQuery, new Document("$set",
                    new Document("count", 0).append("users", new ArrayList())));
        }
    }

    public void updateChatAfterPlaying(long chatId, String couple){
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        calendar.add(Calendar.HOUR, 12);
        chatsCollection.updateOne(new Document("chatId", chatId),
                new Document("$set",new Document("nextDate", calendar.getTimeInMillis())
                        .append("members", Collections.EMPTY_LIST)
                        .append("couple", couple)));

    }

    public Document chatsDocument(long chatId){
        return chatsCollection.find(new Document("chatId",chatId)).first();
    }

    public String chatsCouple(long chatId){
        return chatsCouple(chatsCollection.find(new Document("chatId", chatId)).first());
    }

    public ArrayList<User> membersList(Document doc){
        ArrayList<Document> docs = (ArrayList<Document>) doc.get("members");
        ArrayList<User> users = new ArrayList<>();
        docs.forEach(el->{
            try {
                users.add(mapper.readValue(el.toJson(), User.class));
            } catch (IOException e) {
                BotLogger.error("Error when deserialize JSON", e);
            }
        });
        return users;
    }

    public long nextDate(Document doc){
        return doc.getLong("nextDate");
    }

    public String chatsCouple(Document doc){
        return doc.getString("couple");
    }
}
