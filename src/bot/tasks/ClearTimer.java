package bot.tasks;

import bot.App;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Timer for clearing information about interactions with bot
 * in database every day.
 */
public class ClearTimer extends TimerTask {
    private App app;

    public ClearTimer(App app) {
        this.app = app;
        new Timer(true).schedule(this, 24*60*60*1000);
    }

    @Override
    public void run() {
        app.clearInteractionsStats();
    }
}
