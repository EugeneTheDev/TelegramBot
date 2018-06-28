package bot;


import org.telegram.abilitybots.api.bot.AbilityBot;
import org.telegram.abilitybots.api.objects.Ability;
import org.telegram.abilitybots.api.objects.Reply;
import org.telegram.telegrambots.api.methods.AnswerInlineQuery;
import org.telegram.telegrambots.api.objects.inlinequery.InlineQuery;
import org.telegram.telegrambots.api.objects.inlinequery.inputmessagecontent.InputTextMessageContent;
import org.telegram.telegrambots.api.objects.inlinequery.result.InlineQueryResult;
import org.telegram.telegrambots.api.objects.inlinequery.result.InlineQueryResultArticle;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.exceptions.TelegramApiException;


import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.telegram.abilitybots.api.objects.Locality.USER;
import static org.telegram.abilitybots.api.objects.Privacy.ADMIN;
import static org.telegram.abilitybots.api.objects.Privacy.PUBLIC;

/**
 * Bot class for reacting to events.
 */
public class Bot extends AbilityBot {
    private App app;

    /**
     * Threads for quick answers to inline queries.
     */
    private ExecutorService service;

    private Set<Integer> admins;
    private int creatorId;

    public Bot(String botToken, String botUsername, DefaultBotOptions options, int creatorId, App app) {
        super(botToken, botUsername, options);
        this.creatorId=creatorId;
        this.app=app;
        admins=new HashSet<>(admins());
        service=Executors.newFixedThreadPool(3);
    }

    @Override
    public int creatorId() {
        return creatorId;
    }


    /**
     * Abilities.
     */
    public Ability sayHello(){
        return Ability.builder()
                .name("hello")
                .info("saying hello to user")
                .locality(USER)
                .privacy(PUBLIC)
                .input(0)
                .action(ctx->silent.send("Hello",ctx.chatId()))
                .build();

    }

    public Ability shutdownBot(){
        return Ability.builder()
                .name("shutdown")
                .info("shutdown bot")
                .locality(USER)
                .privacy(ADMIN)
                .input(0)
                .action(ctx->app.shutdown())
                .build();
    }


    /**
     * Handle inline query and answer.
     */
    public Reply handleInlineQuery(){
        return Reply.of(action-> service.submit(()->{
            InlineQuery query=action.getInlineQuery();
                String translate=app.translate(query.getQuery());
                InlineQueryResult result;
                if (!translate.isEmpty())
                    result=new InlineQueryResultArticle().setTitle("Translate to English")
                            .setDescription(translate)
                            .setInputMessageContent(new InputTextMessageContent().setMessageText(translate))
                            .setId(UUID.randomUUID().toString());
                else result=new InlineQueryResultArticle().setTitle("Ooops..")
                        .setDescription("I can`t translate this.")
                        .setInputMessageContent(new InputTextMessageContent().setMessageText("<Nothing>"))
                        .setId(UUID.randomUUID().toString());
                try {
                    sender.execute(new AnswerInlineQuery().setResults(result)
                            .setInlineQueryId(query.getId())
                            .setCacheTime(30000));
                } catch (TelegramApiException e) {
                    //ignore
                }})
                ,update -> update.hasInlineQuery()&&update.getInlineQuery().hasQuery());
    }


    /**
     * Send message to all admins on start.
     */
    public void startInform(){
        admins.forEach(admin->silent.send("Bot has been started.",admin));
    }

    /**
     * Send message to all admins on shutdown.
     */
    public void shutdownInform(){
        admins.forEach(admin->silent.send("Bot has been shutdowned.",admin));
    }



}
