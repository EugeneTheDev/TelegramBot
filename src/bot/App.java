package bot;

import bot.tasks.Translator;
import bot.utilites.Util;
import org.apache.http.HttpHost;
import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.TelegramBotsApi;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.exceptions.TelegramApiRequestException;
import org.telegram.telegrambots.logging.BotLogger;

import java.io.IOException;
import java.util.Properties;

/**
 * Main app class.
 */
public class App {

    /**
     * Tasks for bot.
     */
    private Translator translator;

    private App() {
        initAndStart();
    }

    public static void main(String[] args) {
        new App();
    }

    /**
     * Init processes.
     */
    private void initAndStart(){
        Util.disableWarning();

        try {

            Properties properties=new Properties();
            properties.load(App.class.getClassLoader().getResourceAsStream("config.properties"));

            ApiContextInitializer.init();
            TelegramBotsApi api=new TelegramBotsApi();
            DefaultBotOptions options=new DefaultBotOptions();
            options.setHttpProxy(new HttpHost(properties.getProperty("proxy-hostname"),
                    Integer.valueOf(properties.getProperty("proxy-port"))));
            Bot bot=new Bot(properties.getProperty("bot-token"),properties.getProperty("bot-username"),
                    options,Integer.valueOf(properties.getProperty("creator-id")),this);
            api.registerBot(bot);

            translator =new Translator();

            Runtime.getRuntime().addShutdownHook(new Thread(()-> {
                System.out.println("Bot has been shutdowned.");
                bot.shutdownInform();
            }));

            bot.startInform();
            System.out.println("Bot has been started.");

        } catch (TelegramApiRequestException e) {
            BotLogger.error("Error on setup.", e);
        } catch (IOException e) {
            BotLogger.error("Can`t load properties",e);
        }

    }


    /**
     * Tasks.
     */
    public String translate(String text){
        return translator.translate(text);
    }


    public void shutdown(){
        System.exit(0);
    }
}
