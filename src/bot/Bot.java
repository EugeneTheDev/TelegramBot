package bot;


import org.telegram.abilitybots.api.bot.AbilityBot;
import org.telegram.abilitybots.api.objects.Ability;
import org.telegram.abilitybots.api.objects.Reply;
import org.telegram.telegrambots.api.methods.AnswerInlineQuery;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.api.objects.inlinequery.InlineQuery;
import org.telegram.telegrambots.api.objects.inlinequery.inputmessagecontent.InputTextMessageContent;
import org.telegram.telegrambots.api.objects.inlinequery.result.InlineQueryResult;
import org.telegram.telegrambots.api.objects.inlinequery.result.InlineQueryResultArticle;

import org.telegram.telegrambots.exceptions.TelegramApiException;


import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.telegram.abilitybots.api.objects.Locality.USER;
import static org.telegram.abilitybots.api.objects.Privacy.CREATOR;
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
    private int creatorId;

    public Bot(String botToken, String botUsername, int creatorId, App app) {
        super(botToken, botUsername);
        this.creatorId=creatorId;
        this.app=app;
        service=Executors.newFixedThreadPool(4);
    }

    @Override
    public int creatorId() {
        return creatorId;
    }

    @Override
    public void onUpdateReceived(Update update) {
        super.onUpdateReceived(update);
        service.submit(()->{
            if(update.hasInlineQuery()) app.newInteraction(update.getInlineQuery().getFrom());
            else if(update.hasMessage()) app.newInteraction(update.getMessage().getFrom());
        });

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
                .action(ctx->service.submit(()->silent.send("Hello",ctx.chatId())))
                .build();


    }

    public Ability shutdownBot(){
        return Ability.builder()
                .name("shutdown")
                .locality(USER)
                .privacy(CREATOR)
                .input(0)
                .action(ctx->service.submit(()->app.shutdown()))
                .build();
    }

    public Ability sendStats(){
        return Ability.builder()
                .name("stats")
                .locality(USER)
                .privacy(CREATOR)
                .input(0)
                .action(ctx->service.submit(()->silent.execute(new SendMessage(
                        ctx.chatId(),app.interactionsStats()).enableHtml(true))))
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
        silent.send("Bot has been started.",creatorId);
    }

    /**
     * Send message to all admins on shutdown.
     */
    public void shutdownInform(){
        silent.send("Bot has been shutdowned.",creatorId);
    }



}
