package bot.db;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.*;
import org.bson.Document;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.logging.BotLogger;

import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Updates.*;

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
            ArrayList<WriteModel<Document>> requests = new ArrayList<>();
            String lastName = user.getLastName(),
                    username = user.getUserName(), languageCode = user.getLanguageCode();

            try {

                requests.add(
                        new UpdateOneModel<>(
                            and(
                                    eq("name", "totalInteractions"),
                                    eq("users.id", user.getId()),
                                    or(
                                            ne("users.first_name", user.getFirstName()),
                                            lastName==null?
                                                    exists("users.last_name"):
                                                    ne("users.last_name", user.getLastName()),
                                            username==null?
                                                    exists("users.username"):
                                                    ne("users.username", user.getUserName()),
                                            languageCode==null?
                                                    exists("users.language_code"):
                                                    ne("users.language_code", languageCode)
                                    )
                            ),
                            set("users.$", Document.parse(mapper.writeValueAsString(user)))
                        )
                );

                requests.add(
                        new UpdateOneModel<>(
                                and(
                                        eq("name", "totalInteractions"),
                                        ne("users.id", user.getId())
                                ),
                                combine(
                                        addToSet("users", Document.parse(mapper.writeValueAsString(user))),
                                        inc("count", 1)
                                )
                        )
                );

            } catch (JsonProcessingException e) {
                BotLogger.error("Error when serializing JSON", e);
            }

            usersCollection.bulkWrite(requests);
        }
    }

    public void newMessageInChat(long chatId, User user){
        synchronized (chatsCollection){
            ArrayList<WriteModel<Document>> requests = new ArrayList<>();
            String lastName = user.getLastName(),
                    username = user.getUserName(), languageCode = user.getLanguageCode();

            try {

                requests.add(
                        new UpdateOneModel<>(
                                and(
                                        eq("chatId", chatId)
                                ),
                                combine(
                                        setOnInsert("chatId", chatId),
                                        setOnInsert("couple", ""),
                                        setOnInsert("nextDate", new Date().getTime()),
                                        setOnInsert("members", Collections.EMPTY_LIST)
                                ),
                                new UpdateOptions().upsert(true)
                        )
                );

                requests.add(
                        new UpdateOneModel<>(
                                and(
                                        eq("chatId", chatId),
                                        eq("members.id", user.getId()),
                                        or(
                                                ne("members.first_name", user.getFirstName()),
                                                lastName==null?
                                                        exists("members.last_name"):
                                                        ne("members.last_name", user.getLastName()),
                                                username==null?
                                                        exists("members.username"):
                                                        ne("members.username", user.getUserName()),
                                                languageCode==null?
                                                        exists("members.language_code"):
                                                        ne("members.language_code", languageCode)
                                        )

                                ),
                                set("members.$", Document.parse(mapper.writeValueAsString(user)))
                        )
                );

                requests.add(
                        new UpdateOneModel<>(
                                eq("chatId", chatId),
                                addToSet("members", Document.parse(mapper.writeValueAsString(user)))
                        )
                );

            } catch (JsonProcessingException e) {
                BotLogger.error("Error when serializing JSON", e);
            }

            chatsCollection.bulkWrite(requests);
        }
    }

    public void removeFromChat(long chadId, int userId){
        chatsCollection.updateOne(
                eq("chatId", chadId),
                pull("members", eq("id", userId))
        );
    }

    public String interactionsStats(){
        StringBuffer buffer = new StringBuffer();
        Document interactions = usersCollection.find(eq("name", "totalInteractions")).iterator().next();
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
            usersCollection.updateOne(
                    eq("name", "totalInteractions"),
                    combine(
                            set("count", 0),
                            set("users", Collections.EMPTY_LIST)
                    )
            );
        }
    }

    public void updateChatAfterPlaying(long chatId, String couple){
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        calendar.add(Calendar.HOUR, 12);
        chatsCollection.updateOne(
                eq("chatId", chatId),
                combine(
                        set("nextDate", calendar.getTimeInMillis()),
                        set("couple", couple)
                )
        );
    }

    public Document chatsDocument(long chatId){
        return chatsCollection.find(eq("chatId",chatId)).first();
    }

    public String chatsCouple(long chatId){
        return chatsCouple(chatsCollection.find(eq("chatId", chatId)).first());
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
