package bot.messages;

import java.util.Random;

/**
 * Creating message body according to some data.
 */
public class MessageFormatter {

    private Random random;

    private final String[] shippersPhrases = {
            "Как Тристан и Изольда,\nКак Ромео и Джульетта,\nКак Сид и Нэнси будут любить друг друга ",
            "Так-так, кто же сегодня сладкая парочка...\nТак вот же она: ",
            "Купидон уже приготовил свой лук,\nПрицелился,\nИ пара дня сегодня "
    };

    public MessageFormatter() {
        random = new Random();
    }

    public String sayHello(){
        return "Hello";
    }

    public String succesfulShippering(String couple){
        return shippersPhrases[random.nextInt(shippersPhrases.length)]+
                couple + " = \u2764";// \u2764 is heart emoji
    }

    public String lastCouple(String couple){
        return "Последняя пара: " + couple.replaceAll("@", "") + " = \u2764";// \u2764 is heart emoji
    }

    public String noLastCouple(){
        return "Нет последней пары. Сначала сыграйте хотя бы один раз.";
    }

    public String tooLessForShippering(){
        return "Слишком мало участников. Сначала напишите что-нибудь в чат";
    }

    public String notFirstShipperingGame(String couple){
        return "Пара дня уже была определена: " + couple.replaceAll("@", "") +
                " = \u2764";// \u2764 is heart emoji;;
    }
}
