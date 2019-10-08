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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.regex.Pattern;

/**
 *
 * @author bri
 */
public class RelacionaPalavras {

    //deleta todo o conteudo de word e rlContentWord
    public void limparTabelas(Connection conn){
        try(Statement stmtDelete = conn.createStatement();){ 
            
            String queryDeleteFromWord = "DELETE FROM word";
            String queryDeleteFromRl = "DELETE FROM rlContentWord";      
            stmtDelete.executeUpdate(queryDeleteFromWord);
            stmtDelete.executeUpdate(queryDeleteFromRl);
            
        }catch(SQLException E){ 
        }
    }
    
    //essa classe sera utilizada como valor mapeado no hashMap globalWordCount
    class wordNode {//dada uma palavra em String, o mapa retorna 
        int ID;    //uma instancia de wordNode, contendo a dupla que indica
        int count; //o ID da palavra, e quantas vezes ela ja foi encontrada no database
        wordNode(int id, int count) {
            this.ID = id;
            this.count = count;
        }
        void novaOcorrencia(){this.count++;}
        int getId(){return this.ID;}
        int getCount(){return this.count;}
    }
    
    public void run() {

        //estabelecendo conexao, criando novo statement, executando select com o novo statement
        //essas operacoes foram executadas no mesmo bloco try-with-resources
        //para aproveitar a implementação de AutoClosable
        try(Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/bri?user=root&password=1234&useSSL=false&autoReconnect=true");
            Statement stmtSelectContent = conn.createStatement();
            ResultSet rs =  stmtSelectContent.executeQuery("SELECT id, text FROM content");){
             
            //DELETE FROM word && DELETE FROM rlContentWord
            limparTabelas(conn);
         
            //uma entry para cada palavra unica encontrada em toda a db
            //as chaves sao as palavras, os valores retornados os indices que irao popular word
            LinkedHashMap<String, wordNode> globalWordCount = new LinkedHashMap();

            Stemmenator st = new Stemmenator();

            String insereStmtWord = "INSERT INTO word (id, word, ni) VALUES (?, ?, ?)";
            String insereStmtRL = "INSERT INTO rlContentWord (contentId, wordId, fij) VALUES (?, ?, ?)";

            //para mehorar a performance dos executeBatch()
            //todas as mudancas serao aplicadas em um unico commit ao fim da execucao
            conn.setAutoCommit(false);

            //padrao expressao regular pre compilado para comparar contra cada String encontrada
            //em cada texto presente na tabela content
            String regexpAlfabeto = "[A-Za-z]+";
            Pattern padraoAlfabeto = Pattern.compile(regexpAlfabeto);

            int palavraCont = 0;
            int batchCount = 0;

            int intervalo = 2500;
            boolean hasNext = true;
                
            //esses PreparedStatements vao realizar os INSERTS
            //nas tabelas rlContentWord e word, respectivamente
            //pstmtInsereRL chama seu executeBatch() a cada "intervalo" paginas de content processadas
            //pstmtInsereWord chama seu executeBatch() apenas uma vez, ao fim da execução
            //utilizasse try-with-resources pois PreparedStatement extends AutoClosable
            try (PreparedStatement pstmtInsereRL = conn.prepareStatement(insereStmtRL);
                 PreparedStatement pstmtInsereWord = conn.prepareStatement(insereStmtWord);){
     
                System.out.println("Batch RL sera executado a cada: "+intervalo+" entries da tabela content");
                
                while (hasNext) {

                    int paginasProcessadas = 0;
                    while ((hasNext = rs.next()) && paginasProcessadas++ < intervalo) {

                        int contentID = rs.getInt("id");
                        //mapeia quais palavras foram encontradas na pagina atual 
                        //de content, com quantas vezes cada uma ocorre
                        HashMap<Integer, Integer> pageWordCount = new HashMap();

                        //pega texto como armazenado na tabla content
                        String dirtyText = new String(rs.getBytes("text"), "UTF-8");
                        //tira tudo que nao eh alfanumerico e troca multiplos espacos por espaco singular
                        String cleanText = dirtyText.replaceAll("\\s+", " ");
                        //separa todas as palavras do texto limpo
                        String[] palavras = cleanText.split(" ");

                        for (String palavraAtual : palavras) {
                            //compara palavra atual com o padrao pre compilado anteriormente
                            //retorna false para toda String que contenha qualquer coisa diferente de [A-Za-z]+
                            if (padraoAlfabeto.matcher(palavraAtual).matches()) {

                                palavraAtual = palavraAtual.toLowerCase();
                                st.stem(palavraAtual);

                                //caso a palavra atual nao tenha aparecido no db ate agora
                                if (!globalWordCount.containsKey(palavraAtual)) {
                                    palavraCont++;
                                    //coloca a palavra no hashmap global
                                    globalWordCount.put(palavraAtual, new wordNode(palavraCont, 0));
                                }
                                int wordID = globalWordCount.get(palavraAtual).getId();
                                //adiciona uma ocorrencia dessa palavra no globalWorkCount
                                globalWordCount.get(palavraAtual).novaOcorrencia();
                                //caso a palavra atual nao tenha aparecido na entrada atual de content
                                if (!pageWordCount.containsKey(wordID)) {
                                    //insere em pageWordCount a aparicao dessa palavra na pagina atual
                                    pageWordCount.put(wordID, 0);
                                }
                                //atualiza o numero de ocorrencia da palavra na tabela atual
                                pageWordCount.computeIfPresent(wordID, (k, v) -> v + 1);
                            }
                        }
                        //INSERT INTO rlContentWord (contentId, wordId, fij) VALUES (?, ?, ?)
                        for (int wordID : pageWordCount.keySet()) {
                            pstmtInsereRL.setInt(1, contentID);  //contentID
                            pstmtInsereRL.setInt(2, wordID);     //wordID
                            pstmtInsereRL.setInt(3, pageWordCount.get(wordID));//fij
                            pstmtInsereRL.addBatch();
                        }                        
                    }                
                    batchCount++;
                    System.out.println("Executing RL batch: " + batchCount);
                    pstmtInsereRL.executeBatch();
                    System.out.println("RL batch: " + batchCount + " -> completed");
                }
                System.out.println("rlContentWord populada por completo\n");
                System.out.println(palavraCont + " palavras unicas encontradas");
            
                //INSERT INTO rlContentWord (contentId, wordId, ni) VALUES (?, ?, ?)
                for (String palavra : globalWordCount.keySet()) {
                    pstmtInsereWord.setInt(1, globalWordCount.get(palavra).getId());//contentID
                    pstmtInsereWord.setBytes(2, palavra.getBytes());//word
                    pstmtInsereWord.setInt(3, globalWordCount.get(palavra).getCount());//ni
                    pstmtInsereWord.addBatch();
                }             
                System.out.println("Executing Word Batch");
                pstmtInsereWord.executeBatch();
                System.out.println("word populada por completo");
                
                //aplicando as mudancas ao db e fechando a conexao
                conn.commit();
            }
        } catch (UnsupportedEncodingException | SQLException E) {
            System.out.println("Falha na conexao: " + E.getMessage());
        }

    }
}
