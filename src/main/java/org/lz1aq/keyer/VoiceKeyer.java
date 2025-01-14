// ***************************************************************************
// *   Copyright (C) 2015 by Chavdar Levkov                              
// *   ch.levkov@gmail.com                                                   
// *                                                                         
// *   This program is free software; you can redistribute it and/or modify  
// *   it under the terms of the GNU General Public License as published by  
// *   the Free Software Foundation; either version 2 of the License, or     
// *   (at your option) any later version.                                   
// *                                                                         
// *   This program is distributed in the hope that it will be useful,       
// *   but WITHOUT ANY WARRANTY; without even the implied warranty of        
// *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the         
// *   GNU General Public License for more details.                          
// *                                                                         
// *   You should have received a copy of the GNU General Public License     
// *   along with this program; if not, write to the                         
// *   Free Software Foundation, Inc.,                                       
// *   59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.             
// ***************************************************************************
package org.lz1aq.keyer;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineListener;
import javax.sound.sampled.LineUnavailableException;
import org.lz1aq.ptt.Ptt;

/**
 *
 * @author Admin
 */
public class VoiceKeyer {

    public static enum Keys {
        F1("f1.wav"),
        F3("f3.wav"),
        F4("f4.wav");

        private final String fileName;

        Keys(String fileName) {
            this.fileName = fileName;
        }

        public String getFileName() {
            return fileName;
        }
    };

    private Clip clip;
    private Ptt ptt = null;

    private static final Logger LOGGER = Logger.getLogger(VoiceKeyer.class.getName());

    public VoiceKeyer() {
        try {
            clip = AudioSystem.getClip();
            clip.addLineListener(new LocalLineListener());
        } catch (LineUnavailableException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }

    }

    static public int getF1LengthInSeconds() {
        File file = new File("f1.wav");
        AudioInputStream audioInputStream;
        try {
            audioInputStream = AudioSystem.getAudioInputStream(file);
        } catch (Exception ex) {
            Logger.getLogger(VoiceKeyer.class.getName()).log(Level.SEVERE, null, ex);
            return 3000;
        }
        AudioFormat format = audioInputStream.getFormat();
        long audioFileLength = file.length();
        int frameSize = format.getFrameSize();
        float frameRate = format.getFrameRate();
        float durationInSeconds = (audioFileLength / (frameSize * frameRate));
        return Math.round(durationInSeconds);
    }

    /**
     * Ptt object should be initialized and ready to use.
     *
     * If activated. This will make the keyer set the PTT to on before sending
     * Voice and set to OFF after sending is done.
     *
     * @param ptt
     */
    public void usePtt(Ptt ptt) {
        this.ptt = ptt;
    }


    /*
    * Supports Signed 16bit PCM (other formats not guaranteed).
    */
    public boolean play(VoiceKeyer.Keys key) {
        File file = new File(key.getFileName());
        if (clip.isOpen()) {
            // Still executing the last call - do nothing
            return false;
        }

        try {
            AudioInputStream inputStream = AudioSystem.getAudioInputStream(file);
            clip.open(inputStream);
            clip.start();
            return true;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, e.getMessage());
            return false;
        }
    }

    public void stopVoice() {
        if (clip.isRunning()) {
            clip.stop();
        }
    }

    public class LocalLineListener implements LineListener {

        @Override
        public void update(LineEvent event) {
            if (event.getType() == LineEvent.Type.OPEN) {
                if (ptt != null) {
                    ptt.on();
                } else {
                    LOGGER.log(Level.WARNING, "PTT not connected.");
                }

            } else if (event.getType() == LineEvent.Type.STOP) {
                if (ptt != null) {
                    ptt.off();
                } else {
                    LOGGER.log(Level.WARNING, "PTT not connected.");
                }
                clip.close();
            }
        }

    }
}
