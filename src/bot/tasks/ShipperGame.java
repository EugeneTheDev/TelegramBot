package bot.tasks;


import org.telegram.telegrambots.meta.api.objects.User;

import java.util.ArrayList;
import java.util.Date;
import java.util.Random;


/**
 * Random choose pair from active users
 */
public class ShipperGame {
    private Random random;

    public ShipperGame() {
        random = new Random();
    }

    public String playShipperGame(long nextDate, ArrayList<User> members){
        if (new Date().getTime()<=nextDate) throw new NotFirstGameException();
        if (members.size()<3) throw new TooLessMembersException();
        else {

            User firstUser, secondUser;
            do {
                firstUser = members.get(random.nextInt(members.size()));
                secondUser = members.get(random.nextInt(members.size()));
            } while (firstUser.getBot() || secondUser.getBot() || firstUser.getId().equals(secondUser.getId()));

            StringBuffer buffer = new StringBuffer();
            if (firstUser.getUserName()!=null) buffer.append("@").append(firstUser.getUserName());
            else buffer.append(firstUser.getFirstName());
            buffer.append(" + ");
            if (secondUser.getUserName()!=null) buffer.append("@").append(secondUser.getUserName());
            else buffer.append(secondUser.getFirstName());

            return buffer.toString();
        }
    }

    public class TooLessMembersException extends IllegalArgumentException{

    }

    public class NotFirstGameException extends IllegalArgumentException{

    }
}
