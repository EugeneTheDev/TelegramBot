package bot.tasks;

import bot.App;

import java.util.Calendar;
import java.util.GregorianCalendar;
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
        Calendar date = Calendar.getInstance();
        date.set(Calendar.HOUR_OF_DAY, 0);
        date.set(Calendar.MINUTE, 0);
        date.set(Calendar.SECOND, 0);
        date.set(Calendar.MILLISECOND, 0);
        date.add(Calendar.DAY_OF_MONTH, 1);
        new Timer(true).schedule(this, date.getTime(), 24*60*60*1000);
    }

    @Override
    public void run() {
        app.clearInteractionsStats();
    }
}
