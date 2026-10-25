package com.axie.advisor;

import android.graphics.Rect;
import com.google.mlkit.vision.text.Text;
import java.util.*;
import java.util.regex.*;

/** Experimental OCR-based card candidate detection; numbers must be verified by player. */
public final class BattleStrategy {
    public static final class Card {
        public String name; public int energy,attack,shield;
        Card(String n,int e,int a,int s){name=n;energy=e;attack=a;shield=s;}
    }
    public static final class Detection {
        public final ArrayList<Card> cards=new ArrayList<>();
        public String warning="";
    }
    private static final Pattern NUMBER=Pattern.compile("\\b\\d{1,3}\\b");
    public static Detection detect(Text text,int imageWidth,int imageHeight){
        Detection d=new Detection();
        // Heuristic only: cards are usually near the bottom of landscape battle screens.
        ArrayList<Text.TextBlock> blocks=new ArrayList<>();
        for(Text.TextBlock b:text.getTextBlocks()){
            Rect r=b.getBoundingBox();
            if(r!=null && r.centerY()>imageHeight*0.52)blocks.add(b);
        }
        blocks.sort((a,b)->Integer.compare(a.getBoundingBox().centerX(),b.getBoundingBox().centerX()));
        for(Text.TextBlock block:blocks){
            String raw=block.getText().trim();
            if(raw.length()<3)continue;
            ArrayList<Integer> nums=new ArrayList<>();
            Matcher m=NUMBER.matcher(raw);
            while(m.find())try{nums.add(Integer.parseInt(m.group()));}catch(Exception ignored){}
            if(nums.size()<2)continue;
            // Do not assume number meanings; card candidates remain unverified.
            String title=raw.split("\\n")[0].replaceAll("[0-9]","").trim();
            if(title.length()<2)title="Unknown card";
            int cost=nums.get(0),attack=nums.get(1),shield=nums.size()>2?nums.get(2):0;
            if(cost>4||attack>250||shield>250)continue;
            d.cards.add(new Card(title,cost,attack,shield));
            if(d.cards.size()>=12)break;
        }
        d.warning="Experimental OCR candidates. Verify each card, energy, attack and shield before relying on a recommendation. Enemy targeting, class bonuses, buffs and card effects are not modeled.";
        return d;
    }
    public static String recommend(List<Card> cards,int energy,int enemyHp){
        if(energy<0||energy>20)return "Enter a valid energy value (0-20).";
        if(cards.isEmpty())return "No card candidates detected. Try a clearer landscape battle screenshot.";
        if(cards.size()>16)return "Too many cards to evaluate.";
        int bestMask=0,bestScore=Integer.MIN_VALUE,bestEnergy=0,bestAttack=0,bestShield=0;
        for(int mask=0;mask<(1<<cards.size());mask++){
            int e=0,a=0,s=0;
            for(int i=0;i<cards.size();i++)if((mask&(1<<i))!=0){Card c=cards.get(i);e+=c.energy;a+=c.attack;s+=c.shield;}
            if(e>energy)continue;
            int score=a*10+s*3-e*4;
            if(enemyHp>0&&a>=enemyHp)score+=1000-(a-enemyHp);
            if(score>bestScore){bestScore=score;bestMask=mask;bestEnergy=e;bestAttack=a;bestShield=s;}
        }
        StringBuilder result=new StringBuilder("Experimental combination: ");
        for(int i=0;i<cards.size();i++)if((bestMask&(1<<i))!=0)result.append("#").append(i+1).append(" ").append(cards.get(i).name).append("; ");
        result.append("\\nEnergy: ").append(bestEnergy).append("/").append(energy);
        result.append("\\nRaw attack: ").append(bestAttack).append(" | Shield: ").append(bestShield);
        if(enemyHp>0)result.append("\\nEnemy HP entered: ").append(enemyHp);
        result.append("\\nNot a reliable win prediction. Confirm OCR and actual game rules.");
        return result.toString();
    }
}
