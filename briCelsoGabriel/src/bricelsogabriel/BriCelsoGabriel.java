/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bricelsogabriel;

import java.sql.SQLException;

/**
 *
 * @author bri
 */
public class BriCelsoGabriel {

    /**
     * @param args the command line arguments
     * @throws java.sql.SQLException
     */
    public static void main(String[] args) throws SQLException {
        //populaContent pc = new populaContent();
        //pc.testeConexao();
        //pc.run();
        RelacionaPalavras rp = new RelacionaPalavras();
        rp.run();
        
    }
    
}
