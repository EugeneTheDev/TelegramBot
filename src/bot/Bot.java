package bot;


import org.telegram.abilitybots.api.bot.AbilityBot;
import org.telegram.abilitybots.api.objects.Ability;
import org.telegram.abilitybots.api.objects.Privacy;
import org.telegram.abilitybots.api.objects.Reply;
import org.telegram.telegrambots.api.methods.AnswerInlineQuery;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.objects.Message;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.api.objects.User;
import org.telegram.telegrambots.api.objects.inlinequery.InlineQuery;
import org.telegram.telegrambots.api.objects.inlinequery.inputmessagecontent.InputTextMessageContent;
import org.telegram.telegrambots.api.objects.inlinequery.result.InlineQueryResult;
import org.telegram.telegrambots.api.objects.inlinequery.result.InlineQueryResultArticle;

import org.telegram.telegrambots.exceptions.TelegramApiException;


import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.telegram.abilitybots.api.objects.Locality.ALL;
import static org.telegram.abilitybots.api.objects.Locality.GROUP;
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
        checkNewMessageInChat(update);
        super.onUpdateReceived(update);

    }

    private void checkNewMessageInChat(Update update){
        service.submit(()->{
            if(update.hasMessage()){
                Message message = update.getMessage();
                if (message.isGroupMessage() || message.isSuperGroupMessage())
                    app.newMessageInChat(message.getChatId(), message.getFrom());

                if (message.getNewChatMembers()!=null && !message.getNewChatMembers().isEmpty())
                    message.getNewChatMembers().forEach(e->app.newMessageInChat(message.getChatId(), e));

                if (message.getLeftChatMember()!=null) app.removeFromChat(message.getChatId(),
                        message.getLeftChatMember().getId());

            }
        });
    }

    private void newInteraction(User user){
        service.submit(()->app.newInteraction(user));
    }

    /**
     * Abilities.
     */
    public Ability sayHello(){
        return Ability.builder()
                .name("hello")
                .info("say hello")
                .locality(ALL)
                .privacy(PUBLIC)
                .input(0)
                .action(ctx->service.submit(()->{
                    silent.send(app.sayHello(),ctx.chatId());
                    newInteraction(ctx.update().getMessage().getFrom());
                }))
                .build();
    }

    public Ability playShipperGame(){
        return Ability.builder()
                .name("shipper")
                .info("play shipper game")
                .locality(GROUP)
                .privacy(PUBLIC)
                .input(0)
                .action(ctx->service.submit(()->{
                    silent.send(app.playShipperGame(ctx.chatId()),ctx.chatId());
                    newInteraction(ctx.update().getMessage().getFrom());
                }))
                .build();
    }

    public Ability showCouple(){
        return Ability.builder()
                .name("couple")
                .info("show last couple")
                .locality(GROUP)
                .privacy(PUBLIC)
                .input(0)
                .action(ctx->service.submit(()->{
                    silent.send(app.chatsCouple(ctx.chatId()),ctx.chatId());
                    newInteraction(ctx.update().getMessage().getFrom());
                }))
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
                }
                newInteraction(action.getInlineQuery().getFrom());
        }),update -> update.hasInlineQuery()&&update.getInlineQuery().hasQuery());
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
