/*
 * Copyright (C) 2015 Aeranythe Echosong
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package com.screen;

import com.world.*;
import com.asciiPanel.AsciiPanel;
import java.awt.Color;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


import java.io.File;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;


/**
 *
 * @author Aeranythe Echosong
 */
public class PlayScreen implements Screen {

    private World world;
    private Creature player;
    private Creature boss;
    private int screenWidth;
    private int screenHeight;
    private List<String> messages;
    private List<String> oldMessages;
    private boolean isboss;
    private long startTime;
    private long score;
    private Creature player2;

    public PlayScreen(int flag) throws IOException {
        this.screenWidth = 40;
        this.screenHeight = 21;
        this.startTime = System.currentTimeMillis();
        this.score = 0;
        createWorld();
        this.messages = new ArrayList<String>();
        this.oldMessages = new ArrayList<String>();
        this.isboss = false;

        CreatureFactory creatureFactory = new CreatureFactory(this.world);
        createCreatures(creatureFactory);
        if (flag == 0) {
            Loadgame();
        }
    }

    public long getScore() {
        long endTime = System.currentTimeMillis();
        this.score = (endTime - startTime) / 1000;
        return score;
    }

    public void wirtedown() throws IOException {
        File file = new File("saveai.txt");
        BufferedWriter fileout = new BufferedWriter(new FileWriter(file));
        int[][] a = new int[4][2];
        for (int i = 0; i < 4; i++) {
            if (creaturelist[i].hp() > 0) {
                a[i][0] = creaturelist[i].x();
                a[i][1] = creaturelist[i].y();
            } else {
                a[i][0] = -1;
                a[i][1] = -1;
            }
        }
        String line = "";
        for (int i = 0; i < 4; i++) {
            line = line + String.valueOf(a[i][0]) + "\r\n" + String.valueOf(a[i][1]) + "\r\n";
        }
        line = line + String.valueOf(player.x()) + "\r\n" + String.valueOf(player.y()) + "\r\n";
        fileout.write(line);
        fileout.close();
    }

    private  Creature[] creaturelist;
    private void createCreatures(CreatureFactory creatureFactory) {
        this.player = creatureFactory.newPlayer(this.messages);

        creaturelist = new Creature[4];
        for (int i = 0; i < 4; i++) {
            creaturelist[i] = creatureFactory.newMonster();
            Thread t = new Thread(creaturelist[i].getAI());
            t.start();
        }
    }

    private void createWorld() {
        world = new WorldBuilder(50, 30).makeCaves().build();
    }

   

    private List<String> message2;

    public void setplayer2() {
        CreatureFactory cf = new CreatureFactory(this.world);
        this.player2 = cf.newPlayer(this.message2);
    }
    
    public void moveplayer2(String s) {
        switch (s) {
            case "W":
                player2.moveBy(0, -1);
                break;
            case "S":
                player2.moveBy(0, 1);
                break;
            case "A":
                player2.moveBy(-1, 0);
                break;
            case "D":
                player2.moveBy(1, 0);
                break;
            default:
                break;
        }
    }
    private void displayTiles(AsciiPanel terminal, int left, int top) {
        // Show terrain
        for (int x = 0; x < screenWidth; x++) {
            for (int y = 0; y < screenHeight; y++) {
                int wx = x + left;
                int wy = y + top;

                if (player.canSee(wx, wy)) {
                    terminal.write(world.glyph(wx, wy), x, y, world.color(wx, wy));
                } else {
                    terminal.write(world.glyph(wx, wy), x, y, Color.DARK_GRAY);
                }
            }
        }
        // Show creatures
        for (Creature creature : world.getCreatures()) {
            if (creature.x() >= left && creature.x() < left + screenWidth && creature.y() >= top
                    && creature.y() < top + screenHeight) {
                if (player.canSee(creature.x(), creature.y())) {
                    terminal.write(creature.glyph(), creature.x() - left, creature.y() - top, creature.color());
                }
            }
        }
        // Creatures can choose their next action now
        try{
            wirtedown();
        } catch (IOException e) {
            e.printStackTrace();
        }
        world.update();
    }

    private void displayMessages(AsciiPanel terminal, List<String> messages) {
        int top = this.screenHeight - messages.size();
        for (int i = 0; i < messages.size(); i++) {
            terminal.write(messages.get(i), 1, top + i + 1);
        }
        this.oldMessages.addAll(messages);
        messages.clear();
    }

    @Override
    public Screen displayOutput(AsciiPanel terminal) {
        // Terrain and creatures
        try{
            wirtedown();
        } catch (IOException e) {
            e.printStackTrace();
        }
        displayTiles(terminal, getScrollX(), getScrollY());
        // Player
        terminal.write(player.glyph(), player.x() - getScrollX(), player.y() - getScrollY(), player.color());
        // Stats
        String stats = String.format("%3d/%3d hp", player.hp(), player.maxHP());
        String killnumber = String.format("You have killed %3d fungus", player.killnum());
        String warning = "Boss is coming!";
        terminal.write(stats, 1, 21);
        if (!isboss) {
            terminal.write(killnumber);
        } else {
            if (boss.hp() > 0) {
                terminal.write(warning);
            }
        }
        // Messages
        displayMessages(terminal, this.messages);

        if (player.hp() <= 0) {
            return new LoseScreen();
        }

        if (isboss) {
            if (boss.hp() <= 0) {
                return new WinScreen(getScore());
            }
        }
        return this;
    }

    private void Savegame() throws IOException {
        File file = new File("save.txt");
        BufferedWriter fileout = new BufferedWriter(new FileWriter(file));
        int[] a = new int[3];
        a[0] = player.x();
        a[1] = player.y();
        a[2] = (int) this.startTime;
        String line = "";
        for (int i = 0; i < 3; i++) {
            line = line + String.valueOf(a[i]) + "\r\n";
        }
        fileout.write(line);
        fileout.close();
    }

    private void Loadgame() throws IOException {
        File file = new File("save.txt");
        FileInputStream fileinput = new FileInputStream(file);
        InputStreamReader reader = new InputStreamReader(fileinput);
        BufferedReader br = new BufferedReader(reader);
        int[] a = new int[3];
        String line = "";
        for (int i = 0; i < 3; i++) {
            line = br.readLine();
            a[i] = Integer.valueOf(line).intValue();
        }
        this.startTime = a[2];
        player.setX(a[0]);
        player.setY(a[1]);
        br.close();
    }
    
    @Override
    public Screen respondToUserInput(KeyEvent key) throws IOException {
        switch (key.getKeyCode()) {
        case KeyEvent.VK_LEFT:
            player.moveBy(-1, 0);
            break;
        case KeyEvent.VK_RIGHT:
            player.moveBy(1, 0);
            break;
        case KeyEvent.VK_UP:
            player.moveBy(0, -1);
            break;
        case KeyEvent.VK_DOWN:
            player.moveBy(0, 1);
            break;
        case KeyEvent.VK_ENTER:
            Savegame();
            break;
        }

        if (player.killnum() >= 4 && !isboss) {
            isboss = true;
            createboss();
        }
        return this;
    }

    private void createboss() {
        CreatureFactory creatureFactory = new CreatureFactory(this.world);
        this.boss = creatureFactory.newBoss(player);
        Thread t = new Thread(this.boss.getAI());
        t.start();
    }

    public int getScrollX() {
        return Math.max(0, Math.min(player.x() - screenWidth / 2, world.width() - screenWidth));
    }

    public int getScrollY() {
        return Math.max(0, Math.min(player.y() - screenHeight / 2, world.height() - screenHeight));
    }

}
