package bot.tasks;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.json.JSONArray;
import org.json.JSONException;
import org.telegram.telegrambots.logging.BotLogger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Translator class.
 */
public class Translator {
    private final static String GOOGLE_TRANSLATE="https://translate.googleapis.com/translate_a/single?client=gtx&sl=auto&tl=en&dt=t&q=@";

    public String translate(String text){
        String result="";
        try {

            HttpClient client=HttpClientBuilder.create().build();
            HttpGet request=new HttpGet(GOOGLE_TRANSLATE.
                    replace("@",URLEncoder.encode("\""+text+"\"", StandardCharsets.UTF_8)));
            request.addHeader("User-Agent", "Mozilla/5.0");
            HttpResponse response=client.execute(request);
            BufferedReader reader=new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
            StringBuffer buffer=new StringBuffer();
            String line;
            while ((line=reader.readLine())!=null) buffer.append(line);
            reader.close();
            result=new JSONArray(buffer.toString()).getJSONArray(0).getJSONArray(0).getString(0).replaceAll("\\\"","");
        } catch (IOException e) {
            BotLogger.error("Exception when translating.",e);
        } catch (JSONException e){
           //ignore
        } finally {
            return result;
        }

    }
}
