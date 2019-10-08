/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bricelsogabriel;

import org.tartarus.snowball.ext.PorterStemmer;

/**
 *
 * @author bri
 */
public class Stemmenator {
    
    PorterStemmer stemmer;
    Stemmenator(){
        this.stemmer = new PorterStemmer();
    }

    public String stem(String palavra){      
        stemmer.setCurrent(palavra); //set string you need to stem
        stemmer.stem();              //stem the word
        return stemmer.getCurrent(); //get the stemmed word
    }
}
