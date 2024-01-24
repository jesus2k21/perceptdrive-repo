package com.sha.perceptdrive.services;

import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class SMSBodyProcessorService {

    private static final Pattern LOCATION_PATT = Pattern.compile("\\bsave\\b\\+(.*?)\\+\\bas\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern NICKNAME_PATT = Pattern.compile("\\bas\\+([a-zA-Z\\d]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern ORIGIN_NICKNAME_PATT = Pattern.compile("\\bfrom\\b\\+(.*?)\\+\\bto\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern DESTINATION_NICKNAME_PATT = Pattern.compile("\\bto\\b\\+(.*?)\\+\\bon\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern DAYS_PATT = Pattern.compile("\\bon\\b\\+(.*?)\\+\\bat\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern TIME_PATT = Pattern.compile("(?<=\\bat\\+\\b).*", Pattern.CASE_INSENSITIVE);
    private static final Pattern CHANGE_NICKNAME_PATT = Pattern.compile("\\bchange\\b\\+(.*?)\\+\\baddress\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern ADDRESS_PATT = Pattern.compile("(?<=\\bto\\+\\b).*", Pattern.CASE_INSENSITIVE);
    private static final Pattern RENAME_NICKNAME_PATT = Pattern.compile("\\brename\\b\\+(.*?)\\+\\blocation\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern SECOND_WORD_PATT = Pattern.compile("\\bdelete\\+([a-zA-Z\\d]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern DESTINATION_NICKNAME_AT_PATT = Pattern.compile("\\bto\\b\\+(.*?)\\+\\bat\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern DESTINATION_NICKNAME_PATT_BASIC_REQUEST = Pattern.compile("\\bto\\b\\+(.*?)\\+\\bfrom\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern ORIGIN_NICKNAME_PATT_BASIC_REQUEST = Pattern.compile("(?<=\\bfrom\\+\\b).*", Pattern.CASE_INSENSITIVE);
    private static final Pattern NUM_DAYS_PAUSE_PATT = Pattern.compile("\\d+", Pattern.CASE_INSENSITIVE);
    private static final Pattern FROM_NUMBER_PATT = Pattern.compile("(?<=From=%2B)(\\d+)(?=&)", Pattern.CASE_INSENSITIVE);


    public String[] formatOriDest(String msgBody) {
        // "im going to DEST_ADDRESS from HOME_ADDRESS"
        // msgBody = Im going to 301 N Greenville Ave Allen TX from 3409 N Central Expressway Plano TX
        // + characters are added automatically
        String origin;
        String dest;

        Matcher originMatch = ORIGIN_NICKNAME_PATT_BASIC_REQUEST.matcher(msgBody);
        Matcher destMatch = DESTINATION_NICKNAME_PATT_BASIC_REQUEST.matcher(msgBody);

        if (destMatch.find() && originMatch.find()) {
            origin = originMatch.group().trim();
            dest = destMatch.group(1).trim();
            return new String[] {origin, dest};
        }
        return null;
    }

    public String[] formatSaveDest(final String msgBody) {
        // msgBody will be - {Save} {location} as {nickname}
        // we'll return the {location} and {nickname}
        String location;
        String nickname;

        Matcher locationMatch = LOCATION_PATT.matcher(msgBody);
        Matcher nickNameMatch = NICKNAME_PATT.matcher(msgBody);

        if (locationMatch.find() && nickNameMatch.find()) {
            location = locationMatch.group(1).trim();
            nickname = nickNameMatch.group(1).trim();
            return new String[] {location, nickname};
        }
        return null;
    }

    public String[] formatSchedule(final String msgBody) {
        // {Schedule} updates from {nicknameOrigin} to {nicknameDest} on {days} at {time}
        String nickNameOrigin;
        String nickNameDest;
        String days;
        String time;

        Matcher matchOrigin = ORIGIN_NICKNAME_PATT.matcher(msgBody);
        Matcher matchDest = DESTINATION_NICKNAME_PATT.matcher(msgBody);
        Matcher matchDays = DAYS_PATT.matcher(msgBody);
        Matcher matchTime = TIME_PATT.matcher(msgBody);
        if (matchOrigin.find() && matchDest.find() && matchDays.find() && matchTime.find()) {
            nickNameOrigin = matchOrigin.group(1).trim();
            nickNameDest = matchDest.group(1).trim();
            days = matchDays.group(1).trim();
            time = matchTime.group().trim().replaceAll("%3A", ":");
            return new String[] {nickNameOrigin, nickNameDest, days, time};
        }
        return null;
    }

    public String[] formatChangeSavedDest(final String msgBody) {
        // {Change} {school} address to 2423 Blinn Blvd, Bryan, TX
        String nickname;
        String newAddress;

        Matcher nicknameMatch = CHANGE_NICKNAME_PATT.matcher(msgBody);
        Matcher addressMatch = ADDRESS_PATT.matcher(msgBody);

        if (nicknameMatch.find() && addressMatch.find()) {
            nickname = nicknameMatch.group(1).trim();
            newAddress = addressMatch.group(0).trim();
            return new String[] {nickname, newAddress};
        }
        return null;
    }

    public String[] formatRenameSavedDest(final String msgBody) {
        // {Change} {school} address to 2423 Blinn Blvd, Bryan, TX
        String nickname;
        String newNickname;

        Matcher renameNickPatt = RENAME_NICKNAME_PATT.matcher(msgBody);
        Matcher addressMatch = ADDRESS_PATT.matcher(msgBody);

        if (renameNickPatt.find() && addressMatch.find()) {
            nickname = renameNickPatt.group(1).trim();
            newNickname = addressMatch.group(0).trim();
            return new String[] {nickname, newNickname};
        }
        return null;
    }

    public String[] formatDeleteSavedDestSchedule(final String msgBody) {
        // Delete either their saved location (by name) or a schedule (by time)
        // Delete {school} - to delete their saved location
        if (!msgBody.toLowerCase().contains("schedule")) {
            // We'll assume they're trying to delete a saved location
            // We'll get the word immediately after delete
            Matcher nickNameMatch = SECOND_WORD_PATT.matcher(msgBody);
            if (nickNameMatch.find()) {
                String locationNickName = nickNameMatch.group(1);
                return new String[] {locationNickName};
            } else {
                return null;
            }
        }
        // We'll assume they're trying to delete a schedule
        // Delete {schedule} from {apartment} to {work} at {8:45 am}
        String nickNameOrigin;
        String nickNameDest;
        String time;

        Matcher matchOrigin = ORIGIN_NICKNAME_PATT.matcher(msgBody);
        Matcher matchDest = DESTINATION_NICKNAME_AT_PATT.matcher(msgBody);
        Matcher matchTime = TIME_PATT.matcher(msgBody);
        if (matchOrigin.find() && matchDest.find() && matchTime.find()) {
            nickNameOrigin = matchOrigin.group(1).trim();
            nickNameDest = matchDest.group(1).trim();
            time = matchTime.group().trim().replaceAll("%3A", ":");
            return new String[] {nickNameOrigin, nickNameDest, time};
        }
        return null;
    }

    public int formatNumPauseDays(final String msgBody) {
        // Pause for 5 days
        Matcher numMatch = NUM_DAYS_PAUSE_PATT.matcher(msgBody);
        if (numMatch.find()) {
            return Integer.parseInt(numMatch.group().trim());
        }
        return 0;
    }

    public String formatFromNumber(final String fullSMSResponse) {
        // Get the from number including the country code, e.g. 19366897391
        Matcher fromNumMatch = FROM_NUMBER_PATT.matcher(fullSMSResponse);
        if (fromNumMatch.find()) {
            return fromNumMatch.group().trim();
        }
        return null;
    }

    public String replaceCharacters(final String text) {
        // Used to prettily display info to the user
        return text.replaceAll("\\++", "+").replaceAll("(%27)", "'").replaceAll("(%2C)|(%2c)", ",").replaceAll("(\\+)", " ").replaceAll("\\.", "");
    }
}
