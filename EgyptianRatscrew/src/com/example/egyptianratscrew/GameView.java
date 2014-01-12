package com.example.egyptianratscrew;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Random;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

public class GameView extends View{
    HashMap <Card, Bitmap> cardGraphics;
    Context mContext;
    Paint mBGPaint;
    boolean drawSlap, drawBurn, computerSlap;
    int windowWidth, windowHeight, rotation = 0;

    TextView myDeckCounter;
    TextView cpuDeckCounter1;
    TextView cpuDeckCounter2;
    TextView cpuDeckCounter3;
    TextView burnCounter;
    TextView slapMsg;

    ArrayList<Card> drawnCards = new ArrayList<Card>();
    ArrayList<Card> table = new ArrayList<Card>();
    ArrayList<Card> burned = new ArrayList<Card>();
    Deck playerDeck, computerDeck1, computerDeck2, computerDeck3;
    Computer computer;
    int[] settings;
    /* settings[0] = # of players
     * settings[1] = difficulty
     * settings[2] = Doubles
     * settings[3] = Sandwiches
     * settings[4] = Tens (x,10-x)
     * settings[5] = Tens sandwiches
     * settings[6] = Marriage (K,Q or Q,K)
     * settings[7] = Divorce (K,x,Q or Q,x,K)
     * settings[8] = Slap 7s
     * settings[9] = Top-Bottom
     */
    
    int turn = 0; //0 = player's turn
    int players; //# of players including user
    int difficulty; //difficulty level
    int owed_card_counter = 1; //number of cards owed for each respective face card - queen: 2 king: 3 ace: 4 else: 1
    int face_card_counter = -1; //counts down to 0; whoever played the face card takes the pile
    int temp_counter; //temporary counter used to improve efficiency
    int gameOver = -1; //0 = lose, 1 = win
    String slap_message = ""; //displayed message depending on the combination slapped
    Handler handlerTimer = new Handler();
    Runnable reaction;
    Random rand = new Random();
    int turnFlag = 0; //stop computer from playing at the same time as the player
    int slapFlag = 0; //stop certain code from executing when a valid slap occurs
    int slap_no_burn_flag = 0; //grace period for player slapping at the same time as the computer
    int take_pile_flag = 0; //stop player from playing a card when the pile needs to be taken
    
    //statistic counters
    int slaps_won = 0, total_slaps = 0, cards_burned = 0;
    double time_elapsed = 0;
    
    public GameView(Context context){
        super(context);
        mContext = context;
        init();
    }
    public GameView(Context context, AttributeSet attrs){
        super(context, attrs);
        mContext = context;
        init();
    }
    public GameView(Context context, AttributeSet attrs, int defStyle){
        super(context, attrs, defStyle);
        mContext = context;
        init();
    }

    public void setupCounter(TextView mDC, TextView bC, TextView cpuDC1, TextView cpuDC2, TextView cpuDC3){
        myDeckCounter = mDC;
        burnCounter = bC;
        cpuDeckCounter1 = cpuDC1;
        cpuDeckCounter2 = cpuDC2;
        cpuDeckCounter3 = cpuDC3;
    }
    
    public void updateCounter(){
        myDeckCounter.setText(Integer.toString(playerDeck.getSize()));
        myDeckCounter.invalidate();
        cpuDeckCounter1.setText(Integer.toString(computerDeck1.getSize()));
        cpuDeckCounter1.invalidate();
        burnCounter.setText(Integer.toString(burned.size()));
        burnCounter.invalidate();
        if(cpuDeckCounter2 != null){
            cpuDeckCounter2.setText(Integer.toString(computerDeck2.getSize()));
            cpuDeckCounter2.invalidate();
        }
        if(cpuDeckCounter3 != null){
            cpuDeckCounter3.setText(Integer.toString(computerDeck3.getSize()));
            cpuDeckCounter3.invalidate();
        }
    }
    
    public void setupBundle(Bundle b){
        settings = b.getIntArray("settings");
        ArrayList<Card> deckArr = new ArrayList<Card>();
        setup(deckArr);
        invalidate();
    }
    
    public void setupTextViews(TextView slapTV){
        slapMsg = slapTV;
    }
    
    @SuppressWarnings("deprecation")
    public void init(){
        setFocusable(true);
        setFocusableInTouchMode(true);
        
        WindowManager wm = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        windowWidth = wm.getDefaultDisplay().getWidth();
        windowHeight = wm.getDefaultDisplay().getHeight();
        
        cardGraphics = new HashMap<Card, Bitmap>();
   
        drawSlap = false;
        computerSlap = false;
        drawBurn = false;
    }
    
    public void drawCard(Card c){
        drawnCards.add(c);
        invalidate();
    }

    @SuppressLint("DrawAllocation")
    @Override
    public void onDraw(Canvas canvas){
        
        Paint p = new Paint();
        Bitmap cp1, cp2, cp3, deckimg;
        
        //Highlight active player
        if (turn == 0) deckimg = BitmapFactory.decodeResource(getResources(), R.drawable.card_back_turn);
        else deckimg = BitmapFactory.decodeResource(getResources(), R.drawable.card_back);
        deckimg = Bitmap.createScaledBitmap(deckimg, windowWidth/4, windowHeight/4, false);
        canvas.drawBitmap(deckimg,  windowWidth/2 - deckimg.getWidth()/2, windowHeight - deckimg.getHeight() - windowHeight/10, p);
        
        if (players == 4)
        {
            if (turn == 1) cp1 = BitmapFactory.decodeResource(getResources(), R.drawable.p3_turn);
            else cp1 = BitmapFactory.decodeResource(getResources(), R.drawable.p3);
        }
        else
        {
            if (turn == 1) cp1 = BitmapFactory.decodeResource(getResources(), R.drawable.p1_turn);
            else cp1 = BitmapFactory.decodeResource(getResources(), R.drawable.p1);
        }
        cp1 = Bitmap.createScaledBitmap(cp1, windowWidth/4, windowHeight/4, false);
        canvas.drawBitmap(cp1, windowWidth/2 - cp1.getWidth()/2 - cp1.getWidth()/2 * (players - 2), 0, p);
        
        if (players >= 3)
        {
            if (turn == players - 1) cp2 = BitmapFactory.decodeResource(getResources(), R.drawable.p2_turn);
            else cp2 = BitmapFactory.decodeResource(getResources(), R.drawable.p2);
            cp2 = Bitmap.createScaledBitmap(cp2, windowWidth/4, windowHeight/4, false);
            canvas.drawBitmap(cp2, windowWidth/2 - cp2.getWidth()/2 + cp2.getWidth()/2 * (players - 2), 0, p);
        }
        
        if (players == 4)
        {
            if (turn == 2) cp3 = BitmapFactory.decodeResource(getResources(), R.drawable.p1_turn);
            else cp3 = BitmapFactory.decodeResource(getResources(), R.drawable.p1);
            cp3 = Bitmap.createScaledBitmap(cp3, windowWidth/4, windowHeight/4, false);
            canvas.drawBitmap(cp3, windowWidth/2 - cp3.getWidth()/2, 0, p);
        }
        
        //Draw cards if it should
        Matrix matrix = new Matrix();
        Bitmap c = null;
        rotation = 0;
        for(int i = 0; i < drawnCards.size(); i++){
            matrix.postRotate(rotation); // clockwise by 30 degrees
            rotation += 38;
            c = cardGraphics.get(drawnCards.get(i));
            c = Bitmap.createBitmap(c, 0, 0, c.getWidth(), c.getHeight(), matrix, true);
            canvas.drawBitmap(c, windowWidth/2 - c.getWidth()/2, windowHeight/2-c.getHeight()/2, p);
        }
        
        Bitmap fireimg = BitmapFactory.decodeResource(getResources(), R.drawable.burn2);
        fireimg = Bitmap.createScaledBitmap(fireimg, windowWidth/10, (int)(((windowWidth/10.0)/fireimg.getWidth()*fireimg.getHeight())), false);
        canvas.drawBitmap(fireimg, windowWidth/10 - fireimg.getWidth() / 4, windowHeight/2 + fireimg.getHeight()/2, p);
        
        for(int i = burned.size(); i > 0; i--){
            if(i > 3) i = 3;
            c = cardGraphics.get(burned.get(burned.size() - i));
            c = Bitmap.createScaledBitmap(c, (int)(windowWidth/15.0), (int)(windowHeight/15.0), false);
            canvas.drawBitmap(c, windowWidth/10 - 8, windowHeight/2 - 10 - c.getHeight()/2 * i, p);
        }
        
        //Draw slap if it should
        if(drawSlap){
            Bitmap slapimg = BitmapFactory.decodeResource(getResources(), R.drawable.hand);
            slapimg = Bitmap.createScaledBitmap(slapimg, slapimg.getWidth()/2, slapimg.getHeight()/2, false);
            canvas.drawBitmap(slapimg, windowWidth/2 - slapimg.getWidth()/2, windowHeight/2 - slapimg.getHeight()/2, p);
            drawSlap = false;
            invalidate();
        }
        if (computerSlap){
            Bitmap slapimg = BitmapFactory.decodeResource(getResources(), R.drawable.slap);
            slapimg = Bitmap.createScaledBitmap(slapimg,  windowWidth/4, (int)(((windowWidth/4.0)/slapimg.getWidth()*slapimg.getHeight())), false);
            canvas.drawBitmap(slapimg, windowWidth/2 - cp1.getWidth()/2 + (turn-2)*cp1.getWidth(), cp1.getHeight()/2, p);
            computerSlap = false;
            invalidate();
        }
        if (drawBurn){
            Bitmap burnimg = BitmapFactory.decodeResource(getResources(), R.drawable.burn);
            burnimg = Bitmap.createScaledBitmap(burnimg,  windowWidth/4, (int)(((windowWidth/4.0)/burnimg.getWidth()*burnimg.getHeight())), false);
            canvas.drawBitmap(burnimg, windowWidth/2 - burnimg.getWidth(), windowHeight - deckimg.getHeight() - windowHeight/5-burnimg.getHeight(), p);
            drawBurn = false;
            invalidate();
        }
        if(gameOver == 1){
            Bitmap win = BitmapFactory.decodeResource(getResources(), R.drawable.win);
            win = Bitmap.createScaledBitmap(win,  windowWidth/2, (int)(((windowWidth/2.0)/win.getWidth()*win.getHeight())), false);
            canvas.drawBitmap(win, windowWidth/2 - win.getWidth()/2, windowHeight/2 - win.getHeight()/2, p);
        }
        else if(gameOver == 0){
            Bitmap lose = BitmapFactory.decodeResource(getResources(), R.drawable.lose);
            lose = Bitmap.createScaledBitmap(lose,  windowWidth/2, (int)(((windowWidth/2.0)/lose.getWidth()*lose.getHeight())), false);
            canvas.drawBitmap(lose, windowWidth/2 - lose.getWidth()/2, windowHeight/2 - lose.getHeight()/2, p);
        }
       
        updateCounter();
    }
     
    @SuppressWarnings("unused")
    private void setup(ArrayList<Card> cards){
        Bitmap b = BitmapFactory.decodeResource(getResources(), R.drawable.card_back);

        for (int i = 0; i <= 3; i++){
            for (int j = 1; j <= 13; j++){
                String suit = null;
                switch (i){
                    case 0:
                        suit = "clubs";
                        break;
                    case 1:
                        suit = "diamonds";
                        break;
                    case 2:
                        suit = "hearts";
                        break;
                    case 3:
                        suit = "spades";
                        break;
                }
            
                String value;
                if (j==1)
                    value = "a";
                else
                    value = "" + j;
                Card c = new Card(i,j);
                cards.add(c);
                
                if (i == 0 && j == 1) b = BitmapFactory.decodeResource(getResources(), R.drawable.clubs_a);        
                else if (i == 0 && j == 2) b = BitmapFactory.decodeResource(getResources(), R.drawable.clubs_2);
                else if (i == 0 && j == 3) b = BitmapFactory.decodeResource(getResources(), R.drawable.clubs_3);
                else if (i == 0 && j == 4) b = BitmapFactory.decodeResource(getResources(), R.drawable.clubs_4);
                else if (i == 0 && j == 5) b = BitmapFactory.decodeResource(getResources(), R.drawable.clubs_5);
                else if (i == 0 && j == 6) b = BitmapFactory.decodeResource(getResources(), R.drawable.clubs_6);
                else if (i == 0 && j == 7) b = BitmapFactory.decodeResource(getResources(), R.drawable.clubs_7);
                else if (i == 0 && j == 8) b = BitmapFactory.decodeResource(getResources(), R.drawable.clubs_8);
                else if (i == 0 && j == 9) b = BitmapFactory.decodeResource(getResources(), R.drawable.clubs_9);
                else if (i == 0 && j == 10) b = BitmapFactory.decodeResource(getResources(), R.drawable.clubs_10);
                else if (i == 0 && j == 11) b = BitmapFactory.decodeResource(getResources(), R.drawable.clubs_11);
                else if (i == 0 && j == 12) b = BitmapFactory.decodeResource(getResources(), R.drawable.clubs_12);
                else if (i == 0 && j == 13) b = BitmapFactory.decodeResource(getResources(), R.drawable.clubs_13);
                else if (i == 1 && j == 1) b = BitmapFactory.decodeResource(getResources(), R.drawable.diamonds_a);
                else if (i == 1 && j == 2) b = BitmapFactory.decodeResource(getResources(), R.drawable.diamonds_2);
                else if (i == 1 && j == 3) b = BitmapFactory.decodeResource(getResources(), R.drawable.diamonds_3);
                else if (i == 1 && j == 4) b = BitmapFactory.decodeResource(getResources(), R.drawable.diamonds_4);
                else if (i == 1 && j == 5) b = BitmapFactory.decodeResource(getResources(), R.drawable.diamonds_5);
                else if (i == 1 && j == 6) b = BitmapFactory.decodeResource(getResources(), R.drawable.diamonds_6);
                else if (i == 1 && j == 7) b = BitmapFactory.decodeResource(getResources(), R.drawable.diamonds_7);
                else if (i == 1 && j == 8) b = BitmapFactory.decodeResource(getResources(), R.drawable.diamonds_8);
                else if (i == 1 && j == 9) b = BitmapFactory.decodeResource(getResources(), R.drawable.diamonds_9);
                else if (i == 1 && j == 10) b = BitmapFactory.decodeResource(getResources(), R.drawable.diamonds_10);
                else if (i == 1 && j == 11) b = BitmapFactory.decodeResource(getResources(), R.drawable.diamonds_11);
                else if (i == 1 && j == 12) b = BitmapFactory.decodeResource(getResources(), R.drawable.diamonds_12);
                else if (i == 1 && j == 13) b = BitmapFactory.decodeResource(getResources(), R.drawable.diamonds_13);
                else if (i == 2 && j == 1) b = BitmapFactory.decodeResource(getResources(), R.drawable.hearts_a);
                else if (i == 2 && j == 2) b = BitmapFactory.decodeResource(getResources(), R.drawable.hearts_2);
                else if (i == 2 && j == 3) b = BitmapFactory.decodeResource(getResources(), R.drawable.hearts_3);
                else if (i == 2 && j == 4) b = BitmapFactory.decodeResource(getResources(), R.drawable.hearts_4);
                else if (i == 2 && j == 5) b = BitmapFactory.decodeResource(getResources(), R.drawable.hearts_5);
                else if (i == 2 && j == 6) b = BitmapFactory.decodeResource(getResources(), R.drawable.hearts_6);
                else if (i == 2 && j == 7) b = BitmapFactory.decodeResource(getResources(), R.drawable.hearts_7);
                else if (i == 2 && j == 8) b = BitmapFactory.decodeResource(getResources(), R.drawable.hearts_8);
                else if (i == 2 && j == 9) b = BitmapFactory.decodeResource(getResources(), R.drawable.hearts_9);
                else if (i == 2 && j == 10) b = BitmapFactory.decodeResource(getResources(), R.drawable.hearts_10);
                else if (i == 2 && j == 11) b = BitmapFactory.decodeResource(getResources(), R.drawable.hearts_11);
                else if (i == 2 && j == 12) b = BitmapFactory.decodeResource(getResources(), R.drawable.hearts_12);
                else if (i == 2 && j == 13) b = BitmapFactory.decodeResource(getResources(), R.drawable.hearts_13);
                else if (i == 3 && j == 1) b = BitmapFactory.decodeResource(getResources(), R.drawable.spades_a);
                else if (i == 3 && j == 2) b = BitmapFactory.decodeResource(getResources(), R.drawable.spades_2);
                else if (i == 3 && j == 3) b = BitmapFactory.decodeResource(getResources(), R.drawable.spades_3);
                else if (i == 3 && j == 4) b = BitmapFactory.decodeResource(getResources(), R.drawable.spades_4);
                else if (i == 3 && j == 5) b = BitmapFactory.decodeResource(getResources(), R.drawable.spades_5);
                else if (i == 3 && j == 6) b = BitmapFactory.decodeResource(getResources(), R.drawable.spades_6);
                else if (i == 3 && j == 7) b = BitmapFactory.decodeResource(getResources(), R.drawable.spades_7);
                else if (i == 3 && j == 8) b = BitmapFactory.decodeResource(getResources(), R.drawable.spades_8);
                else if (i == 3 && j == 9) b = BitmapFactory.decodeResource(getResources(), R.drawable.spades_9);
                else if (i == 3 && j == 10) b = BitmapFactory.decodeResource(getResources(), R.drawable.spades_10);
                else if (i == 3 && j == 11) b = BitmapFactory.decodeResource(getResources(), R.drawable.spades_11);
                else if (i == 3 && j == 12) b = BitmapFactory.decodeResource(getResources(), R.drawable.spades_12);
                else if (i == 3 && j == 13) b = BitmapFactory.decodeResource(getResources(), R.drawable.spades_13);
                
                b = Bitmap.createScaledBitmap(b, windowWidth/4, windowHeight/4, false);
                cardGraphics.put(c, b);
            }
        }
        
        Collections.shuffle(cards);
        
        ArrayList<Card> p1 = new ArrayList<Card>();
        ArrayList<Card> p2 = new ArrayList<Card>();
        ArrayList<Card> p3 = new ArrayList<Card>();
        ArrayList<Card> p4 = new ArrayList<Card>();
        
        players = settings[0];
        switch (players){
            case 2:
                for (int i = 0; i < 52;){
                    p1.add(cards.get(i++));
                    p2.add(cards.get(i++));
                }
                
                break;
                
            case 3:
                for (int i = 0; i < 51;){
                    p1.add(cards.get(i++));
                    p2.add(cards.get(i++));
                    p3.add(cards.get(i++));
                }
                p1.add(cards.get(51)); //extra card goes to player 18-17-17
                
                break;
                
            case 4:
                for (int i = 0; i < 52;){
                    p1.add(cards.get(i++));
                    p2.add(cards.get(i++));
                    p3.add(cards.get(i++));
                    p4.add(cards.get(i++));
                }
                
                break;        
        }
        
        Collections.reverse(p1);
        Collections.reverse(p2);
        Collections.reverse(p3);
        Collections.reverse(p4);
        
        playerDeck = new Deck(p1);
        computerDeck1 = new Deck(p2);
        computerDeck2 = new Deck(p3);
        computerDeck3 = new Deck(p4);

        difficulty = settings[1];
        computer = new Computer(difficulty);
    }
    
    //slap trigger
    public void slap()
    {
        if (turn != -1)
        {
            if (checkSlap(playerDeck)){
                slaps_won++;
                drawSlap = true;
                invalidate();
            }
            else if (slap_no_burn_flag != 1){
                Log.v("egyptianRS", "turn0: BURN SLAP");
                
                drawBurn = true;
                invalidate();
            }
        }
    }
    
    //draw trigger
    public void playerTurn(){
        if (turn == 0 && owed_card_counter > 0 && take_pile_flag != 1){
            turnFlag = 1;
            slapFlag = 0;
            slap_no_burn_flag = 0;
            
            if (time_elapsed == 0)
                time_elapsed = System.currentTimeMillis();
            
            if (computerDeck1.getSize() == 0 && computerDeck2.getSize() == 0 && computerDeck3.getSize() == 0){
                Log.v("egyptianRS", "WIN");
                
                gameOver = 1;
                time_elapsed = (System.currentTimeMillis() - time_elapsed) / 1000 / 60; //in minutes
                turn = -1;
                invalidate();
                return;
            }
            
            final Card card = playerDeck.draw();
            
            if (card == null){
                if (players == 2 || (players == 3 && (computerDeck1.getSize() == 0 || computerDeck2.getSize() == 0)) ||
                    (players == 4 && ((computerDeck1.getSize() == 0 && computerDeck2.getSize() == 0) ||
                            (computerDeck2.getSize() == 0 && computerDeck3.getSize() == 0) ||
                            (computerDeck3.getSize() == 0 && computerDeck1.getSize() == 0)))){
                    
                    Log.v("egyptianRS", "LOSE");
                    
                    gameOver = 0;
                    time_elapsed = (System.currentTimeMillis() - time_elapsed) / 1000 / 60; //in minutes
                    turn = -1;
                    invalidate();
                }
                else{
                    turn = (turn + 1) % players;
                    if (getCurrentDeck().getSize() == 0)
                        turn = (turn + 1) % players;
                    if (getCurrentDeck().getSize() == 0)
                        turn = (turn + 1) % players;
                    computerTurn();
                }
            }
            else{
                face_card_counter--;
                owed_card_counter--;
                
                Log.v("egyptianRS", "turn0:" + card.getValue() + " face:" + face_card_counter + " owed:" + owed_card_counter);
 
                table.add(card);
                drawCard(card);
                slapMsg.setText("");
                slapMsg.invalidate();
                
                if ((temp_counter = card.numCardsOwed()) > 1 || card.getValue() == 11){
                    face_card_counter = temp_counter;
                    owed_card_counter = 0;
                }
                
                if (face_card_counter == 0)
                    take_pile_flag = 1;
                else if (owed_card_counter == 0){
                    turn = (turn + 1) % players;
                    if (getCurrentDeck().getSize() == 0)
                        turn = (turn + 1) % players;
                    if (getCurrentDeck().getSize() == 0)
                        turn = (turn + 1) % players;
                    
                    Log.v("egyptianRS", "player: turn is now 1");
                    
                    if (getCurrentDeck().getSize() != 0)
                        invalidate();
                }
                
                handlerTimer.removeCallbacks(reaction);
                handlerTimer.postDelayed (reaction = new Runnable(){public void run(){
                    if (computerSlap()){}
                    else if (slapFlag != 1){ //if no one slapped
                        if (take_pile_flag == 1){
                            
                            Log.v("egyptianRS", "turn:" + ((turn + players - 1) % players) + " TAKE PILE");
                            
                            take_pile_flag = 0;
                            takePile(getPreviousDeck());
                        }
                        else if (owed_card_counter == 0){
                            owed_card_counter = temp_counter;
                            turnFlag = 0;
                            computerTurn();
                        }
                    }
                }}, computer.getDelay());
            }
        }
        else{
            if (turn != -1 && playerDeck.getSize() != 0)
            {
                Log.v("egyptianRS", "turn0: BURN TURN");
                
                drawBurn = true;
                invalidate();
                
                Card card = playerDeck.draw();
                
                if (card != null){
                    burned.add(card);
                    cards_burned++;
                }
            }
        }
    }
    
    private void computerTurn(){
        if (turn != 0 && turnFlag != 1){
            invalidate();
            slapFlag = 0;
            
            if (playerDeck.getSize() == 0 && ((players == 2 || (players == 3 && (computerDeck1.getSize() == 0 || computerDeck2.getSize() == 0))
                    || (players == 4 && ((computerDeck1.getSize() == 0 && computerDeck2.getSize() == 0) ||
                            (computerDeck2.getSize() == 0 && computerDeck3.getSize() == 0) ||
                            (computerDeck3.getSize() == 0 && computerDeck1.getSize() == 0)))))){
                    
                    Log.v("egyptianRS", "LOSE");
                    
                    gameOver = 0;
                    time_elapsed = (System.currentTimeMillis() - time_elapsed) / 1000 / 60; //in minutes
                    turn = -1;
                    invalidate();
                    return;
            }
            
            final Card card = getComputerDeck().draw();
            
            if (card == null){
                if (computerDeck1.getSize() == 0 && computerDeck2.getSize() == 0 && computerDeck3.getSize() == 0){
                    Log.v("egyptianRS", "WIN");
                    
                    gameOver = 1;
                    time_elapsed = (System.currentTimeMillis() - time_elapsed) / 1000 / 60; //in minutes
                    turn = -1;
                    invalidate();
                }
                else{
                    turn = (turn + 1) % players;
                    if (getCurrentDeck().getSize() == 0)
                        turn = (turn + 1) % players;
                    if (getCurrentDeck().getSize() == 0)
                        turn = (turn + 1) % players;
                    
                    if (turn != 0)
                        computerTurn();
                }
            }
            else{
                face_card_counter--;
                owed_card_counter--;
                
                Log.v("egyptianRS", "turn:" + turn + " " + card.getValue() + " face:" + face_card_counter + " owed:" + owed_card_counter);

                table.add(card);
                drawCard(card);
                slapMsg.setText("");
                slapMsg.invalidate();
                
                if ((temp_counter = card.numCardsOwed()) > 1 || card.getValue() == 11){
                    face_card_counter = temp_counter;
                    owed_card_counter = temp_counter;
                    
                    turn = (turn + 1) % players;
                    if (getCurrentDeck().getSize() == 0)
                        turn = (turn + 1) % players;
                    if (getCurrentDeck().getSize() == 0)
                        turn = (turn + 1) % players;
                    
                    if (getCurrentDeck().getSize() != 0)
                        invalidate();
   
                    Log.v("egyptianRS", "turn+++ in if");
                }
                else if (owed_card_counter == 0){
                    owed_card_counter = temp_counter;

                    if (face_card_counter == 0)
                        take_pile_flag = 1;
                    else
                    {
                        turn = (turn + 1) % players;
                        if (getCurrentDeck().getSize() == 0)
                            turn = (turn + 1) % players;
                        if (getCurrentDeck().getSize() == 0)
                            turn = (turn + 1) % players;
                        
                        if (getCurrentDeck().getSize() != 0)
                            invalidate();
                        
                        Log.v("egyptianRS", "turn+++ in else if");
                    }
                }
                
                handlerTimer.removeCallbacks(reaction);
                handlerTimer.postDelayed (reaction = new Runnable(){public void run(){
                    if(computerSlap()){}
                    else if (slapFlag != 1){ //if no one slapped
                        if (take_pile_flag == 1){
                            
                            Log.v("egyptianRS", "turn:" + ((turn + players - 1) % players) + " TAKE PILE");
                            
                            take_pile_flag = 0;
                            takePile(getPreviousDeck());
                        }
                        else if (turn != 0)
                            computerTurn();
                    }
                }}, computer.getDelay());
            }
        }
    }
    
    private boolean computerSlap(){
        Deck deck;
        
        if (checkRules()){
            slap_no_burn_flag = 1;
            deck = randomCompDeck();
            checkSlap(deck);
            computerSlap = true;
            invalidate();
            return true;
        }
        
        return false;
    }
    
    private Deck randomCompDeck(){
        int randomDeck;
        
        if (players == 2) return computerDeck1;
        else if (players == 3){
            randomDeck = rand.nextInt(2);
            
            if (randomDeck == 0) return computerDeck1;
            else return computerDeck2; //(randomDeck == 1)
        }
        else{//(players == 4)
            randomDeck = rand.nextInt(3);
            
            if (randomDeck == 0) return computerDeck1;
            else if (randomDeck == 1) return computerDeck2;
            else return computerDeck3; //(randomDeck == 2)
        }
    }
    
    private Deck getComputerDeck()
    {
        Deck computerDeck;
        
        if (turn == 1) computerDeck = computerDeck1;
        else if (turn == 2) computerDeck = computerDeck2;
        else computerDeck = computerDeck3; //(turn == 3)
        
        return computerDeck;
    }
    
    private Deck getCurrentDeck()
    {
        Deck currentDeck;
        
        if (turn == 0) currentDeck = playerDeck;
        else if (turn == 1) currentDeck = computerDeck1;
        else if (turn == 2) currentDeck = computerDeck2;
        else currentDeck = computerDeck3; //(turn == 3)
        
        return currentDeck;
    }
    
    private Deck getPreviousDeck()
    {
        Deck previousDeck;
        
        if (turn == 0)
        {
            if (players == 2) previousDeck = computerDeck1;
            else if (players == 3) previousDeck = computerDeck2;
            else previousDeck = computerDeck3; //(players == 4)
        }
        else if (turn == 1) previousDeck = playerDeck;
        else if (turn == 2) previousDeck = computerDeck1;
        else previousDeck = computerDeck2; //(turn == 3)
        
        if (previousDeck.getSize() == 0)
        {
            turn = (turn + players - 1) % players;
            previousDeck = getPreviousDeck();
        }
        return previousDeck;
    }
    
    private boolean checkSlap(Deck d){
        if (checkRules()){
            slapFlag = 1;
            total_slaps++;
            
            if (d == playerDeck) Log.v("egyptianRS", "SLAP PLAYER");
            else if (d == computerDeck1) Log.v("egyptianRS", "SLAP COMPUTER1");
            else if (d == computerDeck2) Log.v("egyptianRS", "SLAP COMPUTER2");
            else if (d == computerDeck3) Log.v("egyptianRS", "SLAP COMPUTER3");
            
            slapMsg.setText(slap_message);
            slapMsg.invalidate();
            
            takePile(d);
            return true;
        }
        else {
            if (slap_no_burn_flag != 1){
                Card card = d.draw();
                
                if (card != null){
                    cards_burned++;
                    burned.add(card);
                }
            }
            
            return false;
        }
    }
    
    private void takePile(Deck d){
        if (d == playerDeck) turn = 0;
        else if (d == computerDeck1) turn = 1;
        else if (d == computerDeck2) turn = 2;
        else if (d == computerDeck3) turn = 3;
        invalidate();
        
        d.add(table);
        d.add(burned);
        table.clear();
        burned.clear();
        drawnCards.clear();
        
        //reset everything
        face_card_counter = -1;
        owed_card_counter = 1;
        turnFlag = 0;
        slapFlag = 0;
        take_pile_flag = 0;
        
        handlerTimer.postDelayed (new Runnable(){public void run(){      
            if (getCurrentDeck() != playerDeck)
                computerTurn();
        }}, computer.getDelay());
    }
    
    private boolean checkRules(){
        slap_message = "";
        
        if ((settings[2] == 1) &&
                (table.size() - 2 >= 0) && //pile is sufficiently large
                (table.get(table.size() - 1).getValue() == table.get(table.size() - 2).getValue()))
            slap_message = "Double!";
        
        if ((settings[3] == 1) &&
                (table.size() - 3 >= 0) && //pile is sufficiently large
                (table.get(table.size() - 1).getValue() == table.get(table.size() - 3).getValue()))
            slap_message = "Sandwich!";
        
        if ((settings[4] == 1) &&
                (table.size() - 2 >= 0) && //pile is sufficiently large
                (table.get(table.size() - 1).getValue() + table.get(table.size() - 2).getValue()) == 10)
            slap_message = "Tens!";
        
        if ((settings[5] == 1) &&
                (table.size() - 3 >= 0) && //pile is sufficiently large
                (table.get(table.size() - 1).getValue() + table.get(table.size() - 3).getValue()) == 10)
            slap_message = "Tens Sandwich!";
        
        if ((settings[6] == 1) &&
                (table.size() - 2 >= 0) && //pile is sufficiently large
                ((table.get(table.size() - 1).getValue() == 12 && table.get(table.size() - 2).getValue() == 13) ||
                (table.get(table.size() - 1).getValue() == 13 && table.get(table.size() - 2).getValue() == 12)))
            slap_message = "Marriage!";
        
        if ((settings[7] == 1) &&
                (table.size() - 3 >= 0) && //pile is sufficiently large
                ((table.get(table.size() - 1).getValue() == 12 && table.get(table.size() - 3).getValue() == 13) ||
                (table.get(table.size() - 1).getValue() == 13 && table.get(table.size() - 3).getValue() == 12)))
            slap_message = "Divorce!";
        
        if ((settings[8] == 1) &&
        		(table.size() - 1 >= 0) && //pile is sufficiently large
                (table.get(table.size() - 1).getValue() == 7))
            slap_message = "7's!";
        
        if ((settings[9] == 1) &&
                (table.size() - 2 >= 0) && //pile is sufficiently large
                (table.get(0).getValue() == table.get(table.size() - 1).getValue()))
            slap_message = "Top-Bottom!";

        if (slap_message == "") return false;
        else return true;
    }
}