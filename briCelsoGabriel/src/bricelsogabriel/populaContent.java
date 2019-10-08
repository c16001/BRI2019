/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bricelsogabriel;

import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import wikipediafunctions.WikipediaFunctions;

/**
 *
 * @author bri
 */
public class  populaContent {
    
    static HashMap<Integer, String> namespaceReference;
    
    populaContent(){
            namespaceReference = new HashMap();
            namespaceReference.put(0, "");
            namespaceReference.put(1, "Talk:");
            namespaceReference.put(2, "User:");
            namespaceReference.put(3, "User_talk:");
            namespaceReference.put(4, "Wikipedia:");
            namespaceReference.put(5, "Wikipedia_talk:");
            namespaceReference.put(6, "File:");
            namespaceReference.put(7, "File_talk:");
            namespaceReference.put(8, "MediaWiki:");
            namespaceReference.put(9, "MediaWiki_talk:");
            namespaceReference.put(10, "Template:");
            namespaceReference.put(11, "Template_talk:");
            namespaceReference.put(12, "Help:");
            namespaceReference.put(13, "Help_talk:");
            namespaceReference.put(14, "Category:");
            namespaceReference.put(15, "Category_talk:");
            namespaceReference.put(100, "Portal:");
            namespaceReference.put(101, "Portal_talk:");
            namespaceReference.put(108, "Book:");
            namespaceReference.put(109, "Book_talk:");
            namespaceReference.put(118, "Draft:");
            namespaceReference.put(119, "Draft_talk:");
            namespaceReference.put(446, "Education_Program:");
            namespaceReference.put(447, "Education_Program_talk:");
            namespaceReference.put(710, "TimedText:");
            namespaceReference.put(711, "TimedText_talk:");
            namespaceReference.put(828, "Module:");
            namespaceReference.put(829, "Module_talk:");
            namespaceReference.put(2300, "Gadget:");
            namespaceReference.put(2301, "Gadget_talk:");
            namespaceReference.put(2302, "Gadget_definition:");
            namespaceReference.put(2303, "Gadget_definition_talk:");
            namespaceReference.put(-1, "Special:");
            namespaceReference.put(-2, "Media:");
    }
    
    public String getNamespace(int namespace) {
		return (namespaceReference.get(namespace));
    }
    
    public void testeConexao(){      
        Connection conn;                    
        System.out.println("Testando conexao 1.");      
        try{
            conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/wikipedia?user=root&password=1234&useSSL=false&autoReconnect=true");
            System.out.println("Conexao 1 bem sucedida.");    
            conn.close();
        }catch (SQLException E) {
            System.out.println("Falha na conexao 1: " + E.getMessage());
	}   
        System.out.println("Testando conexao 2.");
        try{
          conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/wikipedia?user=root&password=1234&useSSL=false&autoReconnect=true");
          System.out.println("Conexao 2 bem sucedida.");    
          conn.close();
        }catch(SQLException E){
           System.out.println("Falha na conexao 2: " + E.getMessage());
        }
    }
    
    public void run(){
        Connection conn1;
        Connection conn2;
        
        Statement stmt1;
        PreparedStatement pstmt2;
        
        ResultSet rs;
        
        try{
            conn1 = DriverManager.getConnection("jdbc:mysql://localhost:3306/wikipedia?user=root&password=1234&useSSL=false&autoReconnect=true");
            conn2 = DriverManager.getConnection("jdbc:mysql://localhost:3306/bri?user=root&password=1234&useSSL=false&autoReconnect=true");
            
            stmt1 = conn1.createStatement();
            
            rs = stmt1.executeQuery("SELECT MIN(page_latest), MAX(page_latest) FROM page;");
            rs.next();
            int min = rs.getInt(1);
            int max = rs.getInt(2);
            rs.close();
            /*Poderia retirar o limite ou aumentalo
              Poderia ler toda a base primeiro em uma linked list, fazer as alterações necessarias
              e só depois colocá-las na base, sem fazer alretações linha por linha.*/
            int interval = 4_999;
            WikipediaFunctions wikiCleaner = new WikipediaFunctions();
            while (min <= max) {
                    String querySelect = "SELECT page_title, page_namespace, old_text "
                                    + "FROM page, revision, text "
                                    + "WHERE page_latest BETWEEN " + min + " AND " + (min + interval) + " "
                                    + "AND page_latest = rev_id "
                                    + "AND rev_text_id = old_id;";
                    
                    rs = stmt1.executeQuery(querySelect);
                    while(rs.next()){
                        
                        String page_title = new String(rs.getBytes("page_title"), "UTF-8");
                        int namespace = rs.getInt("page_namespace");
                        String old_text = new String(rs.getBytes("old_text"), "UTF-8");
                        
                        String plainText = wikiCleaner.cleanText(old_text);
                        String url = "https://en.wikipedia.org/wiki/" + getNamespace(namespace) + page_title;
                        
                        String query2 = "INSERT INTO content (title, url, originalText, text) values (?, ?, ?, ?);";

                        pstmt2 = conn2.prepareStatement(query2);
                        pstmt2.setBytes(1, page_title.getBytes());
                        pstmt2.setBytes(2, url.getBytes());
                        pstmt2.setBytes(3, old_text.getBytes());
                        pstmt2.setBytes(4, plainText.getBytes());
                        pstmt2.executeUpdate();
                        pstmt2.close();               
                    }
                    rs.close();
                    System.out.println((min + interval) / (double) max + " concluidos.");
                    min += interval + 1;
            }
            
            
        } catch (UnsupportedEncodingException | SQLException E) {
			System.out.println("Falha na conexao: " + E.getMessage());
	}
        
        
    }
    
}
