package com.fredrikux.unitedcolors.models;

import android.content.Context;
import android.content.SharedPreferences;

import com.fredrikux.unitedcolors.R;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * This class keeps the score safe, it uses hidden keys and primes
 * for creating safe way of storing score local on the device for later
 * use when internet connection becomes available. This class implements
 * the Singleton-pattern.
 */
public class ScoreGuard {

    private static ScoreGuard mGuard = new ScoreGuard();

    private long mBigPrime;
    private SharedPreferences.Editor mEditor;
    private String mScoreKey;
    private SharedPreferences mSharedPref;
    private String mSavedScoreKey;


    // Private constructor for singelton-pattern.
    private ScoreGuard(){}

    public static ScoreGuard sharedInstance(){
        return mGuard;
    }


    /**
     * Initiate the score guard, can only be run once. Throws exception
     * otherwise.
     *
     * @param context a context to use for saving the score locally.
     * @throws Exception if the score guard is all ready initiated.
     */
    public void init(final Context context) throws Exception {
        if(mEditor != null)
            throw new Exception("Is already initiated.");
        mSharedPref = context.getSharedPreferences(
                context.getString(R.string.preference_file_key), Context
                        .MODE_PRIVATE);

        mSavedScoreKey = context.getString(R.string.saved_score_key);
        mBigPrime = Long.parseLong(context.getString(R.string.big_prime));
        mEditor = mSharedPref.edit();
        mScoreKey = context.getString(R.string.score_key);

    }

    /**
     * Saves the score localy on the deveice.
     *
     * @param score
     */
    public void saveScore(final int score){
        isInit();

        final long bNumber = score * mBigPrime;
        final String hexString = Long.toHexString(bNumber);
        final String value = mScoreKey + hexString;
        final String checksum = calculateChecksum(value);

        final String val = checksum + ":" + value;

        mEditor.putString(mSavedScoreKey, val);
        mEditor.commit();
    }

    /**
     * Calculates the checksum of the string.
     *
     * @param str the string to calculate the checksum on.
     * @return the checksum as string.
     */
    private String calculateChecksum(final String str){
        MessageDigest mdEnc = null;
        try {
            mdEnc = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Some thing went wrong with MD5.");
        }
        mdEnc.update(str.getBytes(), 0, str.length());
        String s = new BigInteger(1, mdEnc.digest()).toString(16);
        String.format("%05d", 2);
        return s;
    }

    /**
     * Loads the score saved locally on the device.
     * @return
     */
    public int getScore() {
        isInit();

        String defualt = "default";

        String saved = mSharedPref.getString(mSavedScoreKey, defualt);

        if (saved.equals(defualt))
            return 0;

        String[] split = saved.split(":");

        if(split.length != 2)
            throw new RuntimeException("Wrong score key.");

        String checksum = split[0];
        String value = split[1];

        if(!checksum.equals(calculateChecksum(value)))
            throw new RuntimeException("Wrong score key.");

        String scoreKey = value.substring(0, mScoreKey.length()).toLowerCase();

        if(!scoreKey.equals(mScoreKey))
            throw new RuntimeException("Wrong score key.");

        String hexString = value.substring(mScoreKey.length(), value.length());

        long bNumber = Long.parseLong(hexString, 16);
        int score = (int) (bNumber/mBigPrime);


        return score;
    }

    /**
     * Checks to see if the guard already has been initiated.
     */
    private void isInit(){
        if(mEditor == null){
            throw new RuntimeException("Need to run init(Context) first");
        }
    }
}
